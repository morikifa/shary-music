package com.example.sharedmusic.ui.components

import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.sharedmusic.ui.theme.SharyPink
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Виниловая пластинка.
 *
 * Что важно в поведении:
 * - чёрная часть с бороздками вращается, пока трек играет, и замирает на паузе;
 * - пользователь может «взять» пластинку и крутить её пальцем — это перемотка;
 * - центральный круг с обложкой можно оставить неподвижным (как в макете комнаты);
 * - при отключённых системных анимациях пластинка не вращается, чтобы не экономить зря батарею.
 */
@Composable
fun VinylRecord(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 280.dp,
    artworkPath: String? = null,
    seed: String = "",
    /** Вращать ли центральную часть вместе с диском */
    rotateCenter: Boolean = true,
    /** Включена ли перемотка вращением */
    seekEnabled: Boolean = true,
    /** Сообщает изменение угла в градусах при перетаскивании */
    onSeekDegrees: (Float) -> Unit = {},
    onSeekStart: () -> Unit = {},
    onSeekEnd: () -> Unit = {},
    contentDescription: String? = null
) {
    val context = LocalContext.current
    val animationsEnabled = remember { areAnimationsEnabled(context) }

    val shouldSpin = isPlaying && animationsEnabled

    val latestOnSeek by rememberUpdatedState(onSeekDegrees)
    val latestOnStart by rememberUpdatedState(onSeekStart)
    val latestOnEnd by rememberUpdatedState(onSeekEnd)
    val latestSeekEnabled by rememberUpdatedState(seekEnabled)

    // Накопленный угол вращения: не сбрасывается при паузе
    var angleDegrees by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(shouldSpin) {
        if (!shouldSpin) return@LaunchedEffect

        var previousNanos = withFrameNanos { it }

        while (true) {
            withFrameNanos { now ->
                val deltaSeconds = (now - previousNanos) / 1_000_000_000f
                previousNanos = now

                angleDegrees = (angleDegrees + SPIN_DEGREES_PER_SECOND * deltaSeconds) % 360f
            }
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription?.let { this.contentDescription = it } }
            .pointerInput(seekEnabled) {
                if (!seekEnabled) return@pointerInput

                var lastAngle = 0f
                var dragging = false
                var accumulatedShortMove = 0f

                detectDragGestures(
                    onDragStart = { offset ->
                        lastAngle = angleOf(offset, this.size.width / 2f, this.size.height / 2f)
                        dragging = false
                        accumulatedShortMove = 0f
                    },
                    onDragEnd = {
                        if (dragging) latestOnEnd()
                    },
                    onDragCancel = {
                        if (dragging) latestOnEnd()
                    }
                ) { change, _ ->
                    change.consume()

                    val current = angleOf(
                        change.position,
                        this.size.width / 2f,
                        this.size.height / 2f
                    )

                    var delta = current - lastAngle

                    // Переход через 0/360: приводим разницу к диапазону (-180, 180]
                    if (delta > 180f) delta -= 360f
                    if (delta < -180f) delta += 360f

                    lastAngle = current

                    // Слишком мелкие движения игнорируем: это не осознанная перемотка
                    accumulatedShortMove += kotlin.math.abs(delta)

                    if (dragging || accumulatedShortMove >= MIN_SEEK_MOVE_DEGREES) {
                        if (!dragging) {
                            dragging = true
                            latestOnStart()
                        }

                        angleDegrees = (angleDegrees + delta + 360f) % 360f
                        latestOnSeek(delta)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = this.size.minDimension / 2f
            val center = Offset(this.size.width / 2f, this.size.height / 2f)

            // Тень под диском
            drawCircle(
                color = Color.Black.copy(alpha = 0.35f),
                radius = radius,
                center = center.copy(y = center.y + radius * 0.04f)
            )

            rotate(degrees = angleDegrees, pivot = center) {
                drawDisc(radius = radius, center = center)
            }

            if (rotateCenter) {
                rotate(degrees = angleDegrees, pivot = center) {
                    drawLabel(radius = radius, center = center)
                }
            } else {
                drawLabel(radius = radius * 0.98f, center = center)
            }
        }

        // Обложка и нота поверх канвы: так проще подключить загрузку картинки
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(size * 0.36f),
            contentAlignment = Alignment.Center
        ) {
            if (artworkPath != null) {
                CircleArtwork(
                    artworkPath = artworkPath,
                    seed = seed,
                    size = size * 0.36f
                )
            }
        }

        if (artworkPath == null) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(size * 0.14f)
            )
        }
    }
}

