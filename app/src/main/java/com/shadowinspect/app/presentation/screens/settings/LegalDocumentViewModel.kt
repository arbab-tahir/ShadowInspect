package com.shadowinspect.app.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shadowinspect.app.data.legal.LegalDocuments
import com.shadowinspect.app.domain.settings.LegalDocument
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LegalDocumentViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(LegalDocumentUiState())
    val uiState: StateFlow<LegalDocumentUiState> = _uiState.asStateFlow()

    fun loadDocument(type: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val document = when (type.lowercase()) {
                "terms" -> LegalDocuments.termsOfService
                "privacy" -> LegalDocuments.privacyPolicy
                "gdpr" -> LegalDocuments.gdprCompliance
                "disclaimer" -> LegalDocuments.disclaimer
                "licenses" -> LegalDocuments.openSourceLicenses
                "copyright" -> LegalDocuments.copyright
                "mitre" -> LegalDocuments.mitreAttribution
                else -> null
            }
            
            _uiState.value = _uiState.value.copy(
                document = document,
                isLoading = false
            )
        }
    }

    data class LegalDocumentUiState(
        val document: LegalDocument? = null,
        val isLoading: Boolean = false
    )
}
