package com.example.sharedmusic.ui.navigation

import android.content.Intent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.sharedmusic.data.model.RoomRole
import com.example.sharedmusic.data.model.Track
import com.example.sharedmusic.ui.screens.ChatSheet
import com.example.sharedmusic.ui.screens.HistoryScreen
import com.example.sharedmusic.ui.screens.OnboardingScreen
import com.example.sharedmusic.ui.screens.PlayerScreen
import com.example.sharedmusic.ui.screens.ProfileScreen
import com.example.sharedmusic.ui.screens.RoomEntryScreen
import com.example.sharedmusic.ui.screens.RoomScreen
import com.example.sharedmusic.ui.screens.RoomSettingsDialog
import com.example.sharedmusic.ui.screens.SettingsScreen
import com.example.sharedmusic.ui.screens.TrackListScreen
import com.example.sharedmusic.ui.theme.SharyTheme
import com.example.sharedmusic.viewmodel.AppViewModel
import com.example.sharedmusic.viewmodel.MessageRes
import com.example.sharedmusic.viewmodel.PlayerViewModel
import com.example.sharedmusic.viewmodel.RoomViewModel
import kotlinx.coroutines.launch

/** Маршруты приложения */
private object Routes {
    const val ONBOARDING = "onboarding"
    const val PLAYER = "player"
    const val TRACK_LIST = "tracks"
    const val ROOM_ENTRY = "room_entry"
    const val ROOM = "room"
    const val CHAT = "chat"
    const val SETTINGS = "settings"
    const val PROFILE = "profile"
    const val HISTORY = "history"
}

/**
 * Каркас навигации SHARY.
 *
 * Переходы короткие и мягкие: новый экран въезжает справа, старый плавно
 * исчезает. Системная кнопка «Назад» работает штатно через NavHost.
 */
