package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

private data class SnackbarStyle(
    val icon: ImageVector,
    val iconTint: Color,
    val iconBackground: Color
)

@Composable
private fun resolveSnackbarStyle(message: String): SnackbarStyle {
    val lower = message.lowercase()
    return when {
        lower.contains("cleared") || lower.contains("deleted") || lower.contains("wipe") || lower.contains("removed") -> {
            SnackbarStyle(
                icon = Icons.Outlined.DeleteOutline,
                iconTint = StatusExpired,
                iconBackground = StatusExpired.copy(alpha = 0.16f)
            )
        }
        lower.contains("fail") || lower.contains("error") || lower.contains("denied") || lower.contains("invalid") -> {
            SnackbarStyle(
                icon = Icons.Outlined.ErrorOutline,
                iconTint = StatusWarning,
                iconBackground = StatusWarning.copy(alpha = 0.16f)
            )
        }
        lower.contains("lock") || lower.contains("security") || lower.contains("pin") || lower.contains("biometric") -> {
            SnackbarStyle(
                icon = Icons.Outlined.Lock,
                iconTint = KaamKaajGold,
                iconBackground = KaamKaajGold.copy(alpha = 0.16f)
            )
        }
        lower.contains("internet") || lower.contains("update") || lower.contains("offline") || lower.contains("network") -> {
            SnackbarStyle(
                icon = Icons.Outlined.WifiOff,
                iconTint = KaamKaajGold,
                iconBackground = KaamKaajGold.copy(alpha = 0.16f)
            )
        }
        lower.contains("notification") || lower.contains("reminder") || lower.contains("scheduled") -> {
            SnackbarStyle(
                icon = Icons.Outlined.NotificationsActive,
                iconTint = Color(0xFF818CF8),
                iconBackground = Color(0xFF818CF8).copy(alpha = 0.16f)
            )
        }
        lower.contains("saved") || lower.contains("restored") || lower.contains("exported") ||
        lower.contains("marked") || lower.contains("success") || lower.contains("active") || lower.contains("complete") -> {
            SnackbarStyle(
                icon = Icons.Outlined.CheckCircle,
                iconTint = StatusActive,
                iconBackground = StatusActive.copy(alpha = 0.16f)
            )
        }
        else -> {
            SnackbarStyle(
                icon = Icons.Outlined.Info,
                iconTint = MaterialTheme.colorScheme.primary,
                iconBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KaamKaajFeedbackSnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier
) {
    val message = snackbarData.visuals.message
    val style = resolveSnackbarStyle(message)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val surfaceColor = if (isDark) Color(0xF2161A26) else Color(0xF81E293B)
    val borderColor = if (isDark) Color(0x33818CF8) else Color(0x3364748B)
    val textColor = Color(0xFFF8FAFC)

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd || value == SwipeToDismissBoxValue.EndToStart) {
                snackbarData.dismiss()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {},
        modifier = modifier.fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = Color(0x40000000)
                )
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
            color = surfaceColor,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(style.iconBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = style.icon,
                        contentDescription = null,
                        tint = style.iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                        lineHeight = 18.sp
                    ),
                    color = textColor,
                    modifier = Modifier.weight(1f)
                )

                snackbarData.visuals.actionLabel?.let { actionLabel ->
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(
                        onClick = { snackbarData.performAction() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = actionLabel,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = KaamKaajGold
                        )
                    }
                }

                // Small close (X) button for easy dismissal
                IconButton(
                    onClick = { snackbarData.dismiss() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Dismiss",
                        tint = textColor.copy(alpha = 0.65f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
