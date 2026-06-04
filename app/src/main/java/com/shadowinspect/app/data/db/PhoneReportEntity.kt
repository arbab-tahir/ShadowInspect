package com.shadowinspect.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persists a single user/community spam report for a phone number.
 *
 * Privacy notes:
 * - Only the normalised E.164 number is stored (no user identity).
 * - Reports are never transmitted externally without explicit consent.
 * - [reason] is optional free-text and may be left blank.
 */
@Entity(
    tableName = "phone_spam_reports",
    indices = [Index(value = ["normalizedNumber"])]   // fast lookup by number
)
data class PhoneReportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** E.164-normalised number, e.g. "+16502530000". Used as the lookup key. */
    val normalizedNumber: String,

    /** Short user-supplied reason (e.g. "Robocall", "Scam", "Telemarketing"). */
    val reason: String = "",

    /** Epoch ms when this report was filed. */
    val timestamp: Long = System.currentTimeMillis()
)
