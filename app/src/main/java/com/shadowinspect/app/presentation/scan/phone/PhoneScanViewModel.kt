package com.shadowinspect.app.presentation.scan.phone

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shadowinspect.app.domain.model.PhoneAnalysisResult
import com.shadowinspect.app.domain.phone.PhoneNumberFormatter
import com.shadowinspect.app.domain.phone.PhoneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
//  PhoneScanUiState
// ─────────────────────────────────────────────────────────────────────────────

data class PhoneScanUiState(
    val phoneInput: String = "",
    val countryCode: String = "",       // e.g. "US", "GB", "IN" (ISO-3166-1 alpha-2)
    val isScanning: Boolean = false,
    val scanResult: PhoneAnalysisResult? = null,
    val error: String? = null,
    val isValidFormat: Boolean = false,
    val showCountryPicker: Boolean = false,
    val recentScans: List<PhoneAnalysisResult> = emptyList()
)

// ─────────────────────────────────────────────────────────────────────────────
//  PhoneScanViewModel
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class PhoneScanViewModel @Inject constructor(
    private val phoneRepository: PhoneRepository,
    private val phoneNumberFormatter: PhoneNumberFormatter
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhoneScanUiState())
    val uiState: StateFlow<PhoneScanUiState> = _uiState.asStateFlow()

    private var recentScansJob: Job? = null

    init {
        loadRecentScans()
    }

    // ── 1. Input Handling ─────────────────────────────────────────────────────

    /**
     * Updates the raw phone input as the user types.
     * Also formats the input on-the-fly (e.g., adding spaces or hyphens)
     * and evaluates whether the length/characters look valid enough to scan.
     */
    fun updatePhone(input: String) {
        // Optionally, we could format the string as they type.
        // For standard UI flows, allowing free-form typing is often smoother,
        // but we'll apply the domain formatter to get an aesthetic preview string if we can.
        // However, restricting input characters to `+` and digits is safest here.
        val cleaned = input.filter { it.isDigit() || it == '+' || it == ' ' || it == '-' || it == '(' || it == ')' }

        _uiState.update { state ->
            state.copy(
                phoneInput = cleaned,
                isValidFormat = validatePhoneFormat(cleaned, state.countryCode.takeIf { it.isNotBlank() }),
                error = null // clear any previous scan errors
            )
        }
    }

    /**
     * Updates the ISO country code hint (from a UI dropdown/flag picker).
     * This helps the Formatter and Repository handle numbers entered without
     * an international prefix (e.g. "2025551234" instead of "+12025551234").
     */
    fun updateCountryCode(code: String) {
        _uiState.update { state ->
            val newCode = code.uppercase()
            state.copy(
                countryCode = newCode,
                showCountryPicker = false,
                isValidFormat = validatePhoneFormat(state.phoneInput, newCode)
            )
        }
    }

    fun toggleCountryPicker(show: Boolean) {
        _uiState.update { it.copy(showCountryPicker = show) }
    }

    // ── 2. Scanning ───────────────────────────────────────────────────────────

    /**
     * Calls the `PhoneRepository` to analyze the number, aggregating DB
     * patterns, free API quotas, and community reports.
     */
    fun scanPhone() {
        val currentState = _uiState.value
        val input = currentState.phoneInput.trim()

        if (input.isBlank() || !currentState.isValidFormat) {
            _uiState.update { it.copy(error = "Please enter a valid phone number for the selected country.") }
            return
        }

        val codeHint = currentState.countryCode.takeIf { it.isNotBlank() }

        _uiState.update {
            it.copy(
                isScanning = true,
                error = null,
                scanResult = null // clear old result
            )
        }

        viewModelScope.launch {
            try {
                val result = phoneRepository.analyzePhoneNumber(
                    number = input,
                    countryCode = codeHint
                )

                _uiState.update { state ->
                    if (!result.isError) {
                        state.copy(
                            isScanning = false,
                            scanResult = result,
                            error = null
                        )
                    } else {
                        state.copy(
                            isScanning = false,
                            scanResult = result,
                            error = result.errorMessage ?: "Analysis failed."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        isScanning = false,
                        error = e.message ?: "An unexpected error occurred during the scan."
                    )
                }
            }
        }
    }

    // ── 3. Reset ──────────────────────────────────────────────────────────────

    /**
     * Clears the current analysis screen so the user can scan another number.
     */
    fun clearResult() {
        _uiState.update {
            it.copy(
                scanResult = null,
                error = null,
                phoneInput = "",
                isValidFormat = false
            )
        }
    }

    // ── 4. History Flow ───────────────────────────────────────────────────────

    /**
     * Observes the Room database for recent 'PHONE' type scans.
     * Starts automatically in init{}.
     */
    private fun loadRecentScans() {
        viewModelScope.launch {
            try {
                recentScansJob?.cancel()
                recentScansJob = phoneRepository.getRecentPhoneScans()
                    .onEach { scans ->
                        _uiState.update { it.copy(recentScans = scans) }
                    }
                    .catch { e ->
                        Log.e("PHONE_SCAN", "Error observing history", e)
                    }
                    .launchIn(this)
            } catch (e: Exception) {
                Log.e("PHONE_SCAN", "Failed to initialize history flow", e)
            }
        }
    }

    // ── 5. Internal Validation ────────────────────────────────────────────────

    /**
     * Does a lightweight check before allowing the "Scan" button to be clicked.
     * Delegates to the domain-layer `PhoneNumberFormatter` which checks E.164
     * bounds and country-specific constraints.
     */
    private fun validatePhoneFormat(number: String, countryCode: String? = null): Boolean {
        // Formatter strips whitespace and checks bounds (7-15 digits)
        return phoneNumberFormatter.validateNumber(number, countryCode)
    }
}
