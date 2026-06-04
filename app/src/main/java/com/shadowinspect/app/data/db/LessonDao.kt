package com.shadowinspect.app.data.db

import androidx.room.*
import com.shadowinspect.app.domain.education.LessonCategory
import com.shadowinspect.app.domain.education.LearningLevel
import com.shadowinspect.app.domain.education.SecurityLesson
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonDao {

    @Query("SELECT * FROM lessons ORDER BY orderIndex ASC")
    fun getAllLessons(): Flow<List<SecurityLesson>>

    @Query("SELECT * FROM lessons WHERE category = :category ORDER BY orderIndex ASC")
    fun getLessonsByCategory(category: LessonCategory): Flow<List<SecurityLesson>>

    @Query("SELECT * FROM lessons WHERE level = :level ORDER BY orderIndex ASC")
    fun getLessonsByLevel(level: LearningLevel): Flow<List<SecurityLesson>>

    @Query("SELECT * FROM lessons WHERE id = :lessonId LIMIT 1")
    suspend fun getLesson(lessonId: Long): SecurityLesson?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLesson(lesson: SecurityLesson)

    @Query("SELECT COUNT(*) FROM lessons")
    suspend fun getLessonCount(): Long
}
