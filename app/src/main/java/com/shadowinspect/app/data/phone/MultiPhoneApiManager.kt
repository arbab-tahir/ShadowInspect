package com.shadowinspect.app.data.phone

import android.content.Context
import android.util.Log
import com.shadowinspect.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
//  1. API Key Provider & Configuration
// ─────────────────────────────────────────────────────────────────────────────

data class ApiConfig(
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val monthlyQuota: Int,
    val isEnabled: Boolean = true,
    val priority: Int = 0 // Higher = tried first
)

@Singleton
class ApiKeyProvider @Inject constructor() {
    fun getAllConfigs(): List<ApiConfig> = listOf(
        ApiConfig(
            name = PhoneApiProvider.ABSTRACT_API.id,
            baseUrl = "https://phonevalidation.abstractapi.com/",
            apiKey = BuildConfig.ABSTRACT_API_KEY,
            monthlyQuota = PhoneApiProvider.ABSTRACT_API.monthlyQuota,
            priority = 10
        ),
        ApiConfig(
            name = PhoneApiProvider.NUMVERIFY.id,
            baseUrl = "http://apilayer.net/api/",
            apiKey = BuildConfig.NUMVERIFY_API_KEY,
            monthlyQuota = PhoneApiProvider.NUMVERIFY.monthlyQuota,
            priority = 9
        ),
        ApiConfig(
            name = PhoneApiProvider.IPQUALITY.id,
            baseUrl = "https://ipqualityscore.com/",
            apiKey = BuildConfig.IPQUALITY_KEY,
            monthlyQuota = PhoneApiProvider.IPQUALITY.monthlyQuota,
            priority = 8
        ),
        ApiConfig(
            name = PhoneApiProvider.VERIPHONE.id,
            baseUrl = "https://veriphone.p.rapidapi.com/",
            apiKey = BuildConfig.VERIPHONE_KEY,
            monthlyQuota = PhoneApiProvider.VERIPHONE.monthlyQuota,
            priority = 7
        )
    ).filter { it.apiKey.isNotBlank() && it.apiKey != "your_key_here" }
}

// ─────────────────────────────────────────────────────────────────────────────
//  2. Service Interfaces
// ─────────────────────────────────────────────────────────────────────────────

interface AbstractApiService {
    @GET("v1/")
    suspend fun validatePhone(
        @Query("api_key") apiKey: String,
        @Query("phone") phone: String
    ): Response<AbstractApiResponse>
}

data class AbstractApiResponse(
    val valid: Boolean,
    val number: String,
    val format: FormatData?,
    val country: CountryData?,
    val carrier: String?,
    val type: String?
) {
    data class FormatData(val international: String?, val local: String?)
    data class CountryData(val code: String?, val name: String?, val prefix: String?)
}

interface NumverifyService {
    @GET("validate")
    suspend fun validatePhone(
        @Query("access_key") apiKey: String,
        @Query("number") number: String,
        @Query("country_code") countryCode: String? = null
    ): Response<NumverifyResponse>
}

data class NumverifyResponse(
    val valid: Boolean,
    val number: String,
    val local_format: String?,
    val international_format: String?,
    val country_prefix: String?,
    val country_code: String?,
    val country_name: String?,
    val location: String?,
    val carrier: String?,
    val line_type: String?
)

interface IPQualityService {
    @GET("api/json/phone/{apiKey}/{phone}")
    suspend fun validatePhone(
        @Path("apiKey") apiKey: String,
        @Path("phone") phone: String,
        @Query("country") countryCode: String? = null
    ): Response<IPQualityResponse>
}

data class IPQualityResponse(
    val success: Boolean,
    val valid: Boolean,
    val active: Boolean?,
    val number: String?,
    val formatted: String?,
    val country: String?,
    val carrier: String?,
    val line_type: String?,
    val prepaid: Boolean?,
    val voip: Boolean?
)

interface VeriphoneService {
    @GET("verify")
    suspend fun validatePhone(
        @Header("X-RapidAPI-Key") apiKey: String,
        @Header("X-RapidAPI-Host") host: String = "veriphone.p.rapidapi.com",
        @Query("phone") phone: String
    ): Response<VeriphoneResponse>
}

data class VeriphoneResponse(
    val status: String,
    val phone: String,
    val phone_valid: Boolean,
    val phone_type: String?,
    val phone_region: String?,
    val country: String?,
    val country_code: String?,
    val country_prefix: String?,
    val international_number: String?,
    val local_number: String?,
    val carrier: String?
)

// ─────────────────────────────────────────────────────────────────────────────
//  3. Unified Responses
// ─────────────────────────────────────────────────────────────────────────────

data class ApiResult(
    val source: String,
    val isValid: Boolean,
    val formattedNumber: String,
    val countryCode: String?,
    val countryName: String?,
    val carrier: String?,
    val lineType: String?,
    val isVoIP: Boolean,
    val isPrepaid: Boolean?
)

data class AggregatedApiResult(
    val isValid: Boolean,
    val confidenceScore: Int,
    val sourcesUsed: List<String>,
    val aggregatedData: ApiResult?
)

// Extensions to map service responses to ApiResult
fun AbstractApiResponse.toApiResult(): ApiResult = ApiResult(
    source = PhoneApiProvider.ABSTRACT_API.id,
    isValid = valid,
    formattedNumber = format?.international ?: number,
    countryCode = country?.code,
    countryName = country?.name,
    carrier = carrier,
    lineType = type,
    isVoIP = type?.equals("voip", ignoreCase = true) == true,
    isPrepaid = null
)

