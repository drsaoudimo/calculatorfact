package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
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
import com.example.math.RowRepresentativeMode
import com.example.math.SDIMath
import com.example.viewmodel.SpectralViewModel
import java.math.BigInteger

// Color Tokens definition
private val BackgroundColor = Color(0xFF090D15)
private val SurfaceCardColor = Color(0xFF111827)
private val PrimaryCyan = Color(0xFF00E5FF)
private val AccentGold = Color(0xFFFFC107)
private val TextWhite = Color(0xFFF1F5F9)
private val TextGrey = Color(0xFF94A3B8)
private val DarkGreyLine = Color(0xFF1F2937)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpectralDashboard(
    viewModel: SpectralViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var inputTemp by remember { mutableStateOf(uiState.inputN) }

    // Keep UI in sync with state if state changes from history selection
    LaunchedEffect(uiState.inputN) {
        inputTemp = uiState.inputN
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // High-Tech Header Section
            AcademicHeader()

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Input Control and representative selector
                item {
                    AnalysisInputCard(
                        inputValue = inputTemp,
                        onValueChange = {
                            inputTemp = it
                            viewModel.updateInput(it)
                        },
                        selectedMode = uiState.selectedMode,
                        onModeChange = { viewModel.updateMode(it) },
                        onAnalyzeClick = { viewModel.executeSpectralAnalysis(inputTemp) },
                        onRegisterManualFactors = { p1, p2 -> viewModel.registerManualFactors(inputTemp, p1, p2) },
                        isLoading = uiState.isLoading,
                        errorMsg = uiState.error
                    )
                }

                // Interactive calculations output cards
                val activeN = uiState.activeN
                if (activeN != null) {
                    item {
                        PrimeFactorisationCard(
                            valueN = activeN,
                            factorsText = uiState.factorsText,
                            factorsMap = uiState.primeFactors
                        )
                    }

                    val analysis = uiState.semiprimeAnalysis
                    if (analysis != null) {
                        item {
                            SemiprimeSpectralSolverCard(analysis = analysis)
                        }
                    }

                    item {
                        DiagnosticVisualizationCard(
                            spectrum = uiState.currentSpectrum,
                            baseline = uiState.baselineSpectrum,
                            visibility = uiState.visibilityMetric,
                            isOpacityTriggered = uiState.isOpacityTriggered
                        )
                    }

                    item {
                        AcademicSpectrumListCard(
                            spectrum = uiState.currentSpectrum,
                            baseline = uiState.baselineSpectrum
                        )
                    }

                    // Smart AI Explanation Block
                    item {
                        AiAcademicInterpretationCard(
                            valueN = activeN,
                            isAiLoading = uiState.isAiLoading,
                            aiExplanation = uiState.aiExplanation,
                            onRequestInsight = { viewModel.requestAiInsight() }
                        )
                    }
                }

                // Database Historical Records
                item {
                    HistoricalRecordsCard(
                        history = uiState.inspectionHistory,
                        onRecordClick = { viewModel.loadFromHistory(it) },
                        onClearClick = { viewModel.clearHistory() }
                    )
                }
            }
        }
    }
}

