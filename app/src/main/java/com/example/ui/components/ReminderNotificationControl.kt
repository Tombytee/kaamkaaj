package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.SecurityUpdateWarning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderNotificationControl(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    selectedDaysBefore: Int,
    onDaysSelected: (Int) -> Unit,
    options: List<Pair<Int, String>>,
    itemTypeLabel: String = "item expires",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var permissionRequested by remember { mutableStateOf(false) }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        permissionRequested = true
    }

    var dropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) GlassSurfaceDark else Color(0x08FFFFFF))
            .border(
                1.dp,
                if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else GlassBorderDark,
                RoundedCornerShape(14.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Toggle Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (enabled) StatusActive.copy(alpha = 0.15f)
                            else Color(0x12FFFFFF)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (enabled) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsOff,
                        contentDescription = null,
                        tint = if (enabled) StatusActive else TextMutedDark,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = if (enabled) "Notifications: ON" else "Notifications: OFF",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (enabled) Color.White else TextMutedDark
                    )
                    Text(
                        text = if (enabled) "You’ll receive notifications before this $itemTypeLabel"
                        else "Reminder notifications are OFF (no alerts)",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled) TextSecondaryDark else TextMutedDark,
                        lineHeight = 16.sp
                    )
                }
            }

            Switch(
                checked = enabled,
                onCheckedChange = { isChecked ->
                    onEnabledChange(isChecked)
                    if (isChecked && !hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permissionRequested) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = StatusActive,
                    uncheckedThumbColor = TextMutedDark,
                    uncheckedTrackColor = GlassBorderDark
                )
            )
        }

        // Timing Configuration (Expanded only when enabled)
        AnimatedVisibility(
            visible = enabled,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HorizontalDivider(color = GlassBorderDark)

                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                ) {
                    val currentOptionText = options.find { it.first == selectedDaysBefore }?.second
                        ?: if (selectedDaysBefore == 0) "On the day at 9:00 AM" else "$selectedDaysBefore days before"

                    OutlinedTextField(
                        value = currentOptionText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Reminder Timing") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = GlassBorderDark
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        options.forEach { (days, label) ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = label,
                                        fontWeight = if (days == selectedDaysBefore) FontWeight.Bold else FontWeight.Normal,
                                        color = if (days == selectedDaysBefore) MaterialTheme.colorScheme.primary else Color.White
                                    )
                                },
                                onClick = {
                                    onDaysSelected(days)
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Text(
                    text = "⏰ Alert will trigger reliably at 9:00 AM on scheduled reminder day",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = TextMutedDark
                )

                // Permission Warning Banner if on Android 13+ and permission missing
                if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Surface(
                        color = StatusWarning.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StatusWarning.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.SecurityUpdateWarning,
                                contentDescription = null,
                                tint = StatusWarning,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Notification permission required. Tap here to grant.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.5.sp
                                ),
                                color = StatusWarning
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReminderStatusCard(
    enabled: Boolean,
    infoText: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (enabled) StatusActive.copy(alpha = 0.08f) else Color(0x0CFFFFFF),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (enabled) StatusActive.copy(alpha = 0.25f) else GlassBorderDark
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (enabled) StatusActive.copy(alpha = 0.15f)
                        else Color(0x12FFFFFF)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (enabled) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsOff,
                    contentDescription = null,
                    tint = if (enabled) StatusActive else TextMutedDark,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = if (enabled) "Reminder notifications are ON" else "Reminder notifications are OFF",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.3.sp
                    ),
                    color = if (enabled) Color.White else TextMutedDark
                )
                Text(
                    text = if (enabled) infoText else "No notifications or alerts will be sent for this item",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = if (enabled) TextSecondaryDark else TextMutedDark
                )
            }
        }
    }
}
