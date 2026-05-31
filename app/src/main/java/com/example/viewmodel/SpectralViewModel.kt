package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiClient
import com.example.db.SpectralRecord
import com.example.db.SpectralRepository
import com.example.math.RowRepresentativeMode
import com.example.math.SDIMath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigInteger

data class SpectralUiState(
    val inputN: String = "220",
    val selectedMode: RowRepresentativeMode = RowRepresentativeMode.PRIMES,
    val isLoading: Boolean = false,
    val error: String? = null,
    
    // Calculated numerical outputs
    val activeN: BigInteger? = null,
    val primeFactors: Map<BigInteger, Int> = emptyMap(),
    val factorsText: String = "",
    val currentSpectrum: DoubleArray = DoubleArray(6),
    val baselineSpectrum: DoubleArray = DoubleArray(6),
    val visibilityMetric: Double = 0.0,
    val isOpacityTriggered: Boolean = false,
    val semiprimeAnalysis: SDIMath.SemiprimeAnalysis? = null,
    
    // AI Explanations
    val isAiLoading: Boolean = false,
    val aiExplanation: String = "",
    
    // Local persistence history
    val inspectionHistory: List<SpectralRecord> = emptyList()
)

class SpectralViewModel(private val repository: SpectralRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SpectralUiState())
    val uiState: StateFlow<SpectralUiState> = _uiState.asStateFlow()

    init {
        // Collect local history reactively from repository
        viewModelScope.launch {
            repository.allRecords.collect { records ->
                _uiState.update { it.copy(inspectionHistory = records) }
            }
        }
        
        // Initial run with default N
        calculateSpectralAttributes(_uiState.value.inputN)
    }

    fun updateInput(newInput: String) {
        _uiState.update { it.copy(inputN = newInput, error = null) }
    }

    fun updateMode(mode: RowRepresentativeMode) {
        _uiState.update { it.copy(selectedMode = mode) }
        calculateSpectralAttributes(_uiState.value.inputN)
    }

    fun executeSpectralAnalysis(customInput: String? = null) {
        if (customInput != null) {
            _uiState.update { it.copy(inputN = customInput) }
        }
        calculateSpectralAttributes(_uiState.value.inputN)
    }

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
                calculateSpectralAttributes(nStr)
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
        return sanitized
    }

    private fun calculateSpectralAttributes(inputString: String) {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val sanitized = sanitizeInput(inputString)
            val nVal = try {
                BigInteger(sanitized)
            } catch (e: Exception) {
                null
            }
            
            if (nVal == null || nVal <= BigInteger.ZERO) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = "الرجاء إدخال عدد طبيعي صحيح موجب أكبر من الصفر"
                    ) 
                }
                return@launch
            }

            // High-speed prime factorization
            var factors = SDIMath.factorise(nVal)

            // Dynamic AI Factorization for extremely large composites / semiprimes
            val isCompositeButUnfactored = nVal.isProbablePrime(25) == false && (factors.size == 1 && factors.containsKey(nVal))
            if (isCompositeButUnfactored) {
                val aiFactors = com.example.api.GeminiClient.factorizeWithGemini(nVal)
                if (aiFactors != null) {
                    SDIMath.registerDynamicFactorization(nVal, aiFactors.first, aiFactors.second)
                    factors = SDIMath.factorise(nVal)
                }
            }

            val factorsStr = SDIMath.factorsToString(factors)

            // Diagnostic matrices computation
            val mode = _uiState.value.selectedMode
            val dn = SDIMath.computeDN(nVal, mode)
            val hn = SDIMath.computeHN(dn)
            val spectrum = SDIMath.computeEigenvalues(hn)

            // Baseline matrix (coprime / fully opaque N reference)
            val baseDn = DoubleArray(19) { 1.0 }
            val baseHn = SDIMath.computeHN(baseDn)
            val baselineSpectrum = SDIMath.computeEigenvalues(baseHn)

            // Visibility Metric and complete Opacity boundary triggers
            val visibility = SDIMath.computeSpectralVisibility(spectrum, baselineSpectrum)
            val isOpacityTriggered = SDIMath.isSpectralOpacityTriggered(nVal)

            // Robust math solver representation for semiprimes/composites
            val semiprime = SDIMath.analyzeSemiprime(nVal, mode)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    activeN = nVal,
                    primeFactors = factors,
                    factorsText = factorsStr,
                    currentSpectrum = spectrum,
                    baselineSpectrum = baselineSpectrum,
                    visibilityMetric = visibility,
                    isOpacityTriggered = isOpacityTriggered,
                    semiprimeAnalysis = semiprime,
                    aiExplanation = "" // Reset explanation when new N is analyzed
                )
            }

            // Save trace records safely inside database
            val record = SpectralRecord(
                valueN = nVal.toString(),
                factorsText = factorsStr,
                spectrumText = spectrum.joinToString(",") { String.format("%.2f", it) },
                visibilityMetric = visibility,
                isOpacityTriggered = isOpacityTriggered
            )
            repository.insertRecord(record)
        }
    }

    fun requestAiInsight() {
        val state = _uiState.value
        val activeNVal = state.activeN ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isAiLoading = true) }
            
            val summaryText = GeminiClient.generateAcademicAnalysis(
                valueN = activeNVal,
                factorsText = state.factorsText,
                spectrum = state.currentSpectrum,
                opacitySpectrum = state.baselineSpectrum,
                visibility = state.visibilityMetric,
                isOpacityTriggered = state.isOpacityTriggered,
                modeName = state.selectedMode.name
            )

            _uiState.update {
                it.copy(
                    isAiLoading = false,
                    aiExplanation = summaryText
                )
            }
        }
    }

    fun loadFromHistory(record: SpectralRecord) {
        val mode = _uiState.value.selectedMode
        _uiState.update {
            it.copy(
                inputN = record.valueN,
                selectedMode = mode,
                aiExplanation = ""
            )
        }
        calculateSpectralAttributes(record.valueN)
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }
}

class SpectralViewModelFactory(private val repository: SpectralRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SpectralViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SpectralViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
