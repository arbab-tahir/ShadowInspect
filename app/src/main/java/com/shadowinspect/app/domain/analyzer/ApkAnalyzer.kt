package com.shadowinspect.app.domain.analyzer

import android.net.Uri
import com.shadowinspect.app.domain.model.ApkAnalysisResult
import com.shadowinspect.app.domain.model.ApkInfo

interface ApkAnalyzer {
    suspend fun analyzeApk(uri: Uri): ApkAnalysisResult
    fun probeApk(uri: Uri): ApkInfo?
}
