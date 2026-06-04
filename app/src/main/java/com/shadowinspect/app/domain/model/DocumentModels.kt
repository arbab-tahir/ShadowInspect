package com.shadowinspect.app.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.shadowinspect.app.data.db.converter.AppConverters
import java.util.Date

/**
 * Types of documents that can be scanned
 */
enum class DocumentType {
    PDF,
    DOCX,
    XLSX,
    PPTX,
    DOC,
    XLS,
    PPT,
    TXT,
    RTF,
    ODT,
    UNKNOWN
}

/**
 * Threat categories for documents — comprehensive forensic classification
 */
enum class DocumentThreatType {
    MALICIOUS_MACRO,
    EMBEDDED_SCRIPT,
    SUSPICIOUS_URL,
    PHISHING_CONTENT,
    SENSITIVE_DATA,
    ENCRYPTED_CONTENT,
    OUTDATED_FORMAT,
    METADATA_LEAK,
    EMBEDDED_FILE_DROPPER,  // File hidden inside document (e.g., .exe inside .docx)
    HIDDEN_TEXT,            // White-on-white text used to evade scanners
    FORENSIC_ANOMALY,       // Time-stomping or impossible timestamp combinations
    EXPLOIT_SIGNATURE,      // Matches known CVE exploit pattern
    UNSIGNED_MACRO,         // Macros present but no digital signature
    TIMESTOMP,              // Creation date is AFTER modification date (impossible)
    FUTURE_TIMESTAMP,       // A timestamp is set in the future
    EPOCH_TIMESTAMP,        // Date is at or near Unix epoch (metadata wiped)
    CREDENTIAL_LEAK,        // API key, private key, or secret token found in content
    CRYPTO_ADDRESS,         // Bitcoin/Ethereum wallet address found in content
    EXTERNAL_TEMPLATE,      // Document loads a template from an external URL (dropper vector)
    HIDDEN_SHEET,           // XLSX hidden worksheets (content concealed)
    DOCUMENT_PROTECTION,    // Enforced document protection hiding content from scanners
    PDF_JAVASCRIPT          // JavaScript embedded in PDF (execution risk)
}

/**
 * Document analysis request
 */
data class DocumentAnalysisRequest(
    val filePath: String,
    val fileName: String,
    val fileSize: Long,
    val documentType: DocumentType,
    val mimeType: String
)

/**
 * Document analysis result — comprehensive forensic report stored in Room
 */
@Entity(tableName = "document_scans")
@TypeConverters(AppConverters::class)
data class DocumentAnalysisResult(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // ── File Identity ──────────────────────────────────────────────
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val documentType: DocumentType,
    val mimeType: String,
    val format: String? = null,             // e.g. "PDF", "DOCX", "XLS"
    val version: String? = null,            // App or format version string

    // ── Cryptographic Hashes ───────────────────────────────────────
    val hashSha256: String,
    val hashMd5: String,

    // ── Authorship ────────────────────────────────────────────────
    val author: String? = null,
    val creator: String? = null,
    val producer: String? = null,
    val company: String? = null,            // Organisation that created the doc
    val manager: String? = null,            // Manager field from Office metadata
    val lastModifiedBy: String? = null,     // Last editor's username

    // ── Document Identity / Categorisation ────────────────────────
    val title: String? = null,
    val subject: String? = null,
    val keywords: List<String> = emptyList(),
    val language: String? = null,           // BCP-47 language tag e.g. "en-US"

    // ── Content Statistics ────────────────────────────────────────
    val pageCount: Int? = null,
    val wordCount: Int? = null,
    val characterCount: Int? = null,
    val paragraphCount: Int? = null,
    val tableCount: Int? = null,
    val slideCount: Int? = null,            // PPTX/PPT slide count
    val sheetCount: Int? = null,            // XLSX/XLS sheet count
    val embeddedImageCount: Int? = null,

    // ── Forensic Timeline ─────────────────────────────────────────
    val creationDate: Long? = null,
    val modificationDate: Long? = null,
    val revisionNumber: Int? = null,        // Number of save revisions
    val totalEditingTime: Long? = null,     // Total editing time in minutes

    // ── Document Security Settings ────────────────────────────────
    val isEncrypted: Boolean = false,
    val isDigitallySigned: Boolean = false,
    val isProtected: Boolean = false,       // Enforced document protection
    val pdfPermissions: PdfPermissions? = null, // PDF-only DRM flags

    // ── Macro & Script Analysis ───────────────────────────────────
    val hasMacros: Boolean = false,
    val macroCount: Int = 0,
    val maliciousMacros: List<String> = emptyList(),

    // ── Embedded Content ──────────────────────────────────────────
    val embeddedUrls: List<String> = emptyList(),
    val suspiciousUrls: List<String> = emptyList(),
    val totalUrls: Int = 0,
    val embeddedScripts: List<String> = emptyList(),
    val suspiciousScripts: List<String> = emptyList(),
    val pdfJsActions: List<String> = emptyList(),       // JavaScript in PDF
    val embeddedFileCount: Int = 0,                     // Files hidden inside the document
    val embeddedFileNames: List<String> = emptyList(),
    val hasExternalReferences: Boolean = false,
    val externalReferences: List<String> = emptyList(),
    val templateUrl: String? = null,                    // External template URL (dropper risk)
    val hasTrackedChanges: Boolean = false,
    val hasHiddenSheets: Boolean = false,

    // ── Sensitive Data ────────────────────────────────────────────
    val containsSensitiveData: Boolean = false,
    val sensitiveDataTypes: List<String> = emptyList(),
    val sensitiveDataMatches: Map<String, List<String>> = emptyMap(), // type → list of matches

    // ── Forensic Anomalies ────────────────────────────────────────
    val hasForensicAnomaly: Boolean = false,
    val forensicAnomalyDetails: List<String> = emptyList(),
    val hasHiddenText: Boolean = false,

    // ── CVE Exploit Signatures ────────────────────────────────────
    val detectedCves: List<CveMatch> = emptyList(),

    // ── Risk & Threat Intelligence ────────────────────────────────
    val riskScore: Int,
    val riskLevel: String,
    val threatCategories: List<DocumentThreatType>,
    val mitreTechniques: List<String> = emptyList(),

    // ── Report ────────────────────────────────────────────────────
    val explanation: String,
    val recommendations: List<String>,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
    val errorMessage: String? = null
) {
    companion object {
        fun error(fileName: String, message: String): DocumentAnalysisResult {
            return DocumentAnalysisResult(
                fileName = fileName,
                filePath = "",
                fileSize = 0,
                documentType = DocumentType.UNKNOWN,
                mimeType = "",
                hashSha256 = "",
                hashMd5 = "",
                riskScore = 0,
                riskLevel = "ERROR",
                threatCategories = emptyList(),
                explanation = "Analysis failed: $message",
                recommendations = emptyList(),
                isError = true,
                errorMessage = message
            )
        }
    }
}

