package com.rombaro.tv.data.m3u

import com.rombaro.tv.data.db.toEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedReader
import java.io.StringReader

class DemoPlaylistFixtureTest {

    private val EMBEDDED_M3U = """
        #EXTM3U

        #EXTINF:-1 tvg-id="demo.cinema" group-title="Movies",Demo Cinema
        https://www.w3schools.com/html/mov_bbb.mp4

        #EXTINF:-1 tvg-id="demo.animation" group-title="Animation",Demo Animation
        https://samplelib.com/lib/preview/mp4/sample-15s.mp4

        #EXTINF:-1 tvg-id="demo.scifi" group-title="Sci-Fi Shorts",Demo Sci-Fi Short
        https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8

        #EXTINF:-1 tvg-id="demo.mux" group-title="Test Streams",Demo Mux Test
        https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8

        #EXTINF:-1 tvg-id="demo.akamai" group-title="Test Streams",Demo Akamai Test
        https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8

        #EXTINF:-1 tvg-id="demo.showcase" group-title="Showcase",Demo Showcase
        https://samplelib.com/lib/preview/mp4/sample-5s.mp4

        #EXTINF:-1 tvg-id="demo.classics" group-title="Movies",Demo Classics
        https://www.w3schools.com/html/movie.mp4

        #EXTINF:-1 tvg-id="demo.kids" group-title="Kids",Demo Kids
        https://samplelib.com/lib/preview/mp4/sample-30s.mp4
    """.trimIndent()

    @Test
    fun `embedded fixture M3U has exactly 8 channels`() {
        val channels = M3UParser.parse(BufferedReader(StringReader(EMBEDDED_M3U)), 1L)
        assertEquals(8, channels.size)
    }

    @Test
    fun `fixture channels have stream URLs and non-empty names`() {
        val channels = M3UParser.parse(BufferedReader(StringReader(EMBEDDED_M3U)), 1L)
        channels.forEach { ch ->
            assertTrue("Name must not be blank: id=${ch.streamId}", ch.name.isNotBlank())
            assertTrue("URL must be http(s): ${ch.streamUrl}",
                ch.streamUrl.startsWith("http://") || ch.streamUrl.startsWith("https://"))
        }
    }

    @Test
    fun `fixture channels have distinct streamIds`() {
        val channels = M3UParser.parse(BufferedReader(StringReader(EMBEDDED_M3U)), 1L)
        val ids = channels.map { it.streamId }
        assertEquals("Stream IDs must be unique", ids.size, ids.toSet().size)
    }

    @Test
    fun `fixture channels have unique epgChannelIds where set`() {
        val channels = M3UParser.parse(BufferedReader(StringReader(EMBEDDED_M3U)), 1L)
        val epgIds = channels.mapNotNull { it.epgChannelId }
        assertEquals("EPG IDs must be unique", epgIds.size, epgIds.toSet().size)
    }

    @Test
    fun `fixture channel names start with Demo prefix`() {
        val channels = M3UParser.parse(BufferedReader(StringReader(EMBEDDED_M3U)), 1L)
        val names = channels.map { it.name }
        names.forEach { n ->
            assertTrue("Channel name '$n' must start with 'Demo '", n.startsWith("Demo "))
        }
    }

    @Test
    fun `fixture no commercial brand name leakage`() {
        val channels = M3UParser.parse(BufferedReader(StringReader(EMBEDDED_M3U)), 1L)
        val names = channels.map { it.name }.joinToString(" ")
        val blocked = listOf("CNN", "Disney", "Eurosport", "NASA", "Al Jazeera", "CGTN")
        blocked.forEach { brand ->
            assertTrue("Brand name '$brand' must not appear in: $names",
                !names.contains(brand, ignoreCase = true))
        }
    }

    @Test
    fun `fixture all URLs are public sample endpoints`() {
        val channels = M3UParser.parse(BufferedReader(StringReader(EMBEDDED_M3U)), 1L)
        val blockedDomains = listOf("dvr05.rombaro-demo.net", "demo-1.rombaro.tv",
            "ntv1.akamaihd.net")
        channels.forEach { ch ->
            blockedDomains.forEach { domain ->
                assertTrue("URL ${ch.streamUrl} must not contain blocked domain $domain",
                    !ch.streamUrl.contains(domain))
            }
        }
        val publicDomains = listOf(
            "w3schools.com",
            "samplelib.com",
            "demo.unified-streaming.com",
            "test-streams.mux.dev",
            "cph-p2p-msl.akamaized.net",
        )
        channels.forEach { ch ->
            assertTrue("URL ${ch.streamUrl} must be from a verified public domain",
                publicDomains.any { ch.streamUrl.contains(it) })
        }
    }

    @Test
    fun `fixture all channels have a category`() {
        val channels = M3UParser.parse(BufferedReader(StringReader(EMBEDDED_M3U)), 1L)
        channels.forEach { ch ->
            assertTrue("${ch.name} must have a category", !ch.category.isNullOrBlank())
        }
        val categories = channels.mapNotNull { it.category }.toSet()
        assertTrue("Need at least 3 distinct categories, got: $categories", categories.size >= 3)
    }

    @Test
    fun `fixture has 6 distinct categories`() {
        val channels = M3UParser.parse(BufferedReader(StringReader(EMBEDDED_M3U)), 1L)
        val categories = channels.mapNotNull { it.category }.toSet()
        assertEquals("Expected 6 categories, got: $categories", 6, categories.size)
    }

    @Test
    fun `fixture channel names match expected demo lineup`() {
        val channels = M3UParser.parse(BufferedReader(StringReader(EMBEDDED_M3U)), 1L)
        val names = channels.map { it.name }.toSet()
        val expected = setOf(
            "Demo Akamai Test",
            "Demo Animation",
            "Demo Cinema",
            "Demo Classics",
            "Demo Kids",
            "Demo Mux Test",
            "Demo Sci-Fi Short",
            "Demo Showcase",
        )
        assertEquals("Demo lineup mismatch", expected, names)
    }

    @Test
    fun `fixture has exactly 8 channels`() {
        val channels = M3UParser.parse(BufferedReader(StringReader(EMBEDDED_M3U)), 1L)
        assertEquals(8, channels.size)
    }

    @Test
    fun `stamped channels carry persisted playlistId through toEntity`() {
        val channels = M3UParser.parse(BufferedReader(StringReader(EMBEDDED_M3U)), 0L)
        val persistedId = 42L
        val stamped = channels.map { ch -> ch.toEntity().copy(playlistId = persistedId) }
        assertEquals("all 8 entities must carry persisted playlistId", 8, stamped.size)
        stamped.forEach { e ->
            assertEquals("playlistId mismatch on streamId=${e.streamId}", persistedId, e.playlistId)
        }
    }
}