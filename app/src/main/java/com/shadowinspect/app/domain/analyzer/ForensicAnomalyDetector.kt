package com.shadowinspect.app.domain.analyzer

import com.shadowinspect.app.domain.model.DocumentMetadata
import com.shadowinspect.app.domain.model.DocumentType
import javax.inject.Inject
import javax.inject.Singleton

data class ForensicAnomalyReport(
    val anomalies: List<String>,
    val riskPenalty: Int
)

/**
 * Detects forensic anomalies in document metadata:
 *  - Time-stomping (creation date after modification date)
 *  - Future timestamps
 *  - Epoch/zero timestamps (metadata likely wiped)
 *  - Author ↔ company domain mismatch
 */
@Singleton
class ForensicAnomalyDetector @Inject constructor() {

    companion object {
        // 1 Jan 2000 — anything before this is suspicious for a "recently created" document
        private val YEAR_2000_MS = 946_684_800_000L
        // Unix epoch window: 0 ± 1 year
        private val EPOCH_WINDOW_MS = 365L * 24 * 60 * 60 * 1000
    }

    fun detect(metadata: DocumentMetadata, documentType: DocumentType): ForensicAnomalyReport {
        val anomalies = mutableListOf<String>()
        var penalty = 0

        val now          = System.currentTimeMillis()
        val creationDate = metadata.creationDate
        val modDate      = metadata.modificationDate

        // ── 1. Time-Stomp: creation AFTER modification ─────────────
        if (creationDate != null && modDate != null && creationDate > modDate) {
            val diffDays = (creationDate - modDate) / (1000 * 60 * 60 * 24)
            anomalies.add("TIME-STOMP: Creation date is $diffDays day(s) AFTER modification date. " +
                "This is forensically impossible and indicates metadata tampering.")
            penalty += 25
        }

        // ── 2. Future timestamps ────────────────────────────────────
        if (creationDate != null && creationDate > now) {
            anomalies.add("FUTURE CREATION DATE: Document claims to have been created in the future " +
                "(${formatDate(creationDate)}). Indicates metadata manipulation.")
            penalty += 15
        }
        if (modDate != null && modDate > now) {
            anomalies.add("FUTURE MODIFICATION DATE: Last-modified date is set in the future " +
                "(${formatDate(modDate)}). Strong indicator of metadata forgery.")
            penalty += 15
        }

        // ── 3. Epoch-zero timestamps (metadata wiped) ────────────────
        if (creationDate != null && creationDate < EPOCH_WINDOW_MS) {
            anomalies.add("EPOCH TIMESTAMP: Creation date is at or near Unix epoch (Jan 1970). " +
                "This typically means metadata has been deliberately wiped or reset.")
            penalty += 20
        }
        if (modDate != null && modDate < EPOCH_WINDOW_MS) {
            anomalies.add("EPOCH TIMESTAMP: Modification date is at or near Unix epoch (Jan 1970). " +
                "Metadata may have been stripped by a sanitisation tool or malicious actor.")
            penalty += 10
        }

        // ── 4. Author / company domain mismatch ─────────────────────
        val author  = metadata.author?.lowercase() ?: ""
        val company = metadata.company?.lowercase() ?: ""
        if (author.contains("@") && company.isNotBlank()) {
            val emailDomain   = author.substringAfter("@").substringBefore(".")
            val companyTokens = company.split(" ", "-", "_", ".", ",")
                .map { it.lowercase().trim() }
                .filter { it.length > 2 }
            if (companyTokens.isNotEmpty() && companyTokens.none { emailDomain.contains(it) || it.contains(emailDomain) }) {
                anomalies.add("AUTHOR/COMPANY MISMATCH: Author email domain '$emailDomain' does not match " +
                    "company '$company'. Document may have been repurposed or spoofed.")
                penalty += 10
            }
        }

        // ── 5. Suspiciously old document with modern edits ───────────
        if (creationDate != null && modDate != null) {
            val ageYears = (now - creationDate) / (1000L * 60 * 60 * 24 * 365)
            val editYears = (now - modDate) / (1000L * 60 * 60 * 24 * 365)
            if (ageYears > 10 && editYears < 1) {
                anomalies.add("SUSPICIOUS AGE: Document claims to have been created over 10 years ago " +
                    "but was modified very recently. Possible document spoofing.")
                penalty += 10
            }
        }

        // ── 6. Impossible revision count ─────────────────────────────
        val revision = metadata.revisionNumber
        if (revision != null && revision > 10_000) {
            anomalies.add("ABNORMAL REVISION COUNT: Revision number is $revision, which is highly unusual. " +
                "May indicate automated document manipulation or revision counter spoofing.")
            penalty += 5
        }

        return ForensicAnomalyReport(
            anomalies   = anomalies,
            riskPenalty = penalty.coerceIn(0, 50)
        )
    }

    private fun formatDate(ms: Long): String {
        return try {
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                .format(java.util.Date(ms))
        } catch (e: Exception) { ms.toString() }
    }
}
