package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

// Sophisticated Dark & Light Palette
// Deep luxury obsidian / OLED dark background (#050505)
val BackgroundDark = Color(0xFF050505)
val DarkBackground = BackgroundDark
val OnBackgroundDark = Color(0xFFF1F5F9)

val SurfaceDark = Color(0xFF0C0C10)
val DarkSurface = SurfaceDark
val OnSurfaceDark = Color(0xFFF8FAFC)
val SurfaceVariantDark = Color(0xFF14141B)
val OnSurfaceVariantDark = Color(0xFF94A3B8)
val OutlineDark = Color(0x1EFFFFFF)

// KaamKaaj Gold & Amber luxury accents
val KaamKaajGold = Color(0xFFF59E0B)
val GoldAccentLight = Color(0xFFD97706)

// Indigo / Periwinkle accents (#A5B4FC / #6366F1 / #818CF8)
val IndigoPrimaryDark = Color(0xFFA5B4FC)
val IndigoOnPrimaryDark = Color(0xFF050505)
val IndigoPrimaryContainerDark = Color(0x2E6366F1)
val IndigoOnPrimaryContainerDark = Color(0xFFC7D2FE)

// Secondary Emerald / Mint accents
val SecondaryTealDark = Color(0xFF34D399)
val SecondaryOnDark = Color(0xFF050505)
val SecondaryContainerDark = Color(0x2610B981)
val SecondaryOnContainerDark = Color(0xFFA7F3D0)

// Tertiary Amber / Coral accents
val TertiaryAmberDark = Color(0xFFFB923C)
val TertiaryOnDark = Color(0xFF050505)
val TertiaryContainerDark = Color(0x26F97316)
val TertiaryOnContainerDark = Color(0xFFFED7AA)

// Glass & Ambient Styling Tokens
val GlassSurfaceDark = Color(0x0DFFFFFF)
val GlassSurfaceElevated = Color(0x17FFFFFF)
val GlassBorderDark = Color(0x17FFFFFF)
val GlassBorderStrong = Color(0x2BFFFFFF)
val TextMutedDark = Color(0xFF64748B)
val TextSecondaryDark = Color(0xFF94A3B8)

// Status colors
val StatusActive = Color(0xFF34D399)
val StatusWarning = Color(0xFFFB923C)
val StatusExpired = Color(0xFFFB7185)
val StatusInfo = Color(0xFFA5B4FC)
val StatusPurple = Color(0xFFC084FC)

// Light Theme Palette (Clean refined minimal)
val IndigoPrimaryLight = Color(0xFF3730A3)
val IndigoOnPrimaryLight = Color(0xFFFFFFFF)
val IndigoPrimaryContainerLight = Color(0xFFEEF2FF)
val IndigoOnPrimaryContainerLight = Color(0xFF312E81)

val SecondaryTealLight = Color(0xFF0D9488)
val SecondaryOnLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFCCFBF1)
val SecondaryOnContainerLight = Color(0xFF115E59)

val TertiaryAmberLight = Color(0xFFEA580C)
val TertiaryOnLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFFFEDD5)
val TertiaryOnContainerLight = Color(0xFF9A3412)

val BackgroundLight = Color(0xFFF8FAFC)
val OnBackgroundLight = Color(0xFF0F172A)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF0F172A)
val SurfaceVariantLight = Color(0xFFF1F5F9)
val OnSurfaceVariantLight = Color(0xFF475569)
val OutlineLight = Color(0xFFE2E8F0)

@Composable
fun getCardBackground(): Color {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (isDark) GlassSurfaceDark else Color(0xFFFFFFFF)
}

@Composable
fun getCardBorder(): Color {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (isDark) GlassBorderDark else Color(0xFFE2E8F0)
}

@Composable
fun getCardElevated(): Color {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (isDark) GlassSurfaceElevated else Color(0xFFF8FAFC)
}

@Composable
fun getTextMuted(): Color {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (isDark) TextMutedDark else Color(0xFF64748B)
}

@Composable
fun getTextSecondary(): Color {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (isDark) TextSecondaryDark else Color(0xFF334155)
}

@Composable
fun getCardTitleColor(): Color {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (isDark) Color.White else Color(0xFF0F172A)
}


