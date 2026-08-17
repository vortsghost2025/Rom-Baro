package com.rombaro.tv.ui.browse

import com.rombaro.tv.domain.ChannelWithNow

data class BrowseState(
    val favoriteChannels: List<ChannelWithNow>,
    val categoryRows: List<Pair<String?, List<ChannelWithNow>>>,
)

fun buildBrowseState(
    categories: List<String>,
    channels: List<ChannelWithNow>,
): BrowseState {
    val favoriteChannels = channels.filter { it.isFavorite }
    val categoryRows = if (categories.isEmpty()) {
        listOf(null to channels)
    } else {
        categories.map { cat ->
            cat to channels.filter { it.channel.category == cat }
        }
    }
    return BrowseState(favoriteChannels, categoryRows)
}
