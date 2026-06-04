package com.shadowinspect.app.presentation.screens.education

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shadowinspect.app.presentation.theme.CyberSurface
import com.shadowinspect.app.presentation.theme.NeonCyan
import com.shadowinspect.app.presentation.theme.NeonGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonDetailScreen(
    lessonId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToNextLesson: (Long) -> Unit,
    onNavigateToNextPath: (com.shadowinspect.app.domain.education.LearningLevel) -> Unit,
    onNavigateToDashboard: () -> Unit,
    viewModel: LessonDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    LaunchedEffect(lessonId) {
        viewModel.loadLesson(lessonId)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.lesson?.title?.uppercase() ?: "LESSON",
                        style = MaterialTheme.typography.titleSmall,
                        color = NeonGreen,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            if (state.lesson != null) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    var showLevelAnimation by remember { mutableStateOf(false) }
                    var showMasterAnimation by remember { mutableStateOf(false) }
                    
                    LaunchedEffect(state.isLevelCompleted) {
                        if (state.isLevelCompleted && state.nextPathLevel != null) {
                            showLevelAnimation = true
                            kotlinx.coroutines.delay(3500)
                            showLevelAnimation = false
                        }
                    }

                    LaunchedEffect(state.isEntireCourseCompleted) {
                        if (state.isEntireCourseCompleted) {
                            showMasterAnimation = true
                            kotlinx.coroutines.delay(4000)
                            showMasterAnimation = false
                        }
                    }

                    if (showMasterAnimation) {
                        MasterHackerAnimation(
                            modifier = Modifier.fillMaxWidth().height(100.dp)
                        )
                    } else if (showLevelAnimation) {
                        CyberLevelUpAnimation(
                            levelName = state.nextPathLevel?.name ?: "UNKNOWN",
                            modifier = Modifier.fillMaxWidth().height(80.dp)
                        )
                    } else {
                        if (!state.isCompleted) {
                            Button(
                                onClick = { viewModel.markComplete(onNavigateNext = null, onNavigateNextLevel = null, onNavigateToDashboard = null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonGreen,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "COMPLETE  +${state.lesson?.xpReward} XP",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                            // Option to mark complete AND go next immediately
                            if (state.nextLessonId != null) {
                                Button(
                                    onClick = { viewModel.markComplete(onNavigateNext = onNavigateToNextLesson, onNavigateNextLevel = null, onNavigateToDashboard = null) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NeonCyan,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "COMPLETE & GO NEXT",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                                }
                            }
                            // Option to mark complete AND go to next level immediately
                            if (state.nextPathLevel != null) {
                                Button(
                                    onClick = { viewModel.markComplete(onNavigateNext = null, onNavigateNextLevel = onNavigateToNextPath, onNavigateToDashboard = null) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NeonCyan,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "COMPLETE & START NEXT LEVEL",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                                }
                            } else if (state.lesson?.level == com.shadowinspect.app.domain.education.LearningLevel.EXPERT) {
                                // Final lesson of Expert level
                                Button(
                                    onClick = { viewModel.markComplete(onNavigateNext = null, onNavigateNextLevel = null, onNavigateToDashboard = onNavigateToDashboard) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFFD700), // Gold
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "COMPLETE & BECOME EXECUTOR",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                                }
                            }
                        } else if (state.nextLessonId != null) {
                            Button(
                                onClick = { onNavigateToNextLesson(state.nextLessonId!!) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonCyan,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "NEXT LESSON",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                            }
                        } else if (state.nextPathLevel != null) {
                            Button(
                                onClick = { onNavigateToNextPath(state.nextPathLevel!!) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonCyan,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "START NEXT LEVEL",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                            }
                        } else if (state.isEntireCourseCompleted) {
                            Button(
                                onClick = onNavigateToDashboard,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFD700),
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "RETURN TO DASHBOARD",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonGreen)
                }
            }
            state.lesson == null -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text("Lesson not found", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }
            else -> {
                val lesson = state.lesson!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(4.dp))

                    // Completed banner
                    if (state.isCompleted) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = if (state.isLevelCompleted) NeonCyan.copy(alpha = 0.15f) else NeonGreen.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, if (state.isLevelCompleted) NeonCyan.copy(alpha = 0.4f) else NeonGreen.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = if (state.isLevelCompleted) NeonCyan else NeonGreen, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (state.isLevelCompleted) "🏆 LEVEL COMPLETE! +250 XP BONUS" else "LESSON COMPLETE — ${lesson.xpReward} XP EARNED",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (state.isLevelCompleted) NeonCyan else NeonGreen,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }

                    // Header
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CyberSurface),
                        border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = lesson.iconRes, fontSize = 48.sp, modifier = Modifier.width(60.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                DifficultyBadge(lesson.difficulty)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = lesson.title, style = MaterialTheme.typography.titleMedium, color = NeonGreen, fontWeight = FontWeight.Bold)
                                Text(text = "${lesson.estimatedMinutes} min read  •  +${lesson.xpReward} XP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    // Content
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CyberSurface),
                        border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            lesson.content.lines().forEach { line ->
                                when {
                                    line.startsWith("# ") -> Text(
                                        text = line.removePrefix("# "),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = NeonGreen,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    line.startsWith("## ") -> Text(
                                        text = line.removePrefix("## "),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = NeonCyan,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                    )
                                    line.startsWith("### ") -> Text(
                                        text = line.removePrefix("### "),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                                    )
                                    line.startsWith("- ") -> Row(modifier = Modifier.padding(vertical = 1.dp)) {
                                        Text("▸ ", color = NeonGreen, style = MaterialTheme.typography.bodySmall)
                                        Text(line.removePrefix("- "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                                    }
                                    line.isBlank() -> Spacer(modifier = Modifier.height(4.dp))
                                    else -> Text(text = line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                                }
                            }
                        }
                    }

                    // Key Takeaways
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = NeonGreen.copy(alpha = 0.07f)),
                        border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(text = "KEY TAKEAWAYS", style = MaterialTheme.typography.labelSmall, color = NeonGreen, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            lesson.summaryPoints.forEach { point ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                    Text("✓  ", color = NeonGreen, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text(point, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                                }
                            }
                        }
                    }

                    // Related MITRE Techniques
                    if (lesson.mitreTechniques.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CyberSurface),
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Text(text = "RELATED MITRE TECHNIQUES", style = MaterialTheme.typography.labelSmall, color = NeonCyan, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                lesson.mitreTechniques.forEach { tech ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                        color = NeonCyan.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = tech,
                                            modifier = Modifier.padding(8.dp),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NeonCyan,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun CyberLevelUpAnimation(
    levelName: String,
    modifier: Modifier = Modifier
) {
    var textState by remember { mutableStateOf("") }
    val fullText = "SYSTEM BREACHED - $levelName UNLOCKED"
    val charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*"
    
    // Blinking cursor effect
    val infiniteTransition = rememberInfiniteTransition(label = "Blink")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    LaunchedEffect(fullText) {
        textState = ""
        // Matrix decoding text effect
        for (i in fullText.indices) {
            val targetChar = fullText[i]
            
            // Random character scrambling before locking in the real character
            for (scramble in 0..5) {
                if (targetChar == ' ') continue
                val randomChar = charset.random()
                textState = textState.take(i) + randomChar
                kotlinx.coroutines.delay(15)
            }
            // Lock in the real character
            textState = textState.take(i) + targetChar
            kotlinx.coroutines.delay(30)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = "$textState${if(cursorAlpha > 0.5f) "_" else ""}",
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            fontSize = 18.sp,
            color = Color(0xFF00FFCC), // NeonCyan
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun MasterHackerAnimation(
    modifier: Modifier = Modifier
) {
    var textState by remember { mutableStateOf("") }
    val fullText = "SHADOW INSPECT MASTER\nALL MODULES CLEARED"
    val charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*<>?~"
    
    // Blinking cursor effect
    val infiniteTransition = rememberInfiniteTransition(label = "BlinkMaster")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AlphaMaster"
    )

    LaunchedEffect(fullText) {
        textState = ""
        // Matrix decoding text effect for the grand finale
        for (i in fullText.indices) {
            val targetChar = fullText[i]
            
            // Random character scrambling before locking in the real character
            for (scramble in 0..6) {
                if (targetChar == ' ' || targetChar == '\n') continue
                val randomChar = charset.random()
                textState = textState.take(i) + randomChar
                kotlinx.coroutines.delay(12)
            }
            // Lock in the real character
            textState = textState.take(i) + targetChar
            kotlinx.coroutines.delay(25)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = "$textState${if(cursorAlpha > 0.5f) "_" else ""}",
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
            fontSize = 20.sp,
            color = Color(0xFFFFD700), // Gold for master
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center
        )
    }
}
