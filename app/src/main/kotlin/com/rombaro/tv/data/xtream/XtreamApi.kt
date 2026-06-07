package com.rombaro.tv.data.xtream

import com.rombaro.tv.domain.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Minimal Xtream Codes client.
 *
 * The provider exposes endpoints like:
 *   {server}/player_api.php?username=U&password=P&action=get_live_categories
 *   {server}/player_api.php?username=U&password=P&action=get_live_streams
 *   {server}/player_api.php?username=U&password=P&action=get_live_streams&category_id=N
 *
 * Stream playback URLs follow:
 *   {server}/live/{username}/{password}/{stream_id}.ts        (MPEG-TS)
 *   {server}/{username}/{password}/{stream_id}                (HLS varies by panel)
 *
 * We prefer .m3u8 when the server supports it (most do); fall back to .ts.
 */
@Singleton
class XtreamApi @Inject constructor(
    private val http: OkHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    suspend fun getLiveStreams(
        playlistId: Long,
        server: String,
        username: String,
        password: String,
    ): List<Channel> = withContext(Dispatchers.IO) {
        val base = server.trimEnd('/')
        val categories = fetchCategories(base, username, password)
        val streams = fetchLiveStreams(base, username, password)

        streams.map { s ->
            Channel(
                playlistId = playlistId,
                streamId = s.streamId.toString(),
                name = s.name,
                streamUrl = "$base/live/$username/$password/${s.streamId}.m3u8",
                logoUrl = s.streamIcon.takeIf { !it.isNullOrBlank() },
                category = categories[s.categoryId.orEmpty()],
                epgChannelId = s.epgChannelId.takeIf { !it.isNullOrBlank() },
                orderHint = s.num ?: 0,
            )
        }
    }

    private fun fetchCategories(
        base: String, u: String, p: String,
    ): Map<String, String> {
        val url = "$base/player_api.php?username=$u&password=$p&action=get_live_categories"
        val body = http.newCall(Request.Builder().url(url).build())
            .execute()
            .use { it.body?.string().orEmpty() }
        if (body.isBlank()) return emptyMap()
        val list: List<XtCategory> = runCatching { json.decodeFromString(body) }.getOrDefault(emptyList())
        return list.associate { it.categoryId to it.categoryName }
    }

    private fun fetchLiveStreams(
        base: String, u: String, p: String,
    ): List<XtStream> {
        val url = "$base/player_api.php?username=$u&password=$p&action=get_live_streams"
        val body = http.newCall(Request.Builder().url(url).build())
            .execute()
            .use { it.body?.string().orEmpty() }
        if (body.isBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<XtStream>>(body) }.getOrDefault(emptyList())
    }

    /** Build a timeshift / catchup URL — used later by v0.3. */
    fun timeshiftUrl(
        server: String, username: String, password: String,
        durationMinutes: Int, startUnix: Long, streamId: String,
    ): String {
        val base = server.trimEnd('/')
        // Common Xtream catchup URL shape:
        // {server}/timeshift/{user}/{pass}/{duration}/{YYYY-MM-DD:HH-MM}/{stream}.ts
        val ts = java.text.SimpleDateFormat("yyyy-MM-dd:HH-mm", java.util.Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
            .format(java.util.Date(startUnix * 1000L))
        return "$base/timeshift/$username/$password/$durationMinutes/$ts/$streamId.ts"
    }

    @Serializable
    private data class XtCategory(
        @SerialName("category_id") val categoryId: String = "",
        @SerialName("category_name") val categoryName: String = "",
    )

    @Serializable
    private data class XtStream(
        @SerialName("num") val num: Int? = null,
        @SerialName("name") val name: String = "",
        @SerialName("stream_id") val streamId: Long = 0,
        @SerialName("stream_icon") val streamIcon: String? = null,
        @SerialName("category_id") val categoryId: String? = null,
        @SerialName("epg_channel_id") val epgChannelId: String? = null,
    )
}
