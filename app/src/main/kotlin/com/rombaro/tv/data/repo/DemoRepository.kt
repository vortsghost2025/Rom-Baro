package com.rombaro.tv.data.repo

import android.content.Context
import com.rombaro.tv.data.db.ChannelDao
import com.rombaro.tv.data.db.PlaylistDao
import com.rombaro.tv.data.db.ProgrammeDao
import com.rombaro.tv.data.db.toEntity
import com.rombaro.tv.data.m3u.M3UParser
import com.rombaro.tv.domain.Channel
import com.rombaro.tv.domain.Playlist
import com.rombaro.tv.domain.PlaylistType
import com.rombaro.tv.domain.Programme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Self-contained demo-mode data provider.
 *
 * Reads the bundled [demo_playlist.m3u] asset, saves it as a normal Room playlist,
 * and writes synthetic Now/Next EPG entries relative to the current wall-clock time.
 *
 * Demo data is isolated to its own playlist row in the database; normal production
 * playlist flows and all existing favorites / EPG behaviour are unaffected.
 */
class DemoRepository @Inject constructor(
    private val ctx: Context,
    private val playlistDao: PlaylistDao,
    private val channelDao: ChannelDao,
    private val programmeDao: ProgrammeDao,
) {

    companion object {
        private const val ASSET_M3U = "demo_playlist.m3u"
        private const val DEMO_PLAYLIST_NAME = "Rom Baro Demo"

        /** Programme duration in milliseconds — 2 hours per slot. */
        private const val SLOT_MS = 2L * 60 * 60 * 1000
    }

    /**
     * Wall-clock epoch-millis source. Override in tests to freeze time.
     */
    private fun nowMs(): Long = System.currentTimeMillis()

    /**
     * Loads the demo fixture into the local database and returns the new playlist ID.
     *
     * Existing demo rows are replaced, so this is safe to call more than once.
     */
    suspend fun loadDemo(): Long = withContext(Dispatchers.IO) {
        val channels = parseDemoM3U()
        if (channels.isEmpty()) return@withContext 0L

        val playlist = Playlist(
            name = DEMO_PLAYLIST_NAME,
            type = PlaylistType.M3U,
            m3uUrl = "asset://$ASSET_M3U",
            serverUrl = "asset://$ASSET_M3U",
        )

        val playlistId = playlistDao.upsert(playlist.toEntity())
        channelDao.replaceAll(
            playlistId,
            channels.map { ch -> ch.toEntity().copy(playlistId = playlistId) },
        )

        val programmes = generateProgrammes(channels, nowMs())
        if (programmes.isNotEmpty()) {
            val nowMs = nowMs()
            val minStartMs = programmes.minOf { it.startMs }
            val maxStartMs = programmes.maxOf { it.startMs }
            programmeDao.replaceProgrammesForChannelsAndWindow(
                epgChannelIds = channels.mapNotNull { it.epgChannelId },
                startMsFrom = minStartMs,
                startMsTo = maxStartMs + 1,
                newProgrammes = programmes.map { it.toEntity() },
            )
            programmeDao.pruneBefore(nowMs - 6L * 60 * 60 * 1000)
        }

        playlistId
    }

    private fun parseDemoM3U(): List<Channel> {
        return ctx.assets.open(ASSET_M3U).bufferedReader().use { reader ->
            M3UParser.parse(reader, 0L)
        }
    }

    /**
     * Generates synthetic EPG entries for [channels] as of [fixedNow].
     *
     * For each channel with a non-null [epgChannelId], produces 5 slots:
     *   3 × 2 h in the past, 1 × current 2-h window, 1 × next 2-h window.
     * All timestamps are derived from [fixedNow] so the output is fully deterministic.
     */
    internal fun generateProgrammes(
        channels: List<Channel>,
        fixedNow: Long,
    ): List<Programme> {
        val result = mutableListOf<Programme>()

        var globalSeq = 0
        val thisHourStart = (fixedNow / SLOT_MS) * SLOT_MS

        for (ch in channels) {
            val epgId = ch.epgChannelId ?: continue

            val slots = listOf(
                thisHourStart - 3L * SLOT_MS to "Earlier: ${ch.name}",
                thisHourStart - 2L * SLOT_MS to "Late Morning: ${ch.name}",
                thisHourStart - 1L * SLOT_MS to "Midday: ${ch.name}",
                thisHourStart             to ch.name,
                thisHourStart + 1L * SLOT_MS to "Coming Up: ${ch.name}",
            )

            for ((startMs, title) in slots) {
                result += Programme(
                    epgChannelId = epgId,
                    startMs = startMs,
                    endMs = startMs + SLOT_MS,
                    title = title,
                    description = "Demo programme — Episode ${globalSeq + 1}",
                )
                globalSeq++
            }
        }

        return result
    }
}