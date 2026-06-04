package com.shadowinspect.app.presentation.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Patterns
import com.shadowinspect.app.data.network.UrlScanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UrlScanViewModel @Inject constructor(
    private val repository: UrlScanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UrlScanState())
    val uiState: StateFlow<UrlScanState> = _uiState.asStateFlow()

    fun onUrlInputChanged(newUrl: String) {
        _uiState.update { it.copy(urlInput = newUrl, error = null) }
    }

    fun validateUrl(url: String): Boolean {
        if (url.isBlank()) return false
        return Patterns.WEB_URL.matcher(url).matches()
    }

    fun clearResult() {
        _uiState.update {
            it.copy(
                urlInput = "",
                isScanning = false,
                scanResult = null,
                error = null
            )
        }
    }

    fun scanUrl() {
        val url = _uiState.value.urlInput

        if (!validateUrl(url)) {
            _uiState.update { it.copy(error = "Invalid URL format. Please enter a valid URL.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isScanning = true,
                    error = null,
                    scanResult = null
                )
            }

            val result = repository.scanUrl(url)

            result.onSuccess { scanResult ->
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        scanResult = scanResult
                    )
                }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        error = exception.message ?: "Unknown error occurred"
                    )
                }
            }
        }
    }
}
