package com.shadowinspect.app.presentation.screens.education

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shadowinspect.app.domain.education.Badge
import com.shadowinspect.app.presentation.theme.CyberSurface
import com.shadowinspect.app.presentation.theme.NeonCyan
import com.shadowinspect.app.presentation.theme.NeonGreen
import com.shadowinspect.app.presentation.theme.NeonRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgesScreen(
    onNavigateBack: () -> Unit,
    viewModel: BadgesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ACHIEVEMENT BADGES",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonGreen,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Stats row
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BadgeStat(value = "${state.earnedBadgeIds.size}", label = "EARNED")
                    BadgeStat(value = "${state.totalBadges}", label = "TOTAL")
                    val pct = if (state.totalBadges > 0) state.earnedBadgeIds.size * 100 / state.totalBadges else 0
                    BadgeStat(value = "$pct%", label = "COLLECTED")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (state.allBadges.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonGreen)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.allBadges) { badge ->
                        BadgeCard(
                            badge = badge,
                            isEarned = state.earnedBadgeIds.contains(badge.id),
                            isNew = state.newBadgeIds.contains(badge.id)
                        )
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
fun BadgeStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.headlineSmall, color = NeonGreen, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), letterSpacing = 1.sp)
    }
}

@Composable
fun BadgeCard(badge: Badge, isEarned: Boolean, isNew: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isEarned) NeonGreen.copy(alpha = 0.1f) else CyberSurface.copy(alpha = 0.6f)
        ),
        border = BorderStroke(1.dp, if (isEarned) NeonGreen.copy(alpha = 0.4f) else NeonGreen.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(14.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                // Icon circle
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (isEarned) NeonGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isEarned) badge.iconRes else "🔒",
                        fontSize = 28.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = badge.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isEarned) NeonGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (!isEarned) {
                    Text(
                        text = badge.requirement,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 2
                    )
                } else {
                    Text(
                        text = "+${badge.xpReward} XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonGreen.copy(alpha = 0.7f),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            // NEW badge indicator
            if (isNew) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(6.dp))
                        .background(NeonRed)
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(text = "NEW", style = MaterialTheme.typography.labelSmall, color = Color.White, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}
