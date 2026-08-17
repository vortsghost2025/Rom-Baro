package com.rombaro.tv.data.repo

import com.rombaro.tv.data.db.ProgrammeDao
import com.rombaro.tv.data.db.PlaylistDao
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
open class EpgRepository @Inject constructor(
    private val programmes: ProgrammeDao,
    private val playlists: PlaylistDao,
    private val http: OkHttpClient,
) {
    @Throws(Exception::class)
    open suspend fun refresh(playlistId: Long) =
        withContext(Dispatchers.IO) { refreshInternal(playlistId) }

    /**
     * Core refresh logic, kept on a single dispatcher so it can be driven
     * deterministically from JVM tests without a real HTTP round-trip.
     *
     * HTTP failure, parse failure, null body and an empty parsed feed all
     * short-circuit before any database mutation occurs.
     */
    protected open suspend fun refreshInternal(playlistId: Long) {
        val pl = playlists.get(playlistId) ?: return
        val url = pl.xmltvUrl ?: return

        val req = Request.Builder().url(url).apply {
            pl.userAgent?.let { header("User-Agent", it) }
        }.build()

        val parsed = fetchProgrammes(req) ?: return
        applyParsed(parsed)
    }

    /**
     * Derives the affected channel set and replacement window from the
     * parsed feed, replaces the programmes in that window, then prunes
     * anything older than 6h. Runs inside the bounded
     * [replaceProgrammesForChannelsAndWindow] transaction.
     */
    protected open suspend fun applyParsed(parsed: List<Programme>) {
        if (parsed.isEmpty()) return

        val epgChannelIds = parsed.map { it.epgChannelId }.toSet()
        val startMsValues = parsed.map { it.startMs }
        val minStartMs = startMsValues.minOrNull() ?: return
        val maxStartMs = startMsValues.maxOrNull() ?: return

        val newProgrammes = parsed.map { it.toEntity() }

        programmes.replaceProgrammesForChannelsAndWindow(
            epgChannelIds.toList(),
            minStartMs,
            maxStartMs + 1, // +1 to make the window inclusive of the end
            newProgrammes,
        )

        // prune anything older than 6h to keep the DB small
        programmes.pruneBefore(nowMs() - 6L * 60 * 60 * 1000)
    }

    /**
     * Fetches and parses the EPG feed for the given request.
     *
     * The HTTP response is always closed via `use`. Returns null for any
     * failure (HTTP error, missing body, parse failure) so that callers
     * never mutate the database on a failed refresh.
     */
    protected open suspend fun fetchProgrammes(req: Request): List<Programme>? =
        withContext(Dispatchers.IO) {
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    throw RuntimeException("HTTP ${resp.code}")
                }
                val body = resp.body ?: return@withContext null
                XmlTvParser.parse(body.byteStream())
            }
        }

    /**
     * Current wall-clock time in millis. Overridable in tests so that
     * synthetic EPG timestamps are not pruned immediately.
     */
    protected open suspend fun nowMs(): Long = System.currentTimeMillis()

    suspend fun nowPlaying(epgChannelId: String, nowMs: Long = System.currentTimeMillis()): Programme? =
        programmes.nowPlaying(epgChannelId, nowMs)?.toDomain()

    suspend fun upcoming(epgChannelId: String, limit: Int = 5): List<Programme> =
        programmes.upcoming(epgChannelId, System.currentTimeMillis(), limit).map { it.toDomain() }
}
