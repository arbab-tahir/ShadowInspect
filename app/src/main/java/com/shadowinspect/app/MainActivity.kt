package com.shadowinspect.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.shadowinspect.app.presentation.auth.LoginScreen
import com.shadowinspect.app.presentation.auth.SignUpScreen
import com.shadowinspect.app.presentation.goodbye.GoodbyeScreen
import com.shadowinspect.app.presentation.home.HomeScreen
import com.shadowinspect.app.presentation.nav.Screen
import com.shadowinspect.app.presentation.reports.ReportDetailScreen
import com.shadowinspect.app.presentation.reports.ReportsScreen
import com.shadowinspect.app.presentation.scan.FileScanScreen
import com.shadowinspect.app.presentation.scan.PhoneScanScreen
import com.shadowinspect.app.presentation.scan.UrlScanScreen
import com.shadowinspect.app.presentation.reports.MitreReportsScreen
import com.shadowinspect.app.presentation.screens.mitre.MitreBrowserScreen
import com.shadowinspect.app.presentation.screens.mitre.MitreTechniqueDetailScreen
import com.shadowinspect.app.presentation.start.StartScreen
import com.shadowinspect.app.presentation.start.WelcomeScreen
import com.shadowinspect.app.presentation.start.SplashScreen
import com.shadowinspect.app.presentation.screens.widgets.WidgetSettingsScreen
import com.shadowinspect.app.presentation.screens.export.ExportScreen
import com.shadowinspect.app.presentation.screens.settings.*
import com.shadowinspect.app.presentation.screens.documentscan.DocumentScanScreen
import com.shadowinspect.app.presentation.screens.imagescan.ImageScanScreen
import com.shadowinspect.app.presentation.screens.education.*
import com.shadowinspect.app.presentation.theme.ShadowInspectTheme
import dagger.hilt.android.AndroidEntryPoint
import android.content.Intent

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Global crash handler for debugging
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("GLOBAL_CRASH", "CRASH in ${thread.name}: ${throwable.message}", throwable)
            runOnUiThread {
                android.widget.Toast.makeText(this, "Crash: ${throwable.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }

        setContent {
            ShadowInspectTheme {
                ShadowInspectApp()
            }
        }
    }

}

