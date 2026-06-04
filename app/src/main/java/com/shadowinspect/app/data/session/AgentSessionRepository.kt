package com.shadowinspect.app.data.session

import kotlinx.coroutines.flow.Flow

/**
 * Persists and exposes the current Agent session.
 *
 * ShadowInspect uses the term "Agent" for the signed-in user identity.
 */
interface AgentSessionRepository {
    val session: Flow<AgentSession?>

    suspend fun login(agentHandle: String)

    suspend fun logout()
}

