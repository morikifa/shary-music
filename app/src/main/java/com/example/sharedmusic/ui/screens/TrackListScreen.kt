package com.example.sharedmusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sharedmusic.R
import com.example.sharedmusic.data.model.Track
import com.example.sharedmusic.domain.TimeFormat
import com.example.sharedmusic.ui.components.PlaybackBars
import com.example.sharedmusic.ui.components.SearchBar
import com.example.sharedmusic.ui.components.SharyIconButton
import com.example.sharedmusic.ui.components.SharyLogo
import com.example.sharedmusic.ui.components.TrackArtwork
import com.example.sharedmusic.ui.theme.SharyTheme

/**
 * Список треков библиотеки.
 *
 * Открывается по розовому уголку или из нижней панели.
 * Здесь же поиск, сердечко и удаление трека.
 */
@Composable
fun TrackListScreen(
    tracks: List<Track>,
    currentTrackId: String?,
    isPlaying: Boolean,
    searchQuery: String,
    isSearchActive: Boolean,
    canDelete: Boolean,
    onTrackClick: (Track) -> Unit,
    onShareTrack: ((Track) -> Unit)? = null,
    onToggleLike: (Track) -> Unit,
    onDelete: (Track) -> Unit,
    onSearchChange: (String) -> Unit,
    onSearchOpen: () -> Unit,
    onSearchClose: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var trackToDelete by remember { mutableStateOf<Track?>(null) }
    // Показывать только понравившиеся: фильтр работает поверх поиска
    var likedOnly by remember { mutableStateOf(false) }

    val visible = if (likedOnly) tracks.filter { it.isLiked } else tracks

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SharyIconButton(
                icon = Icons.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
                onClick = onBack
            )

            SharyLogo(size = 26.dp)

            Column(modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)) {
                Text(
                    text = stringResource(R.string.library_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.library_track_count, tracks.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SearchBar(
                query = searchQuery,
                isActive = isSearchActive,
                onQueryChange = onSearchChange,
                onOpen = onSearchOpen,
                onClose = onSearchClose,
                modifier = Modifier.weight(if (isSearchActive) 1.4f else 0f)
            )
        }

        // Переключатель «Все / Понравившиеся»
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChipItem(
                label = stringResource(R.string.library_show_all),
                selected = !likedOnly,
                onClick = { likedOnly = false }
            )

            FilterChipItem(
                label = stringResource(R.string.library_liked_title),
                selected = likedOnly,
                onClick = { likedOnly = true }
            )
        }

        if (visible.isEmpty()) {
            EmptyState(
                text = when {
                    likedOnly -> stringResource(R.string.library_liked_empty)
                    searchQuery.isBlank() -> stringResource(R.string.state_empty_library)
                    else -> stringResource(R.string.state_no_results)
                }
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(items = visible, key = { it.id }) { track ->
                TrackRow(
                    track = track,
                    isCurrent = track.id == currentTrackId,
                    isPlaying = isPlaying && track.id == currentTrackId,
                    canDelete = canDelete,
                    onShare = onShareTrack?.let { share -> { share(track) } },
                    onClick = { onTrackClick(track) },
                    onToggleLike = { onToggleLike(track) },
                    onDelete = { trackToDelete = track }
                )
            }
        }
    }

    val pending = trackToDelete
    if (pending != null) {
        AlertDialog(
            onDismissRequest = { trackToDelete = null },
            title = { Text(stringResource(R.string.library_delete_track)) },
            text = { Text(stringResource(R.string.library_delete_confirm, pending.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(pending)
                        trackToDelete = null
                    }
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { trackToDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

/** Кнопка-переключатель фильтра списка */
@Composable
private fun FilterChipItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    )
}

/** Строка трека: обложка, название, исполнитель, длительность, лайк, удаление */
@Composable
private fun TrackRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    canDelete: Boolean,
    onShare: (() -> Unit)?,
    onClick: () -> Unit,
    onToggleLike: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isCurrent) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrackArtwork(
            artworkPath = track.artworkPath,
            seed = track.id,
            cornerRadius = 12.dp,
            contentDescription = track.title,
            modifier = Modifier.size(48.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrent) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1
            )

            Text(
                text = stringResource(
                    R.string.track_row_subtitle,
                    track.artist.ifBlank { stringResource(R.string.library_unknown_artist) },
                    TimeFormat.clock(track.durationMs)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        if (isPlaying) {
            PlaybackBars(
                color = MaterialTheme.colorScheme.primary,
                size = 14.dp,
                modifier = Modifier.padding(end = 6.dp)
            )
        }

        // Отправка трека участникам комнаты: доступна только из комнаты
        if (onShare != null) {
            SharyIconButton(
                icon = Icons.Rounded.Send,
                contentDescription = stringResource(R.string.room_track_share),
                onClick = onShare,
                size = 40.dp,
                iconSize = 19.dp
            )
        }

        SharyIconButton(
            icon = if (track.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = stringResource(
                if (track.isLiked) R.string.player_unlike else R.string.player_like
            ),
            onClick = onToggleLike,
            active = track.isLiked,
            size = 40.dp,
            iconSize = 19.dp
        )

        if (canDelete) {
            SharyIconButton(
                icon = Icons.Rounded.Delete,
                contentDescription = stringResource(R.string.library_delete_track),
                onClick = onDelete,
                size = 40.dp,
                iconSize = 19.dp
            )
        }
    }
}

/** Пустое состояние списка */
@Composable
private fun EmptyState(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(name = "Список треков", showBackground = true)
@Composable
private fun TrackListPreview() {
    SharyTheme {
        TrackListScreen(
            tracks = listOf(
                Track(
                    id = "1",
                    title = "NinjaristicNinja CHU CHU LOVELY",
                    artist = "Ninja",
                    fileName = "a.mp3",
                    filePath = "",
                    durationMs = 55_000,
                    sizeBytes = 1_000,
                    isLiked = true
                ),
                Track(
                    id = "2",
                    title = "Ночной город",
                    artist = "",
                    fileName = "b.mp3",
                    filePath = "",
                    durationMs = 180_000,
                    sizeBytes = 1_000
                )
            ),
            currentTrackId = "1",
            isPlaying = true,
            searchQuery = "",
            isSearchActive = false,
            canDelete = true,
            onTrackClick = {},
            onShareTrack = {},
            onToggleLike = {},
            onDelete = {},
            onSearchChange = {},
            onSearchOpen = {},
            onSearchClose = {},
            onBack = {}
        )
    }
}
