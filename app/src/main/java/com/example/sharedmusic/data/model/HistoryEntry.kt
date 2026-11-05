package com.example.sharedmusic.data.model

/** Режим прослушивания: локально или в комнате */
sealed interface ListeningMode {
    data object Local : ListeningMode
    data class Room(val code: String) : ListeningMode
}

/** Запись истории прослушивания */
data class HistoryEntry(
    val id: Long = 0L,
    val trackId: String,
    val trackTitle: String,
    val trackArtist: String,
    val startedAtMs: Long,
    val listenedMs: Long,
    val roomCode: String? = null
)
