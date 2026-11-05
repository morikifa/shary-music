package com.example.sharedmusic.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Сущность локального трека в базе данных */
@androidx.room.Entity(tableName = "tracks")
data class TrackEntity(
    @androidx.room.PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val fileName: String,
    val filePath: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val artworkPath: String?,
    val isLiked: Boolean,
    val addedAt: Long,
    val isWeakId: Boolean
)

/** Сущность записи истории прослушивания */
@androidx.room.Entity(tableName = "history")
data class HistoryEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val trackId: String,
    val trackTitle: String,
    val trackArtist: String,
    val startedAtMs: Long,
    val listenedMs: Long,
    val roomCode: String?
)

@Dao
interface TrackDao {

    @Query("SELECT * FROM tracks ORDER BY addedAt ASC")
    fun observeAll(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY addedAt ASC")
    suspend fun getAll(): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): TrackEntity?

    @Query("SELECT * FROM tracks WHERE isLiked = 1 ORDER BY addedAt DESC")
    fun observeLiked(): Flow<List<TrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(track: TrackEntity)

    @Update
    suspend fun update(track: TrackEntity)

    @Query("UPDATE tracks SET isLiked = :liked WHERE id = :id")
    suspend fun setLiked(id: String, liked: Boolean)

    @Query("UPDATE tracks SET artworkPath = :path WHERE id = :id")
    suspend fun setArtwork(id: String, path: String?)

    @Delete
    suspend fun delete(track: TrackEntity)

    @Query("DELETE FROM tracks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM tracks")
    suspend fun count(): Int
}

@Dao
interface HistoryDao {

    @Query("SELECT * FROM history ORDER BY startedAtMs DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Insert
    suspend fun insert(entry: HistoryEntity): Long

    @Query("DELETE FROM history")
    suspend fun clear()

    @Query("SELECT COALESCE(SUM(listenedMs), 0) FROM history WHERE roomCode IS NOT NULL")
    suspend fun totalRoomListenMs(): Long
}

@Database(
    entities = [TrackEntity::class, HistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SharyDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun historyDao(): HistoryDao

    companion object {
        const val NAME = "shary.db"
    }
}
