package com.shadowinspect.app.di

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.shadowinspect.app.data.session.AgentSessionRepository
import com.shadowinspect.app.data.session.EncryptedPrefsAgentSessionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface SessionModule {

    @Binds
    @Singleton
    fun bindAgentSessionRepository(
        impl: EncryptedPrefsAgentSessionRepository
    ): AgentSessionRepository

    companion object {
        @Provides
        @Singleton
        fun provideSharedPreferences(
            @ApplicationContext context: Context
        ): SharedPreferences {
            return try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                EncryptedSharedPreferences.create(
                    context,
                    "shadowinspect_secure_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                // FALLBACK: If hardware security is unavailable (common on some emulators),
                // use standard SharedPreferences to prevent an immediate startup crash.
                android.util.Log.e("SessionModule", "EncryptedSharedPreferences failed, falling back", e)
                context.getSharedPreferences("shadowinspect_prefs", Context.MODE_PRIVATE)
            }
        }
    }
}
