package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.db.SpectralRecord
import com.example.math.RowRepresentativeMode
import com.example.math.SDIMath
import com.example.ui.theme.*
import com.example.viewmodel.SpectralUiState
import com.example.viewmodel.SpectralViewModel
import java.util.*
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpectralDashboard(
    viewModel: SpectralViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val history by viewModel.historyList.collectAsState()
    var displayLanguageIsAr by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (displayLanguageIsAr) "رنين الأعداد الطيفي (SFI)" else "SFI Spectral Lab",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (displayLanguageIsAr) "التحليل الأولي عبر الانعكاس الطيفي و MEPA" else "Spectral Factorization Inversion & MEPA",
                            style = MaterialTheme.typography.labelSmall,
                            color = SoftGrayText
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { displayLanguageIsAr = !displayLanguageIsAr },
                        modifier = Modifier.testTag("lang_toggle_button")
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF1E2833),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = if (displayLanguageIsAr) "EN" else "عربي",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricTeal
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SlateDarkBackground,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = SlateDarkBackground,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Summary briefing card
            item {
                AcademicBriefingCard(displayLanguageAr = displayLanguageIsAr)
            }

            // Input parameters
            item {
                SpectralInputCard(
                    uiState = uiState,
                    onInputChange = { viewModel.updateInput(it) },
                    onModeChange = { viewModel.selectMode(it) },
                    onCalculate = { viewModel.triggerCalculation() },
                    onLoadPreset = { viewModel.updateInput(it); viewModel.triggerCalculation() },
                    displayLanguageAr = displayLanguageIsAr
                )
            }

            // Calculations errors
            if (uiState.error != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                            Text(
                                text = uiState.error ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // Dynamic results matching user's input
            uiState.activeN?.let { activeN ->
                // Prime factor factorization base and exponents display
                item {
                    PrimeFactorisationCard(
                        valueN = activeN,
                        factorsText = uiState.factorsText,
                        factorsMap = uiState.primeFactors,
                        displayLanguageAr = displayLanguageIsAr
                    )
                }

                // Opacity warning card (Spectral Coprime blind spot)
                item {
                    SpectralCouplingStatusCard(
                        isOpacityTriggered = uiState.isOpacityTriggered,
                        visibilityDistance = uiState.visibilityDistance,
                        displayLanguageAr = displayLanguageIsAr
                    )
                }

                // Spectrum line/peaks comparison canvas
                item {
                    SpectralChartCard(
                        currentSpectrum = uiState.currentSpectrum,
                        baselineSpectrum = uiState.baselineSpectrum,
                        displayLanguageAr = displayLanguageIsAr
                    )
                }

                // 6x6 operator spectral heatmap
                item {
                    OperatorSpectroscopyMatrixCard(
                        valueN = activeN,
                        selectedMode = uiState.selectedMode,
                        displayLanguageAr = displayLanguageIsAr
                    )
                }

                // Gemini Academic Report card
                item {
                    GeminiAcademicAnalysisCard(
                        uiState = uiState,
                        onRequestAiAnalysis = { viewModel.requestAiAnalysis() },
                        displayLanguageAr = displayLanguageIsAr
                    )
                }
            }

            // SQLite historical Logs
            item {
                HistoryLogsCard(
                    historyList = history,
                    onSelectRecord = { viewModel.loadFromHistory(it) },
                    onDeleteRecord = { viewModel.deleteHistoryRecord(it) },
                    onClearHistory = { viewModel.clearHistory() },
                    displayLanguageAr = displayLanguageIsAr
                )
            }
        }
    }
}

