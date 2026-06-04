package com.shadowinspect.app.presentation.screens.education

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
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
import com.shadowinspect.app.domain.education.LearningLevel
import com.shadowinspect.app.domain.education.SecurityLesson
import com.shadowinspect.app.presentation.theme.CyberSurface
import com.shadowinspect.app.presentation.theme.NeonCyan
import com.shadowinspect.app.presentation.theme.NeonGreen
import com.shadowinspect.app.presentation.theme.NeonRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningPathScreen(
    level: LearningLevel,
    onNavigateBack: () -> Unit,
    onNavigateToLesson: (Long) -> Unit,
    viewModel: LearningPathViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(level) {
        viewModel.loadLevel(level)
    }

    LaunchedEffect(Unit) {
        viewModel.refreshProgress() // Refresh on resume
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "${level.name} PATH",
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
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonGreen)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(bottom = 32.dp, top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                
                item {
                    Text(
                        text = "Complete lessons in order to master this level.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                itemsIndexed(state.lessons) { index, lesson ->
                    val isCompleted = state.completedLessonIds.contains(lesson.id)
                    // Lesson is unlocked if it's the first one, or the previous one is completed
                    val isUnlocked = index == 0 || state.completedLessonIds.contains(state.lessons[index - 1].id)

                    PathNode(
                        lesson = lesson,
                        isCompleted = isCompleted,
                        isUnlocked = isUnlocked,
                        isLast = index == state.lessons.size - 1,
                        isNextToPlay = isUnlocked && !isCompleted,
                        onClick = {
                            if (isUnlocked) onNavigateToLesson(lesson.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PathNode(
    lesson: SecurityLesson,
    isCompleted: Boolean,
    isUnlocked: Boolean,
    isNextToPlay: Boolean,
    isLast: Boolean,
    onClick: () -> Unit
) {
    val nodeColor by animateColorAsState(
        targetValue = when {
            isCompleted -> NeonGreen
            isNextToPlay -> NeonCyan
            else -> Color.Gray.copy(alpha = 0.5f)
        }, label = "nodeColor"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Node
        Box(
            modifier = Modifier
                .size(if (isNextToPlay) 80.dp else 64.dp)
                .clip(CircleShape)
                .background(if (isNextToPlay) nodeColor.copy(alpha = 0.15f) else nodeColor.copy(alpha = 0.05f))
                .border(
                    width = if (isNextToPlay) 3.dp else 2.dp,
                    color = nodeColor,
                    shape = CircleShape
                )
                .clickable(enabled = isUnlocked, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            when {
                isCompleted -> Icon(Icons.Default.Check, contentDescription = "Done", tint = nodeColor, modifier = Modifier.size(32.dp))
                isNextToPlay -> Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = nodeColor, modifier = Modifier.size(40.dp))
                else -> Icon(Icons.Default.Lock, contentDescription = "Locked", tint = nodeColor, modifier = Modifier.size(28.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Title
        Text(
            text = lesson.title,
            style = MaterialTheme.typography.labelMedium,
            color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            fontWeight = if (isNextToPlay) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(180.dp)
        )

        if (!isLast) {
            // Connecting Line
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .background(if (isCompleted) NeonGreen.copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.2f))
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
