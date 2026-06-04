package com.shadowinspect.app.presentation.screens.settings

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shadowinspect.app.domain.settings.AppPermission
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecurityViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SecurityUiState())
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    init {
        loadSecurityInfo()
    }

    private fun loadSecurityInfo() {
        viewModelScope.launch {
            val permissions = listOf(
                AppPermission(
                    name = "Storage Access",
                    purpose = "Used to analyze APK files, documents, and images locally.",
                    isRequired = true,
                    isDangerous = true,
                    dataCollected = "File metadata and contents (processed locally)",
                    userControl = "Grant/Revoke in Android Settings"
                ),
                AppPermission(
                    name = "Internet",
                    purpose = "Used for optional URL safety checks and threat database updates.",
                    isRequired = true,
                    isDangerous = false,
                    dataCollected = "URL strings (only when explicitly scanned)",
                    userControl = "Managed by Android System"
                ),
                AppPermission(
                    name = "Notifications",
                    purpose = "Alerts you about scan results and background security tasks.",
                    isRequired = false,
                    isDangerous = false,
                    dataCollected = "None",
                    userControl = "Enable/Disable in App Settings"
                )
            )

            _uiState.value = SecurityUiState(
                permissions = permissions,
                encryptionStatus = "AES-256 (On-device data storage)",
                isBiometricEnabled = false // Placeholder for future feature
            )
        }
    }

    data class SecurityUiState(
        val permissions: List<AppPermission> = emptyList(),
        val encryptionStatus: String = "Unknown",
        val isBiometricEnabled: Boolean = false
    )
}