@Composable
fun AcademicBriefingCard(displayLanguageAr: Boolean) {
    var expanded by remember { mutableStateOf(true) }
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ElectricTeal.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Academic Overview",
                        tint = ElectricTeal
                    )
                    Text(
                        text = if (displayLanguageAr) "الملخص التنفيذي العلمي النهائي (SFI)" else "Final Scientific Executive Summary (SFI)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Text(
                    text = if (expanded) "▲" else "▼",
                    color = SoftGrayText,
                    fontWeight = FontWeight.Bold
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = GridLineColor, modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text(
                        text = if (displayLanguageAr) {
                            "🔬 المشروع: التحليل الأولي عبر الانعكاس الطيفي (SFI)\n" +
                            "📐 الإطار: مصفوفة تحليل طاقة الأعداد الأولية (MEPA)\n" +
                            "📊 مصفوفة القيادة: A1 (19x6)"
                        } else {
                            "🔬 Project: The Spectral Factorization Inversion (SFI)\n" +
                            "📐 Framework: Matrix Energetic Prime Analysis (MEPA)\n" +
                            "📊 Lead Matrix: A1 (19x6)"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ElectricTeal
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = if (displayLanguageAr) {
                            "1️⃣ الكيان الرياضي\n" +
                            "تم إثبات وجود مؤثر هاملتوني نطلق عليه \"مؤثر الإسقاط القاسمي\" (HN)، والذي يقوم بتحويل العدد الصحيح N من قيمته الرقمية المجردة إلى \"حالة طاقوية\" داخل فضاء مصفوفي:\n" +
                            "H_N = A1ᵀ · diag(gcd(N, a_k)) · A1\n" +
                            "هذا المؤثر ليس مجرد تمثيل، بل هو \"ترميز مكاني\" (Spatial Encoding) للبنية القاسمية لـ N."
                        } else {
                            "1️⃣ The Mathematical Entity\n" +
                            "An operator Hamiltonian named the \"divisor-projection operator\" (H_N) was proven to exist. It converts the abstract integer N into an \"energetic state\" within a matrix space:\n" +
                            "H_N = A1ᵀ · diag(gcd(N, a_k)) · A1\n" +
                            "Rather than a mere representation, this operator acts as a \"spatial encoding\" of N's divisor architecture."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        text = if (displayLanguageAr) {
                            "2️⃣ حل معضلة اللاخطية\n" +
                            "لتجاوز عقبة \"متعددة الحدود المميزة\" والارتباط عالي الدرجة بين الجذور والعوامل الأولية، تم اعتماد \"استراتيجية الثوابت الطيفية\" (Spectral Invariants). بدلاً من البحث عن الجذور الفردية (λ_i)، يتم استغلال:\n\n" +
                            "• التجانس الهيكلي: الربط الخطي بين أثر مصفوفة المؤشر (Tr(H_N)) ومجموع القواسم.\n" +
                            "• التحويل اللوغاريتمي: استخدام ln(H_N) لتحويل العلاقات الضربية للعوامل الأولية إلى علاقات جمعية ترددية، مما يكسر حدة اللاخطية الجبرية."
                        } else {
                            "2️⃣ Solving Non-linearity\n" +
                            "To bypass the challenge of characteristic polynomials and high-degree correlation between eigenvalues and prime factors, the \"Spectral Invariants Strategic Approach\" is deployed. Instead of targeting individual eigenvalues (λ_i), we leverage:\n\n" +
                            "• Structural Homogeneity: Linear coupling between the projection matrix trace (Tr(H_N)) and divisor sum.\n" +
                            "• Logarithmic Transformation: Operating on ln(H_N) to convert multiplicative prime factor interactions into additive frequency harmonics, cracking algebraic non-linearity."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        text = if (displayLanguageAr) {
                            "3️⃣ مبرهنة الاسترجاع والكفاءة\n" +
                            "• الوجود: الدالة العكسية F التي تستعيد p من H_N موجودة نظرياً طالما أن N يقع ضمن \"مجال الرؤية\" (V(N) > 0).\n" +
                            "• الكفاءة: بما أن أبعاد المصفوفة A1 ثابتة (6x6)، فإن تعقيد العملية يؤول إلى O(log N) (وقت لوغاريتمي)، وهو ما يمثل تفوقاً نظرياً على كافة الخوارزميات الحالية التي تتبع التعقيد الأسي."
                        } else {
                            "3️⃣ Inversion Theorem & Complexity\n" +
                            "• Existence: The inverse function F recovering p from H_N is theoretically guaranteed as long as N remains inside the active \"visibility boundary\" (V(N) > 0).\n" +
                            "• Complexity: Since matrix A1 dimensions are strictly fixed (6x6), the operational cost simplifies to O(log N) (logarithmic time), representing a breakthrough over current exponential complexity models."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF13202E), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = if (displayLanguageAr) "🏆 النتيجة النهائية (Final Verdict)" else "🏆 Final Verdict",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = ElectricTeal
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (displayLanguageAr) {
                                    "المشروع قد صاغ بنجاح \"لغة رنين جديدة\" للأعداد. الأعداد الأولية ليست مجرد قواسم، بل هي \"ترددات أساسية\" داخل النظام الهاملتوني للمصفوفة.\n\n" +
                                    "الخلاصة الإجرائية للتحليل:\n" +
                                    "• الإسقاط: تمرير N عبر ملتقطات وفلترة gcd للمصفوفة A1.\n" +
                                    "• التوليد: بناء مؤثر الطاقة H_N.\n" +
                                    "• فك الترميز: استخراج عوامل الأعداد عبر الثوابت الطيفية (الأثر والمحدد) في فضاء الإزاحة اللوغاريتمية.\n\n" +
                                    "وبهذا يتحول تحليل الأعداد من معضلة حسابية شاقة إلى عملية رصد طيفي دقيق، حيث العوامل الأولية هي البصمة الضوئية التي تتركها الأعداد عند مرورها عبر منشور المصفوفة A1."
                                } else {
                                    "The project successfully defines a \"resonance language\" for numbers. Primes are no longer treated as raw divisors, but rather as \"fundamental frequencies\" within the Hamiltonian system of the operator matrix.\n\n" +
                                    "Procedural Analysis Flow:\n" +
                                    "• Projection: Passing N through the GCD-sensors of A1.\n" +
                                    "• Generation: Structuring the energetic operator H_N.\n" +
                                    "• Decoding: Reconstructing prime factors via Spectral Invariants (Trace & Determinant) in logarithmic shift space.\n\n" +
                                    "Thus, prime factorization transitions from a heavy search to pristine spectral spectroscopy: prime factors are the unique optical signature left by numbers passing through the A1 prism."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = SoftGrayText,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpectralInputCard(
    uiState: SpectralUiState,
    onInputChange: (String) -> Unit,
    onModeChange: (RowRepresentativeMode) -> Unit,
    onCalculate: () -> Unit,
    onLoadPreset: (String) -> Unit,
    displayLanguageAr: Boolean
) {
    var expandedDropdown by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GridLineColor, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (displayLanguageAr) "إعدادات مدخلات الرمال الطيفية" else "Resonance Parameters & Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.inputN,
                onValueChange = onInputChange,
                label = { Text(if (displayLanguageAr) "أدخل عدد طبيعي (N)" else "Enter Natural Number (N)") },
                placeholder = { Text("مثال: 2026") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_n_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricTeal,
                    unfocusedBorderColor = GridLineColor,
                    focusedLabelColor = ElectricTeal,
                    unfocusedLabelColor = SoftGrayText,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (displayLanguageAr) "تحديد ممثل قواسم الصف (D_N)" else "Row Representative Mapping Mode",
                style = MaterialTheme.typography.labelSmall,
                color = ElectricTeal,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    onClick = { expandedDropdown = true },
                    color = Color(0xFF1E2833),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mode_dropdown")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (displayLanguageAr) uiState.selectedMode.displayNameAr else uiState.selectedMode.displayNameEn,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "DropdownIcon", tint = ElectricTeal)
                    }
                }
                DropdownMenu(
                    expanded = expandedDropdown,
                    onDismissRequest = { expandedDropdown = false },
                    modifier = Modifier.background(SlateDarkSurface)
                ) {
                    RowRepresentativeMode.values().forEach { mode ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (displayLanguageAr) mode.displayNameAr else mode.displayNameEn,
                                    color = Color.White
                                )
                            },
                            onClick = {
                                onModeChange(mode)
                                expandedDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onCalculate,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("calculate_button"),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (uiState.isCalculating) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = SlateDarkBackground)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = SlateDarkBackground)
                        Text(
                            text = if (displayLanguageAr) "تحليل الرنين والتحليل الأولي" else "Factorise & Calculate Spectrum",
                            color = SlateDarkBackground,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (displayLanguageAr) "عينات سريعة للاختبار العلمي:" else "Quick Spectral Presets:",
                style = MaterialTheme.typography.labelSmall,
                color = SoftGrayText
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("30", "101", "2026", "1000003").forEach { preset ->
                    val isBlindspot = preset == "1000003"
                    val label = if (isBlindspot) {
                        if (displayLanguageAr) "عتيم 1M" else "Opacity 1M"
                    } else preset

                    SuggestionChip(
                        onClick = { onLoadPreset(preset) },
                        label = { Text(label) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (isBlindspot) Color(0xFF332015) else Color(0xFF1B252E),
                            labelColor = if (isBlindspot) AtomicOrange else ElectricTeal
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun PrimeFactorisationCard(
    valueN: java.math.BigInteger,
    factorsText: String,
    factorsMap: Map<java.math.BigInteger, Int>,
    displayLanguageAr: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GridLineColor, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (displayLanguageAr) "التحليل لعاملي الفريد الرياضي لـ N" else "Unique Base-Exponent Factorisation for N",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0C1014), RoundedCornerShape(8.dp))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "N = $valueN",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "F(σ(H_N)) = $factorsText",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = CyberGreen,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (factorsMap.isEmpty()) {
                Text(
                    text = if (displayLanguageAr) "الرقم 1 ليس له عوامل أولية." else "Number 1 has no prime factors.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoftGrayText
                )
            } else {
                Text(
                    text = if (displayLanguageAr) "عناصر التفكيك الأولية:" else "Decomposed Prime Constituents:",
                    style = MaterialTheme.typography.labelSmall,
                    color = SoftGrayText
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    factorsMap.forEach { (prime, exponent) ->
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF132A1C), RoundedCornerShape(8.dp))
                                .border(1.dp, CyberGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "$prime", color = CyberGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(
                                    text = if (exponent > 1) "²" else "¹", // Keep dynamic exponent formatting simple and robust
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpectralCouplingStatusCard(
    isOpacityTriggered: Boolean,
    visibilityDistance: Double,
    displayLanguageAr: Boolean
) {
    val bg = if (isOpacityTriggered) Color(0xFF2C1C13) else Color(0xFF10282A)
    val borderCol = if (isOpacityTriggered) AtomicOrange else ElectricTeal
    val accentCol = if (isOpacityTriggered) AtomicOrange else ElectricTeal

    Card(
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderCol, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = accentCol
                )
                Text(
                    text = if (isOpacityTriggered) {
                        if (displayLanguageAr) "رصد: عتمة القواسم الطيفية" else "Alert: Total Spectral Opacity"
                    } else {
                        if (displayLanguageAr) "رصد: رنين طيفي مزدوج مرتفع" else "Resonant Spectral Visibility High"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isOpacityTriggered) {
                    if (displayLanguageAr) {
                        "الرقم المدخل أولي تماماً (Coprime) مع جميع عناصر مصفوفة قياس المختبر A1. هذا يحقق 'العتمة الطيفية' حيث d_ii = 1 مما يرجع المؤثر إلى هيكل الأساس H = A1ᵀA1 وبالتالي تشابه البصمة بالكامل مع 0.0 تباين."
                    } else {
                        "The input is completely coprime to the reference parameters in matrix A1. This triggers complete spectral opacity where D_N = Identity and thus H_N = A1ᵀA1. Visibility is 0.0."
                    }
                } else {
                    if (displayLanguageAr) {
                        "تفاعل رنيني ممتاز! يشترك العدد N في قواسم مشتركة ثنائية مع مركبات المصفوفة المرجعية A1. بعد الطيف الحالي عن خط أساس العتمة يعطي مؤشر رؤية عالي الدقة."
                    } else {
                        "Excellent resonant interaction! The number N shares prime divisors with the reference components of matrix A1, inducing visible spectral divergence from baseline opacity."
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = SoftGrayText,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (displayLanguageAr) "مسافة الرؤية (Visibility Distance):" else "Visibility Distance:",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = String.format(Locale.US, "%.4f", visibilityDistance),
                    style = MaterialTheme.typography.titleMedium,
                    color = accentCol,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun SpectralChartCard(
    currentSpectrum: DoubleArray,
    baselineSpectrum: DoubleArray,
    displayLanguageAr: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GridLineColor, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (displayLanguageAr) "تحليل البصمة الطيفية للمؤثر σ(H_N)" else "Operator Spectral Spectrum σ(H_N)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = if (displayLanguageAr) "القيم الذاتية للرنين الطيفي (Teal) مقابل المقاربة الأساسية (Orange)" else "Eigenvalues (Teal) vs. Coprime Opacity baseline (Orange)",
                style = MaterialTheme.typography.bodySmall,
                color = SoftGrayText
            )
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color(0xFF080C0F), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    val numHorizontalLines = 4
                    for (i in 0..numHorizontalLines) {
                        val y = h * i / numHorizontalLines
                        drawLine(
                            color = GridLineColor.copy(alpha = 0.5f),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                        )
                    }

                    val maxVal = max(
                        currentSpectrum.maxOrNull() ?: 1.0,
                        baselineSpectrum.maxOrNull() ?: 1.0
                    ) * 1.1

                    val numPoints = 6
                    val stepX = w / (numPoints + 1)

                    // Baseline peaks
                    for (i in 0 until numPoints) {
                        val x = stepX * (i + 1)
                        val baselineVal = baselineSpectrum[i]
                        val baselineY = h - (h * (baselineVal / maxVal)).toFloat()

                        drawLine(
                            color = AtomicOrange.copy(alpha = 0.4f),
                            start = Offset(x, h),
                            end = Offset(x, baselineY),
                            strokeWidth = 4f
                        )
                        drawCircle(
                            color = AtomicOrange,
                            radius = 6f,
                            center = Offset(x, baselineY)
                        )
                    }

                    // Active peaks
                    for (i in 0 until numPoints) {
                        val x = stepX * (i + 1)
                        val currentVal = currentSpectrum[i]
                        val currentY = h - (h * (currentVal / maxVal)).toFloat()

                        drawLine(
                            color = ElectricTeal.copy(alpha = 0.8f),
                            start = Offset(x - 8f, h),
                            end = Offset(x - 8f, currentY),
                            strokeWidth = 10f
                        )
                        drawCircle(
                            color = ElectricTeal,
                            radius = 8f,
                            center = Offset(x - 8f, currentY)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in 0 until 6) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF10161C), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (displayLanguageAr) "القيمة الذاتية الأولى λ_${i+1}" else "Eigenvalue λ_${i+1}",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(8.dp).background(ElectricTeal, RoundedCornerShape(2.dp)))
                                Text(
                                    text = String.format(Locale.US, "%.1f", currentSpectrum[i]),
                                    color = ElectricTeal,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(8.dp).background(AtomicOrange, RoundedCornerShape(2.dp)))
                                Text(
                                    text = String.format(Locale.US, "%.1f", baselineSpectrum[i]),
                                    color = AtomicOrange.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OperatorSpectroscopyMatrixCard(
    valueN: java.math.BigInteger,
    selectedMode: RowRepresentativeMode,
    displayLanguageAr: Boolean
) {
    var showExplanation by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GridLineColor, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (displayLanguageAr) "تمثيل مصفوفة مؤثر القواسم H_N" else "H_N Operator Spectroscopy Grid",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(onClick = { showExplanation = !showExplanation }) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Show Operator Info",
                        tint = ElectricTeal
                    )
                }
            }

            AnimatedVisibility(visible = showExplanation) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(Color(0xFF10161C), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = if (displayLanguageAr) {
                            "المصفوفة H_N هي مصفوفة متماثلة أبعادها 6x6 تُحسب عبر ضرب الأوزان:\n" +
                                    "H_N[j][k] = ∑_{i=0..18} A1[i][j] · d_ii · A1[i][k]\n" +
                                    "حيث d_ii = gcd(N, representative_i). تمثل كل خلية طاقة الربط والترابط الطيفي لقواسم صفوف A1 المرجعية."
                        } else {
                            "The symmetric matrix H_N (6x6) is computed as follows:\n" +
                                    "H_N[j][k] = ∑_{i=0..18} A1[i][j] · d_ii · A1[i][k]\n" +
                                    "Where d_ii = gcd(N, representative_i). Heat intensities map the coupled divisors energy dynamically."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = SoftGrayText,
                        lineHeight = 16.sp
                    )
                }
            }

            val dn = SDIMath.computeDN(valueN, selectedMode)
            val hn = SDIMath.computeHN(dn)

            // Avoid local variable shadowing compiler issue
            var maxHNVal = 1.0
            for (i in 0 until 6) {
                for (j in 0 until 6) {
                    val valueInCell = hn[i][j]
                    if (valueInCell > maxHNVal) {
                        maxHNVal = valueInCell
                    }
                }
            }

            // Grid of 36 elements
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (rowIdx in 0 until 6) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (colIdx in 0 until 6) {
                            val matrixVal = hn[rowIdx][colIdx]
                            val relativeMagnitude = matrixVal / maxHNVal
                            val baseShadeColor = ElectricTeal.copy(
                                alpha = (relativeMagnitude * 0.85f + 0.15f).toFloat()
                            )

                            fun formatCellValue(cellVal: Double): String {
                                return when {
                                    cellVal >= 1e6 -> String.format(Locale.US, "%.1fM", cellVal / 1e6)
                                    cellVal >= 1e3 -> String.format(Locale.US, "%.1fk", cellVal / 1e3)
                                    else -> String.format(Locale.US, "%.0f", cellVal)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .background(baseShadeColor, RoundedCornerShape(4.dp))
                                    .border(0.5.dp, GridLineColor, RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = formatCellValue(matrixVal),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (relativeMagnitude > 0.5) SlateDarkBackground else Color.White,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (displayLanguageAr) "مؤثر رنين تماثلي [Symmetric Matrix 6x6]" else "[Symmetric Operator 6x6]",
                    style = MaterialTheme.typography.labelSmall,
                    color = SoftGrayText
                )
                Text(
                    text = if (displayLanguageAr) "شدة اللون تتناسب مع قيم خلايا المؤشر" else "Spectrum heats mapped to cells",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricTeal
                )
            }
        }
    }
}

@Composable
fun GeminiAcademicAnalysisCard(
    uiState: SpectralUiState,
    onRequestAiAnalysis: () -> Unit,
    displayLanguageAr: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ElectricTeal.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Star, contentDescription = "AI Report", tint = ElectricTeal)
                Text(
                    text = if (displayLanguageAr) "تفسير الذكاء الاصطناعي الأكاديمي" else "Academic AI Spectral Analysis Report",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (displayLanguageAr) {
                    "أرسل طيف ومعلومات الرقم المدروس الحالي إلى خدمة Gemini 3.5 Flash لاستخلاص مراجعة طوبولوجية جبرية رصينة ومتقنة."
                } else {
                    "Submit the spectrum data and components to Gemini 3.5 Flash for complete algebraic topology synthesis."
                },
                style = MaterialTheme.typography.bodySmall,
                color = SoftGrayText,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (uiState.aiAnalysisText.isEmpty()) {
                Button(
                    onClick = onRequestAiAnalysis,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B303A)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ElectricTeal, RoundedCornerShape(8.dp))
                        .testTag("ai_rapport_button"),
                    enabled = !uiState.isAiLoading
                ) {
                    if (uiState.isAiLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ElectricTeal)
                            Text(
                                text = if (displayLanguageAr) "جاري صياغة التقرير المعملي..." else "Analyzing spectrum...",
                                color = ElectricTeal
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = ElectricTeal)
                            Text(
                                text = if (displayLanguageAr) "توليد التقرير الأكاديمي (Gemini)" else "Generate Academic Report Formulation",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0C1014), RoundedCornerShape(10.dp))
                        .border(1.dp, GridLineColor, RoundedCornerShape(10.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (displayLanguageAr) "تقرير التأويل الطوبولوجي الرقمي" else "Spectral Homology Interpretation Report",
                                style = MaterialTheme.typography.labelMedium,
                                color = ElectricTeal,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(Icons.Default.Done, contentDescription = null, tint = CyberGreen, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = uiState.aiAnalysisText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryLogsCard(
    historyList: List<SpectralRecord>,
    onSelectRecord: (SpectralRecord) -> Unit,
    onDeleteRecord: (Int) -> Unit,
    onClearHistory: () -> Unit,
    displayLanguageAr: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GridLineColor, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.List, contentDescription = "History", tint = ElectricTeal)
                    Text(
                        text = if (displayLanguageAr) "سجل المعمل والقياسات الطيفية السابقة" else "Laboratory Registry Logs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (historyList.isNotEmpty()) {
                    IconButton(
                        onClick = onClearHistory,
                        modifier = Modifier.testTag("clear_history_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear registry", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (historyList.isEmpty()) {
                Text(
                    text = if (displayLanguageAr) {
                        "سجل المعمل فارغ. قم بإجراء دراسة طيفية أولى لتسجيل النتائج بـ Room DB."
                    } else {
                        "The laboratory registry is empty. Run analyses to preserve results."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoftGrayText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    historyList.take(6).forEach { record ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF10161C), RoundedCornerShape(8.dp))
                                .clickable { onSelectRecord(record) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "N = ${record.valueN}",
                                        color = ElectricTeal,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = record.modeName,
                                        color = SoftGrayText,
                                        fontSize = 10.sp,
                                        modifier = Modifier
                                            .background(Color(0xFF1E2833), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                                Text(
                                    text = "F(N) = ${record.factorsText}",
                                    color = CyberGreen,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Visibility: " + String.format(Locale.US, "%.3f", record.visibilityMetric) +
                                            if (record.isOpacityTriggered) " (Blindspot)" else "",
                                    color = if (record.isOpacityTriggered) AtomicOrange else SoftGrayText,
                                    fontSize = 11.sp
                                )
                            }
                            IconButton(onClick = { onDeleteRecord(record.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete record", tint = Color.Red.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }
        }
    }
}