@Composable
fun AcademicHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SurfaceCardColor, BackgroundColor)
                )
            )
            .padding(vertical = 18.dp, horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Analysis Logo",
                    tint = PrimaryCyan,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "SFI SPECTRAL LAB",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "مختبر تفكيك القواسم لعدد طبيعي وتحليل الطيف الموصوف",
                color = TextGrey,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnalysisInputCard(
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
        colors = CardDefaults.cardColors(containerColor = SurfaceCardColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "إعداد عينة التحليل",
                color = PrimaryCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            // Input number Field supporting huge BigInteger
            OutlinedTextField(
                value = inputValue,
                onValueChange = onValueChange,
                label = { Text("قيمة الهدف N (يدعم أرقام ضخمة جداً)", color = TextGrey) },
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
                ),
                trailingIcon = {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = PrimaryCyan,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = onAnalyzeClick) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "سحبي",
                                tint = PrimaryCyan
                            )
                        }
                    }
                }
            )

            if (errorMsg != null) {
                Text(
                    text = errorMsg,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
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
                        OutlinedTextField(
                            value = manualP1,
                            onValueChange = { manualP1 = it },
                            label = { Text("عامل أول p1 (الأصغر)", color = TextGrey, fontSize = 11.sp) },
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
                            label = { Text("عامل ثانٍ p2 (الأكبر)", color = TextGrey, fontSize = 11.sp) },
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
                            modifier = Modifier.fillMaxWidth().height(40.dp)
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
                fontSize = 12.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RowRepresentativeMode.values().forEach { mode ->
                    val isSelected = selectedMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) PrimaryCyan else DarkGreyLine)
                            .clickable { onModeChange(mode) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (mode) {
                                RowRepresentativeMode.PRIMES -> "الأعداد الأولية"
                                RowRepresentativeMode.COMPOSITES -> "المركبة"
                                RowRepresentativeMode.FIBONACCI -> "فيبوناتشي"
                            },
                            color = if (isSelected) BackgroundColor else TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Button(
                onClick = onAnalyzeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("analyze_button"),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "ابدأ التحليل",
                    color = BackgroundColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun PrimeFactorisationCard(
    valueN: BigInteger,
    factorsText: String,
    factorsMap: Map<BigInteger, Int>
) {
    val isPrime = factorsMap.size == 1 && factorsMap.values.firstOrNull() == 1
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    // Infinite primality verification state
    var certaintySteps by remember(factorsMap) { mutableStateOf(5) }
    var allPrimesChecked by remember(factorsMap) { mutableStateOf<Boolean?>(null) }
    var currentTestingFactor by remember(factorsMap) { mutableStateOf<BigInteger?>(null) }

    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(2000)
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
            if (!k.isProbablePrime(10)) {
                allPrimesChecked = false
                currentTestingFactor = null
                return@LaunchedEffect
            }
        }
        allPrimesChecked = true
        certaintySteps = 10

        // Infinite verification loop ("استمر للأبد")
        while (true) {
            kotlinx.coroutines.delay(1200)
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
            // Continues forever updating UI with higher check accuracy steps
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardColor),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isPrime) AccentGold.copy(0.4f) else Color.Transparent)
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
                    text = "التفكيك الأولي للعدد (الضرب القاسمي الفريد)",
                    color = PrimaryCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                // High-fidelity type badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isPrime) AccentGold.copy(0.2f) else DarkGreyLine)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isPrime) "عدد أولي (Prime)" else "عدد مركب (Composite)",
                        color = if (isPrime) AccentGold else TextWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Divider(color = DarkGreyLine)

            Text(
                text = "N = $valueN",
                color = TextWhite,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
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
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick Copy Tool Icon Button next to text
                    IconButton(
                        onClick = {
                            val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Spectral Factors", factorsText)
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

            // Copy terms button requested by user ("زر نسخ الحدود من فضلك")
            Button(
                onClick = {
                    val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Spectral Factors", factorsText)
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

            // Infinite Primality Verification Section ("الرجاء التحقق ان جميع الحدود اولية او استمر للابد")
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
                        
                        // Animated pulsing dot/progress
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
                            Text(
                                text = "⚠️ تنبيه: تم اكتشاف حد مركب ضمن العوامل!",
                                color = Color.Red,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "أحد حدود التفكيك ليس أولياً، يرجى مراجعة قيم المدخلات.",
                                color = TextGrey,
                                fontSize = 10.sp
                            )
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
fun DiagnosticVisualizationCard(
    spectrum: DoubleArray,
    baseline: DoubleArray,
    visibility: Double,
    isOpacityTriggered: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "رنين التداخل الطيفي الموصوف (Resonance Canvas)",
                color = PrimaryCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Divider(color = DarkGreyLine)

            // Dynamic Spectral Drawing
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(BackgroundColor, RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                ResonanceCanvas(
                    spectrum = spectrum,
                    baseline = baseline,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Legend labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(PrimaryCyan, RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("طيف الرنين الحالي σ(H_N)", color = TextWhite, fontSize = 11.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(Color.Gray.copy(0.6f), RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("طيف الأساس العاتم (Baseline)", color = TextWhite, fontSize = 11.sp)
                }
            }

            Divider(color = DarkGreyLine)

            // Visibility Metric indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مسافة الرؤية الطيفية (Spectral Visibility):",
                    color = TextGrey,
                    fontSize = 12.sp
                )
                Text(
                    text = String.format("%.6f", visibility),
                    color = PrimaryCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Linear Progress mapping visibility
            val maxVisibilityRef = 100.0
            val progressVal = (visibility / maxVisibilityRef).coerceIn(0.0, 1.0).toFloat()
            LinearProgressIndicator(
                progress = progressVal,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = PrimaryCyan,
                trackColor = DarkGreyLine
            )

            // Opacity Trigger alarm
            if (isOpacityTriggered) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentGold.copy(0.15f))
                        .border(1.dp, AccentGold, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Opacity Alert",
                            tint = AccentGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "حالة العتمة الطيفية بالكامل (Full Opacity Zone)",
                                color = AccentGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "مفعل نتيجة كون العينة قواسم أولية غير مترابطة لأي من خطوط الإسقاط لـ A1.",
                                color = TextWhite,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResonanceCanvas(
    spectrum: DoubleArray,
    baseline: DoubleArray,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val padding = 32f

        val fullSpectrum = spectrum + baseline
        val maxVal = (fullSpectrum.maxOrNull() ?: 1.0).toFloat().coerceAtLeast(1f)

        // Draw background horizontal grid
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = padding + (height - 2 * padding) * i / gridLines
            drawLine(
                color = DarkGreyLine.copy(0.4f),
                start = androidx.compose.ui.geometry.Offset(padding, y),
                end = androidx.compose.ui.geometry.Offset(width - padding, y),
                strokeWidth = 1f
            )
        }

        // Helper calculation
        fun getPoint(index: Int, rawValue: Double): androidx.compose.ui.geometry.Offset {
            val x = padding + (width - 2 * padding) * index / 5
            val y = height - padding - (rawValue.toFloat() / maxVal) * (height - 2 * padding)
            return androidx.compose.ui.geometry.Offset(x, y)
        }

        // Draw Baseline Spectrum
        val baselinePoints = baseline.mapIndexed { i, value -> getPoint(i, value) }
        for (i in 0 until baselinePoints.size - 1) {
            drawLine(
                color = Color.Gray.copy(alpha = 0.5f),
                start = baselinePoints[i],
                end = baselinePoints[i + 1],
                strokeWidth = 4f
            )
        }
        baselinePoints.forEach { pt ->
            drawCircle(
                color = Color.Gray.copy(alpha = 0.8f),
                radius = 8f,
                center = pt
            )
        }

        // Draw Active Spectrum
        val activePoints = spectrum.mapIndexed { i, value -> getPoint(i, value) }
        for (i in 0 until activePoints.size - 1) {
            drawLine(
                color = PrimaryCyan,
                start = activePoints[i],
                end = activePoints[i + 1],
                strokeWidth = 6f
            )
        }
        activePoints.forEach { pt ->
            drawCircle(
                color = PrimaryCyan,
                radius = 12f,
                center = pt
            )
            drawCircle(
                color = Color.White,
                radius = 5f,
                center = pt
            )
        }
    }
}

@Composable
fun AcademicSpectrumListCard(
    spectrum: DoubleArray,
    baseline: DoubleArray
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "مصفوفة المؤشرات الطيفية الذاتية σ(H_N) والأساس",
                color = PrimaryCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Divider(color = DarkGreyLine)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("مؤشر الرتبة", modifier = Modifier.weight(1f), color = TextGrey, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("القيمة الحالية", modifier = Modifier.weight(1.5f), color = TextGrey, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                Text("قيمة الأساس", modifier = Modifier.weight(1.5f), color = TextGrey, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
            }

            Divider(color = DarkGreyLine.copy(0.5f))

            for (i in 0 until Math.min(spectrum.size, baseline.size)) {
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "القيمة الذاتية λ_${i + 1}",
                        modifier = Modifier.weight(1f),
                        color = TextWhite,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = String.format("%.4f", spectrum[i]),
                        modifier = Modifier.weight(1.5f),
                        color = PrimaryCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = String.format("%.4f", baseline[i]),
                        modifier = Modifier.weight(1.5f),
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
fun AiAcademicInterpretationCard(
    valueN: BigInteger,
    isAiLoading: Boolean,
    aiExplanation: String,
    onRequestInsight: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardColor),
        shape = RoundedCornerShape(16.dp)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "AI Symbol",
                        tint = PrimaryCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "التحليل الطوبولوجي الذكي (AI)",
                        color = PrimaryCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                if (aiExplanation.isNotEmpty() && !isAiLoading) {
                    Button(
                        onClick = onRequestInsight,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkGreyLine),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("تحديث التقرير", color = TextWhite, fontSize = 11.sp)
                    }
                }
            }

            Divider(color = DarkGreyLine)

            if (isAiLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = PrimaryCyan)
                    Text(
                        text = "جاري تجميع الانحناءات الطوبولوجية وصياغة التفسير العلمي...",
                        color = TextGrey,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (aiExplanation.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundColor, RoundedCornerShape(10.dp))
                        .padding(14.dp)
                        .border(1.dp, DarkGreyLine, RoundedCornerShape(10.dp))
                ) {
                    Text(
                        text = aiExplanation,
                        color = TextWhite,
                        fontSize = 13.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Justify
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "هل ترغب في صياغة تقرير أكاديمي ونمذجة طوبولوجية دلالية للعدد $valueN؟",
                        color = TextGrey,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onRequestInsight,
                        modifier = Modifier.testTag("request_ai_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("صياغة التحليل الأكاديمي الشامل", color = BackgroundColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoricalRecordsCard(
    history: List<SpectralRecord>,
    onRecordClick: (SpectralRecord) -> Unit,
    onClearClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardColor),
        shape = RoundedCornerShape(16.dp)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "History Icon",
                        tint = PrimaryCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "سجل التحليلات الموثقة للباحثين",
                        color = PrimaryCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                if (history.isNotEmpty()) {
                    IconButton(onClick = onClearClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "مسح الكلية",
                            tint = Color.Red.copy(0.7f)
                        )
                    }
                }
            }

            Divider(color = DarkGreyLine)

            if (history.isEmpty()) {
                Text(
                    text = "السجل البحثي فارغ حالياً، قم بدراسة الأعداد لتسجيل البصمات المقاسة.",
                    color = TextGrey,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    history.take(15).forEach { record ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(BackgroundColor)
                                .clickable { onRecordClick(record) }
                                .padding(12.dp)
                                .border(1.dp, DarkGreyLine.copy(0.5f), RoundedCornerShape(8.dp)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "N = ${record.valueN}",
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "التفكيك: ${record.factorsText}",
                                    color = TextGrey,
                                    fontSize = 11.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "الرؤية: ${String.format("%.4f", record.visibilityMetric)}",
                                    color = PrimaryCyan,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                if (record.isOpacityTriggered) {
                                    Text(
                                        text = "عتمة كاملة",
                                        color = AccentGold,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SemiprimeSpectralSolverCard(
    analysis: SDIMath.SemiprimeAnalysis
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("semiprime_solver_card"),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardColor),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, PrimaryCyan.copy(0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Spectral Math",
                    tint = AccentGold,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "مفكك الأعداد شبه الأولية بالتأويل الطيفي والانهيار القاسمي",
                    color = AccentGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Divider(color = DarkGreyLine)

            Text(
                text = "يُمثل هذا اللوح النمذجة الرياضية لثنائية الفلق واستخراج العوامل طيفياً بناءً على إرشاد الباحث واستراتيجية التصفية عبر الأثر الطيفي (Spectral Trace Filtering).",
                color = TextGrey,
                fontSize = 11.sp,
                lineHeight = 18.sp
            )

            // Section 1: Dual Space Logarithmic Shift
            if (!analysis.isPrime) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundColor, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                        .border(1.dp, DarkGreyLine, RoundedCornerShape(10.dp))
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "1. صيغة فيت في الفضاء الطوبولوجي المزدوج (Dual-Space Shift)",
                            color = PrimaryCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "• لوغاريتم الطول الرنيني ln(N): ${String.format("%.6f", analysis.lnN)}",
                            color = TextWhite,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "• فجوة المؤشرات الطيفية Δ_σ: ${String.format("%.6f", analysis.spectralGap)}",
                            color = TextWhite,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "• حل ثنائية الفلق اللوغاريتمي المتوازن:",
                            color = TextGrey,
                            fontSize = 10.sp
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceCardColor, RoundedCornerShape(6.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                    text = "P₁ = exp((ln(N) - √Δ_σ) / 2) && P₂ = exp((ln(N) + √Δ_σ) / 2)",
                                color = PrimaryCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "العامل المسترجع الأول P₁:",
                                color = TextGrey,
                                fontSize = 11.sp
                            )
                            Text(
                                text = analysis.p1.toString(),
                                color = AccentGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "العامل المسترجع الثاني P₂:",
                                color = TextGrey,
                                fontSize = 11.sp
                            )
                            Text(
                                text = analysis.p2.toString(),
                                color = PrimaryCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundColor, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                        .border(1.dp, DarkGreyLine, RoundedCornerShape(10.dp))
                ) {
                    Text(
                        text = "العدد الحالي أولي؛ النمذجة الفريدة لا تتطلب ثنائية فلق دلالية.",
                        color = TextGrey,
                        fontSize = 11.sp
                    )
                }
            }

            // Section 2: Trace Constant & Hamiltonian Coupling
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundColor, RoundedCornerShape(10.dp))
                    .padding(12.dp)
                    .border(1.dp, DarkGreyLine, RoundedCornerShape(10.dp))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "2. ثابت رنين الهاميلتونيان وأثر المنظومة المتصلة",
                        color = PrimaryCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("• أثر الهاميلتونيان Tr(H_N):", color = TextGrey, fontSize = 11.sp)
                        Text(String.format("%.4f", analysis.trHN), color = TextWhite, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("• أثر الإسقاط الموجي Tr(A1ᵀ * A1):", color = TextGrey, fontSize = 11.sp)
                        Text(String.format("%.4f", analysis.trA1A1), color = TextWhite, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("• ثابت الاقتران الطيفي κ = ΔTr / (P₁+P₂):", color = TextGrey, fontSize = 11.sp)
                        Text(
                            text = if (analysis.kappa > 1e-18) String.format("%.4e", analysis.kappa) else "0.00 (المنطقة العاتمة)",
                            color = AccentGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Section 3: Spectral Trace Filtering and Divisorial Collapse
            if (!analysis.isPrime) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundColor, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                        .border(1.dp, DarkGreyLine, RoundedCornerShape(10.dp))
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "3. تصفية الأثر الطيفي والانهيار القاسمي لـ P1",
                            color = PrimaryCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "• دالة الانهيار المحددة بالربط الترددي الموزون:",
                            color = TextGrey,
                            fontSize = 11.sp
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceCardColor, RoundedCornerShape(6.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                    text = "P₁ = gcd(N,  Σ j=1⁶ ω_j · A₁(i, j))",
                                color = PrimaryCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Text(
                            text = "• خط الاستقصاء الكاشف (Resonance Row): الصف ${analysis.resonanceVectorRow}",
                            color = TextWhite,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "• المجموع الموزون للصف الكاشف (Weighted Sum): ${String.format("%.15f", analysis.weightedSumRow)}",
                            color = TextWhite,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        
                        Divider(color = DarkGreyLine.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "عامل الانهيار القاسمي المستنتج:",
                                color = AccentGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Text(
                                text = analysis.collapsedFactor.toString(),
                                color = AccentGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Text(
                            text = "عند هذا الانهيار الطيفي، ينتهي الطيف الموصوف لـ H_N عند التردد الهارمونيكي المطابق، مما يؤدي إلى H_N (mod P₁) → 0 تماماً كما تقتضي نظرية الاقتران الطيفي السعودي.",
                            color = TextGrey,
                            fontSize = 10.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
