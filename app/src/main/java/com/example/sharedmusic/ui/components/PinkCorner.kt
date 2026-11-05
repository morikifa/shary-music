package com.example.sharedmusic.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Розовый уголок в правом верхнем углу карточки.
 * В макете это «загнутый» угол: по нажатию открывается список треков
 * или чат комнаты — в зависимости от экрана.
 */
@Composable
fun PinkCorner(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    contentDescription: String? = null
) {
    val accent = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = modifier
            .size(size)
            .clickable(onClick = onClick)
            .semantics { contentDescription?.let { this.contentDescription = it } }
    ) {
        val width = this.size.width
        val height = this.size.height

        // Треугольник от правого верхнего угла к нижнему левому
        val corner = Path().apply {
            moveTo(width, 0f)
            lineTo(width, height)
            lineTo(0f, 0f)
            close()
        }

        drawPath(path = corner, color = accent)
    }
}
