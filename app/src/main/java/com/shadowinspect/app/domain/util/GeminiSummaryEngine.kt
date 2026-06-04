package com.shadowinspect.app.domain.util

import com.shadowinspect.app.BuildConfig
import com.shadowinspect.app.data.remote.api.GeminiApi
import com.shadowinspect.app.data.remote.api.GeminiContent
import com.shadowinspect.app.data.remote.api.GeminiPart
import com.shadowinspect.app.data.remote.api.GeminiRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiSummaryEngine @Inject constructor(
    private val geminiApi: GeminiApi
) {
    suspend fun generateUrlSummary(url: String, vtRiskScore: Int, vtStats: String): String {
        if (BuildConfig.GEMINI_API_KEY.isEmpty()) return "AI Summary unavailable (No API Key)"
        
        val prompt = """
            You are an expert cybersecurity analyst. Provide a short, easy-to-understand 2-sentence summary 
            explaining the risk of this URL based on the combined VirusTotal and urlscan.io results. 
            URL: $url
            Risk Score: $vtRiskScore / 100
            Analysis Context: $vtStats
        """.trimIndent()

        return try {
            val response = geminiApi.generateContent(
                apiKey = BuildConfig.GEMINI_API_KEY,
                request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(
                            parts = listOf(GeminiPart(text = prompt))
                        )
                    )
                )
            )
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim() 
                ?: "Failed to generate AI summary."
        } catch (e: retrofit2.HttpException) {
            when (e.code()) {
                429 -> "API Quota Exceeded (HTTP 429). Please check your Gemini billing/limits."
                404 -> "AI Model Not Found (HTTP 404). Endpoint might have changed."
                403 -> "API Key Unauthorized (HTTP 403). Ensure the key is valid."
                else -> "AI Summary Error: HTTP ${e.code()}"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "AI Summary Error: ${e.message}"
        }
    }

    suspend fun generateApkSummary(
        fileName: String,
        packageName: String?,
        riskScore: Int,
        permissions: Int,
        dangerous: Int,
        threats: String
    ): String {
        if (BuildConfig.GEMINI_API_KEY.isEmpty()) return "AI Summary unavailable (No API Key)"
        
        val prompt = """
            You are a mobile security engineer. Give a concise 2-sentence verdict on this APK file. 
            Focus on whether standard users should install it given its permissions and threat categories.
            File: $fileName
            Package: $packageName
            Total Permissions: $permissions
            Dangerous Permissions: $dangerous
            Risk Score: $riskScore / 100
            Threat Categories Identified: $threats
        """.trimIndent()

        return try {
            val response = geminiApi.generateContent(
                apiKey = BuildConfig.GEMINI_API_KEY,
                request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(
                            parts = listOf(GeminiPart(text = prompt))
                        )
                    )
                )
            )
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim() 
                ?: "Failed to generate AI summary."
        } catch (e: retrofit2.HttpException) {
            when (e.code()) {
                429 -> "API Quota Exceeded (HTTP 429). Please check your Gemini billing/limits."
                404 -> "AI Model Not Found (HTTP 404). Endpoint might have changed."
                403 -> "API Key Unauthorized (HTTP 403). Ensure the key is valid."
                else -> "AI Summary Error: HTTP ${e.code()}"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "AI Summary Error: ${e.message}"
        }
    }
}
