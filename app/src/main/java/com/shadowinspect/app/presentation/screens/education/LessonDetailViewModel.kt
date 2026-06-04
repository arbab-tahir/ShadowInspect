package com.shadowinspect.app.presentation.screens.education

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shadowinspect.app.domain.education.SecurityLesson
import com.shadowinspect.app.domain.repository.EducationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LessonDetailUiState(
    val lesson: SecurityLesson? = null,
    val isCompleted: Boolean = false,
    val isLoading: Boolean = true,
    val showCompletedBanner: Boolean = false,
    val nextLessonId: Long? = null,
    val nextPathLevel: com.shadowinspect.app.domain.education.LearningLevel? = null,
    val isLevelCompleted: Boolean = false,
    val isEntireCourseCompleted: Boolean = false
)

@HiltViewModel
class LessonDetailViewModel @Inject constructor(
    private val repository: EducationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LessonDetailUiState())
    val uiState: StateFlow<LessonDetailUiState> = _uiState.asStateFlow()

    fun loadLesson(lessonId: Long) {
        viewModelScope.launch {
            val lesson = repository.getLesson(lessonId)
            val progress = repository.getUserProgress()
            
            // Find next lesson within the same level
            val allLessons = repository.getAllLessons().first()
            val nextLesson = if (lesson != null) {
                allLessons.filter { it.level == lesson.level && it.orderIndex > lesson.orderIndex }
                    .minByOrNull { it.orderIndex }
            } else null
            
            val isCompleted = progress.completedLessons.contains(lessonId)
            
            // Calculate if exactly the end of the current level
            val isLevelCompletedHere = nextLesson == null && lesson != null 
            
            val nextLevel = if (isLevelCompletedHere) {
                when (lesson!!.level) {
                    com.shadowinspect.app.domain.education.LearningLevel.BEGINNER -> com.shadowinspect.app.domain.education.LearningLevel.INTERMEDIATE
                    com.shadowinspect.app.domain.education.LearningLevel.INTERMEDIATE -> com.shadowinspect.app.domain.education.LearningLevel.ADVANCED
                    com.shadowinspect.app.domain.education.LearningLevel.ADVANCED -> com.shadowinspect.app.domain.education.LearningLevel.EXPERT
                    com.shadowinspect.app.domain.education.LearningLevel.EXPERT -> null
                }
            } else null

            val isWholeCourseDone = isLevelCompletedHere && lesson?.level == com.shadowinspect.app.domain.education.LearningLevel.EXPERT

            _uiState.value = _uiState.value.copy(
                lesson = lesson,
                isCompleted = isCompleted,
                nextLessonId = nextLesson?.id,
                nextPathLevel = nextLevel,
                isLevelCompleted = isLevelCompletedHere && isCompleted && !isWholeCourseDone,
                isEntireCourseCompleted = isWholeCourseDone && isCompleted,
                isLoading = false
            )
        }
    }

    fun markComplete(
        onNavigateNext: ((Long) -> Unit)? = null, 
        onNavigateNextLevel: ((com.shadowinspect.app.domain.education.LearningLevel) -> Unit)? = null,
        onNavigateToDashboard: (() -> Unit)? = null
    ) {
        val lesson = _uiState.value.lesson ?: return
        val lessonId = lesson.id
        
        // Optimistically update UI immediately
        val isLevelFinishedHere = _uiState.value.nextLessonId == null
        val isWholeCourseDone = isLevelFinishedHere && lesson.level == com.shadowinspect.app.domain.education.LearningLevel.EXPERT
        val isLevelFinished = _uiState.value.nextPathLevel != null
        
        _uiState.value = _uiState.value.copy(
            isCompleted = true,
            showCompletedBanner = true,
            isLevelCompleted = isLevelFinished,
            isEntireCourseCompleted = isWholeCourseDone
        )
        
        viewModelScope.launch {
            repository.completeLesson(lessonId, isLevelFinished || isWholeCourseDone)
            // If the user wants to jump directly to next lesson
            val nextId = _uiState.value.nextLessonId
            if (onNavigateNext != null && nextId != null) {
                onNavigateNext(nextId)
            } else if (onNavigateNextLevel != null && isLevelFinished) {
                onNavigateNextLevel(_uiState.value.nextPathLevel!!)
            } else if (onNavigateToDashboard != null && isWholeCourseDone) {
                onNavigateToDashboard()
            }
        }
    }

    fun dismissBanner() {
        _uiState.value = _uiState.value.copy(showCompletedBanner = false)
    }
}
