package com.rombaro.tv.data.repo

import com.rombaro.tv.data.db.ProgrammeDao
import com.rombaro.tv.data.db.ProgrammeEntity
import com.rombaro.tv.data.db.PlaylistDao
import com.rombaro.tv.data.db.PlaylistEntity
import com.rombaro.tv.domain.Programme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EpgRepositoryIdempotentTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var testScope: TestScope

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        testScope = TestScope(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ---- production behaviour under test ----

    @Test
    fun identicalProgrammeTwice_remainsOneLogicalRowPerSlot() = runTest {
        val dao = InMemoryProgrammeDao()
        val repo = testRepo(dao) { listOf(prog("ch1", "Test")) }

        repo.refresh(1L)
        testScheduler.advanceUntilIdle()
        assertEquals("first refresh inserts one row", 1L, dao.programmesInDb.size.toLong())

        repo.refresh(1L)
        testScheduler.advanceUntilIdle()
        assertEquals("identical second refresh keeps one row", 1L, dao.programmesInDb.size.toLong())
    }

    @Test
    fun changedTitle_replacesOldData() = runTest {
        val dao = InMemoryProgrammeDao()
        val feed = arrayOf(
            listOf(prog("ch1", "Original")),
            listOf(prog("ch1", "Updated")),
        )
        var idx = 0
        val repo = testRepo(dao) { feed[idx].also { idx++ } }

        repo.refresh(1L)
        testScheduler.advanceUntilIdle()
        assertEquals("Original", dao.get("ch1", 3600000L)?.title)

        repo.refresh(1L)
        testScheduler.advanceUntilIdle()
        assertEquals("title should be replaced", "Updated", dao.get("ch1", 3600000L)?.title)
        assertEquals("still one row for the slot", 1L, dao.programmesInDb.size.toLong())
    }

    @Test
    fun HTTP_failure_preservesExistingProgrammeData() = runTest {
        val dao = InMemoryProgrammeDao()
        dao.insertAll(listOf(entity("ch1", "Existing")))

        val repo = testRepo(dao) { throw RuntimeException("HTTP 500") }

        var failed = false
        try {
            repo.refresh(1L)
        } catch (e: Exception) {
            failed = true
        }
        testScheduler.advanceUntilIdle()
        assertTrue("HTTP failure must propagate", failed)
        assertEquals("existing data preserved", "Existing", dao.get("ch1", 3600000L)?.title)
        assertEquals("no DB mutation on failure", 1L, dao.programmesInDb.size.toLong())
    }

    @Test
    fun emptyFeed_preservesExistingProgrammeData() = runTest {
        val dao = InMemoryProgrammeDao()
        dao.insertAll(listOf(entity("ch1", "Existing")))

        val repo = testRepo(dao) { emptyList() }

        repo.refresh(1L)
        testScheduler.advanceUntilIdle()
        assertEquals("existing data preserved", "Existing", dao.get("ch1", 3600000L)?.title)
        assertEquals("empty feed mutates nothing", 1L, dao.programmesInDb.size.toLong())
    }

    @Test
    fun programmeOutsideRefreshWindow_survives() = runTest {
        val dao = InMemoryProgrammeDao()
        dao.insertAll(listOf(
            entity("ch1", "Within", 3600000L),
            entity("ch1", "Outside", 10000000L),
        ))

        val repo = testRepo(dao) { listOf(prog("ch1", "Replaced")) }

        repo.refresh(1L)
        testScheduler.advanceUntilIdle()
        assertEquals("within-window replaced", "Replaced", dao.get("ch1", 3600000L)?.title)
        assertEquals("outside-window survives", "Outside", dao.get("ch1", 10000000L)?.title)
        assertEquals("outside-window row untouched", 2L, dao.programmesInDb.size.toLong())
    }

    // ---- test seam: drives production refresh with a controlled feed ----

    private fun testRepo(
        dao: ProgrammeDao,
        feed: (Request) -> List<Programme>?,
    ) = TestEpgRepository(dao, stubPlaylistDao(), feed)

    private class TestEpgRepository(
        dao: ProgrammeDao,
        playlistDao: PlaylistDao,
        private val feed: (Request) -> List<Programme>?,
    ) : EpgRepository(dao, playlistDao, OkHttpClient()) {
        override suspend fun fetchProgrammes(req: Request): List<Programme>? = feed(req)
        override suspend fun nowMs(): Long = 3_600_000L
    }

    // ---- fixtures ----

    private fun prog(channel: String, title: String) = Programme(
        epgChannelId = channel,
        startMs = 3600000L,
        endMs = 5400000L,
        title = title,
    )

    private fun entity(channel: String, title: String, startMs: Long = 3600000L) =
        ProgrammeEntity(
            epgChannelId = channel,
            startMs = startMs,
            endMs = 5400000L,
            title = title,
            description = null,
        )
}

