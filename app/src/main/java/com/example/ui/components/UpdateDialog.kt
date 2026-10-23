package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.BuildConfig
import com.example.ui.theme.KaamKaajGold
import com.example.update.AppUpdateManager
import com.example.update.DownloadState
import com.example.update.UpdateInfo

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val downloadState by AppUpdateManager.downloadState.collectAsState()

    // Clean reset download state when dialog closes
    DisposableEffect(Unit) {
        onDispose {
            if (downloadState is DownloadState.Failed || downloadState is DownloadState.Completed) {
                AppUpdateManager.resetDownloadState()
            }
        }
    }

    // macOS/iOS Liquid Glass color tokens with high content legibility
    val dialogGlassBg = if (isDark) {
        Color(0xEE161622) // 93% obsidian-tinted liquid glass
    } else {
        Color(0xF7FFFFFF) // 97% frosted porcelain liquid glass
    }

    val dialogBorderBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0x55FFFFFF), // Specular rim reflection
                Color(0x22FFFFFF),
                KaamKaajGold.copy(alpha = 0.35f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFE2E8F0),
                Color(0xFFCBD5E1),
                KaamKaajGold.copy(alpha = 0.45f)
            )
        )
    }

    val innerSectionBg = if (isDark) {
        Color(0xB30C0C14) // 70% deep obsidian glass for high contrast text
    } else {
        Color(0xF0F1F5F9) // 94% soft cool slate
    }

    val innerSectionBorder = if (isDark) {
        Color(0x2EFFFFFF)
    } else {
        Color(0xFFE2E8F0)
    }

    val titleColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF0F172A)
    val bodyTextColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)
    val labelMutedColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    Dialog(
        onDismissRequest = {
            if (downloadState is DownloadState.Downloading) {
                AppUpdateManager.cancelDownload(context)
            }
            onDismiss()
        },
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(220)) + scaleIn(spring(dampingRatio = 0.75f, stiffness = 400f), initialScale = 0.88f),
            exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.90f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 16.dp)
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(28.dp),
                        spotColor = if (isDark) KaamKaajGold.copy(alpha = 0.3f) else Color(0x20000000),
                        ambientColor = if (isDark) Color(0x60000000) else Color(0x15000000)
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(dialogGlassBg)
                    .border(
                        BorderStroke(1.2.dp, dialogBorderBrush),
                        RoundedCornerShape(28.dp)
                    )
            ) {
                // Subtle ambient liquid glow aura at top
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    KaamKaajGold.copy(alpha = if (isDark) 0.16f else 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Liquid Glass Icon Orb
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        KaamKaajGold.copy(alpha = 0.30f),
                                        KaamKaajGold.copy(alpha = 0.12f)
                                    )
                                )
                            )
                            .border(
                                1.2.dp,
                                Brush.linearGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.6f),
                                        KaamKaajGold.copy(alpha = 0.5f)
                                    )
                                ),
                                RoundedCornerShape(22.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.RocketLaunch,
                            contentDescription = "Update Icon",
                            tint = KaamKaajGold,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Feature Badge Pill
                    Surface(
                        shape = CircleShape,
                        color = KaamKaajGold.copy(alpha = if (isDark) 0.22f else 0.14f),
                        border = BorderStroke(1.dp, KaamKaajGold.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = KaamKaajGold,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "NEW VERSION AVAILABLE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = KaamKaajGold,
                                letterSpacing = 0.9.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Title
                    Text(
                        text = updateInfo.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp),
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                        textAlign = TextAlign.Center,
                        lineHeight = 26.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Version Transition Glass Section
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(innerSectionBg)
                            .border(1.dp, innerSectionBorder, RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "CURRENT VERSION",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = labelMutedColor,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "v${BuildConfig.VERSION_NAME}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                    fontWeight = FontWeight.SemiBold,
                                    color = bodyTextColor
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(KaamKaajGold.copy(alpha = if (isDark) 0.20f else 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = KaamKaajGold,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "LATEST VERSION",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = KaamKaajGold,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = updateInfo.latestVersion,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = KaamKaajGold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dynamic Section: Release Notes OR Download Progress / Error
                    when (val state = downloadState) {
                        is DownloadState.Downloading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(innerSectionBg)
                                    .border(1.dp, innerSectionBorder, RoundedCornerShape(16.dp))
                                    .padding(14.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Downloading Update...",
                                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = titleColor
                                        )
                                        Text(
                                            text = if (state.progress >= 0f) "${(state.progress * 100).toInt()}%" else "Preparing...",
                                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = KaamKaajGold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (state.progress >= 0f) {
                                        LinearProgressIndicator(
                                            progress = { state.progress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(CircleShape),
                                            color = KaamKaajGold,
                                            trackColor = if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1)
                                        )
                                    } else {
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(CircleShape),
                                            color = KaamKaajGold,
                                            trackColor = if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1)
                                        )
                                    }
                                    if (state.totalBytes > 0) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        val downloadedMb = String.format("%.2f", state.bytesDownloaded / (1024f * 1024f))
                                        val totalMb = String.format("%.2f", state.totalBytes / (1024f * 1024f))
                                        Text(
                                            text = "$downloadedMb MB / $totalMb MB",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = labelMutedColor
                                        )
                                    }
                                }
                            }
                        }
                        is DownloadState.Failed -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isDark) Color(0x33FF5252) else Color(0x15FF5252))
                                    .border(1.dp, Color(0x66FF5252), RoundedCornerShape(16.dp))
                                    .padding(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ErrorOutline,
                                        contentDescription = null,
                                        tint = Color(0xFFFF5252),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Download Failed",
                                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFF5252)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = state.message,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                            color = bodyTextColor
                                        )
                                    }
                                }
                            }
                        }
                        else -> {
                            // Release Notes / Message Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(innerSectionBg)
                                    .border(1.dp, innerSectionBorder, RoundedCornerShape(16.dp))
                                    .padding(14.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "What's New",
                                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = titleColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = updateInfo.message,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                        fontWeight = FontWeight.Normal,
                                        color = bodyTextColor,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (val state = downloadState) {
                            is DownloadState.Downloading -> {
                                OutlinedButton(
                                    onClick = {
                                        AppUpdateManager.cancelDownload(context)
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isDark) Color(0x1AFFFFFF) else Color(0x10000000)
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isDark) Color(0x35FFFFFF) else Color(0xFFCBD5E1)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                ) {
                                    Text(
                                        text = "Cancel Download",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                                    )
                                }
                            }
                            is DownloadState.Failed -> {
                                OutlinedButton(
                                    onClick = {
                                        AppUpdateManager.cancelDownload(context)
                                        onDismiss()
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isDark) Color(0x1AFFFFFF) else Color(0x10000000)
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isDark) Color(0x35FFFFFF) else Color(0xFFCBD5E1)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                ) {
                                    Text(
                                        text = "Close",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                                    )
                                }

                                Button(
                                    onClick = {
                                        AppUpdateManager.startDownload(context, updateInfo, scope)
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = KaamKaajGold,
                                        contentColor = Color(0xFF0F172A)
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = 6.dp,
                                        pressedElevation = 2.dp
                                    ),
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .height(50.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Refresh,
                                        contentDescription = null,
                                        tint = Color(0xFF0F172A),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Retry",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }
                            is DownloadState.Completed -> {
                                OutlinedButton(
                                    onClick = onDismiss,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isDark) Color(0x1AFFFFFF) else Color(0x10000000)
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isDark) Color(0x35FFFFFF) else Color(0xFFCBD5E1)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                ) {
                                    Text(
                                        text = "Later",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                                    )
                                }

                                Button(
                                    onClick = {
                                        AppUpdateManager.installApk(context, state.apkFile)
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = KaamKaajGold,
                                        contentColor = Color(0xFF0F172A)
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = 6.dp,
                                        pressedElevation = 2.dp
                                    ),
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .height(50.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Download,
                                        contentDescription = null,
                                        tint = Color(0xFF0F172A),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Install Now",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }
                            else -> {
                                OutlinedButton(
                                    onClick = onDismiss,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isDark) Color(0x1AFFFFFF) else Color(0x10000000)
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isDark) Color(0x35FFFFFF) else Color(0xFFCBD5E1)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                ) {
                                    Text(
                                        text = "Later",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                                    )
                                }

                                Button(
                                    onClick = {
                                        AppUpdateManager.startDownload(context, updateInfo, scope)
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = KaamKaajGold,
                                        contentColor = Color(0xFF0F172A)
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = 6.dp,
                                        pressedElevation = 2.dp
                                    ),
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .height(50.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Download,
                                        contentDescription = null,
                                        tint = Color(0xFF0F172A),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Update Now",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
