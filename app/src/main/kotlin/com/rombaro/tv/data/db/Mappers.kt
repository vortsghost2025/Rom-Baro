package com.rombaro.tv.data.db

import com.rombaro.tv.domain.Channel
import com.rombaro.tv.domain.Playlist
import com.rombaro.tv.domain.PlaylistType
import com.rombaro.tv.domain.Programme

fun PlaylistEntity.toDomain() = Playlist(
    id, name, PlaylistType.valueOf(type), serverUrl,
    username, password, m3uUrl, xmltvUrl, userAgent,
)

fun Playlist.toEntity() = PlaylistEntity(
    id, name, type.name, serverUrl,
    username, password, m3uUrl, xmltvUrl, userAgent,
)

fun ChannelEntity.toDomain() = Channel(
    id, playlistId, streamId, name, streamUrl,
    logoUrl, category, epgChannelId, orderHint,
)

fun Channel.toEntity() = ChannelEntity(
    id, playlistId, streamId, name, streamUrl,
    logoUrl, category, epgChannelId, orderHint,
)

fun ProgrammeEntity.toDomain() = Programme(
    id, epgChannelId, startMs, endMs, title, description,
)

fun Programme.toEntity() = ProgrammeEntity(
    id, epgChannelId, startMs, endMs, title, description,
)
