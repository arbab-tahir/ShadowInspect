package com.shadowinspect.app.presentation.screens.education

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shadowinspect.app.domain.education.Badge
import com.shadowinspect.app.domain.repository.EducationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BadgesUiState(
    val allBadges: List<Badge> = emptyList(),
    val earnedBadgeIds: Set<Long> = emptySet(),
    val newBadgeIds: Set<Long> = emptySet(),
    val totalBadges: Int = 0
) {
    val earnedBadges: List<Badge> get() = allBadges.filter { earnedBadgeIds.contains(it.id) }
}

@HiltViewModel
class BadgesViewModel @Inject constructor(
    private val repository: EducationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BadgesUiState())
    val uiState: StateFlow<BadgesUiState> = _uiState.asStateFlow()

    init {
        loadBadges()
    }

    fun loadBadges() {
        viewModelScope.launch {
            val earned = repository.getEarnedBadges()
            val earnedIds = earned.map { it.badgeId }.toSet()
            val newIds = earned.filter { it.isNew }.map { it.badgeId }.toSet()

            repository.getAllBadges().collect { badges ->
                _uiState.value = _uiState.value.copy(
                    allBadges = badges,
                    earnedBadgeIds = earnedIds,
                    newBadgeIds = newIds,
                    totalBadges = badges.size
                )
            }

            // Badges seen status is managed automatically
        }
    }
}
