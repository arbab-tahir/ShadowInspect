package com.shadowinspect.app.presentation.screens.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shadowinspect.app.domain.ml.ApkFeatureVector
import com.shadowinspect.app.domain.ml.MLModelManager
import com.shadowinspect.app.domain.ml.MLModelMetadata
import com.shadowinspect.app.domain.ml.ModelPerformance
import com.shadowinspect.app.domain.ml.ModelType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MLSettingsViewModel @Inject constructor(
    private val mlModelManager: MLModelManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MLSettingsUiState())
    val uiState: StateFlow<MLSettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadModelState()
    }
    
    private fun loadModelState() {
        viewModelScope.launch {
            mlModelManager.modelState.collect { state ->
                _uiState.value = _uiState.value.copy(
                    isInitialized = state.isInitialized,
                    isInitializing = state.isInitializing,
                    activeModel = state.activeModel,
                    availableModels = state.availableModels,
                    error = state.error,
                    performance = mlModelManager.getModelPerformance()
                )
            }
        }
    }
    
    fun selectModel(modelType: ModelType) {
        mlModelManager.selectModel(modelType)
    }
    
    fun reinitializeModels() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isInitializing = true, error = null)
            
            val success = mlModelManager.initializeModels()
            
            _uiState.value = _uiState.value.copy(
                isInitializing = false,
                isInitialized = success,
                error = if (success) "Models initialized successfully ✅" else "Initialization failed ❌"
            )
            
            mlModelManager.debugMLStatus()
        }
    }
    
    fun testMLWithSample() {
        viewModelScope.launch {
            try {
                // Create sample features (simulating a suspicious app)
                val sampleFeatures = ApkFeatureVector(
                    permissions = FloatArray(50) { if (it < 20) 1f else 0f },
                    apiCalls = FloatArray(16) { if (it < 8) 1f else 0f },
                    opcodes = FloatArray(22) { (0..100).random().toFloat() / 100f },
                    suspiciousStrings = 15f,
                    obfuscationScore = 0.8f,
                    urlCount = 5f,
                    dangerousPermissions = 12f
                )
                
                val result = mlModelManager.predict(sampleFeatures)
                
                Log.d("MLSettings", "Test prediction: ${result.threatClass} (${result.confidence})")
                
                _uiState.value = _uiState.value.copy(
                    error = "Test: ${result.threatClass} — ${(result.confidence * 100).toInt()}% confidence (${result.inferenceTimeMs}ms)"
                )
            } catch (e: Exception) {
                Log.e("MLSettings", "Test prediction failed", e)
                _uiState.value = _uiState.value.copy(
                    error = "Test failed: ${e.message}"
                )
            }
        }
    }
    
    data class MLSettingsUiState(
        val isInitialized: Boolean = false,
        val isInitializing: Boolean = false,
        val activeModel: ModelType? = null,
        val availableModels: List<MLModelMetadata> = emptyList(),
        val performance: Map<ModelType, ModelPerformance> = emptyMap(),
        val error: String? = null
    )
}
