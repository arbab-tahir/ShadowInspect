package com.shadowinspect.app.domain.phone

import com.shadowinspect.app.data.phone.LocalSpamDatabase
import com.shadowinspect.app.domain.model.LineType
import com.shadowinspect.app.domain.model.SpamSource
import com.shadowinspect.app.domain.model.isHighRiskLine
import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
//  AggregatedRisk – output of the risk engine
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Fully aggregated risk assessment produced by [PhoneRiskEngine].
 *
 * @param finalScore          Clamped score in [0, 100].
 * @param riskLevel           One of: SAFE, LOW, MEDIUM, HIGH, CRITICAL.
 * @param sourcesUsed         All [SpamSource] entries that contributed.
 * @param contributingFactors Human-readable list of reasons for the score.
 */
data class AggregatedRisk(
    val finalScore: Int,
    val riskLevel: String,
    val sourcesUsed: List<SpamSource>,
    val contributingFactors: List<String>
)

// ─────────────────────────────────────────────────────────────────────────────
//  PhoneValidationResult – lightweight carrier / line-type input
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Minimal validation envelope passed into the risk engine.
 * Populated by whichever validation source (API or offline heuristic) ran first.
 */
data class PhoneValidationResult(
    val isValid: Boolean,
    val lineType: LineType = LineType.UNKNOWN,
    val isVoIP: Boolean = false,
    val isPrepaid: Boolean? = null,
    val carrier: String? = null
)

// ─────────────────────────────────────────────────────────────────────────────
//  PhoneRiskEngine  (Expert System / Inference Engine)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Multi-source risk aggregation engine for phone numbers.
 *
 * Aggregation order (each step is additive):
 * 1. Validity check       — invalid number → immediate CRITICAL (100)
 * 2. Line-type heuristics — VoIP / Premium / Unknown carrier signals
 * 3. Local pattern scores — from [LocalSpamDatabase.checkPatterns]
 * 4. Community reports    — capped contribution from community database
 * 5. Prepaid signal       — moderate risk indicator
 *
 * The engine always clamps the cumulative score to [0, 100].
 */
