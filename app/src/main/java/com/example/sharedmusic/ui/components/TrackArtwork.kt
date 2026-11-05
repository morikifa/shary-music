package com.example.sharedmusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.sharedmusic.domain.ArtworkGenerator

/**
 * Обложка трека.
 *
 * Если у файла нет встроенной картинки, рисуется градиент из ArtworkGenerator:
 * оттенок зависит от идентификатора трека, поэтому он стабилен между запусками.
 */
@Composable
fun TrackArtwork(
    artworkPath: String?,
    seed: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    contentDescription: String? = null
) {
    val brush = gradientBrush(seed)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush)
    ) {
        if (artworkPath != null) {
            AsyncImage(
                model = artworkPath,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(cornerRadius))
            )
        }
    }
}

/** Круглая обложка для центра пластинки */
@Composable
fun CircleArtwork(
    artworkPath: String?,
    seed: String,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val brush = gradientBrush(seed)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(brush),
        contentAlignment = Alignment.Center
    ) {
        if (artworkPath != null) {
            AsyncImage(
                model = artworkPath,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }
    }
}

/** Единый градиент для обложки-заглушки */
private fun gradientBrush(seed: String): Brush {
    val colors = ArtworkGenerator.gradientFor(seed)
    return Brush.linearGradient(listOf(Color(colors.first), Color(colors.second)))
}
