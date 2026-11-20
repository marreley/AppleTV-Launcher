package com.appletv.launcher

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils

/**
 * Grand espace supérieur : visuel plein cadre (affiche de film/série ou bannière d'appli),
 * dégradés sombres, titre, description, et fond d'ambiance teinté par l'appli sélectionnée.
 */
class HeroView(context: Context, private val s: Scale) : FrameLayout(context) {

    private val base = 0xFF080A10.toInt()
    private var ambient = base
    private var ambientAnimator: ValueAnimator? = null

    private val backdrop = CrossfadeImageView(context)
    private val bottomScrim = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        intArrayOf(base, base, base, base)
    )

    private val textGroup = LinearLayout(context)
    private val badge = TextView(context)
    private val title = TextView(context)
    private val meta = TextView(context)
    private val overview = TextView(context)
    private val card = ImageView(context)

    init {
        clipChildren = false
        clipToPadding = false
        val heroHeight = s.px(800)

        addView(backdrop, FrameLayout.LayoutParams(MATCH_PARENT, heroHeight))

        val scrimBottomView = View(context)
        scrimBottomView.background = bottomScrim
        addView(scrimBottomView, FrameLayout.LayoutParams(MATCH_PARENT, heroHeight))

        val scrimLeftView = View(context)
        scrimLeftView.background = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(0xEB080A10.toInt(), 0x99080A10.toInt(), 0x00080A10)
        )
        addView(scrimLeftView, FrameLayout.LayoutParams(s.px(1300), heroHeight))

        // Textes
        textGroup.orientation = LinearLayout.VERTICAL

        badge.apply {
            s.text(this, 22)
            setTextColor(0x99FFFFFF.toInt())
            letterSpacing = 0.18f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        title.apply {
            s.text(this, 78)
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            setShadowLayer(s.f(12), 0f, s.f(2), 0x80000000.toInt())
        }
        meta.apply {
            s.text(this, 26)
            setTextColor(0xCCFFFFFF.toInt())
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            maxLines = 1
        }
        overview.apply {
            s.text(this, 24)
            setTextColor(0xB3FFFFFF.toInt())
            setLineSpacing(0f, 1.2f)
            maxLines = 3
            ellipsize = TextUtils.TruncateAt.END
        }

        textGroup.addView(badge, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        textGroup.addView(
            title,
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = s.px(14) }
        )
        textGroup.addView(
            meta,
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = s.px(18) }
        )
        textGroup.addView(
            overview,
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = s.px(22) }
        )
        addView(
            textGroup,
            FrameLayout.LayoutParams(s.px(900), WRAP_CONTENT).apply {
                leftMargin = s.px(96)
                topMargin = s.px(230)
            }
        )

        // Affiche (portrait) ou bannière/icône de l'appli
        card.apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            roundCorners(s.f(24))
            elevation = s.f(30)
            visibility = View.GONE
        }
        addView(
            card,
            FrameLayout.LayoutParams(s.px(300), s.px(450), Gravity.TOP or Gravity.END).apply {
                topMargin = s.px(150)
                marginEnd = s.px(160)
            }
        )

        applyAmbient(base)
        changeText("AppleTV Launcher", "Bienvenue", "", "")
    }

    /** Affiche un film ou une série à la une. */
    fun showFeatured(item: Featured, backdropBitmap: Bitmap?, posterBitmap: Bitmap?) {
        backdrop.show(backdropBitmap)
        changeText("À LA UNE", item.title, item.subtitle, item.overview)
        setCard(s.px(300), s.px(450), posterBitmap, ImageView.ScaleType.CENTER_CROP, 0, Color.BLACK)
    }

    /** Mode sans affiches : met en avant l'application sélectionnée. */
    fun showApp(app: AppEntry) {
        backdrop.show(null)
        changeText(
            "APPLICATION",
            app.label,
            "Installée sur cette TV",
            "Appuyez sur OK pour ouvrir.\nMaintenez OK pour afficher les informations de l'application."
        )
        val banner = app.banner
        if (banner != null) {
            setCard(s.px(560), s.px(315), banner, ImageView.ScaleType.CENTER_CROP, 0, app.accent)
        } else {
            val pad = s.px(40)
            setCard(s.px(300), s.px(300), app.icon, ImageView.ScaleType.FIT_CENTER, pad, app.accent)
        }
    }

    /** Teinte douce du fond, animée, selon l'appli sélectionnée. */
    fun setAccent(accent: Int) {
        val target = ColorUtils.blendARGB(base, accent, 0.30f)
        val from = ambient
        ambientAnimator?.cancel()
        ambientAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 450L
            addUpdateListener { animator ->
                applyAmbient(ColorUtils.blendARGB(from, target, animator.animatedFraction))
            }
            start()
        }
    }

    private fun applyAmbient(color: Int) {
        ambient = color
        setBackgroundColor(color)
        bottomScrim.setColors(
            intArrayOf(
                withAlpha(base, 0x66),
                withAlpha(base, 0x00),
                withAlpha(color, 0xB8),
                withAlpha(color, 0xFF)
            )
        )
    }

    private fun withAlpha(color: Int, alpha: Int): Int = (color and 0x00FFFFFF) or (alpha shl 24)

    private fun changeText(badgeText: String, titleText: String, metaText: String, overviewText: String) {
        badge.text = badgeText
        title.text = titleText
        meta.text = metaText
        meta.visibility = if (metaText.isEmpty()) View.GONE else View.VISIBLE
        overview.text = overviewText
        overview.visibility = if (overviewText.isEmpty()) View.GONE else View.VISIBLE

        textGroup.animate().cancel()
        textGroup.alpha = 0f
        textGroup.translationY = s.f(18)
        textGroup.animate().alpha(1f).translationY(0f).setDuration(380L).start()
    }

    private fun setCard(
        width: Int,
        height: Int,
        bitmap: Bitmap?,
        scaleType: ImageView.ScaleType,
        padding: Int,
        background: Int
    ) {
        if (bitmap == null) {
            card.visibility = View.GONE
            return
        }
        val lp = card.layoutParams as FrameLayout.LayoutParams
        lp.width = width
        lp.height = height
        card.layoutParams = lp
        card.scaleType = scaleType
        card.setPadding(padding, padding, padding, padding)
        card.setBackgroundColor(background)
        card.setImageBitmap(bitmap)
        card.visibility = View.VISIBLE
        card.animate().cancel()
        card.alpha = 0f
        card.animate().alpha(1f).setDuration(450L).start()
    }
}
