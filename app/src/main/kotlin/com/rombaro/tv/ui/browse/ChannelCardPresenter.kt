package com.rombaro.tv.ui.browse

import android.view.ViewGroup
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import coil.load
import com.rombaro.tv.R
import com.rombaro.tv.domain.Channel

class ChannelCardPresenter : Presenter() {

    private val CARD_W = 320
    private val CARD_H = 180

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val card = ImageCardView(parent.context).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            setMainImageDimensions(CARD_W, CARD_H)
        }
        return ViewHolder(card)
    }

    override fun onBindViewHolder(holder: ViewHolder, item: Any?) {
        val ch = item as? Channel ?: return
        val card = holder.view as ImageCardView
        card.titleText = ch.name
        card.contentText = ch.category.orEmpty()
        card.mainImageView.load(ch.logoUrl) {
            placeholder(R.drawable.ic_channel_placeholder)
            error(R.drawable.ic_channel_placeholder)
        }
    }

    override fun onUnbindViewHolder(holder: ViewHolder) {
        (holder.view as ImageCardView).mainImage = null
    }
}
