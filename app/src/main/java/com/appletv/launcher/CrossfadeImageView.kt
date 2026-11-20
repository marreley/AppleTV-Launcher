package com.appletv.launcher

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import android.widget.ImageView

/** Deux ImageView superposées : fondu enchaîné + très léger zoom lent (effet « Ken Burns »). */
class CrossfadeImageView(context: Context) : FrameLayout(context) {

    private val first = createImage(context)
    private val second = createImage(context)
    private var firstOnTop = false
    private var zoom: ObjectAnimator? = null

    init {
        addView(first, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        addView(second, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
    }

    fun show(bitmap: Bitmap?, durationMs: Long = 700L) {
        val incoming = if (firstOnTop) second else first
        val outgoing = if (firstOnTop) first else second
        firstOnTop = !firstOnTop

        zoom?.cancel()
        incoming.animate().cancel()
        outgoing.animate().cancel()

        bringChildToFront(incoming)
        incoming.setImageBitmap(bitmap)
        incoming.scaleX = 1f
        incoming.scaleY = 1f

        if (bitmap != null) {
            incoming.alpha = 0f
            incoming.animate().alpha(1f).setDuration(durationMs).start()
            zoom = ObjectAnimator.ofPropertyValuesHolder(
                incoming,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.06f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.06f)
            ).apply {
                duration = 11_000L
                start()
            }
        } else {
            incoming.alpha = 0f
        }
        outgoing.animate().alpha(0f).setDuration(durationMs).start()
    }

    private fun createImage(context: Context) = ImageView(context).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        alpha = 0f
    }
}
