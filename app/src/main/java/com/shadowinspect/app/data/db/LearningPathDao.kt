package com.shadowinspect.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shadowinspect.app.domain.education.LearningPath
import kotlinx.coroutines.flow.Flow

@Dao
interface LearningPathDao {
    @Query("SELECT * FROM learning_paths ORDER BY id ASC")
    fun getAllPaths(): Flow<List<LearningPath>>

    @Query("SELECT * FROM learning_paths WHERE id = :id")
    suspend fun getPathById(id: Long): LearningPath?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPath(path: LearningPath)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaths(paths: List<LearningPath>)
    
    @Query("SELECT COUNT(*) FROM learning_paths")
    suspend fun getPathCount(): Int
}
