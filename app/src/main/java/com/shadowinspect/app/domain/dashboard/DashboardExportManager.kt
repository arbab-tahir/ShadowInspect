package com.shadowinspect.app.domain.dashboard

import android.content.Context
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.AreaBreak
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

import android.content.Intent
import androidx.core.content.FileProvider
import android.os.Build
import android.content.ContentValues
import android.provider.MediaStore
import android.os.Environment
import java.io.FileInputStream

@Singleton
class DashboardExportManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    suspend fun exportDashboardSummary(state: DashboardState): File = withContext(Dispatchers.IO) {
        val fileName = "ShadowInspect_Dashboard_${System.currentTimeMillis()}.pdf"
        val reportsDir = File(context.getExternalFilesDir(null), "Reports")
        if (!reportsDir.exists()) reportsDir.mkdirs()
        
        val file = File(reportsDir, fileName)
        FileOutputStream(file).use { outputStream ->
            val writer = PdfWriter(outputStream)
            val pdfDocument = PdfDocument(writer)
            val document = Document(pdfDocument, PageSize.A4)
            document.setMargins(36f, 36f, 36f, 36f)
            
            // Colors
            val neonGreen = DeviceRgb(0, 200, 83)
            val neonRed = DeviceRgb(255, 59, 59)
            val midnightBlue = DeviceRgb(25, 42, 86)
            
            // Title
            document.add(Paragraph("ShadowInspect Security Analytics")
                .setFontSize(24f)
                .setBold()
                .setFontColor(midnightBlue)
                .setTextAlignment(TextAlignment.CENTER))
            
            document.add(Paragraph("Generated on: ${dateFormat.format(Date())}")
                .setFontSize(10f)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20f))
            
            // Score Summary
            document.add(Paragraph("OVERALL SECURITY POSTURE")
                .setBold()
                .setFontSize(14f)
                .setFontColor(midnightBlue))
            
            val scoreTable = Table(2).useAllAvailableWidth()
            scoreTable.addCell(createKeyCell("Device Security Score"))
            scoreTable.addCell(createValueCell("${state.securityScore}/100").setFontColor(if (state.securityScore > 70) neonGreen else neonRed))
            scoreTable.addCell(createKeyCell("Total Scans Performed"))
            scoreTable.addCell(createValueCell(state.totalScans.toString()))
            scoreTable.addCell(createKeyCell("Critical Threats Found"))
            scoreTable.addCell(createValueCell(state.criticalCount.toString()).setFontColor(if (state.criticalCount > 0) neonRed else midnightBlue))
            scoreTable.addCell(createKeyCell("High Risk Findings"))
            scoreTable.addCell(createValueCell(state.highRiskCount.toString()))
            document.add(scoreTable.setMarginBottom(16f))
            
            // Risk Distribution
            document.add(Paragraph("RISK DISTRIBUTION")
                .setBold()
                .setFontSize(14f)
                .setFontColor(midnightBlue))
            
            val distTable = Table(5).useAllAvailableWidth()
            distTable.addHeaderCell("Critical")
            distTable.addHeaderCell("High")
            distTable.addHeaderCell("Medium")
            distTable.addHeaderCell("Low")
            distTable.addHeaderCell("Safe")
            
            distTable.addCell(createValueCell(state.riskDistribution.critical.toString()))
            distTable.addCell(createValueCell(state.riskDistribution.high.toString()))
            distTable.addCell(createValueCell(state.riskDistribution.medium.toString()))
            distTable.addCell(createValueCell(state.riskDistribution.low.toString()))
            distTable.addCell(createValueCell(state.riskDistribution.safe.toString()))
            document.add(distTable.setMarginBottom(16f))
            
            // Top MITRE Techniques
            if (state.techniqueDistribution.isNotEmpty()) {
                document.add(Paragraph("TOP DETECTED TECHNIQUES (MITRE ATT&CK)")
                    .setBold()
                    .setFontSize(14f)
                    .setFontColor(midnightBlue))
                
                val techTable = Table(floatArrayOf(1f, 4f, 1f)).useAllAvailableWidth()
                techTable.addHeaderCell("ID")
                techTable.addHeaderCell("Technique Name")
                techTable.addHeaderCell("Count")
                
                state.techniqueDistribution.take(10).forEach { item ->
                    techTable.addCell(createValueCell(item.techniqueId))
                    techTable.addCell(createValueCell(item.techniqueName))
                    techTable.addCell(createValueCell(item.count.toString()))
                }
                document.add(techTable.setMarginBottom(16f))
            }
            
            // Recent Activity
            if (state.recentActivity.isNotEmpty()) {
                document.add(AreaBreak())
                document.add(Paragraph("RECENT SECURITY ACTIVITY")
                    .setBold()
                    .setFontSize(14f)
                    .setFontColor(midnightBlue))
                
                val activityTable = Table(floatArrayOf(1f, 3f, 1.5f, 1f)).useAllAvailableWidth()
                activityTable.addHeaderCell("Type")
                activityTable.addHeaderCell("Target")
                activityTable.addHeaderCell("Date")
                activityTable.addHeaderCell("Score")
                
                state.recentActivity.take(20).forEach { activity ->
                    activityTable.addCell(createValueCell(activity.scanType))
                    activityTable.addCell(createValueCell(activity.target))
                    activityTable.addCell(createValueCell(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(activity.timestamp))))
                    activityTable.addCell(createValueCell(activity.riskScore.toString()))
                }
                document.add(activityTable)
            }
            
            document.add(Paragraph("\nGenerated by ShadowInspect Mobile Security Engine.")
                .setFontSize(8f)
                .setTextAlignment(TextAlignment.CENTER))
            
            document.close()
        }
        file
    }
    
    /**
     * Share report via intent
     */
    fun shareReport(file: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Save a generated file to the public Downloads folder
     */
    suspend fun saveToDownloads(file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues) ?: return@withContext false
                resolver.openOutputStream(uri)?.use { outputStream ->
                    FileInputStream(file).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                true
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val destFile = File(downloadsDir, file.name)
                FileInputStream(file).use { inputStream ->
                    FileOutputStream(destFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    private fun createKeyCell(text: String): Cell {
        return Cell().add(Paragraph(text).setBold()).setBackgroundColor(DeviceRgb(240, 240, 240))
    }
    
    private fun createValueCell(text: String): Cell {
        return Cell().add(Paragraph(text))
    }
}