@Composable
fun ShadowInspectApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToStart = {
                    navController.navigate(Screen.Start.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Start.route) {
            StartScreen(
                onGoToLogin = { navController.navigate(Screen.Login.route) },
                onGoToSignUp = { navController.navigate(Screen.SignUp.route) },
                onAlreadyLoggedIn = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Start.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { agentHandle ->
                    navController.navigate(Screen.Welcome.createRoute(agentHandle)) {
                        popUpTo(Screen.Start.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.SignUp.route) {
            SignUpScreen(
                onSignUpSuccess = { agentHandle ->
                    navController.navigate(Screen.Welcome.createRoute(agentHandle)) {
                        popUpTo(Screen.Start.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Welcome.route,
            arguments = listOf(
                navArgument("agent") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val agent = backStackEntry.arguments?.getString("agent")
            WelcomeScreen(
                agentHandle = agent,
                onAnimationFinished = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToUrlScan = { navController.navigate(Screen.UrlScan.route) },
                onNavigateToFileAnalysis = { navController.navigate(Screen.FileAnalysis.route) },
                onNavigateToPhoneScan = { navController.navigate(Screen.PhoneScan.route) },
                onNavigateToDocumentScan = { navController.navigate(Screen.DocumentScan.route) },
                onNavigateToImageScan = { navController.navigate(Screen.ImageScan.route) },
                onNavigateToReports = { navController.navigate(Screen.Reports.route) },
                onNavigateToMitreBrowser = { navController.navigate(Screen.MitreBrowser.route) },
                onNavigateToResearch = { navController.navigate(Screen.ResearchExport.route) },
                onNavigateToDashboard = { navController.navigate(Screen.Dashboard.route) },
                onNavigateToEducation = { navController.navigate(Screen.Education.route) },
                onLogout = { agentHandle ->
                    navController.navigate(Screen.Goodbye.createRoute(agentHandle)) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        
        composable(Screen.Dashboard.route) {
            com.shadowinspect.app.presentation.screens.dashboard.DashboardScreen(
                onNavigateToScan = { scanType ->
                    when (scanType) {
                        "url" -> navController.navigate(Screen.UrlScan.route)
                        "apk" -> navController.navigate(Screen.FileAnalysis.route)
                        "phone" -> navController.navigate(Screen.PhoneScan.route)
                        "image" -> navController.navigate(Screen.ImageScan.route)
                        "document" -> navController.navigate(Screen.DocumentScan.route)
                    }
                },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToWidgetSettings = { navController.navigate(Screen.WidgetSettings.route) },
                onNavigateToExport = { navController.navigate(Screen.Export.route) }
            )
        }

        composable(Screen.WidgetSettings.route) {
            com.shadowinspect.app.presentation.screens.widgets.WidgetSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMLSettings = { navController.navigate(Screen.MLSettings.route) },
                onNavigateToLegal = { type -> 
                    if (type == "privacy") {
                        navController.navigate(Screen.PrivacyPolicy.route)
                    } else {
                        navController.navigate(Screen.LegalDocument.passType(type))
                    }
                },
                onNavigateToSecurity = { navController.navigate(Screen.SecuritySettings.route) },
                onNavigateToLicenses = { navController.navigate(Screen.LegalDocument.passType("licenses")) },
                onNavigateToAdvanced = { navController.navigate(Screen.AdvancedSettings.route) },
                onNavigateToStorage = { navController.navigate(Screen.StorageSettings.route) }
            )
        }

        composable(Screen.PrivacyPolicy.route) {
            PrivacyPolicyScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SecuritySettings.route) {
            SecuritySettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPermissions = { navController.navigate(Screen.PermissionsExplained.route) },
                onNavigateToEncryption = { navController.navigate(Screen.Encryption.route) },
                onNavigateToReportVulnerability = { navController.navigate(Screen.ReportVulnerability.route) }
            )
        }

        composable(Screen.AdvancedSettings.route) {
            AdvancedSettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMLSettings = { navController.navigate(Screen.MLSettings.route) },
                onNavigateToDiagnostics = { navController.navigate(Screen.SystemDiagnostics.route) }
            )
        }

        composable(Screen.SystemDiagnostics.route) {
            AdvancedScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.StorageSettings.route) {
            StorageSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PermissionsExplained.route) {
            AppPermissionsExplainedScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Encryption.route) {
            DataEncryptionScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ReportVulnerability.route) {
            ReportVulnerabilityScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.MLSettings.route) {
            com.shadowinspect.app.presentation.screens.settings.MLSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.DocumentScan.route) {
            DocumentScanScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ImageScan.route) {
            ImageScanScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Module 15 — Education
        composable(Screen.Education.route) {
            EducationMainScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLesson = { lessonId ->
                    navController.navigate(Screen.LessonDetail.passId(lessonId))
                },
                onNavigateToBadges = { navController.navigate(Screen.Badges.route) },
                onNavigateToMitreExplorer = { navController.navigate(Screen.MitreExplorer.route) },
                onNavigateToPath = { level ->
                    navController.navigate(Screen.LearningPath.passLevel(level.name))
                }
            )
        }

        composable(
            route = Screen.LearningPath.route,
            arguments = listOf(navArgument("level") { type = NavType.StringType })
        ) { backStackEntry ->
            val levelStr = backStackEntry.arguments?.getString("level") ?: com.shadowinspect.app.domain.education.LearningLevel.BEGINNER.name
            val level = try {
                com.shadowinspect.app.domain.education.LearningLevel.valueOf(levelStr)
            } catch (e: Exception) {
                com.shadowinspect.app.domain.education.LearningLevel.BEGINNER
            }
            LearningPathScreen(
                level = level,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLesson = { lessonId ->
                    navController.navigate(Screen.LessonDetail.passId(lessonId))
                }
            )
        }

        composable(
            route = Screen.LessonDetail.route,
            arguments = listOf(navArgument("lessonId") { type = NavType.StringType })
        ) { backStackEntry ->
            val lessonId = backStackEntry.arguments?.getString("lessonId")?.toLongOrNull() ?: 0L
            LessonDetailScreen(
                lessonId = lessonId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToNextLesson = { nextId ->
                    navController.popBackStack()
                    if (nextId != -1L) {
                        navController.navigate(Screen.LessonDetail.passId(nextId))
                    }
                },
                onNavigateToNextPath = { nextLevel ->
                    // Go all the way back up to Education screen, then into the new level
                    navController.popBackStack(Screen.Education.route, false)
                    navController.navigate(Screen.LearningPath.passLevel(nextLevel.name))
                },
                onNavigateToDashboard = {
                    // Go all the way back up to Education screen
                    navController.popBackStack(Screen.Education.route, false)
                }
            )
        }

        composable(Screen.Badges.route) {
            BadgesScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.MitreExplorer.route) {
            MitreExplorerScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.UrlScan.route) {
            UrlScanScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.FileAnalysis.route) {
            FileScanScreen(
                onBack = { navController.popBackStack() },
                onNavigateToReports = { navController.navigate(Screen.MitreReports.route) }
            )
        }

        composable(Screen.PhoneScan.route) {
            PhoneScanScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Reports.route) {
            ReportsScreen(
                onReportClick = { reportId ->
                    navController.navigate(Screen.ReportDetail.createRoute(reportId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Export.route) {
            ExportScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.MitreReports.route) {
            MitreReportsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.MitreBrowser.route) {
            MitreBrowserScreen(
                onNavigateToDetail = { techniqueId ->
                    navController.navigate(Screen.MitreTechniqueDetail.createRoute(techniqueId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.MitreTechniqueDetail.route,
            arguments = listOf(
                navArgument("techniqueId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val techniqueId = backStackEntry.arguments?.getString("techniqueId") ?: ""
            MitreTechniqueDetailScreen(
                techniqueId = techniqueId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ResearchExport.route) {
            com.shadowinspect.app.presentation.reports.ResearchExportScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ReportDetail.route,
            arguments = listOf(
                navArgument("reportId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val reportId = backStackEntry.arguments?.getString("reportId")
            ReportDetailScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Goodbye.route,
            arguments = listOf(
                navArgument("agent") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val agent = backStackEntry.arguments?.getString("agent")
            GoodbyeScreen(
                agentHandle = agent,
                onBackToLogin = {
                    navController.navigate(Screen.Start.route) {
                        popUpTo(Screen.Goodbye.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = Screen.LegalDocument.route,
            arguments = listOf(
                navArgument("type") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: ""
            LegalDocumentScreen(
                documentType = type,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
