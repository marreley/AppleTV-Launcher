package com.appletv.launcher

import android.content.Context
import android.graphics.Outline
import android.util.TypedValue
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.TextView

/**
 * Toutes les dimensions de l'interface sont exprimées en « pixels de design » pour un écran de
 * 1920 px de large. Sur une TV 4K (3840 px) tout est simplement doublé : la mise en page reste
 * identique quelle que soit la résolution ou la densité renvoyée par le système.
 */
class Scale(context: Context) {
    private val factor = (context.resources.displayMetrics.widthPixels / 1920f).coerceAtLeast(0.5f)

    fun px(designPx: Number): Int = (designPx.toFloat() * factor).toInt()

    fun f(designPx: Number): Float = designPx.toFloat() * factor

    fun text(view: TextView, designPx: Number) {
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, f(designPx))
    }
}

/** Coins arrondis rapides (découpe GPU, sans surcoût pendant les animations). */
fun View.roundCorners(radius: Float) {
    outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(0, 0, view.width, view.height, radius)
        }
    }
    clipToOutline = true
}
