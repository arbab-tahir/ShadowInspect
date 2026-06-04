package com.shadowinspect.app.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.shadowinspect.app.data.db.converter.AppConverters

/**
 * Types of images that can be scanned
 */
enum class ImageType {
    JPEG,
    PNG,
    GIF,
    BMP,
    WEBP,
    HEIC,
    TIFF,
    RAW,
    UNKNOWN
}

/**
 * Image threat categories
 */
enum class ImageThreatType {
    STEGANOGRAPHY,
    METADATA_LEAK,
    GPS_LOCATION,
    MANIPULATED_IMAGE,
    ILLEGAL_CONTENT,
    EMBEDDED_URL,
    MALICIOUS_SCRIPT
}

/**
 * GPS coordinates from image
 */
data class GpsCoordinates(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val accuracy: Float? = null
) {
    fun toLatLngString(): String = String.format("%.6f, %.6f", latitude, longitude)
}

/**
 * EXIF metadata extracted from image
 */
data class ExifMetadata(
    val make: String? = null,
    val model: String? = null,
    val dateTime: String? = null,
    val gpsCoordinates: GpsCoordinates? = null,
    val imageWidth: Int? = null,
    val imageHeight: Int? = null,
    val orientation: Int? = null,
    val exposureTime: String? = null,
    val fNumber: Float? = null,
    val iso: Int? = null,
    val flash: Boolean? = null,
    val focalLength: Float? = null,
    val software: String? = null,
    val artist: String? = null,
    val copyright: String? = null,
    val userComment: String? = null
)

/**
 * Steganography detection result
 */
data class SteganographyResult(
    val hasHiddenData: Boolean,
    val confidenceScore: Int,
    val method: String?,
    val estimatedSize: Long? = null,
    val extractedData: ByteArray? = null,
    val extractionNotes: String? = null
)

/**
 * Image analysis result
 */
@Entity(tableName = "image_scans")
@TypeConverters(AppConverters::class)
data class ImageAnalysisResult(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val imageType: ImageType,
    val mimeType: String,

    val hashSha256: String,
    val hashMd5: String,

    val imageWidth: Int,
    val imageHeight: Int,
    val bitDepth: Int,
    val hasAlpha: Boolean = false,

    val exifMetadata: ExifMetadata? = null,
    val hasExifData: Boolean = false,

    val steganographyResult: SteganographyResult? = null,
    val hasHiddenData: Boolean = false,

    val embeddedUrls: List<String> = emptyList(),
    val suspiciousUrls: List<String> = emptyList(),

    val containsGpsLocation: Boolean = false,
    val gpsCoordinates: GpsCoordinates? = null,

    val isManipulated: Boolean = false,
    val manipulationConfidence: Int = 0,

    val riskScore: Int,
    val riskLevel: String,
    val threatCategories: List<ImageThreatType>,

    val mitreTechniques: List<String> = emptyList(),

    val explanation: String,
    val recommendations: List<String>,

    val timestamp: Long = System.currentTimeMillis(),

    val isError: Boolean = false,
    val errorMessage: String? = null
) {
    companion object {
        fun error(fileName: String, message: String): ImageAnalysisResult {
            return ImageAnalysisResult(
                fileName = fileName,
                filePath = "",
                fileSize = 0,
                imageType = ImageType.UNKNOWN,
                mimeType = "",
                hashSha256 = "",
                hashMd5 = "",
                imageWidth = 0,
                imageHeight = 0,
                bitDepth = 0,
                riskScore = 0,
                riskLevel = "ERROR",
                threatCategories = emptyList(),
                explanation = "Analysis failed: $message",
                recommendations = emptyList(),
                isError = true,
                errorMessage = message
            )
        }
    }
}
