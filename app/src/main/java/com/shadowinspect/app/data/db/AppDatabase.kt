package com.shadowinspect.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.shadowinspect.app.data.auth.AgentDao
import com.shadowinspect.app.data.auth.AgentEntity
import com.shadowinspect.app.data.db.converter.AppConverters
import com.shadowinspect.app.domain.education.Badge
import com.shadowinspect.app.domain.education.DailyTip
import com.shadowinspect.app.domain.education.SecurityLesson
import com.shadowinspect.app.domain.education.UserBadge
import com.shadowinspect.app.domain.education.UserProgress
import com.shadowinspect.app.domain.model.DocumentAnalysisResult
import com.shadowinspect.app.domain.model.ImageAnalysisResult
import androidx.room.TypeConverters

@Database(
    entities = [
        AgentEntity::class,
        ScanEntity::class,
        PhoneReportEntity::class,
        com.shadowinspect.app.domain.mitre.MitreDetection::class,
        com.shadowinspect.app.domain.mitre.MitreReport::class,
        com.shadowinspect.app.domain.research.ResearchDataPoint::class,
        com.shadowinspect.app.domain.research.UserFeedback::class,
        com.shadowinspect.app.domain.score.HistoricalScore::class,
        com.shadowinspect.app.domain.widgets.WidgetConfig::class,
        DocumentAnalysisResult::class,
        ImageAnalysisResult::class,
        SecurityLesson::class,    // Module 15 (Education)
        DailyTip::class,          // Module 15 (Education)
        UserProgress::class,      // Module 15 (Education)
        Badge::class,             // Module 15 (Education)
        UserBadge::class,         // Module 15 (Education)
        com.shadowinspect.app.domain.education.LearningPath::class,  // Module 15 Expansion
        com.shadowinspect.app.domain.education.Quiz::class,
        com.shadowinspect.app.domain.education.QuizQuestion::class,
        com.shadowinspect.app.domain.education.QuizAttempt::class,
        com.shadowinspect.app.domain.education.GlossaryTerm::class
    ],
    version = 22,
    exportSchema = false
)
@TypeConverters(AppConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun agentDao(): AgentDao
    abstract fun scanDao(): ScanDao
    abstract fun phoneReportDao(): PhoneReportDao
    abstract fun mitreDetectionDao(): MitreDetectionDao
    abstract fun mitreReportDao(): MitreReportDao
    abstract fun researchDataDao(): ResearchDataDao
    abstract fun userFeedbackDao(): UserFeedbackDao
    abstract fun scoreDao(): ScoreDao
    abstract fun widgetConfigDao(): WidgetConfigDao
    abstract fun documentDao(): DocumentDao
    abstract fun imageDao(): ImageDao
    abstract fun lessonDao(): LessonDao           // Module 15
    abstract fun dailyTipDao(): DailyTipDao       // Module 15
    abstract fun badgeDao(): BadgeDao             // Module 15
    abstract fun userProgressDao(): UserProgressDao // Module 15
    abstract fun learningPathDao(): LearningPathDao // Module 15 Expansion

    companion object {
        val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Ensure MITRE tables exist if not already created by destructive migration previously
                db.execSQL("CREATE TABLE IF NOT EXISTS `mitre_detections` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `scanId` INTEGER NOT NULL, `scanType` TEXT NOT NULL, `techniqueId` TEXT NOT NULL, `techniqueName` TEXT NOT NULL, `tactic` TEXT NOT NULL, `confidenceScore` INTEGER NOT NULL, `evidence` TEXT NOT NULL, `detectedAt` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `mitre_reports` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `scanId` INTEGER NOT NULL, `scanTarget` TEXT NOT NULL, `scanType` TEXT NOT NULL, `reportTitle` TEXT NOT NULL, `reportSummary` TEXT NOT NULL, `overallRiskScore` INTEGER NOT NULL, `overallRiskLevel` TEXT NOT NULL, `detectedTechniques` TEXT NOT NULL, `techniqueCount` INTEGER NOT NULL, `tacticCount` INTEGER NOT NULL, `riskFactors` TEXT NOT NULL, `recommendations` TEXT NOT NULL, `predictedThreats` TEXT NOT NULL, `confidenceLevel` REAL NOT NULL, `generatedAt` INTEGER NOT NULL, `reportFormat` TEXT NOT NULL, `isExported` INTEGER NOT NULL, `exportPath` TEXT)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `research_detections` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `researchId` TEXT NOT NULL, `techniqueId` TEXT NOT NULL, `tactic` TEXT NOT NULL, `confidenceScore` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `appCategory` TEXT, `riskLevel` TEXT NOT NULL, `osVersion` INTEGER NOT NULL)")
            }
        }

        val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Add indexes for performance
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mitre_detections_scanId` ON `mitre_detections` (`scanId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mitre_detections_techniqueId` ON `mitre_detections` (`techniqueId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mitre_detections_detectedAt` ON `mitre_detections` (`detectedAt`)")
            }
        }

        val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `research_data` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `scanType` TEXT NOT NULL, `riskScore` INTEGER NOT NULL, `riskLevel` TEXT NOT NULL, `techniqueCount` INTEGER NOT NULL, `detectedTechniques` TEXT NOT NULL, `detectionAccuracy` INTEGER, `falsePositive` INTEGER, `timestamp` INTEGER NOT NULL, `androidVersion` INTEGER NOT NULL, `deviceModel` TEXT NOT NULL, `appVersion` TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `user_feedback` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `scanId` INTEGER NOT NULL, `wasAccurate` INTEGER NOT NULL, `comments` TEXT, `timestamp` INTEGER NOT NULL)")
            }
        }

        val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `research_data` ADD COLUMN `scanId` INTEGER")
            }
        }

        val MIGRATION_12_13 = object : androidx.room.migration.Migration(12, 13) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `research_data` ADD COLUMN `appCategory` TEXT NOT NULL DEFAULT 'Unknown'")
                db.execSQL("ALTER TABLE `research_data` ADD COLUMN `detectedTactics` TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE `research_data` ADD COLUMN `dangerousPermissionCount` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `research_data` ADD COLUMN `dangerousPermissions` TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE `research_data` ADD COLUMN `isDebuggable` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `research_data` ADD COLUMN `virusTotalVerdict` TEXT NOT NULL DEFAULT 'Not Checked'")
            }
        }

        val MIGRATION_13_14 = object : androidx.room.migration.Migration(13, 14) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `score_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `overallScore` INTEGER NOT NULL, `appScore` INTEGER NOT NULL, `urlScore` INTEGER NOT NULL, `phoneScore` INTEGER NOT NULL, `systemScore` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `note` TEXT)")
            }
        }

        val MIGRATION_14_15 = object : androidx.room.migration.Migration(14, 15) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `widget_configs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `widgetType` TEXT NOT NULL, `title` TEXT NOT NULL, `size` TEXT NOT NULL, `row` INTEGER NOT NULL, `col` INTEGER NOT NULL, `rowSpan` INTEGER NOT NULL, `colSpan` INTEGER NOT NULL, `isVisible` INTEGER NOT NULL, `refreshInterval` INTEGER NOT NULL, `customSettings` TEXT, `colorTheme` TEXT, `lastUpdated` INTEGER NOT NULL)")
            }
        }
        val MIGRATION_15_16 = object : androidx.room.migration.Migration(15, 16) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `document_scans` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `fileName` TEXT NOT NULL,
                        `fileSize` INTEGER NOT NULL,
                        `fileType` TEXT NOT NULL,
                        `riskScore` INTEGER NOT NULL,
                        `riskLevel` TEXT NOT NULL,
                        `metadata` TEXT NOT NULL,
                        `embeddedUrls` TEXT NOT NULL,
                        `suspiciousPatterns` TEXT NOT NULL,
                        `hasMacros` INTEGER NOT NULL,
                        `macrosCount` INTEGER NOT NULL,
                        `summary` TEXT NOT NULL,
                        `recommendations` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `isError` INTEGER NOT NULL,
                        `errorMessage` TEXT
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_document_timestamp ON document_scans(timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_document_risk ON document_scans(riskLevel)")
            }
        }

        val MIGRATION_16_17 = object : androidx.room.migration.Migration(16, 17) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `document_scans`")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `document_scans` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `fileName` TEXT NOT NULL,
                        `filePath` TEXT NOT NULL,
                        `fileSize` INTEGER NOT NULL,
                        `documentType` TEXT NOT NULL,
                        `mimeType` TEXT NOT NULL,
                        `hashSha256` TEXT NOT NULL,
                        `hashMd5` TEXT NOT NULL,
                        `pageCount` INTEGER,
                        `wordCount` INTEGER,
                        `author` TEXT,
                        `creator` TEXT,
                        `producer` TEXT,
                        `creationDate` INTEGER,
                        `modificationDate` INTEGER,
                        `hasMacros` INTEGER NOT NULL,
                        `macroCount` INTEGER NOT NULL,
                        `maliciousMacros` TEXT NOT NULL,
                        `embeddedUrls` TEXT NOT NULL,
                        `suspiciousUrls` TEXT NOT NULL,
                        `totalUrls` INTEGER NOT NULL,
                        `embeddedScripts` TEXT NOT NULL,
                        `suspiciousScripts` TEXT NOT NULL,
                        `containsSensitiveData` INTEGER NOT NULL,
                        `sensitiveDataTypes` TEXT NOT NULL,
                        `isEncrypted` INTEGER NOT NULL,
                        `hasExternalReferences` INTEGER NOT NULL,
                        `externalReferences` TEXT NOT NULL,
                        `riskScore` INTEGER NOT NULL,
                        `riskLevel` TEXT NOT NULL,
                        `threatCategories` TEXT NOT NULL,
                        `mitreTechniques` TEXT NOT NULL,
                        `explanation` TEXT NOT NULL,
                        `recommendations` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `isError` INTEGER NOT NULL,
                        `errorMessage` TEXT
                    )
                """)
            }
        }

        val MIGRATION_17_18 = object : androidx.room.migration.Migration(17, 18) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `image_scans` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `fileName` TEXT NOT NULL,
                        `filePath` TEXT NOT NULL,
                        `fileSize` INTEGER NOT NULL,
                        `imageType` TEXT NOT NULL,
                        `mimeType` TEXT NOT NULL,
                        `hashSha256` TEXT NOT NULL,
                        `hashMd5` TEXT NOT NULL,
                        `imageWidth` INTEGER NOT NULL,
                        `imageHeight` INTEGER NOT NULL,
                        `bitDepth` INTEGER NOT NULL,
                        `hasAlpha` INTEGER NOT NULL,
                        `exifMetadata` TEXT,
                        `hasExifData` INTEGER NOT NULL,
                        `steganographyResult` TEXT,
                        `hasHiddenData` INTEGER NOT NULL,
                        `embeddedUrls` TEXT NOT NULL,
                        `suspiciousUrls` TEXT NOT NULL,
                        `containsGpsLocation` INTEGER NOT NULL,
                        `gpsCoordinates` TEXT,
                        `isManipulated` INTEGER NOT NULL,
                        `manipulationConfidence` INTEGER NOT NULL,
                        `riskScore` INTEGER NOT NULL,
                        `riskLevel` TEXT NOT NULL,
                        `threatCategories` TEXT NOT NULL,
                        `mitreTechniques` TEXT NOT NULL,
                        `explanation` TEXT NOT NULL,
                        `recommendations` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `isError` INTEGER NOT NULL,
                        `errorMessage` TEXT
                    )
                """)
            }
        }

        val MIGRATION_18_19 = object : androidx.room.migration.Migration(18, 19) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Module 15: Education tables
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `lessons` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `difficulty` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `summaryPoints` TEXT NOT NULL,
                        `iconRes` TEXT NOT NULL,
                        `estimatedMinutes` INTEGER NOT NULL,
                        `xpReward` INTEGER NOT NULL,
                        `isLocked` INTEGER NOT NULL,
                        `orderIndex` INTEGER NOT NULL,
                        `quizId` INTEGER,
                        `mitreTechniques` TEXT NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `daily_tips` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `iconRes` TEXT NOT NULL,
                        `dateAdded` INTEGER NOT NULL,
                        `timesShown` INTEGER NOT NULL,
                        `likes` INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `user_progress` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `completedLessons` TEXT NOT NULL,
                        `quizScores` TEXT NOT NULL,
                        `totalXp` INTEGER NOT NULL,
                        `currentStreak` INTEGER NOT NULL,
                        `longestStreak` INTEGER NOT NULL,
                        `lastActiveDate` INTEGER NOT NULL,
                        `badges` TEXT NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `badges` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `iconRes` TEXT NOT NULL,
                        `requirement` TEXT NOT NULL,
                        `xpReward` INTEGER NOT NULL,
                        `isSecret` INTEGER NOT NULL,
                        `orderIndex` INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `user_badges` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `badgeId` INTEGER NOT NULL,
                        `earnedDate` INTEGER NOT NULL,
                        `isNew` INTEGER NOT NULL
                    )
                """)
            }
        }

        val MIGRATION_19_20 = object : androidx.room.migration.Migration(19, 20) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Add level column to lessons table
                db.execSQL("ALTER TABLE `lessons` ADD COLUMN `level` TEXT NOT NULL DEFAULT 'BEGINNER'")
                
                // Add columns to user_progress
                db.execSQL("ALTER TABLE `user_progress` ADD COLUMN `completedPaths` TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE `user_progress` ADD COLUMN `currentPathId` INTEGER")
                db.execSQL("ALTER TABLE `user_progress` ADD COLUMN `learningTimeMinutes` INTEGER NOT NULL DEFAULT 0")

                // Ensure quizScores uses updated JSON string (existing JSON string, so no schema change needed, but let's be safe)
                // AppConverters handles Map<Long, Int> now, but previous was String "{}". Data preservation should be OK if it was JSON string.

                // Create new tables for Learning Paths
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `learning_paths` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL DEFAULT '',
                        `name` TEXT NOT NULL DEFAULT '',
                        `description` TEXT NOT NULL,
                        `level` TEXT NOT NULL,
                        `iconRes` TEXT NOT NULL,
                        `estimatedHours` INTEGER NOT NULL DEFAULT 0,
                        `lessonIds` TEXT NOT NULL DEFAULT '[]',
                        `prerequisitePathId` INTEGER,
                        `xpReward` INTEGER NOT NULL DEFAULT 0,
                        `badgeReward` TEXT,
                        `requiredXp` INTEGER NOT NULL DEFAULT 0,
                        `orderIndex` INTEGER NOT NULL DEFAULT 0
                    )
                """)

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `quizzes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `lessonId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `passingScore` INTEGER NOT NULL,
                        `timeLimitSeconds` INTEGER,
                        `xpReward` INTEGER NOT NULL
                    )
                """)

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `quiz_questions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `quizId` INTEGER NOT NULL,
                        `question` TEXT NOT NULL,
                        `options` TEXT NOT NULL,
                        `correctOptionIndex` INTEGER NOT NULL,
                        `explanation` TEXT NOT NULL,
                        `points` INTEGER NOT NULL,
                        `imageUrl` TEXT,
                        `hint` TEXT
                    )
                """)

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `quiz_attempts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `quizId` INTEGER NOT NULL,
                        `score` INTEGER NOT NULL,
                        `passed` INTEGER NOT NULL,
                        `answers` TEXT NOT NULL,
                        `timeSpentSeconds` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                """)

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `glossary` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `term` TEXT NOT NULL,
                        `definition` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `relatedTerms` TEXT NOT NULL,
                        `mitreTechnique` TEXT
                    )
                """)
            }
        }
    }
}
