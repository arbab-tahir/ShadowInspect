package com.shadowinspect.app.data.auth

import com.shadowinspect.app.security.PasswordHash
import com.shadowinspect.app.security.PasswordHasher
import javax.inject.Inject

class AgentAuthRepository @Inject constructor(
    private val agentDao: AgentDao
) {

    suspend fun signUp(handle: String, accessKey: CharArray): AuthResult {
        val normalized = handle.trim()
        if (normalized.isBlank()) return AuthResult.Error("Agent handle required.")
        if (accessKey.joinToString("").trim().isBlank()) return AuthResult.Error("Access key required.")
        if (normalized.length !in 3..24) return AuthResult.Error("Agent handle must be 3–24 characters.")

        val existing = agentDao.findByHandle(normalized)
        if (existing != null) return AuthResult.Error("Agent handle already exists.")

        val hashed = PasswordHasher.hash(accessKey)
        val entity = AgentEntity(
            handle = normalized,
            passwordHashB64 = hashed.hashB64,
            saltB64 = hashed.saltB64,
            iterations = hashed.iterations,
            createdAtEpochMs = System.currentTimeMillis()
        )

        return try {
            agentDao.insert(entity)
            AuthResult.Success(normalized)
        } catch (_: Throwable) {
            AuthResult.Error("Failed to register Agent.")
        }
    }

    suspend fun login(handle: String, accessKey: CharArray): AuthResult {
        val normalized = handle.trim()
        if (normalized.isBlank()) return AuthResult.Error("Agent handle required.")
        if (accessKey.joinToString("").trim().isBlank()) return AuthResult.Error("Access key required.")

        val agent = agentDao.findByHandle(normalized)
            ?: return AuthResult.Error("Unknown Agent.")

        val stored = PasswordHash(
            hashB64 = agent.passwordHashB64,
            saltB64 = agent.saltB64,
            iterations = agent.iterations
        )

        return if (PasswordHasher.verify(accessKey, stored)) {
            AuthResult.Success(normalized)
        } else {
            AuthResult.Error("Access denied.")
        }
    }
}
