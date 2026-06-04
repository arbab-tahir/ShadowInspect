package com.shadowinspect.app.data.seed

import com.shadowinspect.app.domain.education.LearningLevel
import com.shadowinspect.app.domain.education.LessonCategory
import com.shadowinspect.app.domain.education.LessonDifficulty
import com.shadowinspect.app.domain.education.SecurityLesson

object IntermediateLessons {
    val data = listOf(
        SecurityLesson(
            id = 11,
            title = "🔬 Malware Analysis 101",
            description = "Learn how security researchers analyze malware to understand its behavior",
            category = LessonCategory.MALWARE_ANALYSIS,
            difficulty = LessonDifficulty.INTERMEDIATE,
            level = LearningLevel.INTERMEDIATE,
            content = """
# Malware Analysis: Understanding the Enemy

Malware analysis is like an autopsy for malicious software. Security researchers dissect malware to understand:
- What it does
- How it spreads
- How to stop it
- Who created it

## Two Main Approaches:

### 1. Static Analysis (Without Running)
- Examine the code without executing it
- Check file hashes against virus databases
- Extract strings and metadata
- Identify packers/obfuscation
- **Tools:** Detect It Easy, PEiD, strings

### 2. Dynamic Analysis (In Safe Environment)
- Run malware in a sandbox
- Monitor file system changes
- Track registry modifications
- Capture network traffic
- **Tools:** ProcMon, Wireshark, Cuckoo Sandbox

## Key Indicators to Look For:

### File System Changes
- Creates files in system directories
- Modifies critical system files
- Drops additional malware

### Registry Changes (Windows)
- Adds auto-run entries
- Modifies security settings
- Disables protections

### Network Activity
- Connects to command & control servers
- Downloads additional payloads
- Exfiltrates data

### Process Behavior
- Injects code into legitimate processes
- Creates hidden processes
- Terminates security tools

## Practical Exercise:

Using ShadowInspect, when you scan an APK, we perform automated static analysis:

```
APK: suspicious_app.apk

Static Analysis Results:
- Detected 15 malicious API calls
- Found 3 suspicious strings: "crypt", "ransom", "pay"
- Obfuscation score: 0.85 (HIGH)
- Known malware family: TeaBot (92% match)

Dynamic Indicators (if run):
- Would request SMS permissions
- Would connect to C2 server: 185.142.53.12
- Would overlay banking apps
```

## Safety First!

⚠️ **NEVER analyze malware on your real device!**
- Use isolated VMs or sandboxes
- Disconnect from network
- Use ShadowInspect's safe static analysis

## Tools for Beginners:

1. **VirusTotal** - Check file hashes
2. **Any.Run** - Online sandbox
3. **ShadowInspect** - Mobile malware analysis
4. **Hybrid Analysis** - Free malware scanner
            """.trimIndent(),
            summaryPoints = listOf(
                "Malware analysis reveals what malicious software does",
                "Static analysis examines code without running it",
                "Dynamic analysis runs malware in safe environments",
                "ShadowInspect performs automated static analysis safely"
            ),
            iconRes = "🔬",
            estimatedMinutes = 12,
            xpReward = 120,
            orderIndex = 11,
            mitreTechniques = listOf("T1027 - Obfuscated Files or Information")
        ),
        SecurityLesson(
            id = 12,
            title = "🕵️ Advanced Phishing Techniques",
            description = "Go beyond basic phishing - learn how professional attackers trick even experts",
            category = LessonCategory.ADVANCED_PHISHING,
            difficulty = LessonDifficulty.INTERMEDIATE,
            level = LearningLevel.INTERMEDIATE,
            content = """
# Advanced Phishing: Beyond the Basics

Basic phishing is obvious to many users now. Advanced attackers use sophisticated techniques that can fool even security professionals.

## Spear Phishing

Targeted attacks using personal information:

```
Scenario: You work at Company X
Attacker researches:
- Your role and responsibilities
- Colleagues you work with
- Current projects
- Software you use

Email appears to come from your boss:
"Hi [Your Name], working on the [Project Name] report?
Can you review the attached updated requirements?
Thanks, [Boss's Name]"

Attachment: Project_Requirements_2024.docx (with macros)
```

## Whaling

Targeting C-level executives:
- Legal threats
- SEC investigations
- Acquisition opportunities
- Personal scandals

## Clone Phishing

1. Attacker copies a legitimate email you received
2. Replaces links/attachments with malicious versions
3. Sends from spoofed address
4. Looks identical to original

## Evilginx (Man-in-the-Middle)

Advanced tool that bypasses 2FA:
```
User visits: bank.com (actually attacker's proxy)
User enters: username/password
User enters: 2FA code
Attractor captures ALL in real-time
Attacker logs into real bank immediately
```

## Deep Fake Audio/Video

AI-generated voices and faces:
- CEO's voice cloned (3 seconds of audio needed)
- Video calls with fake faces
- Urgent wire transfer requests

**Real Case:** ${'$'}35M stolen using deep fake CEO voice

## Watering Hole Attacks

1. Attacker identifies websites you frequently visit
2. Compromises those websites
3. Waits for you to visit
4. Exploits browser vulnerabilities

## Quishing (QR Code Phishing)

```
Email pretending to be from IT:
"Important security update!
Scan this QR code to verify your account"

QR code points to malicious site
Mobile devices often lack URL preview
```

## How to Protect Yourself:

1. **Verify out-of-band** - Call to confirm urgent requests
2. **Check URLs carefully** - Hover, don't click
3. **Use hardware tokens** - YubiKey prevents Evilginx
4. **Enable DMARC reporting** - See who's spoofing you
5. **Security awareness training** - Regular updates
6. **Report suspicious emails** - Help others

## Try It Yourself:

Use ShadowInspect's URL scanner on suspicious links before clicking:
```
Shortened URL: https://bit.ly/3xY7z9k
ShadowInspect reveals: https://evil-site.com/bank-login
Risk: CRITICAL - Phishing site detected
```
            """.trimIndent(),
            summaryPoints = listOf(
                "Advanced phishing uses personalization and technical tricks",
                "Even 2FA can be bypassed with proxy attacks",
                "Always verify urgent requests through another channel"
            ),
            iconRes = "🕵️",
            estimatedMinutes = 10,
            xpReward = 100,
            orderIndex = 12,
            mitreTechniques = listOf("T1566 - Phishing", "T1184 - Drive-by Compromise")
        ),
        SecurityLesson(
            id = 13,
            title = "🔧 Android Internals & Security",
            description = "Deep dive into how Android really works and where security matters",
            category = LessonCategory.ANDROID_SECURITY,
            difficulty = LessonDifficulty.INTERMEDIATE,
            level = LearningLevel.INTERMEDIATE,
            content = """
# Android Internals: Under the Hood

Understanding how Android works helps you understand security risks.

## Android Architecture

```
┌─────────────────────────────┐
│         Apps                │
├─────────────────────────────┤
│    Application Framework    │
├─────────────────────────────┤
│    Android Runtime (ART)    │
├─────────────────────────────┤
│    Native Libraries         │
├─────────────────────────────┤
│    Hardware Abstraction     │
├─────────────────────────────┤
│    Linux Kernel             │
└─────────────────────────────┘
```

## Security Layers

### 1. Linux Kernel
- User isolation (each app = different user)
- File permissions
- SELinux (mandatory access control)
- Namespaces and cgroups

### 2. Application Sandbox
Each app runs in its own sandbox:
- Unique user ID (UID)
- Separate data directory
- Cannot access other apps' data
- Cannot directly access hardware

### 3. Permissions System
- Install-time permissions (legacy)
- Runtime permissions (Android 6+)
- One-time permissions (Android 11+)
- Auto-reset unused permissions

### 4. APK Signing
- Apps must be cryptographically signed
- Signatures verified at install
- Updates must use same key
- v1, v2, v3 signature schemes

## Important Security Features

### Verified Boot
- Checks system integrity at boot
- Prevents persistent malware
- Shows warning if modified

### Google Play Protect
- Scans installed apps
- Checks for harmful behavior
- Cloud-based detection

### SafetyNet/Play Integrity
- Attests device integrity
- Used by banking apps
- Detects rooted devices

## How ShadowInspect Helps

When you scan an APK, we check:
```
✓ Target SDK version (older = less secure)
✓ Requested permissions (dangerous combinations)
✓ Signature scheme (v2/v3 are secure, v1 vulnerable)
✓ Manifest anomalies (debuggable, backup enabled)
✓ Hardcoded secrets (API keys, passwords)
```
            """.trimIndent(),
            summaryPoints = listOf(
                "Android has multiple security layers: kernel, sandbox, permissions",
                "Each app runs in an isolated environment",
                "Understanding internals helps you make security decisions"
            ),
            iconRes = "🔧",
            estimatedMinutes = 12,
            xpReward = 120,
            orderIndex = 13,
            mitreTechniques = listOf("T1614 - System Location Discovery", "T1636 - Protected User Data")
        ),
        SecurityLesson(
            id = 14,
            title = "📊 Understanding the MITRE Matrix",
            description = "Deep dive into the MITRE ATT&CK framework and how professionals use it",
            category = LessonCategory.MITRE_MATRIX,
            difficulty = LessonDifficulty.INTERMEDIATE,
            level = LearningLevel.INTERMEDIATE,
            content = """
# MITRE ATT&CK Matrix: Professional's Toolkit

MITRE ATT&CK isn't just a list - it's a framework used by security teams worldwide.

## The 14 Tactics (Why)

```
1. Reconnaissance         - Gathering info before attack
2. Resource Development   - Setting up infrastructure
3. Initial Access        - Getting in
4. Execution             - Running malicious code
5. Persistence           - Staying in
6. Privilege Escalation  - Getting more power
7. Defense Evasion       - Hiding from detection
8. Credential Access     - Stealing passwords
9. Discovery             - Learning the environment
10. Lateral Movement     - Moving through network
11. Collection           - Gathering data
12. Command & Control    - Talking to attacker
13. Exfiltration         - Stealing data out
14. Impact               - Damage/ransom
```

## Enterprise vs Mobile

### Enterprise Matrix
- Windows, macOS, Linux
- Cloud, Containers
- Network devices

### Mobile Matrix
- Android, iOS
- Device-specific techniques
- App store abuse

## Real-World Example: Banking Trojan

```
Tactic: Initial Access (TA0001)
├── Technique: Spearphishing Link (T1566.002)
│   └── User clicks SMS link
│
Tactic: Execution (TA0002)
├── Technique: User Execution (T1204)
│   └── User installs malicious app
│
Tactic: Persistence (TA0003)
├── Technique: Boot or Logon Autostart (T1547)
│   └── App starts on boot
```

## Using ShadowInspect's MITRE View

In our app, go to the MITRE section to see:
- All core techniques matched against suspicious apps
- Which techniques your scanned apps used
- Links to official MITRE documentation
            """.trimIndent(),
            summaryPoints = listOf(
                "MITRE ATT&CK has 14 tactics and hundreds of techniques",
                "Security professionals use it for detection, response, and threat intel",
                "Each attacker group has unique technique patterns"
            ),
            iconRes = "📊",
            estimatedMinutes = 15,
            xpReward = 150,
            orderIndex = 14
        ),
        SecurityLesson(
            id = 15,
            title = "🔍 OSINT & Digital Footprint",
            description = "Learn how attackers find information about you online and how to protect your privacy",
            category = LessonCategory.OSINT,
            difficulty = LessonDifficulty.INTERMEDIATE,
            level = LearningLevel.INTERMEDIATE,
            content = """
# OSINT: Open Source Intelligence

OSINT is collecting information from publicly available sources. Attackers use it to build profiles for social engineering, password guessing, and targeted attacks.

## What Attackers Can Find

### Social Media
```
Facebook/Instagram:
- Your full name, birthday, location
- Family members, friends
- Places you visit
- Interests and hobbies
- Pet names (common passwords!)
```

### Data Breaches
```
HaveIBeenPwned shows:
- Emails in breaches
- Passwords leaked
- Which sites compromised
```

### Technical OSINT

#### WHOIS Lookups
Domain registration exposes contact details.

#### GitHub Search
Search for company name + "password" or API keys accidentally committed.

### Google Dorking
Advanced Google searches to find exposed data.
`site:linkedin.com "Company Name" "Software Engineer"`
`intitle:"index of" "backup" site:example.com`

## Your Digital Footprint

Every website you visit records your IP, device information, and rough location.

## Protecting Your Digital Footprint

### 1. Social Media Settings
Set profiles to private. Don't share full birth dates or pet names.

### 2. Use Aliases
Different usernames per platform. Use email aliases (SimpleLogin).

### 3. Technical Protections
VPN (hides IP). Privacy-focused browser (Firefox).

## Exercise: Audit Yourself
Try Googling your full name in quotes. Search HaveIBeenPwned. You might be surprised!
            """.trimIndent(),
            summaryPoints = listOf(
                "OSINT collects publicly available information about you",
                "Attackers piece together data from multiple sources",
                "Protect yourself with privacy settings and minimal sharing"
            ),
            iconRes = "🔍",
            estimatedMinutes = 12,
            xpReward = 120,
            orderIndex = 15,
            mitreTechniques = listOf("T1589 - Gather Victim Identity Information")
        ),
        SecurityLesson(
            id = 16,
            title = "🛡️ Enterprise Security Concepts",
            description = "How large organizations protect thousands of endpoints",
            category = LessonCategory.ENTERPRISE_SECURITY,
            difficulty = LessonDifficulty.INTERMEDIATE,
            level = LearningLevel.INTERMEDIATE,
            content = """
# Enterprise Security: Scaling Defense

Protecting a single PC is one thing; protecting 10,000 corporate laptops is an entirely different beast.

## Core Concepts

### IAM (Identity and Access Management)
Managing who has access to what. Involves Single Sign-On (SSO), Active Directory, and Role-Based Access Control (RBAC).
*Rule:* Principle of Least Privilege. Only grant exactly the access an employee needs.

### EDR (Endpoint Detection and Response)
Antivirus is dead. EDR solutions continuously monitor all laptops and servers for suspicious behaviors (like unusal PowerShell commands), catching threats that signature-based AV misses.

### SIEM (Security Information and Event Management)
A massive database that collects logs from every firewall, router, and server in the company. Security analysts write rules to detect anomalies globally.

### DLP (Data Loss Prevention)
Systems designed to prevent sensitive data (like credit cards or source code) from leaving the corporate network, either maliciously or accidentally.

## Network Segmentation
A company's network isn't flat. HR shouldn't be able to access the developer databases. Segmentation limits the "blast radius" if an attacker breaches one department.
            """.trimIndent(),
            summaryPoints = listOf(
                "Enterprise security focuses on scale, visibility, and management.",
                "EDR looks for bad behaviors, not just known bad files.",
                "Zero Trust is the modern standard: trust nothing, verify everything."
            ),
            iconRes = "🛡️",
            estimatedMinutes = 10,
            xpReward = 100,
            orderIndex = 16
        ),
        SecurityLesson(
            id = 17,
            title = "📡 Wi-Fi Security & MITM Attacks",
            description = "Deep dive into WPA frameworks and rogue access points",
            category = LessonCategory.WIFI_SECURITY,
            difficulty = LessonDifficulty.INTERMEDIATE,
            level = LearningLevel.INTERMEDIATE,
            content = """
# Wi-Fi Security & Interception

Wireless networks broadcast data through the air, meaning anyone with an antenna can "hear" the conversation. 

## Encryption Standards

### WEP (Wired Equivalent Privacy)
Obsolete. Can be cracked in minutes using tools like Aircrack-ng.

### WPA2
The standard for over a decade. Secure as long as the password is long. Vulnerable to "KRACK" attacks if devices are unpatched.

### WPA3
The modern standard. Features "Simultaneous Authentication of Equals" (SAE) which makes offline dictionary attacks nearly impossible.

## Rogue Access Points & Evil Twins

An "Evil Twin" is a rogue Wi-Fi access point that appears to be legitimate (e.g., named "Starbucks_Free_WiFi") but is actually run by an attacker.
When you connect, the attacker performs a Man-In-The-Middle (MITM) attack.

### Captive Portal Phishing
The Evil Twin redirects you to a fake login page, asking for your Google or Microsoft credentials to "authenticate" you to the internet.

### SSL Stripping
The attacker intercepts your HTTPS requests and downgrades them to unencrypted HTTP, allowing them to read your passwords in plain text. Modern HSTS largely prevents this, but edge cases remain.
            """.trimIndent(),
            summaryPoints = listOf(
                "WPA3 drastically increases wireless security.",
                "Evil Twins trick devices into connecting to malicious networks.",
                "Always use a VPN on untrusted or public Wi-Fi."
            ),
            iconRes = "📡",
            estimatedMinutes = 10,
            xpReward = 100,
            orderIndex = 17,
            mitreTechniques = listOf("T1439 - Eavesdrop on Insecure Network Communication")
        ),
        SecurityLesson(
            id = 18,
            title = "💳 Payment Security & Fraud",
            description = "How credit card data is stolen and protected",
            category = LessonCategory.PAYMENT_SECURITY,
            difficulty = LessonDifficulty.INTERMEDIATE,
            level = LearningLevel.INTERMEDIATE,
            content = """
# Payment Security

Financial fraud is the primary motivation for most cybercriminals.

## Point of Sale (POS) Malware
Attackers target the cash registers at physical retail stores. The malware scrapes the memory of the POS machine to steal magnetic stripe data (Track 1 and Track 2 data) the exact moment a card is swiped, before it's encrypted.

## Magecart & e-Skimming
Digital credit card skimming. Attackers compromise thousands of e-commerce websites and inject malicious JavaScript on the checkout page. When a user enters their card info, the script silently sends a copy to the attacker.

## Tokenization
Modern protection mechanism. When you use Apple Pay, Google Pay, or chip readers, the store never sees your actual card number. Instead, a single-use "token" is generated. Even if a hacker steals the token, it cannot be used for any other transaction.

## PCI-DSS
The Payment Card Industry Data Security Standard. A strict set of regulations any merchant must follow if they handle credit card data. It mandates encryption, network segmentation, and regular audits.
            """.trimIndent(),
            summaryPoints = listOf(
                "Magecart attacks steal credit card data directly from website checkout pages.",
                "Tokenization replaces real card numbers with single-use codes.",
                "Avoid using magnetic stripes; use Chip or Contactless whenever possible."
            ),
            iconRes = "💳",
            estimatedMinutes = 8,
            xpReward = 80,
            orderIndex = 18
        ),
        SecurityLesson(
            id = 19,
            title = "📧 Email Security & Headers",
            description = "Understanding DMARC, SPF, and inspecting raw email headers",
            category = LessonCategory.EMAIL_SECURITY,
            difficulty = LessonDifficulty.INTERMEDIATE,
            level = LearningLevel.INTERMEDIATE,
            content = """
# Email Authentication & Headers

Email was invented without security in mind. Anyone could send an email claiming to be anyone else.

## The Big Three: SPF, DKIM, DMARC

### SPF (Sender Policy Framework)
A DNS record that lists exactly which IP addresses are allowed to send email on behalf of a domain. "Only IPs X and Y can send email as @google.com."

### DKIM (DomainKeys Identified Mail)
Adds a cryptographic signature to emails. The receiving server uses the sender's public key (found via DNS) to verify the signature. Ensures the email wasn't altered in transit.

### DMARC (Domain-based Message Authentication)
Tells the receiving server what to do if an email fails SPF or DKIM checks. Policies include:
- `p=none` (Log the failure but deliver it)
- `p=quarantine` (Send to Spam folder)
- `p=reject` (Drop the email entirely)

## Inspecting Email Headers
Every email includes hidden metadata called headers. By viewing the "Raw Email" or "Original Message", you can trace the exact path the email took across the internet.
You can look for the `Authentication-Results` header to see if SPF and DKIM passed or failed.
            """.trimIndent(),
            summaryPoints = listOf(
                "SPF verifies sender IPs, DKIM verifies message integrity.",
                "DMARC instructs the receiver on how to handle spoofed emails.",
                "Raw email headers reveal the true origin of a phishing attack."
            ),
            iconRes = "📧",
            estimatedMinutes = 10,
            xpReward = 100,
            orderIndex = 19
        ),
        SecurityLesson(
            id = 20,
            title = "🗑️ Data Destruction & Privacy",
            description = "Why securely wiping data is harder than it looks",
            category = LessonCategory.DATA_DESTRUCTION,
            difficulty = LessonDifficulty.INTERMEDIATE,
            level = LearningLevel.INTERMEDIATE,
            content = """
# Secure Data Destruction

When you press "Delete" and empty the recycle bin, the file isn't actually gone. 

## How Deletion Works
Operating systems maintain a "Table of Contents" for your hard drive. Deleting a file simply removes its entry from the table, marking the space as "available." The actual data (the 1s and 0s) remains on the disk until another file overwrites it.

## File Recovery
Because the data isn't overwritten immediately, tools like Recuva, Autopsy, or PhotoRec can routinely recover deleted files from hard drives, USBs, and SD cards weeks or months later.

## Secure Wiping

### Magnetic Drives (HDD)
To securely delete a file on an HDD, you must use a file shredder that overwrites the file's physical location with random data or zeros multiple times.

### Solid State Drives (SSD)
SSDs use "Wear Leveling" to distribute data evenly across flash chips. If you tell a shredder to overwrite a specific file, the SSD controller might silently write those zeros to a DIFFERENT flash chip, leaving the original data intact!
*Solution:* For SSDs and smartphones, the only mathematically secure form of physical data destruction is full-device encryption. 

## Crypto-Shredding
If your phone is natively encrypted (like all modern Androids and iPhones), a factory reset simply deletes the encryption key. Boom—the data is instantly rendered as unrecoverable gibberish, even if the raw physical flash memory is extracted.
            """.trimIndent(),
            summaryPoints = listOf(
                "Deleting a file does not erase the underlying data.",
                "SSDs wear-leveling makes targeted file shredding unreliable.",
                "Crypto-shredding (deleting the encryption key) is the safest way to wipe a modern phone."
            ),
            iconRes = "🗑️",
            estimatedMinutes = 10,
            xpReward = 100,
            orderIndex = 20
        )
    )
}
