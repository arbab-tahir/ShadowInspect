package com.shadowinspect.app.data.seed

import com.shadowinspect.app.domain.education.LearningLevel
import com.shadowinspect.app.domain.education.LearningPath

object LearningPathsSeed {
    val data = listOf(
        LearningPath(
            id = 1L,
            title = "Beginner: Security Foundations",
            description = "Learn the basics of digital hygiene, spotting scams, and protecting your personal devices.",
            level = LearningLevel.BEGINNER,
            iconRes = "🌱",
            orderIndex = 1,
            requiredXp = 0
        ),
        LearningPath(
            id = 2L,
            title = "Intermediate: The Hacker's Playbook",
            description = "Understand how malware works, dissect advanced phishing, and protect enterprise networks.",
            level = LearningLevel.INTERMEDIATE,
            iconRes = "🛡️",
            orderIndex = 2,
            requiredXp = 500 // Require 500 XP from beginner level
        ),
        LearningPath(
            id = 3L,
            title = "Advanced: System Architect & Analysis",
            description = "Dive into reverse engineering, cryptography, web vulnerabilities, and zero-day exploits.",
            level = LearningLevel.ADVANCED,
            iconRes = "⚙️",
            orderIndex = 3,
            requiredXp = 1500
        ),
        LearningPath(
            id = 4L,
            title = "Expert: Cyber Threat Intelligence",
            description = "Master AI in cyber, threat hunting, ransomware cartels, SOC operations, and red team strategies.",
            level = LearningLevel.EXPERT,
            iconRes = "🏴☠️",
            orderIndex = 4,
            requiredXp = 3000
        )
    )
}
