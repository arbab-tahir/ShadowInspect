package com.shadowinspect.app.data.seed

import com.shadowinspect.app.domain.education.LearningLevel
import com.shadowinspect.app.domain.education.LessonCategory
import com.shadowinspect.app.domain.education.LessonDifficulty
import com.shadowinspect.app.domain.education.SecurityLesson

object ExpertLessons {
    val data = listOf(
        SecurityLesson(
            id = 31,
            title = "🤖 AI in Cybersecurity",
            description = "Offensive and defensive applications of Large Language Models and ML",
            category = LessonCategory.AI_CYBERSECURITY,
            difficulty = LessonDifficulty.ADVANCED,
            level = LearningLevel.EXPERT,
            content = """
# Artificial Intelligence in Cyber

Machine Learning (ML) and Large Language Models (LLMs) are radically altering the cybersecurity landscape.

## Defensive AI
Security Operations Centers (SOCs) are drowning in alerts. ML models analyze billions of logs to spot deviations from "normal" human behavior (User and Entity Behavior Analytics - UEBA). LLMs are now used to summarize complex incidents into plain English for analysts instantly.

## Offensive AI
Attackers are leveraging AI to scale operations:
- **Polymorphic Malware:** Using LLMs to constantly rewrite malware source code, evading static signature detection permanently.
- **Deepfake Phishing:** Generating convincing voice clones of executives approving wire transfers.
- **Automated Recon:** Using AI to scrape GitHub and LinkedIn to formulate hyper-personalized, flawless spear-phishing emails at scale.

## Hacking the AI (Adversarial ML)
- **Prompt Injection:** Tricking an LLM chatbot deployed on a corporate website into executing unauthorized commands.
- **Data Poisoning:** Attackers silently corrupting the training data model of an antivirus company so that it classifies a specific malware family as "benign."
            """.trimIndent(),
            summaryPoints = listOf(
                "AI enables automated SOC log analysis and behavioral anomaly detection.",
                "Attackers use AI for deepfakes, flawless phishing, and polymorphic code.",
                "Adversarial ML involves poisoning training data or prompt-injecting LLMs."
            ),
            iconRes = "🤖",
            estimatedMinutes = 10,
            xpReward = 100,
            orderIndex = 31
        ),
        SecurityLesson(
            id = 32,
            title = "🔮 Threat Intelligence & Hunting",
            description = "Proactively searching for undetected adversaries",
            category = LessonCategory.THREAT_INTEL,
            difficulty = LessonDifficulty.ADVANCED,
            level = LearningLevel.EXPERT,
            content = """
# Cyber Threat Intelligence (CTI) & Hunting

Most security reacts to alerts. Threat Hunting assumes the network is already compromised and no alerts fired.

## Cyber Threat Intelligence
CTI is not just a feed of bad IP addresses (Indicators of Compromise - IoCs). Strategic CTI tracks attacker campaigns. It analyzes the *infrastructure* (TTPs - Tactics, Techniques, and Procedures) of Advanced Persistent Threats (APTs).
If a CTI team observes a state actor targeting aerospace companies using a specific zero-day in MS Exchange, they warn their own aerospace organization to hunt for that zero-day immediately.

## Hypothesis-Driven Hunting
A Threat Hunter forms a hypothesis: *"Given our industry, APT29 might have bypassed our email filter using HTML smuggling."*
The hunter then dives into the raw SIEM logs, bypassing automated alerts, manually querying for large HTML files dropping ZIP payloads, anomalies in parent-child process trees, and unusual beaconing traffic.

## The Pyramid of Pain
When hunting, focusing on IP addresses is trivial—attackers change IPs instantly. Hunting for TTPs (how the attacker fundamentally operates) causes them the most "pain" because re-engineering their entire attack strategy takes months.
            """.trimIndent(),
            summaryPoints = listOf(
                "Threat hunting is proactive, assuming defenses have already failed.",
                "CTI shifts focus from blocking signatures to understanding adversary behavior.",
                "Tracking TTPs is highly effective on the Pyramid of Pain."
            ),
            iconRes = "🔮",
            estimatedMinutes = 12,
            xpReward = 120,
            orderIndex = 32
        ),
        SecurityLesson(
            id = 33,
            title = "🏴☠️ Advanced Persistent Threats (APT)",
            description = "Analyzing the tactics of nation-state hacking groups",
            category = LessonCategory.APT,
            difficulty = LessonDifficulty.ADVANCED,
            level = LearningLevel.EXPERT,
            content = """
# Advanced Persistent Threats

APTs are incredibly sophisticated, heavily resourced, highly patient hacking groups, almost exclusively backed by nation-state intelligence agencies.

## The Goal is Persistence
Unlike ransomware gangs who smash-and-grab for quick cash, an APT might breach a nuclear power plant and sit silently inside the network for three years, mapping out every system and exfiltrating engineering schematics byte-by-byte.

## APT Supply Chain Attacks 
(e.g., SolarWinds)
Instead of hacking the US Government directly, APT29 hacked SolarWinds, a company that makes IT monitoring software used by the government. They silently slipped a backdoor into a routine software update. When SolarWinds signed the update and shipped it to its customers, thousands of high-level networks were compromised simultaneously.

## "Living off the Land" (LotL)
To avoid detection by antivirus, APTs rarely bring their own custom malware into the environment. Instead, they use built-in administrative tools already on the computer (PowerShell, WMI, Windows Scheduled Tasks) to move laterally. Since these are legitimate tools, security software usually ignores them.
            """.trimIndent(),
            summaryPoints = listOf(
                "APTs prioritize long-term, stealthy espionage over short-term financial gain.",
                "Supply Chain attacks compromise trusted vendors to breach devastatingly secure targets.",
                "'Living off the Land' involves using native operating system tools to mask malicious intent."
            ),
            iconRes = "🏴☠️",
            estimatedMinutes = 15,
            xpReward = 150,
            orderIndex = 33
        ),
        SecurityLesson(
            id = 34,
            title = "💣 Ransomware Strategies",
            description = "Ransomware-as-a-Service and double extortion models",
            category = LessonCategory.RANSOMWARE_STRATEGIES,
            difficulty = LessonDifficulty.ADVANCED,
            level = LearningLevel.EXPERT,
            content = """
# The Economics of Ransomware

Ransomware is no longer written by a lone hacker; it is a multi-billion dollar organized corporate enterprise.

## Ransomware-as-a-Service (RaaS)
Cartels like LockBit or ALPHV operate like SaaS companies. The core cartel writes the devastating encryption software and runs the dark web payment portals. They "lease" the software to "Affiliates" (freelance hackers). 
The affiliate breaches the network and deploys the software. The cartel takes a 20% cut of the million-dollar ransom, and the affiliate keeps 80%.

## Double & Triple Extortion
Initially, ransomware just encrypted files. Companies fought back by using robust backups. 
**Double Extortion:** Attackers now steal 5 terabytes of sensitive data (HR records, customer data) BEFORE encrypting. If you refuse to pay (because you have backups), they threaten to leak everything to the public, incurring massive GDPR fines and lawsuits.
**Triple Extortion:** They also launch a massive DDoS attack against the company's website to paralyze them further during the negotiation window.

## Initial Access Brokers (IAB)
The ransomware hackers often aren't the ones who breached the network. IABs specialize purely in breaching networks. They establish a backdoor, then sell that access on the dark web for ${'$'}5,000 to ransomware affiliates.
            """.trimIndent(),
            summaryPoints = listOf(
                "Ransomware operates as franchise corporate enterprises (RaaS).",
                "Double extortion neutralizes backups by threatening to leak sensitive data.",
                "Initial Access Brokers commoditize network breaches on the dark web."
            ),
            iconRes = "💣",
            estimatedMinutes = 12,
            xpReward = 120,
            orderIndex = 34
        ),
        SecurityLesson(
            id = 35,
            title = "🛡️ Blue Team Operations",
            description = "Deep dive into Security Operations Centers and Incident Response",
            category = LessonCategory.BLUE_TEAM,
            difficulty = LessonDifficulty.ADVANCED,
            level = LearningLevel.EXPERT,
            content = """
# Blue Team: Defending the Castle

The Blue Team comprises the analysts and engineers actively defending the organization.

## The Security Operations Center (SOC)
A 24/7 watch floor. Tier 1 analysts monitor an endless stream of alerts generated by the SIEM. If an alert looks like true malicious activity, it is escalated to Tier 2 incident responders.

## Alert Fatigue
The greatest threat to a SOC. If automated tools generate 5,000 overly sensitive alerts a day, analysts stop paying close attention. A critical alert from a real breach will be buried in the noise and ignored. Fine-tuning detection rules is a daily struggle.

## The Incident Response (IR) Lifecycle
When a major breach occurs:
1. **Preparation:** Having runbooks ready BEFORE the attack.
2. **Identification:** Confirming a breach is active (Patient Zero).
3. **Containment:** Unplugging servers, isolating network segments, cutting internet access to stop the adversary from moving.
4. **Eradication:** Deleting the malware, closing the vulnerabilities.
5. **Recovery:** Restoring from backups, securely bringing services back online.
6. **Lessons Learned:** Fixing the architectural flaws that allowed the breach.
            """.trimIndent(),
            summaryPoints = listOf(
                "The SOC provides 24/7 continuous monitoring and triage of security alerts.",
                "Alert fatigue causes critical security events to be ignored by overwhelmed analysts.",
                "Incident Response follows a strict lifecycle from Containment to Recovery."
            ),
            iconRes = "🛡️",
            estimatedMinutes = 12,
            xpReward = 120,
            orderIndex = 35
        ),
        SecurityLesson(
            id = 36,
            title = "⚔️ Red Team Techniques",
            description = "Simulating full-scale adversary attacks to test Blue Teams",
            category = LessonCategory.RED_TEAM,
            difficulty = LessonDifficulty.ADVANCED,
            level = LearningLevel.EXPERT,
            content = """
# Red Teaming: The Ethical Adversary

Unlike a standard vulnerability scan, a Red Team does not care about finding every missing patch. A Red Team's goal is to simulate a full-scale APT attack to completely bypass and stress-test the Blue Team.

## Objectives-Based Hacking
The Red Team is given a goal: "Extract the secret formula from the R&D server without the SOC noticing." They might spend 3 weeks doing OSINT, writing custom undetectable malware, and executing a highly targeted spear-phishing campaign against a single HR manager to get a foothold.

## Purple Teaming
The modern evolution. Historically, Red and Blue teams were adversarial and secretive. In Purple Teaming, the Red hacker sits down next to the Blue defender. Red executes a technique (e.g., dumping LSASS memory), and they both immediately look at the SIEM logs to see if it triggered an alert, tuning the detection rule on the spot.

## Physical Penetration Testing
Digital boundaries are only part of the equation. Red teams will clone RFID badges, tailgate employees through security doors, pick locks, and plug rogue Raspberry Pi devices secretly into office ethernet jacks to bypass firewalls entirely.
            """.trimIndent(),
            summaryPoints = listOf(
                "Red Teaming simulates targeted, quiet APT attacks rather than broad scanning.",
                "Purple Teaming maximizes ROI by combining offensive and defensive efforts in real-time.",
                "Physical security breaches render digital firewalls completely irrelevant."
            ),
            iconRes = "⚔️",
            estimatedMinutes = 10,
            xpReward = 100,
            orderIndex = 36
        ),
        SecurityLesson(
            id = 37,
            title = "📈 Security Architecture",
            description = "Cloud architecture, Kubernetes security, and secure design patterns",
            category = LessonCategory.SECURITY_ARCHITECTURE,
            difficulty = LessonDifficulty.ADVANCED,
            level = LearningLevel.EXPERT,
            content = """
# Advanced Security Architecture

Designing secure infrastructure in the modern era of Cloud and Containers.

## Secure Cloud Architecture (AWS/GCP/Azure)
The greatest risk in the cloud is not hardware failure, but misconfiguration.
An architect must ensure:
- **IAM Policies:** Extremely strict, utilizing assumed roles instead of long-lived access keys.
- **Security Groups:** State-ful cloud firewalls allowing only specific IP ranges.
- **VPC Subnets:** Public subnets for Application Load Balancers, Private subnets (no internet route) for databases.

## Kubernetes and Container Security
Docker and Kubernetes have revolutionized deployment, but introduce massive attack surfaces.
- **Capabilities:** Stripping Linux kernel capabilities from containers using Seccomp and AppArmor profiles so that a breached container cannot compromise the underlying host node.
- **Network Policies:** By default, all pods in a Kubernetes cluster can talk to each other. Architects must enforce strict zero-trust network policies within the cluster.

## Shift-Left Paradigm
Integrating security scanners directly into the developer's CI/CD pipeline (Continuous Integration). If a developer writes insecure code with hardcoded passwords, the build instantly fails before it is ever merged or deployed. Security happens "Shifted Left" in the timeline.
            """.trimIndent(),
            summaryPoints = listOf(
                "Cloud breaches are overwhelmingly caused by IAM misconfigurations, not platform vulnerabilities.",
                "Container security requires stripping kernel capabilities to prevent host compromise.",
                "Shift-Left integrates security directly into the development pipeline seamlessly."
            ),
            iconRes = "📈",
            estimatedMinutes = 15,
            xpReward = 150,
            orderIndex = 37
        ),
        SecurityLesson(
            id = 38,
            title = "🔐 Blockchain Security",
            description = "Smart contract vulnerabilities and 51% attacks",
            category = LessonCategory.BLOCKCHAIN_SECURITY,
            difficulty = LessonDifficulty.ADVANCED,
            level = LearningLevel.EXPERT,
            content = """
# Web3 and Blockchain Security

The decentralized nature of blockchains means if code is exploited, there is no central authority to reverse the transaction. Code is law.

## Smart Contract Vulnerabilities

### Reentrancy Attacks
The vulnerability that destroyed the DAO in 2016 for ${'$'}50M. A malicious contract repeatedly calls a vulnerable contract's "withdraw" function before the vulnerable contract has a chance to update its internal balance ledger, draining all the funds in a recursive loop.

### Flash Loan Attacks
An attacker takes out a massive, uncollateralized loan (hundreds of millions of dollars) within a single transaction block. They use that massive capital to manipulate prices on decentralized exchanges (DeFi), profit from the arbitrage, and pay back the loan instantly within the same block.

## 51% Attacks
If an adversary controls more than 50% of the computing power (hashrate) on a Proof of Work blockchain, they can unilaterally rewrite the ledger, double-spend coins, and block other miners' transactions. This is incredibly expensive to execute on Bitcoin, but frequently occurs on smaller alt-coins.
            """.trimIndent(),
            summaryPoints = listOf(
                "Smart contract bugs are highly lucrative and often unpatchable post-deployment.",
                "Reentrancy attacks recursively drain funds by interrupting execution states.",
                "Flash loans allow attackers temporarily infinite capital to manipulate decentralized financial markets."
            ),
            iconRes = "🔐",
            estimatedMinutes = 12,
            xpReward = 120,
            orderIndex = 38
        ),
        SecurityLesson(
            id = 39,
            title = "📱 IoT Security",
            description = "Massive botnets and the risks of embedded systems",
            category = LessonCategory.IOT_SECURITY,
            difficulty = LessonDifficulty.ADVANCED,
            level = LearningLevel.EXPERT,
            content = """
# Internet of Things Security

Your smart fridge, your internet-connected doorbell, and industrial sensors. These are full Linux computers, and their security is historically atrocious.

## The Mirai Botnet
In 2016, the Mirai malware scanned the internet looking for hundreds of thousands of internet-connected security cameras using default factory credentials (admin/admin). It infected them and slaved them into a botnet. Mirai then launched a 600 Gbps DDoS attack that took down Twitter, Reddit, and Netflix.

## Hardware Constraints
IoT devices are cheap and have minimal CPU/RAM. They literally cannot run a modern EDR agent or process complex cryptography.

## Embedded Vulnerabilities
- Hardcoded firmware backdoor passwords that cannot be changed by the user.
- Unencrypted communication to sketchy overseas cloud servers.
- No ability to receive Over-The-Air (OTA) security updates, meaning vulnerabilities last until the hardware is thrown in the trash.

## OT Security (Operational Technology)
The most critical form of IoT. Stuxnet proved that hacking PLCs (Programmable Logic Controllers) can cause physical machinery to destroy itself (Iranian nuclear centrifuges). When OT is connected to the IT network without air-gapping, kinetic physical damage is just a firewall bypass away.
            """.trimIndent(),
            summaryPoints = listOf(
                "IoT devices frequently rely on hardcoded, unchangeable default credentials.",
                "Compromised IoT fleets form massive DDoS botnets, completely anonymously.",
                "OT cyber attacks can have devastating kinetic physical consequences."
            ),
            iconRes = "📱",
            estimatedMinutes = 10,
            xpReward = 100,
            orderIndex = 39
        ),
        SecurityLesson(
            id = 40,
            title = "🚀 Career Paths in Security",
            description = "Certifications, roles, and how to become a cybersecurity professional",
            category = LessonCategory.CAREER_PATHS,
            difficulty = LessonDifficulty.ADVANCED,
            level = LearningLevel.EXPERT,
            content = """
# Forging a Career in Cybersecurity

You've completed the Zero to Hero path. How do you make this your living?

## Core Domains

1. **Security Engineering:** Building secure networks, managing firewalls, writing secure code architectures.
2. **Security Operations (SOC):** The front lines. Analyzing alerts, fighting active incidents, threat hunting.
3. **Offensive Security (Pentesting):** Getting paid to legally break into companies.
4. **Governance, Risk & Compliance (GRC):** Writing policy, auditing against frameworks (SOC2, ISO 27001). Less technical, highly lucrative.

## Recommended Certifications

### Entry Level
- **CompTIA Security+:** The gold standard for HR filters. Broad conceptual knowledge.
- **eJPT:** Fantastic hands-on introduction to ethical hacking.

### Intermediate
- **CySA+ / BTL1:** Excellent practical training for Blue Team analysts.
- **OSCP (Offensive Security Certified Professional):** The holy grail for Pen Testers. A grueling 24-hour practical hacking exam.

### Advanced
- **CISSP:** A management certification requiring 5 years of experience. Incredible ROI for senior roles.
- **CISM / CISA:** For auditing and high-level risk management.

## Building Experience
Certifications don't guarantee a job; practical skill does.
- Compete in CTFs (HackTheBox, TryHackMe).
- Set up a home lab (Active Directory, Proxmox, PF_Sense, Splunk).
- Document your lab write-ups on a public blog or GitHub. Let employers see your passion.
            """.trimIndent(),
            summaryPoints = listOf(
                "Cybersecurity has vastly different roles, from highly technical reversing to policy-driven GRC.",
                "The OSCP and Security+ are landmark certifications for offensive and foundational roles.",
                "Practical experience demonstrated through a home lab or CTF write-ups is invaluable."
            ),
            iconRes = "🚀",
            estimatedMinutes = 15,
            xpReward = 150,
            orderIndex = 40
        )
    )
}
