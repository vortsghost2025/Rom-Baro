package com.rombaro.tv.ui.browse

import android.content.Intent
import android.os.Bundle
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.lifecycle.lifecycleScope
import com.rombaro.tv.R
import com.rombaro.tv.data.repo.PlaylistRepository
import com.rombaro.tv.domain.Channel
import com.rombaro.tv.player.PlayerActivity
import com.rombaro.tv.ui.settings.PlaylistSetupActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainBrowseFragment : BrowseSupportFragment() {

    @Inject lateinit var playlistRepo: PlaylistRepository

    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title = getString(R.string.app_name)
        headersState = HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true
        adapter = rowsAdapter

        setOnSearchClickedListener {
            // TODO v0.2: search across channels + EPG
        }

        setOnItemViewClickedListener { _, item, _, _ ->
            if (item is Channel) {
                startActivity(
                    Intent(requireContext(), PlayerActivity::class.java).apply {
                        putExtra(PlayerActivity.EXTRA_STREAM_URL, item.streamUrl)
                        putExtra(PlayerActivity.EXTRA_TITLE, item.name)
                    }
                )
            }
        }

        loadRows()
    }

    private fun loadRows() {
        viewLifecycleOwner.lifecycleScope.launch {
            playlistRepo.observePlaylists().collectLatest { playlists ->
                rowsAdapter.clear()
                if (playlists.isEmpty()) {
                    // First-run nudge: send to setup
                    startActivity(Intent(requireContext(), PlaylistSetupActivity::class.java))
                    return@collectLatest
                }
                val first = playlists.first()
                playlistRepo.observeCategories(first.id).collectLatest { cats ->
                    rowsAdapter.clear()
                    val displayCats = if (cats.isEmpty()) listOf<String?>(null) else cats
                    displayCats.forEachIndexed { idx, cat ->
                        val header = HeaderItem(idx.toLong(), cat ?: getString(R.string.row_all_channels))
                        val rowAdapter = ArrayObjectAdapter(ChannelCardPresenter())
                        rowsAdapter.add(ListRow(header, rowAdapter))
                        viewLifecycleOwner.lifecycleScope.launch {
                            playlistRepo.observeByCategory(first.id, cat).collectLatest { channels ->
                                rowAdapter.clear()
                                rowAdapter.addAll(0, channels)
                            }
                        }
                    }
                }
            }
        }
    }
}
