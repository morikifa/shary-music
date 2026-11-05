package com.example.sharedmusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sharedmusic.R
import com.example.sharedmusic.SharyApplication
import com.example.sharedmusic.core.SharyConfig
import com.example.sharedmusic.data.local.NetworkStatus
import com.example.sharedmusic.data.model.ChatMessage
import com.example.sharedmusic.data.model.RoomMember
import com.example.sharedmusic.data.model.RoomRole
import com.example.sharedmusic.data.model.RoomTrack
import com.example.sharedmusic.data.model.Track
import com.example.sharedmusic.data.remote.FirebaseGate
import com.example.sharedmusic.data.remote.RoomRemoteSource
import com.example.sharedmusic.domain.ChatRules
import com.example.sharedmusic.domain.ChatValidation
import com.example.sharedmusic.domain.RoomCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Логика экранов комнаты: создание, вход, участники, чат, треки.
 *
 * UI не знает про Firebase: он получает состояние и вызывает события.
 */
class RoomViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SharyApplication
    private val session = app.sharySession
    private val remote = app.roomRemoteSource

    val roomCode: StateFlow<String?> = session.roomCode
    val role: StateFlow<RoomRole> = session.role
    val members: StateFlow<List<RoomMember>> = session.members
    val hostOnlyControl: StateFlow<Boolean> = session.hostOnlyControl
    val isLoading: StateFlow<Boolean> = session.isLoading

    val roomTracks: StateFlow<List<RoomTrack>> = remote.tracks
    val messages: StateFlow<List<ChatMessage>> = remote.messages
    val roomClosed: StateFlow<Boolean> = remote.roomClosed

    val libraryTracks: StateFlow<List<Track>> = app.trackStore
        .observeTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _chatInput = MutableStateFlow("")
    val chatInput: StateFlow<String> = _chatInput.asStateFlow()

    private val _chatError = MutableStateFlow<MessageRes?>(null)
    val chatError: StateFlow<MessageRes?> = _chatError.asStateFlow()

    private val _notice = MutableStateFlow<MessageRes?>(null)
    val notice: StateFlow<MessageRes?> = _notice.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _transferProgress = MutableStateFlow(0)
    val transferProgress: StateFlow<Int> = _transferProgress.asStateFlow()

    private val _isTransferring = MutableStateFlow(false)
    val isTransferring: StateFlow<Boolean> = _isTransferring.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    /** Плейлист комнаты с отметкой, есть ли файл на устройстве */
    val roomTracksWithAvailability: StateFlow<List<RoomTrackUi>> = combine(
        remote.tracks,
        libraryTracks,
        _searchQuery
    ) { roomList, localList, query ->
        val localById = localList.associateBy { it.id }
        val lower = query.trim().lowercase()
        val myUid = FirebaseGate.currentUid

        roomList
            .filter { roomTrack ->
                if (lower.isEmpty()) return@filter true
                roomTrack.title.lowercase().contains(lower) ||
                        (localById[roomTrack.trackId]?.artist?.lowercase()?.contains(lower) == true)
            }
            .map { roomTrack ->
                RoomTrackUi(
                    track = roomTrack,
                    isAvailableLocally = localById.containsKey(roomTrack.trackId),
                    addedByMe = roomTrack.addedByUid == myUid
                )
            }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    /** Сколько треков комнаты отсутствует на устройстве */
    val missingCount: StateFlow<Int> = combine(
        remote.tracks,
        libraryTracks
    ) { roomList, localList ->
        val localIds = localList.map { it.id }.toSet()
        roomList.count { it.trackId !in localIds }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val currentUid: String?
        get() = FirebaseGate.currentUid

    /** Создаёт комнату */
    fun createRoom(capacity: Int, password: String) {
        viewModelScope.launch {
            val auth = FirebaseGate.ensureSignedIn()

            if (auth !is FirebaseGate.AuthResult.Ready) {
                _notice.value = MessageRes(R.string.error_auth)
                return@launch
            }

            val name = resolveDisplayName()

            when (
                val result = session.createRoom(
                    uid = auth.uid,
                    displayName = name,
                    capacity = capacity.coerceIn(
                        SharyConfig.MIN_ROOM_CAPACITY,
                        SharyConfig.MAX_ROOM_CAPACITY
                    ),
                    password = password
                )
            ) {
                is RoomRemoteSource.RoomResult.Success ->
                    _notice.value = MessageRes(R.string.room_created, listOf(result.code))
                is RoomRemoteSource.RoomResult.Failure ->
                    _notice.value = messageFor(result.kind)
            }
        }
    }

    /** Входит в комнату по коду и паролю */
    fun joinRoom(code: String, password: String) {
        val normalized = RoomCode.normalize(code)

        if (!RoomCode.isValid(normalized)) {
            _notice.value = MessageRes(R.string.room_code_invalid)
            return
        }

        viewModelScope.launch {
            val auth = FirebaseGate.ensureSignedIn()

            if (auth !is FirebaseGate.AuthResult.Ready) {
                _notice.value = MessageRes(R.string.error_auth)
                return@launch
            }

            val name = resolveDisplayName()

            when (val result = session.joinRoom(normalized, auth.uid, name, password)) {
                is RoomRemoteSource.RoomResult.Success -> _notice.value = null
                is RoomRemoteSource.RoomResult.Failure ->
                    _notice.value = messageFor(result.kind)
            }
        }
    }

    /** Выходит из комнаты; хост при выходе закрывает комнату */
    fun leaveRoom() {
        viewModelScope.launch {
            val wasHost = session.isHost
            session.leaveRoom(closeRoom = wasHost)

            if (wasHost) {
                _notice.value = MessageRes(R.string.room_left_notice)
            }
        }
    }

    /** Удаляет комнату по требованию хоста */
    fun closeRoom() {
        val code = roomCode.value ?: return

        viewModelScope.launch {
            runCatching { remote.closeRoom(code) }
            session.onRoomClosed()
            _notice.value = MessageRes(R.string.room_deleted_notice)
        }
    }

    /** Меняет пароль комнаты */
    fun changePassword(password: String) {
        viewModelScope.launch {
            val changed = session.changePassword(password)

            _notice.value = if (changed) {
                MessageRes(R.string.room_password_changed)
            } else {
                MessageRes(R.string.room_password_change_failed)
            }
        }
    }

    /** Исключает участника из комнаты */
    fun kickMember(uid: String) {
        if (!session.isHost) {
            _notice.value = MessageRes(R.string.room_member_no_rights)
            return
        }

        session.removeMember(uid)
        _notice.value = MessageRes(R.string.room_member_removed)
    }

    /** Управление музыкой только хостом */
    fun setHostOnlyControl(hostOnly: Boolean) {
        session.setHostOnlyControl(hostOnly)
    }

    fun updateChatInput(text: String) {
        _chatInput.value = text.take(SharyConfig.MAX_CHAT_LENGTH)
        _chatError.value = null
    }

    /** Отправляет сообщение чата после проверки правил */
    fun sendMessage() {
        if (_isSending.value) return

        when (val validation = ChatRules.validate(_chatInput.value)) {
            is ChatValidation.Empty -> {
                _chatError.value = MessageRes(R.string.chat_empty_message)
                return
            }
            is ChatValidation.TooLong -> {
                _chatError.value = MessageRes(R.string.chat_too_long, listOf(validation.maxLength))
                return
            }
            is ChatValidation.Valid -> {
                val uid = FirebaseGate.currentUid

                if (uid == null) {
                    _chatError.value = MessageRes(R.string.chat_offline)
                    return
                }

                _isSending.value = true

                viewModelScope.launch {
                    val name = resolveDisplayName()
                    val id = remote.sendMessage(validation.text, uid, name)

                    _isSending.value = false

                    if (id == null) {
                        _chatError.value = MessageRes(R.string.chat_error)
                    } else {
                        _chatInput.value = ""
                    }
                }
            }
        }
    }

    /** Отправляет локальный трек участникам комнаты */
    fun shareTrack(track: Track) {
        val code = roomCode.value ?: return

        if (!session.isHost && hostOnlyControl.value) {
            _notice.value = MessageRes(R.string.room_share_only_host)
            return
        }

        viewModelScope.launch {
            val uid = FirebaseGate.currentUid ?: return@launch

            _isTransferring.value = true
            _transferProgress.value = 0

            val source = File(track.filePath)

            val shared = withContext(Dispatchers.IO) {
                remote.shareTrack(
                    file = source,
                    trackId = track.id,
                    title = track.title,
                    artist = track.artist,
                    addedByUid = uid,
                    onProgress = { percent -> _transferProgress.value = percent }
                )
            }

            _isTransferring.value = false

            _notice.value = if (shared) {
                MessageRes(R.string.room_track_sent, listOf(code))
            } else {
                MessageRes(R.string.room_track_send_failed)
            }
        }
    }

    /** Скачивает трек комнаты, которого нет на устройстве */
    fun downloadTrack(roomTrack: RoomTrack) {
        if (_isTransferring.value) return

        viewModelScope.launch {
            // Настройка «только Wi-Fi»: в мобильной сети загрузка не начинается
            val wifiOnly = app.settingsStore.settings.first().downloadOverWifiOnly
            if (wifiOnly && !NetworkStatus.isUnmetered(app)) {
                _notice.value = MessageRes(R.string.library_wifi_required)
                return@launch
            }

            _isTransferring.value = true
            _transferProgress.value = 0

            val destination = File(app.cacheDir, "room_${roomTrack.trackId}.audio")

            val downloaded = remote.downloadTrack(roomTrack, destination) { percent ->
                _transferProgress.value = percent
            }

            if (!downloaded) {
                _isTransferring.value = false
                _notice.value = MessageRes(R.string.room_track_download_failed)
                return@launch
            }

            val imported = app.trackImporter.importDownloadedFile(
                file = destination,
                trackId = roomTrack.trackId,
                title = roomTrack.title,
                artist = roomTrack.artist,
                durationMs = 0L
            )

            destination.delete()
            _isTransferring.value = false

            _notice.value = if (imported != null) {
                MessageRes(R.string.track_added_to_library)
            } else {
                MessageRes(R.string.track_add_failed)
            }
        }
    }

    fun updateSearch(query: String) {
        _searchQuery.value = query.take(SharyConfig.MAX_TITLE_LENGTH)
    }

    fun openSearch() {
        _isSearchActive.value = true
    }

    fun closeSearch() {
        _isSearchActive.value = false
        _searchQuery.value = ""
    }

    fun clearNotice() {
        _notice.value = null
    }

    fun clearChatError() {
        _chatError.value = null
    }

    /** Имя пользователя из профиля, иначе нейтральная подпись */
    private suspend fun resolveDisplayName(): String {
        val profile = app.profileStore.profile.first()

        return profile.displayName.ifBlank { app.getString(R.string.room_participant_default) }
    }

    private fun messageFor(kind: RoomRemoteSource.FailureKind): MessageRes = when (kind) {
        RoomRemoteSource.FailureKind.NOT_FOUND -> MessageRes(R.string.room_not_found)
        RoomRemoteSource.FailureKind.WRONG_PASSWORD -> MessageRes(R.string.room_wrong_password)
        RoomRemoteSource.FailureKind.FULL -> MessageRes(R.string.room_full)
        RoomRemoteSource.FailureKind.CLOSED -> MessageRes(R.string.room_closed)
        RoomRemoteSource.FailureKind.NO_CONNECTION -> MessageRes(R.string.room_no_connection)
        RoomRemoteSource.FailureKind.UNKNOWN -> MessageRes(R.string.state_error_generic)
    }
}

/** Плейлист комнаты глазами пользователя: есть ли файл локально */
data class RoomTrackUi(
    val track: RoomTrack,
    val isAvailableLocally: Boolean,
    val addedByMe: Boolean
)
