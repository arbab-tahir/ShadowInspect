package com.shadowinspect.app.presentation.scan

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shadowinspect.app.domain.model.PhoneAnalysisResult
import com.shadowinspect.app.domain.model.SpamSource
import com.shadowinspect.app.presentation.scan.phone.PhoneScanUiState
import com.shadowinspect.app.presentation.scan.phone.PhoneScanViewModel
import com.shadowinspect.app.presentation.scan.PhoneResultCard
import com.shadowinspect.app.presentation.scan.RecentPhoneScansSection
import com.shadowinspect.app.presentation.theme.*

// Palette from Theme.kt (Background, Surface, etc. are used)

// ─────────────────────────────────────────────────────────────────────────────
//  Root
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneScanScreen(
    onBack: () -> Unit,
    viewModel: PhoneScanViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        try {
             android.util.Log.d("PHONE_SCREEN", "PhoneScanScreen initialized")
        } catch (e: Exception) {
             android.util.Log.e("PHONE_SCREEN", "Init error: ${e.message}", e)
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "PHONE SECURITY ANALYZER",
                        color = NeonCyan,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.titleMedium,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background,
                    titleContentColor = NeonCyan,
                    navigationIconContentColor = NeonCyan
                )
            )
        },
        containerColor = Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // A. Animated Header
            PhoneAnimatedHeader()

            Spacer(modifier = Modifier.height(20.dp))

            // B. Country Code Selector
            CountryCodeSelector(
                selectedCode = state.countryCode,
                showPicker = state.showCountryPicker,
                onTogglePicker = { viewModel.toggleCountryPicker(it) },
                onSelectCountry = { viewModel.updateCountryCode(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // C. Phone Input
            PhoneInputField(
                value = state.phoneInput,
                isValid = state.isValidFormat,
                selectedCountryCode = state.countryCode,
                onValueChange = { viewModel.updatePhone(it) },
                onScan = { viewModel.scanPhone() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // D. Scan Button
            ScanButton(
                isScanning = state.isScanning,
                isEnabled = state.isValidFormat && !state.isScanning,
                onClick = { viewModel.scanPhone() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // E. Loading Animation
            if (state.isScanning) {
                PhoneScanLoadingIndicator()
                Spacer(modifier = Modifier.height(16.dp))
            }

            // F. Results Card
            AnimatedVisibility(
                visible = state.scanResult != null,
                enter = fadeIn(tween(400)) + expandVertically(tween(500)),
                exit = fadeOut(tween(300))
            ) {
                state.scanResult?.let { result ->
                    PhoneResultCard(
                        result = result,
                        onClear = { viewModel.clearResult() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // G. Recent Scans
            if (state.recentScans.isNotEmpty()) {
                RecentPhoneScansSection(
                    scans = state.recentScans,
                    onRescan = { number ->
                        viewModel.updatePhone(number)
                        viewModel.scanPhone()
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//       Animated Header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PhoneAnimatedHeader() {
    val pulseTransition = rememberInfiniteTransition(label = "phonePulse")
    val scale by pulseTransition.animateFloat(
        initialValue = 0.92f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOutCubic), RepeatMode.Reverse),
        label = "pulseScale"
    )
    val glowAlpha by pulseTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOutCubic), RepeatMode.Reverse),
        label = "glowAlpha"
    )

    Box(
        modifier = Modifier
            .size((80 * scale).dp)
            .drawBehind {
                drawCircle(
                    color = NeonCyan.copy(alpha = glowAlpha * 0.3f),
                    radius = size.minDimension / 1.5f
                )
            }
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(NeonCyan.copy(alpha = 0.2f), Color.Transparent)
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(Surface, CircleShape)
                .border(2.dp, NeonCyan.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("📞", fontSize = 28.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//       Country Code Selector
// ─────────────────────────────────────────────────────────────────────────────

private data class QuickCountry(val code: String, val flag: String, val dial: String, val name: String, val example: String)

private val quickCountries = listOf(
    QuickCountry("US", "🇺🇸", "+1", "United States",    "+1 (202) 555-1234"),
    QuickCountry("GB", "🇬🇧", "+44", "United Kingdom",  "+44 7911 123456"),
    QuickCountry("IN", "🇮🇳", "+91", "India",           "+91 98765 43210"),
    QuickCountry("PK", "🇵🇰", "+92", "Pakistan",        "+92 300 1234567"),
    QuickCountry("CA", "🇨🇦", "+1", "Canada",           "+1 (416) 555-1234"),
    QuickCountry("AU", "🇦🇺", "+61", "Australia",       "+61 4 1234 5678"),
    QuickCountry("DE", "🇩🇪", "+49", "Germany",         "+49 151 234 5678"),
    QuickCountry("FR", "🇫🇷", "+33", "France",          "+33 6 12 34 56 78"),
    QuickCountry("CN", "🇨🇳", "+86", "China",           "+86 138 1234 5678"),
    QuickCountry("BR", "🇧🇷", "+55", "Brazil",          "+55 (11) 99999-1234"),
    QuickCountry("NG", "🇳🇬", "+234", "Nigeria",        "+234 801 234 5678"),
    QuickCountry("AE", "🇦🇪", "+971", "UAE",            "+971 50 123 4567"),
    QuickCountry("SA", "🇸🇦", "+966", "Saudi Arabia",   "+966 50 123 4567"),
    QuickCountry("JP", "🇯🇵", "+81", "Japan",           "+81 90 1234 5678"),
    QuickCountry("KR", "🇰🇷", "+82", "South Korea",     "+82 10 1234 5678")
)

@Composable
private fun CountryCodeSelector(
    selectedCode: String,
    showPicker: Boolean,
    onTogglePicker: (Boolean) -> Unit,
    onSelectCountry: (String) -> Unit
) {
    val selected = quickCountries.firstOrNull { it.code == selectedCode }
    var searchQuery by remember { mutableStateOf("") }

    // Display Button
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onTogglePicker(!showPicker) }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selected?.let { "${it.flag}  ${it.name}  (${it.dial})" }
                    ?: "🌐  Select Country Code",
                color = if (selected != null) Color.White else DimWhite,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (showPicker) Icons.Default.KeyboardArrowUp
                              else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = NeonCyan
            )
        }
    }

    // Picker Dropdown
    AnimatedVisibility(
        visible = showPicker,
        enter = expandVertically(tween(300)) + fadeIn(),
        exit = shrinkVertically(tween(200)) + fadeOut()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .heightIn(max = 260.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.2f))
        ) {
            Column {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search country…", color = DimWhite, fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        cursorColor = NeonCyan
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                )

                val filtered = quickCountries.filter {
                    searchQuery.isBlank() ||
                        it.name.contains(searchQuery, ignoreCase = true) ||
                        it.dial.contains(searchQuery) ||
                        it.code.contains(searchQuery, ignoreCase = true)
                }

                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    filtered.forEach { country ->
                        val isSelected = country.code == selectedCode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectCountry(country.code)
                                    searchQuery = ""
                                }
                                .background(if (isSelected) NeonCyan.copy(alpha = 0.1f) else Color.Transparent)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(country.flag, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                country.name,
                                color = Color.White,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                country.dial,
                                color = NeonCyan,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//       Phone Input Field
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PhoneInputField(
    value: String,
    isValid: Boolean,
    selectedCountryCode: String,
    onValueChange: (String) -> Unit,
    onScan: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val exampleNumber = quickCountries.firstOrNull { it.code == selectedCountryCode }?.example
        ?: "+1 234 567 8900"

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text("Phone Number", color = DimWhite, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
        },
        placeholder = {
            Text(exampleNumber, color = DimWhite.copy(alpha = 0.4f), fontFamily = FontFamily.Monospace)
        },
        leadingIcon = {
            Icon(Icons.Default.Phone, contentDescription = null, tint = NeonCyan)
        },
        trailingIcon = {
            if (value.isNotBlank()) {
                val icon = if (isValid) Icons.Default.CheckCircle else Icons.Default.Cancel
                val tint = if (isValid) NeonGreen else NeonRed.copy(alpha = 0.6f)
                Icon(icon, contentDescription = null, tint = tint)
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = {
                focusManager.clearFocus()
                if (isValid) onScan()
            }
        ),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NeonCyan,
            unfocusedBorderColor = DimWhite.copy(alpha = 0.3f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = NeonCyan,
            focusedContainerColor = Surface,
            unfocusedContainerColor = Surface
        ),
        textStyle = LocalTextStyle.current.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 18.sp,
            letterSpacing = 1.sp
        )
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//       Scan Button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ScanButton(isScanning: Boolean, isEnabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = isEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        border = BorderStroke(1.dp, NeonCyan),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = NeonCyan,
            containerColor = Color.Black,
            disabledContentColor = DimWhite.copy(alpha = 0.3f)
        )
    ) {
        if (isScanning) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = NeonCyan,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (isEnabled) NeonCyan else DimWhite.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isScanning) "SCANNING…" else "SCAN NUMBER",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//       Loading Animation
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PhoneScanLoadingIndicator() {
    val transition = rememberInfiniteTransition(label = "phoneLoadPulse")
    val alpha by transition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "loadAlpha"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, NeonCyan.copy(alpha = alpha * 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = NeonCyan, strokeWidth = 3.dp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "ANALYZING NUMBER…",
                color = NeonCyan.copy(alpha = alpha),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Checking patterns • APIs • Community DB",
                color = DimWhite,
                fontSize = 12.sp
            )
        }
    }
}

// End of file
