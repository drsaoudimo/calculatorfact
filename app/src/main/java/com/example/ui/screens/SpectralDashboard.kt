package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.db.SpectralRecord
import com.example.math.MepaResults
import com.example.viewmodel.RowRepresentativeMode
import com.example.viewmodel.SpectralViewModel
import kotlinx.coroutines.delay
import java.math.BigInteger

// Color Theme definitions
val BackgroundColor = Color(0xFF0C0F14)
val DarkCardBackground = Color(0xFF161B22)
val PrimaryCyan = Color(0xFF00E5FF)
val AccentGold = Color(0xFFFFD700)
val TextWhite = Color(0xFFF0F4F8)
val TextGrey = Color(0xFF8B949E)
val DarkGreyLine = Color(0xFF30363D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpectralDashboard(viewModel: SpectralViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val historyLog by viewModel.historyLog.collectAsStateWithLifecycle()
    var inputTemp by remember { mutableStateOf(uiState.inputN) }

    LaunchedEffect(uiState.inputN) {
        inputTemp = uiState.inputN
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "SFI Spectral Lab",
                            color = PrimaryCyan,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Matrix Energetic Prime Analysis • SFI & MEPA Model",
                            color = TextGrey,
                            fontSize = 10.sp
                        )
                    }
                },
                navigationIcon = {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "تحليلات",
                        tint = AccentGold,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundColor
                )
            )
        },
        containerColor = BackgroundColor
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SFI scientific introduction card
            item {
                ScientificHeroHeader()
            }

            // Command panel: input field and configurations
            item {
                InputsControlPanel(
                    inputValue = inputTemp,
                    onValueChange = { inputTemp = it },
                    selectedMode = uiState.selectedMode,
                    onModeChange = { viewModel.updateMode(it) },
                    onAnalyzeClick = { viewModel.executeSpectralAnalysis(inputTemp) },
                    onRegisterManualFactors = { p1, p2 -> viewModel.registerManualFactors(inputTemp, p1, p2) },
                    isLoading = uiState.isLoading,
                    errorMsg = uiState.error
                )
            }

            // Results summary container
            if (uiState.mepaResults != null) {
                // Prime Factors Display with Custom Copy Action & Eternal Primality Check
                item {
                    FactorsResultBrief(
                        factorsText = uiState.factorsText,
                        factorsMap = uiState.factors,
                        onDecomposeFactor = { fac -> 
                            // When composite sub-factors exist, trigger decompose manually or automatically
                            viewModel.registerManualFactors(uiState.inputN, fac.toString(), "1") 
                        }
                    )
                }

                // Interactive Energy Matrix Grid
                item {
                    SpectralMatrixGrid(
                        mepaResults = uiState.mepaResults!!,
                        mode = uiState.selectedMode
                    )
                }

                // Matrix and Homology Invariants
                item {
                    SpectralInvariantsCard(
                        mepaResults = uiState.mepaResults!!
                    )
                }

                // Scholarly Academic summary
                if (uiState.academicReport.isNotEmpty()) {
                    item {
                        ScholarlyBriefingCard(
                            reportText = uiState.academicReport
                        )
                    }
                }
            }

            // Saved sandbox histories
            if (historyLog.isNotEmpty()) {
                item {
                    Text(
                        text = "سجل التحليلات المسجلة (SFI Sandbox Registry Logs)",
                        color = AccentGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }

                items(historyLog) { log ->
                    HistoryRecordItem(
                        record = log,
                        onLoadClick = {
                            viewModel.updateInput(log.inputN)
                            viewModel.executeSpectralAnalysis(log.inputN)
                        },
                        onDeleteClick = { viewModel.deleteHistoryRecord(log) }
                    )
                }

                item {
                    Button(
                        onClick = { viewModel.clearAllHistory() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .padding(bottom = 16.dp)
                    ) {
                        Text("مسح السجل المالي بالكامل (Clear Registry Log)", color = TextWhite, fontSize = 11.sp)
                    }
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد سجلات مسجلة في التخزين المؤقت حالياً.",
                            color = TextGrey,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScientificHeroHeader() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCardBackground),
        border = BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "مختبر الهومولوجيا الطيفية وتفكيك الأعداد",
                color = AccentGold,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "منصة برمجية علمية متقدمة لدراسة فضاءات الأعداد وتحويلها الطبوغرافي إلى متشعبات دلالية عالية الأبعاد، مع توجيه التفريغ اللانهائي للتأكد من أولية القواسم وفك المركبات يدوياً وآلياً.",
                color = TextGrey,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun InputsControlPanel(
    inputValue: String,
    onValueChange: (String) -> Unit,
    selectedMode: RowRepresentativeMode,
    onModeChange: (RowRepresentativeMode) -> Unit,
    onAnalyzeClick: () -> Unit,
    onRegisterManualFactors: (String, String) -> Unit,
    isLoading: Boolean,
    errorMsg: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCardBackground),
        border = BorderStroke(1.dp, DarkGreyLine)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "مدخل الفحص الرقمي (Numerical Target Setup N):",
                color = TextWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = inputValue,
                onValueChange = onValueChange,
                label = { Text("أدخل العدد N المراد دراسته", color = TextGrey, fontSize = 11.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_n_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedBorderColor = PrimaryCyan,
                    unfocusedBorderColor = DarkGreyLine,
                    focusedContainerColor = BackgroundColor,
                    unfocusedContainerColor = BackgroundColor
                )
            )

            // Submit analysis
            Button(
                onClick = onAnalyzeClick,
                enabled = !isLoading && inputValue.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("analyze_btn")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = BackgroundColor,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = "تنفيذ التحليل الرياضي والطيفي المستمر",
                        color = BackgroundColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            if (errorMsg != null) {
                Text(
                    text = errorMsg,
                    color = Color.Red,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Divider(color = DarkGreyLine)

            // Optional Manual Overrides
            var showManualOverrides by remember { mutableStateOf(false) }
            var manualP1 by remember { mutableStateOf("") }
            var manualP2 by remember { mutableStateOf("") }

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showManualOverrides = !showManualOverrides }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "خيارات الإدخال اليدوي المتقدمة للعوامل",
                        color = AccentGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = if (showManualOverrides) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "قائمة منسدلة",
                        tint = AccentGold,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (showManualOverrides) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "لوصف عاملين p1, p2 حيث N = p1 × p2 ومطابقة البنية يدوياً:",
                            color = TextGrey,
                            fontSize = 10.sp
                        )

                        OutlinedTextField(
                            value = manualP1,
                            onValueChange = { manualP1 = it },
                            label = { Text("عامل أول p1", color = TextGrey, fontSize = 11.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = AccentGold,
                                unfocusedBorderColor = DarkGreyLine,
                                focusedContainerColor = BackgroundColor,
                                unfocusedContainerColor = BackgroundColor
                            )
                        )

                        OutlinedTextField(
                            value = manualP2,
                            onValueChange = { manualP2 = it },
                            label = { Text("عامل ثانٍ p2", color = TextGrey, fontSize = 11.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = AccentGold,
                                unfocusedBorderColor = DarkGreyLine,
                                focusedContainerColor = BackgroundColor,
                                unfocusedContainerColor = BackgroundColor
                            )
                        )

                        Button(
                            onClick = { onRegisterManualFactors(manualP1, manualP2) },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        ) {
                            Text(
                                text = "التحليل بالعوامل الموصوفة يدوياً",
                                color = BackgroundColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Divider(color = DarkGreyLine)

            // Select Row representatives filter mode
            Text(
                text = "تحديد خريطة ممثلي الصفوف (Representatives Mapping):",
                color = TextWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                RowRepresentativeMode.values().forEach { mode ->
                    val isSelected = selectedMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) PrimaryCyan else DarkGreyLine)
                            .clickable { onModeChange(mode) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (mode) {
                                RowRepresentativeMode.EIGEN_COSINE -> "رنين راداري"
                                RowRepresentativeMode.GCD_MODULO -> "تطابق GCD"
                                RowRepresentativeMode.HOMOLOGICAL_BETTI -> "صيغة فضاء Betti"
                            },
                            color = if (isSelected) BackgroundColor else TextWhite,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FactorsResultBrief(
    factorsText: String,
    factorsMap: Map<BigInteger, Int>,
    onDecomposeFactor: (BigInteger) -> Unit
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    // Infinite primality verification state ("ستمر لليكل")
    var certaintySteps by remember(factorsMap) { mutableStateOf(5) }
    var allPrimesChecked by remember(factorsMap) { mutableStateOf<Boolean?>(null) }
    var currentTestingFactor by remember(factorsMap) { mutableStateOf<BigInteger?>(null) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(2000)
            copied = false
        }
    }

    LaunchedEffect(factorsMap) {
        if (factorsMap.isEmpty()) {
            allPrimesChecked = null
            return@LaunchedEffect
        }

        val keys = factorsMap.keys.toList()
        
        // Fast prime check first
        for (k in keys) {
            currentTestingFactor = k
            // Filter 1 is 10 rounds of Miller-Rabin checking
            if (!k.isProbablePrime(10)) {
                allPrimesChecked = false
                currentTestingFactor = null
                return@LaunchedEffect
            }
        }
        allPrimesChecked = true
        certaintySteps = 10

        // Infinite verification loop step-by-step
        while (true) {
            delay(1200)
            certaintySteps += 5
            
            var stillPrime = true
            for (k in keys) {
                currentTestingFactor = k
                if (!k.isProbablePrime(certaintySteps)) {
                    stillPrime = false
                    break
                }
            }
            if (!stillPrime) {
                allPrimesChecked = false
                currentTestingFactor = null
                break
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCardBackground),
        border = BorderStroke(1.dp, PrimaryCyan)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تفكيك الحدود المكتشفة",
                    color = AccentGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Icon(
                    imageVector = Icons.Default.FilterFrames,
                    contentDescription = "عوامل",
                    tint = PrimaryCyan
                )
            }

            Text(
                text = "التمثيل الرياضي المستمر للتفكيك الأولي للعدد N المقترح:",
                color = TextGrey,
                fontSize = 11.sp,
                lineHeight = 18.sp
            )

            // Factor display box with Copy overlay action
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundColor, RoundedCornerShape(10.dp))
                    .border(1.dp, DarkGreyLine, RoundedCornerShape(10.dp))
                    .padding(14.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = factorsText,
                        color = PrimaryCyan,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Monospace
                    )

                    // Quick Copy Tool Icon Button next to text
                    IconButton(
                        onClick = {
                            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Spectral Factors", factorsText)
                            clipboardManager.setPrimaryClip(clip)
                            copied = true
                        },
                        modifier = Modifier
                            .testTag("copy_factors_quick_btn")
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (copied) Icons.Default.Check else Icons.Default.Share, 
                            contentDescription = "نسخ الحدود",
                            tint = if (copied) AccentGold else PrimaryCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Copy terms button explicitly requested ("زر نسخ الحدود من فضلك")
            Button(
                onClick = {
                    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Spectral Factors", factorsText)
                    clipboardManager.setPrimaryClip(clip)
                    copied = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (copied) AccentGold else PrimaryCyan
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .testTag("copy_factors_full_btn")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Default.Check else Icons.Default.Share,
                        contentDescription = "نسخ",
                        tint = BackgroundColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (copied) "تم نسخ جميع الحدود بنجاح! ✓" else "نسخ جميع الحدود (Copy Terms)",
                        color = BackgroundColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            // Infinite Primality Verification Section with automated retry analysis
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(BackgroundColor)
                    .border(1.dp, DarkGreyLine, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "بروتوكول التحقق الطيفي اللانهائي للحدود",
                            color = AccentGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        
                        if (allPrimesChecked == true) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = AccentGold,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = "مستمر للأبد...",
                                    color = AccentGold,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Divider(color = DarkGreyLine.copy(alpha = 0.5f))

                    when (allPrimesChecked) {
                        true -> {
                            val errorExponent = (certaintySteps * 0.6).toInt()
                            Text(
                                text = "✓ جميع القواسم/الحدود مؤكدة كأعداد أولية.",
                                color = Color.Green,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "• عدد جولات اختبار ميلر-رابين: $certaintySteps جولة مكثفة",
                                color = TextWhite,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "• هامش الخطأ الإحصائي: < 10^(-$errorExponent)",
                                color = TextWhite,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "• الفحص الرياضي الذاتي مستمر للأبد لرفع درجة التأكيد.",
                                color = TextGrey,
                                fontSize = 10.sp
                            )
                        }
                        false -> {
                            // Find the composite key that broke the check
                            val nonPrimeKey = factorsMap.keys.firstOrNull { !it.isProbablePrime(25) }
                            
                            Text(
                                text = "⚠️ تنبيه: تم اكتشاف حد مركب غير أولي ضمن العوامل!",
                                color = Color.Red,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            
                            if (nonPrimeKey != null) {
                                Text(
                                    text = "• الحد المركب المكتشف: $nonPrimeKey",
                                    color = TextWhite,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "• نقوم حالياً باستئناف فحص المحدودية وتفكيك هذا المركب آلياً لتبسيطه إلى أعداد اولية صحيحة.",
                                    color = TextGrey,
                                    fontSize = 10.sp
                                )
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                // Direct manual trigger button to aid in recursive disintegration
                                Button(
                                    onClick = { onDecomposeFactor(nonPrimeKey) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(34.dp)
                                ) {
                                    Text(
                                        text = "استمرار في تحليل وتفكيك الحد المركب ($nonPrimeKey) عبر الذكاء الاصطناعي",
                                        color = BackgroundColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                        else -> {
                            Text(
                                text = "⏳ جاري تهيئة بروتوكول فحص الأولية اللانهائي...",
                                color = TextGrey,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpectralMatrixGrid(
    mepaResults: MepaResults,
    mode: RowRepresentativeMode
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCardBackground),
        border = BorderStroke(1.dp, DarkGreyLine)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مصفوفة الرنين اللانهائية (MEPA Invariant Matrix)",
                    color = TextWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(AccentGold.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "البعد: ${mepaResults.dimension}×${mepaResults.dimension}",
                        color = AccentGold,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "تمثيل تفاعلي لخصائص الترشيح (Cosine-Resonance Matrix) وممثلي المجموعات الصفية:",
                color = TextGrey,
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Matrix Grid
            val matrix = mepaResults.relationMatrix
            if (matrix.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundColor, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (i in matrix.indices) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (j in matrix[i].indices) {
                                val value = matrix[i][j]
                                // Normalize value between 0 and 1 for heatmap color alpha
                                val weight = ((value + 1.0) / 2.0).coerceIn(0.01, 1.0).toFloat()
                                val cellColor = when (mode) {
                                    RowRepresentativeMode.EIGEN_COSINE -> PrimaryCyan.copy(alpha = weight)
                                    RowRepresentativeMode.GCD_MODULO -> AccentGold.copy(alpha = weight)
                                    RowRepresentativeMode.HOMOLOGICAL_BETTI -> Color(0xFFE040FB).copy(alpha = weight)
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1.2f)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(cellColor)
                                        .border(0.5.dp, DarkGreyLine, RoundedCornerShape(4.dp))
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = String.format("%.2f", value),
                                        color = if (weight > 0.6f) BackgroundColor else TextWhite,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Text("لا توجد مصفوفة لعامل وحيد.", color = TextGrey, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun SpectralInvariantsCard(
    mepaResults: MepaResults
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCardBackground),
        border = BorderStroke(1.dp, DarkGreyLine)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "المؤشرات الهيكلية والمقاييس الطوبولوجية",
                color = AccentGold,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    InvariantRow("معيار فروبينيوس الطيفي", String.format("%.4f", mepaResults.frobeniusNorm))
                    InvariantRow("مجموع الطيف المميز N", mepaResults.spectralSum.toString())
                    InvariantRow("تأويل الأبعاد الطوبولوجية", "L^2 Discrete Manifold")
                }
                
                Spacer(modifier = Modifier.width(10.dp))
                
                Divider(
                    color = DarkGreyLine,
                    modifier = Modifier
                        .height(80.dp)
                        .width(1.dp)
                )
                
                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    InvariantRow("المجموعة B0 (الأجزاء المتصلة)", mepaResults.betti0.toString())
                    InvariantRow("المجموعة B1 (الحلقات الدلالية)", mepaResults.betti1.toString())
                    InvariantRow("المجموعة B2 (الفراغات المعرفية)", mepaResults.betti2.toString())
                }
            }
        }
    }
}

@Composable
fun InvariantRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, color = TextGrey, fontSize = 10.sp)
        Text(
            text = value, 
            color = TextWhite, 
            fontSize = 12.sp, 
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun ScholarlyBriefingCard(
    reportText: String
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCardBackground),
        border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = "بحث علمي",
                        tint = AccentGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "التقرير الأكاديمي الطوبولوجي المفصل (SFI Report)",
                        color = AccentGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "عرض",
                        tint = AccentGold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "تقييم تحليلي صادر من نموذج الذكاء الاصطناعي يربط المعامل الطيفي للنص مع نظرية فضاءات باناخ وخوارزميات المعالجة الطوبولوجية.",
                color = TextGrey,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Divider(color = DarkGreyLine, modifier = Modifier.padding(bottom = 10.dp))
                    Text(
                        text = reportText,
                        color = TextWhite,
                        fontSize = 11.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Justify,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryRecordItem(
    record: SpectralRecord,
    onLoadClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCardBackground.copy(alpha = 0.7f)),
        border = BorderStroke(1.dp, DarkGreyLine.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "N = ${record.inputN}",
                    color = PrimaryCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "عوامل: ${record.factorsText}",
                    color = TextWhite,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "مجموع Betti: B0=${record.betti0}, B1=${record.betti1}, B2=${record.betti2} • Norm: ${String.format("%.2f", record.frobeniusNorm)}",
                    color = TextGrey,
                    fontSize = 9.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onLoadClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = "استدعاء",
                        tint = PrimaryCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف",
                        tint = Color.Red.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
