package com.shadowinspect.app.utils

import com.shadowinspect.app.data.db.ScanDao
import com.shadowinspect.app.data.db.ScanEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestDataGenerator @Inject constructor(
    private val scanDao: ScanDao
) {
    
    suspend fun generateTestData() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        
        // Generate APK scans
        val apkScans = listOf(
            ScanEntity(
                scanType = "APK",
                target = "com.facebook.katana",
                riskScore = 15,
                riskLevel = "LOW",
                timestamp = now - 2 * 24 * 60 * 60 * 1000,
                detailsJson = "{}"
            ),
            ScanEntity(
                scanType = "APK",
                target = "com.whatsapp",
                riskScore = 10,
                riskLevel = "LOW",
                timestamp = now - 5 * 24 * 60 * 60 * 1000,
                detailsJson = "{}"
            ),
            ScanEntity(
                scanType = "APK",
                target = "com.suspicious.malware",
                riskScore = 85,
                riskLevel = "CRITICAL",
                timestamp = now - 1 * 24 * 60 * 60 * 1000,
                detailsJson = "{}"
            )
        )
        
        // Generate URL scans
        val urlScans = listOf(
            ScanEntity(
                scanType = "URL",
                target = "https://google.com",
                riskScore = 5,
                riskLevel = "SAFE",
                timestamp = now - 3 * 24 * 60 * 60 * 1000,
                detailsJson = "{}"
            ),
            ScanEntity(
                scanType = "URL",
                target = "https://suspicious-site.com",
                riskScore = 65,
                riskLevel = "HIGH",
                timestamp = now - 4 * 24 * 60 * 60 * 1000,
                detailsJson = "{}"
            )
        )
        
        // Generate phone scans
        val phoneScans = listOf(
            ScanEntity(
                scanType = "PHONE",
                target = "+1234567890",
                riskScore = 20,
                riskLevel = "LOW",
                timestamp = now - 6 * 24 * 60 * 60 * 1000,
                detailsJson = "{}",
                phoneNumber = "+1234567890",
                carrier = "T-Mobile",
                lineType = "MOBILE"
            )
        )
        
        // Insert all test data
        (apkScans + urlScans + phoneScans).forEach { scan ->
            scanDao.insert(scan)
        }
    }
}
