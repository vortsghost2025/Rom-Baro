package com.rombaro.tv.ui.browse

import com.rombaro.tv.domain.Programme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EpgSelectionTest {

    private fun p(startMs: Long, endMs: Long, title: String = "Show") = Programme(
        id = 0L, epgChannelId = "ch1", startMs = startMs, endMs = endMs,
        title = title, description = null,
    )

    @Test
    fun emptyList_returnsNull() {
        val (now, next) = selectNowNext(emptyList(), nowMs = 1000L)
        assertNull(now)
        assertNull(next)
    }

    @Test
    fun singleProgramme_isCurrent() {
        val prog = p(1000L, 2000L, "A")
        val (now, next) = selectNowNext(listOf(prog), nowMs = 1500L)
        assertEquals("A", now?.title)
        assertNull(next)
    }

    @Test
    fun singleProgramme_notYetStarted_returnsNull() {
        val prog = p(2000L, 3000L, "A")
        val (now, next) = selectNowNext(listOf(prog), nowMs = 1000L)
        assertNull(now)
        assertEquals("A", next?.title)
    }

    @Test
    fun singleProgramme_alreadyEnded_returnsNull() {
        val prog = p(1000L, 2000L, "A")
        val (now, next) = selectNowNext(listOf(prog), nowMs = 3000L)
        assertNull(now)
        assertNull(next)
    }

    @Test
    fun twoProgrammes_firstIsCurrent_secondIsNext() {
        val a = p(1000L, 2000L, "A")
        val b = p(2000L, 3000L, "B")
        val (now, next) = selectNowNext(listOf(b, a), nowMs = 1500L)
        assertEquals("A", now?.title)
        assertEquals("B", next?.title)
    }

    @Test
    fun twoProgrammes_firstEnded_secondIsCurrent() {
        val a = p(1000L, 2000L, "A")
        val b = p(2000L, 3000L, "B")
        val (now, next) = selectNowNext(listOf(a, b), nowMs = 2500L)
        assertEquals("B", now?.title)
        assertNull(next)
    }

    @Test
    fun threeProgrammes_middleIsCurrent_lastIsNext() {
        val a = p(1000L, 2000L, "A")
        val b = p(2000L, 3000L, "B")
        val c = p(3000L, 4000L, "C")
        val (now, next) = selectNowNext(listOf(c, a, b), nowMs = 2500L)
        assertEquals("B", now?.title)
        assertEquals("C", next?.title)
    }

    @Test
    fun noCurrentProgramme_findsEarliestFuture() {
        val a = p(1000L, 2000L, "A")
        val b = p(3000L, 4000L, "B")
        val c = p(5000L, 6000L, "C")
        val (now, next) = selectNowNext(listOf(c, a, b), nowMs = 2500L)
        assertNull(now)
        assertEquals("B", next?.title)
    }

    @Test
    fun gapBetweenProgrammes_noCurrent_findsNextAfterGap() {
        val a = p(1000L, 2000L, "A")
        val b = p(4000L, 5000L, "B")
        val (now, next) = selectNowNext(listOf(a, b), nowMs = 3000L)
        assertNull(now)
        assertEquals("B", next?.title)
    }

    @Test
    fun boundaryExactEnd_isNotCurrent() {
        val a = p(1000L, 2000L, "A")
        val b = p(2000L, 3000L, "B")
        val (now, next) = selectNowNext(listOf(a, b), nowMs = 2000L)
        assertEquals("B", now?.title)
        assertNull(next)
    }

    @Test
    fun realCase_wmurNowIsCurrent_nextIsNext() {
        // Mirrors the deterministic rows on the OnePlus 9.
        val nowProg = Programme(
            id = 1L, epgChannelId = "WMURTV91.us@HD",
            startMs = 1_000L, endMs = 2_000L, title = "ROM BARO NOW TEST",
        )
        val nextProg = Programme(
            id = 2L, epgChannelId = "WMURTV91.us@HD",
            startMs = 2_000L, endMs = 3_000L, title = "ROM BARO NEXT TEST",
        )
        val (now, next) = selectNowNext(listOf(nowProg, nextProg), nowMs = 1_500L)
        assertEquals("ROM BARO NOW TEST", now?.title)
        assertEquals(1_000L, now?.startMs)
        assertEquals(2_000L, now?.endMs)
        assertEquals("ROM BARO NEXT TEST", next?.title)
        assertEquals(2_000L, next?.startMs)
        assertEquals("WMURTV91.us@HD", now?.epgChannelId)
    }

    @Test
    fun realCase_exactStartBoundary_currentIsActive() {
        val nowProg = Programme(
            id = 1L, epgChannelId = "WMURTV91.us@HD",
            startMs = 1_000L, endMs = 2_000L, title = "ROM BARO NOW TEST",
        )
        val nextProg = Programme(
            id = 2L, epgChannelId = "WMURTV91.us@HD",
            startMs = 2_000L, endMs = 3_000L, title = "ROM BARO NEXT TEST",
        )
        // At the exact start of NOW (published within the same minute), NOW is current.
        val (now, next) = selectNowNext(listOf(nowProg, nextProg), nowMs = 1_000L)
        assertEquals("ROM BARO NOW TEST", now?.title)
        assertEquals("ROM BARO NEXT TEST", next?.title)
    }

    @Test
    fun realCase_exactEndBoundary_nowYieldToNext() {
        val nowProg = Programme(
            id = 1L, epgChannelId = "WMURTV91.us@HD",
            startMs = 1_000L, endMs = 2_000L, title = "ROM BARO NOW TEST",
        )
        val nextProg = Programme(
            id = 2L, epgChannelId = "WMURTV91.us@HD",
            startMs = 2_000L, endMs = 3_000L, title = "ROM BARO NEXT TEST",
        )
        // At the exact end of NOW, NOW is no longer current (endMs is exclusive); NEXT takes over.
        val (now, next) = selectNowNext(listOf(nowProg, nextProg), nowMs = 2_000L)
        assertEquals("ROM BARO NEXT TEST", now?.title)
        assertNull(next)
    }

    @Test
    fun realCase_secondChannelDoesNotAffectFirst() {
        val wmurNow = Programme(
            id = 1L, epgChannelId = "WMURTV91.us@HD",
            startMs = 1_000L, endMs = 2_000L, title = "ROM BARO NOW TEST",
        )
        val wmurNext = Programme(
            id = 2L, epgChannelId = "WMURTV91.us@HD",
            startMs = 2_000L, endMs = 3_000L, title = "ROM BARO NEXT TEST",
        )
        val accuNow = Programme(
            id = 3L, epgChannelId = "AccuWeatherNOW.us@SD",
            startMs = 1_500L, endMs = 2_500L, title = "ACCUWEATHER NOW TEST",
        )
        val accuNext = Programme(
            id = 4L, epgChannelId = "AccuWeatherNOW.us@SD",
            startMs = 2_500L, endMs = 3_500L, title = "ACCUWEATHER NEXT TEST",
        )
        val (wmurNowSel, wmurNextSel) = selectNowNext(listOf(wmurNow, wmurNext), nowMs = 1_500L)
        assertEquals("ROM BARO NOW TEST", wmurNowSel?.title)
        assertEquals("ROM BARO NEXT TEST", wmurNextSel?.title)

        val (accuNowSel, accuNextSel) = selectNowNext(listOf(accuNow, accuNext), nowMs = 1_500L)
        assertEquals("ACCUWEATHER NOW TEST", accuNowSel?.title)
        assertEquals("ACCUWEATHER NEXT TEST", accuNextSel?.title)
    }
}
