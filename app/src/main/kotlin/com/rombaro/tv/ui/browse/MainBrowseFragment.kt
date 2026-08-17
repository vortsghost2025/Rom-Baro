package com.rombaro.tv.ui.browse

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.lifecycle.lifecycleScope
import com.rombaro.tv.R
import com.rombaro.tv.data.repo.EpgRefreshCoordinator
import com.rombaro.tv.data.repo.FavoritesRepository
import com.rombaro.tv.data.repo.PlaylistRepository
import com.rombaro.tv.domain.ChannelWithNow
import com.rombaro.tv.player.PlayerActivity
import com.rombaro.tv.ui.settings.PlaylistSetupActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainBrowseFragment : BrowseSupportFragment() {

    @Inject lateinit var playlistRepo: PlaylistRepository
    @Inject lateinit var favoritesRepo: FavoritesRepository
    @Inject lateinit var epgRefresh: EpgRefreshCoordinator

    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
    private var lastRenderedState: BrowseState? = null
    private var currentPlaylistId: Long = 0L

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
            if (item is ChannelWithNow) {
                startActivity(
                    Intent(requireContext(), PlayerActivity::class.java).apply {
                        putExtra(PlayerActivity.EXTRA_STREAM_URL, item.channel.streamUrl)
                        putExtra(PlayerActivity.EXTRA_TITLE, item.channel.name)
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
                    startActivity(Intent(requireContext(), PlaylistSetupActivity::class.java))
                    return@collectLatest
                }
                currentPlaylistId = playlists.first().id

                playlists.forEach { playlist ->
                    if (!playlist.xmltvUrl.isNullOrBlank()) {
                        epgRefresh.refreshIfEligible(playlist.id)
                    }
                }

                combine(
                    playlistRepo.observeCategories(currentPlaylistId),
                    playlistRepo.observeChannelsWithNow(currentPlaylistId),
                    favoritesRepo.observe(currentPlaylistId),
                ) { categories, channelsWithNow, favorites ->
                    val favIds = favorites.mapTo(mutableSetOf()) { it.streamId }
                    val marked = channelsWithNow.map { cwn ->
                        if (cwn.channel.streamId in favIds) cwn.copy(isFavorite = true) else cwn
                    }
                    buildBrowseState(categories, marked)
                }.collectLatest { state ->
                    renderBrowseState(state)
                }
            }
        }
    }

    private fun renderBrowseState(state: BrowseState) {
        if (state == lastRenderedState) return
        lastRenderedState = state
        rowsAdapter.clear()
        var idx = 0L
        if (state.favoriteChannels.isNotEmpty()) {
            val favAdapter = ArrayObjectAdapter(ChannelCardPresenter { cwn -> toggleFavorite(cwn) })
            favAdapter.addAll(0, state.favoriteChannels)
            rowsAdapter.add(ListRow(HeaderItem(idx++, getString(R.string.row_favorites)), favAdapter))
        }
        state.categoryRows.forEach { (cat, channels) ->
            val header = HeaderItem(idx++, cat ?: getString(R.string.row_all_channels))
            val rowAdapter = ArrayObjectAdapter(ChannelCardPresenter { cwn -> toggleFavorite(cwn) })
            rowAdapter.addAll(0, channels)
            rowsAdapter.add(ListRow(header, rowAdapter))
        }
    }

    private fun toggleFavorite(cwn: ChannelWithNow) {
        viewLifecycleOwner.lifecycleScope.launch {
            val isNowFav = favoritesRepo.toggle(currentPlaylistId, cwn.channel.streamId)
            val msg = if (isNowFav) "★ Favorited: ${cwn.channel.name}" else "☆ Unfavorited: ${cwn.channel.name}"
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }
}