/** Чёрный диск с бороздками и блик, чтобы пластинка выглядела объёмной */
private fun DrawScope.drawDisc(radius: Float, center: Offset) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF232228), Color(0xFF0B0B0E)),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )

    val grooveCount = 14
    val grooveStart = radius * 0.30f
    val grooveEnd = radius * 0.95f

    for (index in 0 until grooveCount) {
        val fraction = index.toFloat() / (grooveCount - 1).toFloat()
        val grooveRadius = grooveStart + (grooveEnd - grooveStart) * fraction

        val path = Path()
        val points = 260

        for (point in 0..points) {
            val angle = 2.0 * PI * point / points
            // Волна по кругу: бороздки выглядят «живыми», как в макете.
            // sin возвращает Double, поэтому приводим к Float до умножения
            // на Float-радиус — иначе вся геометрия станет Double
            val wave = sin(angle * 9.0 + fraction * 4.0).toFloat() * radius * 0.012f
            val r = grooveRadius + wave
            val x = center.x + cos(angle).toFloat() * r
            val y = center.y + sin(angle).toFloat() * r

            if (point == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        val alpha = if (index % 2 == 0) 0.28f else 0.16f

        // Path в Compose двумерный: x/y имеют тип Float, поэтому точка
        // на окружности приводится к Float до вызова moveTo/lineTo
        drawPath(
            path = path,
            color = Color.White.copy(alpha = alpha),
            style = Stroke(width = radius * 0.006f)
        )
    }

    // Блик слева сверху
    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
            start = Offset(center.x - radius * 0.6f, center.y - radius * 0.7f),
            end = Offset(center.x + radius * 0.3f, center.y + radius * 0.1f)
        ),
        radius = radius * 0.92f,
        center = center
    )
}

/** Розовая наклейка в центре диска */
private fun DrawScope.drawLabel(radius: Float, center: Offset) {
    val labelRadius = radius * 0.36f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(SharyPink, SharyPink.copy(alpha = 0.82f)),
            center = center,
            radius = labelRadius
        ),
        radius = labelRadius,
        center = center
    )

    // Тонкое кольцо по краю наклейки
    drawCircle(
        color = Color.Black.copy(alpha = 0.25f),
        radius = labelRadius,
        center = center,
        style = Stroke(width = radius * 0.006f)
    )

    // Отверстие spindle
    drawCircle(
        color = Color.Black.copy(alpha = 0.55f),
        radius = radius * 0.012f,
        center = center
    )
}

private fun angleOf(offset: Offset, centerX: Float, centerY: Float): Float {
    val degrees = Math.toDegrees(
        atan2((offset.y - centerY).toDouble(), (offset.x - centerX).toDouble())
    ).toFloat()

    return (degrees + 360f) % 360f
}

/** Проверяет системную настройку «убрать анимации» */
private fun areAnimationsEnabled(context: android.content.Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        if (!android.animation.ValueAnimator.areAnimatorsEnabled()) return false
    }

    val scale = runCatching {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
    }.getOrDefault(1f)

    return scale > 0f
}

/** Скорость вращения диска при воспроизведении, градусов в секунду */
private const val SPIN_DEGREES_PER_SECOND = 55f

/** Минимальный суммарный поворот пальца, после которого начинается перемотка */
private const val MIN_SEEK_MOVE_DEGREES = 4f
