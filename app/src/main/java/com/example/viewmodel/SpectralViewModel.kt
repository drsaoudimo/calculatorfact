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

                // 1. Initial High-speed prime factorization
                var factors = SDIMath.factorise(nVal)

                // 2. Continuous Decomposer for non-prime limits / composite factors ("عند اكتشاف حد غير اولي الرجاء الاستمرار في تحليله")
                var hasComposite = factors.keys.any { !it.isProbablePrime(25) }
                var aiDecomposeAttempts = 0
                val maxAttempts = 5 // safety cap

                while (hasComposite && aiDecomposeAttempts < maxAttempts) {
                    aiDecomposeAttempts++
                    // Identify the non-prime factor
                    val compositeFactor = factors.keys.first { !it.isProbablePrime(25) }
                    
                    // Utilize Gemini to break down this composite sub-factor
                    val aiResolvedFactors = GeminiClient.factorizeWithGemini(compositeFactor)
                    if (aiResolvedFactors != null) {
                        val (p1, p2) = aiResolvedFactors
                        SDIMath.registerDynamicFactorization(compositeFactor, p1, p2)
                        
                        // Recalculate whole product factors with newly discovered sub-factors
                        factors = SDIMath.factorise(nVal)
                    } else {
                        // AI could not break this limit down; log error or break retry loop
                        break
                    }
                    hasComposite = factors.keys.any { !it.isProbablePrime(25) }
                }

                val factorsStr = SDIMath.factorsToString(factors)

                // 3. Compute diagnostic energetic matrices
                val mepa = SDIMath.computeMepaDiagnostics(factors)

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
