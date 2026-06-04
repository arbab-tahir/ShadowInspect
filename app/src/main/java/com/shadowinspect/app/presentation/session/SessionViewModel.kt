package com.shadowinspect.app.presentation.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shadowinspect.app.data.session.AgentSession
import com.shadowinspect.app.data.session.AgentSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val repository: AgentSessionRepository
) : ViewModel() {

    val session: StateFlow<AgentSession?> = repository.session
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isLoggedIn: StateFlow<Boolean> = repository.session
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
}
