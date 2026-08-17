package com.rombaro.tv.ui.browse

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.leanback.widget.Presenter
import coil.load
import com.rombaro.tv.R
import com.rombaro.tv.domain.ChannelWithNow

fun epgCardText(cwn: ChannelWithNow): String =
    when {
        cwn.now != null && cwn.next != null ->
            "Now: ${cwn.now.title}\nNext: ${cwn.next.title}"
        cwn.now != null ->
            "Now: ${cwn.now.title}"
        cwn.next != null ->
            "Next: ${cwn.next.title}"
        else ->
            cwn.channel.category.orEmpty()
    }

class ChannelCardPresenter(
    private val onLongClick: ((ChannelWithNow) -> Unit)? = null,
) : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, item: Any?) {
        val cwn = item as? ChannelWithNow ?: return
        val title = holder.view.findViewById<TextView>(R.id.card_title)
        val content = holder.view.findViewById<TextView>(R.id.card_content)
        val image = holder.view.findViewById<ImageView>(R.id.card_image)
        title.text = buildString {
            append(cwn.channel.name)
            if (cwn.isFavorite) append(" \u2605")
        }
        content.text = epgCardText(cwn)
        image.load(cwn.channel.logoUrl) {
            placeholder(R.drawable.ic_channel_placeholder)
            error(R.drawable.ic_channel_placeholder)
        }
        holder.view.setOnLongClickListener {
            onLongClick?.invoke(cwn)
            true
        }
    }

    override fun onUnbindViewHolder(holder: ViewHolder) {
        holder.view.findViewById<ImageView>(R.id.card_image).setImageDrawable(null)
        holder.view.setOnLongClickListener(null)
    }
}
