package com.shadowinspect.app.data.phone

import com.shadowinspect.app.data.db.PhoneReportDao
import com.shadowinspect.app.data.db.PhoneReportEntity
import com.shadowinspect.app.domain.model.SpamPattern
import com.shadowinspect.app.domain.model.normalizePhoneNumber
import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
//  SpamResult – per-number pattern check output
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Aggregated result of pattern-based spam analysis for one phone number.
 *
 * @param score            Total pattern score in [0, 100].
 * @param matchedPatterns  Human-readable description of each pattern that fired.
 * @param communityReports Number of local community reports on file.
 */
data class SpamResult(
    val score: Int,
    val matchedPatterns: List<String>,
    val communityReports: Int
)

// ─────────────────────────────────────────────────────────────────────────────
//  LocalSpamDatabase
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Offline-first phone spam intelligence service.
 *
 * Responsibilities:
 * 1. Maintains a static knowledge base of [SpamPattern]s (prefix rules, digit
 *    sequences, country-code heuristics).
 * 2. Runs the inference engine against an input number and returns a [SpamResult].
 * 3. Persists community spam reports using [PhoneReportDao] (Room) — privacy-safe,
 *    never transmitted externally without explicit user consent.
 * 4. Computes a community-weighted spam score from local report counts.
 */
