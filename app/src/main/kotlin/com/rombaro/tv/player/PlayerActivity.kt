package com.rombaro.tv.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.rombaro.tv.R
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.OkHttpClient
import javax.inject.Inject

/**
 * Fullscreen ExoPlayer host.
 *
 * Passed in via Intent extras:
 *   EXTRA_STREAM_URL — HLS / DASH / MPEG-TS / progressive
 *   EXTRA_TITLE      — display name
 *
 * Supports HLS and MPEG-TS out of the box via media3-exoplayer + media3-exoplayer-hls.
 * For DASH add the corresponding source factory; already included as a dep.
 */
@AndroidEntryPoint
class PlayerActivity : ComponentActivity() {

    @Inject lateinit var http: OkHttpClient

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        playerView = findViewById(R.id.player_view)
    }

    override fun onStart() {
        super.onStart()
        val streamUrl = intent.getStringExtra(EXTRA_STREAM_URL) ?: run { finish(); return }

        val httpFactory = OkHttpDataSource.Factory(http)
            .setUserAgent("RomBaro/0.1 (Android)")

        val mediaSourceFactory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(DefaultDataSource.Factory(this, httpFactory))

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .also { p ->
                playerView.player = p
                p.setMediaItem(MediaItem.fromUri(streamUrl))
                p.playWhenReady = true
                p.prepare()
                p.addListener(object : Player.Listener {
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        // TODO: surface a toast / retry UI
                    }
                })
            }
    }

    override fun onStop() {
        super.onStop()
        player?.release()
        player = null
    }

    companion object {
        const val EXTRA_STREAM_URL = "stream_url"
        const val EXTRA_TITLE = "title"
    }
}
