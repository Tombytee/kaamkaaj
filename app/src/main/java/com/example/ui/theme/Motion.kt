package com.example.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Standard Motion Tokens & Animation Specs for KaamKaaj
 * Designed for 60-120fps fluid responsiveness across high, mid, and low-end devices.
 */
object MotionTokens {
    const val DurationFast = 180
    const val DurationNormal = 260
    const val DurationMedium = 320
    const val DurationSlow = 400

    val FastEasing = FastOutSlowInEasing

    val SubScreenEnterSpec = tween<Float>(DurationNormal, easing = FastEasing)
    val SubScreenExitSpec = tween<Float>(DurationFast, easing = FastEasing)

    val TabTransitionEnterSpec = tween<Float>(DurationNormal, easing = FastEasing)
    val TabTransitionExitSpec = tween<Float>(DurationFast, easing = FastEasing)

    val InteractiveSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val SnappySpring = spring<Float>(
        dampingRatio = 0.8f,
        stiffness = 500f
    )

    val GentleSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )
}

/**
 * Adds a responsive, tactile scale effect on press without creating extra draw layers when idle.
 */
fun Modifier.bouncyClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.95f,
    onClick: () -> Unit
): Modifier = composed {
    if (!enabled) return@composed this

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1.0f,
        animationSpec = MotionTokens.SnappySpring,
        label = "bouncy_click_scale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(),
            onClick = onClick
        )
}

/**
 * Subtle tactile press modifier for larger cards and list items.
 */
fun Modifier.glassCardClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.98f,
    onClick: () -> Unit
): Modifier = composed {
    if (!enabled) return@composed this

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1.0f,
        animationSpec = MotionTokens.SnappySpring,
        label = "glass_card_scale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(),
            onClick = onClick
        )
}
