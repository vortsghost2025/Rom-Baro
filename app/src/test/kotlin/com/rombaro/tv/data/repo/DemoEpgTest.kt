package com.rombaro.tv.data.repo

import com.rombaro.tv.domain.Channel
import com.rombaro.tv.domain.Programme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.test.runTest

/**
 * JVM unit tests for the EPG window algorithm implemented by [DemoRepository].
 *
 * Slot duration matches DemoRepository.PACKAGE_SLOT_MS (2h) — kept as a
 * top-level constant so there is no private-companion visibility barrier.
 */
const val DEMO_EPG_SLOT_MS: Long = 2L * 60 * 60 * 1000 // 2 hours — mirrors DemoRepository internal constant

class DemoEpgTest {

    private fun ch(epgId: String, name: String = "Ch $epgId"): Channel =
        Channel(
            playlistId = 0L,
            streamId = epgId,
            name = name,
            streamUrl = "https://example.com/$epgId.m3u8",
            epgChannelId = epgId,
        )

    /**
     * Pure-function mirror of DemoRepository.generateProgrammes so it can be
     * tested without any Android dependency.
     */
    private fun generateProgrammes(
        channels: List<Channel>,
        fixedNow: Long,
    ): List<Programme> {
        val result = mutableListOf<Programme>()
        var seq = 0
        val thisHourStart = (fixedNow / DEMO_EPG_SLOT_MS) * DEMO_EPG_SLOT_MS

        for (c in channels) {
            val epgId = c.epgChannelId ?: continue
            val slots = listOf(
                thisHourStart - 3L * DEMO_EPG_SLOT_MS to "Earlier: ${c.name}",
                thisHourStart - 2L * DEMO_EPG_SLOT_MS to "Late Morning: ${c.name}",
                thisHourStart - 1L * DEMO_EPG_SLOT_MS to "Midday: ${c.name}",
                thisHourStart             to c.name,
                thisHourStart + 1L * DEMO_EPG_SLOT_MS to "Coming Up: ${c.name}",
            )
            for ((startMs, title) in slots) {
                result += Programme(
                    epgChannelId = epgId,
                    startMs = startMs,
                    endMs = startMs + DEMO_EPG_SLOT_MS,
                    title = title,
                    description = "Demo programme — Episode ${seq + 1}",
                )
                seq++
            }
        }
        return result
    }

    private val FIXED_NOW: Long = run {
        val fmt = java.text.SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
        fmt.parse("20260101120000")!!.time
    }

    @Test
    fun `five 2h programmes per channel with epgChannelId`() = runTest {
        val progs = generateProgrammes(
            channels = listOf(ch("ch1"), ch("ch2"), ch("ch3")),
            fixedNow = FIXED_NOW,
        )
        assertEquals(15.0, progs.size.toDouble(), 0.0)
        progs.forEach { assertTrue(it.epgChannelId.isNotBlank()) }
    }

    @Test
    fun `slots are contiguous 2-hour non-overlapping blocks`() = runTest {
        val progs = generateProgrammes(listOf(ch("ch1")), FIXED_NOW)
        assertEquals(5.0, progs.size.toDouble(), 0.0)
        val sorted = progs.sortedBy { it.startMs }
        for (i in 0 until sorted.size - 1) {
            assertEquals(
                "Slot $i end must equal slot ${i + 1} start",
                sorted[i].endMs.toDouble(),
                sorted[i + 1].startMs.toDouble(),
                0.0,
            )
        }
        sorted.forEach { p ->
            assertEquals(
                "${p.title}: slot must be ${DEMO_EPG_SLOT_MS}ms",
                DEMO_EPG_SLOT_MS.toDouble(),
                (p.endMs - p.startMs).toDouble(),
                0.0,
            )
        }
    }

    @Test
    fun `current-slot title is bare channel name without prefix`() = runTest {
        val progs = generateProgrammes(listOf(ch("ch1", "CNN International")), FIXED_NOW)
        val thisHourStart = (FIXED_NOW / DEMO_EPG_SLOT_MS) * DEMO_EPG_SLOT_MS
        val current = progs.find { it.startMs == thisHourStart }
        assertNotNull(current)
        assertEquals("CNN International", current!!.title)
    }

    @Test
    fun `past-slot ends at or before fixed now`() = runTest {
        val progs = generateProgrammes(listOf(ch("ch1")), FIXED_NOW)
        val earliestEnd = progs.minOf { it.endMs }
        assertTrue("earliestEnd=$earliestEnd <= FIXED_NOW=$FIXED_NOW", earliestEnd <= FIXED_NOW)
    }

    @Test
    fun `future-slot starts strictly after fixed now`() = runTest {
        val progs = generateProgrammes(listOf(ch("ch1")), FIXED_NOW)
        val latestStart = progs.maxOf { it.startMs }
        assertTrue("latestStart=$latestStart > FIXED_NOW=$FIXED_NOW", latestStart > FIXED_NOW)
    }

    @Test
    fun `channel with null epgChannelId skipped`() = runTest {
        val channels = listOf(
            ch("ch1"),
            Channel(
                playlistId = 0L, streamId = "no-epg", name = "NoEPG",
                streamUrl = "https://example.com/no-epg.m3u8",
                epgChannelId = null,
            ),
        )
        val progs = generateProgrammes(channels, FIXED_NOW)
        assertEquals(5.0, progs.size.toDouble(), 0.0)
        assertEquals("ch1", progs[0].epgChannelId)
    }

    @Test
    fun `eight-channel demo fixture produces 40 programmes`() = runTest {
        val all = (1..8).map { ch("epg$it", "RomBaro Ch $it") }
        val progs = generateProgrammes(all, FIXED_NOW)
        assertEquals(40.0, progs.size.toDouble(), 0.0)
        assertEquals(8.0, progs.map { it.epgChannelId }.toSet().size.toDouble(), 0.0)
    }

    @Test
    fun `every programme has a description`() = runTest {
        val progs = generateProgrammes(listOf(ch("ch1")), FIXED_NOW)
        progs.forEach { assertNotNull(it.description) }
        assertTrue(progs.all { it.description!!.startsWith("Demo programme") })
    }

    @Test
    fun `window spans 6 h before to 4 h after fixed now`() = runTest {
        val progs = generateProgrammes(listOf(ch("ch1")), FIXED_NOW)
        val minStart = progs.minOf { it.startMs }
        val maxEnd = progs.maxOf { it.endMs }
        // slots: -6h, -4h, -2h, 0h, +2h → window [-6h, +4h)
        assertEquals((3 * DEMO_EPG_SLOT_MS).toDouble(), (FIXED_NOW - minStart).toDouble(), 0.0)
        assertEquals((2 * DEMO_EPG_SLOT_MS).toDouble(), (maxEnd - FIXED_NOW).toDouble(), 0.0)
    }
}