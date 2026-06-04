package com.shadowinspect.app.domain.ml

import android.content.Context
import com.shadowinspect.app.domain.model.ApkPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.jar.JarFile
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeatureExtractor @Inject constructor(
    private val context: Context
) {
    
    // Predefined permission list (50+ permissions from Droidware dataset)
    private val permissionList = listOf(
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
        "android.permission.INTERNET",
        "android.permission.ACCESS_NETWORK_STATE",
        "android.permission.ACCESS_WIFI_STATE",
        "android.permission.CHANGE_WIFI_STATE",
        "android.permission.BLUETOOTH",
        "android.permission.BLUETOOTH_ADMIN",
        "android.permission.VIBRATE",
        "android.permission.WAKE_LOCK",
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.BIND_ACCESSIBILITY_SERVICE",
        "android.permission.REQUEST_INSTALL_PACKAGES",
        "android.permission.REQUEST_DELETE_PACKAGES",
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
        "android.permission.ACCESS_MEDIA_LOCATION",
        "android.permission.INSTALL_PACKAGES",
        "android.permission.DELETE_PACKAGES",
        "android.permission.CLEAR_APP_CACHE",
        "android.permission.READ_LOGS",
        "android.permission.SET_DEBUG_APP",
        "android.permission.RESTART_PACKAGES",
        "android.permission.KILL_BACKGROUND_PROCESSES",
        "android.permission.GET_TASKS",
        "android.permission.REAL_GET_TASKS",
        "android.permission.CHANGE_CONFIGURATION",
        "android.permission.WRITE_SETTINGS",
        "android.permission.WRITE_SECURE_SETTINGS",
        "android.permission.MODIFY_AUDIO_SETTINGS",
        "android.permission.MOUNT_UNMOUNT_FILESYSTEMS"
    )
    
    // Suspicious API calls (from Droidware)
    private val suspiciousApis = listOf(
        "Runtime.exec",
        "ProcessBuilder",
        "System.load",
        "DexClassLoader",
        "Cipher",
        "SecretKeySpec",
        "HttpURLConnection",
        "URL.openConnection",
        "SmsManager.sendTextMessage",
        "TelephonyManager.getDeviceId",
        "LocationManager.getLastKnownLocation",
        "Camera.open",
        "AudioRecord.startRecording",
        "ContentResolver.query",
        "PackageManager.queryIntentActivities",
        "PackageManager.getInstalledApplications"
    )
    
    // OpCode patterns (simplified)
    private val opCodeList = listOf(
        "nop", "move", "return", "const", "monitor", "check-cast",
        "instance-of", "array-length", "new-instance", "throw",
        "goto", "switch", "cmp", "if", "aget", "aput", "iget",
        "iput", "sget", "sput", "invoke", "execute"
    )
    
    /**
     * Extract feature vector from APK file
     */
    suspend fun extractFeatures(
        apkFile: File,
        permissions: List<ApkPermission>
    ): ApkFeatureVector = withContext(Dispatchers.IO) {
        
        // 1. Permission features (one-hot encoded)
        val permissionFeatures = FloatArray(permissionList.size) { index ->
            if (permissions.any { it.name == permissionList[index] }) 1f else 0f
        }
        
        // 2. API call features
        val apiFeatures = extractApiCalls(apkFile)
        
        // 3. OpCode features
        val opCodeFeatures = extractOpCodes(apkFile)
        
        // 4. Other features
        val suspiciousStrings = countSuspiciousStrings(apkFile)
        val obfuscationScore = calculateObfuscationScore(apkFile)
        val urlCount = extractUrls(apkFile).size.toFloat()
        val dangerousPermissions = permissions.count { isDangerousPermission(it.name) }.toFloat()
        
        ApkFeatureVector(
            permissions = permissionFeatures,
            apiCalls = apiFeatures,
            opcodes = opCodeFeatures,
            suspiciousStrings = suspiciousStrings,
            obfuscationScore = obfuscationScore,
            urlCount = urlCount,
            dangerousPermissions = dangerousPermissions
        )
    }
    
    /**
     * Extract API call frequencies from APK
     */
    private fun extractApiCalls(file: File): FloatArray {
        val apiCounts = FloatArray(suspiciousApis.size) { 0f }
        
        try {
            ZipFile(file).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.name.endsWith(".dex") || entry.name.endsWith(".class")) {
                        // In production, use dex2jar or similar to analyze bytecode
                        // For now, return dummy data for testing
                        return FloatArray(suspiciousApis.size) { 
                            (0..5).random().toFloat() / 10f 
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return apiCounts
    }
    
    /**
     * Extract opCode distributions from APK
     */
    private fun extractOpCodes(file: File): FloatArray {
        val opCodeCounts = FloatArray(opCodeList.size) { 0f }
        
        try {
            // In production, use smali/baksmali to extract opcodes
            // For now, return dummy data
            return FloatArray(opCodeList.size) { 
                (0..100).random().toFloat() / 100f 
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return opCodeCounts
    }
    
    /**
     * Count suspicious strings in APK
     */
    private fun countSuspiciousStrings(file: File): Float {
        val suspiciousPatterns = listOf(
            "password", "credit", "card", "bank", "login", "account",
            "crack", "hack", "exploit", "root", "su", "superuser",
            "bypass", "steal", "phish", "trojan", "virus", "malware",
            "crypt", "ransom", "keylog", "spy", "track", "monitor"
        )
        
        var count = 0f
        try {
            ZipFile(file).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.name.endsWith(".dex") || entry.name.endsWith(".xml")) {
                        // In production, actually read the content
                        // For now, return random count
                        count = suspiciousPatterns.size.toFloat() / 2f
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return count
    }
    
    /**
     * Calculate obfuscation score (0-1)
     */
    private fun calculateObfuscationScore(file: File): Float {
        var score = 0f
        
        try {
            ZipFile(file).use { zip ->
                val entries = zip.entries()
                var suspiciousEntries = 0
                var totalEntries = 0
                
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    totalEntries++
                    
                    // Check for obfuscation indicators
                    if (entry.name.contains(Regex("[a-zA-Z0-9]{30,}")) || // Long random names
                        entry.name.contains("\\d+".toRegex()) || // Numeric class names
                        entry.name.contains("classes\\d+\\.dex".toRegex()) // Split dex
                    ) {
                        suspiciousEntries++
                    }
                }
                
                score = if (totalEntries > 0) suspiciousEntries.toFloat() / totalEntries else 0f
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return score.coerceIn(0f, 1f)
    }
    
    /**
     * Extract URLs from APK
     */
    private fun extractUrls(file: File): List<String> {
        val urls = mutableListOf<String>()
        val urlPattern = Regex("https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")
        
        try {
            ZipFile(file).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.name.endsWith(".xml") || entry.name.endsWith(".dex")) {
                        // In production, read content and extract URLs
                        // For now, add some test URLs
                        if (Math.random() > 0.5) {
                            urls.add("http://example.com/malware")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return urls
    }
    
    /**
     * Check if permission is dangerous
     */
    private fun isDangerousPermission(permission: String): Boolean {
        val dangerous = listOf(
            "READ_SMS", "RECEIVE_SMS", "SEND_SMS",
            "CAMERA", "RECORD_AUDIO",
            "ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION",
            "READ_CONTACTS", "READ_CALL_LOG",
            "READ_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE",
            "BIND_ACCESSIBILITY_SERVICE", "SYSTEM_ALERT_WINDOW"
        )
        
        return dangerous.any { permission.contains(it, ignoreCase = true) }
    }
    
    /**
     * Get feature importance names for explainability
     */
    fun getFeatureNames(): Map<String, String> {
        val names = mutableMapOf<String, String>()
        
        permissionList.forEachIndexed { index, perm ->
            names["perm_$index"] = perm.substringAfterLast('.')
        }
        
        suspiciousApis.forEachIndexed { index, api ->
            names["api_$index"] = api
        }
        
        opCodeList.forEachIndexed { index, op ->
            names["op_$index"] = op
        }
        
        names["suspicious_strings"] = "Suspicious Strings"
        names["obfuscation_score"] = "Obfuscation Level"
        names["url_count"] = "URL Count"
        names["dangerous_perms"] = "Dangerous Permissions"
        
        return names
    }
}
