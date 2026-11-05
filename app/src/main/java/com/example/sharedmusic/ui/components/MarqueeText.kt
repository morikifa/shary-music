package com.example.sharedmusic.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Название трека, которое не влезает целиком.
 *
 * Короткое название стоит по центру. Длинное — плавно едет справа налево,
 * а по краям накладывается градиентная маска, поэтому текст «растворяется»
 * с обеих сторон, а не обрезается резкой линией.
 */
@Composable
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = MaterialTheme.colorScheme.onBackground,
    fadeColor: Color = MaterialTheme.colorScheme.background,
    speedDpPerSecond: Float = 40f,
    pauseMillis: Int = 1200,
    animate: Boolean = true
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()

    val measuredWidthPx = remember(text, style, density) {
        measurer.measure(text = text, style = style, maxLines = 1).size.width
    }

    var containerWidthPx by remember { mutableStateOf(0) }

    val overflow = containerWidthPx > 0 && measuredWidthPx > containerWidthPx

    // Маска: по краям цвет фона, в середине прозрачно — текст тает к обоим краям
    val fadeBrush = remember(fadeColor) {
        Brush.horizontalGradient(
            0f to fadeColor,
            0.10f to Color.Transparent,
            0.90f to Color.Transparent,
            1f to fadeColor
        )
    }

    if (!overflow || !animate) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clipToBounds()
                .onSizeChanged { containerWidthPx = it.width },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = style,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val distanceDp = with(density) { (measuredWidthPx - containerWidthPx).toDp().value }
        .coerceAtLeast(1f)

    val durationMillis = (distanceDp / speedDpPerSecond * 1000f)
        .toInt()
        .coerceAtLeast(2_000)

    val transition = rememberInfiniteTransition(label = "marquee")

    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = durationMillis,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(pauseMillis)
        ),
        label = "marquee-progress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .onSizeChanged { containerWidthPx = it.width }
    ) {
        Text(
            text = text,
            style = style,
            color = color,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.offset(x = (-distanceDp * progress).dp)
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(fadeBrush)
        )
    }
}
