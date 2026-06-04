package com.shadowinspect.app.presentation.goodbye

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadowinspect.app.presentation.theme.NeonCyan
import com.shadowinspect.app.presentation.theme.NeonRed
import kotlinx.coroutines.delay

@Composable
fun GoodbyeScreen(
    agentHandle: String?,
    onBackToLogin: () -> Unit
) {
    var displayedMessage by remember { mutableStateOf("") }
    val fullMessages = listOf(
        "DE-AUTHORIZING AGENT...",
        "WIPING SESSION CACHE...",
        "CONNECTION TERMINATED.",
        "Goodbye, Agent ${agentHandle?.ifBlank { "UNKNOWN" } ?: "UNKNOWN"}."
    )

    LaunchedEffect(Unit) {
        fullMessages.forEach { msg ->
            displayedMessage = ""
            msg.forEachIndexed { index, _ ->
                displayedMessage = msg.substring(0, index + 1)
                delay(30)
            }
            delay(800)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "> $displayedMessage",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 24.sp
                ),
                color = if (displayedMessage.contains("TERMINATED")) NeonRed else NeonCyan
            )
            
            Spacer(Modifier.height(32.dp))
            
            if (displayedMessage.contains("Goodbye")) {
                Button(
                    onClick = onBackToLogin,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text(
                        text = "RETURN TO GATEWAY", 
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
