package com.shadowinspect.app.domain.model

// ─────────────────────────────────────────────────────────────────────────────
//  Module 7 – Phone Number Scanner  |  Domain Models
// ─────────────────────────────────────────────────────────────────────────────

// ── 1. Line Type Enum ─────────────────────────────────────────────────────────

/**
 * Describes the physical or logical type of a phone line.
 *
 * This drives both display UI (icon/label) and risk heuristics
 * (e.g. VOIP and PREMIUM lines carry higher spam probability).
 */
enum class LineType(val label: String, val spamBias: Int) {
    MOBILE(    label = "Mobile",      spamBias = 0  ),
    LANDLINE(  label = "Landline",    spamBias = 0  ),
    VOIP(      label = "VoIP",        spamBias = 15 ),  // Often used by scammers
    TOLL_FREE( label = "Toll-Free",   spamBias = 5  ),
    PREMIUM(   label = "Premium",     spamBias = 25 ),  // Premium-rate scams
    SATELLITE( label = "Satellite",   spamBias = 5  ),
    PAGER(     label = "Pager",       spamBias = 0  ),
    UNKNOWN(   label = "Unknown",     spamBias = 10 );

    companion object {
        fun fromString(value: String?): LineType = entries.firstOrNull {
            it.name.equals(value, ignoreCase = true)
        } ?: UNKNOWN
    }
}

// ── 2. SpamSource – per-source intelligence entry ────────────────────────────

/**
 * Records the spam verdict contributed by a single intelligence source
 * (e.g. a community database, a REST API, or a local pattern matcher).
 *
 * @param sourceName  Display name of the source.
 * @param score       Contribution score in [0, 100].
 * @param reason      Optional human-readable explanation from the source.
 * @param confidence  Optional confidence level of the source's verdict.
 */
data class SpamSource(
    val sourceName: String,
    val score: Int,
    val reason: String? = null,
    val confidence: Float = 1.0f
) {
    /** Weighted contribution: `score × confidence`, clamped to [0, 100]. */
    val weightedScore: Int
        get() = (score * confidence).toInt().coerceIn(0, 100)
}

// ── 3. SpamPattern – local pattern rule ──────────────────────────────────────

/**
 * A single rule in the local spam-pattern knowledge base.
 *
 * @param pattern     Regex matched against the normalised phone number.
 * @param score       Risk contribution in [0, 100] when the pattern fires.
 * @param description Short description shown in the analysis explanation.
 */
data class SpamPattern(
    val pattern: Regex,
    val score: Int,
    val description: String
) {
    /**
     * Returns [score] if [number] matches [pattern], otherwise 0.
     * [number] should be pre-normalised (digits only, with country code).
     */
    fun evaluate(number: String): Int = if (pattern.containsMatchIn(number)) score else 0
}

// ── 4. PhoneAnalysisResult – main result model ───────────────────────────────

/**
 * The consolidated analysis result for a single phone number.
 *
 * Aggregates intelligence from local patterns, community reports, and
 * (optionally) external API sources into a single [spamScore] and [riskLevel].
 *
 * @param phoneNumber        Raw input number as entered by the user.
 * @param isValid            Whether the number passes E.164 / libphonenumber validation.
 * @param formattedNumber    International format, e.g. "+1 650-253-0000".
 * @param e164Number         E.164 machine-readable format, e.g. "+16502530000".
 * @param countryCode        ISO 3166-1 alpha-2 country code, e.g. "US".
 * @param countryName        Full country name, e.g. "United States".
 * @param carrier            Carrier name, if determinable (may be null for VoIP/MVNO).
 * @param lineType           Physical/logical line classification.
 * @param isVoIP             True when the line is known to use VoIP technology.
 * @param isPrepaid          True when the SIM is prepaid; null when unknown.
 * @param spamScore          Aggregated spam probability in [0, 100].
 * @param riskLevel          One of: SAFE, LOW, MEDIUM, HIGH, CRITICAL.
 * @param spamSources        Individual source verdicts that contributed to [spamScore].
 * @param localSpamReports   Number of community spam reports found locally.
 * @param explanation        Human-readable explanation of the verdict.
 * @param recommendations    Actionable advice list shown in the UI.
 * @param timestamp          Epoch ms when the analysis was completed.
 * @param isError            True when analysis could not be completed.
 * @param errorMessage       Reason for failure when [isError] is true.
 */
