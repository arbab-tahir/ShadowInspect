package com.shadowinspect.app.presentation.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shadowinspect.app.domain.repository.ResearchRepository
import com.shadowinspect.app.domain.research.ResearchStatistics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ResearchExportState(
    val isLoading: Boolean = false,
    val stats: ResearchStatistics? = null,
    val exportedFile: File? = null,
    val latexTables: String? = null,
    val error: String? = null
)

@HiltViewModel
class ResearchExportViewModel @Inject constructor(
    private val researchRepository: ResearchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResearchExportState())
    val uiState: StateFlow<ResearchExportState> = _uiState.asStateFlow()

    init {
        loadStatistics()
    }

    fun loadStatistics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val stats = researchRepository.getResearchStatistics()
                _uiState.value = _uiState.value.copy(isLoading = false, stats = stats)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to load stats: ${e.message}")
            }
        }
    }

    fun exportCsv() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val file = researchRepository.exportResearchData()
                _uiState.value = _uiState.value.copy(isLoading = false, exportedFile = file)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Export failed")
            }
        }
    }

    fun generateLatex() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val latex = researchRepository.generateLatexTables()
                _uiState.value = _uiState.value.copy(isLoading = false, latexTables = latex)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "LaTeX generation failed")
            }
        }
    }

    fun clearExportedFile() {
        _uiState.value = _uiState.value.copy(exportedFile = null)
    }
}
