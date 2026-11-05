package com.example.sharedmusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sharedmusic.R
import com.example.sharedmusic.core.SharyConfig
import com.example.sharedmusic.data.model.RoomMember
import com.example.sharedmusic.data.model.RoomRole
import com.example.sharedmusic.data.model.RoomSyncStatus
import com.example.sharedmusic.data.model.Track
import com.example.sharedmusic.domain.TimeFormat
import com.example.sharedmusic.ui.components.MarqueeText
import com.example.sharedmusic.ui.components.MemberAvatar
import com.example.sharedmusic.ui.components.PinkCorner
import com.example.sharedmusic.ui.components.SharyIconButton
import com.example.sharedmusic.ui.components.SharyLogo
import com.example.sharedmusic.ui.components.TrackArtwork
import com.example.sharedmusic.ui.components.VinylRecord
import com.example.sharedmusic.ui.theme.SharyPink
import com.example.sharedmusic.ui.theme.SharyTheme
import com.example.sharedmusic.viewmodel.RoomTrackUi

/**
 * Экран комнаты.
 *
 * Сверху — выход, код комнаты, копирование и отправка кода.
 * Ниже — участники: у хоста розовое кольцо, у остальных — обычные кружки;
 * хост видит шестерёнку на себе и корзину у других участников.
 * В центре — пластинка (вращается только чёрная часть) и вертикальная
 * перемотка справа, рядом — вертикальная лента треков комнаты.
 */
