package com.shadowinspect.app.domain.analyzer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.shadowinspect.app.data.analysis.image.ExifExtractor
import com.shadowinspect.app.data.analysis.image.SteganographyDetector
import com.shadowinspect.app.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageAnalyzer @Inject constructor(
    private val exifExtractor: ExifExtractor,
    private val stegDetector: SteganographyDetector
) {

    private val urlPattern = Pattern.compile(
        "((https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])"
    )

    /**
     * Analyze image file
     */
    suspend fun analyzeImage(file: File): ImageAnalysisResult = withContext(Dispatchers.IO) {
        try {
            val imageType = detectImageType(file)
            val sha256 = calculateHash(file, "SHA-256")
            val md5 = calculateHash(file, "MD5")

            // Get image dimensions
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, options)

            val width = options.outWidth
            val height = options.outHeight
            val bitDepth = getBitDepth(file)

            // Extract EXIF data
            val exif = exifExtractor.extractExif(file)
            val hasGps = exif?.gpsCoordinates != null
            val hasExif = exif != null && (exif.make != null || exif.model != null ||
                    exif.dateTime != null || exif.gpsCoordinates != null || exif.software != null)

            // Detect steganography
            val stegResult = stegDetector.detectHiddenData(file)

            // Check for manipulation
            val manipulationResult = detectManipulation(file)

            // Identify threat categories
            val threatCategories = identifyThreatCategories(
                hasGps = hasGps,
                hasExif = hasExif,
                stegResult = stegResult,
                manipulationResult = manipulationResult
            )

            // Calculate risk score
            val riskScore = calculateRiskScore(
                threatCategories = threatCategories,
                stegConfidence = stegResult.confidenceScore,
                hasGps = hasGps
            )

            // Generate explanation
            val explanation = generateExplanation(riskScore, threatCategories, stegResult)

            // Generate recommendations
            val recommendations = generateRecommendations(threatCategories, hasGps, hasExif)

            ImageAnalysisResult(
                fileName = file.name,
                filePath = file.absolutePath,
                fileSize = file.length(),
                imageType = imageType,
                mimeType = getMimeType(file),
                hashSha256 = sha256,
                hashMd5 = md5,
                imageWidth = width,
                imageHeight = height,
                bitDepth = bitDepth,
                exifMetadata = exif,
                hasExifData = hasExif,
                steganographyResult = stegResult,
                hasHiddenData = stegResult.hasHiddenData,
                containsGpsLocation = hasGps,
                gpsCoordinates = exif?.gpsCoordinates,
                isManipulated = manipulationResult.first,
                manipulationConfidence = manipulationResult.second,
                riskScore = riskScore,
                riskLevel = getRiskLevel(riskScore),
                threatCategories = threatCategories,
                explanation = explanation,
                recommendations = recommendations
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ImageAnalysisResult.error(file.name, e.message ?: "Unknown error")
        }
    }

    private fun detectImageType(file: File): ImageType {
        // First: detect from magic bytes (works for WhatsApp/shared images with wrong extensions)
        try {
            val header = ByteArray(12)
            FileInputStream(file).use { it.read(header) }

            // Check magic bytes
            if (header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte() && header[2] == 0xFF.toByte()) {
                return ImageType.JPEG
            }
            if (header[0] == 0x89.toByte() && header[1] == 0x50.toByte() &&
                header[2] == 0x4E.toByte() && header[3] == 0x47.toByte()) {
                return ImageType.PNG
            }
            if (header[0] == 0x47.toByte() && header[1] == 0x49.toByte() && header[2] == 0x46.toByte()) {
                return ImageType.GIF
            }
            if (header[0] == 0x42.toByte() && header[1] == 0x4D.toByte()) {
                return ImageType.BMP
            }
            // WEBP: starts with RIFF....WEBP
            if (header[0] == 0x52.toByte() && header[1] == 0x49.toByte() &&
                header[2] == 0x46.toByte() && header[3] == 0x46.toByte() &&
                header[8] == 0x57.toByte() && header[9] == 0x45.toByte() &&
                header[10] == 0x42.toByte() && header[11] == 0x50.toByte()) {
                return ImageType.WEBP
            }
        } catch (_: Exception) { }

        // Fallback: detect from file extension
        return when (file.extension.lowercase()) {
            "jpg", "jpeg" -> ImageType.JPEG
            "png" -> ImageType.PNG
            "gif" -> ImageType.GIF
            "bmp" -> ImageType.BMP
            "webp" -> ImageType.WEBP
            "heic" -> ImageType.HEIC
            "tiff", "tif" -> ImageType.TIFF
            else -> ImageType.UNKNOWN
        }
    }

    private fun calculateHash(file: File, algorithm: String): String {
        val digest = MessageDigest.getInstance(algorithm)
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun getBitDepth(file: File): Int {
        return try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return 24
            val config = bitmap.config
            bitmap.recycle()

            when (config) {
                Bitmap.Config.ARGB_8888 -> 32
                Bitmap.Config.RGB_565 -> 16
                @Suppress("DEPRECATION")
                Bitmap.Config.ARGB_4444 -> 16
                Bitmap.Config.ALPHA_8 -> 8
                else -> 24
            }
        } catch (e: Exception) {
            24
        }
    }

    private fun detectManipulation(file: File): Pair<Boolean, Int> {
        // Simplified manipulation detection
        // In production, use ELA (Error Level Analysis) or similar
        return Pair(false, 0)
    }

    private fun identifyThreatCategories(
        hasGps: Boolean,
        hasExif: Boolean,
        stegResult: SteganographyResult,
        manipulationResult: Pair<Boolean, Int>
    ): List<ImageThreatType> {
        val threats = mutableListOf<ImageThreatType>()

        if (stegResult.hasHiddenData) {
            threats.add(ImageThreatType.STEGANOGRAPHY)
        }

        if (hasGps) {
            threats.add(ImageThreatType.GPS_LOCATION)
        }

        if (hasExif) {
            threats.add(ImageThreatType.METADATA_LEAK)
        }

        if (manipulationResult.first) {
            threats.add(ImageThreatType.MANIPULATED_IMAGE)
        }

        return threats
    }

    private fun calculateRiskScore(
        threatCategories: List<ImageThreatType>,
        stegConfidence: Int,
        hasGps: Boolean
    ): Int {
        var score = 0

        if (threatCategories.contains(ImageThreatType.STEGANOGRAPHY)) {
            score += (stegConfidence * 0.7).toInt()
        }

        if (hasGps) score += 30

        if (threatCategories.contains(ImageThreatType.METADATA_LEAK)) {
            score += 10
        }

        return score.coerceIn(0, 100)
    }

    private fun getRiskLevel(score: Int): String {
        return when {
            score >= 70 -> "CRITICAL"
            score >= 50 -> "HIGH"
            score >= 30 -> "MEDIUM"
            score >= 10 -> "LOW"
            else -> "SAFE"
        }
    }

    private fun generateExplanation(
        score: Int,
        threats: List<ImageThreatType>,
        stegResult: SteganographyResult
    ): String {
        val parts = mutableListOf<String>()

        when {
            score >= 70 -> parts.add("⚠️ CRITICAL: Image contains hidden data and sensitive metadata.")
            score >= 50 -> parts.add("⚠️ HIGH RISK: Suspicious elements detected in image.")
            score >= 30 -> parts.add("⚡ MEDIUM RISK: Some privacy concerns detected.")
            score >= 10 -> parts.add("🔵 LOW RISK: Minor metadata present.")
            else -> parts.add("✅ SAFE: No significant threats detected.")
        }

        if (threats.contains(ImageThreatType.STEGANOGRAPHY)) {
            val details = stegResult.extractionNotes ?: "via ${stegResult.method ?: "unknown"} method"
            parts.add("Hidden data detected $details with ${stegResult.confidenceScore}% confidence.")
        }

        if (threats.contains(ImageThreatType.GPS_LOCATION)) {
            parts.add("GPS location data is embedded in this image.")
        }

        if (threats.contains(ImageThreatType.METADATA_LEAK)) {
            parts.add("Camera/device metadata is exposed in this image.")
        }

        return parts.joinToString("\n")
    }

    private fun generateRecommendations(
        threats: List<ImageThreatType>,
        hasGps: Boolean,
        hasExif: Boolean
    ): List<String> {
        val recommendations = mutableListOf<String>()

        if (threats.contains(ImageThreatType.STEGANOGRAPHY)) {
            recommendations.add("Image may contain hidden data — verify source before sharing")
            recommendations.add("Do not open this image on sensitive systems")
        }

        if (hasGps) {
            recommendations.add("Remove GPS metadata before sharing online to protect your location privacy")
        }

        if (hasExif) {
            recommendations.add("Strip EXIF metadata before sharing to prevent device information leakage")
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Image appears safe — always verify image sources before trusting them")
        }

        return recommendations
    }

    private fun getMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            "webp" -> "image/webp"
            "heic" -> "image/heic"
            else -> "image/*"
        }
    }
}
