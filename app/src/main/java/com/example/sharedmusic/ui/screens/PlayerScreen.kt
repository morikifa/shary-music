package com.example.sharedmusic.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sharedmusic.R
import com.example.sharedmusic.data.model.RoomRole
import com.example.sharedmusic.data.model.RoomSyncStatus
import com.example.sharedmusic.data.model.Track
import com.example.sharedmusic.ui.components.CoverCarousel
import com.example.sharedmusic.ui.components.MarqueeText
import com.example.sharedmusic.ui.components.ModeChip
import com.example.sharedmusic.ui.components.PinkCorner
import com.example.sharedmusic.ui.components.PlayerControls
import com.example.sharedmusic.ui.components.SearchBar
import com.example.sharedmusic.ui.components.SharyIconButton
import com.example.sharedmusic.ui.components.SharyLogo
import com.example.sharedmusic.ui.components.VinylRecord
import com.example.sharedmusic.ui.theme.SharyPink
import com.example.sharedmusic.ui.theme.SharyTheme

/**
 * Главный экран «Сейчас играет».
 *
 * Верх — шапка с логотипом, меткой режима и поиском.
 * Центр — виниловая пластинка: она вращается при воспроизведении,
 * а вращение пальцем перематывает трек.
 * Ниже — название, сердечко, управление и лента обложек с кнопкой «+».
 */
