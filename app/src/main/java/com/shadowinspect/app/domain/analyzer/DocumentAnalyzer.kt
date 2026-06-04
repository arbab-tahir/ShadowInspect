package com.shadowinspect.app.domain.analyzer

import android.content.Context
import android.util.Log
import com.shadowinspect.app.data.analysis.office.OfficeParser
import com.shadowinspect.app.data.analysis.pdf.PdfParser
import com.shadowinspect.app.data.analysis.text.TextParser
import com.shadowinspect.app.domain.model.*
import com.shadowinspect.app.domain.util.RiskScoreEngine
import com.shadowinspect.app.domain.mitre.DocumentMitreMapper
import com.shadowinspect.app.data.analysis.office.DocumentTooLargeException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentAnalyzer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pdfParser: PdfParser,
    private val officeParser: OfficeParser,
    private val textParser: TextParser,
    private val riskEngine: RiskScoreEngine,
    private val macroAnalyzer: MacroAnalyzer,
    private val mitreMapper: DocumentMitreMapper,
    private val forensicDetector: ForensicAnomalyDetector,
    private val cveDetector: CveSignatureDetector
) {
    private val tag = "DocumentAnalyzer"
    private val parsers = listOf(pdfParser, officeParser, textParser)
    private val analysisTimeoutMs = 60_000L

    suspend fun analyzeDocument(file: File): DocumentAnalysisResult = withContext(Dispatchers.IO) {
        withTimeoutOrNull(analysisTimeoutMs) { analyzeDocumentInternal(file) }
            ?: DocumentAnalysisResult.error(file.name, "Analysis timed out after ${analysisTimeoutMs/1000}s.")
    }

    private suspend fun analyzeDocumentInternal(file: File): DocumentAnalysisResult {
        try {
            val documentType = detectDocumentType(file)
            val parser = parsers.find { it.canHandle(documentType) }
                ?: return DocumentAnalysisResult.error(file.name, "Unsupported document type: $documentType")

            val sha256 = calculateHash(file, "SHA-256")
            val md5    = calculateHash(file, "MD5")

            // ── Text + Sensitive Data ─────────────────────────────
            val text = try {
                withTimeoutOrNull(10_000L) { parser.extractText(file) } ?: ""
            } catch (e: Exception) { "" }

            val sensitiveResult  = detectSensitiveData(text)
            val sensitiveTypes   = sensitiveResult.keys.toList()
            val sensitiveCounts  = sensitiveResult

            // ── Metadata ──────────────────────────────────────────
            val metadata = try {
                withTimeoutOrNull(15_000L) { parser.parseMetadata(file) } ?: DocumentMetadata()
            } catch (e: Exception) { Log.e(tag, "Metadata: ${e.message}"); DocumentMetadata() }

            // ── URLs ──────────────────────────────────────────────
            val urls = try {
                withTimeoutOrNull(10_000L) { parser.extractUrls(file) } ?: emptyList()
            } catch (e: Exception) { emptyList() }
            val suspiciousUrls = urls.filter { it.isSuspicious }

            // ── Annotation URLs (merge, de-duplicate) ─────────────
            val allAnnotUrls = metadata.annotationUrls.map {
                ExtractedUrl(it, "PDF Annotation", null, isSuspiciousUrl(it))
            }
            
            // ── Text URLs (Regex extraction from content) ────────
            val textUrls = detectUrlsFromText(text).map {
                ExtractedUrl(it, "Document Content", null, isSuspiciousUrl(it))
            }

            val mergedUrls = (urls + allAnnotUrls + textUrls).distinctBy { it.url }
            val mergedSuspicious = mergedUrls.filter { it.isSuspicious }

            // ── Macros ────────────────────────────────────────────
            val basicHasMacros = try {
                withTimeoutOrNull(5_000L) { parser.hasMacros(file) } ?: false
            } catch (e: Exception) { false }

            val macroResult = try {
                if (documentType in listOf(DocumentType.DOCX, DocumentType.XLSX, DocumentType.PPTX,
                        DocumentType.DOC, DocumentType.XLS, DocumentType.PPT)) {
                    withTimeoutOrNull(10_000L) { macroAnalyzer.analyzeMacros(file) }
                } else null
            } catch (e: Exception) { Log.e(tag, "Macros: ${e.message}"); null }

            val hasMacros   = basicHasMacros || (macroResult?.hasMacros == true)
            val macroCount  = macroResult?.macroCount ?: 0
            val maliciousMacroList = if (macroResult?.isMalicious == true)
                macroResult.suspiciousPatterns else emptyList()

            // ── CVE Signatures ────────────────────────────────────
            val cveMatches = try {
                withTimeoutOrNull(5_000L) { cveDetector.scan(text, documentType) } ?: emptyList()
            } catch (e: Exception) { emptyList() }

            // ── Forensic Anomalies ────────────────────────────────
            val anomalyReport = try {
                forensicDetector.detect(metadata, documentType)
            } catch (e: Exception) { ForensicAnomalyReport(emptyList(), 0) }

            // ── Encryption ────────────────────────────────────────
            val isEncrypted = try { parser.isEncrypted(file) } catch (e: Exception) { false }

            // ── Hidden text (PDF: raw size vs. text size delta) ───
            val hasHiddenText = try {
                if (documentType == DocumentType.PDF && text.isNotBlank()) {
                    val rawSize = file.length()
                    val textSize = text.length.toLong()
                    rawSize > 0 && textSize.toDouble() / rawSize < 0.001
                } else false
            } catch (e: Exception) { false }

            // ── Risk Score ────────────────────────────────────────
            var riskScore = calculateRiskScore(hasMacros, mergedSuspicious.size, sensitiveTypes.isNotEmpty(), isEncrypted)
            macroResult?.let {
                if (it.isMalicious) riskScore += 30
                if (it.autoExecMacros.isNotEmpty()) riskScore += 15
                if (it.obfuscationLevel > 7) riskScore += 20
            }
            riskScore += anomalyReport.riskPenalty
            cveMatches.forEach { cve ->
                riskScore += when (cve.severity) {
                    "CRITICAL" -> 25; "HIGH" -> 15; "MEDIUM" -> 10; else -> 5
                }
            }
            if (metadata.embeddedFileCount > 0) riskScore += 20
            if (metadata.pdfJsActions.isNotEmpty()) riskScore += 15
            if (metadata.templateUrl != null) riskScore += 10
            if (metadata.hasHiddenSheets) riskScore += 10
            if (hasHiddenText) riskScore += 15
            riskScore = riskScore.coerceIn(0, 100)

            // ── Threat Categories ─────────────────────────────────
            val threats = buildThreatCategories(
                hasMacros, mergedSuspicious, sensitiveTypes, isEncrypted,
                macroResult, cveMatches, anomalyReport, metadata, hasHiddenText
            )

            // ── MITRE Mapping ─────────────────────────────────────
            val mitreDetailed = mitreMapper.mapThreatsToMitre(
                threatTypes   = threats,
                macroAnalysis = macroResult,
                suspiciousUrls = mergedSuspicious.map { it.url }
            )
            val mitreTechniques = mitreDetailed.map { "${it.id} — ${it.name}: ${it.description}" }

            val explanation     = generateExplanation(riskScore, threats, mergedSuspicious.size, macroResult, mitreDetailed, cveMatches, anomalyReport)
            val recommendations = generateRecommendations(threats, macroResult)

            return DocumentAnalysisResult(
                fileName          = file.name,
                filePath          = file.absolutePath,
                fileSize          = file.length(),
                documentType      = documentType,
                mimeType          = getMimeType(file),
                format            = metadata.format,
                version           = metadata.version,
                hashSha256        = sha256,
                hashMd5           = md5,
                author            = metadata.author,
                creator           = metadata.creator,
                producer          = metadata.producer,
                company           = metadata.company,
                manager           = metadata.manager,
                lastModifiedBy    = metadata.lastModifiedBy,
                title             = metadata.title,
                subject           = metadata.subject,
                keywords          = metadata.keywords,
                language          = metadata.language,
                pageCount         = metadata.pageCount,
                wordCount         = metadata.wordCount,
                characterCount    = metadata.characterCount,
                paragraphCount    = metadata.paragraphCount,
                tableCount        = metadata.tableCount,
                slideCount        = metadata.slideCount,
                sheetCount        = metadata.sheetCount,
                creationDate      = metadata.creationDate,
                modificationDate  = metadata.modificationDate,
                revisionNumber    = metadata.revisionNumber,
                totalEditingTime  = metadata.totalEditingTime,
                isEncrypted       = isEncrypted,
                isDigitallySigned = metadata.isDigitallySigned,
                isProtected       = metadata.isProtected,
                pdfPermissions    = metadata.pdfPermissions,
                hasMacros         = hasMacros,
                macroCount        = macroCount,
                maliciousMacros   = maliciousMacroList,
                embeddedUrls      = mergedUrls.map { it.url },
                suspiciousUrls    = mergedSuspicious.map { it.url },
                totalUrls         = mergedUrls.size,
                embeddedScripts   = metadata.embeddedScripts,
                pdfJsActions      = metadata.pdfJsActions,
                embeddedFileCount = metadata.embeddedFileCount,
                embeddedFileNames = metadata.embeddedFileNames,
                hasExternalReferences = metadata.annotationUrls.isNotEmpty() || metadata.templateUrl != null,
                externalReferences = metadata.annotationUrls,
                templateUrl       = metadata.templateUrl,
                hasTrackedChanges = metadata.hasTrackedChanges,
                hasHiddenSheets   = metadata.hasHiddenSheets,
                containsSensitiveData = sensitiveTypes.isNotEmpty(),
                sensitiveDataTypes    = sensitiveTypes,
                sensitiveDataMatches  = sensitiveCounts,
                hasForensicAnomaly    = anomalyReport.anomalies.isNotEmpty(),
                forensicAnomalyDetails = anomalyReport.anomalies,
                hasHiddenText         = hasHiddenText,
                detectedCves          = cveMatches,
                riskScore             = riskScore,
                riskLevel             = riskEngine.getRiskLevel(riskScore),
                threatCategories      = threats,
                mitreTechniques       = mitreTechniques,
                explanation           = explanation,
                recommendations       = recommendations
            )
        } catch (e: DocumentTooLargeException) {
            return DocumentAnalysisResult.error(file.name, e.message ?: "Document too large")
        } catch (e: Throwable) {
            Log.e(tag, "Analysis failed", e)
            return DocumentAnalysisResult.error(file.name, e.message ?: "Unknown error")
        }
    }

    // ── Sensitive Data Detection ───────────────────────────────────────────────

    private fun detectSensitiveData(text: String): Map<String, List<String>> {
        if (text.isBlank()) return emptyMap()
        val patterns = mapOf(
            "EMAIL"           to Regex("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}"),
            "PHONE"           to Regex("\\b\\d{3}[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b"),
            "SSN"             to Regex("\\b\\d{3}-\\d{2}-\\d{4}\\b"),
            "CREDIT_CARD"     to Regex("\\b\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}\\b"),
            "IP_ADDRESS"      to Regex("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b"),
            "IBAN"            to Regex("\\b[A-Z]{2}\\d{2}[A-Z0-9]{4}\\d{7}([A-Z0-9]{0,16})\\b"),
            "CNIC"            to Regex("\\b\\d{5}-\\d{7}-\\d\\b"),
            "API_KEY_OPENAI"  to Regex("sk-[A-Za-z0-9]{32,}"),
            "API_KEY_GOOGLE"  to Regex("AIza[0-9A-Za-z\\-_]{35}"),
            "API_KEY_AWS"     to Regex("AKIA[0-9A-Z]{16}"),
            "API_KEY_GITHUB"  to Regex("ghp_[A-Za-z0-9]{36}"),
            "API_KEY_GITLAB"  to Regex("glpat-[A-Za-z0-9\\-_]{20}"),
            "PRIVATE_KEY"     to Regex("-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----"),
            "BITCOIN_WALLET"  to Regex("\\b[13][a-km-zA-HJ-NP-Z1-9]{25,34}\\b"),
            "ETHEREUM_WALLET" to Regex("\\b0x[a-fA-F0-9]{40}\\b"),
            "JWT_TOKEN"       to Regex("eyJ[A-Za-z0-9_\\-]+\\.eyJ[A-Za-z0-9_\\-]+\\.[A-Za-z0-9_\\-]+")
        )
        return patterns.mapNotNull { (type, pattern) ->
            val matches = pattern.findAll(text).map { it.value }.distinct().toList()
            if (matches.isNotEmpty()) type to matches else null
        }.toMap()
    }

    // ── Threat Category Builder ────────────────────────────────────────────────

    private fun buildThreatCategories(
        hasMacros: Boolean,
        suspiciousUrls: List<ExtractedUrl>,
        sensitiveTypes: List<String>,
        isEncrypted: Boolean,
        macroResult: MacroAnalysisResult?,
        cveMatches: List<CveMatch>,
        anomalyReport: ForensicAnomalyReport,
        metadata: DocumentMetadata,
        hasHiddenText: Boolean
    ): List<DocumentThreatType> {
        val threats = mutableListOf<DocumentThreatType>()
        if (hasMacros) {
            threats.add(DocumentThreatType.EMBEDDED_SCRIPT)
            if (macroResult?.isMalicious == true) threats.add(DocumentThreatType.MALICIOUS_MACRO)
            if (macroResult?.isMalicious == false) threats.add(DocumentThreatType.UNSIGNED_MACRO)
        }
        if (suspiciousUrls.isNotEmpty())    threats.add(DocumentThreatType.SUSPICIOUS_URL)
        if (sensitiveTypes.isNotEmpty()) {
            threats.add(DocumentThreatType.SENSITIVE_DATA)
            if (sensitiveTypes.any { it.startsWith("API_KEY") || it == "PRIVATE_KEY" })
                threats.add(DocumentThreatType.CREDENTIAL_LEAK)
            if (sensitiveTypes.any { it.contains("WALLET") })
                threats.add(DocumentThreatType.CRYPTO_ADDRESS)
        }
        if (isEncrypted)                    threats.add(DocumentThreatType.ENCRYPTED_CONTENT)
        if (cveMatches.isNotEmpty())        threats.add(DocumentThreatType.EXPLOIT_SIGNATURE)
        if (metadata.embeddedFileCount > 0) threats.add(DocumentThreatType.EMBEDDED_FILE_DROPPER)
        if (metadata.pdfJsActions.isNotEmpty()) threats.add(DocumentThreatType.PDF_JAVASCRIPT)
        if (metadata.templateUrl != null)   threats.add(DocumentThreatType.EXTERNAL_TEMPLATE)
        if (metadata.hasHiddenSheets)       threats.add(DocumentThreatType.HIDDEN_SHEET)
        if (metadata.isProtected)           threats.add(DocumentThreatType.DOCUMENT_PROTECTION)
        if (hasHiddenText)                  threats.add(DocumentThreatType.HIDDEN_TEXT)
        if (anomalyReport.anomalies.isNotEmpty()) {
            threats.add(DocumentThreatType.FORENSIC_ANOMALY)
            val text = anomalyReport.anomalies.joinToString()
            if (text.contains("TIME-STOMP"))   threats.add(DocumentThreatType.TIMESTOMP)
            if (text.contains("FUTURE"))       threats.add(DocumentThreatType.FUTURE_TIMESTAMP)
            if (text.contains("EPOCH"))        threats.add(DocumentThreatType.EPOCH_TIMESTAMP)
        }
        return threats.distinct()
    }

    // ── Risk Score ─────────────────────────────────────────────────────────────

    private fun calculateRiskScore(hasMacros: Boolean, suspUrlCount: Int, hasSensitive: Boolean, isEncrypted: Boolean): Int {
        var score = 0
        if (hasMacros)     score += 30
        if (suspUrlCount > 0) score += minOf(suspUrlCount * 10, 40)
        if (hasSensitive)  score += 20
        if (isEncrypted)   score += 10
        return score.coerceIn(0, 100)
    }

    // ── Explanation ────────────────────────────────────────────────────────────

    private fun generateExplanation(
        riskScore: Int,
        threats: List<DocumentThreatType>,
        suspUrlCount: Int,
        macroResult: MacroAnalysisResult?,
        mitre: List<com.shadowinspect.app.domain.mitre.DocumentMitreTechnique>,
        cves: List<CveMatch>,
        anomalies: ForensicAnomalyReport
    ): String {
        val sb = StringBuilder()
        when {
            riskScore >= 70 -> sb.append("⚠️ CRITICAL: This document contains multiple high-severity security risks.")
            riskScore >= 50 -> sb.append("⚠️ HIGH RISK: Suspicious content detected requiring immediate attention.")
            riskScore >= 30 -> sb.append("⚠️ MEDIUM RISK: Some suspicious elements detected. Review before opening.")
            else            -> sb.append("✅ LOW RISK: No significant threats detected.")
        }
        if (threats.isNotEmpty()) {
            sb.append("\n\nDetected threats:")
            threats.forEach { t ->
                val line = when (t) {
                    DocumentThreatType.MALICIOUS_MACRO        -> "• Malicious Macro — VBA code that may execute commands, download payloads, or steal data."
                    DocumentThreatType.EMBEDDED_SCRIPT        -> "• Embedded Script — Contains scripting code (VBA/JS macros) that can automate actions on your system."
                    DocumentThreatType.SUSPICIOUS_URL         -> "• Suspicious URLs ($suspUrlCount found) — Links to potentially malicious websites, phishing pages, or malware download sites."
                    DocumentThreatType.SENSITIVE_DATA         -> "• Sensitive Data Exposure — Contains PII (emails, phone numbers, SSNs, credit cards)."
                    DocumentThreatType.CREDENTIAL_LEAK        -> "• Credential Leak — API keys, private keys, or secret tokens found. These may expose live services."
                    DocumentThreatType.CRYPTO_ADDRESS         -> "• Crypto Wallet Address — Bitcoin or Ethereum wallet address found. May indicate ransom instruction or financial scam."
                    DocumentThreatType.ENCRYPTED_CONTENT      -> "• Encrypted Content — File is password-protected, which can hide malicious content from scanners."
                    DocumentThreatType.PHISHING_CONTENT       -> "• Phishing Content — Social engineering elements designed to steal credentials."
                    DocumentThreatType.OUTDATED_FORMAT        -> "• Outdated Format — Legacy file format with known security vulnerabilities."
                    DocumentThreatType.METADATA_LEAK          -> "• Metadata Leak — Document reveals sensitive organisation or author information."
                    DocumentThreatType.EMBEDDED_FILE_DROPPER  -> "• Embedded File Dropper — Executable or data files are hidden inside this document."
                    DocumentThreatType.HIDDEN_TEXT            -> "• Hidden Text — White-on-white or invisible text detected, used to evade scanners."
                    DocumentThreatType.FORENSIC_ANOMALY       -> "• Forensic Anomaly — Impossible timestamp or metadata tampering detected."
                    DocumentThreatType.TIMESTOMP              -> "• Time-Stomp — Creation date is AFTER modification date. Metadata has been tampered with."
                    DocumentThreatType.FUTURE_TIMESTAMP       -> "• Future Timestamp — A date is set in the future, indicating metadata manipulation."
                    DocumentThreatType.EPOCH_TIMESTAMP        -> "• Epoch Timestamp — Date is near Unix epoch (1970). Metadata was likely wiped."
                    DocumentThreatType.EXPLOIT_SIGNATURE      -> "• Exploit Signature — Matches a known CVE exploit pattern. Treat as weaponised document."
                    DocumentThreatType.UNSIGNED_MACRO         -> "• Unsigned Macro — Macros present but no digital signature. Cannot verify authenticity."
                    DocumentThreatType.PDF_JAVASCRIPT         -> "• PDF JavaScript — JavaScript is embedded in this PDF. Can execute code on open."
                    DocumentThreatType.EXTERNAL_TEMPLATE      -> "• External Template — Document loads a template from an external URL. Common dropper technique."
                    DocumentThreatType.HIDDEN_SHEET           -> "• Hidden Sheets — XLSX workbook contains hidden sheets. May conceal malicious content or formulas."
                    DocumentThreatType.DOCUMENT_PROTECTION    -> "• Document Protection — Enforced protection may hide or lock content from inspection."
                }
                sb.append("\n$line")
            }
        }
        macroResult?.let { m ->
            if (m.isMalicious) {
                sb.append("\n\n🔴 MALICIOUS MACRO DETAILS:")
                sb.append("\n• Confidence: ${m.confidenceScore}%")
                if (m.autoExecMacros.isNotEmpty()) sb.append("\n• Auto-executing: ${m.autoExecMacros.joinToString()}")
                if (m.obfuscationLevel > 5) sb.append("\n• Obfuscation level: ${m.obfuscationLevel}/10")
                if (m.apiCalls.isNotEmpty()) sb.append("\n• Suspicious API calls: ${m.apiCalls.take(5).joinToString()}")
            }
        }
        if (cves.isNotEmpty()) {
            sb.append("\n\n🔴 CVE EXPLOIT SIGNATURES DETECTED:")
            cves.forEach { sb.append("\n• [${it.severity}] ${it.cveId} — ${it.name}: ${it.description}") }
        }
        if (anomalies.anomalies.isNotEmpty()) {
            sb.append("\n\n🔬 FORENSIC ANOMALIES:")
            anomalies.anomalies.forEach { sb.append("\n• $it") }
        }
        if (mitre.isNotEmpty()) {
            sb.append("\n\n🔬 MITRE ATT&CK Techniques:")
            mitre.forEach { sb.append("\n\n• ${it.id} — ${it.name}\n  Tactic: ${it.tactic}\n  ${it.description}") }
        }
        return sb.toString()
    }

    // ── Recommendations ────────────────────────────────────────────────────────

    private fun generateRecommendations(threats: List<DocumentThreatType>, macroResult: MacroAnalysisResult?): List<String> {
        val r = mutableListOf<String>()
        if (DocumentThreatType.MALICIOUS_MACRO in threats) {
            r.add("🔴 DO NOT ENABLE MACROS: This document contains malicious VBA code designed to compromise your system.")
            if (macroResult?.autoExecMacros?.isNotEmpty() == true)
                r.add("⚠️ AUTO-EXECUTION: Macros will run automatically upon opening. Use an isolated sandbox.")
        }
        if (DocumentThreatType.EMBEDDED_SCRIPT in threats && DocumentThreatType.MALICIOUS_MACRO !in threats)
            r.add("🟠 MACRO REVIEW: Unsigned or suspicious macros detected. Review the source before allowing execution.")
        if (DocumentThreatType.SUSPICIOUS_URL in threats)
            r.add("🔗 URL WARNING: Contains links to high-risk domains. Avoid clicking any hyperlinks in this document.")
        if (DocumentThreatType.CREDENTIAL_LEAK in threats)
            r.add("🔑 DATA LEAK: Sensitive API keys or secrets detected. Revoke these credentials immediately.")
        if (DocumentThreatType.CRYPTO_ADDRESS in threats)
            r.add("₿ CRYPTO WALLET: A cryptocurrency address was found. This is common in ransom-based phishing.")
        if (DocumentThreatType.SENSITIVE_DATA in threats)
            r.add("👤 PII DETECTED: Contains personal data (email, phone, SSN). Ensure this document is handled according to GDPR/Privacy laws.")
        if (DocumentThreatType.ENCRYPTED_CONTENT in threats)
            r.add("🔒 ENCRYPTION: Content is hidden from deep inspection. Verify the sender before entering any password.")
        if (DocumentThreatType.EXPLOIT_SIGNATURE in threats)
            r.add("💣 WEAPONISED: Matches known malware exploit signatures. Delete immediately and perform a full system scan.")
        if (DocumentThreatType.EMBEDDED_FILE_DROPPER in threats)
            r.add("📎 DROPPER RISK: Executable or binary files are hidden inside. These may be launched to install malware.")
        if (DocumentThreatType.PDF_JAVASCRIPT in threats)
            r.add("📄 PDF JAVASCRIPT: Active script content detected in PDF. Use a browser-based viewer or disable JS.")
        if (DocumentThreatType.EXTERNAL_TEMPLATE in threats)
            r.add("🌐 EXTERNAL TEMPLATE: Attempts to load content from the internet. This can bypass local security rules.")
        if (DocumentThreatType.TIMESTOMP in threats || DocumentThreatType.FORENSIC_ANOMALY in threats)
            r.add("🔬 FORGERY DETECTED: Forensic metadata does not match reality. This document may be spoofed or tampered with.")
        if (DocumentThreatType.HIDDEN_TEXT in threats)
            r.add("👻 HIDDEN TEXT: Invisible text detected. This is a common evasion technique used to bypass email filters.")
        if (DocumentThreatType.HIDDEN_SHEET in threats)
            r.add("📊 HIDDEN DATA: Hidden sheets in Excel may contain malicious formulas or hidden commands.")
        
        if (r.isEmpty()) {
            r.add("✅ INTEGRITY CHECK: No high-priority threats found in forensic signatures.")
            r.add("🛡️ PRACTICE CAUTION: Even if scanned, never open unexpected attachments from untrusted senders.")
        } else {
            r.add("⚡ RECOMMENDED ACTION: Move to a secure quarantine or delete if the source is not 100% verified.")
        }
        return r
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun detectDocumentType(file: File) = when (file.extension.lowercase()) {
        "pdf"  -> DocumentType.PDF
        "docx" -> DocumentType.DOCX
        "xlsx" -> DocumentType.XLSX
        "pptx" -> DocumentType.PPTX
        "doc"  -> DocumentType.DOC
        "xls"  -> DocumentType.XLS
        "ppt"  -> DocumentType.PPT
        "txt"  -> DocumentType.TXT
        "rtf"  -> DocumentType.RTF
        "odt"  -> DocumentType.ODT
        else   -> DocumentType.UNKNOWN
    }

    private fun calculateHash(file: File, algorithm: String): String {
        val digest = MessageDigest.getInstance(algorithm)
        file.inputStream().use { fis ->
            val buf = ByteArray(8192); var n = fis.read(buf)
            while (n != -1) { digest.update(buf, 0, n); n = fis.read(buf) }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun getMimeType(file: File) = when (file.extension.lowercase()) {
        "pdf"  -> "application/pdf"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        "doc"  -> "application/msword"
        "xls"  -> "application/vnd.ms-excel"
        "ppt"  -> "application/vnd.ms-powerpoint"
        "txt"  -> "text/plain"
        "rtf"  -> "application/rtf"
        else   -> "application/octet-stream"
    }

    private fun isSuspiciousUrl(url: String): Boolean =
        listOf("bit\\.ly","tinyurl","goo\\.gl","login","signin","verify","confirm","banking","paypal")
            .any { Regex(it, RegexOption.IGNORE_CASE).containsMatchIn(url) }

    private fun detectUrlsFromText(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        // Simple but effective URL regex
        val urlRegex = Regex("(https?|ftp)://[\\w\\d\\.#\\?/=&\\%\\-\\+]+", RegexOption.IGNORE_CASE)
        return urlRegex.findAll(text).map { it.value }.distinct().toList()
    }
}
