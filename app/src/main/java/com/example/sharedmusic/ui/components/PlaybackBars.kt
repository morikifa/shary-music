package com.example.sharedmusic.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Три «пляшущих» полоски — индикатор того, что трек играет.
 * Рисуются на канве: не требуют картинок и анимируются без задержек.
 */
@Composable
fun PlaybackBars(
    color: Color,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    size: Dp = 12.dp
) {
    val transition = rememberInfiniteTransition(label = "bars")

    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bars-phase"
    )

    Canvas(
        modifier = modifier.size(size)
    ) {
        val barCount = 3
        val gap = this.size.width * 0.12f
        val barWidth = (this.size.width - gap * (barCount - 1)) / barCount

        for (index in 0 until barCount) {
            val shift = if (animate) phase + index * 0.28f else 0.35f + index * 0.1f
            val wave = (kotlin.math.sin(shift * 2 * Math.PI).toFloat() + 1f) / 2f
            val height = this.size.height * (0.35f + 0.65f * wave)

            drawRoundRect(
                color = color,
                topLeft = Offset(
                    x = index * (barWidth + gap),
                    y = this.size.height - height
                ),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
        }
    }
}