@Composable
fun RoomScreen(
    roomCode: String,
    members: List<RoomMember>,
    roomTracks: List<RoomTrackUi>,
    currentTrack: Track?,
    currentTrackId: String?,
    currentTrackTitle: String,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    shuffle: Boolean,
    repeatOne: Boolean,
    syncStatus: RoomSyncStatus,
    currentUid: String?,
    isHost: Boolean,
    canControl: Boolean,
    isSearchActive: Boolean,
    searchQuery: String,
    transferProgress: Int?,
    onLeaveRoom: () -> Unit,
    onCopyCode: (String) -> Unit,
    onShareCode: (String) -> Unit,
    onOpenRoomSettings: () -> Unit,
    onKickMember: (String) -> Unit,
    onSelectTrack: (RoomTrackUi) -> Unit,
    onDownloadTrack: (RoomTrackUi) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleLike: (Track) -> Unit,
    onSeek: (Long) -> Unit,
    onOpenChat: () -> Unit,
    onOpenLibraryToShare: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var memberForKick by remember { mutableStateOf<RoomMember?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        // ---------- Шапка комнаты ----------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SharyIconButton(
                icon = Icons.Rounded.Logout,
                contentDescription = stringResource(R.string.room_leave),
                onClick = onLeaveRoom,
                size = 40.dp,
                iconSize = 20.dp
            )

            Spacer(modifier = Modifier.width(6.dp))

            SharyLogo(size = 22.dp)

            Spacer(modifier = Modifier.weight(1f))

            // Код комнаты
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#$roomCode",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                SharyIconButton(
                    icon = Icons.Rounded.ContentCopy,
                    contentDescription = stringResource(R.string.room_copy_code),
                    onClick = { onCopyCode(roomCode) },
                    size = 32.dp,
                    iconSize = 16.dp
                )

                SharyIconButton(
                    icon = Icons.Rounded.Share,
                    contentDescription = stringResource(R.string.room_share_code),
                    onClick = { onShareCode(roomCode) },
                    size = 32.dp,
                    iconSize = 16.dp
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // ---------- Участники ----------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.room_participants),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = stringResource(
                    R.string.room_participants_capacity,
                    members.size,
                    SharyConfig.MAX_ROOM_CAPACITY
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            items(items = members, key = { it.uid }) { member ->
                val isHostMember = member.role == RoomRole.HOST
                // Если Firebase ещё не вернул uid, помечаем себя по роли хоста
                val isMe = (currentUid != null && member.uid == currentUid) ||
                        (currentUid == null && isHost && isHostMember)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(64.dp)
                ) {
                    Box {
                        MemberAvatar(
                            member = member,
                            size = if (isHostMember) 52.dp else 44.dp,
                            isHost = isHostMember
                        )

                        // Шестерёнка на себе у хоста: настройки комнаты
                        if (isMe && isHost) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable(onClick = onOpenRoomSettings),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = stringResource(R.string.room_settings),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        // Корзина у других участников: только для хоста
                        if (isHost && !isMe) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable { memberForKick = member },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = stringResource(R.string.room_kick),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = if (isMe) stringResource(R.string.room_you) else member.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isHostMember) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ---------- Карточка воспроизведения ----------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(26.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 56.dp, top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        MarqueeText(
                            text = currentTrackTitle.ifBlank {
                                stringResource(R.string.player_no_track)
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fadeColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.height(28.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = TimeFormat.clock(positionMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = " | ${TimeFormat.clock(durationMs)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (syncStatus is RoomSyncStatus.TrackMissing) {
                                SharyIconButton(
                                    icon = Icons.Rounded.Download,
                                    contentDescription = stringResource(R.string.player_download),
                                    onClick = {
                                        roomTracks.firstOrNull {
                                            it.track.trackId == currentTrackId
                                        }?.let(onDownloadTrack)
                                    },
                                    size = 30.dp,
                                    iconSize = 15.dp,
                                    active = true
                                )
                            }
                        }
                    }

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
                        size = 36.dp,
                        iconSize = 18.dp
                    )
                }

                // Пластинка + вертикальная перемотка + вертикальная лента
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        val configuration = LocalConfiguration.current
                        val vinylSize = (configuration.screenWidthDp * 0.56f).dp

                        VinylRecord(
                            isPlaying = isPlaying && canControl,
                            size = vinylSize,
                            artworkPath = currentTrack?.artworkPath,
                            seed = currentTrack?.id.orEmpty(),
                            rotateCenter = false,
                            seekEnabled = canControl && durationMs > 0L,
                            onSeekDegrees = { delta ->
                                onSeek(progressFromDegrees(positionMs, durationMs, delta))
                            },
                            contentDescription = stringResource(R.string.cd_vinyl)
                        )
                    }

                    VerticalSeekBar(
                        progress = if (durationMs > 0L) {
                            positionMs.toFloat() / durationMs.toFloat()
                        } else {
                            0f
                        },
                        enabled = canControl && durationMs > 0L,
                        onSeekFraction = { fraction ->
                            onSeek((durationMs * fraction).toLong())
                        },
                        modifier = Modifier
                            .width(28.dp)
                            .fillMaxSize()
                            .padding(vertical = 12.dp)
                    )

                    VerticalTrackRail(
                        tracks = roomTracks,
                        currentTrackId = currentTrack?.id,
                        isHost = isHost,
                        canControl = canControl,
                        onSelectTrack = onSelectTrack,
                        onAddTrack = onOpenLibraryToShare,
                        modifier = Modifier
                            .width(78.dp)
                            .fillMaxSize()
                            .padding(vertical = 6.dp, horizontal = 4.dp)
                    )
                }

                if (transferProgress != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.room_download_progress,
                                transferProgress
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        LinearProgressIndicator(
                            progress = { transferProgress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp)
                        )
                    }
                }

                // Управление комнатой: розовые кнопки только при праве управления
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SharyIconButton(
                        icon = Icons.Rounded.Shuffle,
                        contentDescription = stringResource(R.string.player_shuffle),
                        onClick = onToggleShuffle,
                        active = shuffle,
                        enabled = canControl,
                        size = 40.dp,
                        iconSize = 19.dp
                    )

                    SharyIconButton(
                        icon = Icons.Rounded.SkipPrevious,
                        contentDescription = stringResource(R.string.player_previous),
                        onClick = onPrevious,
                        enabled = canControl,
                        size = 44.dp,
                        iconSize = 23.dp
                    )

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(SharyPink, SharyPink.copy(alpha = 0.75f))))
                            .clickable(enabled = canControl, onClick = onPlayPause),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(
                                if (isPlaying) R.string.player_pause else R.string.player_play
                            ),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    SharyIconButton(
                        icon = Icons.Rounded.SkipNext,
                        contentDescription = stringResource(R.string.player_next),
                        onClick = onNext,
                        enabled = canControl,
                        size = 44.dp,
                        iconSize = 23.dp
                    )

                    SharyIconButton(
                        icon = Icons.Rounded.RepeatOne,
                        contentDescription = stringResource(R.string.player_repeat_one),
                        onClick = onToggleRepeat,
                        active = repeatOne,
                        enabled = canControl,
                        size = 40.dp,
                        iconSize = 19.dp
                    )
                }
            }

            // Розовый уголок открывает чат комнаты
            PinkCorner(
                onClick = onOpenChat,
                modifier = Modifier.align(Alignment.TopEnd),
                contentDescription = stringResource(R.string.player_open_chat)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Подпись режима синхронизации
        Text(
            text = when (syncStatus) {
                is RoomSyncStatus.CatchingUp -> stringResource(R.string.sync_catching_up)
                is RoomSyncStatus.TrackMissing -> stringResource(R.string.sync_track_missing)
                is RoomSyncStatus.Offline -> stringResource(R.string.state_offline)
                else -> stringResource(R.string.sync_synced)
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (syncStatus is RoomSyncStatus.Synced) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.primary
            },
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        )
    }

    val pendingKick = memberForKick
    if (pendingKick != null) {
        AlertDialog(
            onDismissRequest = { memberForKick = null },
            title = { Text(stringResource(R.string.room_kick)) },
            text = { Text(stringResource(R.string.room_kick_confirm, pendingKick.displayName)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onKickMember(pendingKick.uid)
                        memberForKick = null
                    }
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { memberForKick = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

/** Пересчитывает позицию после поворота пластинки в комнате */
private fun progressFromDegrees(positionMs: Long, durationMs: Long, deltaDegrees: Float): Long {
    if (durationMs <= 0L) return positionMs

    val perDegree = durationMs.toFloat() / com.example.sharedmusic.domain.VinylGeometry.degreesPerTrack()

    return (positionMs + deltaDegrees * perDegree).toLong().coerceIn(0L, durationMs)
}

/** Вертикальная перемотка: серая, при перетаскивании становится розовой */
@Composable
private fun VerticalSeekBar(
    progress: Float,
    enabled: Boolean,
    onSeekFraction: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput

                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val fraction = 1f - (offset.y / size.height.toFloat())
                        onSeekFraction(fraction.coerceIn(0f, 1f))
                    },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false }
                ) { change, _ ->
                    change.consume()
                    val fraction = 1f - (change.position.y / size.height.toFloat())
                    onSeekFraction(fraction.coerceIn(0f, 1f))
                }
            }
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        // Фон полосы
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(50))
                .background(
                    if (isDragging) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
        )

        // Заполненная часть
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.verticalGradient(
                        colors = if (isDragging || enabled) {
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            )
                        } else {
                            listOf(
                                MaterialTheme.colorScheme.outline,
                                MaterialTheme.colorScheme.outline
                            )
                        }
                    )
                )
                .fillMaxHeight(progress.coerceIn(0f, 1f))
                .align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