/**
 * PDF document permission flags extracted from encryption dictionary
 */
data class PdfPermissions(
    val canPrint: Boolean = true,
    val canCopy: Boolean = true,
    val canModify: Boolean = true,
    val canAnnotate: Boolean = true,
    val canFillForms: Boolean = true,
    val canExtractContent: Boolean = true,
    val canAssemble: Boolean = true,
    val canPrintHighQuality: Boolean = true
)

/**
 * A matched CVE exploit signature found inside a document
 */
data class CveMatch(
    val cveId: String,          // e.g. "CVE-2022-30190"
    val name: String,           // e.g. "Follina (MSDT)"
    val description: String,
    val severity: String        // "CRITICAL", "HIGH", "MEDIUM"
)

/**
 * Document metadata extraction result — the rich intermediate object parsers return
 */
data class DocumentMetadata(
    // ── Authorship ──────────────────────────────────────────
    val author: String? = null,
    val creator: String? = null,
    val producer: String? = null,
    val company: String? = null,
    val manager: String? = null,
    val lastModifiedBy: String? = null,

    // ── Identity ────────────────────────────────────────────
    val title: String? = null,
    val subject: String? = null,
    val keywords: List<String> = emptyList(),
    val language: String? = null,

    // ── Dates ───────────────────────────────────────────────
    val creationDate: Long? = null,
    val modificationDate: Long? = null,
    val revisionNumber: Int? = null,
    val totalEditingTime: Long? = null,         // In minutes

    // ── Content Stats ────────────────────────────────────────
    val pageCount: Int? = null,
    val wordCount: Int? = null,
    val characterCount: Int? = null,
    val paragraphCount: Int? = null,
    val tableCount: Int? = null,
    val slideCount: Int? = null,
    val sheetCount: Int? = null,

    // ── Format ───────────────────────────────────────────────
    val format: String? = null,
    val version: String? = null,
    val templateUrl: String? = null,

    // ── Security ─────────────────────────────────────────────
    val isDigitallySigned: Boolean = false,
    val isProtected: Boolean = false,
    val hasHiddenSheets: Boolean = false,
    val hasTrackedChanges: Boolean = false,
    val pdfPermissions: PdfPermissions? = null,

    // ── Embedded Content ─────────────────────────────────────
    val embeddedFileCount: Int = 0,
    val embeddedFileNames: List<String> = emptyList(),
    val embeddedScripts: List<String> = emptyList(),
    val pdfJsActions: List<String> = emptyList(),
    val annotationUrls: List<String> = emptyList()
)

/**
 * URL extraction result
 */
data class ExtractedUrl(
    val url: String,
    val context: String?,
    val pageNumber: Int?,
    val isSuspicious: Boolean = false,
    val threatType: String? = null
)

/**
 * Macro analysis result
 */
data class MacroAnalysis(
    val macroName: String,
    val isMalicious: Boolean,
    val confidenceScore: Int,
    val suspiciousPatterns: List<String>,
    val obfuscationLevel: Int,
    val apiCalls: List<String>
)
