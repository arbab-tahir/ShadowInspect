package com.shadowinspect.app.presentation.screens.education

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shadowinspect.app.domain.education.DailyTip
import com.shadowinspect.app.domain.education.LessonCategory
import com.shadowinspect.app.domain.education.SecurityLesson
import com.shadowinspect.app.domain.repository.EducationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.shadowinspect.app.domain.education.LearningPath

data class EducationUiState(
    val totalXp: Int = 0,
    val currentStreak: Int = 0,
    val completedLessons: List<Long> = emptyList(),
    val totalLessons: Int = 0,
    val dailyTip: DailyTip? = null,
    val allLessons: List<SecurityLesson> = emptyList(),
    val filteredLessons: List<SecurityLesson> = emptyList(),
    val learningPaths: List<LearningPath> = emptyList(),
    val selectedCategory: LessonCategory? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class EducationViewModel @Inject constructor(
    private val repository: EducationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EducationUiState())
    val uiState: StateFlow<EducationUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeEducationData()
            loadData()
            loadLessons()
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            // Load daily tip
            val tip = repository.getDailyTip()
            _uiState.value = _uiState.value.copy(dailyTip = tip)

            // Observe real-time progress
            launch {
                repository.getUserProgressFlow().collect { progress ->
                    _uiState.value = _uiState.value.copy(
                        totalXp = progress.totalXp,
                        currentStreak = progress.currentStreak,
                        completedLessons = progress.completedLessons
                    )
                }
            }

            // Observe paths
            launch {
                repository.getAllPaths().collect { paths ->
                    _uiState.value = _uiState.value.copy(
                        learningPaths = paths
                    )
                }
            }
        }
    }

    private fun loadLessons() {
        viewModelScope.launch {
            repository.getAllLessons().collect { lessons ->
                _uiState.value = _uiState.value.copy(
                    allLessons = lessons,
                    filteredLessons = lessons,
                    totalLessons = lessons.size,
                    isLoading = false
                )
            }
        }
    }

    fun selectCategory(category: LessonCategory?) {
        val current = _uiState.value
        val newCategory = if (current.selectedCategory == category) null else category
        val filtered = if (newCategory == null) {
            current.allLessons
        } else {
            current.allLessons.filter { it.category == newCategory }
        }
        _uiState.value = current.copy(
            selectedCategory = newCategory,
            filteredLessons = filtered
        )
    }

    fun refreshProgress() {
        viewModelScope.launch {
            val progress = repository.getUserProgress()
            _uiState.value = _uiState.value.copy(
                totalXp = progress.totalXp,
                currentStreak = progress.currentStreak,
                completedLessons = progress.completedLessons
            )
        }
    }
}
