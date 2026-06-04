package com.shadowinspect.app.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadowinspect.app.presentation.theme.NeonCyan
import kotlinx.coroutines.delay

@Composable
fun HackerLoadingAnimation(
    modifier: Modifier = Modifier,
    statusText: String = "EXECUTING ADVANCED SCAN..."
) {
    var textFrame by remember { mutableStateOf("") }
    val chars = listOf("0", "1", "A", "F", "X", "$", "#", "!", ">")
    
    // Simulate Matrix/Hacker text effect
    LaunchedEffect(Unit) {
        while (true) {
            val randomText = (1..20).map { chars.random() }.joinToString("")
            textFrame = randomText
            delay(50)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF0D1B2A)) // Dark background
            .border(1.dp, NeonCyan.copy(alpha = alpha), RoundedCornerShape(4.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = textFrame,
            color = NeonCyan.copy(alpha = 0.5f),
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            letterSpacing = 4.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Blinking cursor effect
            Box(
                modifier = Modifier
                    .size(width = 8.dp, height = 16.dp)
                    .background(NeonCyan.copy(alpha = alpha))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statusText,
                color = NeonCyan,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
