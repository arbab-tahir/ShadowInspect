package com.shadowinspect.app.domain.analyzer

import com.shadowinspect.app.domain.model.CveMatch
import com.shadowinspect.app.domain.model.DocumentType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scans document text content against a database of known CVE exploit signatures.
 * Each signature is a regex pattern that, when found in document content, strongly
 * suggests the document was crafted to exploit a specific vulnerability.
 */
@Singleton
class CveSignatureDetector @Inject constructor() {

    data class CveSignature(
        val cveId: String,
        val name: String,
        val description: String,
        val severity: String,
        val applicableTypes: Set<DocumentType>,
        val patterns: List<Regex>
    )

    private val signatures = listOf(

        CveSignature(
            cveId = "CVE-2022-30190",
            name = "Follina (MSDT Remote Code Execution)",
            description = "Exploits the Microsoft Support Diagnostic Tool (MSDT) via crafted Office documents. " +
                "Uses ms-msdt:// URI scheme to trigger PowerShell execution without macros.",
            severity = "CRITICAL",
            applicableTypes = setOf(DocumentType.DOCX, DocumentType.DOC, DocumentType.RTF),
            patterns = listOf(
                Regex("ms-msdt:", RegexOption.IGNORE_CASE),
                Regex("msdt\\.exe", RegexOption.IGNORE_CASE),
                Regex("PCWDiagnostic.*IT_BrowseForFile", RegexOption.IGNORE_CASE)
            )
        ),

        CveSignature(
            cveId = "CVE-2017-11882",
            name = "Equation Editor Buffer Overflow",
            description = "A stack buffer overflow in Microsoft Equation Editor (EQNEDT32.EXE) " +
                "allows arbitrary code execution when a crafted .doc/.xls document is opened.",
            severity = "HIGH",
            applicableTypes = setOf(DocumentType.DOC, DocumentType.XLS, DocumentType.DOCX, DocumentType.XLSX),
            patterns = listOf(
                Regex("Equation\\.3", RegexOption.IGNORE_CASE),
                Regex("EQNEDT32", RegexOption.IGNORE_CASE),
                Regex("Microsoft Equation 3\\.0", RegexOption.IGNORE_CASE)
            )
        ),

        CveSignature(
            cveId = "CVE-2021-40444",
            name = "MSHTML Remote Code Execution",
            description = "Exploits a flaw in the MSHTML browser engine via a crafted ActiveX control " +
                "embedded in an Office document. Allows arbitrary code execution without macros.",
            severity = "CRITICAL",
            applicableTypes = setOf(DocumentType.DOCX, DocumentType.DOC, DocumentType.XLSX),
            patterns = listOf(
                Regex("CLSID.*\\{[0-9A-Fa-f-]{36}\\}", RegexOption.IGNORE_CASE),
                Regex("ActiveX.*codebase.*http", RegexOption.IGNORE_CASE),
                Regex("classid.*clsid:", RegexOption.IGNORE_CASE)
            )
        ),

        CveSignature(
            cveId = "CVE-2012-0158",
            name = "MSCOMCTL.OCX Buffer Overflow",
            description = "A buffer overflow in the Windows common controls library (MSCOMCTL.OCX) " +
                "triggered by a malformed ListView or TreeView control embedded in an Office document.",
            severity = "HIGH",
            applicableTypes = setOf(DocumentType.DOC, DocumentType.XLS, DocumentType.PPT),
            patterns = listOf(
                Regex("MSCOMCTL\\.OCX", RegexOption.IGNORE_CASE),
                Regex("ListView.*\\{BDD1F04B", RegexOption.IGNORE_CASE),
                Regex("TreeView.*\\{C74190B6", RegexOption.IGNORE_CASE)
            )
        ),

        CveSignature(
            cveId = "CVE-2017-0199",
            name = "OLE HTA Handler Code Execution",
            description = "Exploits the Windows OLE interface to load a remote HTML Application (.hta) " +
                "via a crafted RTF or Office document, enabling full code execution.",
            severity = "HIGH",
            applicableTypes = setOf(DocumentType.RTF, DocumentType.DOC, DocumentType.DOCX),
            patterns = listOf(
                Regex("\\.hta", RegexOption.IGNORE_CASE),
                Regex("objupdate.*http", RegexOption.IGNORE_CASE),
                Regex("application/hta", RegexOption.IGNORE_CASE),
                Regex("mshta\\.exe", RegexOption.IGNORE_CASE)
            )
        ),

        CveSignature(
            cveId = "CVE-2018-0802",
            name = "Equation Editor Memory Corruption",
            description = "A second memory corruption vulnerability in Microsoft Equation Editor, " +
                "commonly chained with CVE-2017-11882. Allows remote code execution via crafted documents.",
            severity = "HIGH",
            applicableTypes = setOf(DocumentType.DOC, DocumentType.XLS, DocumentType.DOCX),
            patterns = listOf(
                Regex("Equation\\.3", RegexOption.IGNORE_CASE),
                Regex("\\\\objdata.*pict", RegexOption.IGNORE_CASE)
            )
        ),

        CveSignature(
            cveId = "CVE-2023-21716",
            name = "Word RTF Heap Corruption",
            description = "A heap corruption vulnerability triggered by a malformed font table " +
                "in an RTF document. Allows remote code execution with no user interaction beyond opening the file.",
            severity = "CRITICAL",
            applicableTypes = setOf(DocumentType.RTF, DocumentType.DOC),
            patterns = listOf(
                Regex("\\\\fonttbl.*\\\\f\\d{4,}", RegexOption.IGNORE_CASE),
                Regex("\\\\fcharset.*\\\\fprq", RegexOption.IGNORE_CASE)
            )
        ),

        CveSignature(
            cveId = "CVE-2020-0674",
            name = "Internet Explorer JScript Engine RCE",
            description = "A remote code execution vulnerability in Internet Explorer's JScript engine. " +
                "Can be triggered via a malicious PDF or Office file containing embedded HTML/JScript.",
            severity = "CRITICAL",
            applicableTypes = setOf(DocumentType.PDF, DocumentType.DOCX, DocumentType.DOC),
            patterns = listOf(
                Regex("jscript\\.dll", RegexOption.IGNORE_CASE),
                Regex("VBScript\\.Regexp", RegexOption.IGNORE_CASE)
            )
        )
    )

    /**
     * Scan [text] extracted from a document of [documentType] against all known CVE signatures.
     * Returns a list of [CveMatch] objects for every CVE whose patterns were found.
     */
    fun scan(text: String, documentType: DocumentType): List<CveMatch> {
        if (text.isBlank()) return emptyList()
        val matches = mutableListOf<CveMatch>()

        for (sig in signatures) {
            // Only check signatures relevant to this document type
            if (documentType !in sig.applicableTypes && DocumentType.UNKNOWN !in sig.applicableTypes) continue

            val patternMatched = sig.patterns.any { pattern ->
                pattern.containsMatchIn(text)
            }
            if (patternMatched) {
                matches.add(
                    CveMatch(
                        cveId       = sig.cveId,
                        name        = sig.name,
                        description = sig.description,
                        severity    = sig.severity
                    )
                )
            }
        }

        return matches
    }
}
