package com.shadowinspect.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PhoneReportDao {

    /** Insert a new community spam report. Ignores exact duplicates. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReport(report: PhoneReportEntity)

    /**
     * Total number of spam reports filed for [normalizedNumber].
     * Used to compute the community spam score.
     */
    @Query("SELECT COUNT(*) FROM phone_spam_reports WHERE normalizedNumber = :normalizedNumber")
    suspend fun getReportCount(normalizedNumber: String): Int

    /**
     * All distinct reason strings for [normalizedNumber].
     * Shown in the explanation to give users context on why it was flagged.
     */
    @Query(
        "SELECT DISTINCT reason FROM phone_spam_reports " +
        "WHERE normalizedNumber = :normalizedNumber AND reason != ''"
    )
    suspend fun getReasons(normalizedNumber: String): List<String>

    /** Delete all reports for a given number (user-initiated cleanup). */
    @Query("DELETE FROM phone_spam_reports WHERE normalizedNumber = :normalizedNumber")
    suspend fun deleteReportsFor(normalizedNumber: String)

    /** Wipe the entire local report database. */
    @Query("DELETE FROM phone_spam_reports")
    suspend fun clearAll()
}
