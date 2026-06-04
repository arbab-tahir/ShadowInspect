package com.shadowinspect.app.presentation.start

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shadowinspect.app.presentation.session.SessionViewModel
import com.shadowinspect.app.presentation.theme.NeonCyan
import com.shadowinspect.app.presentation.theme.NeonGreen

@Composable
fun StartScreen(
    onGoToLogin: () -> Unit,
    onGoToSignUp: () -> Unit,
    onAlreadyLoggedIn: () -> Unit,
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val isLoggedIn by sessionViewModel.isLoggedIn.collectAsState()

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) onAlreadyLoggedIn()
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
                text = "SECURE GATEWAY",
                style = MaterialTheme.typography.headlineLarge,
                color = NeonGreen
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Choose your entry vector, Agent.",
                style = MaterialTheme.typography.titleLarge,
                color = NeonCyan
            )

            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onGoToLogin,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
            ) {
                Text("LOGIN", color = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onGoToSignUp,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("CREATE AGENT", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

