package com.rombaro.tv.ui.browse

import com.rombaro.tv.domain.Channel
import com.rombaro.tv.domain.ChannelWithNow
import com.rombaro.tv.domain.Programme
import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelCardPresenterTest {

    private fun ch(name: String = "Ch1", category: String? = "News") = Channel(
        id = 0L, playlistId = 0L, streamId = "s1", name = name,
        streamUrl = "http://example.com/stream", logoUrl = null,
        category = category, epgChannelId = "epg1", orderHint = 0,
    )

    private fun prog(title: String) = Programme(
        id = 0L, epgChannelId = "epg1",
        startMs = 1000L, endMs = 2000L, title = title, description = null,
    )

    @Test
    fun nowAndNext_bothExist() {
        val cwn = ChannelWithNow(ch(), prog("TV LIVE NOW TEST"), prog("TV LIVE NEXT TEST"))
        assertEquals("Now: TV LIVE NOW TEST\nNext: TV LIVE NEXT TEST", epgCardText(cwn))
    }

    @Test
    fun nowOnly() {
        val cwn = ChannelWithNow(ch(), prog("TV LIVE NOW TEST"), null)
        assertEquals("Now: TV LIVE NOW TEST", epgCardText(cwn))
    }

    @Test
    fun nextOnly() {
        val cwn = ChannelWithNow(ch(), null, prog("TV LIVE NEXT TEST"))
        assertEquals("Next: TV LIVE NEXT TEST", epgCardText(cwn))
    }

    @Test
    fun neither_fallsBackToCategory() {
        val cwn = ChannelWithNow(ch(category = "Weather"), null, null)
        assertEquals("Weather", epgCardText(cwn))
    }
}
