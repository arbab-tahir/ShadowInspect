package com.shadowinspect.app.data.db

import androidx.room.*
import com.shadowinspect.app.domain.education.Badge
import com.shadowinspect.app.domain.education.UserBadge
import kotlinx.coroutines.flow.Flow

@Dao
interface BadgeDao {

    @Query("SELECT * FROM badges ORDER BY orderIndex ASC")
    fun getAllBadges(): Flow<List<Badge>>

    @Query("SELECT * FROM badges ORDER BY orderIndex ASC")
    suspend fun getAllBadgesSync(): List<Badge>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBadge(badge: Badge)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun earnBadge(userBadge: UserBadge)

    @Query("SELECT * FROM user_badges")
    suspend fun getEarnedBadges(): List<UserBadge>

    @Query("SELECT COUNT(*) FROM user_badges WHERE badgeId = :badgeId")
    suspend fun isBadgeEarned(badgeId: Long): Boolean

    @Query("UPDATE user_badges SET isNew = 0 WHERE isNew = 1")
    suspend fun markBadgesSeen()

    @Query("SELECT COUNT(*) FROM badges")
    suspend fun getBadgeCount(): Long
}
