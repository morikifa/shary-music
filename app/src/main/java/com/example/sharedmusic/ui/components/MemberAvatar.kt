package com.example.sharedmusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.sharedmusic.R
import com.example.sharedmusic.data.model.RoomMember
import com.example.sharedmusic.ui.theme.SharyPink
import com.example.sharedmusic.ui.theme.SharyPinkSoft

/**
 * Круглый аватар участника.
 *
 * Если фото нет или файл не найден, показывается базовый значок человека
 * в розовом градиенте — участники без аватара не выглядят «сломанными».
 * У хоста кружок обведён розовым и чуть больше остальных.
 */
@Composable
fun MemberAvatar(
    member: RoomMember,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    isHost: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val ringSize = if (isHost) size + 6.dp else size

    val baseModifier = modifier
        .size(ringSize)
        .then(
            if (isHost) {
                Modifier
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(SharyPink, SharyPinkSoft)))
                    .padding(3.dp)
            } else {
                Modifier
            }
        )
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

    Surface(
        modifier = baseModifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        if (member.avatarPath != null) {
            // Пустое имя не должно становиться пустой подписью для TalkBack
            AsyncImage(
                model = member.avatarPath,
                contentDescription = member.displayName.ifBlank {
                    stringResource(R.string.room_participant_default)
                },
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
            )
        } else {
            PlaceholderAvatar(size = size)
        }
    }
}

/** Заглушка аватара: значок человека на розовом градиенте */
@Composable
private fun PlaceholderAvatar(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(SharyPink, SharyPinkSoft))),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = stringResource(R.string.cd_avatar),
            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
            modifier = Modifier.size(size * 0.6f)
        )
    }
}
