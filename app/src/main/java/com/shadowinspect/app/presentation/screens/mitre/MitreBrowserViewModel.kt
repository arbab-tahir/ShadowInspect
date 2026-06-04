package com.shadowinspect.app.presentation.screens.mitre

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.shadowinspect.app.domain.mitre.MitreJsonParser
import com.shadowinspect.app.domain.mitre.MitreTechnique
import com.shadowinspect.app.domain.mitre.ResearchDataCollector
import com.shadowinspect.app.domain.mitre.ResearchDataset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.shadowinspect.app.domain.repository.ResearchRepository
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MitreBrowserViewModel @Inject constructor(
    private val mitreParser: MitreJsonParser,
    private val researchCollector: ResearchDataCollector,
    private val researchRepository: ResearchRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MitreBrowserState())
    val uiState: StateFlow<MitreBrowserState> = _uiState.asStateFlow()
    
    private var allTechniques: List<MitreTechnique> = emptyList()
    
    init {
        loadTechniques()
        loadResearchStats()
    }
    
    fun loadResearchStats() {
        viewModelScope.launch {
            val count = researchCollector.getResearchCount()
            _uiState.update { it.copy(researchCount = count) }
        }
    }
    
    fun loadTechniques() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                allTechniques = mitreParser.loadMitreData()
                val tactics = allTechniques.flatMap { it.tactics }.distinct().sorted()
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    filteredTechniques = allTechniques,
                    tactics = tactics
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    fun searchTechniques(query: String) {
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(
                filteredTechniques = allTechniques
            )
            return
        }
        
        val lowerQuery = query.lowercase()
        val filtered = allTechniques.filter { technique ->
            technique.id.lowercase().contains(lowerQuery) ||
            technique.name.lowercase().contains(lowerQuery) ||
            technique.description.lowercase().contains(lowerQuery) ||
            technique.tactics.any { it.lowercase().contains(lowerQuery) }
        }
        
        _uiState.value = _uiState.value.copy(
            filteredTechniques = filtered
        )
    }
    
    fun filterByTactic(tactic: String) {
        val filtered = allTechniques.filter { technique ->
            technique.tactics.contains(tactic)
        }
        
        _uiState.value = _uiState.value.copy(
            filteredTechniques = filtered
        )
    }
    
    fun loadAllTechniques() {
        _uiState.value = _uiState.value.copy(
            filteredTechniques = allTechniques
        )
    }

    fun exportResearchData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = "Exporting data...") }
            try {
                val file = researchRepository.exportResearchData()
                _uiState.update { it.copy(
                    isLoading = false,
                    exportedFile = file,
                    statusMessage = if (file != null) "Export complete" else "Export failed"
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Export failed: ${e.message}") }
            }
        }
    }

    fun clearExportedFile() {
        _uiState.update { it.copy(exportedFile = null) }
    }

    fun clearResearchData() {
        viewModelScope.launch {
            try {
                researchCollector.clearResearchData()
                _uiState.update { it.copy(
                    researchCount = 0,
                    statusMessage = "All research data cleared"
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to clear data: ${e.message}") }
            }
        }
    }
    
    data class MitreBrowserState(
        val isLoading: Boolean = false,
        val filteredTechniques: List<MitreTechnique> = emptyList(),
        val tactics: List<String> = emptyList(),
        val researchCount: Int = 0,
        val statusMessage: String? = null,
        val exportedFile: File? = null,
        val error: String? = null
    )
}
