package com.shadowinspect.app.domain.repository

import android.content.Context
import android.os.Build
import com.shadowinspect.app.BuildConfig
import com.shadowinspect.app.data.db.MitreDetectionDao
import com.shadowinspect.app.data.db.ScanDao
import com.shadowinspect.app.data.db.UserFeedbackDao
import com.shadowinspect.app.data.db.ResearchDataDao
import com.shadowinspect.app.domain.mitre.MitreAnalysisResult
import com.shadowinspect.app.domain.research.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import com.google.gson.GsonBuilder

@Singleton
class ResearchRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scanDao: com.shadowinspect.app.data.db.ScanDao,
    private val mitreDao: com.shadowinspect.app.data.db.MitreDetectionDao,
    private val userFeedbackDao: com.shadowinspect.app.data.db.UserFeedbackDao,
    private val researchDataDao: com.shadowinspect.app.data.db.ResearchDataDao
) {
    
    private val gson = GsonBuilder().setPrettyPrinting().create()
    
    /**
     * Collect anonymous research data from a scan
     * NO PII - only technique IDs and risk scores
     */
    /**
     * Collect anonymous research data from a scan.
     * Called automatically after each APK/URL/Phone analysis.
     * NO PII — only technique IDs, risk scores, and anonymous device info.
     */
    suspend fun collectResearchData(
        scanId: Long,
        scanType: String,
        mitreResult: MitreAnalysisResult,
        // New optional enrichment fields
        appCategory: String = "Unknown",
        dangerousPermissions: List<String> = emptyList(),
        isDebuggable: Boolean = false,
        virusTotalVerdict: String = "Not Checked"
    ) {
        // Derive unique tactics from detected techniques
        val detectedTactics = mitreResult.detectedTechniques
            .flatMap { it.technique.tactics }
            .distinct()

        val dataPoint = ResearchDataPoint(
            scanId = scanId,
            scanType = scanType,
            appCategory = appCategory,
            riskScore = mitreResult.riskScore,
            riskLevel = mitreResult.riskLevel,
            techniqueCount = mitreResult.totalTechniques,
            detectedTechniques = mitreResult.detectedTechniques.map { it.technique.id },
            detectedTactics = detectedTactics,
            dangerousPermissionCount = dangerousPermissions.size,
            dangerousPermissions = dangerousPermissions,
            isDebuggable = isDebuggable,
            virusTotalVerdict = virusTotalVerdict,
            timestamp = System.currentTimeMillis(),
            androidVersion = Build.VERSION.SDK_INT,
            deviceModel = getAnonymousDeviceModel(),
            appVersion = BuildConfig.VERSION_NAME
        )

        researchDataDao.insertDataPoint(dataPoint)
    }
    
    /**
     * Get anonymous device model (no specific identifiers)
     */
    private fun getAnonymousDeviceModel(): String {
        val model = Build.MODEL ?: "Unknown"
        return when {
            model.contains("Pixel", ignoreCase = true) -> "Pixel"
            model.contains("Galaxy", ignoreCase = true) -> "Galaxy"
            model.contains("SM-", ignoreCase = true) -> "Samsung"
            model.contains("Redmi", ignoreCase = true) -> "Redmi"
            model.contains("MI", ignoreCase = true) -> "Xiaomi"
            model.contains("OPPO", ignoreCase = true) -> "OPPO"
            model.contains("Vivo", ignoreCase = true) -> "Vivo"
            model.contains("OnePlus", ignoreCase = true) -> "OnePlus"
            else -> "Other"
        }
    }
    
    /**
     * Collect user feedback on detection accuracy
     */
    suspend fun submitFeedback(scanId: Long, wasAccurate: Boolean, comments: String? = null) {
        val feedback = UserFeedback(
            scanId = scanId,
            wasAccurate = wasAccurate,
            comments = comments,
            timestamp = System.currentTimeMillis()
        )
        
        userFeedbackDao.insertFeedback(feedback)
        
        // Update the corresponding research data point with accuracy
        // Note: In the DAO, we'll try to find the most recent research data point for this scan if possible.
        // Assuming research data is inserted close in time to feedback.
        researchDataDao.updateAccuracyForScan(scanId, wasAccurate)
    }
    
    /**
     * Generate research statistics for paper
     */
    suspend fun getResearchStatistics(): ResearchStatistics = withContext(Dispatchers.IO) {
        val totalScans = researchDataDao.getTotalScans()
        val totalTechniques = researchDataDao.getTotalTechniqueDetections()
        val uniqueTechniques = researchDataDao.getUniqueTechniqueCount()
        val avgRisk = researchDataDao.getAverageRiskScore()
        
        // Calculate accuracy from user feedback
        val totalFeedback = userFeedbackDao.getTotalFeedbackCount()
        val accurateFeedback = userFeedbackDao.getAccurateFeedbackCount()
        val detectionRate = if (totalFeedback > 0) 
            accurateFeedback.toFloat() / totalFeedback else 0f
        
        // Get top techniques
        val rawTopTechs = mitreDao.getTopTechniques(limit = 20)
        val topTechs = mutableListOf<TechniqueStat>()
        for (stat in rawTopTechs) {
            val feedbackForTech = userFeedbackDao.getAccuracyForTechnique(stat.techniqueId)
            topTechs.add(
                TechniqueStat(
                    techniqueId = stat.techniqueId,
                    techniqueName = stat.techniqueName,
                    detectionCount = stat.count,
                    accuracy = feedbackForTech?.let { 
                        if (it.total > 0) it.accurate.toFloat() / it.total else null
                    }
                )
            )
        }
        
        // Get risk distribution
        val riskDistRaw = researchDataDao.getRiskDistributionRaw()
        val riskDist = riskDistRaw.associate { it.riskLevel to it.count }
        
        // Get monthly trends
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -1)
        val yearAgo = calendar.timeInMillis
        
        val monthlyData = researchDataDao.getMonthlyStats(yearAgo)
        val monthlyTrends = mutableListOf<MonthlyStat>()
        for (raw in monthlyData) {
            val pointsForMonth = researchDataDao.getDataPointsForMonth(raw.month)
            val uniqueTechs = pointsForMonth.flatMap { it.detectedTechniques }.distinct().size
            
            monthlyTrends.add(
                MonthlyStat(
                    month = raw.month,
                    scanCount = raw.scanCount,
                    averageRisk = raw.avgRisk,
                    newTechniques = uniqueTechs
                )
            )
        }
        
        ResearchStatistics(
            totalScans = totalScans,
            totalTechniques = totalTechniques,
            uniqueTechniques = uniqueTechniques,
            averageRiskScore = avgRisk,
            detectionRate = detectionRate,
            falsePositiveRate = 1f - detectionRate,
            topTechniques = topTechs,
            riskDistribution = riskDist,
            monthlyTrends = monthlyTrends
        )
    }
    
    /**
     * Export research data for paper (improved CSV format)
     * - Human-readable ISO timestamps (2026-03-09 04:30:00)
     * - Android version name instead of API level
     * - Accuracy shown as Correct / Incorrect / Pending
     * - Summary statistics block at the bottom
     */
    suspend fun exportResearchData(): File? = withContext(Dispatchers.IO) {
        try {
            val dataPoints = researchDataDao.getAllDataPoints()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val exportedAt = dateFormat.format(Date())
            val file = File(
                context.getExternalFilesDir(null),
                "ShadowInspect_Research_${android.os.Build.MODEL.replace(" ", "_")}_${System.currentTimeMillis()}.csv"
            )

            file.bufferedWriter().use { writer ->
                // ---- Metadata header ----
                writer.write("# ShadowInspect Research Dataset\n")
                writer.write("# Exported: $exportedAt\n")
                writer.write("# App Version: ${BuildConfig.VERSION_NAME}\n")
                writer.write("# Total Records: ${dataPoints.size}\n")
                writer.write("#\n")

                // ---- Column headers ----
                writer.write(
                    listOf(
                        "Scan Date & Time",
                        "Scan ID",
                        "Scan Type",
                        "App Category",
                        "Risk Score (0-100)",
                        "Risk Level",
                        "Technique Count",
                        "Detected MITRE Techniques",
                        "MITRE Tactics",
                        "Dangerous Permission Count",
                        "Dangerous Permissions",
                        "Debuggable",
                        "VirusTotal Verdict",
                        "Detection Accuracy",
                        "Android Version",
                        "Device Category",
                        "App Version"
                    ).joinToString(",") + "\n"
                )

                // ---- Data rows ----
                dataPoints.forEach { point ->
                    val readableDate = dateFormat.format(Date(point.timestamp))
                    val scanIdStr = point.scanId?.toString() ?: "N/A"
                    val techniques = if (point.detectedTechniques.isEmpty()) "None"
                    else "\"${point.detectedTechniques.joinToString("; ")}\""
                    val tactics = if (point.detectedTactics.isEmpty()) "None"
                    else "\"${point.detectedTactics.joinToString("; ")}\""
                    val dangerousPerms = if (point.dangerousPermissions.isEmpty()) "None"
                    else "\"${point.dangerousPermissions.joinToString("; ")}\""
                    val accuracyStr = when (point.detectionAccuracy) {
                        true  -> "Correct"
                        false -> "Incorrect"
                        null  -> "Pending"
                    }
                    val androidName = androidApiToName(point.androidVersion)

                    val line = listOf(
                        readableDate,
                        scanIdStr,
                        point.scanType,
                        point.appCategory,
                        point.riskScore.toString(),
                        point.riskLevel,
                        point.techniqueCount.toString(),
                        techniques,
                        tactics,
                        point.dangerousPermissionCount.toString(),
                        dangerousPerms,
                        if (point.isDebuggable) "Yes" else "No",
                        point.virusTotalVerdict,
                        accuracyStr,
                        androidName,
                        point.deviceModel,
                        point.appVersion
                    ).joinToString(",")

                    writer.write(line + "\n")
                }

                // ---- Summary statistics ----
                writer.write("\n")
                writer.write("# SUMMARY STATISTICS\n")
                val totalFeedback = dataPoints.count { it.detectionAccuracy != null }
                val correct = dataPoints.count { it.detectionAccuracy == true }
                val incorrect = dataPoints.count { it.detectionAccuracy == false }
                val pending = dataPoints.count { it.detectionAccuracy == null }
                val avgRisk = if (dataPoints.isNotEmpty()) dataPoints.map { it.riskScore }.average() else 0.0
                val accuracy = if (totalFeedback > 0) (correct.toDouble() / totalFeedback * 100) else 0.0

                writer.write("Total Scans,${dataPoints.size}\n")
                writer.write("Average Risk Score,${"%.1f".format(avgRisk)}\n")
                writer.write("Correct Detections,$correct\n")
                writer.write("Incorrect Detections,$incorrect\n")
                writer.write("Pending Review,$pending\n")
                writer.write("Detection Accuracy (when reviewed),${"%.1f".format(accuracy)}%\n")

                val byRisk = dataPoints.groupBy { it.riskLevel }
                writer.write("\n# RISK DISTRIBUTION\n")
                byRisk.entries.sortedByDescending { it.value.size }.forEach { (level, points) ->
                    writer.write("$level,${points.size}\n")
                }

                val allTechs = dataPoints.flatMap { it.detectedTechniques }
                val topTechs = allTechs.groupBy { it }.mapValues { it.value.size }
                    .entries.sortedByDescending { it.value }.take(10)
                if (topTechs.isNotEmpty()) {
                    writer.write("\n# TOP 10 MITRE TECHNIQUES\n")
                    writer.write("Technique ID,Detection Count\n")
                    topTechs.forEach { (id, count) -> writer.write("$id,$count\n") }
                }
            }

            file
        } catch (e: Exception) {
            null
        }
    }

    /** Maps API level integer to a human-readable Android version name */
    private fun androidApiToName(apiLevel: Int): String = when (apiLevel) {
        26, 27 -> "Android 8.x (API $apiLevel)"
        28     -> "Android 9 (API $apiLevel)"
        29, 30 -> "Android 10/11 (API $apiLevel)"
        31, 32 -> "Android 12 (API $apiLevel)"
        33     -> "Android 13 (API $apiLevel)"
        34     -> "Android 14 (API $apiLevel)"
        35     -> "Android 15 (API $apiLevel)"
        else   -> "Android (API $apiLevel)"
    }
    
    /**
     * Generate LaTeX tables for paper
     */
    suspend fun generateLatexTables(): String = withContext(Dispatchers.IO) {
        val stats = getResearchStatistics()
        val sb = StringBuilder()
        
        // Table 1: Overall Statistics
        sb.append("\\begin{table}[h]\n")
        sb.append("\\centering\n")
        sb.append("\\caption{Overall Detection Statistics}\n")
        sb.append("\\begin{tabular}{|l|r|}\n")
        sb.append("\\hline\n")
        sb.append("Metric & Value \\\\\n")
        sb.append("\\hline\n")
        sb.append("Total Scans & ${stats.totalScans} \\\\\n")
        sb.append("Total Techniques Detected & ${stats.totalTechniques} \\\\\n")
        sb.append("Unique Techniques & ${stats.uniqueTechniques} \\\\\n")
        sb.append("Average Risk Score & ${"%.2f".format(stats.averageRiskScore)} \\\\\n")
        sb.append("Detection Accuracy & ${"%.2f".format(stats.detectionRate * 100)}\\% \\\\\n")
        sb.append("False Positive Rate & ${"%.2f".format(stats.falsePositiveRate * 100)}\\% \\\\\n")
        sb.append("\\hline\n")
        sb.append("\\end{tabular}\n")
        sb.append("\\end{table}\n\n")
        
        // Table 2: Top Techniques
        sb.append("\\begin{table}[h]\n")
        sb.append("\\centering\n")
        sb.append("\\caption{Top 10 Detected MITRE Techniques}\n")
        sb.append("\\begin{tabular}{|l|l|r|c|}\n")
        sb.append("\\hline\n")
        sb.append("ID & Name & Count & Accuracy \\\\\n")
        sb.append("\\hline\n")
        
        stats.topTechniques.take(10).forEach { tech ->
            sb.append("${tech.techniqueId} & ${tech.techniqueName} & ${tech.detectionCount} & ")
            sb.append(if (tech.accuracy != null) "${"%.2f".format(tech.accuracy!! * 100)}\\%" else "--")
            sb.append(" \\\\\n")
        }
        
        sb.append("\\hline\n")
        sb.append("\\end{tabular}\n")
        sb.append("\\end{table}\n")
        
        sb.toString()
    }
}
