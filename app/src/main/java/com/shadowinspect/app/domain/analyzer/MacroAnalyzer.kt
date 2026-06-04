package com.shadowinspect.app.domain.analyzer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MacroAnalyzer @Inject constructor() {

    // Known malicious macro patterns
    private val maliciousPatterns = listOf(
        // Auto-execute patterns
        Pattern.compile("AutoOpen|Document_Open|Workbook_Open|Auto_Open", Pattern.CASE_INSENSITIVE),
        // Shell execution
        Pattern.compile("Shell\\(.*\\)|CreateObject\\(.*\"(WScript|Shell)\"", Pattern.CASE_INSENSITIVE),
        // File system access
        Pattern.compile("Kill\\s|FileCopy\\s|MkDir\\s|RmDir\\s", Pattern.CASE_INSENSITIVE),
        // Registry access
        Pattern.compile("RegRead|RegWrite|RegDelete", Pattern.CASE_INSENSITIVE),
        // Network access
        Pattern.compile("URLDownload|XMLHTTP|WinHttp|Open\\s.*URL", Pattern.CASE_INSENSITIVE),
        // Obfuscation attempts
        Pattern.compile("Chr\\(\\d+\\)|Asc\\(|StrReverse|Replace\\s*\\([^,]*,[^,]*,[^)]*\\)", Pattern.CASE_INSENSITIVE),
        // Process creation
        Pattern.compile("CreateObject\\(.*\"(PowerShell|Cmd|WScript)\"", Pattern.CASE_INSENSITIVE),
        // Payload download
        Pattern.compile("GetObject\\(.*\"http|ADODB\\.Stream", Pattern.CASE_INSENSITIVE),
        // Anti-debugging
        Pattern.compile("IsDebuggerPresent|CheckBreaks", Pattern.CASE_INSENSITIVE),
        // Known malicious function names
        Pattern.compile("DownloadAndExecute|DecryptAndRun|StartKeylogger", Pattern.CASE_INSENSITIVE)
    )

    private val obfuscationPatterns = listOf(
        Pattern.compile("\\bChr\\(\\d+\\)\\s*&\\s*Chr\\(\\d+\\)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bStrReverse\\([^)]+\\)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bReplace\\([^,]+,[^,]+,[^)]+\\)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bEval\\([^)]+\\)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bExecute\\([^)]+\\)", Pattern.CASE_INSENSITIVE)
    )

    private val suspiciousApiCalls = listOf(
        "CreateObject", "GetObject", "Shell", "Run", "Exec",
        "URLDownloadToFile", "WinHttpRequest", "XMLHTTP",
        "FileSystemObject", "ADODB.Stream", "WScript.Shell",
        "RegWrite", "RegRead", "RegDelete", "Kill", "FileCopy"
    )

    suspend fun analyzeMacros(file: File): MacroAnalysisResult = withContext(Dispatchers.IO) {
        val fileContent = if (file.extension.lowercase() in listOf("docm", "xlsm", "pptm", "doc", "xls", "docx", "xlsx", "pptx")) {
            readFileContent(file)
        } else {
            ""
        }

        val suspiciousPatterns = findSuspiciousPatterns(fileContent)
        val obfuscationScore = calculateObfuscationScore(fileContent)
        val apiCalls = findApiCalls(fileContent)

        val isMalicious = suspiciousPatterns.size >= 2 ||
                obfuscationScore > 0.5 ||
                apiCalls.count { it in suspiciousApiCalls } >= 3

        val confidenceScore = calculateConfidenceScore(
            suspiciousPatterns.size, obfuscationScore, apiCalls.size
        )

        MacroAnalysisResult(
            hasMacros = fileContent.contains(Regex("Sub\\s+|Function\\s+|Property\\s+", RegexOption.IGNORE_CASE)),
            macroCount = countMacros(fileContent),
            isMalicious = isMalicious,
            confidenceScore = confidenceScore,
            suspiciousPatterns = suspiciousPatterns,
            obfuscationLevel = (obfuscationScore * 10).toInt(),
            apiCalls = apiCalls,
            autoExecMacros = findAutoExecMacros(fileContent)
        )
    }

    private fun readFileContent(file: File): String {
        return try {
            // Read up to 2MB to avoid OOM on massive files
            val maxLength = 2 * 1024 * 1024L
            val bytesToRead = minOf(file.length(), maxLength).toInt()
            val bytes = ByteArray(bytesToRead)
            file.inputStream().use { it.read(bytes) }
            String(bytes, Charsets.ISO_8859_1)
        } catch (e: Exception) {
            ""
        }
    }

    private fun findSuspiciousPatterns(content: String): List<String> {
        val found = mutableListOf<String>()
        maliciousPatterns.forEach { pattern ->
            if (pattern.matcher(content).find()) {
                found.add(pattern.pattern())
            }
        }
        return found
    }

    private fun calculateObfuscationScore(content: String): Float {
        var score = 0f
        obfuscationPatterns.forEach { pattern ->
            val matcher = pattern.matcher(content)
            var count = 0
            while (matcher.find()) count++
            score += count * 0.1f
        }
        return score.coerceIn(0f, 1f)
    }

    private fun findApiCalls(content: String): List<String> {
        val found = mutableListOf<String>()
        suspiciousApiCalls.forEach { api ->
            if (content.contains(api, ignoreCase = true)) {
                found.add(api)
            }
        }
        return found
    }

    private fun countMacros(content: String): Int {
        val subPattern = Regex("Sub\\s+(\\w+)", RegexOption.IGNORE_CASE)
        val functionPattern = Regex("Function\\s+(\\w+)", RegexOption.IGNORE_CASE)
        return subPattern.findAll(content).count() + functionPattern.findAll(content).count()
    }

    private fun findAutoExecMacros(content: String): List<String> {
        val autoExecPatterns = listOf("AutoOpen", "Auto_Open", "Document_Open", "Workbook_Open", "AutoExec")
        return autoExecPatterns.filter { content.contains(it, ignoreCase = true) }
    }

    private fun calculateConfidenceScore(
        suspiciousPatternCount: Int,
        obfuscationScore: Float,
        apiCallCount: Int
    ): Int {
        var score = 0
        score += suspiciousPatternCount * 15
        score += (obfuscationScore * 30).toInt()
        score += apiCallCount * 5
        return score.coerceIn(0, 100)
    }
}

data class MacroAnalysisResult(
    val hasMacros: Boolean,
    val macroCount: Int,
    val isMalicious: Boolean,
    val confidenceScore: Int,
    val suspiciousPatterns: List<String>,
    val obfuscationLevel: Int,
    val apiCalls: List<String>,
    val autoExecMacros: List<String>
)
