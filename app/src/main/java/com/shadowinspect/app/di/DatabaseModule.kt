package com.shadowinspect.app.di

import android.content.Context
import androidx.room.Room
import com.shadowinspect.app.data.auth.AgentAuthRepository
import com.shadowinspect.app.data.auth.AgentDao
import com.shadowinspect.app.data.db.AppDatabase
import com.shadowinspect.app.data.db.BadgeDao
import com.shadowinspect.app.data.db.DailyTipDao
import com.shadowinspect.app.data.db.DocumentDao
import com.shadowinspect.app.data.db.ImageDao
import com.shadowinspect.app.data.db.LearningPathDao
import com.shadowinspect.app.data.db.LessonDao
import com.shadowinspect.app.data.db.PhoneReportDao
import com.shadowinspect.app.data.db.ScanDao
import com.shadowinspect.app.data.db.UserProgressDao
import com.shadowinspect.app.data.db.WidgetConfigDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "shadowinspect.db"
        )
            .addMigrations(
                AppDatabase.MIGRATION_8_9,
                AppDatabase.MIGRATION_9_10,
                AppDatabase.MIGRATION_10_11,
                AppDatabase.MIGRATION_11_12,
                AppDatabase.MIGRATION_12_13,
                AppDatabase.MIGRATION_13_14,
                AppDatabase.MIGRATION_14_15,
                AppDatabase.MIGRATION_15_16,
                AppDatabase.MIGRATION_16_17,
                AppDatabase.MIGRATION_17_18,
                AppDatabase.MIGRATION_17_18,
                AppDatabase.MIGRATION_18_19,
                AppDatabase.MIGRATION_19_20
            )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideAgentDao(db: AppDatabase): AgentDao = db.agentDao()

    @Provides
    fun provideScanDao(db: AppDatabase): ScanDao = db.scanDao()

    @Provides
    fun providePhoneReportDao(db: AppDatabase): PhoneReportDao = db.phoneReportDao()

    @Provides
    fun provideResearchDataDao(database: AppDatabase) = database.researchDataDao()

    @Provides
    fun provideUserFeedbackDao(database: AppDatabase) = database.userFeedbackDao()

    @Provides
    fun provideScoreDao(database: AppDatabase) = database.scoreDao()

    @Provides
    fun provideWidgetConfigDao(database: AppDatabase): WidgetConfigDao = database.widgetConfigDao()

    @Provides
    fun provideDocumentDao(database: AppDatabase): DocumentDao = database.documentDao()

    @Provides
    fun provideImageDao(database: AppDatabase): ImageDao = database.imageDao()

    // Module 15 — Education DAOs
    @Provides
    fun provideLessonDao(database: AppDatabase): LessonDao = database.lessonDao()

    @Provides
    fun provideDailyTipDao(database: AppDatabase): DailyTipDao = database.dailyTipDao()

    @Provides
    fun provideBadgeDao(database: AppDatabase): BadgeDao = database.badgeDao()

    @Provides
    fun provideUserProgressDao(database: AppDatabase): UserProgressDao = database.userProgressDao()

    @Provides
    fun provideLearningPathDao(database: AppDatabase): LearningPathDao = database.learningPathDao()
}