data class PhoneAnalysisResult(
    val phoneNumber: String,
    val isValid: Boolean,
    val formattedNumber: String,
    val e164Number: String = "",
    val countryCode: String? = null,
    val countryName: String? = null,
    val carrier: String? = null,
    val lineType: LineType = LineType.UNKNOWN,
    val isVoIP: Boolean = false,
    val isPrepaid: Boolean? = null,
    val spamScore: Int,                         // 0-100 aggregated
    val riskLevel: String? = null,              // SAFE | LOW | MEDIUM | HIGH | CRITICAL | null
    val spamSources: List<SpamSource> = emptyList(),
    val localSpamReports: Int = 0,
    val explanation: String = "",
    val recommendations: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
    val errorMessage: String? = null
) {
    companion object {

        /** Convenience factory for failure states. */
        fun error(number: String, message: String): PhoneAnalysisResult =
            PhoneAnalysisResult(
                phoneNumber     = number,
                isValid         = false,
                formattedNumber = number,
                e164Number      = "",
                countryCode     = null,
                countryName     = null,
                carrier         = null,
                lineType        = LineType.UNKNOWN,
                isVoIP          = false,
                isPrepaid       = null,
                spamScore       = 0,
                riskLevel       = "ERROR",
                spamSources     = emptyList(),
                localSpamReports = 0,
                explanation     = message,
                recommendations = emptyList(),
                timestamp       = System.currentTimeMillis(),
                isError         = true,
                errorMessage    = message
            )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Extension Functions
// ─────────────────────────────────────────────────────────────────────────────

// ── PhoneAnalysisResult extensions ───────────────────────────────────────────

/** Returns true when this result represents a successful (non-error) analysis. */
val PhoneAnalysisResult.isSuccess: Boolean
    get() = !isError

/** Emoji badge matching the risk level, used in summary UIs. */
val PhoneAnalysisResult.riskBadge: String
    get() = when (riskLevel?.uppercase()) {
        "CRITICAL" -> "🚨"
        "HIGH"     -> "⚠️"
        "MEDIUM"   -> "👀"
        "LOW"      -> "🛡️"
        "SAFE"     -> "✅"
        else       -> "❓"
    }

/**
 * Returns a single human-readable summary line, suitable for list items
 * or notification text.
 *
 * Example: "⚠️ HIGH – +1 650-253-0000 (United States, Mobile)"
 */
val PhoneAnalysisResult.summaryLine: String
    get() {
        if (isError) return "❌ Analysis failed: ${errorMessage ?: "Unknown error"}"
        val location = listOfNotNull(countryName, lineType.label).joinToString(", ")
        val levelTxt = riskLevel ?: "UNKNOWN"
        return "$riskBadge $levelTxt – $formattedNumber" +
               if (location.isNotBlank()) " ($location)" else ""
    }

/**
 * Returns the dominant [SpamSource] — the one with the highest [SpamSource.weightedScore].
 * Returns null when [spamSources] is empty.
 */
val PhoneAnalysisResult.dominantSource: SpamSource?
    get() = spamSources.maxByOrNull { it.weightedScore }

// ── String extensions (phone number formatting) ───────────────────────────────

/**
 * Strips all non-digit characters except a leading '+', producing a
 * compact representation useful for pattern matching and storage.
 *
 * Example: "(+44) 020-7946 0958" → "+442079460958"
 */
fun String.normalizePhoneNumber(): String {
    val stripped = this.filter { it.isDigit() || it == '+' }
    return if (stripped.startsWith('+')) stripped
           else stripped.trimStart('0')      // drop leading local zeros
}

/**
 * Returns true when the string looks plausibly like a phone number —
 * contains between 7 and 15 digits (E.164 range) after normalisation.
 * This is a cheap pre-flight check before hitting a validator or API.
 */
fun String.looksLikePhoneNumber(): Boolean {
    val digits = this.filter { it.isDigit() }
    return digits.length in 7..15
}

/**
 * Masks the middle portion of a phone number for privacy-safe display.
 *
 * Example: "+16502530000" → "+1650****0000"
 */
fun String.maskPhoneNumber(): String {
    val norm = this.normalizePhoneNumber()
    if (norm.length < 8) return norm
    val visible = 4
    val mask = "*".repeat((norm.length - visible * 2).coerceAtLeast(2))
    return norm.take(visible) + mask + norm.takeLast(visible)
}

// ── LineType extensions ───────────────────────────────────────────────────────

/** Returns true for line types that carry an inherently elevated spam risk. */
val LineType.isHighRiskLine: Boolean
    get() = this == LineType.VOIP || this == LineType.PREMIUM || this == LineType.UNKNOWN

/** Icon character / emoji used in result cards. */
val LineType.icon: String
    get() = when (this) {
        LineType.MOBILE    -> "📱"
        LineType.LANDLINE  -> "☎️"
        LineType.VOIP      -> "🌐"
        LineType.TOLL_FREE -> "📞"
        LineType.PREMIUM   -> "💰"
        LineType.SATELLITE -> "🛰️"
        LineType.PAGER     -> "📟"
        LineType.UNKNOWN   -> "❓"
    }
