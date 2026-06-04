package com.shadowinspect.app.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color

object ChartColors {
    // Risk level colors
    val Critical = Color(0xFFFF3B3B)  // Neon Red
    val High = Color(0xFFFFA500)      // Orange
    val Medium = Color(0xFFFFFF00)    // Yellow
    val Low = Color(0xFF00FF9D)       // Neon Green
    val Safe = Color(0xFF00C853)      // Darker Green
    
    // Tactic colors (distinct for each MITRE tactic)
    val Collection = Color(0xFF4285F4)      // Blue
    val CommandAndControl = Color(0xFFEA4335) // Red
    val CredentialAccess = Color(0xFFFBBC05)  // Yellow
    val DefenseEvasion = Color(0xFF34A853)    // Green
    val Discovery = Color(0xFFFF6D00)         // Orange
    val Execution = Color(0xFFAA00FF)         // Purple
    val Exfiltration = Color(0xFF00BCD4)      // Cyan
    val Impact = Color(0xFFF50057)            // Pink
    val InitialAccess = Color(0xFF6200EA)     // Deep Purple
    val Persistence = Color(0xFF795548)       // Brown
    val PrivilegeEscalation = Color(0xFF9E9E9E) // Gray
    
    // Gradient for risk gauge
    val RiskGradient = listOf(
        Safe,
        Low,
        Medium,
        High,
        Critical
    )
    
    fun getTacticColor(tactic: String): Color {
        return when (tactic) {
            "Collection" -> Collection
            "Command and Control" -> CommandAndControl
            "Credential Access" -> CredentialAccess
            "Defense Evasion" -> DefenseEvasion
            "Discovery" -> Discovery
            "Execution" -> Execution
            "Exfiltration" -> Exfiltration
            "Impact" -> Impact
            "Initial Access" -> InitialAccess
            "Persistence" -> Persistence
            "Privilege Escalation" -> PrivilegeEscalation
            else -> NeonGreen // Using NeonGreen as fallback
        }
    }
}