@Composable
fun SharyNavigation(
    appViewModel: AppViewModel,
    playerViewModel: PlayerViewModel,
    roomViewModel: RoomViewModel,
    modifier: Modifier = Modifier
) {
    val profile by appViewModel.profile.collectAsStateWithLifecycle()
    val settings by appViewModel.settings.collectAsStateWithLifecycle()
    val profileLoaded by appViewModel.profileLoaded.collectAsStateWithLifecycle()

    val navController = rememberNavController()

    val startDestination = if (profile.isConfigured) Routes.PLAYER else Routes.ONBOARDING

    SharyTheme(themeMode = settings.themeMode) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            // Профиль ещё читается из DataStore: стартовый экран неизвестен,
            // поэтому вместо мигания онбордингом показываем пустой фон
            if (!profileLoaded) return@Surface

            NavHost(
                navController = navController,
                startDestination = startDestination,
                enterTransition = {
                    slideInHorizontally(
                        animationSpec = tween(260),
                        initialOffsetX = { it / 6 }
                    ) + fadeIn(animationSpec = tween(220))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(160))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(200))
                },
                popExitTransition = {
                    slideOutHorizontally(
                        animationSpec = tween(240),
                        targetOffsetX = { it / 6 }
                    ) + fadeOut(animationSpec = tween(180))
                }
            ) {
                composable(Routes.ONBOARDING) {
                    OnboardingScreen(
                        onContinue = { name ->
                            appViewModel.saveDisplayName(name)
                            // popUpTo(граф) без inclusive: онбординг уходит из стека,
                            // а плеер становится единственным экраном
                            navController.navigate(Routes.PLAYER) {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Routes.PLAYER) {
                    PlayerRoute(
                        appViewModel = appViewModel,
                        playerViewModel = playerViewModel,
                        onOpenList = { navController.navigate(Routes.TRACK_LIST) },
                        onOpenRoom = { navController.navigate(Routes.ROOM_ENTRY) },
                        onOpenChat = { navController.navigate(Routes.CHAT) },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                        onOpenProfile = { navController.navigate(Routes.PROFILE) }
                    )
                }

                composable(Routes.TRACK_LIST) {
                    val activeRoom by roomViewModel.roomCode.collectAsStateWithLifecycle()

                    // Если экран открыт из комнаты, в строках появляется отправка трека
                    TrackListRoute(
                        playerViewModel = playerViewModel,
                        onBack = { navController.popBackStack() },
                        onShareTrack = if (activeRoom == null) {
                            null
                        } else {
                            { track ->
                                roomViewModel.shareTrack(track)
                                navController.popBackStack()
                            }
                        }
                    )
                }

                composable(Routes.ROOM_ENTRY) {
                    val roomCode by roomViewModel.roomCode.collectAsStateWithLifecycle()
                    val isLoading by roomViewModel.isLoading.collectAsStateWithLifecycle()
                    val notice by roomViewModel.notice.collectAsStateWithLifecycle()

                    // Как только комната создана или найдена, открываем её экран
                    LaunchedEffect(roomCode) {
                        if (roomCode != null) {
                            navController.navigate(Routes.ROOM) {
                                popUpTo(Routes.ROOM_ENTRY) { inclusive = true }
                            }
                        }
                    }

                    val context = LocalContext.current

                    RoomEntryScreen(
                        isLoading = isLoading,
                        notice = notice?.resolve(context),
                        onCreateRoom = roomViewModel::createRoom,
                        onJoinRoom = roomViewModel::joinRoom,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.ROOM) {
                    RoomRoute(
                        roomViewModel = roomViewModel,
                        playerViewModel = playerViewModel,
                        onOpenChat = { navController.navigate(Routes.CHAT) },
                        onOpenShareLibrary = { navController.navigate(Routes.TRACK_LIST) },
                        onLeftRoom = {
                            navController.navigate(Routes.PLAYER) {
                                popUpTo(Routes.PLAYER) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Routes.CHAT) {
                    ChatRoute(
                        roomViewModel = roomViewModel,
                        onClose = { navController.popBackStack() }
                    )
                }

                composable(Routes.SETTINGS) {
                    val currentSettings by appViewModel.settings.collectAsStateWithLifecycle()

                    SettingsScreen(
                        settings = currentSettings,
                        onBack = { navController.popBackStack() },
                        onToggleEqualizer = appViewModel::setEqualizerEnabled,
                        onChangeEqualizerBand = appViewModel::setEqualizerBand,
                        onToggleWifiOnly = appViewModel::setWifiOnly,
                        onChangeTheme = appViewModel::setThemeMode
                    )
                }

                composable(Routes.PROFILE) {
                    ProfileRoute(
                        appViewModel = appViewModel,
                        onBack = { navController.popBackStack() },
                        onOpenHistory = { navController.navigate(Routes.HISTORY) }
                    )
                }

                composable(Routes.HISTORY) {
                    val history by appViewModel.history.collectAsStateWithLifecycle()

                    HistoryScreen(
                        entries = history,
                        onBack = { navController.popBackStack() },
                        onClear = appViewModel::clearHistory
                    )
                }
            }
        }
    }
}

/** Экран плеера с подпиской на состояние */
@Composable
private fun PlayerRoute(
    appViewModel: AppViewModel,
    playerViewModel: PlayerViewModel,
    onOpenList: () -> Unit,
    onOpenRoom: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()
    val positionMs by playerViewModel.positionMs.collectAsStateWithLifecycle()
    val durationMs by playerViewModel.durationMs.collectAsStateWithLifecycle()
    val shuffle by playerViewModel.shuffle.collectAsStateWithLifecycle()
    val repeatOne by playerViewModel.repeatOne.collectAsStateWithLifecycle()
    val syncStatus by playerViewModel.syncStatus.collectAsStateWithLifecycle()
    val searchQuery by playerViewModel.searchQuery.collectAsStateWithLifecycle()
    val isSearchActive by playerViewModel.isSearchActive.collectAsStateWithLifecycle()
    val message by playerViewModel.message.collectAsStateWithLifecycle()
    val isImporting by playerViewModel.isImporting.collectAsStateWithLifecycle()

    val roomCode by appViewModel.roomCode.collectAsStateWithLifecycle()
    val role by appViewModel.role.collectAsStateWithLifecycle()

    // Список уже учитывает поисковый запрос
    val tracks by playerViewModel.visibleTracks.collectAsStateWithLifecycle()
    val libraryTracks by playerViewModel.tracks.collectAsStateWithLifecycle()
    val currentTrackId by playerViewModel.currentTrackId.collectAsStateWithLifecycle()

    val currentTrack = libraryTracks.firstOrNull { it.id == currentTrackId }

    // Сообщение показывается один раз и сбрасывается, чтобы не повторялось
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        val current = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(current.resolve(context))
        playerViewModel.clearMessage()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PlayerScreen(
            tracks = tracks,
            currentTrack = currentTrack,
            isPlaying = isPlaying,
            positionMs = positionMs,
            durationMs = durationMs,
            shuffle = shuffle,
            repeatOne = repeatOne,
            syncStatus = syncStatus,
            roomCode = roomCode,
            userRole = role,
            searchQuery = searchQuery,
            isSearchActive = isSearchActive,
            onPlayPause = playerViewModel::togglePlayPause,
            onNext = playerViewModel::playNext,
            onPrevious = playerViewModel::playPrevious,
            onToggleShuffle = playerViewModel::toggleShuffle,
            onToggleRepeat = playerViewModel::toggleRepeatOne,
            onToggleLike = playerViewModel::toggleLike,
            onSeek = playerViewModel::seekTo,
            onVinylDrag = playerViewModel::onVinylDrag,
            onVinylDragStart = playerViewModel::onVinylDragStart,
            onVinylDragEnd = playerViewModel::onVinylDragEnd,
            onSelectTrack = playerViewModel::playTrack,
            onAddTracks = playerViewModel::addTracks,
            onSearchChange = playerViewModel::updateSearch,
            onSearchOpen = playerViewModel::openSearch,
            onSearchClose = playerViewModel::closeSearch,
            onOpenList = onOpenList,
            onOpenRoom = onOpenRoom,
            onOpenChat = onOpenChat,
            onOpenSettings = onOpenSettings,
            onOpenProfile = onOpenProfile,
            onDownloadTrack = playerViewModel::downloadCurrentRoomTrack,
            isImporting = isImporting
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 76.dp)
        )
    }
}

/** Список треков библиотеки */
@Composable
private fun TrackListRoute(
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit,
    onShareTrack: ((Track) -> Unit)? = null
) {
    val currentTrackId by playerViewModel.currentTrackId.collectAsStateWithLifecycle()
    val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()
    val searchQuery by playerViewModel.searchQuery.collectAsStateWithLifecycle()
    val isSearchActive by playerViewModel.isSearchActive.collectAsStateWithLifecycle()
    val tracks by playerViewModel.visibleTracks.collectAsStateWithLifecycle()

    TrackListScreen(
        tracks = tracks,
        currentTrackId = currentTrackId,
        isPlaying = isPlaying,
        searchQuery = searchQuery,
        isSearchActive = isSearchActive,
        canDelete = true,
        onTrackClick = playerViewModel::playTrack,
        onShareTrack = onShareTrack,
        onToggleLike = playerViewModel::toggleLike,
        onDelete = playerViewModel::deleteTrack,
        onSearchChange = playerViewModel::updateSearch,
        onSearchOpen = playerViewModel::openSearch,
        onSearchClose = playerViewModel::closeSearch,
        onBack = onBack
    )
}

/** Экран активной комнаты */
@Composable
private fun RoomRoute(
    roomViewModel: RoomViewModel,
    playerViewModel: PlayerViewModel,
    onOpenChat: () -> Unit,
    onLeftRoom: () -> Unit,
    onOpenShareLibrary: () -> Unit
) {
    val roomCode by roomViewModel.roomCode.collectAsStateWithLifecycle()
    val role by roomViewModel.role.collectAsStateWithLifecycle()
    val members by roomViewModel.members.collectAsStateWithLifecycle()
    val hostOnlyControl by roomViewModel.hostOnlyControl.collectAsStateWithLifecycle()
    val roomTracks by roomViewModel.roomTracksWithAvailability.collectAsStateWithLifecycle()
    val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()
    val positionMs by playerViewModel.positionMs.collectAsStateWithLifecycle()
    val durationMs by playerViewModel.durationMs.collectAsStateWithLifecycle()
    val shuffle by playerViewModel.shuffle.collectAsStateWithLifecycle()
    val repeatOne by playerViewModel.repeatOne.collectAsStateWithLifecycle()
    val syncStatus by playerViewModel.syncStatus.collectAsStateWithLifecycle()
    val currentTrackId by playerViewModel.currentTrackId.collectAsStateWithLifecycle()
    val transferProgress by roomViewModel.transferProgress.collectAsStateWithLifecycle()
    val isTransferring by roomViewModel.isTransferring.collectAsStateWithLifecycle()
    val roomClosed by roomViewModel.roomClosed.collectAsStateWithLifecycle()
    val notice by roomViewModel.notice.collectAsStateWithLifecycle()

    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    // Уведомления комнаты (трек отправлен, пароль изменён и т.п.) показываем всплывающей плашкой
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(notice) {
        val current = notice ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(current.resolve(context))
        roomViewModel.clearNotice()
    }

    var showSettings by remember { mutableStateOf(false) }

    val code = roomCode
    val isHost = role == RoomRole.HOST
    val canControl = !hostOnlyControl || isHost
    val currentTrack = playerViewModel.currentTrack()

    // Комната закрыта хостом или потеряна — возвращаемся к локальному прослушиванию
    LaunchedEffect(roomClosed, code) {
        if (roomClosed || code == null) onLeftRoom()
    }

    if (code == null) return

    Box(modifier = Modifier.fillMaxSize()) {
        RoomScreen(
            roomCode = code,
            members = members,
            roomTracks = roomTracks,
            currentTrack = currentTrack,
            currentTrackId = currentTrackId,
            currentTrackTitle = currentTrack?.title
                ?: roomTracks.firstOrNull()?.track?.title.orEmpty(),
            isPlaying = isPlaying,
            positionMs = positionMs,
            durationMs = durationMs,
            shuffle = shuffle,
            repeatOne = repeatOne,
            syncStatus = syncStatus,
            currentUid = roomViewModel.currentUid,
            isHost = isHost,
            canControl = canControl,
            isSearchActive = false,
            searchQuery = "",
            transferProgress = if (isTransferring) transferProgress else null,
            onLeaveRoom = roomViewModel::leaveRoom,
            onCopyCode = { value ->
                clipboard.setText(AnnotatedString(value))
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.room_code_copied))
                }
            },
            onShareCode = { value ->
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                        Intent.EXTRA_TEXT,
                        context.getString(R.string.room_share_text, value)
                    )
                }
                runCatching { context.startActivity(Intent.createChooser(intent, null)) }
            },
            onOpenRoomSettings = { showSettings = true },
            onKickMember = roomViewModel::kickMember,
            onSelectTrack = { item ->
                playerViewModel.tracks.value
                    .firstOrNull { it.id == item.track.trackId }
                    ?.let(playerViewModel::playTrack)
            },
            onDownloadTrack = { item -> roomViewModel.downloadTrack(item.track) },
            onPlayPause = playerViewModel::togglePlayPause,
            onNext = playerViewModel::playNext,
            onPrevious = playerViewModel::playPrevious,
            onToggleShuffle = playerViewModel::toggleShuffle,
            onToggleRepeat = playerViewModel::toggleRepeatOne,
            onToggleLike = playerViewModel::toggleLike,
            onSeek = playerViewModel::seekTo,
            onOpenChat = onOpenChat,
            onOpenLibraryToShare = {
                // Открываем библиотеку: хост выбирает, какой трек отправить участникам
                onOpenShareLibrary()
            },
            onOpenSearch = {},
            onCloseSearch = {},
            onSearchChange = {}
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        )
    }

    if (showSettings) {
        RoomSettingsDialog(
            hostOnlyControl = hostOnlyControl,
            onDismiss = { showSettings = false },
            onChangeHostOnlyControl = roomViewModel::setHostOnlyControl,
            onChangePassword = roomViewModel::changePassword,
            onDeleteRoom = {
                showSettings = false
                roomViewModel.closeRoom()
            }
        )
    }
}

