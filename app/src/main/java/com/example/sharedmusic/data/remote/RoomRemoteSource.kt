package com.example.sharedmusic.data.remote

import android.net.Uri
import com.example.sharedmusic.data.model.ChatMessage
import com.example.sharedmusic.data.model.RoomMember
import com.example.sharedmusic.data.model.RoomPlayback
import com.example.sharedmusic.data.model.RoomRole
import com.example.sharedmusic.data.model.RoomTrack
import com.example.sharedmusic.domain.RoomCode
import com.example.sharedmusic.domain.TrackIdentity
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Комнаты на Realtime Database.
 *
 * Структура данных:
 * rooms/<CODE>/info      — хост, вместимость, пароль
 * rooms/<CODE>/members   — участники и присутствие через onDisconnect
 * rooms/<CODE>/tracks    — плейлист комнаты и ссылки на файлы
 * rooms/<CODE>/playback  — авторитетное состояние воспроизведения
 * rooms/<CODE>/messages  — чат
 *
 * Аудиофайл отправляется в Firebase Storage: участник без нужного файла
 * скачивает его по кнопке «Скачать». Ссылка удаляется владельцем файла
 * после подтверждения загрузки всеми участниками.
 */
class RoomRemoteSource(
    private val databaseUrl: String,
    private val scope: CoroutineScope
) {

    /** Результат операции комнаты */
    sealed interface RoomResult {
        data class Success(val code: String) : RoomResult
        data class Failure(val kind: FailureKind, val reason: String = "") : RoomResult
    }

    enum class FailureKind {
        NOT_FOUND,
        WRONG_PASSWORD,
        FULL,
        CLOSED,
        NO_CONNECTION,
        UNKNOWN
    }

    private val database: FirebaseDatabase
        get() = FirebaseDatabase.getInstance(databaseUrl)

    private val storage: FirebaseStorage?
        get() = runCatching { FirebaseStorage.getInstance() }.getOrNull()

    private val _members = MutableStateFlow<List<RoomMember>>(emptyList())
    val members: StateFlow<List<RoomMember>> = _members.asStateFlow()

    private val _tracks = MutableStateFlow<List<RoomTrack>>(emptyList())
    val tracks: StateFlow<List<RoomTrack>> = _tracks.asStateFlow()

    private val _playback = MutableStateFlow(RoomPlayback())
    val playback: StateFlow<RoomPlayback> = _playback.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _roomClosed = MutableStateFlow(false)
    val roomClosed: StateFlow<Boolean> = _roomClosed.asStateFlow()

    /** Роль текущего пользователя в комнате, определённая сервером */
    private val _myRole = MutableStateFlow(RoomRole.MEMBER)
    val myRole: StateFlow<RoomRole> = _myRole.asStateFlow()

    val currentCode: String?
        get() = activeCode

    private var activeCode: String? = null
    private var activeUid: String? = null

    /** Смещение серверного времени в миллисекундах: serverNow = localNow + offset */
    private var serverOffsetMs = 0L

    private var roomReference: DatabaseReference? = null
    private var membersListener: ValueEventListener? = null
    private var tracksListener: ValueEventListener? = null
    private var playbackListener: ValueEventListener? = null
    private var infoListener: ValueEventListener? = null
    private var messagesListener: ChildEventListener? = null
    private var offsetListener: ValueEventListener? = null

    private val messagesBuffer = mutableListOf<ChatMessage>()

    /** Текущее серверное время с учётом смещения часов устройства */
    fun serverNowMs(): Long = System.currentTimeMillis() + serverOffsetMs

    init {
        offsetListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                serverOffsetMs = (snapshot.value as? Number)?.toLong() ?: 0L
            }

            override fun onCancelled(error: DatabaseError) = Unit
        }

        runCatching {
            database.getReference(".info/serverTimeOffset")
                .addValueEventListener(offsetListener!!)
        }
    }

    // ---------- Создание и вход ----------

    /** Создаёт комнату. Код генерируется заново, если такой уже занят */
    suspend fun createRoom(
        uid: String,
        displayName: String,
        capacity: Int,
        password: String,
        attempts: Int = 8
    ): RoomResult = withContext(Dispatchers.IO) {
        repeat(attempts) {
            val code = RoomCode.generate()
            val reference = database.getReference("rooms").child(code)

            val exists = runCatching { reference.get().await().exists() }.getOrDefault(true)
            if (exists) return@repeat

            val info = mutableMapOf<String, Any>(
                "hostUid" to uid,
                "createdAt" to ServerValue.TIMESTAMP,
                "capacity" to capacity,
                "isClosed" to false
            )

            val hash = passwordHash(code, password)
            if (hash.isNotBlank()) info["passwordHash"] = hash

            val written = runCatching { reference.child("info").setValue(info).await() }.isSuccess
            if (!written) {
                return@withContext RoomResult.Failure(FailureKind.NO_CONNECTION)
            }

            return@withContext joinInternal(
                code = code,
                uid = uid,
                displayName = displayName,
                passwordHash = hash,
                role = RoomRole.HOST
            )
        }

        RoomResult.Failure(FailureKind.UNKNOWN, "Не удалось подобрать свободный код")
    }

    /** Входит в комнату по коду и паролю */
    suspend fun joinRoom(
        code: String,
        uid: String,
        displayName: String,
        password: String
    ): RoomResult = withContext(Dispatchers.IO) {
        joinInternal(
            code = code,
            uid = uid,
            displayName = displayName,
            passwordHash = passwordHash(code, password),
            role = RoomRole.MEMBER
        )
    }

    private suspend fun joinInternal(
        code: String,
        uid: String,
        displayName: String,
        passwordHash: String,
        role: RoomRole
    ): RoomResult {
        val reference = database.getReference("rooms").child(code)

        val snapshot = runCatching { reference.get().await() }.getOrElse {
            return RoomResult.Failure(FailureKind.NO_CONNECTION)
        }

        if (!snapshot.exists()) return RoomResult.Failure(FailureKind.NOT_FOUND)

        val info = snapshot.child("info")

        if (info.child("isClosed").getValue(Boolean::class.java) == true) {
            return RoomResult.Failure(FailureKind.CLOSED)
        }

        val storedHash = info.child("passwordHash").getValue(String::class.java).orEmpty()
        if (storedHash.isNotBlank() && storedHash != passwordHash) {
            return RoomResult.Failure(FailureKind.WRONG_PASSWORD)
        }

        val capacity = (info.child("capacity").value as? Number)?.toInt() ?: 5
        val membersCount = snapshot.child("members").childrenCount.toInt()
        val alreadyMember = snapshot.child("members").child(uid).exists()

        if (!alreadyMember && membersCount >= capacity) {
            return RoomResult.Failure(FailureKind.FULL)
        }

        // Роль определяется по info/hostUid: если хост перезашёл, права возвращаются
        val hostUid = info.child("hostUid").getValue(String::class.java)
        val actualRole = if (hostUid == uid) RoomRole.HOST else role

        val memberReference = reference.child("members").child(uid)

        val payload = mapOf(
            "name" to displayName,
            "role" to actualRole.name,
            "joinedAt" to ServerValue.TIMESTAMP
        )

        val written = runCatching {
            memberReference.onDisconnect().removeValue().await()
            memberReference.setValue(payload).await()
        }.isSuccess

        if (!written) return RoomResult.Failure(FailureKind.NO_CONNECTION)

        activeCode = code
        activeUid = uid
        _myRole.value = actualRole

        subscribe(code)

        return RoomResult.Success(code)
    }

    /** Выход из комнаты. При transferHost комната закрывается */
    suspend fun leaveRoom(closeIfHost: Boolean = false) {
        val code = activeCode
        val uid = activeUid

        unsubscribe()

        if (code != null && uid != null) {
            val memberReference = database.getReference("rooms")
                .child(code)
                .child("members")
                .child(uid)

            runCatching { memberReference.onDisconnect().cancel().await() }
            runCatching { memberReference.removeValue().await() }
        }

        if (closeIfHost && code != null) {
            closeRoom(code)
        }

        activeCode = null
        activeUid = null
        _myRole.value = RoomRole.MEMBER
        _members.value = emptyList()
        _tracks.value = emptyList()
        _messages.value = emptyList()
        _playback.value = RoomPlayback()
        messagesBuffer.clear()
    }

    /** Закрывает комнату: данные удаляются, участники получают сигнал закрытия */
    suspend fun closeRoom(code: String) {
        if (code.isBlank()) return

        runCatching {
            database.getReference("rooms").child(code).child("info")
                .child("isClosed").setValue(true).await()
        }

        runCatching {
            database.getReference("rooms").child(code).removeValue().await()
        }
    }

    /** Меняет пароль комнаты */
    suspend fun changePassword(code: String, newPassword: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                database.getReference("rooms").child(code).child("info")
                    .child("passwordHash")
                    .setValue(passwordHash(code, newPassword))
                    .await()
                true
            }.getOrDefault(false)
        }

    /** Исключает участника из комнаты */
    suspend fun removeMember(code: String, uid: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                database.getReference("rooms").child(code)
                    .child("members").child(uid)
                    .removeValue().await()
                true
            }.getOrDefault(false)
        }

    /** Включает или отключает запрет на управление музыкой для обычных участников */
    suspend fun setHostOnlyControl(code: String, hostOnly: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                database.getReference("rooms").child(code).child("info")
                    .child("hostOnlyControl")
                    .setValue(hostOnly)
                    .await()
                true
            }.getOrDefault(false)
        }

    /** Читает, разрешено ли управление только хосту */
    suspend fun isHostOnlyControl(code: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                database.getReference("rooms").child(code).child("info")
                    .child("hostOnlyControl")
                    .get()
                    .await()
                    .getValue(Boolean::class.java) ?: false
            }.getOrDefault(false)
        }

    // ---------- Воспроизведение ----------

    /** Публикует состояние воспроизведения; вызывается только хостом */
    suspend fun publishPlayback(
        trackId: String,
        title: String,
        positionMs: Long,
        isPlaying: Boolean,
        uid: String
    ): Boolean = withContext(Dispatchers.IO) {
        val code = activeCode ?: return@withContext false

        runCatching {
            database.getReference("rooms").child(code).child("playback").setValue(
                mapOf(
                    "trackId" to trackId,
                    "title" to title,
                    "positionMs" to positionMs.coerceAtLeast(0L),
                    "isPlaying" to isPlaying,
                    "updatedAt" to ServerValue.TIMESTAMP,
                    "updatedBy" to uid,
                    "revision" to UUID.randomUUID().toString()
                )
            ).await()
            true
        }.getOrDefault(false)
    }

    // ---------- Чат ----------

    /** Отправляет сообщение чата; возвращает id или null при ошибке */
    suspend fun sendMessage(
        text: String,
        uid: String,
        displayName: String
    ): String? = withContext(Dispatchers.IO) {
        val code = activeCode ?: return@withContext null

        val messageReference = database.getReference("rooms")
            .child(code)
            .child("messages")
            .push()

        val id = messageReference.key ?: return@withContext null

        runCatching {
            messageReference.setValue(
                mapOf(
                    "senderUid" to uid,
                    "senderName" to displayName,
                    "text" to text,
                    "sentAt" to ServerValue.TIMESTAMP
                )
            ).await()
            id
        }.getOrNull()
    }

    // ---------- Треки комнаты ----------

    /**
     * Отправляет трек в комнату: файл — в Firebase Storage,
     * ссылка и метаданные — в Realtime Database.
     */
    suspend fun shareTrack(
        file: File,
        trackId: String,
        title: String,
        artist: String,
        addedByUid: String,
        onProgress: (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val code = activeCode ?: return@withContext false
        val storageRef = storage ?: return@withContext false

        try {
            val objectName = "rooms/$code/tracks/${TrackIdentity.sha256Hex(trackId).take(24)}_${file.name}"
            val reference = storageRef.reference.child(objectName)

            val task = reference.putFile(Uri.fromFile(file))

            task.addOnProgressListener { snapshot ->
                val total = snapshot.totalByteCount
                if (total > 0L) {
                    onProgress((snapshot.bytesTransferred * 100 / total).toInt())
                }
            }

            task.await()

            val downloadUrl = reference.downloadUrl.await().toString()

            database.getReference("rooms")
                .child(code)
                .child("tracks")
                .child(trackId)
                .setValue(
                    mapOf(
                        "title" to title,
                        "artist" to artist,
                        "addedBy" to addedByUid,
                        "downloadUrl" to downloadUrl,
                        "storagePath" to objectName,
                        "fileName" to file.name,
                        "addedAt" to ServerValue.TIMESTAMP,
                        "order" to _tracks.value.size
                    )
                ).await()

            true
        } catch (_: Exception) {
            false
        }
    }

    /** Скачивает файл трека комнаты во временный файл */
    suspend fun downloadTrack(
        track: RoomTrack,
        destination: File,
        onProgress: (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val storageRef = storage ?: return@withContext false

        val url = track.downloadUrl.takeIf { it.isNotBlank() } ?: return@withContext false

        try {
            val reference = storageRef.getReferenceFromUrl(url)
            val task = reference.getFile(destination)

            task.addOnProgressListener { snapshot ->
                val total = snapshot.totalByteCount
                if (total > 0L) {
                    onProgress((snapshot.bytesTransferred * 100 / total).toInt())
                }
            }

            task.await()
            true
        } catch (_: Exception) {
            false
        }
    }

    // ---------- Подписки ----------

    private fun subscribe(code: String) {
        unsubscribe()

        val reference = database.getReference("rooms").child(code)
        roomReference = reference
        _roomClosed.value = false

        infoListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.child("isClosed").getValue(Boolean::class.java) == true) {
                    _roomClosed.value = true
                }
            }

            override fun onCancelled(error: DatabaseError) = Unit
        }

        membersListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    val uid = child.key ?: return@mapNotNull null
                    RoomMember(
                        uid = uid,
                        displayName = child.child("name").getValue(String::class.java)
                            ?: "Участник",
                        role = RoomRole.fromName(
                            child.child("role").getValue(String::class.java)
                        )
                    )
                }.sortedBy { if (it.role == RoomRole.HOST) 0 else 1 }

                _members.value = list
            }

            override fun onCancelled(error: DatabaseError) = Unit
        }

        tracksListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    val trackId = child.key ?: return@mapNotNull null
                    RoomTrack(
                        trackId = trackId,
                        title = child.child("title").getValue(String::class.java) ?: "Трек",
                        artist = child.child("artist").getValue(String::class.java).orEmpty(),
                        addedByUid = child.child("addedBy").getValue(String::class.java).orEmpty(),
                        downloadUrl = child.child("downloadUrl")
                            .getValue(String::class.java).orEmpty(),
                        fileName = child.child("fileName")
                            .getValue(String::class.java).orEmpty(),
                        order = (child.child("order").value as? Number)?.toInt() ?: 0
                    )
                }.sortedBy { it.order }

                _tracks.value = list
            }

            override fun onCancelled(error: DatabaseError) = Unit
        }

        playbackListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return

                _playback.value = RoomPlayback(
                    trackId = snapshot.child("trackId").getValue(String::class.java).orEmpty(),
                    title = snapshot.child("title").getValue(String::class.java).orEmpty(),
                    positionMs = (snapshot.child("positionMs").value as? Number)?.toLong() ?: 0L,
                    isPlaying = snapshot.child("isPlaying")
                        .getValue(Boolean::class.java) ?: false,
                    updatedAtServerMs = (snapshot.child("updatedAt").value as? Number)
                        ?.toLong() ?: 0L,
                    updatedBy = snapshot.child("updatedBy")
                        .getValue(String::class.java).orEmpty(),
                    revision = snapshot.child("revision").getValue(String::class.java).orEmpty()
                )
            }

            override fun onCancelled(error: DatabaseError) = Unit
        }

        messagesListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.toChatMessage() ?: return
                messagesBuffer.add(message)
                publishMessages()
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val updated = snapshot.toChatMessage() ?: return
                val index = messagesBuffer.indexOfFirst { it.id == updated.id }
                if (index >= 0) {
                    messagesBuffer[index] = updated
                    publishMessages()
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val id = snapshot.key ?: return
                messagesBuffer.removeAll { it.id == id }
                publishMessages()
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) = Unit

            override fun onCancelled(error: DatabaseError) = Unit
        }

        reference.child("info").addValueEventListener(infoListener!!)
        reference.child("members").addValueEventListener(membersListener!!)
        reference.child("tracks").addValueEventListener(tracksListener!!)
        reference.child("playback").addValueEventListener(playbackListener!!)
        reference.child("messages").addChildEventListener(messagesListener!!)
    }

    private fun publishMessages() {
        _messages.value = messagesBuffer.sortedBy { it.timeMs }
    }

    private fun unsubscribe() {
        val reference = roomReference

        if (reference != null) {
            infoListener?.let { reference.child("info").removeEventListener(it) }
            membersListener?.let { reference.child("members").removeEventListener(it) }
            tracksListener?.let { reference.child("tracks").removeEventListener(it) }
            playbackListener?.let { reference.child("playback").removeEventListener(it) }
            messagesListener?.let { reference.child("messages").removeEventListener(it) }
        }

        infoListener = null
        membersListener = null
        tracksListener = null
        playbackListener = null
        messagesListener = null
        roomReference = null
        messagesBuffer.clear()
    }

    /** Отписывается от всех слушателей */
    fun release() {
        unsubscribe()

        offsetListener?.let {
            runCatching {
                database.getReference(".info/serverTimeOffset").removeEventListener(it)
            }
        }
        offsetListener = null
    }

    private fun DataSnapshot.toChatMessage(): ChatMessage? {
        val senderUid = child("senderUid").getValue(String::class.java) ?: return null
        val text = child("text").getValue(String::class.java) ?: return null

        return ChatMessage(
            id = key ?: return null,
            senderUid = senderUid,
            senderName = child("senderName").getValue(String::class.java).orEmpty(),
            text = text,
            sentAtServerMs = (child("sentAt").value as? Number)?.toLong() ?: 0L
        )
    }

    /** Хеш пароля с солью-кодом комнаты, чтобы одинаковые пароли не совпадали */
    private fun passwordHash(code: String, password: String): String {
        if (password.isBlank()) return ""
        return TrackIdentity.sha256Hex("$code|${password.trim()}")
    }

    /** Обновляет присутствие после возврата в приложение */
    fun refreshPresence() {
        val code = activeCode ?: return
        val uid = activeUid ?: return

        scope.launch {
            runCatching {
                database.getReference("rooms").child(code)
                    .child("members").child(uid)
                    .child("name")
                    .get()
                    .await()
            }
        }
    }
}
