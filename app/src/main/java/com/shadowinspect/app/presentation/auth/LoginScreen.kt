package com.shadowinspect.app.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shadowinspect.app.presentation.theme.NeonCyan
import com.shadowinspect.app.presentation.theme.NeonGreen
import com.shadowinspect.app.presentation.theme.NeonRed

@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }

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
                text = "SHADOWINSPECT",
                style = MaterialTheme.typography.headlineLarge,
                color = NeonGreen
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "AUTHENTICATE AGENT",
                style = MaterialTheme.typography.titleLarge,
                color = NeonCyan
            )
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = uiState.agentHandle,
                onValueChange = viewModel::onAgentHandleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Agent handle") },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.Black),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedBorderColor = NeonGreen,
                    unfocusedBorderColor = NeonGreen.copy(alpha = 0.5f),
                    focusedLabelColor = NeonGreen,
                    unfocusedLabelColor = NeonGreen.copy(alpha = 0.7f),
                    cursorColor = NeonGreen,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = uiState.accessKey,
                onValueChange = viewModel::onAccessKeyChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Access key") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (passwordVisible)
                        Icons.Filled.Visibility
                    else Icons.Filled.VisibilityOff
 
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = null, tint = NeonCyan)
                    }
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.Black),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = NeonCyan.copy(alpha = 0.5f),
                    focusedLabelColor = NeonCyan,
                    unfocusedLabelColor = NeonCyan.copy(alpha = 0.7f),
                    cursorColor = NeonCyan,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            uiState.error?.let { err ->
                Spacer(Modifier.height(12.dp))
                Text(text = err, color = NeonRed)
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { viewModel.login(onSuccess = onLoginSuccess) },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
            ) {
                Text(
                    text = if (uiState.isLoading) "AUTHORIZING..." else "ENTER",
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
