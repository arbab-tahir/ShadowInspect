package com.shadowinspect.app.di

import android.content.Context
import com.shadowinspect.app.domain.mitre.MitreExplanationGenerator
import com.shadowinspect.app.domain.mitre.MitreJsonParser
import com.shadowinspect.app.domain.mitre.MitreSearchEngine
import com.shadowinspect.app.domain.mitre.MitreRiskCalculator
import com.shadowinspect.app.domain.mitre.MitreAnalyzer
import com.shadowinspect.app.domain.repository.MitreRepository
import com.shadowinspect.app.domain.repository.MitreRepositoryImpl
import com.shadowinspect.app.data.db.ScanDao
import com.shadowinspect.app.data.db.MitreDetectionDao
import com.shadowinspect.app.data.db.MitreReportDao
import com.shadowinspect.app.data.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MitreModule {
    
    @Provides
    @Singleton
    fun provideMitreDetectionDao(database: AppDatabase): MitreDetectionDao {
        return database.mitreDetectionDao()
    }
    
    @Provides
    @Singleton
    fun provideMitreReportDao(database: AppDatabase): MitreReportDao {
        return database.mitreReportDao()
    }
    
    @Provides
    @Singleton
    fun provideMitreJsonParser(
        @ApplicationContext context: Context
    ): MitreJsonParser {
        return MitreJsonParser(context)
    }
    
    @Provides
    @Singleton
    fun provideMitreExplanationGenerator(): MitreExplanationGenerator {
        return MitreExplanationGenerator()
    }
    
    @Provides
    @Singleton
    fun provideMitreSearchEngine(): MitreSearchEngine {
        return MitreSearchEngine()
    }
    
    @Provides
    @Singleton
    fun provideMitreRiskCalculator(
        jsonParser: MitreJsonParser
    ): MitreRiskCalculator {
        return MitreRiskCalculator(jsonParser)
    }

    @Provides
    @Singleton
    fun provideMitreAnalyzer(
        jsonParser: MitreJsonParser,
        searchEngine: MitreSearchEngine,
        explanationGenerator: MitreExplanationGenerator,
        riskCalculator: MitreRiskCalculator
    ): MitreAnalyzer {
        return MitreAnalyzer(
            jsonParser = jsonParser,
            searchEngine = searchEngine,
            explanationGenerator = explanationGenerator,
            riskCalculator = riskCalculator
        )
    }

    @Provides
    @Singleton
    fun provideMitreRepository(
        jsonParser: MitreJsonParser,
        explanationGenerator: MitreExplanationGenerator,
        scanDao: ScanDao,
        searchEngine: MitreSearchEngine,
        mitreDetectionDao: MitreDetectionDao,
        mitreReportDao: MitreReportDao
    ): MitreRepository {
        return MitreRepositoryImpl(
             jsonParser = jsonParser,
             explanationGenerator = explanationGenerator,
             scanDao = scanDao,
             searchEngine = searchEngine,
             mitreDetectionDao = mitreDetectionDao,
             mitreReportDao = mitreReportDao
        )
    }
}
