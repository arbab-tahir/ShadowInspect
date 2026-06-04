package com.shadowinspect.app.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * Data models for urlscan.io API
 */

// --- Submission ---

data class UrlScanSubmissionRequest(
    val url: String,
    val visibility: String = "public",
    val tags: List<String> = listOf("ShadowInspect")
)

data class UrlScanSubmissionResponse(
    val message: String,
    val uuid: String,
    val result: String,
    val api: String,
    val visibility: String,
    val options: Map<String, Any>? = null,
    val url: String
)

// --- Result ---

data class UrlScanResultResponse(
    val task: TaskInfo,
    val page: PageInfo,
    val stats: StatsInfo,
    val verdict: VerdictInfo? = null,
    @SerializedName("meta") val meta: MetaInfo? = null
)

data class TaskInfo(
    val uuid: String,
    val time: String,
    val url: String,
    val visibility: String,
    val method: String,
    val screenshotURL: String? = null
)

data class PageInfo(
    val url: String,
    val domain: String,
    val ip: String? = null,
    val country: String? = null,
    val city: String? = null,
    val server: String? = null,
    val title: String? = null,
    val asn: String? = null,
    val asnname: String? = null
)

data class StatsInfo(
    val resourceStats: List<ResourceStat>? = null,
    val protocolStats: List<ProtocolStat>? = null,
    val tlsStats: List<TlsStat>? = null,
    val IPv4Stats: List<IPStat>? = null
)

data class ResourceStat(val count: Int, val size: Long, val type: String)
data class ProtocolStat(val count: Int, val size: Long, val protocol: String)
data class TlsStat(val count: Int, val size: Long, val protocol: String)
data class IPStat(val count: Int, val size: Long, val continent: String? = null)

data class VerdictInfo(
    val overall: OverallVerdict? = null,
    val urlscan: UrlscanVerdict? = null,
    val engines: EnginesVerdict? = null,
    val community: CommunityVerdict? = null
)

data class OverallVerdict(val score: Int, val categories: List<String>?, val malicious: Boolean)
data class UrlscanVerdict(val score: Int, val categories: List<String>?, val malicious: Boolean)
data class EnginesVerdict(val score: Int, val malicious: Int, val benign: Int)
data class CommunityVerdict(val score: Int, val votes: Int)

data class MetaInfo(
    @SerializedName("processors") val processors: ProcessorsInfo? = null
)

data class ProcessorsInfo(
    @SerializedName("wappalyzer") val wappalyzer: WappalyzerInfo? = null
)

data class WappalyzerInfo(
    @SerializedName("data") val data: List<TechData>? = null
)

data class TechData(
    val app: String,
    val categories: List<String>? = null,
    val version: String? = null,
    val icon: String? = null
)
