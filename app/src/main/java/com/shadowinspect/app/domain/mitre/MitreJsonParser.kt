package com.shadowinspect.app.domain.mitre

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MitreJsonParser @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {

    private val gson = Gson()
    private val tag = "MitreJsonParser"

    // Cache for parsed techniques
    private var techniqueCache: List<MitreTechnique>? = null

    /**
     * Load and parse MITRE ATT&CK JSON files
     */
    suspend fun loadMitreData(): List<MitreTechnique> = withContext(Dispatchers.IO) {
        if (techniqueCache != null) {
            return@withContext techniqueCache!!
        }

        val techniques = mutableListOf<MitreTechnique>()

        try {
            // Load mobile attack data
            context.assets.open("mitre/mobile-attack.json").use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val jsonObject = JsonParser.parseReader(reader).asJsonObject
                    val objects = jsonObject.getAsJsonArray("objects")

                    techniques.addAll(parseMitreObjects(objects, "mobile"))
                }
            }

            // Load enterprise attack data (for techniques that also apply to mobile)
            try {
                context.assets.open("mitre/enterprise-attack.json").use { inputStream ->
                    InputStreamReader(inputStream).use { reader ->
                        val jsonObject = JsonParser.parseReader(reader).asJsonObject
                        val objects = jsonObject.getAsJsonArray("objects")

                        // Only add techniques that are marked as applicable to mobile
                        val mobileTechniques = parseMitreObjects(objects, "enterprise")
                            .filter { it.platforms.contains("Android") || it.platforms.contains("iOS") }

                        techniques.addAll(mobileTechniques)
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Enterprise attack data not loaded: ${e.message}")
            }

            Log.d(tag, "Loaded ${techniques.size} MITRE techniques")
            techniqueCache = techniques.distinctBy { it.id }
            return@withContext techniqueCache!!

        } catch (e: Exception) {
            Log.e(tag, "Failed to load MITRE data", e)
            // Fallback to built-in techniques
            return@withContext MitreTechniqueDatabase.techniques
        }
    }

    /**
     * Parse MITRE JSON objects into MitreTechnique objects
     */
    private fun parseMitreObjects(objects: com.google.gson.JsonArray, source: String): List<MitreTechnique> {
        val techniques = mutableListOf<MitreTechnique>()

        for (element in objects) {
            val obj = element.asJsonObject

            // Only process technique objects
            if (!obj.has("type") || obj.get("type").asString != "attack-pattern") {
                continue
            }

            try {
                val technique = parseTechnique(obj)
                if (technique != null) {
                    techniques.add(technique)
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to parse technique: ${e.message}")
            }
        }

        return techniques
    }

    /**
     * Parse a single technique JSON object
     */
    private fun parseTechnique(obj: JsonObject): MitreTechnique? {
        val id = extractTechniqueId(obj)
            ?: return null

        val name = obj.get("name")?.asString ?: "Unknown"
        val description = obj.get("description")?.asString ?: ""

        // Extract tactics
        val tactics = mutableListOf<String>()
        if (obj.has("kill_chain_phases")) {
            val phases = obj.getAsJsonArray("kill_chain_phases")
            for (phase in phases) {
                val phaseObj = phase.asJsonObject
                if (phaseObj.has("phase_name")) {
                    tactics.add(formatTacticName(phaseObj.get("phase_name").asString))
                }
            }
        }

        // Extract platforms
        val platforms = mutableListOf<String>()
        if (obj.has("x_mitre_platforms")) {
            val platformsArray = obj.getAsJsonArray("x_mitre_platforms")
            for (platform in platformsArray) {
                platforms.add(platform.asString)
            }
        }

        // Extract permissions (if available in x_mitre_permissions_required)
        val permissions = mutableListOf<String>()
        if (obj.has("x_mitre_permissions_required")) {
            val permsArray = obj.getAsJsonArray("x_mitre_permissions_required")
            for (perm in permsArray) {
                permissions.add(perm.asString)
            }
        }

        // Extract detection and mitigation
        val detection = obj.get("x_mitre_detection")?.asString
        val mitigation = extractMitigation(obj)

        // Build MITRE URL
        val url = "https://attack.mitre.org/techniques/${id.replace(".", "/")}/"

        return MitreTechnique(
            id = id,
            name = name,
            description = description,
            tactics = tactics,
            platforms = platforms,
            permissionsRequired = permissions,
            url = url,
            detection = detection,
            mitigation = mitigation
        )
    }

    /**
     * Extract technique ID from external references
     */
    private fun extractTechniqueId(obj: JsonObject): String? {
        if (obj.has("external_references")) {
            val refs = obj.getAsJsonArray("external_references")
            for (ref in refs) {
                val refObj = ref.asJsonObject
                if (refObj.has("source_name") &&
                    refObj.get("source_name").asString == "mitre-attack") {
                    return refObj.get("external_id")?.asString
                }
            }
        }
        return null
    }

    /**
     * Extract mitigation information
     */
    private fun extractMitigation(obj: JsonObject): String? {
        if (obj.has("x_mitre_mitigations")) {
            val mitigations = obj.getAsJsonArray("x_mitre_mitigations")
            if (mitigations.size() > 0) {
                return mitigations[0].asJsonObject.get("description")?.asString
            }
        }
        return null
    }

    /**
     * Format tactic name from snake_case to Title Case
     */
    private fun formatTacticName(tactic: String): String {
        return tactic.split("-")
            .joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase() }
            }
    }

    /**
     * Search techniques by ID or name
     */
    suspend fun searchTechniques(query: String): List<MitreTechnique> = withContext(Dispatchers.IO) {
        val techniques = loadMitreData()
        val lowerQuery = query.lowercase()

        return@withContext techniques.filter { technique ->
            technique.id.lowercase().contains(lowerQuery) ||
            technique.name.lowercase().contains(lowerQuery) ||
            technique.description.lowercase().contains(lowerQuery)
        }
    }

    /**
     * Get techniques by tactic
     */
    suspend fun getTechniquesByTactic(tactic: String): List<MitreTechnique> = withContext(Dispatchers.IO) {
        val techniques = loadMitreData()
        return@withContext techniques.filter { it.tactics.contains(tactic) }
    }

    /**
     * Get techniques by permission
     */
    suspend fun getTechniquesByPermission(permission: String): List<MitreTechnique> = withContext(Dispatchers.IO) {
        val techniques = loadMitreData()
        return@withContext techniques.filter { it.permissionsRequired.contains(permission) }
    }

    /**
     * Get technique by ID
     */
    suspend fun getTechniqueById(id: String): MitreTechnique? = withContext(Dispatchers.IO) {
        val techniques = loadMitreData()
        return@withContext techniques.find { it.id == id }
    }

    /**
     * Get all tactics
     */
    suspend fun getAllTactics(): List<String> = withContext(Dispatchers.IO) {
        val techniques = loadMitreData()
        return@withContext techniques.flatMap { it.tactics }.distinct().sorted()
    }

    /**
     * Refresh cache (useful if you update JSON files)
     */
    fun refreshCache() {
        techniqueCache = null
    }
}
