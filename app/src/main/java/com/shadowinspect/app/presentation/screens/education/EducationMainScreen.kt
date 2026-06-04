package com.shadowinspect.app.presentation.screens.education

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shadowinspect.app.domain.education.DailyTip
import com.shadowinspect.app.domain.education.LessonCategory
import com.shadowinspect.app.domain.education.LessonDifficulty
import com.shadowinspect.app.domain.education.SecurityLesson
import com.shadowinspect.app.presentation.theme.CyberSurface
import com.shadowinspect.app.presentation.theme.NeonCyan
import com.shadowinspect.app.presentation.theme.NeonGreen
import com.shadowinspect.app.presentation.theme.NeonRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EducationMainScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLesson: (Long) -> Unit,
    onNavigateToBadges: () -> Unit,
    onNavigateToMitreExplorer: () -> Unit,
    onNavigateToPath: (com.shadowinspect.app.domain.education.LearningLevel) -> Unit,
    viewModel: EducationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SECURITY EDUCATION",
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                // Progress Card
                item {
                    EduProgressCard(
                        xp = state.totalXp,
                        streak = state.currentStreak,
                        completedLessons = state.completedLessons.size,
                        totalLessons = state.totalLessons,
                        onViewBadges = onNavigateToBadges
                    )
                }

                // Celebration Banner
                if (state.completedLessons.size == state.totalLessons && state.totalLessons > 0) {
                    item { CelebrationBanner() }
                }

                // Daily Tip
                state.dailyTip?.let { tip ->
                    item { DailyTipCard(tip = tip) }
                }

                // MITRE Explorer banner
                item {
                    MitreExplorerBanner(onClick = onNavigateToMitreExplorer)
                }

                // Learning Paths
                if (state.learningPaths.isNotEmpty()) {
                    item {
                        Text(
                            text = "LEARNING PATHS",
                            style = MaterialTheme.typography.titleSmall,
                            color = NeonCyan,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(state.learningPaths) { path ->
                        val isLocked = state.totalXp < path.requiredXp
                        LearningPathCard(
                            path = path,
                            isLocked = isLocked,
                            onClick = { 
                                if (!isLocked) onNavigateToPath(path.level) 
                            }
                        )
                    }
                }

                // Category filters
                item {
                    Text(
                        text = "EXPLORE BY TOPIC",
                        style = MaterialTheme.typography.titleSmall,
                        color = NeonCyan,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            EduCategoryChip(
                                label = "ALL",
                                isSelected = state.selectedCategory == null,
                                onClick = { viewModel.selectCategory(null) }
                            )
                        }
                        items(LessonCategory.values()) { category ->
                            EduCategoryChip(
                                label = categoryLabel(category),
                                isSelected = state.selectedCategory == category,
                                onClick = { viewModel.selectCategory(category) }
                            )
                        }
                    }
                }

                // Lessons list
                if (state.filteredLessons.isEmpty()) {
                    item {
                        Text(
                            text = "No lessons in this category yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    items(state.filteredLessons) { lesson ->
                        LessonCard(
                            lesson = lesson,
                            isCompleted = state.completedLessons.contains(lesson.id),
                            onClick = { onNavigateToLesson(lesson.id) }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun EduProgressCard(
    xp: Int,
    streak: Int,
    completedLessons: Int,
    totalLessons: Int,
    onViewBadges: () -> Unit
) {
    val levels = listOf(
        "NOVICE" to 0,
        "ANALYST" to 500,
        "INVESTIGATOR" to 1000,
        "SPECIALIST" to 1500,
        "EXPERT" to 2000,
        "SECURITY MASTER" to 2500
    )
    
    val currentLevelIndex = levels.indexOfLast { xp >= it.second }.coerceAtLeast(0)
    val nextLevelIndex = (currentLevelIndex + 1).coerceAtMost(levels.size - 1)
    
    val currentLevelName = levels[currentLevelIndex].first
    val currentLevelStart = levels[currentLevelIndex].second
    val nextLevelStart = levels[nextLevelIndex].second
    val xpInCurrentLevel = xp - currentLevelStart
    val xpNeededForNext = nextLevelStart - currentLevelStart
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "AGENT LEVEL ${currentLevelIndex + 1}: $currentLevelName",
                style = MaterialTheme.typography.labelSmall,
                color = NeonCyan,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    EduStat(value = "$xp", label = "TOT XP")
                    EduStat(value = "$streak", label = "STREAK")
                    EduStat(value = "$completedLessons/$totalLessons", label = "DONE")
                }
                Button(
                    onClick = onViewBadges,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen.copy(alpha = 0.15f),
                        contentColor = NeonGreen
                    ),
                    border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("BADGES", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            // Dynamic Progress Bar
            val isMaxLevel = currentLevelIndex == levels.size - 1
            val progress = if (isMaxLevel) 1f else (xpInCurrentLevel.toFloat() / xpNeededForNext.toFloat()).coerceIn(0f, 1f)
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = NeonGreen,
                trackColor = NeonGreen.copy(alpha = 0.15f)
            )
            
            val progressText = if (isMaxLevel) {
                "Maximum Rank Reached! Excellence achieved."
            } else {
                "$xpInCurrentLevel / $xpNeededForNext XP earned towards ${levels[nextLevelIndex].first}"
            }
            
            Text(
                text = progressText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun CelebrationBanner() {
    val infiniteTransition = rememberInfiniteTransition(label = "celebration_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NeonGreen.copy(alpha = alpha * 0.15f)),
        border = BorderStroke(1.dp, NeonGreen.copy(alpha = alpha)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Star, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "TRAINING COMPLETE!",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonGreen,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "You've finished all available security lessons. Excellent work, Agent!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun EduStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, color = NeonGreen, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), letterSpacing = 1.sp)
    }
}

@Composable
fun DailyTipCard(tip: DailyTip) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NeonCyan.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "DAILY TIP", style = MaterialTheme.typography.labelSmall, color = NeonCyan, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = tip.title, style = MaterialTheme.typography.titleSmall, color = NeonGreen, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = tip.content, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun MitreExplorerBanner(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = NeonRed.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, NeonRed.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.BugReport, contentDescription = null, tint = NeonRed, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "MITRE ATT&CK EXPLORER", style = MaterialTheme.typography.titleSmall, color = NeonRed, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
                Text(text = "10 attack techniques explained in plain language", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = NeonRed)
        }
    }
}

@Composable
fun EduCategoryChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(
        if (isSelected) NeonGreen.copy(alpha = 0.2f) else CyberSurface, label = "chip_bg"
    )
    val borderColor by animateColorAsState(
        if (isSelected) NeonGreen else NeonGreen.copy(alpha = 0.2f), label = "chip_border"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) NeonGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun LessonCard(lesson: SecurityLesson, isCompleted: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(
        if (isCompleted) NeonGreen.copy(alpha = 0.07f) else CyberSurface, label = "lesson_bg"
    )
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.dp, if (isCompleted) NeonGreen.copy(alpha = 0.35f) else NeonGreen.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji icon
            Text(text = lesson.iconRes, fontSize = 30.sp, modifier = Modifier.width(42.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = lesson.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    if (isCompleted) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Done", tint = NeonGreen, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = lesson.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), maxLines = 2)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    DifficultyBadge(lesson.difficulty)
                    Text(text = "${lesson.estimatedMinutes} min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontFamily = FontFamily.Monospace)
                    Text(text = "+${lesson.xpReward} XP", style = MaterialTheme.typography.labelSmall, color = NeonGreen, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
fun DifficultyBadge(difficulty: LessonDifficulty) {
    val (label, color) = when (difficulty) {
        LessonDifficulty.BEGINNER -> Pair("BEGINNER", NeonGreen)
        LessonDifficulty.INTERMEDIATE -> Pair("INTER", Color(0xFFFFA500))
        LessonDifficulty.ADVANCED -> Pair("ADVANCED", NeonRed)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = color, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun LearningPathCard(
    path: com.shadowinspect.app.domain.education.LearningPath,
    isLocked: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isLocked) CyberSurface.copy(alpha = 0.5f) else CyberSurface
    val borderColor = if (isLocked) Color.Gray.copy(alpha = 0.2f) else NeonGreen.copy(alpha = 0.3f)
    val contentAlpha = if (isLocked) 0.5f else 1f

    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = !isLocked) { onClick() },
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = path.iconRes, fontSize = 28.sp, modifier = Modifier.padding(end = 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = path.title, 
                    style = MaterialTheme.typography.titleSmall, 
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = path.description, 
                    style = MaterialTheme.typography.bodySmall, 
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha * 0.7f)
                )
                if (isLocked) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Requires ${path.requiredXp} XP to unlock", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = NeonRed.copy(alpha = 0.8f),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            if (isLocked) {
                Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.Gray.copy(alpha = 0.5f))
            } else {
                Icon(Icons.Default.ChevronRight, contentDescription = "Enter", tint = NeonGreen)
            }
        }
    }
}

private fun categoryLabel(category: LessonCategory): String {
    return category.name
        .split("_")
        .joinToString(" ") { word -> 
            word.lowercase().replaceFirstChar { it.uppercase() } 
        }
        .replace("Mitre", "MITRE")
        .replace("Api", "API")
        .replace("Iot", "IoT")
        .replace("Apt", "APT")
        .replace("Osint", "OSINT")
}