@Composable
fun PlayerScreen(
    tracks: List<Track>,
    currentTrack: Track?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    shuffle: Boolean,
    repeatOne: Boolean,
    syncStatus: RoomSyncStatus,
    roomCode: String?,
    userRole: RoomRole,
    searchQuery: String,
    isSearchActive: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleLike: (Track) -> Unit,
    onSeek: (Long) -> Unit,
    onVinylDrag: (Float) -> Unit,
    onVinylDragStart: () -> Unit,
    onVinylDragEnd: () -> Unit,
    onSelectTrack: (Track) -> Unit,
    onAddTracks: (List<android.net.Uri>) -> Unit,
    onSearchChange: (String) -> Unit,
    onSearchOpen: () -> Unit,
    onSearchClose: () -> Unit,
    onOpenList: () -> Unit,
    onOpenRoom: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    onDownloadTrack: () -> Unit,
    isImporting: Boolean = false,
    modifier: Modifier = Modifier
) {
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) onAddTracks(uris)
    }

    val canControl = roomCode == null || userRole == RoomRole.HOST

    var showVinylHint by remember { mutableStateOf(true) }


    // Подсказка про вращение показывается только до первой перемотки
    val handleVinylStart: () -> Unit = {
        showVinylHint = false
        onVinylDragStart()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 12.dp)
    ) {
        // Шапка
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SharyLogo(size = 34.dp)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                ModeChip(roomCode = roomCode)
            }

            SearchBar(
                query = searchQuery,
                isActive = isSearchActive,
                onQueryChange = onSearchChange,
                onOpen = onSearchOpen,
                onClose = onSearchClose,
                modifier = Modifier.weight(if (isSearchActive) 1.6f else 0f)
            )
        }

        // Карточка плеера с загнутым уголком
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        VinylRecord(
                            isPlaying = isPlaying && canControl,
                            size = 236.dp,
                            artworkPath = currentTrack?.artworkPath,
                            seed = currentTrack?.id.orEmpty(),
                            seekEnabled = canControl && durationMs > 0L,
                            onSeekDegrees = onVinylDrag,
                            onSeekStart = handleVinylStart,
                            onSeekEnd = onVinylDragEnd,
                            contentDescription = stringResource(R.string.cd_vinyl)
                        )

                        if (showVinylHint && currentTrack != null) {
                            Text(
                                text = stringResource(R.string.player_scrub_hint),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Сердечко справа сверху
                    SharyIconButton(
                        icon = if (currentTrack?.isLiked == true) {
                            Icons.Rounded.Favorite
                        } else {
                            Icons.Rounded.FavoriteBorder
                        },
                        contentDescription = stringResource(R.string.player_like),
                        onClick = { currentTrack?.let(onToggleLike) },
                        active = currentTrack?.isLiked == true,
                        enabled = currentTrack != null,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Название и исполнитель: длинное название уплывает
                MarqueeText(
                    text = currentTrack?.title?.ifBlank {
                        stringResource(R.string.track_untitled)
                    } ?: stringResource(R.string.player_no_track),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fadeColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(30.dp)
                )

                Text(
                    text = currentTrack?.artist?.ifBlank {
                        stringResource(R.string.library_unknown_artist)
                    } ?: stringResource(R.string.player_add_tracks),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 2.dp, start = 24.dp, end = 24.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                PlayerControls(
                    isPlaying = isPlaying,
                    shuffle = shuffle,
                    repeatOne = repeatOne,
                    enabled = canControl && currentTrack != null,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onToggleShuffle = onToggleShuffle,
                    onToggleRepeat = onToggleRepeat,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    positionMs = positionMs,
                    durationMs = durationMs,
                    onSeek = onSeek
                )

                // Статус синхронизации в комнате
                AnimatedVisibility(
                    visible = syncStatus !is RoomSyncStatus.Synced && roomCode != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    SyncStatusLabel(status = syncStatus, onDownload = onDownloadTrack)
                }
            }

            // Загнутый уголок: открывает список треков
            PinkCorner(
                onClick = onOpenList,
                modifier = Modifier.align(Alignment.TopEnd),
                contentDescription = stringResource(R.string.player_open_list)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Пока файлы копируются и читаются теги — сообщаем, что процесс идёт
        AnimatedVisibility(
            visible = isImporting,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = stringResource(R.string.library_import_in_progress),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            )
        }

        // Лента обложек с кнопкой «+» слева
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AddButton(
                onClick = { picker.launch("audio/*") },
                enabled = tracks.size < 500 && !isImporting
            )

            Box(modifier = Modifier.weight(1f)) {
                if (tracks.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = stringResource(R.string.state_empty_library),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                } else {
                    CoverCarousel(
                        tracks = tracks,
                        centerTrackId = currentTrack?.id,
                        isPlaying = isPlaying,
                        onTrackSelect = onSelectTrack,
                        cardWidth = 118.dp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Нижние переходы: библиотека, комната, настройки, профиль
        PlayerBottomBar(
            onOpenList = onOpenList,
            onOpenRoom = onOpenRoom,
            onOpenChat = onOpenChat,
            onOpenSettings = onOpenSettings,
            onOpenProfile = onOpenProfile,
            roomActive = roomCode != null
        )
    }
}

/** Кнопка добавления музыки слева от ленты обложек */
@Composable
private fun AddButton(onClick: () -> Unit, enabled: Boolean) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = stringResource(R.string.player_add_tracks),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
    }
}

/** Подпись и кнопка скачивания для состояния синхронизации */
@Composable
private fun SyncStatusLabel(
    status: RoomSyncStatus,
    onDownload: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, start = 24.dp, end = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = when (status) {
                is RoomSyncStatus.CatchingUp -> stringResource(R.string.sync_catching_up)
                is RoomSyncStatus.TrackMissing -> stringResource(R.string.sync_track_missing)
                is RoomSyncStatus.Offline -> stringResource(R.string.state_offline)
                else -> stringResource(R.string.sync_synced)
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (status is RoomSyncStatus.TrackMissing) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )

        if (status is RoomSyncStatus.TrackMissing) {
            SharyIconButton(
                icon = Icons.Rounded.Download,
                contentDescription = stringResource(R.string.player_download),
                onClick = onDownload,
                size = 36.dp,
                iconSize = 18.dp,
                active = true
            )
        }
    }
}

/** Нижняя панель навигации главного экрана */
@Composable
private fun PlayerBottomBar(
    onOpenList: () -> Unit,
    onOpenRoom: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    roomActive: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomBarItem(
            icon = Icons.Rounded.LibraryMusic,
            label = stringResource(R.string.library_title),
            onClick = onOpenList,
            highlighted = true
        )

        BottomBarItem(
            icon = Icons.Rounded.Groups,
            label = if (roomActive) stringResource(R.string.chat_title)
            else stringResource(R.string.room_entry_title),
            onClick = if (roomActive) onOpenChat else onOpenRoom,
            highlighted = roomActive
        )

        BottomBarItem(
            icon = Icons.Rounded.Settings,
            label = stringResource(R.string.settings_title),
            onClick = onOpenSettings
        )

        BottomBarItem(
            icon = Icons.Rounded.Person,
            label = stringResource(R.string.profile_title),
            onClick = onOpenProfile
        )
    }
}

