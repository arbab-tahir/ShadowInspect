package com.shadowinspect.app.data.analysis.office

import android.content.Context
import android.util.Log
import com.shadowinspect.app.domain.analyzer.DocumentParser
import com.shadowinspect.app.domain.model.DocumentMetadata
import com.shadowinspect.app.domain.model.DocumentType
import com.shadowinspect.app.domain.model.ExtractedUrl
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.apache.poi.hssf.extractor.ExcelExtractor
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.hwpf.extractor.WordExtractor
import org.apache.poi.xssf.extractor.XSSFExcelExtractor
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.hslf.usermodel.HSLFSlideShow
import org.apache.poi.sl.extractor.SlideShowExtractor
import java.io.File
import java.io.FileInputStream
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/** 1 minute = 60 s × 1 000 ms × 1 000 µs × 10 (100-ns ticks) = 600 000 000 ticks */
private const val TICKS_PER_MINUTE = 600_000_000L

@Singleton
class OfficeParser @Inject constructor(
    @ApplicationContext private val context: Context
) : DocumentParser {

    private val tag = "OfficeParser"

    private val urlPattern = Pattern.compile(
        "((https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])"
    )

    private val macroPatterns = listOf(
        "AutoOpen", "AutoExec", "AutoClose", "Document_Open", "Workbook_Open",
        "Sub\\s+Auto", "Shell\\(", "CreateObject", "WScript\\.Shell", "Run\\("
    )

    override fun canHandle(documentType: DocumentType): Boolean = documentType in listOf(
        DocumentType.DOCX, DocumentType.XLSX, DocumentType.PPTX,
        DocumentType.DOC, DocumentType.XLS, DocumentType.PPT
    )

    // ══════════════════════════════════════════════════════════════
    // METADATA
    // ══════════════════════════════════════════════════════════════

    override suspend fun parseMetadata(file: File): DocumentMetadata = withContext(Dispatchers.IO) {
        val name = file.name.lowercase()
        if (file.length() > 50 * 1024 * 1024L) throw DocumentTooLargeException(file.length())
        return@withContext when {
            name.endsWith(".docx") || name.endsWith(".xlsx") || name.endsWith(".pptx") ->
                parseOOXMLMetadata(file)
            name.endsWith(".doc") || name.endsWith(".xls") || name.endsWith(".ppt") ->
                parseOLEMetadata(file)
            else -> emptyMetadata()
        }
    }

    // ── OOXML (DOCX / XLSX / PPTX) ────────────────────────────────

    private suspend fun parseOOXMLMetadata(file: File): DocumentMetadata = withContext(Dispatchers.IO) {
        try {
            when {
                file.name.endsWith(".docx") -> parseDocxMetadata(file)
                file.name.endsWith(".xlsx") -> parseXlsxMetadata(file)
                file.name.endsWith(".pptx") -> parsePptxMetadata(file)
                else -> emptyMetadata()
            }
        } catch (e: Throwable) {
            Log.e(tag, "OOXML metadata failed: ${e.message}")
            emptyMetadata()
        }
    }

    private fun parseDocxMetadata(file: File): DocumentMetadata {
        return FileInputStream(file).use { fis ->
            XWPFDocument(fis).use { doc ->
                val core = doc.properties?.coreProperties
                val ext  = try { doc.properties?.extendedProperties?.underlyingProperties } catch (e: Exception) { null }

                // Word / char counts from Extended Properties
                val wordCount = try { ext?.words?.toLong()?.toInt() } catch (_: Exception) { null }
                val charCount = try { ext?.characters?.toLong()?.toInt() } catch (_: Exception) { null }
                val editTime  = try { ext?.totalTime?.toLong() } catch (_: Exception) { null }    // already minutes in OOXML
                val revision  = try { core?.revision?.toIntOrNull() } catch (_: Exception) { null }
                val company   = try { ext?.company } catch (_: Exception) { null }
                val manager   = try { ext?.manager } catch (_: Exception) { null }
                val template  = try { ext?.template?.takeIf { it.startsWith("http") } } catch (_: Exception) { null }

                // Paragraph and table count
                val paraCount  = try { doc.paragraphs.size } catch (_: Exception) { null }
                val tableCount = try { doc.tables.size }     catch (_: Exception) { null }

                // Tracked changes
                val hasTracked = try { doc.properties?.coreProperties?.revision?.let { it.toIntOrNull() ?: 0 > 1 } ?: false } catch (_: Exception) { false }

                // Embedded OLE objects → potential file dropper
                val embedNames = mutableListOf<String>()

                // Protection
                val isProtected = try { doc.isEnforcedProtection() } catch (_: Exception) { false }

                DocumentMetadata(
                    author           = core?.creator,
                    creator          = core?.creator,
                    producer         = "Microsoft Word",
                    company          = company,
                    manager          = manager,
                    lastModifiedBy   = try { core?.lastModifiedByUser } catch (_: Exception) { null },
                    title            = core?.title,
                    subject          = core?.subject,
                    keywords         = core?.keywords?.split(",", ";")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList(),
                    language         = null,
                    creationDate     = core?.created?.time,
                    modificationDate = core?.modified?.time,
                    revisionNumber   = revision,
                    totalEditingTime = editTime,
                    pageCount        = try { ext?.pages ?: 0 } catch (_: Exception) { 0 },
                    wordCount        = wordCount,
                    characterCount   = charCount,
                    paragraphCount   = paraCount,
                    tableCount       = tableCount,
                    format           = "DOCX",
                    templateUrl      = template,
                    isProtected      = isProtected,
                    hasTrackedChanges = hasTracked,
                    embeddedFileCount = embedNames.size,
                    embeddedFileNames = embedNames
                )
            }
        }
    }

    private fun parseXlsxMetadata(file: File): DocumentMetadata {
        return FileInputStream(file).use { fis ->
            XSSFWorkbook(fis).use { wb ->
                val core = wb.properties?.coreProperties
                val ext  = try { wb.properties?.extendedProperties?.underlyingProperties } catch (_: Exception) { null }

                val wordCount = try { ext?.words?.toLong()?.toInt() } catch (_: Exception) { null }
                val charCount = try { ext?.characters?.toLong()?.toInt() } catch (_: Exception) { null }
                val editTime  = try { ext?.totalTime?.toLong() } catch (_: Exception) { null }
                val revision  = try { core?.revision?.toIntOrNull() } catch (_: Exception) { null }
                val company   = try { ext?.company } catch (_: Exception) { null }
                val manager   = try { ext?.manager } catch (_: Exception) { null }
                val template  = try { ext?.template?.takeIf { it.startsWith("http") } } catch (_: Exception) { null }

                // Hidden sheets
                val hasHiddenSheets = (0 until wb.numberOfSheets).any { wb.isSheetHidden(it) }
                val sheetCount      = wb.numberOfSheets

                DocumentMetadata(
                    author           = core?.creator,
                    creator          = core?.creator,
                    producer         = "Microsoft Excel",
                    company          = company,
                    manager          = manager,
                    lastModifiedBy   = try { core?.lastModifiedByUser } catch (_: Exception) { null },
                    title            = core?.title,
                    subject          = core?.subject,
                    keywords         = core?.keywords?.split(",", ";")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList(),
                    language         = null,
                    creationDate     = core?.created?.time,
                    modificationDate = core?.modified?.time,
                    revisionNumber   = revision,
                    totalEditingTime = editTime,
                    pageCount        = sheetCount,
                    sheetCount       = sheetCount,
                    wordCount        = wordCount,
                    characterCount   = charCount,
                    format           = "XLSX",
                    templateUrl      = template,
                    hasHiddenSheets  = hasHiddenSheets
                )
            }
        }
    }

    private fun parsePptxMetadata(file: File): DocumentMetadata {
        return FileInputStream(file).use { fis ->
            XMLSlideShow(fis).use { pres ->
                val core = pres.properties?.coreProperties
                val ext  = try { pres.properties?.extendedProperties?.underlyingProperties } catch (_: Exception) { null }

                val editTime = try { ext?.totalTime?.toLong() } catch (_: Exception) { null }
                val revision = try { core?.revision?.toIntOrNull() } catch (_: Exception) { null }
                val company  = try { ext?.company } catch (_: Exception) { null }
                val manager  = try { ext?.manager } catch (_: Exception) { null }
                val template = try { ext?.template?.takeIf { it.startsWith("http") } } catch (_: Exception) { null }

                DocumentMetadata(
                    author           = core?.creator,
                    creator          = core?.creator,
                    producer         = "Microsoft PowerPoint",
                    company          = company,
                    manager          = manager,
                    lastModifiedBy   = try { core?.lastModifiedByUser } catch (_: Exception) { null },
                    title            = core?.title,
                    subject          = core?.subject,
                    keywords         = core?.keywords?.split(",", ";")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList(),
                    language         = null,
                    creationDate     = core?.created?.time,
                    modificationDate = core?.modified?.time,
                    revisionNumber   = revision,
                    totalEditingTime = editTime,
                    pageCount        = pres.slides.size,
                    slideCount       = pres.slides.size,
                    format           = "PPTX",
                    templateUrl      = template
                )
            }
        }
    }

    // ── OLE (DOC / XLS / PPT) ─────────────────────────────────────

    private suspend fun parseOLEMetadata(file: File): DocumentMetadata = withContext(Dispatchers.IO) {
        try {
            FileInputStream(file).use { fis ->
                when {
                    file.name.endsWith(".doc") -> parseDocMetadata(file)
                    file.name.endsWith(".xls") -> parseXlsMetadata(file)
                    file.name.endsWith(".ppt") -> parsePptMetadata(file)
                    else -> emptyMetadata()
                }
            }
        } catch (e: Throwable) {
            Log.e(tag, "OLE metadata failed: ${e.message}")
            emptyMetadata()
        }
    }

    private fun parseDocMetadata(file: File): DocumentMetadata {
        return FileInputStream(file).use { fis ->
            WordExtractor(fis).use { extractor ->
                val si  = extractor.summaryInformation
                val dsi = try { extractor.docSummaryInformation } catch (_: Exception) { null }

                // OLE edit time is in 100-nanosecond ticks → convert to minutes
                val rawEditTime = si?.editTime ?: 0L
                val editMinutes = if (rawEditTime > 0) rawEditTime / TICKS_PER_MINUTE else null

                DocumentMetadata(
                    author           = si?.author,
                    creator          = si?.author,
                    producer         = "Microsoft Word",
                    company          = try { dsi?.company } catch (_: Exception) { null },
                    manager          = try { dsi?.manager } catch (_: Exception) { null },
                    lastModifiedBy   = si?.lastAuthor,
                    title            = si?.title,
                    subject          = si?.subject,
                    keywords         = si?.keywords?.split(",", ";")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList(),
                    creationDate     = si?.createDateTime?.time,
                    modificationDate = si?.lastSaveDateTime?.time,
                    revisionNumber   = try { si?.revNumber?.toIntOrNull() } catch (_: Exception) { null },
                    totalEditingTime = editMinutes,
                    wordCount        = try { si?.wordCount } catch (_: Exception) { null },
                    characterCount   = try { si?.charCount } catch (_: Exception) { null },
                    format           = "DOC"
                )
            }
        }
    }

    private fun parseXlsMetadata(file: File): DocumentMetadata {
        return FileInputStream(file).use { fis ->
            HSSFWorkbook(fis).use { wb ->
                ExcelExtractor(wb).use { extractor ->
                    val si  = extractor.summaryInformation
                    val dsi = try { extractor.docSummaryInformation } catch (_: Exception) { null }

                    val rawEditTime = si?.editTime ?: 0L
                    val editMinutes = if (rawEditTime > 0) rawEditTime / TICKS_PER_MINUTE else null

                    val hasHiddenSheets = (0 until wb.numberOfSheets).any { wb.isSheetHidden(it) }

                    DocumentMetadata(
                        author           = si?.author,
                        creator          = si?.author,
                        producer         = "Microsoft Excel",
                        company          = try { dsi?.company } catch (_: Exception) { null },
                        manager          = try { dsi?.manager } catch (_: Exception) { null },
                        lastModifiedBy   = si?.lastAuthor,
                        title            = si?.title,
                        subject          = si?.subject,
                        keywords         = si?.keywords?.split(",", ";")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList(),
                        creationDate     = si?.createDateTime?.time,
                        modificationDate = si?.lastSaveDateTime?.time,
                        revisionNumber   = try { si?.revNumber?.toIntOrNull() } catch (_: Exception) { null },
                        totalEditingTime = editMinutes,
                        pageCount        = wb.numberOfSheets,
                        sheetCount       = wb.numberOfSheets,
                        hasHiddenSheets  = hasHiddenSheets,
                        format           = "XLS"
                    )
                }
            }
        }
    }

    private fun parsePptMetadata(file: File): DocumentMetadata {
        return FileInputStream(file).use { fis ->
            HSLFSlideShow(fis).use { pres ->
                val si  = pres.summaryInformation
                val dsi = try { pres.documentSummaryInformation } catch (_: Exception) { null }

                val rawEditTime = si?.editTime ?: 0L
                val editMinutes = if (rawEditTime > 0) rawEditTime / TICKS_PER_MINUTE else null

                DocumentMetadata(
                    author           = si?.author,
                    creator          = si?.author,
                    producer         = "Microsoft PowerPoint",
                    company          = try { dsi?.company } catch (_: Exception) { null },
                    manager          = try { dsi?.manager } catch (_: Exception) { null },
                    lastModifiedBy   = si?.lastAuthor,
                    title            = si?.title,
                    subject          = si?.subject,
                    keywords         = si?.keywords?.split(",", ";")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList(),
                    creationDate     = si?.createDateTime?.time,
                    modificationDate = si?.lastSaveDateTime?.time,
                    revisionNumber   = try { si?.revNumber?.toIntOrNull() } catch (_: Exception) { null },
                    totalEditingTime = editMinutes,
                    pageCount        = pres.slides.size,
                    slideCount       = pres.slides.size,
                    format           = "PPT"
                )
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    // URL EXTRACTION
    // ══════════════════════════════════════════════════════════════

    override suspend fun extractUrls(file: File): List<ExtractedUrl> = withContext(Dispatchers.IO) {
        val text = extractText(file)
        val urls = mutableListOf<ExtractedUrl>()
        val matcher = urlPattern.matcher(text)
        while (matcher.find()) {
            val url = matcher.group()
            urls.add(ExtractedUrl(url, getContext(text, matcher.start(), 50), null, isSuspiciousUrl(url)))
        }
        return@withContext urls
    }

    // ══════════════════════════════════════════════════════════════
    // MACRO CHECK
    // ══════════════════════════════════════════════════════════════

    override suspend fun hasMacros(file: File): Boolean = withContext(Dispatchers.IO) {
        val text = extractText(file)
        return@withContext macroPatterns.any { Regex(it, RegexOption.IGNORE_CASE).containsMatchIn(text) }
    }

    override suspend fun extractMacros(file: File): List<ByteArray> = emptyList()

    // ══════════════════════════════════════════════════════════════
    // TEXT EXTRACTION
    // ══════════════════════════════════════════════════════════════

    override suspend fun extractText(file: File): String = withContext(Dispatchers.IO) {
        if (file.length() > 50 * 1024 * 1024L) throw DocumentTooLargeException(file.length())
        try {
            withTimeoutOrNull(10_000L) {
                FileInputStream(file).use { fis ->
                    when {
                        file.name.endsWith(".docx") -> XWPFDocument(fis).use { XWPFWordExtractor(it).use { e -> e.text } }
                        file.name.endsWith(".xlsx") -> XSSFWorkbook(fis).use { XSSFExcelExtractor(it).use { e -> e.text } }
                        file.name.endsWith(".doc")  -> WordExtractor(fis).use { it.text }
                        file.name.endsWith(".xls")  -> HSSFWorkbook(fis).use { ExcelExtractor(it).use { e -> e.text } }
                        file.name.endsWith(".pptx") -> XMLSlideShow(fis).use { SlideShowExtractor(it).use { e -> e.text } }
                        file.name.endsWith(".ppt")  -> HSLFSlideShow(fis).use { SlideShowExtractor(it).use { e -> e.text } }
                        else -> ""
                    }
                }
            } ?: ""
        } catch (e: Throwable) { "" }
    }

    // ══════════════════════════════════════════════════════════════
    // ENCRYPTION CHECK
    // ══════════════════════════════════════════════════════════════

    override suspend fun isEncrypted(file: File): Boolean = withContext(Dispatchers.IO) {
        if (file.length() > 50 * 1024 * 1024L) return@withContext false
        try {
            FileInputStream(file).use { fis ->
                when {
                    file.name.endsWith(".docx") -> XWPFDocument(fis).use { false }
                    file.name.endsWith(".xlsx") -> XSSFWorkbook(fis).use { false }
                    file.name.endsWith(".pptx") -> XMLSlideShow(fis).use { false }
                    else -> false
                }
            }
        } catch (e: Throwable) { true }
    }

    override suspend fun getPageCount(file: File): Int? = null

    // ══════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════

    private fun emptyMetadata() = DocumentMetadata()

    private fun getContext(text: String, start: Int, size: Int): String {
        return text.substring(maxOf(0, start - size), minOf(text.length, start + size))
    }

    private fun isSuspiciousUrl(url: String): Boolean {
        return listOf("bit\\.ly","tinyurl","goo\\.gl","ow\\.ly","login","signin",
            "secure","account","verify","update","confirm","banking","paypal")
            .any { Regex(it, RegexOption.IGNORE_CASE).containsMatchIn(url) }
    }
}

class DocumentTooLargeException(fileSize: Long) : Exception(
    "We sincerely apologize, but this document exceeds our maximum safe scanning size limit of 50MB " +
    "(File size: ${fileSize / (1024 * 1024)}MB). " +
    "To protect your device's memory and ensure system stability, we cannot safely analyze Office documents of this size natively."
)
