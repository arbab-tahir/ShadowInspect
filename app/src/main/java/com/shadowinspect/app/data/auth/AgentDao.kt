package com.shadowinspect.app.data.auth

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AgentDao {

    @Query("SELECT * FROM agents WHERE handle = :handle LIMIT 1")
    suspend fun findByHandle(handle: String): AgentEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(agent: AgentEntity): Long
}

