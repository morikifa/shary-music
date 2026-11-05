package com.example.sharedmusic.data.local

import com.example.sharedmusic.data.model.HistoryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Локальная история прослушивания */
class HistoryStore(private val database: SharyDatabase) {

    fun observeHistory(): Flow<List<HistoryEntry>> =
        database.historyDao().observeAll().map { entities ->
            entities.map { entity ->
                HistoryEntry(
                    id = entity.id,
                    trackId = entity.trackId,
                    trackTitle = entity.trackTitle,
                    trackArtist = entity.trackArtist,
                    startedAtMs = entity.startedAtMs,
                    listenedMs = entity.listenedMs,
                    roomCode = entity.roomCode
                )
            }
        }

    suspend fun add(entry: HistoryEntry) {
        database.historyDao().insert(
            HistoryEntity(
                trackId = entry.trackId,
                trackTitle = entry.trackTitle,
                trackArtist = entry.trackArtist,
                startedAtMs = entry.startedAtMs,
                listenedMs = entry.listenedMs,
                roomCode = entry.roomCode
            )
        )
    }

    suspend fun clear() {
        database.historyDao().clear()
    }

    suspend fun totalRoomListenMs(): Long =
        database.historyDao().totalRoomListenMs()
}
