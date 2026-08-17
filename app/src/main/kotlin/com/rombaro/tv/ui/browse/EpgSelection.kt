package com.rombaro.tv.ui.browse

import com.rombaro.tv.domain.Programme

fun selectNowNext(
    programmes: List<Programme>,
    nowMs: Long,
): Pair<Programme?, Programme?> {
    if (programmes.isEmpty()) return null to null

    val sorted = programmes.sortedBy { it.startMs }

    val now = sorted.firstOrNull { it.startMs <= nowMs && it.endMs > nowMs }

    val next = if (now != null) {
        sorted.firstOrNull { it.startMs >= now.endMs }
    } else {
        sorted.firstOrNull { it.startMs > nowMs }
    }

    return now to next
}
