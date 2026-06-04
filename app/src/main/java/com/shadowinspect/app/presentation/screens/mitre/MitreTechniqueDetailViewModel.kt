package com.shadowinspect.app.presentation.screens.mitre

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shadowinspect.app.domain.mitre.MitreJsonParser
import com.shadowinspect.app.domain.mitre.MitreTechnique
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MitreTechniqueDetailViewModel @Inject constructor(
    private val mitreParser: MitreJsonParser
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MitreTechniqueDetailState())
    val uiState: StateFlow<MitreTechniqueDetailState> = _uiState.asStateFlow()
    
    fun loadTechnique(techniqueId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val technique = mitreParser.getTechniqueById(techniqueId)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    technique = technique
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    data class MitreTechniqueDetailState(
        val isLoading: Boolean = false,
        val technique: MitreTechnique? = null,
        val error: String? = null
    )
}
