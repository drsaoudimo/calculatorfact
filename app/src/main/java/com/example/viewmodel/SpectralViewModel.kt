package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiClient
import com.example.db.AppDatabase
import com.example.db.SpectralRecord
import com.example.db.SpectralRepository
import com.example.math.MepaResults
import com.example.math.SDIMath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.math.BigInteger

enum class RowRepresentativeMode {
    EIGEN_COSINE,
    GCD_MODULO,
    HOMOLOGICAL_BETTI
}

data class SpectralUiState(
    val inputN: String = "909249334023",
    val isLoading: Boolean = false,
    val factors: Map<BigInteger, Int> = emptyMap(),
    val factorsText: String = "",
    val mepaResults: MepaResults? = null,
    val selectedMode: RowRepresentativeMode = RowRepresentativeMode.EIGEN_COSINE,
    val academicReport: String = "",
    val error: String? = null
)

class SpectralViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SpectralRepository
    private val _uiState = MutableStateFlow(SpectralUiState())
    val uiState: StateFlow<SpectralUiState> = _uiState.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = SpectralRepository(database.spectralDao())
    }

    // Connect to Room Database Flow
    val historyLog: StateFlow<List<SpectralRecord>> = repository.allRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateInput(newVal: String) {
        _uiState.update { it.copy(inputN = newVal, error = null) }
    }

    fun updateMode(mode: RowRepresentativeMode) {
        _uiState.update { it.copy(selectedMode = mode) }
    }

    fun deleteHistoryRecord(record: SpectralRecord) {
        viewModelScope.launch {
            repository.delete(record)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    /**
     * Executes spectral analysis and recursively breaks down composite factors using AI.
     */
    fun executeSpectralAnalysis(inputStr: String) {
        viewModelScope.launch {
            val sanitized = sanitizeInput(inputStr)
            if (sanitized.isEmpty()) {
                _uiState.update { it.copy(error = "الرجاء إدخال رقم صحيح صالح للتحليل.") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = null, academicReport = "") }

            try {
                val nVal = BigInteger(sanitized)
                if (nVal <= BigInteger.ONE) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "العدد يجب أن يكون أكبر من 1 للتحليل الطيفي."
                        )
                    }
                    return@launch
                }

                // Move off-thread to Dispatchers.Default for ultimate responsiveness and to prevent any freeze/ANR
                val (factors, mepa) = withContext(Dispatchers.Default) {
                    var currentFactors = SDIMath.factorise(nVal)
                    var hasComposite = currentFactors.keys.any { !it.isProbablePrime(30) }

                    while (hasComposite) {
                        val compositeFactor = currentFactors.keys.first { !it.isProbablePrime(30) }
                        
                        var divisor: BigInteger? = SDIMath.qmfgSpectralSplit(compositeFactor)
                        
                        if (divisor == null) {
                            divisor = SDIMath.fermatSplit(compositeFactor)
                        }
                        if (divisor == null) {
                            divisor = SDIMath.pollardRhoBrentSplit(compositeFactor)
                        }
                        if (divisor == null) {
                            divisor = SDIMath.ecmSplit(compositeFactor)
                        }
                        if (divisor == null) {
                            divisor = SDIMath.pollardRhoSplit(compositeFactor)
                        }

                        if (divisor == null) {
                            var aiResolved = GeminiClient.factorizeWithGemini(compositeFactor)
                            if (aiResolved != null && aiResolved.first.multiply(aiResolved.second) == compositeFactor && aiResolved.first > BigInteger.ONE && aiResolved.second > BigInteger.ONE) {
                                divisor = aiResolved.first
                            } else {
                                for (retry in 1..2) {
                                    aiResolved = GeminiClient.factorizeWithGemini(compositeFactor)
                                    if (aiResolved != null && aiResolved.first.multiply(aiResolved.second) == compositeFactor && aiResolved.first > BigInteger.ONE && aiResolved.second > BigInteger.ONE) {
                                        divisor = aiResolved.first
                                        break
                                    }
                                }
                            }
                        }

                        if (divisor == null) {
                            divisor = SDIMath.deepPollardRhoSplit(compositeFactor)
                        }
                        if (divisor == null) {
                            divisor = SDIMath.deepTrialDivisionSplit(compositeFactor)
                        }

                        if (divisor != null && divisor != BigInteger.ONE && divisor != compositeFactor) {
                            val p1 = divisor
                            val p2 = compositeFactor.divide(divisor)
                            SDIMath.registerDynamicFactorization(compositeFactor, p1, p2)
                            currentFactors = SDIMath.factorise(nVal)
                        } else {
                            var fallbackDivisor: BigInteger? = null
                            var testD = BigInteger.valueOf(3L)
                            var countSteps = 0
                            while (testD * testD <= compositeFactor && countSteps < 50000) {
                                if (compositeFactor.remainder(testD) == BigInteger.ZERO) {
                                    fallbackDivisor = testD
                                    break
                                }
                                testD = testD.add(SDIMath.TWO)
                                countSteps++
                            }
                            if (fallbackDivisor != null) {
                                SDIMath.registerDynamicFactorization(compositeFactor, fallbackDivisor, compositeFactor.divide(fallbackDivisor))
                                currentFactors = SDIMath.factorise(nVal)
                            } else {
                                break
                            }
                        }
                        hasComposite = currentFactors.keys.any { !it.isProbablePrime(30) }
                    }
                    val matrixMEPA = SDIMath.computeMepaDiagnostics(currentFactors)
                    Pair(currentFactors, matrixMEPA)
                }

                val factorsStr = SDIMath.factorsToString(factors)

                // 4. Generate scholarly Homology / TDA analysis from Gemini
                val academicReport = GeminiClient.generateAcademicAnalysis(
                    valueN = nVal,
                    factorsText = factorsStr,
                    determinant = mepa.relationMatrix.firstOrNull()?.sum() ?: 0.0,
                    frobeniusNorm = mepa.frobeniusNorm,
                    b0 = mepa.betti0,
                    b1 = mepa.betti1,
                    b2 = mepa.betti2
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        factors = factors,
                        factorsText = factorsStr,
                        mepaResults = mepa,
                        academicReport = academicReport
                    )
                }

                // Save to Room for offline sandbox tracking
                repository.insert(
                    SpectralRecord(
                        inputN = nVal.toString(),
                        factorsText = factorsStr,
                        frobeniusNorm = mepa.frobeniusNorm,
                        dimension = mepa.dimension,
                        betti0 = mepa.betti0,
                        betti1 = mepa.betti1,
                        betti2 = mepa.betti2,
                        academicBriefing = academicReport
                    )
                )

            } catch (e: NumberFormatException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "عذراً، المدخلات ليست رقماً صالحاً ذو قيم صحيحة للتحليل."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "حدث خطأ غير متوقع: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    /**
     * Continuous mathematical and AI analysis to decompose discovered composite elements:
     * - Runs Fermat check
     * - Runs Pollard's Rho Brent cycle-detection
     * - Runs Lenstra ECM curve multiplication
     * - Calls Gemini API as an intelligent spectral solver fallback
     * - Updates the factorization bounds and cascades to update MEPA diagnostics & Room record.
     */
    fun decomposeCompositeFactor(composite: BigInteger) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val inputNVal = BigInteger(sanitizeInput(_uiState.value.inputN))
                
                // Offload sub-tree factorization to background thread as well
                val solvedAny = withContext(Dispatchers.Default) {
                    val tempCompositeList = mutableListOf(composite)
                    var solved = false
                    
                    while (tempCompositeList.isNotEmpty()) {
                        val currentComp = tempCompositeList.removeAt(0)
                        if (currentComp.isProbablePrime(30)) continue
                        
                        var divisor: BigInteger? = SDIMath.qmfgSpectralSplit(currentComp)
                        if (divisor == null) {
                            divisor = SDIMath.fermatSplit(currentComp)
                        }
                        if (divisor == null) {
                            divisor = SDIMath.pollardRhoBrentSplit(currentComp)
                        }
                        if (divisor == null) {
                            divisor = SDIMath.ecmSplit(currentComp)
                        }
                        if (divisor == null) {
                            divisor = SDIMath.pollardRhoSplit(currentComp)
                        }
                        
                        if (divisor == null) {
                            // AI Fallback with retries
                            var aiResolved = GeminiClient.factorizeWithGemini(currentComp)
                            if (aiResolved != null && aiResolved.first.multiply(aiResolved.second) == currentComp && aiResolved.first > BigInteger.ONE && aiResolved.second > BigInteger.ONE) {
                                divisor = aiResolved.first
                            } else {
                                for (retry in 1..2) {
                                    aiResolved = GeminiClient.factorizeWithGemini(currentComp)
                                    if (aiResolved != null && aiResolved.first.multiply(aiResolved.second) == currentComp && aiResolved.first > BigInteger.ONE && aiResolved.second > BigInteger.ONE) {
                                        divisor = aiResolved.first
                                        break
                                    }
                                }
                            }
                        }

                        if (divisor == null) {
                            divisor = SDIMath.deepPollardRhoSplit(currentComp)
                        }
                        if (divisor == null) {
                            divisor = SDIMath.deepTrialDivisionSplit(currentComp)
                        }
                        
                        if (divisor != null && divisor != BigInteger.ONE && divisor != currentComp) {
                            val p1 = divisor
                            val p2 = currentComp.divide(divisor)
                            SDIMath.registerDynamicFactorization(currentComp, p1, p2)
                            solved = true
                            tempCompositeList.add(p1)
                            tempCompositeList.add(p2)
                        } else {
                            // Exhaustive brute-force odd search fallback
                            var fallbackDivisor: BigInteger? = null
                            var testD = BigInteger.valueOf(3L)
                            var testCount = 0
                            while (testD * testD <= currentComp && testCount < 50000) {
                                if (currentComp.remainder(testD) == BigInteger.ZERO) {
                                    fallbackDivisor = testD
                                    break
                                }
                                testD = testD.add(SDIMath.TWO)
                                testCount++
                            }
                            if (fallbackDivisor != null) {
                                val p1 = fallbackDivisor
                                val p2 = currentComp.divide(fallbackDivisor)
                                SDIMath.registerDynamicFactorization(currentComp, p1, p2)
                                solved = true
                                tempCompositeList.add(p1)
                                tempCompositeList.add(p2)
                            }
                        }
                    }
                    solved
                }
                
                if (solvedAny) {
                    val (updatedFactors, mepa) = withContext(Dispatchers.Default) {
                        val currentF = SDIMath.factorise(inputNVal)
                        val computedMepa = SDIMath.computeMepaDiagnostics(currentF)
                        Pair(currentF, computedMepa)
                    }
                    val factorsStr = SDIMath.factorsToString(updatedFactors)
                    
                    val academicReport = GeminiClient.generateAcademicAnalysis(
                        valueN = inputNVal,
                        factorsText = factorsStr,
                        determinant = mepa.relationMatrix.firstOrNull()?.sum() ?: 0.0,
                        frobeniusNorm = mepa.frobeniusNorm,
                        b0 = mepa.betti0,
                        b1 = mepa.betti1,
                        b2 = mepa.betti2
                    )

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            factors = updatedFactors,
                            factorsText = factorsStr,
                            mepaResults = mepa,
                            academicReport = academicReport
                        )
                    }
                    
                    // Save update to offline record db
                    repository.insert(
                        SpectralRecord(
                            inputN = inputNVal.toString(),
                            factorsText = factorsStr,
                            frobeniusNorm = mepa.frobeniusNorm,
                            dimension = mepa.dimension,
                            betti0 = mepa.betti0,
                            betti1 = mepa.betti1,
                            betti2 = mepa.betti2,
                            academicBriefing = academicReport
                        )
                    )
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "تعذر تفكيك الحد $composite بالخوارزميات المدمجة حالياً. جرب مع مقادير أخرى كالمطابقة اليدوية."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "خطأ في تفكيك الحد المركب: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    /**
     * Custom manual register of factors for manual overrides or matching values.
     */
    fun registerManualFactors(nStr: String, p1Str: String, p2Str: String) {
        val sanitizedN = sanitizeInput(nStr)
        val sanitizedP1 = sanitizeInput(p1Str)
        val sanitizedP2 = sanitizeInput(p2Str)
        try {
            val nVal = BigInteger(sanitizedN)
            val p1Val = BigInteger(sanitizedP1)
            val p2Val = BigInteger(sanitizedP2)
            
            if (p1Val.multiply(p2Val) == nVal) {
                SDIMath.registerDynamicFactorization(nVal, p1Val, p2Val)
                _uiState.update { it.copy(inputN = nStr) }
                executeSpectralAnalysis(nStr)
            } else {
                _uiState.update { it.copy(error = "تنبيه: حاصل ضرب العاملين لا يساوي العدد الهدف N!") }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "الرجاء إدخال أرقام صحيحة صالحة للمطابقة") }
        }
    }

    private fun sanitizeInput(input: String): String {
        var sanitized = input.replace("[,\\s_\\.\\-\\(\\)\\[\\]\\u200E\\u200F\\u202A-\\u202E]".toRegex(), "")
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        for (i in 0..9) {
            sanitized = sanitized.replace(arabicDigits[i], '0' + i)
        }
        return sanitized.trim()
    }
}
