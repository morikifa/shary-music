package com.example.sharedmusic.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.sharedmusic.ui.theme.SharyPink
import kotlin.math.abs

/**
 * Логотип SHARY: круг из 20 вертикальных полос, по 4 на каждую четверть.
 *
 * Полосы чередуют акцентный цвет и белый, высота плавно меняется по кругу —
 * получается «круговая волна», как в исходном логотипе.
 * Рисуется векторно, поэтому одинаково чётко выглядит на любом экране.
 */
@Composable
fun SharyLogo(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    accent: Color = SharyPink,
    secondary: Color = MaterialTheme.colorScheme.onBackground
) {
    Canvas(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "SHARY" }
    ) {
        val radius = this.size.minDimension / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)

        val barWidth = radius * 0.13f
        val barRadius = barWidth / 2f
        val ringRadius = radius * 0.72f

        val totalBars = 20

        for (index in 0 until totalBars) {
            val angle = 360f / totalBars * index

            // Волна: высота зависит от положения полосы, крайние — короткие
            val waveFactor = 0.55f + 0.45f * abs(
                kotlin.math.cos(Math.toRadians(angle.toDouble() * 2)).toFloat()
            )

            val barHeight = radius * 1.05f * waveFactor

            val isAccent = (index / 5) % 2 == 0 || index % 5 == 0
            val color = if (isAccent) accent else secondary

            rotate(degrees = angle, pivot = center) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(
                        center.x - barRadius,
                        center.y - ringRadius - barHeight / 2f
                    ),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barRadius)
                )
            }
        }
    }
}