/** Вертикальная лента треков комнаты: листается вверх-вниз */
@Composable
private fun VerticalTrackRail(
    tracks: List<RoomTrackUi>,
    currentTrackId: String?,
    isHost: Boolean,
    canControl: Boolean,
    onSelectTrack: (RoomTrackUi) -> Unit,
    onAddTrack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        // «+» — добавить музыку в комнату
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(enabled = isHost || canControl, onClick = onAddTrack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.room_add_track),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Пустой плейлист: подсказываем, что треки добавляет хост
        if (tracks.isEmpty()) {
            Text(
                text = stringResource(R.string.track_library_empty),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(items = tracks, key = { it.track.trackId }) { item ->
                val isCurrent = item.track.trackId == currentTrackId

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isCurrent) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                        .padding(if (isCurrent) 2.dp else 1.dp)
                        .clickable(enabled = canControl, onClick = { onSelectTrack(item) })
                ) {
                    TrackArtwork(
                        artworkPath = null,
                        seed = item.track.trackId,
                        cornerRadius = 14.dp,
                        contentDescription = item.track.title,
                        modifier = Modifier.size(60.dp)
                    )

                    if (!item.isAvailableLocally) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = stringResource(R.string.room_download_missing),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------- Превью ----------

private fun previewMembers(): List<RoomMember> = listOf(
    RoomMember(uid = "1", displayName = "Вы", role = RoomRole.HOST),
    RoomMember(uid = "2", displayName = "samsung SM-A576B", role = RoomRole.MEMBER),
    RoomMember(uid = "3", displayName = "Пользователь", role = RoomRole.MEMBER)
)

private fun previewRoomTracks(): List<RoomTrackUi> = List(5) { index ->
    RoomTrackUi(
        track = com.example.sharedmusic.data.model.RoomTrack(
            trackId = "track-$index",
            title = "Трек $index",
            artist = "Исполнитель",
            addedByUid = "1",
            order = index
        ),
        isAvailableLocally = index % 2 == 0,
        addedByMe = index == 0
    )
}

@Preview(name = "Комната — хост", showBackground = true)
@Composable
private fun RoomScreenPreview() {
    SharyTheme {
        RoomScreen(
            roomCode = "7KX2MQ",
            members = previewMembers(),
            roomTracks = previewRoomTracks(),
            currentTrack = null,
            currentTrackId = "track-0",
            currentTrackTitle = "NinjaristicNinja CHU CHU LOVELY",
            isPlaying = true,
            positionMs = 15_000,
            durationMs = 55_000,
            shuffle = false,
            repeatOne = true,
            syncStatus = RoomSyncStatus.Synced,
            currentUid = "1",
            isHost = true,
            canControl = true,
            isSearchActive = false,
            searchQuery = "",
            transferProgress = null,
            onLeaveRoom = {},
            onCopyCode = {},
            onShareCode = {},
            onOpenRoomSettings = {},
            onKickMember = {},
            onSelectTrack = {},
            onDownloadTrack = {},
            onPlayPause = {},
            onNext = {},
            onPrevious = {},
            onToggleShuffle = {},
            onToggleRepeat = {},
            onToggleLike = {},
            onSeek = {},
            onOpenChat = {},
            onOpenLibraryToShare = {},
            onOpenSearch = {},
            onCloseSearch = {},
            onSearchChange = {}
        )
    }
}

@Preview(name = "Комната — гость", showBackground = true)
@Composable
private fun RoomScreenGuestPreview() {
    SharyTheme {
        RoomScreen(
            roomCode = "7KX2MQ",
            members = previewMembers(),
            roomTracks = previewRoomTracks(),
            currentTrack = null,
            currentTrackId = "track-0",
            currentTrackTitle = "Тихий дождь за окном",
            isPlaying = false,
            positionMs = 42_000,
            durationMs = 180_000,
            shuffle = true,
            repeatOne = false,
            syncStatus = RoomSyncStatus.TrackMissing,
            currentUid = "2",
            isHost = false,
            canControl = false,
            isSearchActive = false,
            searchQuery = "",
            transferProgress = 40,
            onLeaveRoom = {},
            onCopyCode = {},
            onShareCode = {},
            onOpenRoomSettings = {},
            onKickMember = {},
            onSelectTrack = {},
            onDownloadTrack = {},
            onPlayPause = {},
            onNext = {},
            onPrevious = {},
            onToggleShuffle = {},
            onToggleRepeat = {},
            onToggleLike = {},
            onSeek = {},
            onOpenChat = {},
            onOpenLibraryToShare = {},
            onOpenSearch = {},
            onCloseSearch = {},
            onSearchChange = {}
        )
    }
}
