package com.shadowinspect.app.presentation.start

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadowinspect.app.R
import com.shadowinspect.app.presentation.theme.CyberBackground
import com.shadowinspect.app.presentation.theme.NeonCyan
import com.shadowinspect.app.presentation.theme.NeonGreen
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onNavigateToStart: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    var titleVisible by remember { mutableStateOf(false) }
    var displayedTitle by remember { mutableStateOf("") }
    val fullTitle = "SHADOWINSPECT"

    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000)
    )

    val scaleAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.5f,
        animationSpec = tween(durationMillis = 1000)
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(800) // Wait for icon animation
        
        titleVisible = true
        fullTitle.forEachIndexed { index, _ ->
            displayedTitle = fullTitle.substring(0, index + 1)
            delay(50)
        }
        
        delay(1200) // Hold before navigating
        onNavigateToStart()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_launcher), // This will use the new vector graphic we create
                contentDescription = "ShadowInspect Logo",
                tint = NeonGreen,
                modifier = Modifier
                    .size(120.dp)
                    .scale(scaleAnim.value)
                    .alpha(alphaAnim.value)
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (titleVisible) {
                Text(
                    text = displayedTitle,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp
                    ),
                    color = NeonGreen
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Advanced Threat Intelligence",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                ),
                color = NeonCyan,
                modifier = Modifier.alpha(alphaAnim.value)
            )
        }
    }
}