/** Чат комнаты */
@Composable
private fun ChatRoute(
    roomViewModel: RoomViewModel,
    onClose: () -> Unit
) {
    val roomCode by roomViewModel.roomCode.collectAsStateWithLifecycle()
    val messages by roomViewModel.messages.collectAsStateWithLifecycle()
    val input by roomViewModel.chatInput.collectAsStateWithLifecycle()
    val error by roomViewModel.chatError.collectAsStateWithLifecycle()
    val isSending by roomViewModel.isSending.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val code = roomCode

    // Без активной комнаты чат не имеет смысла: сразу возвращаемся
    LaunchedEffect(code) {
        if (code == null) onClose()
    }

    if (code == null) return

    ChatSheet(
        roomCode = code,
        messages = messages,
        input = input,
        error = error?.resolve(context),
        currentUid = roomViewModel.currentUid,
        isSending = isSending,
        onInputChange = roomViewModel::updateChatInput,
        onSend = roomViewModel::sendMessage,
        onClose = onClose
    )
}

/** Профиль пользователя */
@Composable
private fun ProfileRoute(
    appViewModel: AppViewModel,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit
) {
    val profile by appViewModel.profile.collectAsStateWithLifecycle()
    val trackCount by appViewModel.trackCount.collectAsStateWithLifecycle()
    val likedCount by appViewModel.likedCount.collectAsStateWithLifecycle()
    val roomHours by appViewModel.roomHours.collectAsStateWithLifecycle()

    ProfileScreen(
        profile = profile,
        trackCount = trackCount,
        likedCount = likedCount,
        roomHours = roomHours,
        onBack = onBack,
        onSaveName = appViewModel::saveDisplayName,
        onSaveAvatar = appViewModel::saveAvatar,
        onOpenHistory = onOpenHistory,
        onSignOut = {
            appViewModel.signOut()
            // После выхода имя сброшено — пользователь снова проходит онбординг,
            // а весь прежний стек убирается целиком.
            navController.navigate(Routes.ONBOARDING) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    )
}

/**
 * Превращает сообщение ViewModel в готовую строку интерфейса.
 * ViewModel не знает про Context, поэтому перевод происходит здесь.
 */
private fun MessageRes.resolve(context: android.content.Context): String =
    if (args.isEmpty()) {
        context.getString(id)
    } else {
        context.getString(id, *args.toTypedArray())
    }
