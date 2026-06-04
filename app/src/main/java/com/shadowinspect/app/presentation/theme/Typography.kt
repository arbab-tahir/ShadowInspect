package com.shadowinspect.app.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Cyber theme typography with clear hierarchy
val CyberTypography = Typography(
    // For main titles like "Terms of Service", "Privacy Policy"
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        color = NeonGreen
    ),
    
    // For section headers like "1. Acceptance of Terms"
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        color = NeonGreen.copy(alpha = 0.9f)
    ),
    
    // For subsection headers like "### Description of Service"
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        color = NeonGreen.copy(alpha = 0.8f)
    ),
    
    // For regular body text
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = Color.White
    ),
    
    // For smaller text (disclaimers, footnotes)
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = Color.White.copy(alpha = 0.7f)
    ),
    
    // For legal text, fine print
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Light,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = Color.White.copy(alpha = 0.5f)
    )
)
