package com.example.sharedmusic.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.sharedmusic.R
import com.example.sharedmusic.domain.TimeFormat
import com.example.sharedmusic.ui.theme.SharyPink
import com.example.sharedmusic.ui.theme.SharyPinkSoft

/**
 * Ряд управления воспроизведением: перемешать, назад, играть/пауза, вперёд, повтор.
 *
 * Активные переключатели становятся розовыми — так пользователь сразу видит,
 * включён ли случайный порядок и повтор одного трека.
 */
@Composable
fun PlayerControls(
    isPlaying: Boolean,
    shuffle: Boolean,
    repeatOne: Boolean,
    enabled: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    modifier: Modifier = Modifier,
    positionMs: Long = 0L,
    durationMs: Long = 0L
) {
    val playScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.96f,
        label = "play-scale"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SharyIconButton(
                icon = Icons.Rounded.Shuffle,
                contentDescription = stringResource(R.string.player_shuffle),
                onClick = onToggleShuffle,
                active = shuffle,
                enabled = enabled,
                size = 44.dp,
                iconSize = 21.dp
            )

            SharyIconButton(
                icon = Icons.Rounded.SkipPrevious,
                contentDescription = stringResource(R.string.player_previous),
                onClick = onPrevious,
                enabled = enabled,
                size = 48.dp,
                iconSize = 26.dp
            )

            PlayButton(
                isPlaying = isPlaying,
                enabled = enabled,
                onClick = onPlayPause,
                scale = playScale
            )

            SharyIconButton(
                icon = Icons.Rounded.SkipNext,
                contentDescription = stringResource(R.string.player_next),
                onClick = onNext,
                enabled = enabled,
                size = 48.dp,
                iconSize = 26.dp
            )

            SharyIconButton(
                icon = Icons.Rounded.RepeatOne,
                contentDescription = stringResource(R.string.player_repeat_one),
                onClick = onToggleRepeat,
                active = repeatOne,
                enabled = enabled,
                size = 44.dp,
                iconSize = 21.dp
            )
        }

        // Время прослушивания под кнопками
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, start = 24.dp, end = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = TimeFormat.clock(positionMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = TimeFormat.clock(durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Круглая розовая кнопка «играть/пауза» — главный элемент управления */
@Composable
private fun PlayButton(
    isPlaying: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    scale: Float
) {
    val brush = Brush.linearGradient(listOf(SharyPink, SharyPinkSoft))
    val description = stringResource(
        if (isPlaying) R.string.player_pause else R.string.player_play
    )

    Box(
        modifier = Modifier
            .size(64.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(if (enabled) brush else Brush.linearGradient(listOf(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.surfaceVariant
            )))
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures(onTap = { onClick() })
                }
            }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(30.dp)
        )
    }
}

/** Тонкая полоса прогресса с делением на прослушанную часть */
@Composable
fun ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 3.dp,
    active: Boolean = true
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(height)
                .clip(CircleShape)
                .background(if (active) SharyPink else MaterialTheme.colorScheme.outline)
        )
    }
}
