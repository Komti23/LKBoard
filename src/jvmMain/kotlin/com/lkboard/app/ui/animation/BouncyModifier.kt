package com.lkboard.app.ui.animation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Модификатор тактильной упругой анимации при нажатии (Spring Press Bounce):
 * - При наведении слегка приподнимает элемент (1.02x).
 * - При нажатии плавно сжимает (0.94x), обеспечивая осязаемый тактильный отклик.
 * - При отпускании создает мягкий физический отскок (Spring bounce).
 */
@Composable
fun Modifier.bouncyClickable(
    pressedScale: Float = 0.94f,
    hoverScale: Float = 1.0f,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            !enabled -> 1.0f
            isPressed -> pressedScale
            isHovered -> hoverScale
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bouncyScale"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * Модификатор анимации нажатия для элементов, у которых уже используется существующий interactionSource.
 */
@Composable
fun Modifier.pressScaleEffect(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.91f,
    hoverScale: Float = 1.0f
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> pressedScale
            isHovered -> hoverScale
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScaleEffect"
    )

    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
