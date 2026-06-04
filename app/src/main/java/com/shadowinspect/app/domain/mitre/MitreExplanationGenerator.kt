package com.shadowinspect.app.domain.mitre

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MitreExplanationGenerator @Inject constructor() {

    fun generateUserFriendlyExplanation(techniques: List<MitreTechnique>): String {
        if (techniques.isEmpty()) {
            return "✅ No MITRE ATT&CK techniques detected. This appears to be safe."
        }

        val sb = StringBuilder()
        sb.append("⚠️ **MITRE ATT&CK Analysis**\n\n")
        sb.append("This app exhibits behavior matching ${techniques.size} known attack techniques:\n\n")

        techniques.groupBy { it.tactics.firstOrNull() ?: "Unknown" }
            .forEach { (tactic, techs) ->
                sb.append("**$tactic** (${techs.size} techniques)\n")
                techs.forEach { technique ->
                    sb.append("• **${technique.id}**: ${technique.name}\n")
                    sb.append("  ${technique.description.take(100)}...\n")
                }
                sb.append("\n")
            }

        sb.append("\n**What this means for you:**\n")
        when (techniques.size) {
            1 -> sb.append("This app has one concerning behavior pattern.")
            2, 3 -> sb.append("This app has multiple suspicious behavior patterns.")
            else -> sb.append("This app shows significant malicious behavior patterns!")
        }

        return sb.toString()
    }

    fun generateTechnicalReport(techniques: List<MitreTechnique>): String {
        val sb = StringBuilder()
        sb.append("MITRE ATT&CK TECHNICAL REPORT\n")
        sb.append("=".repeat(50) + "\n\n")

        techniques.forEachIndexed { index, technique ->
            sb.append("${index + 1}. ${technique.id}: ${technique.name}\n")
            sb.append("   Tactic: ${technique.tactics.joinToString()}\n")
            sb.append("   Description: ${technique.description}\n")
            sb.append("   Required Permissions: ${technique.permissionsRequired.joinToString()}\n")
            sb.append("   Detection: ${technique.detection ?: "N/A"}\n")
            sb.append("   Mitigation: ${technique.mitigation ?: "N/A"}\n")
            sb.append("   Reference: ${technique.url}\n\n")
        }

        return sb.toString()
    }
}
