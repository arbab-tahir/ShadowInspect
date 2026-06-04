package com.shadowinspect.app.data.session

import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

import javax.inject.Inject

class EncryptedPrefsAgentSessionRepository @Inject constructor(
    private val prefs: SharedPreferences
) : AgentSessionRepository {


    private val _session = MutableStateFlow(readSession())
    override val session: Flow<AgentSession?> = _session.asStateFlow()

    override suspend fun login(agentHandle: String) {
        prefs.edit()
            .putString(KEY_AGENT_HANDLE, agentHandle)
            .apply()
        _session.value = AgentSession(agentHandle)
    }

    override suspend fun logout() {
        prefs.edit()
            .remove(KEY_AGENT_HANDLE)
            .apply()
        _session.value = null
    }

    private fun readSession(): AgentSession? {
        val handle = prefs.getString(KEY_AGENT_HANDLE, null)?.trim().orEmpty()
        return handle.takeIf { it.isNotBlank() }?.let { AgentSession(it) }
    }

    private companion object {
        const val KEY_AGENT_HANDLE = "agent_handle"
    }
}

