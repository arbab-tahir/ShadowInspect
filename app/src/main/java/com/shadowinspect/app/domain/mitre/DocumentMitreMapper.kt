package com.shadowinspect.app.domain.mitre

import com.shadowinspect.app.domain.model.DocumentThreatType
import com.shadowinspect.app.domain.analyzer.MacroAnalysisResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentMitreMapper @Inject constructor(
    private val jsonParser: MitreJsonParser
) {

    // Fallback technique descriptions when the JSON DB doesn't have the technique
    private val techniqueDescriptions = mapOf(
        "T1204.002" to TechniqueInfo(
            "User Execution: Malicious File",
            "Execution",
            "An adversary relies upon a user opening a malicious file to gain execution. Users may be tricked into opening a file that leads to code execution, such as a macro-enabled Office document."
        ),
        "T1547.001" to TechniqueInfo(
            "Boot or Logon Autostart Execution: Registry Run Keys",
            "Persistence",
            "Adversaries may achieve persistence by adding a program to a startup folder or referencing it via Run/RunOnce keys, so the program is executed at user login."
        ),
        "T1059" to TechniqueInfo(
            "Command and Scripting Interpreter",
            "Execution",
            "Adversaries may abuse command and script interpreters to execute commands or scripts. Common interpreters include PowerShell, Windows Command Shell, and VBScript."
        ),
        "T1059.001" to TechniqueInfo(
            "PowerShell",
            "Execution",
            "Adversaries may abuse PowerShell commands and scripts for execution. PowerShell is a powerful interactive command-line interface that can be used to perform a wide range of actions."
        ),
        "T1105" to TechniqueInfo(
            "Ingress Tool Transfer",
            "Command And Control",
            "Adversaries may transfer tools or other files from an external system into a compromised environment. Files may be copied from an external system to the victim network."
        ),
        "T1005" to TechniqueInfo(
            "Data from Local System",
            "Collection",
            "Adversaries may search local system sources, such as file systems or local databases, to find files of interest and sensitive data before exfiltration."
        ),
        "T1027" to TechniqueInfo(
            "Obfuscated Files or Information",
            "Defense Evasion",
            "Adversaries may attempt to make an executable or file difficult to discover or analyze by encrypting, encoding, or otherwise obfuscating its contents."
        ),
        "T1566" to TechniqueInfo(
            "Phishing",
            "Initial Access",
            "Adversaries may send phishing messages to gain access to victim systems. Phishing messages are typically delivered through social engineering."
        ),
        "T1566.001" to TechniqueInfo(
            "Phishing: Spearphishing Attachment",
            "Initial Access",
            "Adversaries may send targeted emails with malicious attachments to gain access. These attachments can contain exploits or malicious macros."
        ),
        "T1566.002" to TechniqueInfo(
            "Phishing: Spearphishing Link",
            "Initial Access",
            "Adversaries may send spearphishing emails with a malicious link to download malware or harvest credentials through a fake website."
        ),
        "T1114" to TechniqueInfo(
            "Email Collection",
            "Collection",
            "Adversaries may target user email to collect sensitive information. Emails may contain sensitive data including PII, credentials, and confidential documents."
        ),
        "T1486" to TechniqueInfo(
            "Data Encrypted for Impact",
            "Impact",
            "Adversaries may encrypt data on target systems or networks to interrupt availability. This is commonly done using ransomware that holds data hostage for payment."
        )
    )

    /**
     * Map document threats to MITRE ATT&CK techniques with full descriptions
     */
    suspend fun mapThreatsToMitre(
        threatTypes: List<DocumentThreatType>,
        macroAnalysis: MacroAnalysisResult?,
        suspiciousUrls: List<String>
    ): List<DocumentMitreTechnique> {
        val techniques = mutableListOf<DocumentMitreTechnique>()

        threatTypes.forEach { threat ->
            when (threat) {
                DocumentThreatType.MALICIOUS_MACRO -> {
                    techniques.addAll(getMacroMitreTechniques(macroAnalysis))
                }
                DocumentThreatType.SUSPICIOUS_URL -> {
                    techniques.addAll(getUrlMitreTechniques(suspiciousUrls))
                }
                DocumentThreatType.EMBEDDED_SCRIPT -> {
                    techniques.add(resolveTechnique("T1059"))
                }
                DocumentThreatType.SENSITIVE_DATA -> {
                    techniques.add(resolveTechnique("T1114"))
                }
                DocumentThreatType.ENCRYPTED_CONTENT -> {
                    techniques.add(resolveTechnique("T1027"))
                }
                DocumentThreatType.PHISHING_CONTENT -> {
                    techniques.add(resolveTechnique("T1566"))
                }
                DocumentThreatType.OUTDATED_FORMAT -> {
                    techniques.add(resolveTechnique("T1204.002"))
                }
                DocumentThreatType.METADATA_LEAK -> {
                    techniques.add(resolveTechnique("T1005"))
                }
                else -> {
                    // Other threat types don't have direct Mitre mappings yet
                }
            }
        }

        return techniques.distinctBy { it.id }
    }

    private fun getMacroMitreTechniques(macroAnalysis: MacroAnalysisResult?): List<DocumentMitreTechnique> {
        val techniques = mutableListOf<DocumentMitreTechnique>()

        techniques.add(resolveTechnique("T1204.002"))

        macroAnalysis?.let { analysis ->
            if (analysis.autoExecMacros.isNotEmpty()) {
                techniques.add(resolveTechnique("T1547.001"))
            }
            if (analysis.apiCalls.any { it.contains("Shell", true) || it.contains("PowerShell", true) }) {
                techniques.add(resolveTechnique("T1059.001"))
            }
            if (analysis.apiCalls.any { it.contains("URLDownload", true) || it.contains("WinHttp", true) }) {
                techniques.add(resolveTechnique("T1105"))
            }
            if (analysis.apiCalls.any { it.contains("FileSystemObject", true) || it.contains("ADODB", true) }) {
                techniques.add(resolveTechnique("T1005"))
            }
            if (analysis.obfuscationLevel > 5) {
                techniques.add(resolveTechnique("T1027"))
            }
        }

        return techniques
    }

    private fun getUrlMitreTechniques(suspiciousUrls: List<String>): List<DocumentMitreTechnique> {
        val techniques = mutableListOf<DocumentMitreTechnique>()

        techniques.add(resolveTechnique("T1566.001"))

        if (suspiciousUrls.any { url ->
                url.contains("bit.ly") || url.contains("tinyurl") || url.contains("goo.gl")
            }) {
            techniques.add(resolveTechnique("T1566.002"))
        }

        return techniques
    }

    /**
     * Resolve a technique ID to a full DocumentMitreTechnique with description
     */
    private fun resolveTechnique(id: String): DocumentMitreTechnique {
        val fallback = techniqueDescriptions[id]
        return DocumentMitreTechnique(
            id = id,
            name = fallback?.name ?: id,
            tactic = fallback?.tactic ?: "Unknown",
            description = fallback?.description ?: "No description available."
        )
    }

    private data class TechniqueInfo(
        val name: String,
        val tactic: String,
        val description: String
    )
}

/**
 * Simplified technique model for document scanning results
 * (avoids coupling to the full MitreTechnique which requires JSON parsing)
 */
data class DocumentMitreTechnique(
    val id: String,
    val name: String,
    val tactic: String,
    val description: String
)
