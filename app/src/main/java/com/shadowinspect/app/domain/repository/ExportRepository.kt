package com.shadowinspect.app.domain.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.shadowinspect.app.BuildConfig
import com.shadowinspect.app.domain.dashboard.ThreatTrend
import com.shadowinspect.app.domain.dashboard.TechniqueCount
import com.shadowinspect.app.domain.trends.ThreatPrediction
import com.shadowinspect.app.domain.export.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val csvExporter: CsvExporter,
    private val jsonExporter: JsonExporter,
    private val pdfExporter: PdfExporter
) {
    
    private val exportsDir: File by lazy {
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ShadowInspect_Reports").apply {
            if (!exists()) mkdirs()
        }
    }
    
    /**
     * Export data in specified format
     */
    suspend fun export(
        config: ExportConfig,
        threatTrends: List<ThreatTrend>,
        topTechniques: List<TechniqueCount>,
        predictions: List<ThreatPrediction>,
        mitreDetections: List<com.shadowinspect.app.domain.mitre.MitreDetection>? = null,
        scanStats: com.shadowinspect.app.domain.export.ScanExportStats? = null
    ): ExportResult = withContext(Dispatchers.IO) {
        
        when (config.format) {
            ExportFormat.CSV -> {
                if (config.includeRawData) {
                    csvExporter.exportScans(config)
                } else {
                    csvExporter.exportAnalytics(config, threatTrends, topTechniques)
                }
            }
            ExportFormat.JSON -> {
                jsonExporter.exportCompleteAnalytics(config, threatTrends, topTechniques, predictions, mitreDetections, scanStats)
            }
            ExportFormat.PDF -> {
                pdfExporter.exportPdfReport(config, threatTrends, topTechniques, predictions, mitreDetections, scanStats)
            }
            else -> throw IllegalArgumentException("Format not supported")
        }
    }
    
    /**
     * Get all exports
     */
    fun getExports(): Flow<List<ExportMetadata>> = flow {
        val files = exportsDir.listFiles()
            ?.filter { it.isFile }
            ?.map { file ->
                ExportMetadata(
                    id = file.nameWithoutExtension,
                    fileName = file.name,
                    format = getFormatFromExtension(file.extension),
                    size = file.length(),
                    generatedAt = file.lastModified(),
                    recordCount = estimateRecordCount(file),
                    thumbnailPath = null
                )
            }
            ?.sortedByDescending { it.generatedAt }
            ?: emptyList()
        
        emit(files)
    }
    
    /**
     * Share export file
     */
    fun shareExport(fileName: String): Intent {
        val file = File(exportsDir, fileName)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val mimeType = when (file.extension.lowercase()) {
            "csv" -> "text/csv"
            "json" -> "application/json"
            "pdf" -> "application/pdf"
            else -> "*/*"
        }
        
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    
    /**
     * Delete export file
     */
    fun deleteExport(fileName: String): Boolean {
        val file = File(exportsDir, fileName)
        return file.delete()
    }
    
    /**
     * Clear all exports
     */
    fun clearAllExports(): Int {
        val files = exportsDir.listFiles() ?: return 0
        var deleted = 0
        files.forEach {
            if (it.delete()) deleted++
        }
        return deleted
    }
    
    private fun getFormatFromExtension(extension: String): ExportFormat {
        return when (extension.lowercase()) {
            "csv" -> ExportFormat.CSV
            "json" -> ExportFormat.JSON
            "pdf" -> ExportFormat.PDF
            else -> ExportFormat.CSV
        }
    }
    
    private fun estimateRecordCount(file: File): Int {
        // Rough estimate - in production you'd parse the file
        return (file.length() / 1024).toInt()
    }
}