@Singleton
class LocalSpamDatabase @Inject constructor(
    private val phoneReportDao: PhoneReportDao
) {

    // ── 1. Knowledge Base ─────────────────────────────────────────────────────

    /**
     * Static list of [SpamPattern] rules loaded into memory once.
     * Each pattern carries an independent score; the inference engine sums them.
     *
     * Scores are intentionally conservative — aggregation across sources will
     * push the final spam score upward when multiple signals agree.
     */
    fun loadSpamPatterns(): List<SpamPattern> = listOf(

        // ── Repeated-digit sequences (e.g. 5555555555, 1111111111) ────────────
        SpamPattern(
            pattern     = Regex("""(\d)\1{5,}"""),
            score       = 40,
            description = "Contains 6+ consecutive identical digits — common robocall spoofing pattern"
        ),

        // ── Sequential ascending digits (e.g. 123456, 12345678) ──────────────
        SpamPattern(
            pattern     = Regex("""(?:0123|1234|2345|3456|4567|5678|6789){1,}"""),
            score       = 35,
            description = "Contains ascending sequential digits — common in fake/spoofed numbers"
        ),

        // ── Sequential descending digits (e.g. 987654, 654321) ───────────────
        SpamPattern(
            pattern     = Regex("""(?:9876|8765|7654|6543|5432|4321|3210){1,}"""),
            score       = 35,
            description = "Contains descending sequential digits — common in fake/spoofed numbers"
        ),

        // ── Premium-rate prefixes – North America (1900x) ─────────────────────
        SpamPattern(
            pattern     = Regex("""^\+?1900"""),
            score       = 70,
            description = "1900 premium-rate prefix — pay-per-call scam numbers"
        ),

        // ── Premium-rate prefixes – International (0900x) ─────────────────────
        SpamPattern(
            pattern     = Regex("""^\+?\d{1,3}0900"""),
            score       = 65,
            description = "0900 premium-rate prefix — international pay-per-call scams"
        ),

        // ── UK premium-rate (09xx) ─────────────────────────────────────────────
        SpamPattern(
            pattern     = Regex("""^\+?4409\d"""),
            score       = 60,
            description = "UK 09xx premium-rate line — often used for scam services"
        ),

        // ── Known high-risk country codes: Nigeria (+234), Ghana (+233) ────────
        SpamPattern(
            pattern     = Regex("""^\+?(234|233)\d"""),
            score       = 30,
            description = "Originates from a country code with elevated fraud reports (NG/GH)"
        ),

        // ── Moldova (+373) – frequent wangiri/one-ring scam source ─────────────
        SpamPattern(
            pattern     = Regex("""^\+?373\d"""),
            score       = 30,
            description = "Moldova (+373) — frequent source of wangiri (one-ring) scam calls"
        ),

        // ── Kosovo (+383) — common in telemarketing fraud ──────────────────────
        SpamPattern(
            pattern     = Regex("""^\+?383\d"""),
            score       = 25,
            description = "Kosovo (+383) — elevated telemarketing fraud reports"
        ),

        // ── Caribbean high-risk area codes (268, 284, 473, 664, 721, 758, …) ──
        SpamPattern(
            pattern     = Regex("""^\+?1(268|284|473|664|721|758|767|784|809|829|849|876)\d"""),
            score       = 45,
            description = "Caribbean area code — one-ring/wangiri scam hotspot"
        ),

        // ── All-zeros (0000000000) ────────────────────────────────────────────
        SpamPattern(
            pattern     = Regex("""^0+$"""),
            score       = 80,
            description = "Number consists entirely of zeros — invalid/spoofed"
        ),

        // ── Very short numbers (fewer than 7 digits after normalisation) ───────
        SpamPattern(
            pattern     = Regex("""^\+?\d{1,6}$"""),
            score       = 50,
            description = "Unusually short number — may be spoofed or an invalid caller-ID"
        ),

        // ── Numbers beginning with 555 (fictional / test numbers) ─────────────
        SpamPattern(
            pattern     = Regex("""^\+?1555"""),
            score       = 40,
            description = "US 555-xxxx — fictional/reserved range, unlikely real caller"
        ),

        // ── Caller-ID spoofing: starts with 000 or 0000 ──────────────────────
        SpamPattern(
            pattern     = Regex("""^\+?000"""),
            score       = 55,
            description = "Starts with 000 — common caller-ID spoofing pattern"
        ),

        // ── International call-back fraud: +8 codes (satellite/premium) ───────
        SpamPattern(
            pattern     = Regex("""^\+8[78]\d"""),
            score       = 60,
            description = "International (+87/+88) satellite/premium line — call-back fraud source"
        )
    )

    // ── 2. Inference Engine ───────────────────────────────────────────────────

    /**
     * Runs all [SpamPattern]s in the knowledge base against [number] and
     * aggregates a capped [SpamResult].
     *
     * The number is normalised before matching, so formatting differences
     * (" ", "-", "()", etc.) are stripped.
     *
     * @param number Raw phone number string from user input or API.
     * @return [SpamResult] with aggregated score, matched descriptions, and 0 community reports
     *         (community score is fetched separately and merged by the analyzer).
     */
    fun checkPatterns(number: String): SpamResult {
        val normalised = number.normalizePhoneNumber()
        val patterns   = loadSpamPatterns()

        val matched = mutableListOf<String>()
        var total   = 0

        for (pattern in patterns) {
            val contribution = pattern.evaluate(normalised)
            if (contribution > 0) {
                matched.add(pattern.description)
                total += contribution
            }
        }

        return SpamResult(
            score            = total.coerceIn(0, 100),
            matchedPatterns  = matched,
            communityReports = 0   // populated separately in getCommunitySpamScore()
        )
    }

    // ── 3. Community Reports ──────────────────────────────────────────────────

    /**
     * Records a user's spam report for [number].
     *
     * Privacy guarantees:
     * - Only the normalised number and optional [reason] are stored.
     * - No device ID, user account, IP address, or timestamp beyond epoch ms.
     * - Data is never shared externally without explicit user consent.
     *
     * @param number Raw number string (will be normalised before storage).
     * @param reason Short free-text reason chosen by the user; may be empty.
     */
    suspend fun addUserReport(number: String, reason: String) {
        val normalised = number.normalizePhoneNumber()
        phoneReportDao.insertReport(
            PhoneReportEntity(
                normalizedNumber = normalised,
                reason           = reason.trim().take(120)   // cap to avoid abuse
            )
        )
    }

    /**
     * Computes a community-weighted spam score from local reports.
     *
     * Scoring curve (logarithmic to avoid outlier amplification):
     * ```
     *  1 report  →  10 pts
     *  3 reports →  25 pts
     *  5 reports →  35 pts
     * 10 reports →  50 pts
     * 20+ reports → 70 pts  (hard cap)
     * ```
     *
     * @param number Raw number string.
     * @return Score in [0, 70] so pattern-based signals can still outweigh community alone.
     */
    suspend fun getCommunitySpamScore(number: String): Int {
        val normalised = number.normalizePhoneNumber()
        val count      = phoneReportDao.getReportCount(normalised)

        return when {
            count <= 0  ->  0
            count == 1  -> 10
            count <= 3  -> 25
            count <= 5  -> 35
            count <= 10 -> 50
            count <= 20 -> 60
            else        -> 70
        }
    }

    /**
     * Returns all distinct user-supplied reason strings for [number].
     * Used to enrich the analysis explanation with community context.
     */
    suspend fun getCommunityReasons(number: String): List<String> {
        val normalised = number.normalizePhoneNumber()
        return phoneReportDao.getReasons(normalised)
    }

    /**
     * Raw report count for [number], useful for UI badges (e.g. "5 reports").
     */
    suspend fun getReportCount(number: String): Int {
        val normalised = number.normalizePhoneNumber()
        return phoneReportDao.getReportCount(normalised)
    }
}