@Composable
private fun BottomBarItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    highlighted: Boolean = false
) {
    val tint by androidx.compose.animation.animateColorAsState(
        targetValue = if (highlighted) {
            SharyPink
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "bar-tint"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 1,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

// ---------- Превью ----------

private val previewTracks = listOf(
    Track(
        id = "1",
        title = "NinjaristicNinja CHU CHU LOVELY",
        artist = "Ninja",
        fileName = "a.mp3",
        filePath = "",
        durationMs = 55_000,
        sizeBytes = 1_000_000,
        isLiked = true
    ),
    Track(
        id = "2",
        title = "Ночной город",
        artist = "Неизвестный исполнитель",
        fileName = "b.mp3",
        filePath = "",
        durationMs = 180_000,
        sizeBytes = 1_000_000
    ),
    Track(
        id = "3",
        title = "Тихий дождь за окном",
        artist = "Ambient",
        fileName = "c.mp3",
        filePath = "",
        durationMs = 240_000,
        sizeBytes = 1_000_000
    )
)

@Preview(name = "Плеер — тёмная тема", showBackground = true)
@Composable
private fun PlayerScreenDarkPreview() {
    SharyTheme {
        PlayerScreen(
            tracks = previewTracks,
            currentTrack = previewTracks.first(),
            isPlaying = true,
            positionMs = 15_000,
            durationMs = 55_000,
            shuffle = true,
            repeatOne = false,
            syncStatus = RoomSyncStatus.Synced,
            roomCode = null,
            userRole = RoomRole.HOST,
            searchQuery = "",
            isSearchActive = false,
            onPlayPause = {},
            onNext = {},
            onPrevious = {},
            onToggleShuffle = {},
            onToggleRepeat = {},
            onToggleLike = {},
            onSeek = {},
            onVinylDrag = {},
            onVinylDragStart = {},
            onVinylDragEnd = {},
            onSelectTrack = {},
            onAddTracks = {},
            onSearchChange = {},
            onSearchOpen = {},
            onSearchClose = {},
            onOpenList = {},
            onOpenRoom = {},
            onOpenChat = {},
            onOpenSettings = {},
            onOpenProfile = {},
            onDownloadTrack = {}
        )
    }
}

@Preview(name = "Плеер — пустая библиотека", showBackground = true)
@Composable
private fun PlayerScreenEmptyPreview() {
    SharyTheme {
        Box(
            modifier = Modifier.background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
        ) {
            PlayerScreen(
                tracks = emptyList(),
                currentTrack = null,
                isPlaying = false,
                positionMs = 0,
                durationMs = 0,
                shuffle = false,
                repeatOne = false,
                syncStatus = RoomSyncStatus.Synced,
                roomCode = null,
                userRole = RoomRole.MEMBER,
                searchQuery = "",
                isSearchActive = false,
                onPlayPause = {},
                onNext = {},
                onPrevious = {},
                onToggleShuffle = {},
                onToggleRepeat = {},
                onToggleLike = {},
                onSeek = {},
                onVinylDrag = {},
                onVinylDragStart = {},
                onVinylDragEnd = {},
                onSelectTrack = {},
                onAddTracks = {},
                onSearchChange = {},
                onSearchOpen = {},
                onSearchClose = {},
                onOpenList = {},
                onOpenRoom = {},
                onOpenChat = {},
                onOpenSettings = {},
                onOpenProfile = {},
                onDownloadTrack = {}
            )
        }
    }
}
