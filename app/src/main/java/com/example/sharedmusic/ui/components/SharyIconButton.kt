package com.example.sharedmusic.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.sharedmusic.ui.theme.SharyPink

/**
 * Круглая кнопка-иконка.
 *
 * Используется для всех управляющих элементов плеера, чтобы интерфейс
 * оставался единым: обычное состояние — приглушённое, активное — розовое.
 * Минимальная цель касания — 48 dp по гайдлайнам доступности.
 */
@Composable
fun SharyIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    enabled: Boolean = true,
    active: Boolean = false,
    activeColor: Color = SharyPink,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    filled: Boolean = false
) {
    val tint by animateColorAsState(
        targetValue = when {
            !enabled -> inactiveColor.copy(alpha = 0.35f)
            active -> activeColor
            else -> inactiveColor
        },
        label = "icon-tint"
    )

    val scale by animateFloatAsState(
        targetValue = if (active) 1.08f else 1f,
        label = "icon-scale"
    )

    val background by animateColorAsState(
        targetValue = when {
            filled && enabled -> activeColor.copy(alpha = if (active) 0.22f else 0.12f)
            filled -> Color.Transparent
            else -> Color.Transparent
        },
        label = "icon-background"
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier
                .size(iconSize)
                .scale(scale)
        )
    }
}
