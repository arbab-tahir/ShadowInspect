package com.shadowinspect.app.data.remote.api

import com.shadowinspect.app.data.remote.model.UrlScanResultResponse
import com.shadowinspect.app.data.remote.model.UrlScanSubmissionRequest
import com.shadowinspect.app.data.remote.model.UrlScanSubmissionResponse
import retrofit2.http.*

/**
 * Retrofit interface for urlscan.io API
 */
interface UrlScanApi {

    /**
     * Submit a URL for scanning
     */
    @POST("scan/")
    suspend fun submitUrl(
        @Header("api-key") apiKey: String,
        @Body request: UrlScanSubmissionRequest
    ): UrlScanSubmissionResponse

    /**
     * Retrieve the results of a scan using the UUID
     */
    @GET("result/{uuid}/")
    suspend fun getScanResult(
        @Path("uuid") uuid: String
    ): UrlScanResultResponse

    companion object {
        const val BASE_URL = "https://urlscan.io/api/v1/"
    }
}
