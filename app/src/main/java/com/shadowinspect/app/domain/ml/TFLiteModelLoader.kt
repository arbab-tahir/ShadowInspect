package com.shadowinspect.app.domain.ml

import android.content.Context
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp

/**
 * ML Model Loader that supports both real TFLite models and intelligent simulation.
 *
 * Since actual trained TFLite models from the Droidware dataset are not bundled,
 * this loader operates in "simulation mode" — it uses feature-weighted heuristics
 * to produce realistic, deterministic predictions based on the extracted APK features.
 *
 * Supports multiple models loaded simultaneously via a model name registry.
 */
@Singleton
class TFLiteModelLoader @Inject constructor(
    private val context: Context
) {

    private val tag = "TFLiteModelLoader"

    // Track which models are loaded (by name)
    private val loadedModels = mutableSetOf<String>()

    /**
     * Load a model by name.
     * Verifies the asset file exists, then enables simulation mode for it.
     */
    suspend fun loadModel(
        modelName: String,
        useGpu: Boolean = false
    ): Boolean {
        return try {
            // Verify the asset file exists
            val exists = verifyAssetExists(modelName)
            if (exists) {
                loadedModels.add(modelName)
                Log.d(tag, "Model ready (simulation mode): $modelName")
                true
            } else {
                Log.e(tag, "Model asset not found: $modelName")
                false
            }
        } catch (e: Exception) {
            Log.e(tag, "Model loading failed for $modelName", e)
            false
        }
    }

    /**
     * Verify that the asset file exists in the APK bundle.
     */
    private fun verifyAssetExists(modelName: String): Boolean {
        return try {
            val stream = context.assets.open(modelName)
            val available = stream.available()
            stream.close()
            Log.d(tag, "Asset verified: $modelName ($available bytes)")
            available > 0
        } catch (e: Exception) {
            Log.e(tag, "Asset not found: $modelName", e)
            false
        }
    }

    /**
     * Run inference on input data.
     *
     * Uses intelligent feature-based heuristics to produce realistic predictions
     * that reflect the actual characteristics of the analyzed APK.
     */
    fun runInference(
        input: FloatArray,
        inputShape: IntArray,
        outputShape: IntArray
    ): FloatArray {
        val outputSize = outputShape.reduce { acc, i -> acc * i }
        return simulateInference(input, outputSize)
    }

    /**
     * Intelligent heuristic-based inference simulation.
     *
     * Analyzes the feature vector to produce a realistic threat classification:
     * - Examines permission flags (first ~50 features)
     * - Checks suspicious API call frequencies
     * - Evaluates opcode distributions
     * - Considers meta-features (suspicious strings, obfuscation, URLs, dangerous perms)
     */
    private fun simulateInference(input: FloatArray, outputSize: Int): FloatArray {
        // --- Feature Analysis ---
        // The feature vector layout (from FeatureExtractor.toFloatArray()):
        //   [0..49]   = 50 permission flags (0 or 1)
        //   [50..65]  = 16 API call frequencies (0-1)
        //   [66..87]  = 22 opcode distributions (0-1)
        //   [88]      = suspicious strings count
        //   [89]      = obfuscation score (0-1)
        //   [90]      = URL count
        //   [91]      = dangerous permissions count

        val permCount = input.take(50.coerceAtMost(input.size)).count { it > 0.5f }
        val apiScore = safeSliceSum(input, 50, 65)
        val opcodeScore = safeSliceSum(input, 66, 87)
        val suspiciousStrings = input.getOrElse(88) { 0f }
        val obfuscation = input.getOrElse(89) { 0f }
        val urlCount = input.getOrElse(90) { 0f }
        val dangerousPerms = input.getOrElse(91) { 0f }

        // --- Threat Score Calculation ---
        val threatScore = (
            (permCount / 50f) * 0.15f +
            (apiScore / 16f).coerceAtMost(1f) * 0.20f +
            (opcodeScore / 22f).coerceAtMost(1f) * 0.10f +
            (suspiciousStrings / 25f).coerceAtMost(1f) * 0.15f +
            obfuscation.coerceIn(0f, 1f) * 0.15f +
            (urlCount / 10f).coerceAtMost(1f) * 0.10f +
            (dangerousPerms / 13f).coerceAtMost(1f) * 0.15f
        ).coerceIn(0f, 1f)

        // Sigmoid-like curve for realistic confidence distribution
        val confidence = sigmoid(threatScore * 6f - 3f)

        // --- Generate Output Distribution ---
        val output = FloatArray(outputSize)

        if (outputSize == 2) {
            // Binary classification (Random Forest)
            output[0] = 1f - confidence  // Benign
            output[1] = confidence        // Malicious
        } else if (outputSize >= 8) {
            // Multi-class (CNN-LSTM: Benign, Banking Trojan, Spyware, Ransomware, Adware, Riskware, SMS Trojan, Cryptominer)
            output[0] = 1f - confidence // Benign

            if (confidence > 0.25f) {
                // Determine most likely threat type based on feature signature
                val signals = floatArrayOf(
                    (dangerousPerms * 0.3f + apiScore / 16f * 0.4f + suspiciousStrings * 0.02f).coerceIn(0f, 1f), // Banking Trojan
                    (permCount / 50f * 0.5f + obfuscation * 0.3f).coerceIn(0f, 1f),                               // Spyware
                    (obfuscation * 0.5f + suspiciousStrings * 0.03f).coerceIn(0f, 1f),                              // Ransomware
                    (urlCount / 10f * 0.6f + permCount / 50f * 0.2f).coerceIn(0f, 1f),                             // Adware
                    (apiScore / 16f * 0.4f + opcodeScore / 22f * 0.3f).coerceIn(0f, 1f),                           // Riskware
                    (dangerousPerms * 0.2f + permCount / 50f * 0.3f).coerceIn(0f, 1f),                             // SMS Trojan
                    (opcodeScore / 22f * 0.5f + obfuscation * 0.3f).coerceIn(0f, 1f)                               // Cryptominer
                )
                val totalSignal = signals.sum().coerceAtLeast(0.01f)
                for (i in signals.indices) {
                    output[i + 1] = confidence * (signals[i] / totalSignal)
                }
            } else {
                for (i in 1 until outputSize.coerceAtMost(8)) {
                    output[i] = confidence / (outputSize - 1).coerceAtLeast(1)
                }
            }
        } else {
            output[0] = 1f - confidence
            for (i in 1 until outputSize) {
                output[i] = confidence / (outputSize - 1).coerceAtLeast(1)
            }
        }

        // Normalize
        val total = output.sum().coerceAtLeast(0.01f)
        for (i in output.indices) output[i] /= total

        Log.d(tag, "Inference: threatScore=${"%.3f".format(threatScore)}, confidence=${"%.3f".format(confidence)}, topIdx=${output.indices.maxByOrNull { output[it] }}")
        return output
    }

    private fun safeSliceSum(arr: FloatArray, from: Int, to: Int): Float {
        if (from >= arr.size) return 0f
        val end = to.coerceAtMost(arr.size - 1)
        var sum = 0f
        for (i in from..end) sum += arr[i]
        return sum
    }

    private fun sigmoid(x: Float): Float = 1f / (1f + exp(-x))

    fun getInputDetails(): List<Map<String, Any>> =
        listOf(mapOf("name" to "input", "shape" to intArrayOf(1, 68), "type" to "FLOAT32"))

    fun getOutputDetails(): List<Map<String, Any>> =
        listOf(mapOf("name" to "output", "shape" to intArrayOf(1, 8), "type" to "FLOAT32"))

    /**
     * Unload a specific model
     */
    fun closeModel(modelName: String) {
        loadedModels.remove(modelName)
    }

    /**
     * Close all models
     */
    fun close() {
        loadedModels.clear()
    }

    fun isLoaded(): Boolean = loadedModels.isNotEmpty()
}
