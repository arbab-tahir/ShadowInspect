package com.shadowinspect.app.presentation.screens.education

import androidx.lifecycle.ViewModel
import com.shadowinspect.app.domain.education.MitreExplained
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class MitreExplorerUiState(
    val allTechniques: List<MitreExplained> = emptyList(),
    val filteredTechniques: List<MitreExplained> = emptyList(),
    val searchQuery: String = ""
)

@HiltViewModel
class MitreExplorerViewModel @Inject constructor() : ViewModel() {

    private val techniques = listOf(
        MitreExplained(
            techniqueId = "T1429",
            techniqueName = "Capture Audio via Microphone",
            simpleExplanation = "An app secretly records your conversations through the device microphone without your knowledge.",
            realWorldExample = "A spy app activates your mic when you receive a phone call, recording both sides of the conversation.",
            howToProtect = "Review microphone permissions. Revoke for apps that don't need it. Watch for mic-access indicators.",
            severityLevel = "HIGH"
        ),
        MitreExplained(
            techniqueId = "T1428",
            techniqueName = "Capture Camera via Exploit",
            simpleExplanation = "Malware silently takes photos or records video using your device camera, even when the camera app is closed.",
            realWorldExample = "Stalkerware activates the front camera to take periodic selfies to confirm your identity and location.",
            howToProtect = "Check camera permissions. Use a physical camera cover. Look for the camera-in-use indicator light.",
            severityLevel = "HIGH"
        ),
        MitreExplained(
            techniqueId = "T1430",
            techniqueName = "Location Tracking",
            simpleExplanation = "An app constantly monitors and reports your GPS location, creating a detailed log of your movements.",
            realWorldExample = "A game app with location permission sends your coordinates every 5 minutes to a tracking server.",
            howToProtect = "Set location permissions to 'Only while using'. Avoid granting 'Always' permission unless essential.",
            severityLevel = "HIGH"
        ),
        MitreExplained(
            techniqueId = "T1517",
            techniqueName = "Access Notifications",
            simpleExplanation = "An app reads all your notifications, including banking 2FA codes, messages, and alerts from other apps.",
            realWorldExample = "A banking trojan reads the 2FA code from your bank's notification and sends it to the attacker.",
            howToProtect = "Only grant notification access to apps that truly need it (like Wear OS companion apps).",
            severityLevel = "HIGH"
        ),
        MitreExplained(
            techniqueId = "T1529",
            techniqueName = "Capture SMS Messages",
            simpleExplanation = "Malware intercepts and forwards your SMS messages, including one-time passwords sent by your bank.",
            realWorldExample = "Banking trojan intercepts 'Your OTP is 123456' from your bank and sends it to attackers.",
            howToProtect = "Only grant SMS permissions to messaging apps. Use authenticator apps instead of SMS for 2FA.",
            severityLevel = "HIGH"
        ),
        MitreExplained(
            techniqueId = "T1476",
            techniqueName = "Deliver Malicious App via Authorized App Store",
            simpleExplanation = "Attackers sneak malware into official app stores by hiding malicious code in seemingly legitimate apps.",
            realWorldExample = "A fake flashlight app on Play Store secretly mines cryptocurrency using your phone's CPU.",
            howToProtect = "Check developer name, reviews, permissions, and download count before installing any app.",
            severityLevel = "MEDIUM"
        ),
        MitreExplained(
            techniqueId = "T1444",
            techniqueName = "Masquerade as Legitimate Application",
            simpleExplanation = "Malware looks exactly like a real app (same icon, name, screenshots) to trick you into installing it.",
            realWorldExample = "Fake 'WhatsApp Update' app on a third-party site installs a banking trojan instead.",
            howToProtect = "Only download apps from official app stores. Verify the developer name matches the real publisher.",
            severityLevel = "HIGH"
        ),
        MitreExplained(
            techniqueId = "T1418",
            techniqueName = "Application Discovery",
            simpleExplanation = "Malware scans the list of installed apps on your device to find banking, cryptocurrency, or other high-value targets.",
            realWorldExample = "After installation, a trojan checks if PayPal, Coinbase, or your bank app is installed, then targets those specifically.",
            howToProtect = "Use security apps to monitor suspicious behavior. Scan APKs before installing.",
            severityLevel = "MEDIUM"
        ),
        MitreExplained(
            techniqueId = "T1406",
            techniqueName = "Obfuscated Files or Information",
            simpleExplanation = "Malware hides its true purpose by scrambling its code so security tools can't easily detect what it does.",
            realWorldExample = "A malicious app encrypts its payload so antivirus scanners see random data instead of recognizable malware code.",
            howToProtect = "ShadowInspect's APK scanner checks for obfuscation patterns. Highly obfuscated apps are suspicious.",
            severityLevel = "MEDIUM"
        ),
        MitreExplained(
            techniqueId = "T1439",
            techniqueName = "Eavesdrop on Insecure Network Communication",
            simpleExplanation = "On public Wi-Fi, attackers can intercept data you send/receive if the connection isn't encrypted.",
            realWorldExample = "At a coffee shop, an attacker on the same Wi-Fi reads your unencrypted login credentials.",
            howToProtect = "Always use HTTPS. Use a VPN on public networks. Avoid sensitive tasks on public Wi-Fi.",
            severityLevel = "MEDIUM"
        )
    )

    private val _uiState = MutableStateFlow(MitreExplorerUiState(allTechniques = techniques, filteredTechniques = techniques))
    val uiState: StateFlow<MitreExplorerUiState> = _uiState.asStateFlow()

    fun search(query: String) {
        val filtered = if (query.isBlank()) {
            techniques
        } else {
            techniques.filter {
                it.techniqueId.contains(query, ignoreCase = true) ||
                        it.techniqueName.contains(query, ignoreCase = true) ||
                        it.simpleExplanation.contains(query, ignoreCase = true)
            }
        }
        _uiState.value = _uiState.value.copy(searchQuery = query, filteredTechniques = filtered)
    }
}
