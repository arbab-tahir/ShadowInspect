package com.shadowinspect.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Core cyber / neon palette for ShadowInspect.
val CyberBackground = Color(0xFF0A0A0A)
val CyberSurface = Color(0xFF1A1A1A)
val Surface = Color(0xFF1A1A1A)
val SurfaceLight = Color(0xFF252525)
val NeonGreen = Color(0xFF00FF9D)
val NeonCyan = Color(0xFF00E5FF)
val NeonRed = Color(0xFFFF3B3B)
val NeonYellow = Color(0xFFFFC107)
val DimWhite = Color(0xFFB0B0B0)
val Background = Color(0xFF0A0A0A)

val RiskLow = Color(0xFF00FF9D)
val RiskMedium = Color(0xFFFFB300) // Amber/Yellow
val RiskHigh = Color(0xFFFF3B3B)

private val CyberDarkColorScheme: ColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = CyberBackground,
    secondary = NeonCyan,
    onSecondary = CyberBackground,
    tertiary = NeonRed,
    onTertiary = CyberBackground,
    background = CyberBackground,
    onBackground = Color.White,
    surface = CyberSurface,
    onSurface = Color.White,
    error = NeonRed,
    onError = CyberBackground
)

@Composable
fun ShadowInspectTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (useDarkTheme) {
        CyberDarkColorScheme
    } else {
        // For now we mirror dark styling in light mode.
        CyberDarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CyberTypography,
        content = content
    )
}

