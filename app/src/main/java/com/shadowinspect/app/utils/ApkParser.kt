package com.shadowinspect.app.utils

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_PERMISSIONS
import android.content.pm.PermissionInfo
import android.net.Uri
import android.provider.OpenableColumns
import com.shadowinspect.app.domain.model.ApkInfo
import com.shadowinspect.app.domain.model.ApkPermission
import com.shadowinspect.app.domain.model.PermissionDangerousLevel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import javax.inject.Inject

/**
 * Utility for extracting metadata and analysis-relevant information from APK files.
 */
class ApkParser @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val dangerousPermissions = setOf(
        "android.permission.READ_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.SEND_SMS",
        "android.permission.READ_CONTACTS",
        "android.permission.WRITE_CONTACTS",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.CAMERA",
        "android.permission.RECORD_AUDIO",
        "android.permission.READ_CALL_LOG",
        "android.permission.WRITE_CALL_LOG",
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.ACCESS_MEDIA_LOCATION",
        "android.permission.READ_PHONE_STATE",
        "android.permission.CALL_PHONE",
        "android.permission.ADD_VOICEMAIL",
        "android.permission.USE_SIP",
        "android.permission.PROCESS_OUTGOING_CALLS",
        "android.permission.ANSWER_PHONE_CALLS",
        "android.permission.READ_PHONE_NUMBERS",
        "android.permission.BODY_SENSORS",
        "android.permission.ACTIVITY_RECOGNITION",
        "android.permission.ACCESS_BACKGROUND_LOCATION",
        "android.permission.BIND_ACCESSIBILITY_SERVICE",
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.WRITE_SETTINGS",
        "android.permission.REQUEST_INSTALL_PACKAGES",
        "android.permission.REQUEST_DELETE_PACKAGES"
    )

    /**
     * Parse basic APK info from a content Uri.
     * Returns null if parsing fails.
     */
    fun parseApk(uri: Uri): ApkInfo? {
        val pm = context.packageManager
        var temp: File? = null
        return try {
            // Copy content URI to a temp file so PackageManager can read it.
            temp = File.createTempFile("shadow_apk_", ".apk", context.cacheDir)
            context.contentResolver.openInputStream(uri).use { input ->
                FileOutputStream(temp).use { out ->
                    input?.copyTo(out)
                }
            }

            val flags = PackageManager.GET_PERMISSIONS or
                PackageManager.GET_ACTIVITIES or
                PackageManager.GET_SERVICES or
                PackageManager.GET_RECEIVERS or
                PackageManager.GET_PROVIDERS or
                PackageManager.GET_SIGNATURES or
                (if (android.os.Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else 0)

            val pkgInfo = pm.getPackageArchiveInfo(temp.absolutePath, flags)
                ?: return null

            // On some devices you need to set sourceDir/publicSourceDir to read label/icon
            pkgInfo.applicationInfo?.sourceDir = temp.absolutePath
            pkgInfo.applicationInfo?.publicSourceDir = temp.absolutePath

            val packageName = pkgInfo.packageName
            val versionName = pkgInfo.versionName
            val versionCode = try {
                if (android.os.Build.VERSION.SDK_INT >= 28) pkgInfo.longVersionCode.toInt() else pkgInfo.versionCode
            } catch (_: Exception) { null }

            val minSdk = try {
                // ApplicationInfo.minSdkVersion exists on newer APIs; may throw on older
                val ai = pkgInfo.applicationInfo
                if (ai != null) {
                    val field = ai::class.java.getDeclaredField("minSdkVersion")
                    field.isAccessible = true
                    (field.getInt(ai))
                } else null
            } catch (_: Throwable) {
                null
            }

            val targetSdk = try {
                pkgInfo.applicationInfo?.targetSdkVersion
            } catch (_: Exception) { null }

            val fileName = queryDisplayName(uri) ?: temp.name
            val fileSize = getFileSize(uri)

            val ai = pkgInfo.applicationInfo
            val isDebuggable = ai != null && (ai.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
            val isAllowBackup = ai != null && (ai.flags and android.content.pm.ApplicationInfo.FLAG_ALLOW_BACKUP) != 0

            val activitiesCount = pkgInfo.activities?.size ?: 0
            val servicesCount = pkgInfo.services?.size ?: 0
            val receiversCount = pkgInfo.receivers?.size ?: 0
            val providersCount = pkgInfo.providers?.size ?: 0

            var certificateInfo: String? = null
            try {
                val signingInfo = pkgInfo.signingInfo
                val signatures = pkgInfo.signatures
                
                if (android.os.Build.VERSION.SDK_INT >= 28 && signingInfo != null) {
                    val sigs = signingInfo.apkContentsSigners
                    if (!sigs.isNullOrEmpty()) {
                        val cert = java.security.cert.CertificateFactory.getInstance("X.509")
                            .generateCertificate(java.io.ByteArrayInputStream(sigs[0].toByteArray())) as? java.security.cert.X509Certificate
                        certificateInfo = cert?.subjectX500Principal?.name
                    }
                } else if (signatures != null && signatures.isNotEmpty()) {
                    val cert = java.security.cert.CertificateFactory.getInstance("X.509")
                        .generateCertificate(java.io.ByteArrayInputStream(signatures[0].toByteArray())) as? java.security.cert.X509Certificate
                    certificateInfo = cert?.subjectX500Principal?.name
                }
            } catch (e: Exception) {
                // Ignore certificate parsing errors
            }

            ApkInfo(
                fileName = fileName,
                fileSize = fileSize,
                packageName = packageName,
                versionName = versionName,
                versionCode = versionCode,
                minSdkVersion = minSdk,
                targetSdkVersion = targetSdk,
                isDebuggable = isDebuggable,
                isAllowBackup = isAllowBackup,
                activitiesCount = activitiesCount,
                servicesCount = servicesCount,
                receiversCount = receiversCount,
                providersCount = providersCount,
                certificateInfo = certificateInfo,
                requestedPermissions = pkgInfo.requestedPermissions?.toList() ?: emptyList()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try { temp?.delete() } catch (_: Exception) {}
        }
    }

    /**
     * Extract declared permissions for an installed package.
     * If the package is not installed or cannot be read, returns empty list.
     */
    /**
     * Convert a list of permission names to ApkPermission objects.
     */
    fun getApkPermissions(names: List<String>): List<ApkPermission> {
        val pm = context.packageManager
        return names.mapNotNull { name ->
            try {
                val info = pm.getPermissionInfo(name, 0)
                val base = info.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE
                val isDangerous = (base == PermissionInfo.PROTECTION_DANGEROUS) || dangerousPermissions.contains(name)
                ApkPermission(
                    name = name,
                    isDangerous = isDangerous,
                    description = info.loadDescription(pm)?.toString(),
                    protectionLevel = protectionLevelToString(base)
                )
            } catch (e: Exception) {
                ApkPermission(name = name, isDangerous = dangerousPermissions.contains(name), description = null, protectionLevel = null)
            }
        }
    }

    private fun protectionLevelToString(level: Int): String = when (level) {
        PermissionInfo.PROTECTION_DANGEROUS -> PermissionDangerousLevel.DANGEROUS.name
        PermissionInfo.PROTECTION_SIGNATURE -> PermissionDangerousLevel.SIGNATURE.name
        PermissionInfo.PROTECTION_SIGNATURE_OR_SYSTEM -> PermissionDangerousLevel.SYSTEM.name
        else -> PermissionDangerousLevel.NORMAL.name
    }

    /**
     * Compute SHA-256 of the content at given Uri. Returns hex string or null on error.
     */
    fun calculateSha256(uri: Uri): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            context.contentResolver.openInputStream(uri).use { input ->
                if (input != null) updateDigest(digest, input) else return null
            }
            digestToHex(digest.digest())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Compute MD5 of the content at given Uri. Returns hex string or null on error.
     */
    fun calculateMd5(uri: Uri): String? {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            context.contentResolver.openInputStream(uri).use { input ->
                if (input != null) updateDigest(digest, input) else return null
            }
            digestToHex(digest.digest())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun updateDigest(digest: MessageDigest, input: InputStream) {
        val buffer = ByteArray(8 * 1024)
        var read: Int
        while (true) {
            read = input.read(buffer)
            if (read <= 0) break
            digest.update(buffer, 0, read)
        }
    }

    private fun digestToHex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) sb.append(String.format("%02x", b))
        return sb.toString()
    }

    /**
     * Best-effort file size retrieval for content Uris.
     */
    fun getFileSize(uri: Uri): Long {
        // Try query first
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (idx >= 0 && cursor.moveToFirst()) {
                        val size = cursor.getLong(idx)
                        if (size >= 0) return size
                    }
                }
        } catch (_: Exception) {}

        // Try AssetFileDescriptor
        try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                if (afd.length >= 0) return afd.length
            }
        } catch (_: Exception) {}

        // Fallback: stream count
        var total = 0L
        try {
            context.contentResolver.openInputStream(uri).use { input ->
                if (input != null) {
                    val buffer = ByteArray(8 * 1024)
                    var read: Int
                    while (true) {
                        read = input.read(buffer)
                        if (read <= 0) break
                        total += read
                    }
                }
            }
        } catch (_: Exception) {}

        return total
    }

    private fun queryDisplayName(uri: Uri): String? {
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0 && cursor.moveToFirst()) return cursor.getString(idx)
                }
        } catch (_: Exception) {}
        return null
    }
}
