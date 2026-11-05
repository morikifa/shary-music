package com.example.sharedmusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.sharedmusic.R

/**
 * Постоянная метка режима: «Локально» или «Комната ABC123».
 * Продуктовое требование: режим всегда виден пользователю.
 */
@Composable
fun ModeChip(
    roomCode: String?,
    modifier: Modifier = Modifier
) {
    val inRoom = roomCode != null

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (inRoom) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(
                    if (inRoom) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
        )

        Text(
            text = if (inRoom) {
                stringResource(R.string.mode_room, roomCode ?: "")
            } else {
                stringResource(R.string.mode_local)
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (inRoom) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}
