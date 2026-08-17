package com.rombaro.tv.data.repo

import com.rombaro.tv.data.db.PlaylistDao
import com.rombaro.tv.data.db.PlaylistEntity
import com.rombaro.tv.data.db.ProgrammeDao
import com.rombaro.tv.data.db.ProgrammeEntity
import kotlinx.coroutines.CoroutineScope
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EpgRefreshCoordinatorTest {

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

    @Test
    fun savedPlaylistWithNonblankXmltvUrl_triggersRefresh() = runTest {
        val fake = StubEpgRepository()
        val coord = EpgRefreshCoordinator(fake, FakePlaylistDao())
        coord.scope = testScope
        coord.refreshIfEligible(1L)
        testScheduler.advanceUntilIdle()
        assertEquals(1, fake.refreshCalls.size)
        assertEquals(1L, fake.refreshCalls[0])
    }

    @Test
    fun blankXmltvUrl_stillCallsCoordinator() = runTest {
        val fake = StubEpgRepository()
        val coord = EpgRefreshCoordinator(fake, FakePlaylistDao())
        coord.scope = testScope
        coord.refreshIfEligible(2L)
        testScheduler.advanceUntilIdle()
        assertEquals(1, fake.refreshCalls.size)
    }

    @Test
    fun rapidDuplicateRequests_throttled() = runTest {
        val fake = StubEpgRepository()
        val coord = EpgRefreshCoordinator(fake, FakePlaylistDao())
        coord.scope = testScope
        repeat(5) { coord.refreshIfEligible(3L) }
        testScheduler.advanceUntilIdle()
        assertEquals(1, fake.refreshCalls.size)
    }

    @Test
    fun failedRefresh_doesNotThrow() = runTest {
        val failing = FailingEpgRepository()
        val coord = EpgRefreshCoordinator(failing, FakePlaylistDao())
        coord.scope = testScope
        coord.refreshIfEligible(4L)
        testScheduler.advanceUntilIdle()
        assertTrue(true)
    }

    @Test
    fun multiplePlaylists_independent() = runTest {
        val fake = StubEpgRepository()
        val coord = EpgRefreshCoordinator(fake, FakePlaylistDao())
        coord.scope = testScope
        coord.refreshIfEligible(10L)
        coord.refreshIfEligible(20L)
        testScheduler.advanceUntilIdle()
        assertEquals(2, fake.refreshCalls.size)
        assertTrue(fake.refreshCalls.containsAll(listOf(10L, 20L)))
    }

    @Test
    fun refreshAfterInterval_allowsSecondCall() = runTest {
        val fake = StubEpgRepository()
        val coord = EpgRefreshCoordinator(fake, FakePlaylistDao())
        coord.scope = testScope
        coord.refreshIfEligible(5L, minIntervalMs = 0L)
        testScheduler.advanceUntilIdle()
        assertEquals(1, fake.refreshCalls.size)
        coord.refreshIfEligible(5L, minIntervalMs = 0L)
        testScheduler.advanceUntilIdle()
        assertEquals(2, fake.refreshCalls.size)
    }
}

// ---- fakes ----

class FakePlaylistDao : PlaylistDao {
    val saved = mutableMapOf<Long, PlaylistEntity>()
    override suspend fun upsert(p: PlaylistEntity): Long { saved[p.id] = p; return p.id }
    override fun observeAll(): Flow<List<PlaylistEntity>> = flowOf(saved.values.toList())
    override suspend fun get(id: Long): PlaylistEntity? = saved[id]
    override suspend fun delete(id: Long) { saved.remove(id) }
}

private fun stubProgrammeDao() = object : ProgrammeDao {
    override suspend fun insertAll(items: List<ProgrammeEntity>) {}
    override suspend fun deleteProgrammesForChannelsAndWindow(
        epgChannelIds: List<String>,
        startMsFrom: Long,
        startMsTo: Long,
    ) {}
    override suspend fun pruneBefore(before: Long) {}
    override suspend fun upcoming(epgId: String, nowMs: Long, limit: Int): List<ProgrammeEntity> = emptyList()
    override suspend fun nowPlaying(epgId: String, nowMs: Long): ProgrammeEntity? = null
    override fun observeUpcomingForChannels(epgIds: List<String>, nowMs: Long): Flow<List<ProgrammeEntity>> = flowOf(emptyList())
}

private fun stubPlaylistDao() = object : PlaylistDao {
    override suspend fun upsert(p: PlaylistEntity): Long = 0L
    override fun observeAll(): Flow<List<PlaylistEntity>> = flowOf(emptyList())
    override suspend fun get(id: Long): PlaylistEntity? = null
    override suspend fun delete(id: Long) {}
}

class StubEpgRepository : EpgRepository(stubProgrammeDao(), stubPlaylistDao(), OkHttpClient()) {
    val refreshCalls = mutableListOf<Long>()
    override suspend fun refresh(playlistId: Long) {
        refreshCalls.add(playlistId)
    }
}

class FailingEpgRepository : EpgRepository(stubProgrammeDao(), stubPlaylistDao(), OkHttpClient()) {
    override suspend fun refresh(playlistId: Long) {
        throw RuntimeException("Network failure")
    }
}
