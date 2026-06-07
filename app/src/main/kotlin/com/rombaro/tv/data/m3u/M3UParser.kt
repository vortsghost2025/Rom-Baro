package com.rombaro.tv.data.m3u

import com.rombaro.tv.domain.Channel
import java.io.BufferedReader

/**
 * Parses extended M3U / M3U8 playlists (the de-facto IPTV format).
 *
 * Example input:
 *   #EXTM3U
 *   #EXTINF:-1 tvg-id="cnn.us" tvg-logo="https://.../cnn.png" group-title="News",CNN HD
 *   http://provider.tld/live/user/pass/12345.ts
 *
 * Robust to:
 *   - Missing attributes
 *   - Comments / blank lines
 *   - Quoted values that contain commas
 *   - Unix or Windows line endings
 */
object M3UParser {

    private val attrRegex = Regex("""([a-zA-Z0-9_-]+)="([^"]*)"""")

    fun parse(reader: BufferedReader, playlistId: Long): List<Channel> {
        val out = mutableListOf<Channel>()
        var pending: PendingExtinf? = null
        var order = 0

        reader.useLines { lines ->
            for (raw in lines) {
                val line = raw.trim()
                when {
                    line.isEmpty() -> Unit
                    line.startsWith("#EXTM3U", ignoreCase = true) -> Unit
                    line.startsWith("#EXTINF", ignoreCase = true) -> {
                        pending = parseExtinf(line)
                    }
                    line.startsWith("#") -> Unit // ignore other directives for now
                    else -> {
                        val p = pending
                        if (p != null) {
                            out += Channel(
                                playlistId = playlistId,
                                streamId = p.tvgId.ifBlank { line.hashCode().toString() },
                                name = p.title,
                                streamUrl = line,
                                logoUrl = p.tvgLogo.ifBlank { null },
                                category = p.groupTitle.ifBlank { null },
                                epgChannelId = p.tvgId.ifBlank { null },
                                orderHint = order++,
                            )
                            pending = null
                        }
                    }
                }
            }
        }
        return out
    }

    private fun parseExtinf(line: String): PendingExtinf {
        // EXTINF:-1 attr="val" attr="val",Display Name
        val commaIdx = line.indexOf(',')
        val attrSection = if (commaIdx >= 0) line.substring(0, commaIdx) else line
        val title = if (commaIdx >= 0) line.substring(commaIdx + 1).trim() else "Unnamed"

        val attrs = attrRegex.findAll(attrSection)
            .associate { it.groupValues[1].lowercase() to it.groupValues[2] }

        return PendingExtinf(
            title = title,
            tvgId = attrs["tvg-id"].orEmpty(),
            tvgLogo = attrs["tvg-logo"].orEmpty(),
            groupTitle = attrs["group-title"].orEmpty(),
        )
    }

    private data class PendingExtinf(
        val title: String,
        val tvgId: String,
        val tvgLogo: String,
        val groupTitle: String,
    )
}
