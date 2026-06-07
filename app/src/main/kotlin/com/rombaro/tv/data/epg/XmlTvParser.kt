package com.rombaro.tv.data.epg

import android.util.Xml
import com.rombaro.tv.domain.Programme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.GZIPInputStream

/**
 * Streams an XMLTV document and emits Programme rows.
 * Handles gzipped input transparently (auto-detects magic bytes).
 *
 * XMLTV time format: "20240615103000 +0000" (YYYYMMDDhhmmss ±zzzz)
 */
object XmlTvParser {

    private val FMT = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US)

    suspend fun parse(input: InputStream): List<Programme> = withContext(Dispatchers.IO) {
        val pushback = java.io.PushbackInputStream(input, 2)
        val b1 = pushback.read()
        val b2 = pushback.read()
        if (b1 != -1) pushback.unread(b2)
        if (b1 != -1 && b2 != -1) pushback.unread(b1)
        val isGzip = b1 == 0x1f && b2 == 0x8b
        val stream = if (isGzip) GZIPInputStream(pushback) else pushback

        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(stream, null)
        }

        val out = ArrayList<Programme>(8_000)
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "programme") {
                val start = parser.getAttributeValue(null, "start").orEmpty()
                val stop = parser.getAttributeValue(null, "stop").orEmpty()
                val channel = parser.getAttributeValue(null, "channel").orEmpty()
                var title = ""
                var desc: String? = null

                // walk children until </programme>
                var depth = 1
                while (depth > 0) {
                    val e = parser.next()
                    when (e) {
                        XmlPullParser.START_TAG -> {
                            when (parser.name) {
                                "title" -> title = parser.nextText().orEmpty()
                                "desc" -> desc = parser.nextText()
                                else -> { /* skip */ }
                            }
                        }
                        XmlPullParser.END_TAG -> if (parser.name == "programme") depth--
                        XmlPullParser.END_DOCUMENT -> depth = 0
                    }
                }

                val startMs = parseXmltvTime(start)
                val endMs = parseXmltvTime(stop)
                if (startMs != null && endMs != null && channel.isNotEmpty()) {
                    out += Programme(
                        epgChannelId = channel,
                        startMs = startMs,
                        endMs = endMs,
                        title = title.ifBlank { "—" },
                        description = desc?.takeIf { it.isNotBlank() },
                    )
                }
            }
            event = parser.next()
        }
        out
    }

    private fun parseXmltvTime(s: String): Long? = runCatching {
        // pad timezone if missing
        val normalized = if (s.length == 14) "$s +0000" else s
        FMT.parse(normalized)?.time
    }.getOrNull()
}
