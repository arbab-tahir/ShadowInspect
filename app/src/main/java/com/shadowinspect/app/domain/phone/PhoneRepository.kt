package com.shadowinspect.app.domain.phone

import com.google.gson.Gson
import com.shadowinspect.app.data.db.ScanDao
import com.shadowinspect.app.data.db.ScanEntity
import com.shadowinspect.app.data.phone.AggregatedApiResult
import com.shadowinspect.app.data.phone.ApiUsageTracker
import com.shadowinspect.app.data.phone.LocalSpamDatabase
import com.shadowinspect.app.data.phone.MultiPhoneApiManager
import com.shadowinspect.app.domain.model.LineType
import com.shadowinspect.app.domain.model.PhoneAnalysisResult
import com.shadowinspect.app.domain.model.SpamSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import com.shadowinspect.app.domain.mitre.MitreDetection
import com.shadowinspect.app.domain.mitre.ResearchDataCollector
import com.shadowinspect.app.domain.repository.MitreRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhoneRepository @Inject constructor(
    private val multiApiManager: MultiPhoneApiManager?,
    private val localSpamDb: LocalSpamDatabase,
    private val riskEngine: PhoneRiskEngine,
    private val formatter: PhoneNumberFormatter,
    private val scanDao: ScanDao,
    private val mitreRepository: MitreRepository,
    private val researchDataCollector: ResearchDataCollector,
    private val usageTracker: ApiUsageTracker?,
    private val gson: Gson
) {

    init {
        android.util.Log.d("PHONE_REPO", "PhoneRepository initialized. MultiApi: ${multiApiManager != null}")
    }

    /**
     * Main orchestration function for Phone Number Scanning.
     * Implements the prompt 7.6 steps: normalize -> local -> community -> API -> aggregate -> save.
     */
    suspend fun analyzePhoneNumber(
        number: String,
        countryCode: String? = null
    ): PhoneAnalysisResult = withContext(Dispatchers.IO) {
        
        // a. Clean and normalize number
        val cleanedNumber = cleanNumber(number)

        // b. Check local patterns (unlimited, free)
        val localSpamResult = localSpamDb.checkPatterns(cleanedNumber)
        
        // c. Get community reports from database
        val communityReportsCount = localSpamDb.getReportCount(cleanedNumber)

        // Basic validation offline check before burning API quota
        if (!formatter.validateNumber(cleanedNumber, countryCode)) {
            val invalidResult = PhoneAnalysisResult.error(
                number = cleanedNumber,
                message = "Number failed basic length/character validation for the given country"
            )
            saveToDatabase(invalidResult)
            return@withContext invalidResult
        }

        // d. Try multiple APIs (with quota management & fallback)
        val apiResults = multiApiManager?.validatePhoneWithAllApis(cleanedNumber, countryCode) ?: emptyList()
        val aggregatedApiResult = multiApiManager?.aggregateResults(apiResults) 
            ?: AggregatedApiResult(false, 0, emptyList(), null)

        // e/f/g. Aggregate all sources and calculate risk
        val finalRisk = calculateCombinedRisk(
            apiResult = aggregatedApiResult,
            localScore = localSpamResult.score,
            communityReports = communityReportsCount,
            localPatterns = localSpamResult.matchedPatterns
        )

        val mappedSources = finalRisk.sourcesUsed.map { source ->
            SpamSource(
                sourceName = source,
                score = finalRisk.finalScore / finalRisk.sourcesUsed.size.coerceAtLeast(1),
                reason = when {
                    source == "LocalPatterns" -> "Matched ${localSpamResult.matchedPatterns.size} spam patterns"
                    source == "CommunityReports" -> "Reported by community"
                    else -> "API validation"
                }
            )
        }

        // h. Generate explanation and recommendations
        val explanation = riskEngine.generateExplanation(
            score = finalRisk.finalScore,
            sources = mappedSources,
            patterns = localSpamResult.matchedPatterns
        )
        val recommendations = riskEngine.getRecommendations(riskEngine.getRiskLevel(finalRisk.finalScore))

        val bestData = aggregatedApiResult.aggregatedData

        // Construct final domain model
        val finalResult = PhoneAnalysisResult(
            phoneNumber = cleanedNumber,
            isValid = aggregatedApiResult.isValid || formatter.validateNumber(cleanedNumber),
            formattedNumber = bestData?.formattedNumber ?: formatter.formatNumber(cleanedNumber, countryCode),
            countryCode = bestData?.countryCode ?: formatter.extractCountryCode(cleanedNumber),
            countryName = bestData?.countryName,
            carrier = bestData?.carrier,
            lineType = parseLineType(bestData?.lineType),
            isVoIP = bestData?.isVoIP ?: false,
            isPrepaid = bestData?.isPrepaid,
            spamScore = finalRisk.finalScore,
            riskLevel = riskEngine.getRiskLevel(finalRisk.finalScore),
            spamSources = mappedSources,
            localSpamReports = communityReportsCount,
            explanation = explanation,
            recommendations = recommendations,
            errorMessage = null
        )

        // i. Save to database
        saveToDatabase(finalResult)

        // j. Return result
        return@withContext finalResult
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  Helper Functions
    // ─────────────────────────────────────────────────────────────────────────────

    /** 3. Clean and normalize number for database lookups and API calls. */
    private fun cleanNumber(number: String): String {
        return number.filter { it.isDigit() || it == '+' }
    }

    private fun calculateCombinedRisk(
        apiResult: AggregatedApiResult,
        localScore: Int,
        communityReports: Int,
        localPatterns: List<String>
    ): CombinedRisk {
        val apiWeight = if (apiResult.sourcesUsed.isNotEmpty()) 0.6 else 0.0
        val localWeight = 0.3
        val communityWeight = 0.1
        
        var totalScore = 0
        val sourcesUsed = mutableListOf<String>()
        
        if (apiWeight > 0) {
            val apiScore = if (!apiResult.isValid || apiResult.aggregatedData?.isVoIP == true) 100 else 0
            totalScore += (apiScore * apiWeight).toInt()
            sourcesUsed.addAll(apiResult.sourcesUsed)
        }
        
        totalScore += (localScore * localWeight).toInt()
        if (localScore > 0) sourcesUsed.add("LocalPatterns")
        
        val communityScore = (communityReports * 10).coerceAtMost(50)
        totalScore += (communityScore * communityWeight).toInt()
        if (communityReports > 0) sourcesUsed.add("CommunityReports")
        
        return CombinedRisk(
            finalScore = totalScore.coerceIn(0, 100),
            sourcesUsed = sourcesUsed.distinct(),
            contributingFactors = localPatterns
        )
    }

    private fun parseLineType(type: String?): LineType {
        if (type == null) return LineType.UNKNOWN
        return when (type.lowercase()) {
            "mobile" -> LineType.MOBILE
            "landline" -> LineType.LANDLINE
            "voip", "virtual" -> LineType.VOIP
            "toll_free" -> LineType.TOLL_FREE
            "premium" -> LineType.PREMIUM
            else -> LineType.UNKNOWN
        }
    }
    
    data class CombinedRisk(
        val finalScore: Int,
        val sourcesUsed: List<String>,
        val contributingFactors: List<String>
    )

    /** 6. Save formatted results to the Room scan history database. */
    private suspend fun saveToDatabase(result: PhoneAnalysisResult) {
        val detailsJson = gson.toJson(result)
        val sourcesJson = gson.toJson(result.spamSources)

        val entity = ScanEntity(
            scanType = ScanEntity.SCAN_TYPE_PHONE,
            target = (result.formattedNumber ?: "").ifBlank { result.phoneNumber ?: "Unknown" },
            riskScore = result.spamScore,
            riskLevel = result.riskLevel ?: "UNKNOWN",
            timestamp = System.currentTimeMillis(),
            detailsJson = detailsJson,
            reportPath = null,
            // Phone-specific fields
            phoneNumber = result.phoneNumber,
            countryCode = result.countryCode,
            carrier = result.carrier,
            lineType = result.lineType.name,
            isVoIP = result.isVoIP,
            phoneSpamScore = result.spamScore,
            validationSources = sourcesJson
        )

        val scanId = scanDao.insert(entity)

        // MITRE Analysis for Phone
        val spamIndicators = result.spamSources.map { "${it.sourceName}: ${it.reason}" }
        val mitreDetections = mitreRepository.analyzePhoneForMitre(
            scanId = scanId,
            phoneNumber = result.phoneNumber,
            spamIndicators = spamIndicators
        )

        if (mitreDetections.isNotEmpty()) {
            mitreRepository.saveMitreDetections(mitreDetections)
            
            // Track for research
            mitreDetections.forEach { detection ->
                researchDataCollector.trackDetection(
                    techniqueId = detection.techniqueId,
                    tactic = detection.tactic,
                    confidenceScore = detection.confidenceScore,
                    riskLevel = result.riskLevel ?: "UNKNOWN"
                )
            }
        }
    }

    /** 7. Expose recent phone scans to the UI. */
    fun getRecentPhoneScans(): Flow<List<PhoneAnalysisResult>> {
        return scanDao.getScansByTypeFlow("PHONE").map { entities ->
            entities.mapNotNull { entity ->
                try {
                    gson.fromJson(entity.detailsJson, PhoneAnalysisResult::class.java)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
}
