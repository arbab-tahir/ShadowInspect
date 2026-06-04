package com.shadowinspect.app.data.seed

import com.shadowinspect.app.domain.education.LearningLevel
import com.shadowinspect.app.domain.education.LessonCategory
import com.shadowinspect.app.domain.education.LessonDifficulty
import com.shadowinspect.app.domain.education.SecurityLesson

object BeginnerLessons {
    val data = listOf(
        SecurityLesson(
            id = 1,
            title = "What is Malware?",
            description = "Learn about different types of malicious software and how they infect your device",
            category = LessonCategory.MALWARE_TYPES,
            difficulty = LessonDifficulty.BEGINNER,
            level = LearningLevel.BEGINNER,
            content = """
# What is Malware?

Malware (malicious software) is any program designed to harm your device or steal your data.

## Common Types:

### Viruses
Self-replicating programs that spread by inserting copies of themselves into other programs.

### Spyware
Secretly monitors your activity, steals passwords, and tracks your location.

### Ransomware
Encrypts your files and demands payment to unlock them.

### Banking Trojans
Specifically target banking apps to steal login credentials and 2FA codes.

### Adware
Shows unwanted ads and can redirect your browser to malicious sites.

## How Malware Infects Your Device:
- Fake apps from unknown sources
- Phishing links in SMS/email
- Infected attachments
- Drive-by downloads from compromised websites

## Prevention Tips:
- Only install apps from Play Store
- Keep your device updated
- Use ShadowInspect to scan suspicious files
- Don't click links in messages from strangers
            """.trimIndent(),
            summaryPoints = listOf("Malware is software designed to harm or steal", "Common types: viruses, spyware, ransomware, banking trojans", "Always verify sources before installing apps"),
            iconRes = "🦠", estimatedMinutes = 5, xpReward = 50, orderIndex = 1,
            mitreTechniques = listOf("T1476 - Deliver Malicious App via Authorized App Store")
        ),
        SecurityLesson(
            id = 2,
            title = "Spotting Phishing Attacks",
            description = "Learn how to identify and avoid phishing attempts in email, SMS, and calls",
            category = LessonCategory.PHISHING,
            difficulty = LessonDifficulty.BEGINNER,
            level = LearningLevel.BEGINNER,
            content = """
# Phishing: Don't Take the Bait!

Phishing is when scammers pretend to be legitimate companies to steal your information.

## Common Phishing Channels:

### Email Phishing
Fake emails claiming to be from banks, PayPal, Amazon, etc.

### SMS Phishing (Smishing)
Text messages with urgent requests and malicious links.

### Vishing (Voice Phishing)
Fake calls from "tech support" or "bank security".

## Red Flags to Watch For:

- URGENCY: "Your account will be closed in 24 hours!"
- Generic greetings: "Dear Customer" instead of your name
- Suspicious links: Always check before clicking
- Poor grammar: Spelling mistakes, awkward phrasing
- Password requests: Legitimate companies never ask

## Safe Practices:
- Type URLs directly, don't click links
- Call the company using official numbers
- Use ShadowInspect's URL scanner on suspicious links
            """.trimIndent(),
            summaryPoints = listOf("Phishing tries to trick you into revealing information", "Check URLs carefully, look for misspellings", "Legitimate companies never ask for passwords"),
            iconRes = "🎣", estimatedMinutes = 6, xpReward = 60, orderIndex = 2,
            mitreTechniques = listOf("T1444 - Masquerade as Legitimate Application")
        ),
        SecurityLesson(
            id = 3,
            title = "App Permissions Explained",
            description = "Understand what app permissions mean and how to protect your privacy",
            category = LessonCategory.PERMISSIONS,
            difficulty = LessonDifficulty.BEGINNER,
            level = LearningLevel.BEGINNER,
            content = """
# App Permissions: What They Really Mean

When an app asks for permission, it's asking to access sensitive parts of your phone.

## Dangerous Permissions:

### SMS (Read/Receive/Send)
Allows reading your text messages, including 2FA codes.
Risk: Banking trojans use this to steal verification codes.

### Location (Fine/Coarse)
Tracks where you are at all times.
Risk: Spyware can monitor your movements.

### Camera
Takes photos/videos anytime (even in background).
Risk: Malware can spy on you through your camera.

### Microphone
Records audio through your mic.
Risk: Can record conversations without your knowledge.

### Accessibility Service
FULL CONTROL — reads screen content, performs actions, grants permissions.
Risk: Banking trojans use this to steal passwords.

## How to Review Permissions:

Android:
1. Settings → Apps
2. Select the app
3. Tap "Permissions"
4. Review and revoke unnecessary access

## Remember:
- If an app asks for permissions it doesn't need, be suspicious
- A calculator app doesn't need your contacts!
            """.trimIndent(),
            summaryPoints = listOf("Permissions are requests to access sensitive data", "Some permissions are more dangerous than others", "Regularly review and revoke unnecessary permissions"),
            iconRes = "🔐", estimatedMinutes = 7, xpReward = 70, orderIndex = 3,
            mitreTechniques = listOf("T1418 - Application Discovery", "T1517 - Access Notifications")
        ),
        SecurityLesson(
            id = 4,
            title = "MITRE ATT&CK for Beginners",
            description = "Learn how security experts classify attacks using the MITRE framework",
            category = LessonCategory.MITRE_101,
            difficulty = LessonDifficulty.BEGINNER,
            level = LearningLevel.BEGINNER,
            content = """
# MITRE ATT&CK: The Hacker's Playbook

MITRE ATT&CK is like a dictionary of all known hacker techniques. Security professionals use it to talk about threats in a common language.

## Structure:

### Tactics (The "Why")
What the attacker is trying to achieve:
- Initial Access — Getting into your device
- Execution — Running malicious code
- Persistence — Staying there after reboot
- Credential Access — Stealing passwords
- Exfiltration — Stealing data out

### Techniques (The "How")
Specific methods attackers use:
- T1529 — Capture SMS Messages
- T1428 — Capture Video via Camera
- T1429 — Capture Audio via Microphone
- T1430 — Location Tracking

## In ShadowInspect:

When you scan an app, we map its permissions to MITRE techniques to tell you exactly what risks it poses. A banking app that requests SMS + Accessibility is mapped to T1529 (Credential Access).

## Try It Yourself:
Go to the MITRE section in ShadowInspect and explore 500+ techniques!
            """.trimIndent(),
            summaryPoints = listOf("MITRE ATT&CK is the standard language for cyber threats", "Tactics = goals, Techniques = methods", "ShadowInspect maps app behaviors to MITRE techniques"),
            iconRes = "📚", estimatedMinutes = 8, xpReward = 100, orderIndex = 4,
            mitreTechniques = listOf("T1529 - Capture SMS Messages", "T1430 - Location Tracking")
        ),
        SecurityLesson(
            id = 5,
            title = "Password Security 101",
            description = "Create strong passwords and keep them safe with password managers",
            category = LessonCategory.PASSWORD_SECURITY,
            difficulty = LessonDifficulty.BEGINNER,
            level = LearningLevel.BEGINNER,
            content = """
# Password Security: Your First Line of Defense

Passwords are like keys to your digital life. Weak passwords = easy break-in.

## What Hackers Do:

### Brute Force
Try every possible combination (works on short passwords).

### Dictionary Attacks
Try common words and passwords.

### Credential Stuffing
Use passwords leaked from other sites.

## Strong Password Formula:

Long (12+ characters)
+ Mixed case (Aa)
+ Numbers (123)
+ Symbols (!@#)
= Strong Password

Good example: Coffee${'$'}Rainbow#Sunset89

## Password Manager: Your Best Friend

Free Options:
- Bitwarden (open source, recommended)
- KeePassXC
- Apple Keychain
- Google Password Manager

## Password Checklist:
- At least 12 characters
- Mix of letters, numbers, symbols
- No personal info (birthday, name)
- Different for each account
- Stored in password manager
- 2FA enabled where possible
            """.trimIndent(),
            summaryPoints = listOf("Use long, complex passwords (12+ characters)", "Password managers make security easy", "Enable 2FA everywhere possible"),
            iconRes = "🔑", estimatedMinutes = 6, xpReward = 60, orderIndex = 5
        ),
        SecurityLesson(
            id = 6,
            title = "Network Security Basics",
            description = "Protect yourself on public Wi-Fi and understand network threats",
            category = LessonCategory.NETWORK_SECURITY,
            difficulty = LessonDifficulty.BEGINNER,
            level = LearningLevel.BEGINNER,
            content = """
# Network Security: Stay Safe Online

Your network connection is a door attackers can use. Understanding network threats helps you stay safe.

## Public Wi-Fi Dangers:

### Man-in-the-Middle Attacks
Attackers intercept your data between you and the server.

### Evil Twin Networks
Fake "Free Wi-Fi" hotspots that capture all your traffic.

### Packet Sniffing
Reading unencrypted data on the network.

## How to Stay Safe:

### Use a VPN
A VPN encrypts all your traffic. Recommended: ProtonVPN, Mullvad.

### Look for HTTPS
The padlock icon in your browser means your connection is encrypted.

### Avoid Sensitive Tasks on Public Wi-Fi
Don't do banking or shopping unless you absolutely must.

## Home Network Security:
- Change default router password
- Use WPA3 encryption
- Disable WPS
- Update router firmware regularly
            """.trimIndent(),
            summaryPoints = listOf("Public Wi-Fi can be dangerous", "Always use HTTPS and consider a VPN", "Secure your home router properly"),
            iconRes = "🌐", estimatedMinutes = 7, xpReward = 70, orderIndex = 6,
            mitreTechniques = listOf("T1439 - Eavesdrop on Insecure Network Communication")
        ),
        SecurityLesson(
            id = 7,
            title = "The Power of 2FA",
            description = "Learn why Two-Factor Authentication is your strongest defense",
            category = LessonCategory.TWO_FACTOR_AUTH,
            difficulty = LessonDifficulty.BEGINNER,
            level = LearningLevel.BEGINNER,
            content = """
# Two-Factor Authentication (2FA)

Even if a hacker steals your password, 2FA stops them in their tracks.

## How it Works
1. **Something you know:** Your password
2. **Something you have:** Your phone (for a code)

## Types of 2FA:
- **SMS Codes:** Better than nothing, but vulnerable to SIM swapping.
- **Authenticator Apps:** (Google Auth, Authy, Aegis) Very secure. Generates codes offline.
- **Hardware Keys:** (YubiKey) The most secure method available.

## Why it's Essential:
Hackers use stolen password databases. Without the second factor, those passwords are useless to them.

## Action Plan:
Turn on 2FA for:
1. Email accounts (most important!)
2. Banking & Finance
3. Social media
            """.trimIndent(),
            summaryPoints = listOf("2FA blocks 99.9% of automated account hacks", "Authenticator apps are safer than SMS codes", "Always secure your primary email first"),
            iconRes = "🔐", estimatedMinutes = 4, xpReward = 60, orderIndex = 7
        ),
        SecurityLesson(
            id = 8,
            title = "Safe Browsing Habits",
            description = "How to navigate the web without catching malware",
            category = LessonCategory.SAFE_BROWSING,
            difficulty = LessonDifficulty.BEGINNER,
            level = LearningLevel.BEGINNER,
            content = """
# Safe Browsing Habits

The web is full of invisible traps. A few simple rules can keep you safe.

## The Padlock (HTTPS)
Always look for the padlock icon in your browser's address bar. It means your connection is encrypted. Never enter passwords or credit cards on HTTP sites.

## Malicious Downloads
"Your Flash Player is out of date!" — these popups are always fake. Never download software from random popups or third-party file-sharing sites.

## Browser Extensions
Only install extensions from the official web store, and check their permissions. A simple ad-blocker shouldn't need access to "all your data on all websites."

## Ad Blockers
Using a good ad-blocker (like uBlock Origin) isn't just about annoyance—it actually blocks "malvertising" (malware distributed through ad networks).
            """.trimIndent(),
            summaryPoints = listOf("Never enter data on non-HTTPS sites", "Ignore update popups on websites", "Use a reputable ad-blocker for security"),
            iconRes = "🧭", estimatedMinutes = 5, xpReward = 50, orderIndex = 8
        ),
        SecurityLesson(
            id = 9,
            title = "Your Data, Your Rules (Privacy)",
            description = "Take control of your digital footprint across the internet",
            category = LessonCategory.PRIVACY,
            difficulty = LessonDifficulty.BEGINNER,
            level = LearningLevel.BEGINNER,
            content = """
# Digital Privacy

Privacy isn't about hiding bad things; it's about protecting yourself from manipulation and identity theft.

## Data Brokers
Companies collect your location, purchases, and browsing habits to build a profile of you and sell it.

## How to Fight Back:

### Change Your Search Engine
Google tracks everything. Try DuckDuckGo or Brave Search for private searching.

### Review Phone Settings
Turn off personalized ads in your Google/Apple settings. Turn off Location History if you don't use it.

### App Tracking
On iOS, use "Ask App Not to Track." On Android, continuously review which apps have background location access.

### Social Media
Lock down your privacy settings. Don't make your friends list or birth date public.
            """.trimIndent(),
            summaryPoints = listOf("Privacy protects you from identity theft", "Use privacy-respecting search engines", "Minimize what you share publicly"),
            iconRes = "👁️", estimatedMinutes = 6, xpReward = 60, orderIndex = 9
        ),
        SecurityLesson(
            id = 10,
            title = "Mind Games (Social Engineering)",
            description = "How hackers hack the human brain instead of computers",
            category = LessonCategory.SOCIAL_ENGINEERING,
            difficulty = LessonDifficulty.BEGINNER,
            level = LearningLevel.BEGINNER,
            content = """
# Social Engineering

The easiest way to break into a system isn't hacking the firewall—it's tricking an employee or user into giving up the keys.

## Common Tactics:

### Urgency & Fear
"Your account will be suspended in 2 hours!" Fear makes people stop thinking logically.

### Authority
"This is the IRS" or "This is the CEO." People naturally comply with authority figures.

### Familiarity
Hacking a friend's account to send you malicious links ("Hey, is this a picture of you?").

## How to Defeat Social Engineering:

**The Golden Rule:** STOP AND VERIFY.
If someone calls from the bank, hang up, look up the bank's official number, and call them back.

Take a breath. Scammers rely on rushed decisions.
            """.trimIndent(),
            summaryPoints = listOf("Social engineering targets human psychology", "Beware of artificial urgency or fear", "Always verify identities through independent channels"),
            iconRes = "🎭", estimatedMinutes = 5, xpReward = 50, orderIndex = 10
        )
    )
}
