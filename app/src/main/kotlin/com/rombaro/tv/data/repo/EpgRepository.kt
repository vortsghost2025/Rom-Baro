package com.rombaro.tv.data.repo

import com.rombaro.tv.data.db.PlaylistDao
import com.rombaro.tv.data.db.ProgrammeDao
import com.rombaro.tv.data.db.toDomain
import com.rombaro.tv.data.db.toEntity
import com.rombaro.tv.data.epg.XmlTvParser
import com.rombaro.tv.domain.Programme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpgRepository @Inject constructor(
    private val programmes: ProgrammeDao,
    private val playlists: PlaylistDao,
    private val http: OkHttpClient,
) {
    suspend fun refresh(playlistId: Long) = withContext(Dispatchers.IO) {
        val pl = playlists.get(playlistId) ?: return@withContext
        val url = pl.xmltvUrl ?: return@withContext

        val req = Request.Builder().url(url).apply {
            pl.userAgent?.let { header("User-Agent", it) }
        }.build()

        http.newCall(req).execute().use { resp ->
            val body = resp.body ?: return@withContext
            val parsed = XmlTvParser.parse(body.byteStream())
            programmes.insertAll(parsed.map(Programme::toEntity))
        }
        // prune anything older than 6h to keep the DB small
        programmes.pruneBefore(System.currentTimeMillis() - 6L * 60 * 60 * 1000)
    }

    suspend fun nowPlaying(epgChannelId: String, nowMs: Long = System.currentTimeMillis()): Programme? =
        programmes.nowPlaying(epgChannelId, nowMs)?.toDomain()

    suspend fun upcoming(epgChannelId: String, limit: Int = 5): List<Programme> =
        programmes.upcoming(epgChannelId, System.currentTimeMillis(), limit).map { it.toDomain() }
}
