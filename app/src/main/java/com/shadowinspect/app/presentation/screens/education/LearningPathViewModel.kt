package com.shadowinspect.app.presentation.screens.education

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shadowinspect.app.domain.education.LearningLevel
import com.shadowinspect.app.domain.education.SecurityLesson
import com.shadowinspect.app.domain.repository.EducationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LearningPathUiState(
    val level: LearningLevel = LearningLevel.BEGINNER,
    val lessons: List<SecurityLesson> = emptyList(),
    val completedLessonIds: List<Long> = emptyList(),
    val totalXp: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class LearningPathViewModel @Inject constructor(
    private val repository: EducationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LearningPathUiState())
    val uiState: StateFlow<LearningPathUiState> = _uiState.asStateFlow()

    fun loadLevel(level: LearningLevel) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(level = level, isLoading = true)
            
            // Observe lessons
            launch {
                repository.getLessonsByLevel(level).collect { lessons ->
                    _uiState.value = _uiState.value.copy(
                        lessons = lessons,
                        isLoading = false
                    )
                }
            }
            
            // Observe user progress continuously
            launch {
                repository.getUserProgressFlow().collect { progress ->
                    _uiState.value = _uiState.value.copy(
                        completedLessonIds = progress.completedLessons,
                        totalXp = progress.totalXp
                    )
                }
            }
        }
    }
    
    fun refreshProgress() {
        viewModelScope.launch {
            val progress = repository.getUserProgress()
            _uiState.value = _uiState.value.copy(
                completedLessonIds = progress.completedLessons,
                totalXp = progress.totalXp
            )
        }
    }
}
