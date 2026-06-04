package com.shadowinspect.app.domain.ml

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Types of ML models available
 */
enum class ModelType {
    CNN_LSTM,           // 97.4% accuracy - balanced
    RANDOM_FOREST,      // 95.2% accuracy - faster
    LIGHTWEIGHT,        // 93.8% accuracy - minimal resources
    ENSEMBLE            // Combined models for highest accuracy
}

/**
 * ML detection result
 */
data class MLDetectionResult(
    val isMalicious: Boolean,
    val confidence: Float,              // 0-1
    val threatClass: String,            // "Banking Trojan", "Spyware", etc.
    val threatFamily: String? = null,
    val topClasses: List<PredictedClass>,
    val modelType: ModelType,
    val inferenceTimeMs: Long,
    val featureImportance: Map<String, Float>? = null
)

/**
 * Predicted class with probability
 */
data class PredictedClass(
    val className: String,
    val probability: Float
)

/**
 * Features extracted from APK for ML
 */
data class ApkFeatureVector(
    val permissions: FloatArray,         // 50+ permission features
    val apiCalls: FloatArray,            // API call frequencies
    val opcodes: FloatArray,             // OpCode distributions
    val suspiciousStrings: Float,        // Count of suspicious strings
    val obfuscationScore: Float,         // 0-1 obfuscation level
    val urlCount: Float,                 // Number of URLs found
    val dangerousPermissions: Float,     // Count of dangerous perms
    val packageName: String? = null,
    val fileSize: Long = 0
) {
    fun toFloatArray(): FloatArray {
        // Combine all features into single array for model input
        val totalSize = permissions.size + apiCalls.size + opcodes.size + 4
        val result = FloatArray(totalSize)
        
        var index = 0
        permissions.copyInto(result, index)
        index += permissions.size
        
        apiCalls.copyInto(result, index)
        index += apiCalls.size
        
        opcodes.copyInto(result, index)
        index += opcodes.size
        
        result[index++] = suspiciousStrings
        result[index++] = obfuscationScore
        result[index++] = urlCount
        result[index] = dangerousPermissions
        
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ApkFeatureVector

        if (!permissions.contentEquals(other.permissions)) return false
        if (!apiCalls.contentEquals(other.apiCalls)) return false
        if (!opcodes.contentEquals(other.opcodes)) return false
        if (suspiciousStrings != other.suspiciousStrings) return false
        if (obfuscationScore != other.obfuscationScore) return false
        if (urlCount != other.urlCount) return false
        if (dangerousPermissions != other.dangerousPermissions) return false

        return true
    }

    override fun hashCode(): Int {
        var result = permissions.contentHashCode()
        result = 31 * result + apiCalls.contentHashCode()
        result = 31 * result + opcodes.contentHashCode()
        result = 31 * result + suspiciousStrings.hashCode()
        result = 31 * result + obfuscationScore.hashCode()
        result = 31 * result + urlCount.hashCode()
        result = 31 * result + dangerousPermissions.hashCode()
        return result
    }
}

/**
 * ML model metadata
 */
@Entity(tableName = "ml_models")
data class MLModelMetadata(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val modelName: String,
    val modelType: ModelType,
    val version: String,
    val accuracy: Float,
    val precision: Float,
    val recall: Float,
    val f1Score: Float,
    val sizeBytes: Long,
    val isActive: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis(),
    val description: String? = null
)

/**
 * Training data point for model improvement
 */
@Entity(tableName = "ml_training_data")
data class MLTrainingData(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val scanId: Long,                    // Reference to original scan
    val features: String,                 // JSON serialized features
    val modelPrediction: Boolean,
    val userFeedback: Boolean? = null,    // User said correct/incorrect
    val actualResult: Boolean? = null,    // Verified result (if known)
    val timestamp: Long = System.currentTimeMillis(),
    val isUploaded: Boolean = false
)

/**
 * Model performance metrics
 */
data class ModelPerformance(
    val modelType: ModelType,
    val predictions: Int,
    val correctPredictions: Int,
    val accuracy: Float,
    val avgConfidence: Float,
    val avgInferenceTime: Float,
    val lastUpdated: Long
)

/**
 * Feature importance for explainability
 */
data class FeatureImportance(
    val featureName: String,
    val importance: Float,
    val category: String  // "permission", "api", "opcode", etc.
)
