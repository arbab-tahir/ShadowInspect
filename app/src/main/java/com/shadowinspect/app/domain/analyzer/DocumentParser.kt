package com.shadowinspect.app.domain.analyzer

import com.shadowinspect.app.domain.model.DocumentMetadata
import com.shadowinspect.app.domain.model.DocumentType
import com.shadowinspect.app.domain.model.ExtractedUrl
import java.io.File

/**
 * Interface for document parsers
 * Each document type will have its own implementation
 */
interface DocumentParser {
    
    /**
     * Check if this parser can handle the given document type
     */
    fun canHandle(documentType: DocumentType): Boolean
    
    /**
     * Parse document and extract metadata
     */
    suspend fun parseMetadata(file: File): DocumentMetadata
    
    /**
     * Extract all URLs from document
     */
    suspend fun extractUrls(file: File): List<ExtractedUrl>
    
    /**
     * Check if document contains macros
     */
    suspend fun hasMacros(file: File): Boolean
    
    /**
     * Extract macros for analysis
     */
    suspend fun extractMacros(file: File): List<ByteArray>
    
    /**
     * Extract plain text content
     */
    suspend fun extractText(file: File): String
    
    /**
     * Check if document is encrypted/password protected
     */
    suspend fun isEncrypted(file: File): Boolean
    
    /**
     * Get page count (for PDFs, multi-page docs)
     */
    suspend fun getPageCount(file: File): Int?
}