// ============================================
// In-memory ProgrammeDao used by the idempotency tests
// ============================================

class InMemoryProgrammeDao : ProgrammeDao {
    val programmesInDb = mutableMapOf<Long, ProgrammeEntity>()
    private var idCounter = 1L

    override suspend fun insertAll(items: List<ProgrammeEntity>) {
        items.forEach { programmesInDb[idCounter++] = it }
    }

    override suspend fun replaceProgrammesForChannelsAndWindow(
        epgChannelIds: List<String>,
        startMsFrom: Long,
        startMsTo: Long,
        newProgrammes: List<ProgrammeEntity>,
    ) {
        deleteProgrammesForChannelsAndWindow(epgChannelIds, startMsFrom, startMsTo)
        insertAll(newProgrammes)
    }

    override suspend fun deleteProgrammesForChannelsAndWindow(
        epgChannelIds: List<String>,
        startMsFrom: Long,
        startMsTo: Long,
    ) {
        val toRemove = programmesInDb.filter { (_, p) ->
            epgChannelIds.contains(p.epgChannelId) &&
                p.startMs >= startMsFrom &&
                p.startMs < startMsTo
        }
        toRemove.keys.forEach { programmesInDb.remove(it) }
    }

    override suspend fun pruneBefore(before: Long) {
        val toRemove = programmesInDb.filter { (_, p) -> p.endMs < before }
        toRemove.keys.forEach { programmesInDb.remove(it) }
    }

    override suspend fun upcoming(epgId: String, nowMs: Long, limit: Int): List<ProgrammeEntity> =
        programmesInDb.values.filter { p ->
            p.epgChannelId == epgId && p.endMs >= nowMs
        }.sortedBy { it.startMs }.take(limit)

    override suspend fun nowPlaying(epgId: String, nowMs: Long): ProgrammeEntity? =
        programmesInDb.values.find { p ->
            p.epgChannelId == epgId && p.startMs <= nowMs && p.endMs > nowMs
        }

    override fun observeUpcomingForChannels(epgIds: List<String>, nowMs: Long): Flow<List<ProgrammeEntity>> =
        flowOf(emptyList())

    fun get(epgChannelId: String, startMs: Long): ProgrammeEntity? =
        programmesInDb.values.find { it.epgChannelId == epgChannelId && it.startMs == startMs }
}

// ============================================
// Stub playlist used by the idempotency tests
// ============================================

private fun stubPlaylistDao(): PlaylistDao = object : PlaylistDao {
    override suspend fun upsert(p: PlaylistEntity): Long = 1L
    override fun observeAll(): Flow<List<PlaylistEntity>> = flowOf()
    override suspend fun get(id: Long): PlaylistEntity? =
        PlaylistEntity(
            id = id,
            name = "Test",
            type = "M3U",
            serverUrl = "",
            username = null,
            password = null,
            m3uUrl = null,
            xmltvUrl = "https://example.com/epg.xml",
            userAgent = null,
        )
    override suspend fun delete(id: Long) {}
}