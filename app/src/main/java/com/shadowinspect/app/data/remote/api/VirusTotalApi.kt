package com.shadowinspect.app.data.remote.api

import com.google.gson.annotations.SerializedName
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

// --- Data Classes for API Responses ---

data class UrlScanResponse(
    @SerializedName("data") val data: ScanData
)

data class ScanData(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String? = null
)

data class AnalysisReportResponse(
    @SerializedName("data") val data: AnalysisData
)

data class AnalysisData(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String? = null,
    @SerializedName("attributes") val attributes: AnalysisAttributes
)

data class AnalysisAttributes(
    @SerializedName("status") val status: String,
    @SerializedName("stats") val stats: ApiAnalysisStats? = null,
    @SerializedName("results") val results: Map<String, EngineResult>? = null
)

data class EngineResult(
    @SerializedName("method") val method: String? = null,
    @SerializedName("engine_name") val engineName: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("result") val result: String? = null
)

data class ApiAnalysisStats(
    @SerializedName("malicious") val malicious: Int,
    @SerializedName("suspicious") val suspicious: Int,
    @SerializedName("harmless") val harmless: Int,
    @SerializedName("undetected") val undetected: Int,
    @SerializedName("timeout") val timeout: Int = 0
)

// --- Interface Definition ---

interface VirusTotalApi {

    @FormUrlEncoded
    @POST("urls")
    suspend fun scanUrl(
        @Field("url") url: String
    ): UrlScanResponse

    @GET("analyses/{id}")
    suspend fun getAnalysisReport(
        @Path("id") id: String
    ): AnalysisReportResponse

    companion object {
        private const val BASE_URL = "https://www.virustotal.com/api/v3/"

        /**
         * Generic create method. 
         * Note: In this project, Hilt provides the Retrofit instance in NetworkModule,
         * so this is primarily for testing or specific manual instantiation.
         */
        fun create(apiKey: String): VirusTotalApi {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val authInterceptor = Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("x-apikey", apiKey)
                    .build()
                chain.proceed(request)
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(authInterceptor)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(VirusTotalApi::class.java)
        }
    }
}
