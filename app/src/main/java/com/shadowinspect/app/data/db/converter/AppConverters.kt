package com.shadowinspect.app.data.db.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.shadowinspect.app.domain.education.LessonCategory
import com.shadowinspect.app.domain.education.LessonDifficulty
import com.shadowinspect.app.domain.mitre.*
import com.shadowinspect.app.domain.model.*
import com.shadowinspect.app.domain.widgets.WidgetSize
import com.shadowinspect.app.domain.widgets.WidgetType
import java.util.Date

/**
 * Unified "Super Converter" class for ShadowInspect.
 * This handles ALL Room type conversions in one place to prevent 
 * type erasure conflicts and ensuring build stability.
 */
class AppConverters {
    private val gson = Gson()

    // --- Basic Types ---

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        if (value.isNullOrEmpty()) return emptyList()
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    // --- MITRE Domain Types ---

    @TypeConverter
    fun fromMitreTechniqueList(value: List<MitreTechnique>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toMitreTechniqueList(value: String?): List<MitreTechnique>? {
        if (value.isNullOrEmpty()) return emptyList()
        val listType = object : TypeToken<List<MitreTechnique>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun fromPermissionMappingList(value: List<PermissionMapping>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toPermissionMappingList(value: String?): List<PermissionMapping>? {
        if (value.isNullOrEmpty()) return emptyList()
        val listType = object : TypeToken<List<PermissionMapping>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun fromRiskFactorList(value: List<RiskFactor>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toRiskFactorList(value: String?): List<RiskFactor>? {
        if (value.isNullOrEmpty()) return emptyList()
        val listType = object : TypeToken<List<RiskFactor>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun fromPredictedThreatList(value: List<PredictedThreat>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toPredictedThreatList(value: String?): List<PredictedThreat>? {
        if (value.isNullOrEmpty()) return emptyList()
        val listType = object : TypeToken<List<PredictedThreat>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    // --- Widget Types ---

    @TypeConverter
    fun fromWidgetType(value: WidgetType): String {
        return value.name
    }

    @TypeConverter
    fun toWidgetType(value: String): WidgetType {
        return WidgetType.valueOf(value)
    }

    @TypeConverter
    fun fromWidgetSize(value: WidgetSize): String {
        return value.name
    }

    @TypeConverter
    fun toWidgetSize(value: String): WidgetSize {
        return WidgetSize.valueOf(value)
    }

    // --- Document Types ---

    @TypeConverter
    fun fromDocumentType(value: DocumentType): String {
        return value.name
    }

    @TypeConverter
    fun toDocumentType(value: String): DocumentType {
        return DocumentType.valueOf(value)
    }

    @TypeConverter
    fun fromDocumentThreatTypeList(value: List<DocumentThreatType>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toDocumentThreatTypeList(value: String?): List<DocumentThreatType>? {
        if (value.isNullOrEmpty()) return emptyList()
        val listType = object : TypeToken<List<DocumentThreatType>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    // --- Image Types ---

    @TypeConverter
    fun fromImageType(value: ImageType): String = value.name

    @TypeConverter
    fun toImageType(value: String): ImageType = ImageType.valueOf(value)

    @TypeConverter
    fun fromImageThreatTypeList(value: List<ImageThreatType>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toImageThreatTypeList(value: String?): List<ImageThreatType>? {
        if (value.isNullOrEmpty()) return emptyList()
        val listType = object : TypeToken<List<ImageThreatType>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun fromExifMetadata(value: ExifMetadata?): String? = value?.let { gson.toJson(it) }

    @TypeConverter
    fun toExifMetadata(value: String?): ExifMetadata? {
        if (value.isNullOrEmpty()) return null
        return gson.fromJson(value, ExifMetadata::class.java)
    }

    @TypeConverter
    fun fromSteganographyResult(value: SteganographyResult?): String? = value?.let { gson.toJson(it) }

    @TypeConverter
    fun toSteganographyResult(value: String?): SteganographyResult? {
        if (value.isNullOrEmpty()) return null
        return gson.fromJson(value, SteganographyResult::class.java)
    }

    @TypeConverter
    fun fromGpsCoordinates(value: GpsCoordinates?): String? = value?.let { gson.toJson(it) }

    @TypeConverter
    fun toGpsCoordinates(value: String?): GpsCoordinates? {
        if (value.isNullOrEmpty()) return null
        return gson.fromJson(value, GpsCoordinates::class.java)
    }

    // --- Education Types ---

    @TypeConverter
    fun fromLessonCategory(value: LessonCategory): String = value.name

    @TypeConverter
    fun toLessonCategory(value: String): LessonCategory = LessonCategory.valueOf(value)

    @TypeConverter
    fun fromLessonDifficulty(value: LessonDifficulty): String = value.name

    @TypeConverter
    fun toLessonDifficulty(value: String): LessonDifficulty = LessonDifficulty.valueOf(value)

    @TypeConverter
    fun fromLongList(value: List<Long>?): String? = value?.let { gson.toJson(it) }

    @TypeConverter
    fun toLongList(value: String?): List<Long>? {
        if (value.isNullOrEmpty()) return emptyList()
        val listType = object : TypeToken<List<Long>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun fromLongIntMap(value: Map<Long, Int>?): String? = value?.let { gson.toJson(it) }

    @TypeConverter
    fun toLongIntMap(value: String?): Map<Long, Int> {
        if (value.isNullOrEmpty()) return mapOf()
        val mapType = object : TypeToken<Map<Long, Int>>() {}.type
        return gson.fromJson(value, mapType) ?: mapOf()
    }

    @TypeConverter
    fun fromLearningLevel(value: com.shadowinspect.app.domain.education.LearningLevel): String = value.name

    @TypeConverter
    fun toLearningLevel(value: String): com.shadowinspect.app.domain.education.LearningLevel = com.shadowinspect.app.domain.education.LearningLevel.valueOf(value)

    @TypeConverter
    fun fromIntList(value: List<Int>?): String? = value?.let { gson.toJson(it) }

    @TypeConverter
    fun toIntList(value: String?): List<Int>? {
        if (value.isNullOrEmpty()) return emptyList()
        val listType = object : TypeToken<List<Int>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    // --- Document Forensic Types ---

    @TypeConverter
    fun fromCveMatchList(value: List<CveMatch>?): String? = value?.let { gson.toJson(it) }

    @TypeConverter
    fun toCveMatchList(value: String?): List<CveMatch>? {
        if (value.isNullOrEmpty()) return emptyList()
        val listType = object : TypeToken<List<CveMatch>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun fromPdfPermissions(value: PdfPermissions?): String? = value?.let { gson.toJson(it) }

    @TypeConverter
    fun toPdfPermissions(value: String?): PdfPermissions? {
        if (value.isNullOrEmpty()) return null
        return gson.fromJson(value, PdfPermissions::class.java)
    }

    @TypeConverter
    fun fromStringListMap(value: Map<String, List<String>>?): String? = value?.let { gson.toJson(it) }

    @TypeConverter
    fun toStringListMap(value: String?): Map<String, List<String>> {
        if (value.isNullOrEmpty()) return emptyMap()
        val mapType = object : TypeToken<Map<String, List<String>>>() {}.type
        return gson.fromJson(value, mapType) ?: emptyMap()
    }
}
