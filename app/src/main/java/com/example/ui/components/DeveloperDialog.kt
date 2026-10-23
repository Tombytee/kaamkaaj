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
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.KaamKaajGold
import com.example.ui.theme.bouncyClickable

/**
 * Premium Apple iOS 26 Liquid Glass-inspired Developer Dialog.
 *
 * Implements a multi-layered, self-contained glass surface featuring:
 * - Multi-stop specular rim highlights
 * - Radial caustic lighting & internal atmospheric gradients
 * - Prominent, refined developer visual identity (Shubham Kumar Prusty / SUBHAM)
 * - Nested liquid-glass information lens & architectural pillars
 * - Apple-style responsive Liquid Glass dismissal pill
 */
@Composable
fun DeveloperDialog(
    onDismiss: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    // Liquid Glass Surface Gradient Tokens
    val glassBaseBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xF5131627), // Deep obsidian-sapphire liquid glass top
            Color(0xF80E101D), // Mid atmospheric crystal
            Color(0xFA090A12)  // Ultra-deep obsidian base
        )
    )

    // Signature Chamfered Rim Highlight (Multi-stop specular border)
    val signatureRimHighlight = Brush.linearGradient(
        colors = listOf(
            Color(0xAAFFFFFF), // Top-left specular gleam
            Color(0x40FFFFFF), // Upper edge translucent highlight
            Color(0x25818CF8), // Periwinkle refraction bounce
            Color(0x40F59E0B)  // Ambient warm gold caustic rim
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    // Inner Glass Tile Shader
    val innerLensBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0x24FFFFFF), // Top specular reflection
            Color(0x0EFFFFFF)  // Base glass body
        )
    )

    val innerLensBorder = Brush.verticalGradient(
        colors = listOf(
            Color(0x4DFFFFFF),
            Color(0x1AFFFFFF)
        )
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(220)) + scaleIn(
                animationSpec = spring(dampingRatio = 0.76f, stiffness = 420f),
                initialScale = 0.90f
            ),
            exit = fadeOut(tween(180)) + scaleOut(
                animationSpec = tween(180),
                targetScale = 0.92f
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 410.dp)
                    .padding(vertical = 20.dp)
                    .shadow(
                        elevation = 32.dp,
                        shape = RoundedCornerShape(30.dp),
                        spotColor = Color(0x60818CF8),
                        ambientColor = Color(0x80000000)
                    )
                    .clip(RoundedCornerShape(30.dp))
                    .background(glassBaseBackground)
                    .border(BorderStroke(1.2.dp, signatureRimHighlight), RoundedCornerShape(30.dp))
            ) {
                // Internal Caustic Lighting Flares (Self-contained optical effects)
                // 1. Top-Left Indigo Radial Caustic Flare
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .offset(x = (-60).dp, y = (-60).dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0x30818CF8),
                                    Color(0x106366F1),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // 2. Bottom-Right Warm Gold Ambient Refraction Glow
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 60.dp, y = 60.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    KaamKaajGold.copy(alpha = 0.18f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // 3. Top Specular Sheet Glare
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x1FFFFFFF),
                                    Color(0x06FFFFFF),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Dialog Content Column
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Badge Capsule: "LEAD ARCHITECT"
                    Surface(
                        shape = CircleShape,
                        color = Color(0x1CFFFFFF),
                        border = BorderStroke(
                            1.dp,
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0x60FFFFFF),
                                    Color(0x25818CF8),
                                    KaamKaajGold.copy(alpha = 0.5f)
                                )
                            )
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Code,
                                contentDescription = null,
                                tint = Color(0xFFA5B4FC),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "LEAD DEVELOPER & ARCHITECT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.5.sp,
                                    letterSpacing = 1.2.sp
                                ),
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Liquid Glass Emblem Orb
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0x35818CF8),
                                        Color(0x156366F1),
                                        Color(0x20F59E0B)
                                    )
                                )
                            )
                            .border(
                                1.2.dp,
                                Brush.linearGradient(
                                    listOf(
                                        Color(0x99FFFFFF),
                                        Color(0x30818CF8),
                                        KaamKaajGold.copy(alpha = 0.6f)
                                    )
                                ),
                                RoundedCornerShape(24.dp)
                            )
                            .shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(24.dp),
                                spotColor = Color(0x50818CF8)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = "Developer Emblem",
                            tint = Color(0xFFF1F5F9),
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // DEVELOPER NAME — MAIN VISUAL IDENTITY
                    Text(
                        text = "Subham kumar prusty",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Serif,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        ),
                        color = Color(0xFFFFFFFF),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Developer Alias Monogram Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = KaamKaajGold.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, KaamKaajGold.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = "SUBHAM",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                letterSpacing = 2.sp
                            ),
                            color = KaamKaajGold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // App Philosophy & Bio Lens Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(innerLensBackground)
                            .border(BorderStroke(1.dp, innerLensBorder), RoundedCornerShape(18.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "KaamKaaj",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    color = Color(0xFFF8FAFC)
                                )
                                Text(
                                    text = "• Personal Management",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp
                                    ),
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Designed and architected as your personal offline management assistant for organizing documents, tracking expiry dates, managing recurring bills, warranties, and reminders.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.5.sp,
                                    lineHeight = 17.5.sp
                                ),
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Architectural Pillars (Micro Glass Cards)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GlassPillarItem(
                            icon = Icons.Outlined.Storage,
                            title = "Local Room Database",
                            subtitle = "Zero cloud dependency, offline persistence"
                        )
                        GlassPillarItem(
                            icon = Icons.Outlined.Lock,
                            title = "Private & Zero Tracking",
                            subtitle = "Biometric lock & complete on-device privacy"
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // iOS Liquid Glass Dismiss Action Pill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .bouncyClickable(pressedScale = 0.96f) {
                                onDismiss()
                            }
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x35FFFFFF),
                                        Color(0x18FFFFFF)
                                    )
                                )
                            )
                            .border(
                                BorderStroke(
                                    1.dp,
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0x80FFFFFF),
                                            Color(0x2BFFFFFF)
                                        )
                                    )
                                ),
                                RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Done",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color(0xFFFFFFFF)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassPillarItem(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x12FFFFFF))
            .border(BorderStroke(0.8.dp, Color(0x1EFFFFFF)), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color(0x22818CF8)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFA5B4FC),
                modifier = Modifier.size(13.dp)
            )
        }
        Spacer(modifier = Modifier.width(9.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.5.sp
                ),
                color = Color(0xFFF1F5F9)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.sp
                ),
                color = Color(0xFF94A3B8)
            )
        }
    }
}
