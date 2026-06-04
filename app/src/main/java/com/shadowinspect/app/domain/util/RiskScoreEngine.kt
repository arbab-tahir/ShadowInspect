package com.shadowinspect.app.domain.util

import com.shadowinspect.app.domain.model.ApkPermission
import com.shadowinspect.app.domain.model.PermissionDangerousLevel
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
//  Expert System – Knowledge Base
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A single rule in the knowledge base.
 *
 * @param name        Human-readable threat label (e.g. "Banking Trojan").
 * @param conditions  Permission keywords that must ALL be present for the rule to fire.
 * @param weight      Confidence weight in [0.0, 1.0]; higher = more dangerous.
 * @param description Short description of the threat shown in the explanation.
 * @param mitre       Optional MITRE ATT&CK for Mobile technique ID.
 */
data class ThreatRule(
    val name: String,
    val conditions: List<String>,
    val weight: Double,
    val description: String,
    val mitre: String = ""
) {
    /**
     * Inference step: returns [weight] when ALL conditions match at least one
     * permission string, or 0.0 when the rule does not fire.
     */
    fun infer(permissions: Set<String>): Double {
        val fired = conditions.all { keyword ->
            permissions.any { it.contains(keyword, ignoreCase = true) }
        }
        return if (fired) weight else 0.0
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Risk Score Engine  (Inference Engine)
// ─────────────────────────────────────────────────────────────────────────────

@Singleton
class RiskScoreEngine @Inject constructor() {

    // ── Knowledge Base ──────────────────────────────────────────────────────
    private val threatRules: List<ThreatRule> = listOf(

        ThreatRule(
            name        = "Banking Trojan",
            conditions  = listOf("SMS", "ACCESSIBILITY"),
            weight      = 0.85,
            description = "May intercept OTPs and abuse Accessibility to perform fraudulent bank transactions.",
            mitre       = "T1412"
        ),

        ThreatRule(
            name        = "Spyware",
            conditions  = listOf("CAMERA", "RECORD_AUDIO", "LOCATION"),
            weight      = 0.80,
            description = "May silently record audio/video and exfiltrate precise location data.",
            mitre       = "T1418"
        ),

        ThreatRule(
            name        = "Ransomware",
            conditions  = listOf("EXTERNAL_STORAGE"),
            weight      = 0.70,
            description = "May encrypt or steal user files stored on external storage.",
            mitre       = "T1471"
        ),

        ThreatRule(
            name        = "Adware / Stalkerware",
            conditions  = listOf("INTERNET", "WAKE_LOCK"),
            weight      = 0.45,
            description = "May display intrusive ads or track user activity in the background.",
            mitre       = "T1402"
        ),

        ThreatRule(
            name        = "Call-Log Harvester",
            conditions  = listOf("READ_CALL_LOG", "READ_CONTACTS"),
            weight      = 0.65,
            description = "May harvest call history and contacts for surveillance or social engineering.",
            mitre       = "T1432"
        ),

        ThreatRule(
            name        = "Credential Stealer",
            conditions  = listOf("ACCESSIBILITY", "INTERNET"),
            weight      = 0.75,
            description = "May overlay phishing UI over legitimate apps to steal credentials.",
            mitre       = "T1411"
        ),

        ThreatRule(
            name        = "Device Administrator Abuse",
            conditions  = listOf("BIND_DEVICE_ADMIN"),
            weight      = 0.90,
            description = "May lock the device, wipe data, or enforce malicious policies by abusing device-admin rights.",
            mitre       = "T1401"
        ),
        
        ThreatRule(
            name        = "System Settings Manipulation",
            conditions  = listOf("WRITE_SETTINGS"),
            weight      = 0.70,
            description = "May modify global system settings to disable security features or enable persistent malicious behaviors.",
            mitre       = "T1406"
        )
    )

    // ── Inference Engine ────────────────────────────────────────────────────

    /**
     * Runs the inference engine over [permissions] against every rule in the
     * knowledge base and returns a list of (ThreatRule, score) pairs for all
     * rules that fired.
     */
    private fun runInference(permissions: List<String>): List<Pair<ThreatRule, Double>> {
        val permSet = permissions.map { it.uppercase() }.toSet()
        return threatRules.mapNotNull { rule ->
            val score = rule.infer(permSet)
            if (score > 0.0) rule to score else null
        }
    }

    /**
     * Categorize likely threat types by running the full inference engine and
     * returning the names of all fired rules.
     */
    fun categorizeThreats(permissions: List<String>): List<String> =
        runInference(permissions).map { it.first.name }

    // ── APK Risk Score ───────────────────────────────────────────────────────

    /**
     * Calculates an APK risk score (0–100) combining:
     *  - Rule-based inference over the permission set (contributes up to 60 pts)
     *  - Ratio of dangerous-to-total permissions (up to 20 pts)
     *  - Suspicious manifest patterns (up to 15 pts)
     *  - External threat-intel score (up to 5 pts)
     */
    fun calculateApkRisk(
        permissions: List<String>,
        dangerousPermissionsCount: Int,
        totalPermissionsCount: Int,
        hasSuspiciousPatterns: Boolean,
        threatIntelScore: Float = 0f
    ): Int {
        // 1. Inference engine: aggregate weighted scores from matched rules
        val firedRules = runInference(permissions)
        val ruleScore = firedRules
            .sumOf { it.second }
            .coerceAtMost(1.0) * 60.0                   // cap at 60 pts

        // 2. Dangerous-permission ratio
        val permRatio = (dangerousPermissionsCount.toDouble() / max(1, totalPermissionsCount))
            .coerceAtMost(1.0) * 20.0

        // 3. Manifest heuristics (debuggable, suspicious package name, etc.)
        val suspiciousPoints = if (hasSuspiciousPatterns) 15.0 else 0.0

        // 4. External threat-intel (VirusTotal etc.) – max 5 pts
        val intelPoints = (threatIntelScore.toDouble().coerceIn(0.0, 1.0)) * 5.0

        // 5. Floor for high-risk permissions
        val floorPoints = if (permissions.any { it.contains("WRITE_SETTINGS") || it.contains("ACCESSIBILITY") }) 15.0 else 0.0

        val raw = ruleScore + permRatio + suspiciousPoints + intelPoints + floorPoints
        return raw.roundToInt().coerceIn(0, 100)
    }

    // ── URL Risk Score ───────────────────────────────────────────────────────

    /**
     * Calculates a URL risk score (0–100) from VirusTotal scanner votes.
     * Formula: (malicious × 1.0 + suspicious × 0.5) / totalScanners × 100
     */
    fun calculateUrlRisk(maliciousCount: Int, suspiciousCount: Int, totalScanners: Int): Int {
        if (totalScanners <= 0) return 0
        val riskValue = (maliciousCount * 1.0) + (suspiciousCount * 0.5)
        val score = (riskValue / totalScanners) * 100
        return score.roundToInt().coerceIn(0, 100)
    }

    // ── Risk Level Categorisation ─────────────────────────────────────────────

    /**
     * Maps a numeric score to a risk-level label.
     *
     * CRITICAL ≥ 70 | HIGH ≥ 50 | MEDIUM ≥ 30 | LOW ≥ 10 | SAFE < 10
     */
    fun getRiskLevel(score: Int): String = when {
        score >= 70 -> "CRITICAL"
        score >= 50 -> "HIGH"
        score >= 30 -> "MEDIUM"
        score >= 10 -> "LOW"
        else        -> "SAFE"
    }

    // ── Human-Readable Explanations ──────────────────────────────────────────

    /**
     * Builds an APK explanation that names every fired rule, its description
     * and (optionally) its MITRE technique ID.
     */
    fun generateApkExplanation(
        riskScore: Int,
        dangerousPermissions: List<ApkPermission>,
        permissions: List<String>
    ): String {
        val level = getRiskLevel(riskScore)

        val firedRules = runInference(permissions)
        
        return buildString {
            appendLine("**OVERALL ANALYSIS**")
            appendLine("The application has been assigned a **$level** risk level ($riskScore/100).")
            appendLine()
            appendLine("**DANGEROUS PERMISSIONS DETECTED**")
            if (dangerousPermissions.isEmpty()) {
                appendLine("• No standard dangerous permissions detected.")
            } else {
                dangerousPermissions.forEach { appendLine("• ${it.name}") }
            }
            appendLine()
            appendLine("**IDENTIFIED THREAT PATTERNS**")
            if (firedRules.isEmpty()) {
                appendLine("• No specific malicious threat patterns were detected via static signature matching.")
            } else {
                firedRules.forEach { (rule, score) ->
                    val pct = (score * 100).roundToInt()
                    appendLine("• **${rule.name}** (Confidence: $pct%)")
                    appendLine("  *${rule.description}*")
                    if (rule.mitre.isNotBlank()) appendLine("  MITRE: [${rule.mitre}]")
                }
            }
            appendLine()
            appendLine("**SECURITY RECOMMENDATIONS**")
            appendLine("• Exercise caution if the APK source is unverified.")
            appendLine("• Audit requested permissions against claimed app functionality.")
            appendLine("• Highly suspicious apps should be analyzed in a sandbox environment.")
            if (permissions.any { it.contains("ACCESSIBILITY") }) {
                appendLine("• WARNING: Accessibility services can be abused for credential harvesting.")
            }
        }
    }

    /**
     * Builds a URL scan explanation based on the risk level and scanner totals.
     */
    fun generateUrlExplanation(score: Int, maliciousCount: Int, totalScanners: Int): String {
        return when (getRiskLevel(score)) {
            "CRITICAL" -> "🚨 WARNING! This URL is highly dangerous. $maliciousCount of $totalScanners vendors flagged it malicious. Do not proceed."
            "HIGH"     -> "⚠️ High risk: multiple vendors reported malicious behaviour. Strongly discouraged."
            "MEDIUM"   -> "👀 Moderate risk: some vendors flagged this URL. Proceed with extreme caution."
            "LOW"      -> "🛡️ Low risk: a small number flagged suspicious elements. Verify the source before continuing."
            "SAFE"     -> "✅ Safe: no significant threats detected. Proceed normally."
            else       -> "❓ Unknown risk level."
        }
    }

    // ── Permission-Level Classification ──────────────────────────────────────

    /**
     * Heuristic permission risk-level classification.
     */
    fun getPermissionRiskLevel(permission: String): PermissionDangerousLevel {
        val p = permission.uppercase()

        val systemSet = setOf(
            "ANDROID.PERMISSION.SYSTEM_ALERT_WINDOW",
            "ANDROID.PERMISSION.WRITE_SETTINGS"
        )
        val signatureSet = setOf(
            "ANDROID.PERMISSION.BIND_ACCESSIBILITY_SERVICE",
            "ANDROID.PERMISSION.BIND_DEVICE_ADMIN"
        )
        val dangerousSet = setOf(
            "ANDROID.PERMISSION.READ_SMS",
            "ANDROID.PERMISSION.RECEIVE_SMS",
            "ANDROID.PERMISSION.SEND_SMS",
            "ANDROID.PERMISSION.READ_CONTACTS",
            "ANDROID.PERMISSION.WRITE_CONTACTS",
            "ANDROID.PERMISSION.ACCESS_FINE_LOCATION",
            "ANDROID.PERMISSION.ACCESS_COARSE_LOCATION",
            "ANDROID.PERMISSION.CAMERA",
            "ANDROID.PERMISSION.RECORD_AUDIO",
            "ANDROID.PERMISSION.READ_CALL_LOG",
            "ANDROID.PERMISSION.WRITE_CALL_LOG",
            "ANDROID.PERMISSION.READ_EXTERNAL_STORAGE",
            "ANDROID.PERMISSION.WRITE_EXTERNAL_STORAGE",
            "ANDROID.PERMISSION.MANAGE_EXTERNAL_STORAGE",
            "ANDROID.PERMISSION.READ_PHONE_STATE",
            "ANDROID.PERMISSION.CALL_PHONE"
        )

        return when {
            systemSet.contains(p)    -> PermissionDangerousLevel.SYSTEM
            signatureSet.contains(p) -> PermissionDangerousLevel.SIGNATURE
            dangerousSet.contains(p) -> PermissionDangerousLevel.DANGEROUS
            else                     -> PermissionDangerousLevel.NORMAL
        }
    }
}
