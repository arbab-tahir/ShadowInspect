package com.shadowinspect.app.presentation.scan

import com.shadowinspect.app.domain.model.UrlScanResult

data class UrlScanState(
    val urlInput: String = "",
    val isScanning: Boolean = false,
    val scanResult: UrlScanResult? = null,
    val error: String? = null
)
