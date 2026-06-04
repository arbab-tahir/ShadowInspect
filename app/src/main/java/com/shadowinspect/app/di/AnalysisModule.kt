package com.shadowinspect.app.di

import com.shadowinspect.app.data.analysis.ApkAnalyzerImpl
import com.shadowinspect.app.domain.analyzer.ApkAnalyzer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface AnalysisModule {

    @Binds
    @Singleton
    fun bindApkAnalyzer(impl: ApkAnalyzerImpl): ApkAnalyzer
}
