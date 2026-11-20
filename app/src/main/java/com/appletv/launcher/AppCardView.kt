package com.appletv.launcher

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.graphics.ColorUtils

/** Carte d'application 16:9 avec coins arrondis, reflet et agrandissement fluide au focus. */
class AppCardView(context: Context, private val s: Scale) : FrameLayout(context) {

    private val image = ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
    private val icon = ImageView(context).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
    private val shine = View(context)
    private val interpolator = PathInterpolator(0.22f, 1f, 0.36f, 1f)

    init {
        isFocusable = true
        isClickable = true
        isLongClickable = true
        setDefaultFocusHighlightEnabled(false)
        roundCorners(s.f(18))
        elevation = s.f(6)

        addView(image, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        val iconSize = s.px(CARD_H * 0.62f)
        addView(icon, FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER))

        shine.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(0x44FFFFFF.toInt(), 0x00FFFFFF.toInt())
        )
        shine.alpha = 0f
        addView(shine, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
    }

    private val cardWidth = s.px(CARD_W)
    private val cardHeight = s.px(CARD_H)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(
            View.MeasureSpec.makeMeasureSpec(cardWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(cardHeight, View.MeasureSpec.EXACTLY)
        )
    }

    fun bind(entry: AppEntry) {
        contentDescription = entry.label
        background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                ColorUtils.blendARGB(entry.accent, Color.WHITE, 0.12f),
                ColorUtils.blendARGB(entry.accent, Color.BLACK, 0.45f)
            )
        )
        val banner = entry.banner
        if (banner != null) {
            image.setImageBitmap(banner)
            image.visibility = View.VISIBLE
            icon.visibility = View.GONE
        } else {
            image.setImageDrawable(null)
            image.visibility = View.GONE
            icon.setImageBitmap(entry.icon)
            icon.visibility = View.VISIBLE
        }
        resetLook()
    }

    fun setFocusedLook(focused: Boolean) {
        val target = if (focused) 1.16f else 1f
        animate().cancel()
        animate()
            .scaleX(target)
            .scaleY(target)
            .translationZ(if (focused) s.f(26) else 0f)
            .setDuration(220)
            .setInterpolator(interpolator)
            .start()
        shine.animate().cancel()
        shine.animate().alpha(if (focused) 1f else 0f).setDuration(220).start()
    }

    private fun resetLook() {
        animate().cancel()
        scaleX = 1f
        scaleY = 1f
        translationZ = 0f
        shine.animate().cancel()
        shine.alpha = 0f
    }
}