@Singleton
class PhoneRiskEngine @Inject constructor(
    private val localSpamDb: LocalSpamDatabase
) {

    // ── Scoring constants ─────────────────────────────────────────────────────
    private companion object {
        const val INVALID_NUMBER_SCORE  = 100
        const val VOIP_SCORE            = 30
        const val PREMIUM_SCORE         = 50
        const val PREPAID_SCORE         = 20
        const val UNKNOWN_CARRIER_SCORE = 15
        const val UNKNOWN_LINE_SCORE    = 10
        const val COMMUNITY_PER_REPORT  = 10
        const val COMMUNITY_MAX         = 50
    }

    // ── 1. Main aggregator ────────────────────────────────────────────────────

    /**
     * Aggregates all risk signals into a single [AggregatedRisk].
     *
     * @param validationResult  Carrier / line-type data from validation layer.
     *                          Pass `null` when no validation was possible.
     * @param localSpamScore    Pattern score from [LocalSpamDatabase.checkPatterns].
     * @param communityReports  Raw report count from [LocalSpamDatabase.getReportCount].
     * @param patterns          Matched pattern description strings (for explanation).
     */
    fun calculatePhoneRisk(
        validationResult: PhoneValidationResult?,
        localSpamScore: Int,
        communityReports: Int,
        patterns: List<String>
    ): AggregatedRisk {

        val factors  = mutableListOf<String>()
        val sources  = mutableListOf<SpamSource>()
        var rawScore = 0

        // ── Gate: invalid number → immediate CRITICAL ─────────────────────────
        if (validationResult != null && !validationResult.isValid) {
            factors.add("Invalid or non-existent phone number")
            sources.add(SpamSource("Validation", INVALID_NUMBER_SCORE, "Number failed validation"))
            val level = getRiskLevel(INVALID_NUMBER_SCORE)
            return AggregatedRisk(
                finalScore          = INVALID_NUMBER_SCORE,
                riskLevel           = level,
                sourcesUsed         = sources,
                contributingFactors = factors
            )
        }

        // ── Line-type heuristics ──────────────────────────────────────────────
        if (validationResult != null) {
            var lineScore = 0

            when {
                validationResult.isVoIP ||
                validationResult.lineType == LineType.VOIP -> {
                    lineScore += VOIP_SCORE
                    factors.add("VoIP line — frequently used for spoofing and scams (+$VOIP_SCORE)")
                }
                validationResult.lineType == LineType.PREMIUM -> {
                    lineScore += PREMIUM_SCORE
                    factors.add("Premium-rate line — common in pay-per-call scams (+$PREMIUM_SCORE)")
                }
                validationResult.lineType == LineType.UNKNOWN -> {
                    lineScore += UNKNOWN_LINE_SCORE
                    factors.add("Line type unknown — cannot verify legitimacy (+$UNKNOWN_LINE_SCORE)")
                }
            }

            // Prepaid signal
            if (validationResult.isPrepaid == true) {
                lineScore += PREPAID_SCORE
                factors.add("Prepaid SIM — harder to trace, elevated fraud association (+$PREPAID_SCORE)")
            }

            // Unknown carrier
            if (validationResult.carrier.isNullOrBlank() &&
                validationResult.lineType != LineType.VOIP) {
                lineScore += UNKNOWN_CARRIER_SCORE
                factors.add("Carrier unknown — number may be spoofed or unregistered (+$UNKNOWN_CARRIER_SCORE)")
            }

            if (lineScore > 0) {
                rawScore += lineScore
                sources.add(SpamSource(
                    sourceName = "LineTypeAnalysis",
                    score      = lineScore,
                    reason     = "Line type: ${validationResult.lineType.label}"
                ))
            }
        }

        // ── Local pattern engine ──────────────────────────────────────────────
        if (localSpamScore > 0) {
            rawScore += localSpamScore
            factors.addAll(patterns)
            sources.add(SpamSource(
                sourceName = "LocalPatterns",
                score      = localSpamScore,
                reason     = "${patterns.size} pattern(s) matched"
            ))
        }

        // ── Community reports (capped) ────────────────────────────────────────
        if (communityReports > 0) {
            val communityContribution = (communityReports * COMMUNITY_PER_REPORT)
                .coerceAtMost(COMMUNITY_MAX)
            rawScore += communityContribution
            factors.add("$communityReports community spam report(s) on record (+$communityContribution)")
            sources.add(SpamSource(
                sourceName = "CommunityReports",
                score      = communityContribution,
                reason     = "$communityReports report(s) in local database"
            ))
        }

        val finalScore = rawScore.coerceIn(0, 100)

        return AggregatedRisk(
            finalScore          = finalScore,
            riskLevel           = getRiskLevel(finalScore),
            sourcesUsed         = sources,
            contributingFactors = factors
        )
    }

    // ── 2. Risk level ─────────────────────────────────────────────────────────

    /**
     * Maps a numeric [score] to a risk-level label.
     *
     * | Range  | Level    |
     * |--------|----------|
     * | 80–100 | CRITICAL |
     * | 60–79  | HIGH     |
     * | 40–59  | MEDIUM   |
     * | 20–39  | LOW      |
     * | 0–19   | SAFE     |
     */
    fun getRiskLevel(score: Int): String = when {
        score >= 80 -> "CRITICAL"
        score >= 60 -> "HIGH"
        score >= 40 -> "MEDIUM"
        score >= 20 -> "LOW"
        else        -> "SAFE"
    }

    // ── 3. Explanation ────────────────────────────────────────────────────────

    /**
     * Generates a human-readable, single-string explanation of the verdict.
     *
     * @param score    The final aggregated score.
     * @param sources  [SpamSource] list from [AggregatedRisk.sourcesUsed].
     * @param patterns Matched pattern descriptions.
     */
    fun generateExplanation(
        score: Int,
        sources: List<SpamSource>,
        patterns: List<String>
    ): String {
        val level = getRiskLevel(score)

        val header = when (level) {
            "CRITICAL" -> "🚨 CRITICAL RISK — High probability of fraud or phishing. Interacting with this number could lead to severe financial or personal data loss."
            "HIGH"     -> "⚠️ HIGH RISK — Strongly linked to spam or deceptive practices. Answering could expose you to social engineering or unwanted solicitation."
            "MEDIUM"   -> "👀 MEDIUM RISK — Exhibits unusual traits often used to bypass filters. Be cautious; it might be a masked identity or nuisance caller."
            "LOW"      -> "🛡️ LOW RISK — Minor anomalies detected. Minimal threat, but verify the caller's identity before sharing any information."
            else       -> "✅ SAFE — Clean record. Interacting with this number is considered secure, with no known links to malicious activities."
        }

        val sourcesSummary = if (sources.isEmpty()) "" else {
            val names = sources.joinToString(", ") { "${it.sourceName} (${it.score} pts)" }
            " Sources: $names."
        }

        val patternsSummary = if (patterns.isEmpty()) "" else {
            " Matched patterns: ${patterns.joinToString("; ")}."
        }

        return "$header$sourcesSummary$patternsSummary"
    }

    // ── 4. Recommendations ────────────────────────────────────────────────────

    /**
     * Returns an actionable recommendation list tailored to [riskLevel].
     */
    fun getRecommendations(riskLevel: String): List<String> = when (riskLevel) {
        "CRITICAL" -> listOf(
            "Do not answer or call back this number",
            "Block the number immediately on your device",
            "Report to your national telecommunications authority",
            "If you shared personal information, consider monitoring your accounts",
            "File a report with your local consumer protection agency"
        )
        "HIGH" -> listOf(
            "Avoid calling back — especially if you missed the call",
            "Do not provide personal or financial information",
            "Block the number if you receive repeated calls",
            "Report the number to your carrier's spam line"
        )
        "MEDIUM" -> listOf(
            "Exercise caution before answering or returning this call",
            "Do not share sensitive information without verifying the caller's identity",
            "Search for this number online before responding",
            "Consider letting unknown calls go to voicemail"
        )
        "LOW" -> listOf(
            "Verify the caller's identity if they request information",
            "Be cautious of unsolicited requests for personal data",
            "Let the call go to voicemail if you don't recognise the number"
        )
        else /* SAFE */ -> listOf(
            "Number appears legitimate — no significant risk signals detected",
            "Always stay vigilant with unsolicited calls",
            "You can report this number if you experience suspicious behaviour"
        )
    }
}
