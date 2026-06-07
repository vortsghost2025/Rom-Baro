package com.rombaro.tv.data.m3u

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedReader
import java.io.StringReader

class M3UParserTest {

    private val sample = """
        #EXTM3U
        #EXTINF:-1 tvg-id="cnn.us" tvg-logo="https://logo.png" group-title="News",CNN HD
        http://example.tld/live/u/p/1.ts
        #EXTINF:-1 tvg-id="bbc.uk" group-title="News",BBC One
        http://example.tld/live/u/p/2.ts
        #EXTINF:-1,Some Channel With No Attrs
        http://example.tld/live/u/p/3.ts
    """.trimIndent()

    @Test fun `parses three channels with attrs`() {
        val out = M3UParser.parse(BufferedReader(StringReader(sample)), playlistId = 7L)
        assertEquals(3, out.size)
        assertEquals("CNN HD", out[0].name)
        assertEquals("cnn.us", out[0].epgChannelId)
        assertEquals("News", out[0].category)
        assertEquals("https://logo.png", out[0].logoUrl)
        assertEquals(7L, out[0].playlistId)
        assertTrue(out[2].streamId.isNotBlank()) // hashed fallback
    }
}
