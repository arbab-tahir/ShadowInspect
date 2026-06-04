package com.shadowinspect.app.di

import com.shadowinspect.app.BuildConfig
import com.shadowinspect.app.data.remote.api.VirusTotalApi
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.shadowinspect.app.data.phone.ApiKeyProvider
import com.shadowinspect.app.data.phone.ApiUsageTracker
import com.shadowinspect.app.data.phone.MultiPhoneApiManager
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@kotlin.annotation.Retention(AnnotationRetention.BINARY)
annotation class ApiKey

@Qualifier
@kotlin.annotation.Retention(AnnotationRetention.BINARY)
annotation class UrlScanApiKey

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val BASE_URL = "https://www.virustotal.com/api/v3/"

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    @ApiKey
    fun provideApiKey(): String = BuildConfig.VIRUSTOTAL_API_KEY

    @Provides
    @Singleton
    @UrlScanApiKey
    fun provideUrlScanApiKey(): String = BuildConfig.URLSCAN_API_KEY

    @Provides
    @Singleton
    fun provideApiKeyInterceptor(@ApiKey apiKey: String): Interceptor {
        return Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("x-apikey", apiKey)
                .build()
            chain.proceed(request)
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        apiKeyInterceptor: Interceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(apiKeyInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideVirusTotalApi(@ApiKey apiKey: String): VirusTotalApi {
        return VirusTotalApi.create(apiKey)
    }

    @Provides
    @Singleton
    fun provideUrlScanApi(
        loggingInterceptor: HttpLoggingInterceptor
    ): com.shadowinspect.app.data.remote.api.UrlScanApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return retrofit2.Retrofit.Builder()
            .baseUrl(com.shadowinspect.app.data.remote.api.UrlScanApi.BASE_URL)
            .client(client)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(com.shadowinspect.app.data.remote.api.UrlScanApi::class.java)
    }

    @Provides
    @Singleton
    fun provideGeminiApi(
        loggingInterceptor: HttpLoggingInterceptor
    ): com.shadowinspect.app.data.remote.api.GeminiApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS) // AI generation can take longer
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return retrofit2.Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(client)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(com.shadowinspect.app.data.remote.api.GeminiApi::class.java)
    }

    @Provides
    @Singleton
    fun provideApiKeyProvider(): ApiKeyProvider {
        return ApiKeyProvider()
    }

    @Provides
    @Singleton
    fun provideApiUsageTracker(@ApplicationContext context: Context): ApiUsageTracker {
        return ApiUsageTracker(context)
    }

    @Provides
    @Singleton
    fun provideMultiPhoneApiManager(
        apiKeyProvider: ApiKeyProvider,
        usageTracker: ApiUsageTracker
    ): MultiPhoneApiManager {
        return MultiPhoneApiManager(apiKeyProvider, usageTracker)
    }
}
