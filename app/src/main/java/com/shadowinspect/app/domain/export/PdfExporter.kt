package com.shadowinspect.app.domain.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.shadowinspect.app.data.db.ScanDao
import com.shadowinspect.app.data.db.MitreDetectionDao
import com.shadowinspect.app.domain.dashboard.ThreatTrend
import com.shadowinspect.app.domain.dashboard.TechniqueCount
import com.shadowinspect.app.domain.trends.ThreatPrediction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scanDao: ScanDao,
    private val mitreDao: MitreDetectionDao
) {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    
    /**
     * Export analytics report as PDF with charts
     */
    suspend fun exportPdfReport(
        config: ExportConfig,
        threatTrends: List<ThreatTrend>,
        topTechniques: List<TechniqueCount>,
        predictions: List<ThreatPrediction>,
        mitreDetections: List<com.shadowinspect.app.domain.mitre.MitreDetection>? = null,
        scanStats: com.shadowinspect.app.domain.export.ScanExportStats? = null
    ): ExportResult = withContext(Dispatchers.IO) {
        
        val timeRange = getTimeRange(config)
        val scans = scanDao.getScansByDateRange(timeRange.first, timeRange.second)
        
        val document = PdfDocument()
        val titlePaint = Paint().apply {
            textSize = 24f
            isFakeBoldText = true
            color = android.graphics.Color.rgb(0, 255, 157) // Neon Green
        }
        val textPaint = Paint().apply {
            textSize = 14f
            color = android.graphics.Color.BLACK
        }
        val headerPaint = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
            color = android.graphics.Color.rgb(0, 150, 255) // Cyber Blue
        }
        
        // Page 1: Cover & Summary
        val pageInfo1 = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page1 = document.startPage(pageInfo1)
        val canvas1 = page1.canvas
        
        // Background (optional - let's stay white for printability)
        
        // Title
        canvas1.drawText("ShadowInspect Analytics Report", 50f, 80f, titlePaint)
        
        // Subtitle
        textPaint.textSize = 12f
        canvas1.drawText("Generated on: ${dateFormat.format(Date())}", 50f, 110f, textPaint)
        canvas1.drawText("Period: ${formatTimeRange(timeRange.first, timeRange.second)}", 50f, 130f, textPaint)
        
        // Horizontal Line
        canvas1.drawLine(50f, 150f, 545f, 150f, Paint().apply { 
            strokeWidth = 2f
            color = android.graphics.Color.LTGRAY 
        })
        
        // Executive Summary
        canvas1.drawText("Executive Summary", 50f, 190f, headerPaint)
        
        textPaint.textSize = 14f
        var yPos = 230f
        canvas1.drawText("Total Transactions Analyzed: ${scans.size}", 70f, yPos, textPaint)
        yPos += 30f
        
        val critical = scans.count { it.riskLevel == "CRITICAL" }
        val high = scans.count { it.riskLevel == "HIGH" }
        val medium = scans.count { it.riskLevel == "MEDIUM" }
        val low = scans.count { it.riskLevel == "LOW" }
        val safe = scans.count { it.riskLevel == "SAFE" }
        
        canvas1.drawText("Risk Distribution:", 70f, yPos, textPaint)
        yPos += 25f
        
        textPaint.color = android.graphics.Color.RED
        canvas1.drawText("  • Critical: $critical", 90f, yPos, textPaint)
        yPos += 25f
        
        textPaint.color = android.graphics.Color.rgb(255, 165, 0) // Orange
        canvas1.drawText("  • High Risk: $high", 90f, yPos, textPaint)
        yPos += 25f
        
        textPaint.color = android.graphics.Color.rgb(200, 200, 0) // Dark Yellow
        canvas1.drawText("  • Medium Risk: $medium", 90f, yPos, textPaint)
        yPos += 25f
        
        textPaint.color = android.graphics.Color.GREEN
        canvas1.drawText("  • Low Risk / Safe: ${low + safe}", 90f, yPos, textPaint)
        yPos += 40f
        
        textPaint.color = android.graphics.Color.BLACK
        canvas1.drawText("Scan Type Breakdown:", 70f, yPos, textPaint)
        yPos += 25f
        canvas1.drawText("  • APK Analysis: ${scans.count { it.scanType == "APK" }}", 90f, yPos, textPaint)
        yPos += 25f
        canvas1.drawText("  • URL Verification: ${scans.count { it.scanType == "URL" }}", 90f, yPos, textPaint)
        yPos += 25f
        canvas1.drawText("  • Phone Validation: ${scans.count { it.scanType == "PHONE" }}", 90f, yPos, textPaint)
        
        document.finishPage(page1)
        
        // Page 2: Top Threats & Techniques
        val pageInfo2 = PdfDocument.PageInfo.Builder(595, 842, 2).create()
        val page2 = document.startPage(pageInfo2)
        val canvas2 = page2.canvas
        
        canvas2.drawText("MITRE ATT&CK® Techniques Observed", 50f, 80f, headerPaint)
        
        yPos = 130f
        textPaint.color = android.graphics.Color.BLACK
        if (topTechniques.isEmpty()) {
            canvas2.drawText("No specific attack techniques detected in this period.", 70f, yPos, textPaint)
        } else {
            topTechniques.take(15).forEachIndexed { index, tech ->
                val lineText = "${index + 1}. ${tech.techniqueName} (${tech.techniqueId})"
                canvas2.drawText(lineText, 70f, yPos, textPaint)
                canvas2.drawText("Found in ${tech.count} scans", 400f, yPos, Paint(textPaint).apply { textSize = 11f })
                yPos += 35f
                
                if (yPos > 780f) {
                    // Start new page if needed (simplified here)
                }
            }
        }
        
        document.finishPage(page2)
        
        // Page 3: Predictions & Trends (Optional)
        if (predictions.isNotEmpty() || threatTrends.isNotEmpty()) {
            val pageInfo3 = PdfDocument.PageInfo.Builder(595, 842, 3).create()
            val page3 = document.startPage(pageInfo3)
            val canvas3 = page3.canvas
            
            canvas3.drawText("Future Threat Forecast", 50f, 80f, headerPaint)
            
            yPos = 130f
            predictions.take(5).forEach { pred ->
                canvas3.drawText("Targeted Threat: ${pred.threatType}", 70f, yPos, Paint(textPaint).apply { isFakeBoldText = true })
                yPos += 20f
                canvas3.drawText("Confidence: ${(pred.probability * 100).toInt()}% • Timeframe: ${pred.timeframe}", 90f, yPos, textPaint)
                yPos += 20f
                canvas3.drawText("Recommended Action: ${pred.recommendedActions.firstOrNull() ?: "Monitor closely"}", 90f, yPos, Paint(textPaint).apply { textSize = 11f; color = android.graphics.Color.DKGRAY })
                yPos += 45f
            }
            
            document.finishPage(page3)
        }
        
        // Save PDF
        val exportsDir = File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "ShadowInspect")
        if (!exportsDir.exists()) exportsDir.mkdirs()
        
        val fileName = "security_report_${fileDateFormat.format(Date())}.pdf"
        val file = File(exportsDir, fileName)
        
        FileOutputStream(file).use { outputStream ->
            document.writeTo(outputStream)
        }
        document.close()
        
        ExportResult(
            filePath = file.absolutePath,
            fileSize = file.length(),
            format = ExportFormat.PDF,
            recordCount = scans.size,
            generatedAt = System.currentTimeMillis(),
            timeRange = formatTimeRange(timeRange.first, timeRange.second),
            checksum = calculateChecksum(file)
        )
    }
    
    private fun getTimeRange(config: ExportConfig): Pair<Long, Long> {
        val end = config.endDate ?: System.currentTimeMillis()
        val start = when (config.scope) {
            ExportScope.ALL_TIME -> 0L
            ExportScope.LAST_YEAR -> end - 365L * 24 * 60 * 60 * 1000
            ExportScope.LAST_6_MONTHS -> end - 180L * 24 * 60 * 60 * 1000
            ExportScope.LAST_3_MONTHS -> end - 90L * 24 * 60 * 60 * 1000
            ExportScope.LAST_MONTH -> end - 30L * 24 * 60 * 60 * 1000
            ExportScope.LAST_WEEK -> end - 7L * 24 * 60 * 60 * 1000
            ExportScope.CUSTOM_RANGE -> config.startDate ?: (end - 30L * 24 * 60 * 60 * 1000)
        }
        return Pair(start, end)
    }
    
    private fun formatTimeRange(start: Long, end: Long): String {
        val startStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(start))
        val endStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(end))
        return "$startStr - $endStr"
    }
    
    private fun calculateChecksum(file: File): String {
        return java.security.MessageDigest.getInstance("MD5")
            .digest(file.readBytes())
            .joinToString("") { "%02x".format(it) }
    }
}
