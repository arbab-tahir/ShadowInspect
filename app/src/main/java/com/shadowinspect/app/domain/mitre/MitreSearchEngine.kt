package com.shadowinspect.app.domain.mitre

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MitreSearchEngine @Inject constructor() {
    fun searchTechniques(query: String): List<MitreTechnique> {
        val lowerQuery = query.lowercase()
        return MitreTechniqueDatabase.techniques.filter { technique ->
            technique.name.lowercase().contains(lowerQuery) ||
            technique.id.lowercase().contains(lowerQuery) ||
            technique.description.lowercase().contains(lowerQuery) ||
            technique.tactics.any { it.lowercase().contains(lowerQuery) }
        }
    }

    fun searchByPermission(permission: String): List<MitreTechnique> {
        return MitreTechniqueDatabase.techniques
            .filter { it.permissionsRequired.contains(permission) }
    }

    fun searchByTactic(tactic: String): List<MitreTechnique> {
        return MitreTechniqueDatabase.techniques
            .filter { it.tactics.contains(tactic) }
    }

    fun getTechniquesByRiskLevel(riskScore: Int): Map<String, List<MitreTechnique>> {
        return when {
            riskScore >= 70 -> mapOf("CRITICAL" to MitreTechniqueDatabase.techniques.take(5))
            riskScore >= 50 -> mapOf("HIGH" to MitreTechniqueDatabase.techniques.take(3))
            riskScore >= 30 -> mapOf("MEDIUM" to MitreTechniqueDatabase.techniques.take(2))
            else -> mapOf("LOW" to emptyList())
        }
    }
}
