package com.shadowinspect.app.domain.education

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Learning levels
 */
enum class LearningLevel {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
    EXPERT
}

/**
 * Lesson categories (expanded)
 */
enum class LessonCategory {
    // Beginner
    MALWARE_TYPES,
    PHISHING,
    PERMISSIONS,
    NETWORK_SECURITY,
    PRIVACY,
    SAFE_BROWSING,
    PASSWORD_SECURITY,
    TWO_FACTOR_AUTH,
    SOCIAL_ENGINEERING,
    MITRE_101,
    
    // Intermediate
    MALWARE_ANALYSIS,
    ADVANCED_PHISHING,
    ANDROID_SECURITY,
    MITRE_MATRIX,
    ENTERPRISE_SECURITY,
    OSINT,
    WIFI_SECURITY,
    PAYMENT_SECURITY,
    EMAIL_SECURITY,
    DATA_DESTRUCTION,
    
    // Advanced
    REVERSE_ENGINEERING,
    CRYPTOGRAPHY,
    DARK_WEB,
    WEB_APP_SECURITY,
    MOBILE_MALWARE_DEEP,
    ZERO_DAY,
    CORPORATE_SECURITY,
    CYBER_LAW,
    FORENSICS,
    CTF_CHALLENGES,
    
    // Expert
    AI_CYBERSECURITY,
    THREAT_INTEL,
    APT,
    RANSOMWARE_STRATEGIES,
    BLUE_TEAM,
    RED_TEAM,
    SECURITY_ARCHITECTURE,
    BLOCKCHAIN_SECURITY,
    IOT_SECURITY,
    CAREER_PATHS
}

/**
 * Difficulty levels
 */
enum class LessonDifficulty {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED
}

/**
 * Security lesson
 */
@Entity(tableName = "lessons")
data class SecurityLesson(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,
    val description: String,
    val category: LessonCategory,
    val difficulty: LessonDifficulty,
    val level: LearningLevel = LearningLevel.BEGINNER,
    val content: String,              // Markdown content
    val summaryPoints: List<String>,  // Key takeaways
    val iconRes: String,              // Emoji or icon name
    val estimatedMinutes: Int,
    val xpReward: Int,
    val isLocked: Boolean = false,
    val orderIndex: Int,
    val quizId: Long? = null,
    val mitreTechniques: List<String> = emptyList()
)

/**
 * Learning path (collection of lessons)
 */
@Entity(tableName = "learning_paths")
data class LearningPath(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,
    val name: String = "",  // alias kept for compat
    val description: String,
    val level: LearningLevel,
    val iconRes: String,
    val estimatedHours: Int = 0,
    val lessonIds: List<Long> = emptyList(),
    val prerequisitePathId: Long? = null,
    val xpReward: Int = 0,
    val badgeReward: String? = null,
    val requiredXp: Int = 0,       // XP needed to unlock this path
    val orderIndex: Int = 0        // Display order
)

/**
 * Quiz (for each lesson)
 */
@Entity(tableName = "quizzes")
data class Quiz(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val lessonId: Long,
    val title: String,
    val passingScore: Int,
    val timeLimitSeconds: Int? = null,
    val xpReward: Int
)

/**
 * Quiz question (enhanced)
 */
@Entity(tableName = "quiz_questions")
data class QuizQuestion(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val quizId: Long,
    val question: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val explanation: String,
    val points: Int,
    val imageUrl: String? = null,
    val hint: String? = null
)

/**
 * User quiz attempt
 */
@Entity(tableName = "quiz_attempts")
data class QuizAttempt(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val quizId: Long,
    val score: Int,
    val passed: Boolean,
    val answers: List<Int>,
    val timeSpentSeconds: Int,
    val timestamp: Long
)

/**
 * Daily security tip
 */
@Entity(tableName = "daily_tips")
data class DailyTip(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,
    val content: String,
    val category: LessonCategory,
    val iconRes: String,
    val dateAdded: Long,
    val timesShown: Int = 0,
    val likes: Int = 0
)

/**
 * User progress (enhanced)
 */
@Entity(tableName = "user_progress")
data class UserProgress(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val completedLessons: List<Long> = emptyList(),
    val quizScores: Map<Long, Int> = emptyMap(),
    val completedPaths: List<Long> = emptyList(),
    val currentPathId: Long? = null,
    val totalXp: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastActiveDate: Long = System.currentTimeMillis(),
    val badges: List<String> = emptyList(),
    val learningTimeMinutes: Int = 0
)

/**
 * Glossary term
 */
@Entity(tableName = "glossary")
data class GlossaryTerm(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val term: String,
    val definition: String,
    val category: String,
    val relatedTerms: List<String> = emptyList(),
    val mitreTechnique: String? = null
)

/**
 * Security certification path
 */
data class CertPath(
    val name: String,
    val description: String,
    val requiredLessons: List<Long>,
    val practiceQuizzes: List<Long>,
    val externalResources: List<String>,
    val estimatedMonths: Int
)

/**
 * Achievement badge
 */
@Entity(tableName = "badges")
data class Badge(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,
    val description: String,
    val iconRes: String,
    val requirement: String,
    val xpReward: Int,
    val isSecret: Boolean = false,
    val orderIndex: Int
)

/**
 * User badge (earned)
 */
@Entity(tableName = "user_badges")
data class UserBadge(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val badgeId: Long,
    val earnedDate: Long,
    val isNew: Boolean = true
)

/**
 * MITRE technique explained (for education)
 */
data class MitreExplained(
    val techniqueId: String,
    val techniqueName: String,
    val simpleExplanation: String,
    val realWorldExample: String,
    val howToProtect: String,
    val relatedApps: List<String>? = null,
    val severityLevel: String
)
