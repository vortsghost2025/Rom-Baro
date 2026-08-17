package com.rombaro.tv.data.repo

import com.rombaro.tv.data.db.PlaylistDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates EPG refresh with per-playlist throttle.
 * Prevents refresh storms from rapid fragment recreation or resume.
 */
@Singleton
class EpgRefreshCoordinator @Inject constructor(
    private val epgRepo: EpgRepository,
    private val playlistDao: PlaylistDao,
) {
    // Production default: IO dispatcher. Overridable for tests.
    internal var scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val lastRefreshMs = mutableMapOf<Long, Long>()
    private val pending = mutableSetOf<Long>()

    /**
     * Refresh EPG for a playlist if eligible.
     * Throttled: skips if refreshed within [minIntervalMs].
     * Non-blocking: fire-and-forget, failures are swallowed.
     */
    fun refreshIfEligible(playlistId: Long, minIntervalMs: Long = 60_000L) {
        val now = System.currentTimeMillis()
        val last = lastRefreshMs[playlistId] ?: 0L
        if (now - last < minIntervalMs) return
        if (playlistId in pending) return

        pending += playlistId
        lastRefreshMs[playlistId] = now
        scope.launch {
            try {
                epgRepo.refresh(playlistId)
            } catch (_: Exception) {
                // Failure must not destroy existing data; just skip.
            } finally {
                pending -= playlistId
            }
        }
    }
}
