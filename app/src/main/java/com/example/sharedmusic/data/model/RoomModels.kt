package com.example.sharedmusic.data.model

/** Роль участника комнаты */
enum class RoomRole {
    HOST,
    MEMBER;

    companion object {
        fun fromName(value: String?): RoomRole =
            if (value == HOST.name) HOST else MEMBER
    }
}

/** Участник комнаты */
data class RoomMember(
    val uid: String,
    /** Пустая строка допустима: подпись «Участник» подставляет UI из ресурсов */
    val displayName: String,
    val avatarPath: String? = null,
    val role: RoomRole = RoomRole.MEMBER,
    val isOnline: Boolean = true
) {
    val initial: String
        get() = displayName.trim().take(1).uppercase().ifBlank { "?" }
}

/** Трек в плейлисте комнаты */
data class RoomTrack(
    val trackId: String,
    val title: String,
    val artist: String,
    val addedByUid: String,
    val downloadUrl: String = "",
    val fileName: String = "",
    val order: Int = 0
)

/** Состояние воспроизведения комнаты, авторитетное на сервере */
data class RoomPlayback(
    val trackId: String = "",
    val title: String = "",
    val positionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val updatedAtServerMs: Long = 0L,
    val updatedBy: String = "",
    val revision: String = ""
)

/** Сообщение чата комнаты */
data class ChatMessage(
    val id: String,
    val senderUid: String,
    val senderName: String,
    val text: String,
    val sentAtServerMs: Long = 0L,
    /** Локальное время получения — нужно, пока серверная метка не пришла */
    val localSentAtMs: Long = System.currentTimeMillis(),
    val isPending: Boolean = false
) {
    /** Время для подписи: серверное, если известно, иначе локальное */
    val timeMs: Long
        get() = if (sentAtServerMs > 0L) sentAtServerMs else localSentAtMs
}

/** Статус синхронизации гостя с хостом */
sealed interface RoomSyncStatus {
    data object Synced : RoomSyncStatus
    data object CatchingUp : RoomSyncStatus
    data object TrackMissing : RoomSyncStatus
    data class Offline(val reason: String) : RoomSyncStatus
}
