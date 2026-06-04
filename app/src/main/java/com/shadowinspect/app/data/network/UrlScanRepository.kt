package com.shadowinspect.app.data.network

import com.shadowinspect.app.data.db.ScanEntity
import com.shadowinspect.app.data.scan.ScanRepository
import com.shadowinspect.app.data.remote.api.UrlScanApi
import com.shadowinspect.app.data.remote.api.VirusTotalApi
import com.shadowinspect.app.data.remote.model.UrlScanSubmissionRequest
import com.shadowinspect.app.di.UrlScanApiKey
import com.shadowinspect.app.domain.model.UrlScanResult
import com.shadowinspect.app.domain.util.RiskScoreEngine
import com.google.gson.Gson
import kotlinx.coroutines.delay
import com.shadowinspect.app.domain.util.GeminiSummaryEngine
import com.shadowinspect.app.domain.mitre.MitreDetection
import com.shadowinspect.app.domain.mitre.ResearchDataCollector
import com.shadowinspect.app.domain.repository.MitreRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UrlScanRepository @Inject constructor(
    private val vtApi: VirusTotalApi,
    private val urlscanApi: UrlScanApi,
    @UrlScanApiKey private val urlscanApiKey: String,
    private val riskScoreEngine: RiskScoreEngine,
    private val scanRepository: ScanRepository,
    private val geminiSummaryEngine: GeminiSummaryEngine,
    private val mitreRepository: MitreRepository,
    private val researchDataCollector: ResearchDataCollector,
    private val gson: Gson
) {
    /**
     * Executes the full URL scanning lifecycle:
     * 1. Submits URL to VirusTotal
     * 2. Waits and polls for analysis report using the analysis ID
     * 3. Calculates risk score using RiskScoreEngine
     * 4. Returns consolidated UrlScanResult
     */
    suspend fun scanUrl(url: String): Result<UrlScanResult> {
        return try {
            // Step 1: Submit to BOTH APIs
            val vtAnalysisId = vtApi.scanUrl(url).data.id
            
            var urlscanUuid: String? = null
            if (urlscanApiKey.isNotBlank()) {
                try {
                    val submission = urlscanApi.submitUrl(
                        urlscanApiKey,
                        UrlScanSubmissionRequest(url = url)
                    )
                    urlscanUuid = submission.uuid
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Step 2: Poll for VT report (Primary source for risk score)
            var vtResult: UrlScanResult? = null
            var attempts = 0
            while (attempts < 20 && vtResult == null) {
                delay(3000)
                val report = vtApi.getAnalysisReport(vtAnalysisId)
                if (report.data.attributes.status == "completed") {
                    val attributes = report.data.attributes
                    val stats = attributes.stats!!
                    val total = stats.malicious + stats.suspicious + stats.undetected + stats.harmless
                    
                    if (total > 0) {
                        // Extract granular engine results
                        val fullEngineMap = attributes.results?.mapValues { 
                            com.shadowinspect.app.domain.model.EngineDetail(
                                category = it.value.category ?: "unknown",
                                result = it.value.result
                            )
                        } ?: emptyMap()

                        val engineMap = fullEngineMap.filter { 
                            it.value.category == "malicious" || it.value.category == "suspicious" 
                        }.mapValues { it.value.result ?: "Detected" }

                        // Enhanced explanation with phishing context
                        val phishingCount = attributes.results?.values?.count { 
                            it.result?.contains("phishing", ignoreCase = true) == true 
                        } ?: 0
                        
                        val score = riskScoreEngine.calculateUrlRisk(stats.malicious, stats.suspicious, total)
                        val level = riskScoreEngine.getRiskLevel(score)
                        
                        var explanation = riskScoreEngine.generateUrlExplanation(score, stats.malicious, total)
                        if (phishingCount > 0) {
                            explanation = "🚨 PHISHING DETECTED! $phishingCount security engines identified this as a phishing attempt. $explanation"
                        }
                        
                        vtResult = UrlScanResult(
                            url = url,
                            scanId = vtAnalysisId,
                            riskScore = score,
                            riskLevel = level,
                            explanation = explanation,
                            engineResults = engineMap,
                            fullEngineResults = fullEngineMap,
                            stats = com.shadowinspect.app.domain.model.UrlAnalysisStats(
                                malicious = stats.malicious,
                                suspicious = stats.suspicious,
                                undetected = stats.undetected,
                                harmless = stats.harmless,
                                timeout = stats.timeout
                            )
                        )
                    }
                }
                attempts++
            }

            if (vtResult == null) return Result.failure(Exception("VirusTotal analysis timed out"))

            // Step 3: Poll for urlscan.io results (Advanced Data)
            var finalResult = vtResult!!
            if (urlscanUuid != null) {
                var urlscanAttempts = 0
                while (urlscanAttempts < 10) {
                    delay(4000)
                    try {
                        val response = urlscanApi.getScanResult(urlscanUuid)
                        // If we reach here, scan is ready
                        val screenshot = response.task.screenshotURL ?: "https://urlscan.io/screenshots/${response.task.uuid}.png"
                        finalResult = vtResult.copy(
                            screenshotUrl = screenshot,
                            pageTitle = response.page.title,
                            server = response.page.server,
                            ipAddress = response.page.ip,
                            country = response.page.country,
                            asnName = response.page.asnname,
                            detectedTech = response.meta?.processors?.wappalyzer?.data?.map { it.app } ?: emptyList(),
                            urlscanScore = response.verdict?.urlscan?.score
                        )
                        break
                    } catch (e: Exception) {
                        // Usually 404 while processing
                    }
                    urlscanAttempts++
                }
            }

            // Step 4: AI Summary (Now with aggregated context)
            val aiSummary = try {
                val enginesStr = if (finalResult.engineResults.isNotEmpty()) {
                    " Engines flagging: ${finalResult.engineResults.keys.take(5).joinToString(", ")}."
                } else ""
                
                val statsStr = "VT: M=${finalResult.stats.malicious}, S=${finalResult.stats.suspicious}.$enginesStr " +
                             if (finalResult.urlscanScore != null) "urlscan Score: ${finalResult.urlscanScore}. " else "" +
                             if (finalResult.detectedTech.isNotEmpty()) "Tech: ${finalResult.detectedTech.take(3).joinToString(", ")}." else ""
                
                geminiSummaryEngine.generateUrlSummary(url, finalResult.riskScore, statsStr)
            } catch (e: Exception) { null }


            val resultWithAi = finalResult.copy(aiSummary = aiSummary)

            // Step 5: Save to local history and perform MITRE analysis
            try {
                val scanId = scanRepository.saveScan(
                    ScanEntity(
                        scanType = "URL",
                        target = url,
                        riskLevel = resultWithAi.riskLevel,
                        riskScore = resultWithAi.riskScore,
                        detailsJson = gson.toJson(resultWithAi)
                    )
                )

                // MITRE Analysis for URL
                val indicators = mutableListOf<String>()
                if (resultWithAi.stats.malicious > 0) indicators.add("malicious_engines")
                if (resultWithAi.explanation.contains("phishing", ignoreCase = true)) indicators.add("phishing")
                
                val mitreDetections = mitreRepository.analyzeUrlForMitre(
                    scanId = scanId,
                    url = url,
                    threatIndicators = indicators
                )
                
                if (mitreDetections.isNotEmpty()) {
                    mitreRepository.saveMitreDetections(mitreDetections)
                    
                    // Track for research
                    mitreDetections.forEach { detection ->
                        researchDataCollector.trackDetection(
                            techniqueId = detection.techniqueId,
                            tactic = detection.tactic,
                            confidenceScore = detection.confidenceScore,
                            riskLevel = resultWithAi.riskLevel
                        )
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }

            Result.success(resultWithAi)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
