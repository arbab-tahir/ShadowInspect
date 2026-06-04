package com.shadowinspect.app.presentation.start

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.shadowinspect.app.presentation.theme.NeonCyan
import com.shadowinspect.app.presentation.theme.NeonGreen
import kotlinx.coroutines.delay

@Composable
fun WelcomeScreen(
    agentHandle: String?,
    onAnimationFinished: () -> Unit
) {
    val fullText = "Access granted, Agent ${agentHandle ?: "Unknown"}..."
    var displayedText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        fullText.forEachIndexed { index, _ ->
            displayedText = fullText.substring(0, index + 1)
            delay(50) // Adjust speed of typing here
        }
        delay(1500) // Pause at the end
        onAnimationFinished()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "> $displayedText",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = FontFamily.Monospace
                ),
                color = NeonGreen
            )
            
            Spacer(Modifier.height(16.dp))
            
            Text(
                text = "INITIALIZING SYSTEM...",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace
                ),
                color = NeonCyan.copy(alpha = 0.7f)
            )
        }
    }
}
