package com.shadowinspect.app.data.analysis.pdf

import android.content.Context
import android.util.Log
import com.shadowinspect.app.domain.analyzer.DocumentParser
import com.shadowinspect.app.domain.model.DocumentMetadata
import com.shadowinspect.app.domain.model.DocumentType
import com.shadowinspect.app.domain.model.ExtractedUrl
import com.shadowinspect.app.domain.model.PdfPermissions
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.action.PDActionJavaScript
import com.tom_roush.pdfbox.pdmodel.interactive.action.PDActionURI
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDSignatureField
import com.tom_roush.pdfbox.text.PDFTextStripper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xml.sax.InputSource
import java.io.File
import java.io.StringReader
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton
import javax.xml.parsers.DocumentBuilderFactory

@Singleton
class PdfParser @Inject constructor(
    @ApplicationContext private val context: Context
) : DocumentParser {

    private val tag = "PdfParser"

    init { PDFBoxResourceLoader.init(context) }

    private val urlPattern = Pattern.compile(
        "((https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])"
    )

    override fun canHandle(documentType: DocumentType) = documentType == DocumentType.PDF

    override suspend fun parseMetadata(file: File): DocumentMetadata = withContext(Dispatchers.IO) {
        PDDocument.load(file).use { document ->
            val info = document.documentInformation

            val keywords = info.keywords
                ?.split(",", ";")?.map { it.trim() }?.filter { it.isNotBlank() }
                ?: emptyList()

            // ── XMP Metadata stream ────────────────────────────────
            var xmpLanguage: String? = null
            var xmpCompany: String?  = null
            try {
                val xmpStream = document.documentCatalog.metadata
                if (xmpStream != null) {
                    val xml = String(xmpStream.exportXMPMetadata().readBytes(), Charsets.UTF_8)
                    xmpLanguage = extractXmpValue(xml, "dc:language")
                    xmpCompany  = extractXmpValue(xml, "pdf:Company")
                        ?: extractXmpValue(xml, "xmp:CreatorTool")
                }
            } catch (e: Exception) { Log.w(tag, "XMP: ${e.message}") }

            // ── Word / character count ─────────────────────────────
            var wordCount: Int? = null
            var charCount: Int? = null
            try {
                val text = PDFTextStripper().getText(document)
                wordCount = text.split(Regex("\\s+")).count { it.isNotBlank() }
                charCount = text.replace(Regex("\\s"), "").length
            } catch (e: Exception) { Log.w(tag, "Word count: ${e.message}") }

            // ── PDF Permission flags ───────────────────────────────
            val perms: PdfPermissions? = try {
                val ap = document.currentAccessPermission
                PdfPermissions(
                    canPrint            = ap.canPrint(),
                    canCopy             = ap.canExtractContent(),
                    canModify           = ap.canModify(),
                    canAnnotate         = ap.canModifyAnnotations(),
                    canFillForms        = ap.canFillInForm(),
                    canExtractContent   = ap.canExtractForAccessibility(),
                    canAssemble         = ap.canAssembleDocument(),
                    canPrintHighQuality = ap.canPrint()
                )
            } catch (e: Exception) { null }

            // ── JavaScript detection ───────────────────────────────
            val jsActions = mutableListOf<String>()
            try {
                val openAction = document.documentCatalog.openAction
                if (openAction is PDActionJavaScript) {
                    jsActions.add(openAction.action?.take(200) ?: "JS:OpenAction")
                }
                val jsTree = document.documentCatalog.names?.javaScript
                if (jsTree != null) {
                    jsTree.names?.forEach { jsActions.add("JS:Named:$it") }
                    if (jsActions.isEmpty()) jsActions.add("JS:NamesTree")
                }
                for (page in document.pages) {
                    page.annotations?.forEach { annot ->
                        if (annot is PDAnnotationLink) {
                            val action = annot.action
                            if (action is PDActionJavaScript) {
                                jsActions.add("JS:Page:${action.action?.take(80) ?: "JS"}")
                            }
                        }
                    }
                }
            } catch (e: Exception) { Log.w(tag, "JS scan: ${e.message}") }

            // ── Embedded files ─────────────────────────────────────
            val embeddedFileNames = mutableListOf<String>()
            try {
                document.documentCatalog.names?.embeddedFiles?.names
                    ?.forEach { embeddedFileNames.add(it.key) }
            } catch (e: Exception) { Log.w(tag, "Embedded files: ${e.message}") }

            // ── Digital signature ──────────────────────────────────
            var isSigned = false
            try {
                isSigned = document.documentCatalog.acroForm
                    ?.fields?.any { it is PDSignatureField } == true
            } catch (e: Exception) { Log.w(tag, "Signature: ${e.message}") }

            // ── Annotation URI extraction ──────────────────────────
            val annotationUrls = mutableListOf<String>()
            try {
                for (page in document.pages) {
                    page.annotations?.forEach { annot ->
                        if (annot is PDAnnotationLink) {
                            val uri = (annot.action as? PDActionURI)?.uri
                            if (!uri.isNullOrBlank()) annotationUrls.add(uri)
                        }
                    }
                }
            } catch (e: Exception) { Log.w(tag, "Annot scan: ${e.message}") }

            DocumentMetadata(
                author           = info.author,
                creator          = info.creator,
                producer         = info.producer,
                company          = xmpCompany,
                title            = info.title,
                subject          = info.subject,
                keywords         = keywords,
                language         = xmpLanguage,
                creationDate     = info.creationDate?.time?.time,
                modificationDate = info.modificationDate?.time?.time,
                pageCount        = document.numberOfPages,
                wordCount        = wordCount,
                characterCount   = charCount,
                format           = "PDF",
                version          = "%PDF-${document.version}",
                isDigitallySigned = isSigned,
                pdfPermissions   = perms,
                embeddedFileCount = embeddedFileNames.size,
                embeddedFileNames = embeddedFileNames,
                embeddedScripts  = jsActions,
                pdfJsActions     = jsActions,
                annotationUrls   = annotationUrls
            )
        }
    }

    override suspend fun extractUrls(file: File): List<ExtractedUrl> = withContext(Dispatchers.IO) {
        val urls = mutableListOf<ExtractedUrl>()
        PDDocument.load(file).use { document ->
            val stripper = PDFTextStripper()
            for (pageNum in 1..document.numberOfPages) {
                stripper.startPage = pageNum
                stripper.endPage   = pageNum
                val pageText = stripper.getText(document)
                val matcher  = urlPattern.matcher(pageText)
                while (matcher.find()) {
                    val url = matcher.group()
                    urls.add(ExtractedUrl(url, getContextAroundMatch(pageText, matcher.start(), 50), pageNum, isSuspiciousUrl(url)))
                }
            }
            var pageNum = 1
            for (page in document.pages) {
                page.annotations?.forEach { annot ->
                    if (annot is PDAnnotationLink) {
                        val uri = (annot.action as? PDActionURI)?.uri ?: return@forEach
                        if (urls.none { it.url == uri }) {
                            urls.add(ExtractedUrl(uri, "Annotation link", pageNum, isSuspiciousUrl(uri)))
                        }
                    }
                }
                pageNum++
            }
        }
        return@withContext urls.distinctBy { it.url }
    }

    override suspend fun hasMacros(file: File): Boolean = withContext(Dispatchers.IO) {
        PDDocument.load(file).use { document ->
            val openAction = document.documentCatalog.openAction
            if (openAction is PDActionJavaScript) return@withContext true
            return@withContext document.documentCatalog.names?.javaScript != null
        }
    }

    override suspend fun extractMacros(file: File): List<ByteArray> = emptyList()

    override suspend fun extractText(file: File): String = withContext(Dispatchers.IO) {
        PDDocument.load(file).use { PDFTextStripper().getText(it) }
    }

    override suspend fun isEncrypted(file: File): Boolean = withContext(Dispatchers.IO) {
        try { PDDocument.load(file).use { false } } catch (e: Exception) { true }
    }

    override suspend fun getPageCount(file: File): Int? = withContext(Dispatchers.IO) {
        PDDocument.load(file).use { it.numberOfPages }
    }

    private fun extractXmpValue(xml: String, tagName: String): String? {
        return try {
            val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = false }
            val doc     = factory.newDocumentBuilder().parse(InputSource(StringReader(xml)))
            doc.documentElement.normalize()
            val localName = tagName.substringAfter(":")
            val elements  = doc.getElementsByTagName(tagName).takeIf { it.length > 0 }
                ?: doc.getElementsByTagName(localName)
            for (i in 0 until elements.length) {
                val text = elements.item(i).textContent.trim()
                if (text.isNotBlank()) return text
            }
            null
        } catch (e: Exception) { null }
    }

    private fun getContextAroundMatch(text: String, start: Int, size: Int): String {
        return text.substring(maxOf(0, start - size), minOf(text.length, start + size))
    }

    private fun isSuspiciousUrl(url: String): Boolean {
        return listOf("bit\\.ly","tinyurl","goo\\.gl","ow\\.ly","t\\.co",
            "login","signin","secure","account","verify","update","confirm",
            "banking","paypal","credential","password","wallet","rebrand\\.ly")
            .any { Regex(it, RegexOption.IGNORE_CASE).containsMatchIn(url) }
    }
}
