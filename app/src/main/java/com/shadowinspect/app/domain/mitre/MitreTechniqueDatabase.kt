package com.shadowinspect.app.domain.mitre

/**
* Built-in MITRE ATT&CK techniques for mobile
* This is a subset of the full MITRE ATT&CK for Mobile framework
* Full dataset can be downloaded from: https://attack.mitre.org/
*/
object MitreTechniqueDatabase {
    val techniques = listOf(
        // Collection Tactics
        MitreTechnique(
            id = "T1517",
            name = "Access Notifications",
            description = "Adversaries may collect notifications sent by the device to gather sensitive information or track user activity.",
            tactics = listOf("Collection"),
            platforms = listOf("Android"),
            permissionsRequired = listOf("android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"),
            url = "https://attack.mitre.org/techniques/T1517/",
            detection = "Monitor for apps requesting notification listener permission",
            mitigation = "Restrict notification listener permission to trusted apps only"
        ),
        MitreTechnique(
            id = "T1529",
            name = "Capture SMS Messages",
            description = "Adversaries may capture SMS messages sent to the device to intercept multi-factor authentication codes or gather sensitive information.",
            tactics = listOf("Collection"),
            platforms = listOf("Android"),
            permissionsRequired = listOf("android.permission.READ_SMS", "android.permission.RECEIVE_SMS"),
            url = "https://attack.mitre.org/techniques/T1529/",
            detection = "Monitor for apps requesting SMS permissions",
            mitigation = "Use app permissions to restrict SMS access"
        ),
        MitreTechnique(
            id = "T1530",
            name = "Data from Local System",
            description = "Adversaries may collect data from the local device storage, including files, contacts, and other sensitive information.",
            tactics = listOf("Collection"),
            platforms = listOf("Android", "iOS"),
            permissionsRequired = listOf("android.permission.READ_EXTERNAL_STORAGE", "android.permission.READ_CONTACTS"),
            url = "https://attack.mitre.org/techniques/T1530/",
            detection = "Monitor for apps accessing storage or contacts",
            mitigation = "Implement proper permission controls"
        ),
        // Credential Access
        MitreTechnique(
            id = "T1417",
            name = "Input Capture",
            description = "Adversaries may capture user input, including passwords and other sensitive information, through keylogging or UI overlays.",
            tactics = listOf("Credential Access"),
            platforms = listOf("Android"),
            permissionsRequired = listOf("android.permission.SYSTEM_ALERT_WINDOW"),
            url = "https://attack.mitre.org/techniques/T1417/",
            detection = "Monitor for apps creating overlay windows",
            mitigation = "Restrict overlay permission"
        ),
        // Defense Evasion
        MitreTechnique(
            id = "T1406",
            name = "Obfuscated Files or Information",
            description = "Adversaries may obfuscate malicious code to evade detection by security tools.",
            tactics = listOf("Defense Evasion"),
            platforms = listOf("Android"),
            permissionsRequired = listOf(),
            url = "https://attack.mitre.org/techniques/T1406/",
            detection = "Analyze code for obfuscation techniques",
            mitigation = "Use code analysis tools"
        ),
        // Discovery
        MitreTechnique(
            id = "T1518",
            name = "Determine Device Type",
            description = "Adversaries may gather information about the device, including model, OS version, and installed apps.",
            tactics = listOf("Discovery"),
            platforms = listOf("Android"),
            permissionsRequired = listOf("android.permission.READ_PHONE_STATE"),
            url = "https://attack.mitre.org/techniques/T1518/",
            detection = "Monitor for apps querying device information",
            mitigation = "Restrict READ_PHONE_STATE permission"
        ),
        // Persistence
        MitreTechnique(
            id = "T1525",
            name = "Implant Container Image",
            description = "Adversaries may implant malicious code into app containers to maintain persistence.",
            tactics = listOf("Persistence"),
            platforms = listOf("Android"),
            permissionsRequired = listOf("android.permission.INSTALL_PACKAGES"),
            url = "https://attack.mitre.org/techniques/T1525/",
            detection = "Monitor for unauthorized app installations",
            mitigation = "Disable installation from unknown sources"
        ),
        // Impact
        MitreTechnique(
            id = "T1532",
            name = "Data Encrypted for Impact",
            description = "Adversaries may encrypt data on the device to render it inaccessible, often as part of ransomware.",
            tactics = listOf("Impact"),
            platforms = listOf("Android"),
            permissionsRequired = listOf("android.permission.WRITE_EXTERNAL_STORAGE"),
            url = "https://attack.mitre.org/techniques/T1532/",
            detection = "Monitor for mass file encryption",
            mitigation = "Regular backups and file monitoring"
        ),
        // Location Tracking
        MitreTechnique(
            id = "T1430",
            name = "Location Tracking",
            description = "Adversaries may track the device's physical location to monitor user movements.",
            tactics = listOf("Collection"),
            platforms = listOf("Android"),
            permissionsRequired = listOf("android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION"),
            url = "https://attack.mitre.org/techniques/T1430/",
            detection = "Monitor for location permission requests",
            mitigation = "Grant location only when necessary"
        ),
        // Audio Capture
        MitreTechnique(
            id = "T1429",
            name = "Capture Audio",
            description = "Adversaries may capture audio through the device microphone to spy on conversations.",
            tactics = listOf("Collection"),
            platforms = listOf("Android"),
            permissionsRequired = listOf("android.permission.RECORD_AUDIO"),
            url = "https://attack.mitre.org/techniques/T1429/",
            detection = "Monitor for microphone access",
            mitigation = "Physical camera/microphone covers"
        ),
        // Video Capture
        MitreTechnique(
            id = "T1428",
            name = "Capture Video",
            description = "Adversaries may capture video through the device camera to spy on user activities.",
            tactics = listOf("Collection"),
            platforms = listOf("Android"),
            permissionsRequired = listOf("android.permission.CAMERA"),
            url = "https://attack.mitre.org/techniques/T1428/",
            detection = "Monitor for camera access",
            mitigation = "Physical camera covers"
        )
    )

    // Lookup by ID
    fun getTechniqueById(id: String): MitreTechnique? {
        return techniques.find { it.id == id }
    }

    // Get techniques by tactic
    fun getTechniquesByTactic(tactic: String): List<MitreTechnique> {
        return techniques.filter { it.tactics.contains(tactic) }
    }

    // Get techniques that use a specific permission
    fun getTechniquesByPermission(permission: String): List<MitreTechnique> {
        return techniques.filter { it.permissionsRequired.contains(permission) }
    }

    // Get all unique tactics
    fun getAllTactics(): List<String> {
        return techniques.flatMap { it.tactics }.distinct()
    }
}
