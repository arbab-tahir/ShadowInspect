package com.shadowinspect.app.domain.mitre

/**
* Predefined mappings from Android permissions to MITRE techniques
*/
object MitrePermissionMapping {
    val mappings = listOf(
        // SMS and Messaging
        PermissionMapping(
            permission = "android.permission.READ_SMS",
            techniqueId = "T1529",
            description = "Read SMS messages - used to intercept 2FA codes",
            weight = 0.9
        ),
        PermissionMapping(
            permission = "android.permission.RECEIVE_SMS",
            techniqueId = "T1529",
            description = "Receive SMS messages - intercepts incoming messages",
            weight = 0.9
        ),
        PermissionMapping(
            permission = "android.permission.SEND_SMS",
            techniqueId = "T1518",
            description = "Send SMS - can send premium rate messages",
            weight = 0.8
        ),
        // Location
        PermissionMapping(
            permission = "android.permission.ACCESS_FINE_LOCATION",
            techniqueId = "T1430",
            description = "Precise location tracking",
            weight = 0.9
        ),
        PermissionMapping(
            permission = "android.permission.ACCESS_COARSE_LOCATION",
            techniqueId = "T1430",
            description = "Approximate location tracking",
            weight = 0.6
        ),
        PermissionMapping(
            permission = "android.permission.ACCESS_BACKGROUND_LOCATION",
            techniqueId = "T1430",
            description = "Background location tracking - persistent surveillance",
            weight = 1.0
        ),
        // Camera and Microphone
        PermissionMapping(
            permission = "android.permission.CAMERA",
            techniqueId = "T1428",
            description = "Camera access - can capture photos/videos",
            weight = 0.9
        ),
        PermissionMapping(
            permission = "android.permission.RECORD_AUDIO",
            techniqueId = "T1429",
            description = "Microphone access - can record conversations",
            weight = 0.9
        ),
        // Contacts and Call Log
        PermissionMapping(
            permission = "android.permission.READ_CONTACTS",
            techniqueId = "T1530",
            description = "Read contacts - harvests personal connections",
            weight = 0.7
        ),
        PermissionMapping(
            permission = "android.permission.READ_CALL_LOG",
            techniqueId = "T1530",
            description = "Read call log - understands communication patterns",
            weight = 0.7
        ),
        PermissionMapping(
            permission = "android.permission.READ_PHONE_STATE",
            techniqueId = "T1518",
            description = "Read phone state - device information gathering",
            weight = 0.6
        ),
        // Storage
        PermissionMapping(
            permission = "android.permission.READ_EXTERNAL_STORAGE",
            techniqueId = "T1530",
            description = "Read external storage - steals files",
            weight = 0.8
        ),
        PermissionMapping(
            permission = "android.permission.WRITE_EXTERNAL_STORAGE",
            techniqueId = "T1532",
            description = "Write external storage - can encrypt or modify files",
            weight = 0.8
        ),
        // Accessibility (High risk)
        PermissionMapping(
            permission = "android.permission.BIND_ACCESSIBILITY_SERVICE",
            techniqueId = "T1406",
            description = "Accessibility service - can control device, read screen",
            weight = 1.0
        ),
        // System
        PermissionMapping(
            permission = "android.permission.SYSTEM_ALERT_WINDOW",
            techniqueId = "T1518",
            description = "Draw overlays - can create fake login screens",
            weight = 0.9
        ),
        PermissionMapping(
            permission = "android.permission.REQUEST_INSTALL_PACKAGES",
            techniqueId = "T1525",
            description = "Install unknown apps - can install malware",
            weight = 0.9
        ),
        PermissionMapping(
            permission = "android.permission.REQUEST_DELETE_PACKAGES",
            techniqueId = "T1529",
            description = "Delete packages - can remove security apps",
            weight = 0.8
        ),
        PermissionMapping(
            permission = "android.permission.WRITE_SETTINGS",
            techniqueId = "T1406",
            description = "Modify system settings - can disable security features or enable malicious services",
            weight = 0.9
        ),
        PermissionMapping(
            permission = "android.permission.QUERY_ALL_PACKAGES",
            techniqueId = "T1418",
            description = "Query all packages - software discovery to find targets or security apps",
            weight = 0.7
        ),
        PermissionMapping(
            permission = "android.permission.REQUEST_COMPANION_PROFILE_WATCH",
            techniqueId = "T1518",
            description = "Companion device profile - can persist across device reboots and monitor state",
            weight = 0.6
        )
    )

    // Group mappings by technique for easy lookup
    val mappingsByTechnique: Map<String, List<PermissionMapping>> by lazy {
        mappings.groupBy { it.techniqueId }
    }

    // Group mappings by permission for easy lookup
    val mappingsByPermission: Map<String, List<PermissionMapping>> by lazy {
        mappings.groupBy { it.permission }
    }

    // Get all techniques that a permission maps to
    fun getTechniquesForPermission(permission: String): List<PermissionMapping> {
        return mappingsByPermission[permission] ?: emptyList()
    }

    // Get all permissions that contribute to a technique
    fun getPermissionsForTechnique(techniqueId: String): List<PermissionMapping> {
        return mappingsByTechnique[techniqueId] ?: emptyList()
    }
}
