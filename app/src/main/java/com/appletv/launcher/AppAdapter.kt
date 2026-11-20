package com.appletv.launcher

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

/** Taille d'une carte, en pixels de design (écran de 1920 px de large). */
const val CARD_W = 264
const val CARD_H = 149

private object AppDiff : DiffUtil.ItemCallback<AppEntry>() {
    override fun areItemsTheSame(oldItem: AppEntry, newItem: AppEntry): Boolean =
        oldItem.key == newItem.key

    // Les entrées inchangées sont réutilisées telles quelles par le dépôt (cache).
    override fun areContentsTheSame(oldItem: AppEntry, newItem: AppEntry): Boolean =
        oldItem === newItem
}

class AppAdapter(
    private val scale: Scale,
    private val onFocused: (AppEntry) -> Unit,
    private val onOpen: (AppEntry, View) -> Unit,
    private val onInfo: (AppEntry) -> Unit
) : ListAdapter<AppEntry, AppAdapter.Holder>(AppDiff) {

    class Holder(val card: AppCardView) : RecyclerView.ViewHolder(card)

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = getItem(position).key.hashCode().toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        // Pas de LayoutParams ici : le GridLayoutManager de leanback crée les siens.
        // La carte impose elle-même sa taille (voir AppCardView.onMeasure).
        return Holder(AppCardView(parent.context, scale))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val entry = getItem(position)
        val card = holder.card
        card.bind(entry)
        card.setOnFocusChangeListener { _, hasFocus ->
            card.setFocusedLook(hasFocus)
            if (hasFocus) onFocused(entry)
        }
        card.setOnClickListener { onOpen(entry, card) }
        card.setOnLongClickListener {
            onInfo(entry)
            true
        }
    }
}
