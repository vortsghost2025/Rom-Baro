package com.rombaro.tv.ui.browse

import com.rombaro.tv.domain.Channel
import com.rombaro.tv.domain.ChannelWithNow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowseStateTest {

    private fun chwn(
        streamId: String,
        category: String? = "News",
        playlistId: Long = 1L,
        isFavorite: Boolean = false,
    ) = ChannelWithNow(
        channel = Channel(
            playlistId = playlistId,
            streamId = streamId,
            name = "Channel $streamId",
            streamUrl = "http://example/$streamId",
            category = category,
        ),
        now = null,
        next = null,
        isFavorite = isFavorite,
    )

    @Test
    fun `zero favorites produces empty favorite list`() {
        val channels = listOf(chwn("s1"), chwn("s2"))
        val state = buildBrowseState(
            categories = listOf("News"),
            channels = channels,
        )
        assertTrue(state.favoriteChannels.isEmpty())
    }

    @Test
    fun `matching isFavorite produces correct favorite channels`() {
        val channels = listOf(chwn("s1"), chwn("s2", isFavorite = true), chwn("s3"))
        val state = buildBrowseState(
            categories = listOf("News"),
            channels = channels,
        )
        assertEquals(1, state.favoriteChannels.size)
        assertEquals("s2", state.favoriteChannels[0].channel.streamId)
    }

    @Test
    fun `no favorites are excluded from favorites`() {
        val channels = listOf(chwn("s1"), chwn("s2"))
        val state = buildBrowseState(
            categories = listOf("News"),
            channels = channels,
        )
        assertTrue(state.favoriteChannels.isEmpty())
    }

    @Test
    fun `ordering is deterministic - favorites follow insertion order`() {
        val channels = listOf(
            chwn("s3", isFavorite = true),
            chwn("s1", isFavorite = true),
            chwn("s2"),
        )
        val state = buildBrowseState(
            categories = listOf("News"),
            channels = channels,
        )
        assertEquals(2, state.favoriteChannels.size)
        assertEquals("s3", state.favoriteChannels[0].channel.streamId)
        assertEquals("s1", state.favoriteChannels[1].channel.streamId)
    }

    @Test
    fun `duplicate favorite flags do not duplicate channels`() {
        val channels = listOf(chwn("s1", isFavorite = true))
        val state = buildBrowseState(
            categories = listOf("News"),
            channels = channels,
        )
        assertEquals(1, state.favoriteChannels.size)
        assertEquals("s1", state.favoriteChannels[0].channel.streamId)
    }

    @Test
    fun `empty categories produces single All Channels row`() {
        val channels = listOf(chwn("s1", category = null), chwn("s2", category = null))
        val state = buildBrowseState(
            categories = emptyList(),
            channels = channels,
        )
        assertEquals(1, state.categoryRows.size)
        assertEquals(null, state.categoryRows[0].first)
        assertEquals(2, state.categoryRows[0].second.size)
    }

    @Test
    fun `categories map to correct rows`() {
        val channels = listOf(
            chwn("s1", category = "News"),
            chwn("s2", category = "Sports"),
            chwn("s3", category = "News"),
        )
        val state = buildBrowseState(
            categories = listOf("News", "Sports"),
            channels = channels,
        )
        assertEquals(2, state.categoryRows.size)
        assertEquals("News", state.categoryRows[0].first)
        assertEquals(2, state.categoryRows[0].second.size)
        assertEquals("Sports", state.categoryRows[1].first)
        assertEquals(1, state.categoryRows[1].second.size)
    }

    @Test
    fun `favorites row is separate from category rows`() {
        val channels = listOf(chwn("s1"), chwn("s2", category = "Sports", isFavorite = true))
        val state = buildBrowseState(
            categories = listOf("News", "Sports"),
            channels = channels,
        )
        assertEquals(1, state.favoriteChannels.size)
        assertEquals(2, state.categoryRows.size)
    }

    @Test
    fun `channel not in any category still appears in all-channels fallback`() {
        val channels = listOf(chwn("s1", category = "News"), chwn("s2", category = null))
        val state = buildBrowseState(
            categories = listOf("News"),
            channels = channels,
        )
        assertEquals(1, state.categoryRows.size)
        assertEquals("News", state.categoryRows[0].first)
        assertEquals(1, state.categoryRows[0].second.size)
    }
}
