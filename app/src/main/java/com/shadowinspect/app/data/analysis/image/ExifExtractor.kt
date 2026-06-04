package com.shadowinspect.app.data.analysis.image

import android.graphics.BitmapFactory
import android.media.ExifInterface
import com.shadowinspect.app.domain.model.ExifMetadata
import com.shadowinspect.app.domain.model.GpsCoordinates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExifExtractor @Inject constructor() {

    /**
     * Extract EXIF metadata from image file.
     * For WhatsApp/shared images that strip standard EXIF tags,
     * we also try to extract metadata from raw file bytes and
     * fallback to BitmapFactory dimensions.
     */
    suspend fun extractExif(file: File): ExifMetadata? = withContext(Dispatchers.IO) {
        return@withContext try {
            val exif = ExifInterface(file.absolutePath)

            val latLongArray = FloatArray(2)
            val hasLatLong = exif.getLatLong(latLongArray)
            val altitude = exif.getAltitude(0.0)

            val gps = if (hasLatLong) {
                GpsCoordinates(
                    latitude = latLongArray[0].toDouble(),
                    longitude = latLongArray[1].toDouble(),
                    altitude = if (altitude != 0.0) altitude else null
                )
            } else null

            // Standard EXIF fields
            val make = exif.getAttribute(ExifInterface.TAG_MAKE)
            val model = exif.getAttribute(ExifInterface.TAG_MODEL)
            val dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME)
            val software = exif.getAttribute(ExifInterface.TAG_SOFTWARE)
            val artist = exif.getAttribute(ExifInterface.TAG_ARTIST)
            val copyright = exif.getAttribute(ExifInterface.TAG_COPYRIGHT)
            val userComment = exif.getAttribute(ExifInterface.TAG_USER_COMMENT)
            val exifWidth = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0).takeIf { it > 0 }
            val exifHeight = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0).takeIf { it > 0 }

            // Fallback: Use BitmapFactory for dimensions if EXIF doesn't have them
            val (bmpWidth, bmpHeight) = if (exifWidth == null || exifHeight == null) {
                getBitmapDimensions(file)
            } else {
                Pair(exifWidth, exifHeight)
            }

            // For WhatsApp images: try to extract additional metadata from raw bytes
            val rawMetadata = extractRawMetadata(file)

            ExifMetadata(
                make = make ?: rawMetadata["make"],
                model = model ?: rawMetadata["model"],
                dateTime = dateTime ?: rawMetadata["dateTime"],
                gpsCoordinates = gps,
                imageWidth = bmpWidth,
                imageHeight = bmpHeight,
                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1).takeIf { it != -1 },
                exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME),
                fNumber = exif.getAttribute(ExifInterface.TAG_F_NUMBER)?.toFloatOrNull(),
                iso = exif.getAttributeInt(ExifInterface.TAG_ISO_SPEED_RATINGS, 0).takeIf { it > 0 },
                flash = exif.getAttributeInt(ExifInterface.TAG_FLASH, -1).takeIf { it != -1 }?.let { it != 0 },
                focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)?.let {
                    val parts = it.split("/")
                    if (parts.size == 2) {
                        parts[0].toFloatOrNull()?.div(parts[1].toFloatOrNull() ?: 1f)
                    } else {
                        it.toFloatOrNull()
                    }
                },
                software = software ?: rawMetadata["software"],
                artist = artist,
                copyright = copyright,
                userComment = userComment ?: rawMetadata["comment"]
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if image contains GPS location
     */
    suspend fun hasGpsLocation(file: File): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val exif = ExifInterface(file.absolutePath)
            val latLong = FloatArray(2)
            exif.getLatLong(latLong)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Extract GPS coordinates
     */
    suspend fun extractGps(file: File): GpsCoordinates? = withContext(Dispatchers.IO) {
        return@withContext try {
            val exif = ExifInterface(file.absolutePath)
            val latLong = FloatArray(2)
            if (exif.getLatLong(latLong)) {
                GpsCoordinates(
                    latitude = latLong[0].toDouble(),
                    longitude = latLong[1].toDouble(),
                    altitude = exif.getAltitude(0.0).takeIf { it != 0.0 }
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get bitmap dimensions without loading full image into memory.
     */
    private fun getBitmapDimensions(file: File): Pair<Int?, Int?> {
        return try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, opts)
            val w = opts.outWidth.takeIf { it > 0 }
            val h = opts.outHeight.takeIf { it > 0 }
            Pair(w, h)
        } catch (e: Exception) {
            Pair(null, null)
        }
    }

    /**
     * Try to extract metadata from raw file bytes.
     * WhatsApp strips standard EXIF tags but may leave other data segments
     * (XMP, ICC profiles, JFIF comments, PNG text chunks).
     */
    private fun extractRawMetadata(file: File): Map<String, String> {
        val metadata = mutableMapOf<String, String>()
        try {
            val bytes = FileInputStream(file).use { fis ->
                val header = ByteArray(minOf(file.length(), 65536L).toInt())
                fis.read(header)
                header
            }

            val headerStr = String(bytes, Charsets.ISO_8859_1)

            // Try to extract XMP data (XML metadata that WhatsApp sometimes leaves)
            val xmpStart = headerStr.indexOf("<x:xmpmeta")
            val xmpEnd = headerStr.indexOf("</x:xmpmeta>")
            if (xmpStart >= 0 && xmpEnd > xmpStart) {
                val xmpData = headerStr.substring(xmpStart, xmpEnd + 13)

                // Extract creator tool from XMP
                extractXmpValue(xmpData, "xmp:CreatorTool")?.let {
                    metadata["software"] = it
                }
                extractXmpValue(xmpData, "xmp:CreateDate")?.let {
                    metadata["dateTime"] = it
                }
                extractXmpValue(xmpData, "tiff:Make")?.let {
                    metadata["make"] = it
                }
                extractXmpValue(xmpData, "tiff:Model")?.let {
                    metadata["model"] = it
                }
            }

            // Check for JFIF comment markers (0xFF 0xFE) in JPEG
            if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()) {
                // It's a JPEG — scan for comment marker
                var i = 2
                while (i < bytes.size - 4) {
                    if (bytes[i] == 0xFF.toByte()) {
                        val marker = bytes[i + 1].toInt() and 0xFF
                        if (marker == 0xFE) {
                            // Comment marker
                            val len = ((bytes[i + 2].toInt() and 0xFF) shl 8) or (bytes[i + 3].toInt() and 0xFF)
                            if (i + 4 + len - 2 <= bytes.size) {
                                val comment = String(bytes, i + 4, len - 2, Charsets.UTF_8).trim()
                                if (comment.isNotEmpty()) {
                                    metadata["comment"] = comment
                                }
                            }
                            break
                        } else if (marker != 0x00 && marker != 0x01 && marker in 0xC0..0xFE) {
                            val len = ((bytes[i + 2].toInt() and 0xFF) shl 8) or (bytes[i + 3].toInt() and 0xFF)
                            i += 2 + len
                        } else {
                            i++
                        }
                    } else {
                        i++
                    }
                }
            }

            // Check for PNG text chunks (tEXt, iTXt)
            if (bytes.size >= 8 &&
                bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
                bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()) {
                // It's a PNG — check for text chunks
                val pngStr = String(bytes, Charsets.ISO_8859_1)
                if (pngStr.contains("Software")) {
                    val idx = pngStr.indexOf("Software")
                    if (idx >= 0 && idx + 20 < pngStr.length) {
                        // Null-separated key-value in tEXt chunk
                        val valueStart = pngStr.indexOf('\u0000', idx) + 1
                        if (valueStart > 0 && valueStart < pngStr.length) {
                            val valueEnd = minOf(pngStr.indexOf('\u0000', valueStart).takeIf { it > 0 } ?: pngStr.length, valueStart + 100)
                            val value = pngStr.substring(valueStart, valueEnd).trim()
                            if (value.isNotEmpty() && value.all { it.code in 32..126 }) {
                                metadata["software"] = value
                            }
                        }
                    }
                }
            }

        } catch (e: Exception) {
            // Silently ignore — raw extraction is best-effort
        }
        return metadata
    }

    /**
     * Extract a value from XMP XML data by tag name
     */
    private fun extractXmpValue(xmp: String, tag: String): String? {
        // Try attribute format: tag="value"
        val attrPattern = Regex("""$tag="([^"]+)"""")
        attrPattern.find(xmp)?.groupValues?.getOrNull(1)?.let { return it }

        // Try element format: <tag>value</tag>
        val elemPattern = Regex("""<$tag>([^<]+)</$tag>""")
        elemPattern.find(xmp)?.groupValues?.getOrNull(1)?.let { return it }

        return null
    }
}
