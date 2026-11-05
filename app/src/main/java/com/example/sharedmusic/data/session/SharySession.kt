package com.example.sharedmusic.data.session

import com.example.sharedmusic.data.model.RoomMember
import com.example.sharedmusic.data.model.RoomRole
import com.example.sharedmusic.data.remote.RoomRemoteSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Текущее состояние приложения, которое нужно сразу нескольким экранам:
 * активная комната, роль пользователя в ней и настройки прав.
 *
 * Хранится в одном месте, чтобы плеер и экран комнаты не расходились
 * в понимании того, «в комнате мы или нет».
 */
class SharySession(
    private val remote: RoomRemoteSource,
    private val scope: CoroutineScope
) {

    private val _roomCode = MutableStateFlow<String?>(null)
    val roomCode: StateFlow<String?> = _roomCode.asStateFlow()

    private val _role = MutableStateFlow(RoomRole.MEMBER)
    val role: StateFlow<RoomRole> = _role.asStateFlow()

    private val _hostOnlyControl = MutableStateFlow(false)
    val hostOnlyControl: StateFlow<Boolean> = _hostOnlyControl.asStateFlow()

    private val _members = MutableStateFlow<List<RoomMember>>(emptyList())
    val members: StateFlow<List<RoomMember>> = _members.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<RoomRemoteSource.FailureKind?>(null)
    val error: StateFlow<RoomRemoteSource.FailureKind?> = _error.asStateFlow()

    val isInRoom: Boolean
        get() = _roomCode.value != null

    val isHost: Boolean
        get() = _role.value == RoomRole.HOST

    /** Может ли текущий пользователь управлять воспроизведением */
    val canControl: Boolean
        get() = !isInRoom || isHost || !_hostOnlyControl.value

    /** Создаёт комнату и запоминает сессию */
    suspend fun createRoom(
        uid: String,
        displayName: String,
        capacity: Int,
        password: String
    ): RoomRemoteSource.RoomResult {
        _isLoading.value = true
        _error.value = null

        val result = remote.createRoom(
            uid = uid,
            displayName = displayName,
            capacity = capacity,
            password = password
        )

        _isLoading.value = false

        when (result) {
            is RoomRemoteSource.RoomResult.Success -> {
                _roomCode.value = result.code
                _role.value = remote.myRole.value
                observeMembers()
            }
            is RoomRemoteSource.RoomResult.Failure -> _error.value = result.kind
        }

        return result
    }

    /** Входит в комнату по коду */
    suspend fun joinRoom(
        code: String,
        uid: String,
        displayName: String,
        password: String
    ): RoomRemoteSource.RoomResult {
        _isLoading.value = true
        _error.value = null

        val result = remote.joinRoom(
            code = code,
            uid = uid,
            displayName = displayName,
            password = password
        )

        _isLoading.value = false

        when (result) {
            is RoomRemoteSource.RoomResult.Success -> {
                _roomCode.value = result.code
                _role.value = remote.myRole.value
                _hostOnlyControl.value = runCatching {
                    remote.isHostOnlyControl(result.code)
                }.getOrDefault(false)
                observeMembers()
            }
            is RoomRemoteSource.RoomResult.Failure -> _error.value = result.kind
        }

        return result
    }

    /** Выход из комнаты; хост при выходе закрывает комнату */
    suspend fun leaveRoom(closeRoom: Boolean = isHost) {
        val code = _roomCode.value

        runCatching { remote.leaveRoom(closeIfHost = closeRoom) }

        if (closeRoom && code != null) {
            runCatching { remote.closeRoom(code) }
        }

        _roomCode.value = null
        _role.value = RoomRole.MEMBER
        _hostOnlyControl.value = false
        _members.value = emptyList()
    }

    /** Сообщает о закрытии комнаты хостом */
    fun onRoomClosed() {
        _roomCode.value = null
        _role.value = RoomRole.MEMBER
        _members.value = emptyList()
    }

    fun clearError() {
        _error.value = null
    }

    /** Меняет режим прав в комнате */
    fun setHostOnlyControl(hostOnly: Boolean) {
        val code = _roomCode.value ?: return

        _hostOnlyControl.value = hostOnly

        scope.launch {
            remote.setHostOnlyControl(code, hostOnly)
        }
    }

    /** Исключает участника */
    fun removeMember(uid: String) {
        val code = _roomCode.value ?: return

        scope.launch {
            remote.removeMember(code, uid)
        }
    }

    /** Меняет пароль комнаты */
    suspend fun changePassword(password: String): Boolean {
        val code = _roomCode.value ?: return false

        return remote.changePassword(code, password)
    }

    private var membersJob: kotlinx.coroutines.Job? = null

    private fun observeMembers() {
        membersJob?.cancel()

        membersJob = scope.launch {
            remote.members.collect { list ->
                _members.value = list
            }
        }
    }

    /** Синхронизирует роль после переподключения */
    fun refreshRole(uid: String) {
        val member = _members.value.firstOrNull { it.uid == uid }

        if (member != null) {
            _role.value = member.role
        }
    }
}
