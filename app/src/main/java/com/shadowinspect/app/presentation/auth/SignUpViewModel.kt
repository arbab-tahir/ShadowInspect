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

data class SignUpUiState(
    val agentHandle: String = "",
    val accessKey: String = "",
    val confirmAccessKey: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authRepository: AgentAuthRepository,
    private val sessionRepository: AgentSessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun onAgentHandleChange(value: String) {
        _uiState.update { it.copy(agentHandle = value, error = null) }
    }

    fun onAccessKeyChange(value: String) {
        _uiState.update { it.copy(accessKey = value, error = null) }
    }

    fun onConfirmAccessKeyChange(value: String) {
        _uiState.update { it.copy(confirmAccessKey = value, error = null) }
    }

    fun signUp(onSuccess: (String) -> Unit) {
        val handle = uiState.value.agentHandle.trim()
        val key = uiState.value.accessKey
        val confirm = uiState.value.confirmAccessKey

        if (handle.isBlank() || key.isBlank() || confirm.isBlank()) {
            _uiState.update { it.copy(error = "All fields required.") }
            return
        }
        if (key != confirm) {
            _uiState.update { it.copy(error = "Access keys do not match.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.signUp(handle, key.toCharArray())) {
                is AuthResult.Success -> {
                    sessionRepository.login(agentHandle = result.handle)
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess(result.handle)
                }
                is AuthResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }
}
