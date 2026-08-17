package com.rombaro.tv.data.repo

import com.rombaro.tv.data.db.ChannelDao
import com.rombaro.tv.data.db.PlaylistDao
import com.rombaro.tv.data.db.ProgrammeDao
import com.rombaro.tv.data.db.toDomain
import com.rombaro.tv.data.db.toEntity
import com.rombaro.tv.data.m3u.M3UParser
import com.rombaro.tv.data.xtream.XtreamApi
import com.rombaro.tv.domain.Channel
import com.rombaro.tv.domain.ChannelWithNow
import com.rombaro.tv.domain.Playlist
import com.rombaro.tv.domain.PlaylistType
import com.rombaro.tv.ui.browse.selectNowNext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val channelDao: ChannelDao,
    private val programmeDao: ProgrammeDao,
    private val epgRepo: EpgRepository,
    private val xtream: XtreamApi,
    private val http: OkHttpClient,
) {
    fun observePlaylists(): Flow<List<Playlist>> =
        playlistDao.observeAll().map { it.map(com.rombaro.tv.data.db.PlaylistEntity::toDomain) }

    fun observeChannels(playlistId: Long): Flow<List<Channel>> =
        channelDao.observeByPlaylist(playlistId).map { it.map(com.rombaro.tv.data.db.ChannelEntity::toDomain) }

    fun observeCategories(playlistId: Long): Flow<List<String>> =
        channelDao.observeCategories(playlistId)

    fun observeByCategory(playlistId: Long, category: String?): Flow<List<Channel>> =
        channelDao.observeByCategory(playlistId, category)
            .map { it.map(com.rombaro.tv.data.db.ChannelEntity::toDomain) }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeChannelsWithNow(playlistId: Long): Flow<List<ChannelWithNow>> =
        channelDao.observeByPlaylist(playlistId).flatMapLatest { channelEntities ->
            val channels = channelEntities.map { it.toDomain() }
            val epgIds = channels.mapNotNull { it.epgChannelId }.distinct()
            if (epgIds.isEmpty()) {
                flowOf(channels.map { ChannelWithNow(it, null, null, isFavorite = false) })
            } else {
                val nowMs = System.currentTimeMillis()
                programmeDao.observeUpcomingForChannels(epgIds, nowMs).map { programmeEntities ->
                    val grouped = programmeEntities.groupBy({ it.epgChannelId }, { it.toDomain() })
                    channels.map { ch ->
                        val progs = grouped[ch.epgChannelId].orEmpty()
                        val nowNowMs = System.currentTimeMillis()
                        val (now, next) = selectNowNext(progs, nowNowMs)
                        ChannelWithNow(ch, now, next, isFavorite = false)
                    }
                }
            }
        }

    suspend fun savePlaylist(p: Playlist): Long {
        val id = playlistDao.upsert(p.toEntity())
        refresh(id)
        if (!p.xmltvUrl.isNullOrBlank()) {
            epgRepo.refresh(id)
        }
        return id
    }

    /** Pulls channels from the playlist's source and overwrites the local cache. */
    suspend fun refresh(playlistId: Long) = withContext(Dispatchers.IO) {
        val pl = playlistDao.get(playlistId)?.toDomain() ?: return@withContext
        val channels = when (pl.type) {
            PlaylistType.XTREAM -> xtream.getLiveStreams(
                playlistId = pl.id,
                server = pl.serverUrl,
                username = pl.username.orEmpty(),
                password = pl.password.orEmpty(),
            )
            PlaylistType.M3U -> downloadM3U(pl)
        }
        channelDao.replaceAll(pl.id, channels.map { it.toEntity() })
    }

    private fun downloadM3U(pl: Playlist): List<Channel> {
        val url = pl.m3uUrl ?: pl.serverUrl
        val req = Request.Builder().url(url).apply {
            pl.userAgent?.let { header("User-Agent", it) }
        }.build()
        return http.newCall(req).execute().use { resp ->
            val body = resp.body ?: return emptyList()
            M3UParser.parse(body.charStream().buffered(), pl.id)
        }
    }
}
