package com.shadowinspect.app.domain.repository

import com.shadowinspect.app.data.db.BadgeDao
import com.shadowinspect.app.data.db.DailyTipDao
import com.shadowinspect.app.data.db.LearningPathDao
import com.shadowinspect.app.data.db.LessonDao
import com.shadowinspect.app.data.db.UserProgressDao
import com.shadowinspect.app.data.seed.*
import com.shadowinspect.app.domain.education.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EducationRepository @Inject constructor(
    private val lessonDao: LessonDao,
    private val tipDao: DailyTipDao,
    private val progressDao: UserProgressDao,
    private val badgeDao: BadgeDao,
    private val pathDao: LearningPathDao
) {

    fun getAllLessons(): Flow<List<SecurityLesson>> = lessonDao.getAllLessons()

    fun getLessonsByLevel(level: LearningLevel): Flow<List<SecurityLesson>> = lessonDao.getLessonsByLevel(level)

    fun getAllPaths(): Flow<List<LearningPath>> = pathDao.getAllPaths()

    fun getLessonsByCategory(category: LessonCategory): Flow<List<SecurityLesson>> =
        lessonDao.getLessonsByCategory(category)

    suspend fun getLesson(lessonId: Long): SecurityLesson? = lessonDao.getLesson(lessonId)

    suspend fun completeLesson(lessonId: Long, isLevelCompleted: Boolean = false): UserProgress {
        val progress = progressDao.getProgress() ?: UserProgress()
        if (progress.completedLessons.contains(lessonId)) return progress
        val lesson = lessonDao.getLesson(lessonId)
        val levelBonus = if (isLevelCompleted) 250 else 0
        val updatedProgress = progress.copy(
            completedLessons = progress.completedLessons + lessonId,
            totalXp = progress.totalXp + (lesson?.xpReward ?: 0) + levelBonus
        )
        progressDao.saveProgress(updatedProgress)
        checkBadgeUnlocks(updatedProgress)
        return updatedProgress
    }

    suspend fun getDailyTip(): DailyTip? = withContext(Dispatchers.IO) {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        var tip = tipDao.getTipForDate(today)
        if (tip == null) {
            tip = generateDailyTip()
            tipDao.insertTip(tip)
        }
        tip
    }

    private fun generateDailyTip(): DailyTip {
        val now = System.currentTimeMillis()
        val tips = listOf(
            DailyTip(title = "🔐 Enable 2FA Everywhere", content = "Two-factor authentication blocks 99.9% of automated attacks. Start with your email and banking apps.", category = LessonCategory.TWO_FACTOR_AUTH, iconRes = "🔐", dateAdded = now),
            DailyTip(title = "📱 Check App Permissions", content = "Does a flashlight app really need access to your contacts? Review permissions in Settings regularly.", category = LessonCategory.PERMISSIONS, iconRes = "📱", dateAdded = now),
            DailyTip(title = "🔍 Spot Phishing Emails", content = "Check sender email, hover over links, and look for urgency tactics. When in doubt, go directly to the website.", category = LessonCategory.PHISHING, iconRes = "🔍", dateAdded = now),
            DailyTip(title = "🔄 Update Your Apps", content = "Outdated apps have security holes. Enable auto-updates in Play Store.", category = LessonCategory.NETWORK_SECURITY, iconRes = "🔄", dateAdded = now),
            DailyTip(title = "📍 Limit Location Sharing", content = "Only give location to apps that truly need it (Maps, Uber). Others can wait.", category = LessonCategory.PRIVACY, iconRes = "📍", dateAdded = now),
            DailyTip(title = "🔑 Use a Password Manager", content = "Remembering 50+ strong passwords is impossible. Use Bitwarden or KeepassXC — they're free!", category = LessonCategory.PASSWORD_SECURITY, iconRes = "🔑", dateAdded = now),
            DailyTip(title = "📸 Cover Your Camera", content = "Physical camera covers prevent malware from spying on you. Cheap and effective!", category = LessonCategory.PRIVACY, iconRes = "📸", dateAdded = now),
            DailyTip(title = "🎭 Social Engineering", content = "Scammers create urgency. 'Your account will be closed!' — take a breath and verify independently.", category = LessonCategory.SOCIAL_ENGINEERING, iconRes = "🎭", dateAdded = now)
        )
        return tips.random()
    }

    suspend fun getUserProgress(): UserProgress = progressDao.getProgress() ?: UserProgress()

    fun getUserProgressFlow(): Flow<UserProgress> = progressDao.getProgressFlow().map { it ?: UserProgress() }

    fun getAllBadges(): Flow<List<Badge>> = badgeDao.getAllBadges()

    suspend fun getEarnedBadges(): List<UserBadge> = badgeDao.getEarnedBadges()

    private suspend fun checkBadgeUnlocks(progress: UserProgress) {
        val allBadges = badgeDao.getAllBadgesSync()
        allBadges.forEach { badge ->
            val alreadyEarned = badgeDao.isBadgeEarned(badge.id)
            if (!alreadyEarned && meetsRequirement(progress, badge)) {
                badgeDao.earnBadge(UserBadge(badgeId = badge.id, earnedDate = System.currentTimeMillis(), isNew = true))
                val updated = progress.copy(totalXp = progress.totalXp + badge.xpReward)
                progressDao.saveProgress(updated)
            }
        }
    }

    private fun meetsRequirement(progress: UserProgress, badge: Badge): Boolean {
        return when {
            badge.requirement.contains("lesson") -> {
                val required = badge.requirement.filter { it.isDigit() }.toIntOrNull() ?: 0
                progress.completedLessons.size >= required
            }
            badge.requirement.contains("XP") -> {
                val required = badge.requirement.filter { it.isDigit() }.toIntOrNull() ?: 0
                progress.totalXp >= required
            }
            badge.requirement.contains("streak") -> {
                val required = badge.requirement.filter { it.isDigit() }.toIntOrNull() ?: 0
                progress.currentStreak >= required
            }
            else -> false
        }
    }

    suspend fun initializeEducationData() {
        if (lessonDao.getLessonCount() < 40L) {
            insertDefaultLessons()
        }
        if (pathDao.getPathCount() == 0) {
            insertDefaultLearningPaths()
        }
        insertDefaultBadges()
    }

    private suspend fun insertDefaultLessons() {
        val lessons = BeginnerLessons.data + IntermediateLessons.data + AdvancedLessons.data + ExpertLessons.data
        lessons.forEach { lessonDao.insertLesson(it) }
    }

    private suspend fun insertDefaultLearningPaths() {
        pathDao.insertPaths(LearningPathsSeed.data)
    }

    private suspend fun insertDefaultBadges() {
        val badges = listOf(
            Badge(id = 1, name = "First Steps", description = "Complete your first lesson", iconRes = "👣", requirement = "Complete 1 lesson", xpReward = 10, orderIndex = 1),
            Badge(id = 2, name = "Security Student", description = "Complete 5 lessons", iconRes = "📚", requirement = "Complete 5 lessons", xpReward = 50, orderIndex = 2),
            Badge(id = 3, name = "Security Scholar", description = "Complete all lessons", iconRes = "🎓", requirement = "Complete 10 lessons", xpReward = 100, orderIndex = 3),
            Badge(id = 4, name = "XP Hunter", description = "Earn 200 XP", iconRes = "⚡", requirement = "Earn 200 XP", xpReward = 25, orderIndex = 4),
            Badge(id = 5, name = "XP Master", description = "Earn 1000 XP", iconRes = "🔥", requirement = "Earn 1000 XP", xpReward = 75, orderIndex = 5),
            Badge(id = 6, name = "7-Day Streak", description = "Use the app 7 days in a row", iconRes = "📅", requirement = "7 day streak", xpReward = 100, orderIndex = 6),
            Badge(id = 7, name = "MITRE Explorer", description = "Complete the MITRE 101 lesson", iconRes = "🔬", requirement = "Complete 4 lessons", xpReward = 30, orderIndex = 7),
            Badge(id = 8, name = "Permission Guardian", description = "Complete the App Permissions lesson", iconRes = "🛡️", requirement = "Complete 3 lessons", xpReward = 30, orderIndex = 8)
        )
        badges.forEach { badgeDao.insertBadge(it) }
    }
}
