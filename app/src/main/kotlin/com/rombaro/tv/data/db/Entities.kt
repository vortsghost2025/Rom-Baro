package com.rombaro.tv.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // "XTREAM" or "M3U"
    val serverUrl: String,
    val username: String?,
    val password: String?,
    val m3uUrl: String?,
    val xmltvUrl: String?,
    val userAgent: String?,
)

@Entity(
    tableName = "channels",
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["epgChannelId"]),
        Index(value = ["playlistId", "streamId"], unique = true),
    ],
)
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val streamId: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String?,
    val category: String?,
    val epgChannelId: String?,
    val orderHint: Int,
)

@Entity(
    tableName = "programmes",
    indices = [
        Index(value = ["epgChannelId", "startMs"]),
        Index(value = ["endMs"]),
    ],
)
data class ProgrammeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epgChannelId: String,
    val startMs: Long,
    val endMs: Long,
    val title: String,
    val description: String?,
)

@Entity(tableName = "favorites", primaryKeys = ["playlistId", "streamId"])
data class FavoriteEntity(
    val playlistId: Long,
    val streamId: String,
)
