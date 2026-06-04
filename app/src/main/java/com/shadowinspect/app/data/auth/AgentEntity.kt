package com.shadowinspect.app.data.auth

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "agents",
    indices = [
        Index(value = ["handle"], unique = true)
    ]
)
data class AgentEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "handle")
    val handle: String,

    @ColumnInfo(name = "password_hash_b64")
    val passwordHashB64: String,

    @ColumnInfo(name = "salt_b64")
    val saltB64: String,

    @ColumnInfo(name = "iterations")
    val iterations: Int,

    @ColumnInfo(name = "created_at_epoch_ms")
    val createdAtEpochMs: Long
)

