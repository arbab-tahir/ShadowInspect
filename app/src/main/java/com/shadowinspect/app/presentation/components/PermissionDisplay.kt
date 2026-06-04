package com.shadowinspect.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadowinspect.app.domain.model.ApkPermission

/**
 * Displays a list of APK permissions inside an expandable card.
 * - Header shows count of dangerous/total permissions.
 * - Expand to reveal full permission list (or dangerous-only if requested).
 */
@Composable
fun PermissionList(
    permissions: List<ApkPermission>,
    dangerousOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    val filtered = if (dangerousOnly) permissions.filter { it.isDangerous } else permissions
    val dangerousCount = permissions.count { it.isDangerous }
    val expanded = remember { mutableStateOf(false) }

    val cyberDanger = Color(0xFFFF4D4D)
    val cyberSafe = Color(0xFF4CFFB5)
    val cyberCard = Color(0xFF071029)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = cyberCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded.value = !expanded.value },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Permissions",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${dangerousCount} dangerous • ${permissions.size} total",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                        fontSize = 13.sp
                    )
                }

                val chevron = if (expanded.value) "▴" else "▾"
                Text(
                    text = chevron,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 20.sp
                )
            }

            AnimatedVisibility(visible = expanded.value) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    color = Color.Transparent
                ) {
                    if (filtered.isEmpty()) {
                        Text(
                            text = if (dangerousOnly) "No dangerous permissions found." else "No permissions declared.",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.padding(8.dp)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(filtered) { perm ->
                                PermissionItem(
                                    permission = perm,
                                    cyberDanger = cyberDanger,
                                    cyberSafe = cyberSafe
                                )
                                Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))

                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionItem(
    permission: ApkPermission,
    cyberDanger: Color,
    cyberSafe: Color,
    modifier: Modifier = Modifier
) {
    val indicatorColor = if (permission.isDangerous) cyberDanger else cyberSafe

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(shape = androidx.compose.foundation.shape.CircleShape)
                .background(indicatorColor)
        )

        Spacer(modifier = Modifier.size(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = simplifyPermissionName(permission.name),
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            )
            Text(
                text = permission.description ?: getPermissionDescription(permission.name),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                fontSize = 13.sp
            )
        }

        // small label for danger
        val label = if (permission.isDangerous) "DANGEROUS" else "NORMAL"
        val labelColor = if (permission.isDangerous) cyberDanger else cyberSafe
        Text(
            text = label,
            color = labelColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp
        )
    }
}

/**
 * Simplifies raw permission strings to a friendly display label.
 */
fun simplifyPermissionName(permission: String): String {
    return when {
        permission.endsWith(".READ_SMS") -> "Read SMS messages"
        permission.endsWith(".RECEIVE_SMS") -> "Receive SMS messages"
        permission.endsWith(".SEND_SMS") -> "Send SMS messages"
        permission.endsWith(".READ_CONTACTS") -> "Read contacts"
        permission.endsWith(".WRITE_CONTACTS") -> "Modify contacts"
        permission.endsWith(".CAMERA") -> "Take pictures and video"
        permission.endsWith(".RECORD_AUDIO") -> "Record audio"
        permission.endsWith(".ACCESS_FINE_LOCATION") -> "Access precise location"
        permission.endsWith(".ACCESS_COARSE_LOCATION") -> "Access approximate location"
        permission.endsWith(".READ_PHONE_STATE") -> "Read phone state"
        permission.endsWith(".CALL_PHONE") -> "Make phone calls"
        permission.endsWith(".READ_EXTERNAL_STORAGE") -> "Read external storage"
        permission.endsWith(".WRITE_EXTERNAL_STORAGE") -> "Modify external storage"
        permission.contains("INTERNET") -> "Network access"
        permission.contains("SYSTEM_ALERT_WINDOW") -> "Draw over other apps"
        else -> permission.substringAfterLast('.')
            .split('_')
            .joinToString(" ") { it.lowercase().replaceFirstChar { c -> c.uppercaseChar() } }
    }
}

/**
 * Provides a brief description for well-known permissions when the APK did not include one.
 */
fun getPermissionDescription(permission: String): String {
    return when {
        permission.endsWith(".READ_SMS") -> "Allows the app to read SMS messages stored on the device."
        permission.endsWith(".RECEIVE_SMS") -> "Allows the app to receive SMS messages."
        permission.endsWith(".SEND_SMS") -> "Allows the app to send SMS messages without user interaction."
        permission.endsWith(".READ_CONTACTS") -> "Allows the app to read the user's contacts."
        permission.endsWith(".CAMERA") -> "Allows the app to access the device camera to take pictures or record video."
        permission.endsWith(".RECORD_AUDIO") -> "Allows the app to record audio through the microphone."
        permission.endsWith(".ACCESS_FINE_LOCATION") -> "Allows the app to access precise device location."
        permission.endsWith(".ACCESS_COARSE_LOCATION") -> "Allows the app to access approximate device location."
        permission.endsWith(".READ_PHONE_STATE") -> "Allows the app to read device identifiers and call state."
        permission.endsWith(".CALL_PHONE") -> "Allows the app to initiate phone calls without user confirmation."
        permission.contains("INTERNET") -> "Allows the app to open network sockets and communicate over the internet."
        permission.contains("STORAGE") -> "Allows the app to read or modify files on external storage."
        permission.contains("SYSTEM_ALERT_WINDOW") -> "Allows the app to display overlays on top of other apps."
        else -> "Grants app-specific capability declared by the developer."
    }
}
