package com.shadowinspect.app.data.seed

import com.shadowinspect.app.domain.education.LearningLevel
import com.shadowinspect.app.domain.education.LessonCategory
import com.shadowinspect.app.domain.education.LessonDifficulty
import com.shadowinspect.app.domain.education.SecurityLesson

object AdvancedLessons {
    val data = listOf(
        SecurityLesson(
            id = 21,
            title = "🧠 Reverse Engineering Basics",
            description = "Learn how to analyze compiled code and understand malware internals",
            category = LessonCategory.REVERSE_ENGINEERING,
            difficulty = LessonDifficulty.ADVANCED,
            level = LearningLevel.ADVANCED,
            content = """
# Reverse Engineering: Decompiling the Threat

Reverse engineering (RE) is taking apart software to see how it works, often without having the original source code.

## Why Reverse Engineer?
- To analyze zero-day malware.
- To discover unknown vulnerabilities.
- To bypass DRM or software protections (as attackers do).

## Tools of the Trade

### Disassemblers
Take machine code (binary zeros and ones) and convert it into low-level Assembly Language. 
*Examples:* IDA Pro, Ghidra.

### Decompilers
Attempt to take Assembly and reconstruct it back into high-level code like C or Java. It's never perfect, but it's readable.

### Debuggers
Allow the reverse engineer to pause the program while it's running, inspect memory registers, and step through the code one line at a time.
*Examples:* x64dbg, GDB, Frida.

## Android Reverse Engineering
Android apps (.apk) are essentially zip files containing Dalvik bytecode (`classes.dex`).
1. **Apktool:** Unpacks the APK to reveal resources and `AndroidManifest.xml`.
2. **Jadx:** A powerful tool that converts `.dex` bytecode directly back into readable Java code.

Once decompiled with Jadx, an analyst can hunt for API keys, hardcoded server URLs, or encryption algorithms hidden by the malware author.
            """.trimIndent(),
            summaryPoints = listOf(
                "Reverse engineering turns binary executables back into readable code.",
                "Ghidra and IDA Pro are industry standards for complex binaries.",
                "Android apps use Jadx and Apktool for decompilation."
            ),
            iconRes = "🧠",
            estimatedMinutes = 15,
            xpReward = 150,
            orderIndex = 21
        ),
        SecurityLesson(
            id = 22,
            title = "🔐 Cryptography Explained",
            description = "Understand encryption, hashing, and how they protect your data",
            category = LessonCategory.CRYPTOGRAPHY,
            difficulty = LessonDifficulty.ADVANCED,
            level = LearningLevel.ADVANCED,
            content = """
# Applied Cryptography

Cryptography is the mathematical foundation of digital security. It secures web traffic, passwords, and cryptocurrencies.

## Hashing
A one-way mathematical function. It takes any amount of data and produces a fixed-length string of characters (a hash).
- **Properties:** You cannot calculate the input from the hash. A tiny change in input completely changes the hash.
- **Uses:** Storing passwords safely, verifying file integrity.
- **Algorithms:** SHA-256 (Secure), MD5 (Broken).

## Symmetric Encryption
The same key is used to both encrypt and decrypt the data. Like a physical key that locks and unlocks a door.
- **Fast and efficient**, used for bulk data (like encrypting your hard drive).
- **Algorithms:** AES-256.

## Asymmetric Encryption (Public Key Cryptography)
Uses a mathematically linked pair of keys: a Public Key and a Private Key.
- You share the Public Key with the world. Anyone can use it to encrypt a message to you.
- ONLY your Private Key can decrypt that message.
- **Uses:** Securing HTTPS, SSH keys, digital signatures.
- **Algorithms:** RSA, ECC (Elliptic Curve).

## TLS/SSL
When you connect to HTTPS, your browser uses Asymmetric encryption to securely agree on a secret key with the server, and then switches to Symmetric Encryption (AES) for the rest of the session for speed.
            """.trimIndent(),
            summaryPoints = listOf(
                "Hashing is one-way, used for password storage and file integrity.",
                "Symmetric encryption uses one key; Asymmetric uses two.",
                "Modern internet security relies on Elliptic Curve Cryptography (ECC)."
            ),
            iconRes = "🔐",
            estimatedMinutes = 12,
            xpReward = 120,
            orderIndex = 22
        ),
        SecurityLesson(
            id = 23,
            title = "🌐 Dark Web & Anonymity",
            description = "Learn about Tor, .onion sites, and how criminals operate anonymously",
            category = LessonCategory.DARK_WEB,
            difficulty = LessonDifficulty.ADVANCED,
            level = LearningLevel.ADVANCED,
            content = """
# The Dark Web and Tor Network

The "Dark Web" refers to websites that use anonymity networks to hide their physical server locations and the identities of their users.

## How Tor (The Onion Router) Works
When you use the Tor browser, your traffic doesn't go straight to the destination website. It is bounced through three random volunteer servers (nodes) around the world:
1. **Entry Guard:** Knows your IP address, but doesn't know what site you are visiting.
2. **Middle Relay:** Disconnects your identity from the destination. Knows the entry and exit node, but neither your IP nor the destination.
3. **Exit Node:** Knows what site you are visiting, but has no idea who you are.

The data is wrapped in three layers of encryption (like an onion). Each node peels off one layer.

## Onion Services (.onion)
These are hidden websites hosted entirely within the Tor network. The URL is a long cryptographic hash (e.g., `expyuz5d...onion`). Traffic never exits onto the normal internet, making end-to-end anonymity extremely strong.

## Ransomware and the Dark Web
Modern ransomware gangs host their "leak sites" on the Dark Web. If victims don't pay the cryptocurrency ransom, the gangs dump stolen corporate data onto these untraceable .onion sites.

*Note:* Tor was originally funded by the US Naval Research Laboratory and is widely used by dissidents, journalists, and intelligence agencies, not just criminals.
            """.trimIndent(),
            summaryPoints = listOf(
                "Tor routes traffic through three nodes to mask user identity.",
                "Onion sites (.onion) exist entirely within the Tor network and defy regular tracking.",
                "Law enforcement shuts down dark web markets through OPSEC failures, rarely by breaking Tor encryption."
            ),
            iconRes = "🌐",
            estimatedMinutes = 10,
            xpReward = 100,
            orderIndex = 23
        ),
        SecurityLesson(
            id = 24,
            title = "🕸️ Web Application Security",
            description = "Understanding OWASP Top 10 vulnerabilities like SQLi and XSS",
            category = LessonCategory.WEB_APP_SECURITY,
            difficulty = LessonDifficulty.ADVANCED,
            level = LearningLevel.ADVANCED,
            content = """
# Web Application Exploits

The OWASP Top 10 is the industry-standard list of the most critical security risks to web applications.

## SQL Injection (SQLi)
The most infamous web vulnerability. It occurs when a website takes user input (like a search query) and inserts it directly into a database command without sanitizing it.
*Example:* `SELECT * FROM users WHERE username = 'admin' OR '1'='1' --`
This allows attackers to dump entire databases, steal passwords, or bypass logins. Defeated by using Parameterized Queries.

## Cross-Site Scripting (XSS)
Occurs when a website allows users to post text, and an attacker posts malicious JavaScript. When an innocent user views the page, their browser executes the script.
*Impact:* The script can steal the user's session cookie and send it to the attacker, hijacking their account instantly.

## Insecure Direct Object Reference (IDOR)
An authorization failure. Imagine viewing your bank statement at:
`bank.com/statement?account_id=5555`
If you change the URL to `account_id=5556` and the server doesn't verify that YOU own account 5556, it will display a stranger's bank statement.

## Server-Side Request Forgery (SSRF)
Tricking a server into making HTTP requests to internal, firewalled services (like Amazon AWS metadata servers) on behalf of the attacker.
            """.trimIndent(),
            summaryPoints = listOf(
                "SQL Injection allows hackers to manipulate backend databases.",
                "XSS executes malicious scripts in innocent users' browsers.",
                "IDOR is a failure to properly verify permissions on objects."
            ),
            iconRes = "🕸️",
            estimatedMinutes = 15,
            xpReward = 150,
            orderIndex = 24
        ),
        SecurityLesson(
            id = 25,
            title = "📱 Mobile Malware Deep Dive",
            description = "Advanced analysis of mobile malware families and their techniques",
            category = LessonCategory.MOBILE_MALWARE_DEEP,
            difficulty = LessonDifficulty.ADVANCED,
            level = LearningLevel.ADVANCED,
            content = """
# Advanced Mobile Malware Tactics

Modern Android malware has evolved far beyond simple adware. State-of-the-art Trojans use staggering complexity to stay hidden.

## Accessibility Service Abuse
The Accessibility API was built for users with disabilities (screen readers). Malware abuses it to read everything on the screen in real-time.
- **Overlay Attacks:** The malware detects when a banking app opens, and instantly draws a fake login screen *over* the real app.
- **Automated Interaction:** It can tap the screen by itself to dismiss security prompts or send PayPal transfers.

## Droppers & Dynamic Code Loading
Google Play Protect scans apps when they are uploaded.
*The Bypass:* Criminals upload a "clean", harmless app (a dropper). Once installed on your phone, the app silently downloads a malicious payload from an encrypted server and executes it directly into memory using `DexClassLoader`. Play Protect never sees the actual malware.

## Obfuscation & Commercial Packers
To defeat analysis tools, malware authors run their code through commercial "packers" (like DexGuard or Qihoo). These tools heavily encrypt the logic, rename classes to chaotic characters, and add anti-debugging measures that cause the app to crash intentionally if it detects it's running in an analyst's emulator sandbox.
            """.trimIndent(),
            summaryPoints = listOf(
                "The Accessibility API is the single most abused feature in Android malware.",
                "Dropper apps bypass Play Protect by downloading payloads post-installation.",
                "Packers encrypt Android apps to defeat automated malware analysis systems."
            ),
            iconRes = "📱",
            estimatedMinutes = 12,
            xpReward = 120,
            orderIndex = 25
        ),
        SecurityLesson(
            id = 26,
            title = "🔥 Zero-Day Exploits",
            description = "Understand how unknown vulnerabilities are discovered and exploited",
            category = LessonCategory.ZERO_DAY,
            difficulty = LessonDifficulty.ADVANCED,
            level = LearningLevel.ADVANCED,
            content = """
# Zero-Day Vulnerabilities

A "Zero-Day" (0-day) is a software vulnerability that is unknown to the software vendor. Because it's unknown, there is "zero days" of warning and no patch available.

## The Lifecycle of an Exploit

1. **Discovery:** A security researcher (or hacker) spends months reverse-engineering a complex component (e.g., the iOS iMessage image parser or Android Bluetooth stack).
2. **Development:** They write an exploit—a highly specialized program that specifically abuses the vulnerability to gain execution on the target device.
3. **Weaponization:** The exploit is chained with other exploits (such as a sandbox escape and a kernel privilege escalation) to gain full invisible control of the phone.
4. **Execution:** Sent via an invisible link or network packet.

## Zero-Click Exploits
The holy grail of hacking. The victim doesn't have to click a link or download an app. Simply receiving a maliciously crafted iMessage or WhatsApp packet triggers the exploit before the user's phone even rings. 

## The Zero-Day Market
Zero-day exploits are multi-million dollar business.
- **White Market:** Bug bounty programs (Google, Apple) pay researchers $1M+ to responsibly disclose them so they can be fixed.
- **Gray Market:** Companies like NSO Group or Zerodium buy exploits to build spyware tools (like Pegasus) which are then sold to intelligence agencies.
            """.trimIndent(),
            summaryPoints = listOf(
                "A zero-day has no patch available because the vendor is unaware of the flaw.",
                "Zero-click exploits compromise a device without any user interaction.",
                "The market for zero-days is highly lucrative, reaching millions of dollars per exploit chain."
            ),
            iconRes = "🔥",
            estimatedMinutes = 10,
            xpReward = 100,
            orderIndex = 26
        ),
        SecurityLesson(
            id = 27,
            title = "🏢 Corporate Security Architecture",
            description = "Designing secure networks: Defense in Depth and Zero Trust",
            category = LessonCategory.CORPORATE_SECURITY,
            difficulty = LessonDifficulty.ADVANCED,
            level = LearningLevel.ADVANCED,
            content = """
# Defense Architecture

How do major tech companies secure their massive global architectures?

## Defense in Depth
The "Castle Approach". Do not rely on a single defensive mechanism. If an attacker breaches the outer firewall, they must then face endpoint agents, then database encryption, then multi-factor authentication. Multiple overlapping layers drastically reduce the probability of total compromise.

## The Demilitarized Zone (DMZ)
A network segment that is exposed to the internet (hosting the public web servers) but strictly walled off from the internal corporate network (where HR databases and source code live).

## Zero Trust Architecture
The modern paradigm championed by Google (BeyondCorp).
- **Old Way:** Once a laptop is on the corporate VPN, it is trusted and has wide access.
- **Zero Trust Way:** The network itself is considered hostile. Being plugged into the office wall grants NO privileges. Every single application access request must mathematically verify the identity of the user (YubiKey) AND the health of the device (is the OS patched? Is EDR running?).

Zero Trust assumes the perimeter is already breached and focuses on granular micro-segmentation.
            """.trimIndent(),
            summaryPoints = listOf(
                "Defense in Depth uses overlapping security controls to mitigate single points of failure.",
                "A DMZ isolates internet-facing servers from sensitive internal networks.",
                "Zero Trust removes implicit trust; device and user identity must be continually verified."
            ),
            iconRes = "🏢",
            estimatedMinutes = 10,
            xpReward = 100,
            orderIndex = 27
        ),
        SecurityLesson(
            id = 28,
            title = "⚖️ Cyber Law & Ethics",
            description = "Hacking legally: The Computer Fraud and Abuse Act (CFAA)",
            category = LessonCategory.CYBER_LAW,
            difficulty = LessonDifficulty.ADVANCED,
            level = LearningLevel.ADVANCED,
            content = """
# Cybersecurity Law & Ethics

Hacking into a computer without permission is universally illegal, regardless of intent.

## The CFAA (USA)
The Computer Fraud and Abuse Act is the primary federal anti-hacking law in the United States. It criminalizes "exceeding authorized access." 
If you find a vulnerability on a company's website and download their user database to prove you found the bug, you have committed a federal felony, even if you planned to tell them about it nicely.

## GDPR (Europe)
The General Data Protection Regulation heavily dictates enterprise security. If a company suffers a data breach due to negligence, they can be fined up to 4% of their GLOBAL annual revenue. This law forced companies to take cybersecurity seriously.

## Bug Bounties vs Unauthorized Testing
**Authorized:** A company runs a Bug Bounty program on platforms like HackerOne. They provide a "Safe Harbor" policy explicitly granting you permission to attack specific domains.
**Unauthorized:** Scanning a random company with Nmap or testing their login form for SQL injection. This is illegal.

## Ethics of Exploitation
The difference between a White Hat and a Black Hat is authorization. Always secure written permission before launching a port scan or exploit payload against any system you do not own.
            """.trimIndent(),
            summaryPoints = listOf(
                "The CFAA strictly criminalizes unauthorized access to computer systems.",
                "Safe Harbor agreements in Bug Bounties provide legal protection to researchers.",
                "Authorization is the sole delineating factor between ethical research and cybercrime."
            ),
            iconRes = "⚖️",
            estimatedMinutes = 8,
            xpReward = 80,
            orderIndex = 28
        ),
        SecurityLesson(
            id = 29,
            title = "🔬 Forensic Analysis",
            description = "How investigators reconstruct cyber attacks from digital ashes",
            category = LessonCategory.FORENSICS,
            difficulty = LessonDifficulty.ADVANCED,
            level = LearningLevel.ADVANCED,
            content = """
# Digital Forensics and Incident Response (DFIR)

When an APT breaches a network or a ransomware attack strikes, forensic analysts step in to figure out exactly how the attackers got in, what they took, and how to kick them out.

## RAM Capture (Memory Forensics)
Malware often runs purely in memory to avoid touching the hard drive. If investigators reboot a compromised server, that evidence is lost forever. First responders use tools like Volatility to capture a snapshot of exactly what is in RAM to find hidden backdoors and decrypted passwords.

## Dead Box Forensics
Taking an exact cryptographic "image" (a bit-for-bit clone) of a hard drive. Investigators never analyze the original drive directly to preserve chain of custody for court. They search the replica for deleted files, timeline anomalies, and registry modifications.

## Timeline Analysis
Investigators overlay hundreds of complex logs (web server access, VPN logins, file modifications) into a master timeline (super timeline). By scrolling through, they can spot that 2 seconds after an employee clicked an email, an unusual PowerShell process spawned, and 5 minutes later, an admin account was created.
            """.trimIndent(),
            summaryPoints = listOf(
                "Incident responders must capture RAM before rebooting compromised servers.",
                "Cryptographic drive imaging preserves the legal chain of custody.",
                "Super timelines reconstruct the step-by-step narrative of a cyber attack."
            ),
            iconRes = "🔬",
            estimatedMinutes = 12,
            xpReward = 120,
            orderIndex = 29
        ),
        SecurityLesson(
            id = 30,
            title = "🎯 CTF Challenges & Practice",
            description = "Capture The Flag: How hackers hone their skills legally",
            category = LessonCategory.CTF_CHALLENGES,
            difficulty = LessonDifficulty.ADVANCED,
            level = LearningLevel.ADVANCED,
            content = """
# Capture The Flag (CTF)

CTFs are gamified cybersecurity competitions where players solve complex computer security problems to find a hidden string of text—the "Flag."

## Types of CTFs

### Jeopardy Style
A board of diverse categories:
- **Cryptography:** Deciphering encrypted messages.
- **Web Exploitation:** Hacking intentionally vulnerable websites.
- **Reverse Engineering:** Cracking binary files.
- **Forensics:** Finding hidden data inside packet captures or memory dumps.

### Attack/Defense
More advanced. Teams are given identical servers with vulnerable services. You must simultaneously patch the vulnerabilities on your server while launching exploits against other teams' servers.

## Why Play CTFs?
Certifications teach theory. CTFs teach raw technical skill. There is a massive difference between knowing what SQL Injection is, and actually writing a blind Boolean SQL injection payload by hand to extract a database table.

## Platforms to Start
- **TryHackMe:** Excellent for beginners. Provides guided virtual machines in your browser.
- **HackTheBox:** Intermediate to advanced. Features highly realistic, deeply complex vulnerable server environments.
- **PicoCTF:** Hosted by Carnegie Mellon University, specifically designed for high school/college students to learn.
            """.trimIndent(),
            summaryPoints = listOf(
                "CTFs are competitive, gamified cybersecurity labs.",
                "They provide desperately needed hands-on practical experience.",
                "TryHackMe and HackTheBox are the premier platforms for practice."
            ),
            iconRes = "🎯",
            estimatedMinutes = 8,
            xpReward = 80,
            orderIndex = 30
        )
    )
}
