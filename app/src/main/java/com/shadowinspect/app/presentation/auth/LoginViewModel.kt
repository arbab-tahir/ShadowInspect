package com.shadowinspect.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shadowinspect.app.data.auth.AgentAuthRepository
import com.shadowinspect.app.data.auth.AuthResult
import com.shadowinspect.app.data.session.AgentSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val agentHandle: String = "",
    val accessKey: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AgentAuthRepository,
    private val sessionRepository: AgentSessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onAgentHandleChange(value: String) {
        _uiState.update { it.copy(agentHandle = value, error = null) }
    }

    fun onAccessKeyChange(value: String) {
        _uiState.update { it.copy(accessKey = value, error = null) }
    }

    fun login(onSuccess: (String) -> Unit) {
        val agent = uiState.value.agentHandle.trim()
        val key = uiState.value.accessKey.trim()

        if (agent.isBlank() || key.isBlank()) {
            _uiState.update { it.copy(error = "Agent handle and access key required.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Ensure fresh session state before attempting new login
                sessionRepository.logout()
                
                when (val result = authRepository.login(agent, key.toCharArray())) {
                    is AuthResult.Success -> {
                        sessionRepository.login(agentHandle = result.handle)
                        _uiState.update { it.copy(isLoading = false) }
                        onSuccess(result.handle)
                    }
                    is AuthResult.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = result.message) }
                    }
                }
            } catch (e: Throwable) {
                // Catching Throwable instead of Exception to capture low-level JVM errors/crashes
                val errorMsg = e.message ?: e.javaClass.simpleName
                _uiState.update { it.copy(isLoading = false, error = "CRITICAL ERROR: $errorMsg") }
                android.util.Log.e("LoginViewModel", "Critical failure during login flow", e)
            }
        }
    }
}
