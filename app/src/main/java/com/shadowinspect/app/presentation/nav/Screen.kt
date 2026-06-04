package com.shadowinspect.app.presentation.nav

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Start : Screen("start")
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Welcome : Screen("welcome/{agent}") {
        fun createRoute(agent: String) = "welcome/$agent"
    }
    object Home : Screen("home")
    object UrlScan : Screen("url_scan")
    object FileAnalysis : Screen("file_analysis")
    object PhoneScan : Screen("phone_scan")
    object Reports : Screen("reports")
    object MitreReports : Screen("mitre_reports")
    object MitreBrowser : Screen("mitre_browser")
    object MitreTechniqueDetail : Screen("mitre_technique_detail/{techniqueId}") {
        fun createRoute(techniqueId: String) = "mitre_technique_detail/$techniqueId"
    }
    object ReportDetail : Screen("report_detail/{reportId}") {
        fun createRoute(reportId: String) = "report_detail/$reportId"
    }
    object Goodbye : Screen("goodbye?agent={agent}") {
        fun createRoute(agent: String) = "goodbye?agent=$agent"
    }
    object ResearchExport : Screen("research_export")
    object Dashboard : Screen("dashboard")
    object WidgetSettings : Screen("widget_settings")
    object Export : Screen("export")
    object Settings : Screen("settings")
    object DocumentScan : Screen("document_scan")
    object ImageScan : Screen("image_scan")
    object MLSettings : Screen("ml_settings")
    // Module 15 — Education
    object Education : Screen("education")
    object LearningPath : Screen("learning_path/{level}") {
        fun passLevel(level: String): String = "learning_path/$level"
    }
    object LessonDetail : Screen("lesson_detail/{lessonId}") {
        fun passId(lessonId: Long): String = "lesson_detail/$lessonId"
    }
    object Badges : Screen("badges")
    object MitreExplorer : Screen("mitre_explorer")

    // Legal & Security 
    object LegalDocument : Screen("legal_document/{type}") {
        fun passType(type: String): String = "legal_document/$type"
    }
    object PrivacyPolicy : Screen("privacy_policy")
    object SecuritySettings : Screen("security_settings")
    object AdvancedSettings : Screen("advanced_settings")
    object StorageSettings : Screen("storage_settings")
    object PermissionsExplained : Screen("permissions_explained")
    object Encryption : Screen("encryption")
    object ReportVulnerability : Screen("report_vulnerability")
    object SystemDiagnostics : Screen("system_diagnostics")
}
