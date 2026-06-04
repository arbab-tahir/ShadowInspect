package com.shadowinspect.app.security

import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class PasswordHash(
    val hashB64: String,
    val saltB64: String,
    val iterations: Int
)

object PasswordHasher {
    private const val KEY_LENGTH_BITS = 256
    private const val DEFAULT_ITERATIONS = 120_000

    fun hash(password: CharArray, iterations: Int = DEFAULT_ITERATIONS): PasswordHash {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val derived = pbkdf2(password, salt, iterations)
        return PasswordHash(
            hashB64 = Base64.encodeToString(derived, Base64.NO_WRAP),
            saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP),
            iterations = iterations
        )
    }

    fun verify(password: CharArray, stored: PasswordHash): Boolean {
        return try {
            val salt = Base64.decode(stored.saltB64, Base64.NO_WRAP)
            val expected = Base64.decode(stored.hashB64, Base64.NO_WRAP)
            val actual = pbkdf2(password, salt, stored.iterations)
            MessageDigest.isEqual(expected, actual)
        } catch (e: Exception) {
            false
        }
    }

    private fun pbkdf2(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }
}

