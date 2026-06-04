package com.shadowinspect.app.data.analysis.text

import android.content.Context
import com.shadowinspect.app.domain.analyzer.DocumentParser
import com.shadowinspect.app.domain.model.DocumentMetadata
import com.shadowinspect.app.domain.model.DocumentType
import com.shadowinspect.app.domain.model.ExtractedUrl
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextParser @Inject constructor(
    @ApplicationContext private val context: Context
) : DocumentParser {
    
    private val urlPattern = Pattern.compile(
        "((https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])"
    )
    
    private val emailPattern = Pattern.compile(
        "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
    )
    
    override fun canHandle(documentType: DocumentType): Boolean {
        return documentType in listOf(
            DocumentType.TXT,
            DocumentType.RTF
        )
    }
    
    override suspend fun parseMetadata(file: File): DocumentMetadata = withContext(Dispatchers.IO) {
        // Text files have minimal metadata
        val text = extractText(file)
        DocumentMetadata(
            author = null,
            creator = null,
            producer = null,
            creationDate = file.lastModified(),
            modificationDate = file.lastModified(),
            title = file.nameWithoutExtension,
            subject = null,
            keywords = emptyList(),
            pageCount = null,
            wordCount = countWords(text),
            characterCount = text.length,
            language = null,
            company = null,
            manager = null,
            lastModifiedBy = null,
            revisionNumber = null,
            totalEditingTime = null,
            format = file.extension.uppercase(),
            version = null
        )
    }
    
    override suspend fun extractUrls(file: File): List<ExtractedUrl> = withContext(Dispatchers.IO) {
        val text = extractText(file)
        val urls = mutableListOf<ExtractedUrl>()
        
        val urlMatcher = urlPattern.matcher(text)
        while (urlMatcher.find()) {
            urls.add(
                ExtractedUrl(
                    url = urlMatcher.group(),
                    context = getContextAroundMatch(text, urlMatcher.start(), 50),
                    pageNumber = null,
                    isSuspicious = isSuspiciousUrl(urlMatcher.group())
                )
            )
        }
        
        return@withContext urls
    }
    
    override suspend fun hasMacros(file: File): Boolean = withContext(Dispatchers.IO) {
        return@withContext false // Text files don't have macros
    }
    
    override suspend fun extractMacros(file: File): List<ByteArray> = withContext(Dispatchers.IO) {
        return@withContext emptyList()
    }
    
    override suspend fun extractText(file: File): String = withContext(Dispatchers.IO) {
        return@withContext file.readText()
    }
    
    override suspend fun isEncrypted(file: File): Boolean = withContext(Dispatchers.IO) {
        return@withContext false
    }
    
    override suspend fun getPageCount(file: File): Int? = withContext(Dispatchers.IO) {
        return@withContext null
    }
    
    private fun countWords(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split("\\s+".toRegex()).size
    }
    
    private fun getContextAroundMatch(text: String, start: Int, contextSize: Int): String {
        val contextStart = maxOf(0, start - contextSize)
        val contextEnd = minOf(text.length, start + contextSize)
        return text.substring(contextStart, contextEnd)
    }
    
    private fun isSuspiciousUrl(url: String): Boolean {
        val suspiciousPatterns = listOf(
            "bit\\.ly",
            "tinyurl",
            "goo\\.gl",
            "ow\\.ly",
            "shortlink",
            "login",
            "signin",
            "secure",
            "account",
            "verify",
            "update",
            "confirm"
        )
        
        return suspiciousPatterns.any { pattern ->
            Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(url)
        }
    }
}
