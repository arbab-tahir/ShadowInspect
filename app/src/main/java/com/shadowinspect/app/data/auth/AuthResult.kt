package com.shadowinspect.app.data.auth

sealed interface AuthResult {
    data class Success(val handle: String) : AuthResult
    data class Error(val message: String) : AuthResult
}
