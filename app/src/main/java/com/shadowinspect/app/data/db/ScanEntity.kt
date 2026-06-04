package com.shadowinspect.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scanType: String,                                // "URL", "APK", "PHONE"
    val target: String,                                  // URL, filename, or phone number
    val riskScore: Int,
    val riskLevel: String,
    val timestamp: Long = System.currentTimeMillis(),
    val detailsJson: String,                             // full JSON payload
    val reportPath: String? = null,                      // PDF report path

    // ── Phone-specific fields (Module 7) ──────────────────────────────────────
    @ColumnInfo(defaultValue = "") val phoneNumber: String? = null,
    @ColumnInfo(defaultValue = "") val countryCode: String? = null,
    @ColumnInfo(defaultValue = "") val carrier: String? = null,
    @ColumnInfo(defaultValue = "") val lineType: String? = null,
    @ColumnInfo(defaultValue = "0") val isVoIP: Boolean = false,
    @ColumnInfo(name = "phoneSpamScore", defaultValue = "0") val phoneSpamScore: Int = 0,
    @ColumnInfo(defaultValue = "") val validationSources: String? = null  // JSON of sources used
) {
    companion object {
        const val SCAN_TYPE_URL   = "URL"
        const val SCAN_TYPE_APK   = "APK"
        const val SCAN_TYPE_PHONE = "PHONE"
        const val SCAN_TYPE_IMAGE = "IMAGE"
        const val SCAN_TYPE_DOCUMENT = "DOCUMENT"
    }
}
