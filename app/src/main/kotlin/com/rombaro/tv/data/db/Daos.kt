package com.rombaro.tv.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(p: PlaylistEntity): Long

    @Query("SELECT * FROM playlists ORDER BY id ASC")
    fun observeAll(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun get(id: Long): PlaylistEntity?

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface ChannelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ChannelEntity>)

    @Query("DELETE FROM channels WHERE playlistId = :playlistId")
    suspend fun clearForPlaylist(playlistId: Long)

    @Transaction
    suspend fun replaceAll(playlistId: Long, items: List<ChannelEntity>) {
        clearForPlaylist(playlistId)
        insertAll(items)
    }

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId ORDER BY orderHint, name")
    fun observeByPlaylist(playlistId: Long): Flow<List<ChannelEntity>>

    @Query("""
        SELECT * FROM channels
        WHERE playlistId = :playlistId
          AND (:category IS NULL OR category = :category)
        ORDER BY orderHint, name
    """)
    fun observeByCategory(playlistId: Long, category: String?): Flow<List<ChannelEntity>>

    @Query("SELECT DISTINCT category FROM channels WHERE playlistId = :playlistId AND category IS NOT NULL ORDER BY category")
    fun observeCategories(playlistId: Long): Flow<List<String>>

    @Query("SELECT * FROM channels WHERE id = :id")
    suspend fun get(id: Long): ChannelEntity?
}

@Dao
interface ProgrammeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ProgrammeEntity>)

    @Query("DELETE FROM programmes WHERE endMs < :before")
    suspend fun pruneBefore(before: Long)

    @Query("""
        SELECT * FROM programmes
        WHERE epgChannelId = :epgId
          AND endMs >= :nowMs
        ORDER BY startMs ASC
        LIMIT :limit
    """)
    suspend fun upcoming(epgId: String, nowMs: Long, limit: Int = 5): List<ProgrammeEntity>

    @Query("""
        SELECT * FROM programmes
        WHERE epgChannelId = :epgId
          AND startMs <= :nowMs
          AND endMs > :nowMs
        LIMIT 1
    """)
    suspend fun nowPlaying(epgId: String, nowMs: Long): ProgrammeEntity?

    @Query("""
        SELECT * FROM programmes
        WHERE epgChannelId IN (:epgIds)
          AND endMs >= :nowMs
        ORDER BY startMs ASC
    """)
    fun observeUpcomingForChannels(epgIds: List<String>, nowMs: Long): Flow<List<ProgrammeEntity>>

    @Transaction
    suspend fun replaceProgrammesForChannelsAndWindow(
        epgChannelIds: List<String>,
        startMsFrom: Long,
        startMsTo: Long,
        newProgrammes: List<ProgrammeEntity>
    ) {
        // Delete existing programmes for the specified channels and time window
        deleteProgrammesForChannelsAndWindow(epgChannelIds, startMsFrom, startMsTo)
        // Insert new programmes
        insertAll(newProgrammes)
    }

    @Query("""
        DELETE FROM programmes
        WHERE epgChannelId IN (:epgChannelIds)
          AND startMs >= :startMsFrom
          AND startMs < :startMsTo
    """)
    suspend fun deleteProgrammesForChannelsAndWindow(
        epgChannelIds: List<String>,
        startMsFrom: Long,
        startMsTo: Long
    )
}

@Dao
interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(fav: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE playlistId = :p AND streamId = :s")
    suspend fun remove(p: Long, s: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE playlistId = :p AND streamId = :s)")
    suspend fun isFavorite(p: Long, s: String): Boolean

    @Query("SELECT * FROM favorites WHERE playlistId = :p")
    fun observe(p: Long): Flow<List<FavoriteEntity>>
}
