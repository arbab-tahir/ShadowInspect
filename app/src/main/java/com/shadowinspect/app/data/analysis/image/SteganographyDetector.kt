package com.shadowinspect.app.data.analysis.image

import android.graphics.BitmapFactory
import com.shadowinspect.app.domain.model.SteganographyResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class SteganographyDetector @Inject constructor() {

    /**
     * Detect hidden data in image using multiple methods
     */
    suspend fun detectHiddenData(file: File): SteganographyResult = withContext(Dispatchers.IO) {
        val results = mutableListOf<DetectionMethod>()

        results.add(checkFileSizeAnomaly(file))
        results.add(checkLSBAnalysis(file))
        results.add(checkColorDistribution(file))
        results.add(checkMetadataAnomaly(file))

        // Only consider the highest confidence method as the primary driver
        val primaryMethod = results.maxByOrNull { it.confidence }

        // Stricter threshold: require at least 60% confidence to flag as hidden data
        val hasHiddenData = (primaryMethod?.confidence ?: 0) >= 60

        SteganographyResult(
            hasHiddenData = hasHiddenData,
            confidenceScore = primaryMethod?.confidence ?: 0,
            method = primaryMethod?.methodName,
            estimatedSize = estimateHiddenDataSize(file),
            extractedData = null,
            // Provide the specific detailed description for the UI
            extractionNotes = if (hasHiddenData) primaryMethod?.description else null
        )
    }

    /**
     * Check for file size anomalies
     */
    private fun checkFileSizeAnomaly(file: File): DetectionMethod {
        val fileSize = file.length()

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)

        val width = options.outWidth
        val height = options.outHeight

        if (width <= 0 || height <= 0) {
            return DetectionMethod("SizeAnomaly", false, 0, "Image dimensions could not be read")
        }

        // Uncompressed RGB size (no alpha)
        val expectedSize = width.toLong() * height * 3
        val compressionRatio = fileSize.toDouble() / expectedSize

        // Natural photos and JPEGs are heavily compressed (ratio < 0.2 usually).
        // If an image is suspiciously close to its raw RGB size (e.g., a PNG or BMP injected with data),
        // we flag it. Ratios > 1.0 mean it's larger than RAW RGB, which mathematically shouldn't happen
        // unless there's an enormous chunk of appended data (EOF injection).
        val (confidence, desc) = when {
            compressionRatio > 1.2 -> Pair(95, "Appended data detected (EOF injection): File is mathematically larger than its raw pixels.")
            compressionRatio > 0.95 -> Pair(75, "Suspiciously large file size: Image lacks expected compression, indicating potential payload.")
            compressionRatio > 0.8 -> Pair(40, "Unusually low compression ratio.")
            else -> Pair(0, "File size aligns with normal compression ratios.")
        }

        return DetectionMethod(
            methodName = "FileSizeAnomaly",
            detected = confidence >= 60,
            confidence = confidence,
            description = desc
        )
    }

    /**
     * Basic LSB (Least Significant Bit) analysis
     */
    private fun checkLSBAnalysis(file: File): DetectionMethod {
        return try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                ?: return DetectionMethod("LSBAnalysis", false, 0, "Could not decode image for LSB analysis")

            var totalPixels = 0
            var lsbZeros = 0
            var lsbOnes = 0

            val width = bitmap.width
            val height = bitmap.height

            // Sample every 5th pixel
            for (x in 0 until width step 5) {
                for (y in 0 until height step 5) {
                    val pixel = bitmap.getPixel(x, y)

                    val r = (pixel shr 16) and 0xFF
                    val g = (pixel shr 8) and 0xFF
                    val b = pixel and 0xFF

                    if (r and 1 == 0) lsbZeros++ else lsbOnes++
                    if (g and 1 == 0) lsbZeros++ else lsbOnes++
                    if (b and 1 == 0) lsbZeros++ else lsbOnes++

                    totalPixels += 3
                }
            }

            bitmap.recycle()

            if (totalPixels == 0) return DetectionMethod("LSBAnalysis", false, 0, "No pixels analyzed")

            val oneRatio = lsbOnes.toDouble() / totalPixels
            
            // Natural images generally have unpredictable LSBs (around 50% 1s),
            // but dark images or low-texture flats might naturally skew 0.
            // If the LSB ratio is exactly 50.00% across the board, or extremely dense (>95%),
            // it indicates artificial LSB manipulation.
            val balance = abs(oneRatio - 0.5)
            
            val (confidence, desc) = when {
                balance < 0.001 -> Pair(85, "Perfectly uniform LSB distribution detected, highly characteristic of encrypted LSB steganography.")
                oneRatio > 0.95 -> Pair(80, "Extremely high density of LSB 1s, indicating potential dense payload injection.")
                balance > 0.45 -> Pair(20, "Natural extreme bit skew (e.g. solid black/white areas).") 
                else -> Pair(5, "Normal LSB distribution for a photograph.")
            }

            DetectionMethod(
                methodName = "LSBAnalysis",
                detected = confidence >= 60,
                confidence = confidence,
                description = desc
            )
        } catch (e: Exception) {
            DetectionMethod("LSBAnalysis", false, 0, "Error performing LSB analysis")
        }
    }

    /**
     * Check for unusual color distribution using PoV (Pairs of Values) concepts
     */
    private fun checkColorDistribution(file: File): DetectionMethod {
        return try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                ?: return DetectionMethod("ColorDistribution", false, 0, "Could not decode for color analysis")

            // We only look at Grayscale intensity to detect LSB PoV banding
            val hist = IntArray(256)

            val width = bitmap.width
            val height = bitmap.height
            var totalCount = 0

            for (x in 0 until width step 4) {
                for (y in 0 until height step 4) {
                    val pixel = bitmap.getPixel(x, y)
                    val r = (pixel shr 16) and 0xFF
                    val g = (pixel shr 8) and 0xFF
                    val b = pixel and 0xFF
                    
                    // Standard luminance
                    val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt().coerceIn(0, 255)
                    hist[lum]++
                    totalCount++
                }
            }

            bitmap.recycle()

            if (totalCount == 0) return DetectionMethod("ColorDistribution", false, 0, "No color data")

            // Real steganography often equalizes pairs of color values (e.g., 2v and 2v+1 occur equally often)
            // Normal images (especially compressed or banded ones) will just have flat segments.
            // We check for *consistent* pairwise equalization across the entire spectrum.
            var povAnomalies = 0
            var validPairs = 0
            
            for (i in 0 until 254 step 2) {
                val p1 = hist[i]
                val p2 = hist[i + 1]
                
                // Only evaluate pairs with statistical significance
                if (p1 > 100 || p2 > 100) {
                    validPairs++
                    // If the adjacent pair frequencies are exactly equal or within 1% (highly unnatural in photography)
                    val diff = abs(p1 - p2).toDouble()
                    val max = maxOf(p1, p2).toDouble()
                    if (diff / max < 0.01) {
                        povAnomalies++
                    }
                }
            }

            // Calculate percentage of valid pairs that are anomalously equal
            val anomalyRatio = if (validPairs > 20) povAnomalies.toDouble() / validPairs else 0.0

            val (confidence, desc) = when {
                anomalyRatio > 0.85 -> Pair(90, "Severe color pair anomalies detected (>\$85% PoV equalization): Strong signature of LSB replacement.")
                anomalyRatio > 0.60 -> Pair(70, "Significant color value pairing detected: Frequent in histogram-shifting steganography.")
                anomalyRatio > 0.40 -> Pair(30, "Minor color banding or natural flat tones detected.")
                else -> Pair(5, "Natural chromatic distribution.")
            }

            DetectionMethod(
                methodName = "ColorDistribution",
                detected = confidence >= 60,
                confidence = confidence,
                description = desc
            )
        } catch (e: Exception) {
            DetectionMethod("ColorDistribution", false, 0, "Error analyzing colors")
        }
    }

    /**
     * Check for metadata anomalies
     */
    private fun checkMetadataAnomaly(file: File): DetectionMethod {
        return try {
            val exif = android.media.ExifInterface(file.absolutePath)

            val userComment = exif.getAttribute(android.media.ExifInterface.TAG_USER_COMMENT)
            val commentLength = userComment?.length ?: 0

            val (confidence, desc) = when {
                commentLength > 5000 -> Pair(95, "Massive metadata payload ($commentLength chars): Highly suspicious EXIF data injection.")
                commentLength > 1000 -> Pair(80, "Suspiciously long user comment in EXIF metadata ($commentLength chars).")
                commentLength > 500 -> Pair(60, "Unusually long comment field in metadata.")
                else -> Pair(0, "Metadata fields appear normal.")
            }

            DetectionMethod(
                methodName = "MetadataAnomaly",
                detected = confidence >= 60,
                confidence = confidence,
                description = desc
            )
        } catch (e: Exception) {
            DetectionMethod("MetadataAnomaly", false, 0, "Error reading metadata")
        }
    }

    /**
     * Estimate size of hidden data
     */
    private fun estimateHiddenDataSize(file: File): Long? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)

        val width = options.outWidth
        val height = options.outHeight

        if (width <= 0 || height <= 0) return null

        val maxHiddenBits = width.toLong() * height * 3
        return maxHiddenBits / 8
    }

    private data class DetectionMethod(
        val methodName: String,
        val detected: Boolean,
        val confidence: Int,
        val description: String
    )
}