fun NumverifyResponse.toApiResult(): ApiResult = ApiResult(
    source = PhoneApiProvider.NUMVERIFY.id,
    isValid = valid,
    formattedNumber = international_format ?: number,
    countryCode = country_code,
    countryName = country_name,
    carrier = carrier,
    lineType = line_type,
    isVoIP = line_type?.equals("voip", ignoreCase = true) == true,
    isPrepaid = null
)

fun IPQualityResponse.toApiResult(phone: String): ApiResult = ApiResult(
    source = PhoneApiProvider.IPQUALITY.id,
    isValid = valid,
    formattedNumber = formatted ?: phone,
    countryCode = country,
    countryName = null, // IPQ usually just gives cc
    carrier = carrier,
    lineType = line_type,
    isVoIP = voip == true,
    isPrepaid = prepaid
)

fun VeriphoneResponse.toApiResult(): ApiResult = ApiResult(
    source = PhoneApiProvider.VERIPHONE.id,
    isValid = phone_valid,
    formattedNumber = international_number ?: phone,
    countryCode = country_code,
    countryName = country,
    carrier = carrier,
    lineType = phone_type,
    isVoIP = phone_type?.equals("voip", ignoreCase = true) == true,
    isPrepaid = null
)


// ─────────────────────────────────────────────────────────────────────────────
//  4. Multi-API Manager
// ─────────────────────────────────────────────────────────────────────────────

@Singleton
class MultiPhoneApiManager @Inject constructor(
    private val apiKeyProvider: ApiKeyProvider,
    private val usageTracker: ApiUsageTracker
) {
    private val services = mutableMapOf<String, Any>()

    init {
        initializeServices()
    }

    private fun initializeServices() {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .build()

        apiKeyProvider.getAllConfigs().forEach { config ->
            try {
                val retrofit = Retrofit.Builder()
                    .baseUrl(config.baseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val service: Any? = when (config.name) {
                    PhoneApiProvider.ABSTRACT_API.id -> retrofit.create(AbstractApiService::class.java)
                    PhoneApiProvider.NUMVERIFY.id    -> retrofit.create(NumverifyService::class.java)
                    PhoneApiProvider.IPQUALITY.id    -> retrofit.create(IPQualityService::class.java)
                    PhoneApiProvider.VERIPHONE.id    -> retrofit.create(VeriphoneService::class.java)
                    else -> null
                }

                if (service != null) {
                    services[config.name] = service
                }
            } catch (e: Exception) {
                Log.e("MultiPhoneApi", "Failed to init ${config.name}", e)
            }
        }
    }

    suspend fun validatePhoneWithAllApis(
        phone: String,
        countryCode: String? = null
    ): List<ApiResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ApiResult>()
        val allConfigs = apiKeyProvider.getAllConfigs()
        
        if (allConfigs.isEmpty()) {
            android.util.Log.w("MultiPhoneApi", "No API keys configured - using local discovery only")
            return@withContext results
        }

        val configs = allConfigs
            .filter { usageTracker.canUseProvider(it.name) }
            .sortedByDescending { it.priority }

        for (config in configs) {
            try {
                val result: ApiResult? = when (val service = services[config.name]) {
                    is AbstractApiService -> {
                        val resp = service.validatePhone(config.apiKey, phone)
                        if (resp.isSuccessful) resp.body()?.toApiResult() else null
                    }
                    is NumverifyService -> {
                        val resp = service.validatePhone(config.apiKey, phone, countryCode)
                        if (resp.isSuccessful) resp.body()?.toApiResult() else null
                    }
                    is IPQualityService -> {
                        val resp = service.validatePhone(config.apiKey, phone, countryCode)
                        if (resp.isSuccessful && resp.body()?.success == true) resp.body()?.toApiResult(phone) else null
                    }
                    is VeriphoneService -> {
                        val resp = service.validatePhone(config.apiKey, phone = phone)
                        if (resp.isSuccessful) resp.body()?.toApiResult() else null
                    }
                    else -> null
                }

                if (result != null) {
                    results.add(result)
                    usageTracker.incrementUsage(config.name)
                }

            } catch (e: Exception) {
                Log.e("MultiPhoneApi", "Error calling ${config.name}", e)
            }

            // Stop if we have at least 2 successful API responses
            if (results.size >= 2) break
        }

        results
    }

    fun aggregateResults(results: List<ApiResult>): AggregatedApiResult {
        if (results.isEmpty()) {
            return AggregatedApiResult(false, 0, emptyList(), null)
        }

        val weightedResults = results.map { result ->
            val weight = when (result.source) {
                PhoneApiProvider.IPQUALITY.id -> 1.5
                PhoneApiProvider.ABSTRACT_API.id -> 1.2
                else -> 1.0
            }
            result to weight
        }

        val totalWeight = weightedResults.sumOf { it.second }
        val validWeight = weightedResults.filter { it.first.isValid }.sumOf { it.second }
        
        val confidenceScore = if (totalWeight > 0) ((validWeight / totalWeight) * 100).toInt() else 0
        
        val bestResult = weightedResults.maxByOrNull { it.second }?.first

        return AggregatedApiResult(
            isValid = confidenceScore >= 50,
            confidenceScore = confidenceScore,
            sourcesUsed = results.map { it.source },
            aggregatedData = bestResult
        )
    }
}
