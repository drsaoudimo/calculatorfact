package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiClient
import com.example.db.AppDatabase
import com.example.db.SpectralRecord
import com.example.db.SpectralRepository
import com.example.math.RowRepresentativeMode
import com.example.math.SDIMath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SpectralUiState(
    val inputN: String = "2026",
    val selectedMode: RowRepresentativeMode = RowRepresentativeMode.ROW_GCD,
    val isCalculating: Boolean = false,
    val error: String? = null,
    
    // Results
    val activeN: java.math.BigInteger? = null,
    val primeFactors: Map<java.math.BigInteger, Int> = emptyMap(),
    val factorsText: String = "",
    val currentSpectrum: DoubleArray = DoubleArray(6),
    val baselineSpectrum: DoubleArray = DoubleArray(6),
    val visibilityDistance: Double = 0.0,
    val isOpacityTriggered: Boolean = false,
    
    // AI Rapport
    val aiAnalysisText: String = "",
    val isAiLoading: Boolean = false
)

class SpectralViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = SpectralRepository(database.spectralDao())

    private val _uiState = MutableStateFlow(SpectralUiState())
    val uiState: StateFlow<SpectralUiState> = _uiState.asStateFlow()

    // Expose database history to UI reactively
    val historyList: StateFlow<List<SpectralRecord>> = repository.allRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Run initial calculation for 2026
        calculateSpectralAttributes("2026")
    }

    fun updateInput(input: String) {
        _uiState.update { it.copy(inputN = input, error = null) }
    }

    fun selectMode(mode: RowRepresentativeMode) {
        _uiState.update { it.copy(selectedMode = mode) }
        _uiState.value.activeN?.let {
            calculateSpectralAttributes(it.toString())
        }
    }

    fun triggerCalculation() {
        val input = _uiState.value.inputN.trim()
        if (input.isEmpty()) {
            _uiState.update { it.copy(error = "أدخل قيمة عددية صحيحة أولاً") }
            return
        }
        calculateSpectralAttributes(input)
    }

    private fun calculateSpectralAttributes(inputString: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val nVal = try {
                java.math.BigInteger(inputString.trim())
            } catch (e: Exception) {
                null
            }
            if (nVal == null || nVal <= java.math.BigInteger.ZERO) {
                _uiState.update { it.copy(error = "الرجاء إدخال عدد طبيعي صحيح موجب أكبر من الصفر") }
                return@launch
            }

            _uiState.update { it.copy(isCalculating = true, error = null, aiAnalysisText = "") }

            try {
                // Factorization
                val factors = SDIMath.factorise(nVal)
                val factorsStr = SDIMath.factorsToString(factors)

                // Matrix and Operator calculations
                val mode = _uiState.value.selectedMode
                val dn = SDIMath.computeDN(nVal, mode)
                val hn = SDIMath.computeHN(dn)

                val spectrum = SDIMath.solveEigenvalues(hn)
                val opacitySpectrum = SDIMath.computeOpacitySpectrum(mode)
                val visibility = SDIMath.calculateVisibilityDistance(spectrum, opacitySpectrum)

                val opacityTriggered = SDIMath.isSpectralOpacityTriggered(nVal)

                _uiState.update {
                    it.copy(
                        isCalculating = false,
                        activeN = nVal,
                        primeFactors = factors,
                        factorsText = factorsStr,
                        currentSpectrum = spectrum,
                        baselineSpectrum = opacitySpectrum,
                        visibilityDistance = visibility,
                        isOpacityTriggered = opacityTriggered
                    )
                }

                // Autopersist successful scan to database
                val record = SpectralRecord(
                    valueN = nVal.toString(),
                    factorsText = factorsStr,
                    spectrumText = spectrum.joinToString(",") { String.format("%.2f", it) },
                    visibilityMetric = visibility,
                    isOpacityTriggered = opacityTriggered,
                    modeName = mode.displayNameAr
                )
                repository.insertRecord(record)

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCalculating = false,
                        error = "حدث خطأ أثناء الحسابات الطيفية: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun requestAiAnalysis() {
        val state = _uiState.value
        val activeN = state.activeN ?: return

        _uiState.update { it.copy(isAiLoading = true) }

        viewModelScope.launch {
            val analysis = GeminiClient.generateAcademicAnalysis(
                valueN = activeN,
                factorsText = state.factorsText,
                spectrum = state.currentSpectrum,
                opacitySpectrum = state.baselineSpectrum,
                visibility = state.visibilityDistance,
                isOpacityTriggered = state.isOpacityTriggered,
                modeName = state.selectedMode.displayNameAr
            )
            _uiState.update { it.copy(isAiLoading = false, aiAnalysisText = analysis) }
        }
    }

    fun deleteHistoryRecord(id: Int) {
        viewModelScope.launch {
            repository.deleteRecord(id)
        }
    }

    fun loadFromHistory(record: SpectralRecord) {
        val mode = RowRepresentativeMode.values().find { it.displayNameAr == record.modeName } 
            ?: RowRepresentativeMode.ROW_GCD

        _uiState.update {
            it.copy(
                inputN = record.valueN,
                selectedMode = mode,
                aiAnalysisText = ""
            )
        }
        calculateSpectralAttributes(record.valueN)
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
