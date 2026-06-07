package com.rombaro.tv.domain

/** Playlist source the user added (Xtream creds or raw M3U URL). */
data class Playlist(
    val id: Long = 0,
    val name: String,
    val type: PlaylistType,
    val serverUrl: String,
    val username: String? = null,
    val password: String? = null,
    val m3uUrl: String? = null,
    val xmltvUrl: String? = null,
    val userAgent: String? = null,
)

enum class PlaylistType { XTREAM, M3U }

/** A single playable channel. */
data class Channel(
    val id: Long = 0,
    val playlistId: Long,
    val streamId: String,          // Xtream stream_id or generated from M3U
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val category: String? = null,
    val epgChannelId: String? = null, // maps to <channel id="..."> in XMLTV
    val orderHint: Int = 0,
)

/** A single EPG programme. Times are epoch millis (UTC). */
data class Programme(
    val id: Long = 0,
    val epgChannelId: String,
    val startMs: Long,
    val endMs: Long,
    val title: String,
    val description: String? = null,
)

/** Quick view-model record for UI: a channel plus its current/next programme. */
data class ChannelWithNow(
    val channel: Channel,
    val now: Programme?,
    val next: Programme?,
    val isFavorite: Boolean = false,
)
