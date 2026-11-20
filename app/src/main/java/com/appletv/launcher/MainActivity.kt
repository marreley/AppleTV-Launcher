package com.appletv.launcher

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.leanback.widget.HorizontalGridView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var scale: Scale
    private lateinit var hero: HeroView
    private lateinit var row: HorizontalGridView
    private lateinit var appLabel: TextView
    private lateinit var emptyView: TextView
    private lateinit var adapter: AppAdapter

    private val handler = Handler(Looper.getMainLooper())
    private var pendingHeroUpdate: Runnable? = null
    private var refreshJob: Job? = null
    private var featured: List<Featured> = emptyList()
    private var featuredIndex = 0
    private var receiverRegistered = false

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshApps()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scale = Scale(this)
        window.setBackgroundDrawable(ColorDrawable(0xFF080A10.toInt()))

        adapter = AppAdapter(scale, ::onAppFocused, ::launchApp, ::openAppInfo)
        setContentView(buildUi())

        // Retour : revient d'abord au premier élément, puis quitte vers l'accueil d'origine.
        onBackPressedDispatcher.addCallback(this) {
            if (row.selectedPosition > 0) {
                row.setSelectedPositionSmooth(0)
            } else {
                finish()
            }
        }

        startHeroLoop()
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(this, packageReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        receiverRegistered = true
        refreshApps()
    }

    override fun onStop() {
        if (receiverRegistered) {
            unregisterReceiver(packageReceiver)
            receiverRegistered = false
        }
        super.onStop()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun buildUi(): View {
        val root = FrameLayout(this)
        root.clipChildren = false
        root.clipToPadding = false

        hero = HeroView(this, scale)
        root.addView(hero, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        // Heure et date, en haut à droite
        val clock = TextClock(this).apply {
            format24Hour = "HH:mm"
            format12Hour = "HH:mm"
            setTextColor(Color.WHITE)
            scale.text(this, 40)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.END
        }
        val date = TextClock(this).apply {
            format24Hour = "EEEE d MMMM"
            format12Hour = "EEEE d MMMM"
            setTextColor(0xB3FFFFFF.toInt())
            scale.text(this, 22)
            gravity = Gravity.END
        }
        val topRight = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
            addView(clock, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
            addView(date, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        }
        root.addView(
            topRight,
            FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.TOP or Gravity.END).apply {
                topMargin = scale.px(48)
                marginEnd = scale.px(96)
            }
        )

        // Rangée d'icônes d'applications
        row = HorizontalGridView(this).apply {
            adapter = this@MainActivity.adapter
            itemAnimator = null
            setHasFixedSize(true)
            setItemSpacing(scale.px(36))
            clipChildren = false
            clipToPadding = false
            setPadding(scale.px(96), scale.px(60), scale.px(96), scale.px(60))
        }
        root.addView(
            row,
            FrameLayout.LayoutParams(MATCH_PARENT, scale.px(300), Gravity.BOTTOM).apply {
                bottomMargin = scale.px(110)
            }
        )

        // Nom de l'application sélectionnée
        appLabel = TextView(this).apply {
            scale.text(this, 30)
            setTextColor(0xE6FFFFFF.toInt())
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            maxLines = 1
        }
        root.addView(
            appLabel,
            FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.BOTTOM or Gravity.START).apply {
                leftMargin = scale.px(96)
                bottomMargin = scale.px(58)
            }
        )

        emptyView = TextView(this).apply {
            text = "Aucune application détectée"
            scale.text(this, 30)
            setTextColor(0x99FFFFFF.toInt())
            visibility = View.GONE
        }
        root.addView(
            emptyView,
            FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
                bottomMargin = scale.px(220)
            }
        )

        return root
    }

    private fun refreshApps() {
        refreshJob?.cancel()
        refreshJob = lifecycleScope.launch {
            val list = AppRepository.load(applicationContext)
            val firstLoad = adapter.itemCount == 0
            adapter.submitList(list) {
                emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                if (firstLoad && list.isNotEmpty()) {
                    row.post { row.requestFocus() }
                }
            }
        }
    }

    /** Appelé à chaque changement de sélection dans la rangée. */
    private fun onAppFocused(app: AppEntry) {
        appLabel.text = app.label
        // Petite temporisation : évite de relancer les animations quand on parcourt vite la rangée.
        pendingHeroUpdate?.let { handler.removeCallbacks(it) }
        val update = Runnable {
            hero.setAccent(app.accent)
            if (featured.isEmpty()) hero.showApp(app)
        }
        pendingHeroUpdate = update
        handler.postDelayed(update, HERO_DEBOUNCE_MS)
    }

    /** Rotation des affiches à la une (si une clé TMDB est configurée). */
    private fun startHeroLoop() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                if (featured.isEmpty()) featured = FeaturedRepository.fetch()
                if (featured.isEmpty()) return@repeatOnLifecycle

                while (isActive) {
                    val item = featured[featuredIndex % featured.size]
                    val backdrop = item.backdropUrl?.let { ImageLoader.load(it) }
                    val poster = item.posterUrl?.let { ImageLoader.load(it) }
                    hero.showFeatured(item, backdrop, poster)

                    // Précharge la suivante pour un enchaînement sans attente.
                    val next = featured[(featuredIndex + 1) % featured.size]
                    launch {
                        next.backdropUrl?.let { ImageLoader.load(it) }
                        next.posterUrl?.let { ImageLoader.load(it) }
                    }

                    delay(HERO_INTERVAL_MS)
                    featuredIndex++
                }
            }
        }
    }

    private fun launchApp(app: AppEntry, view: View) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            component = app.component
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }
        try {
            val options = ActivityOptionsCompat.makeScaleUpAnimation(view, 0, 0, view.width, view.height)
            startActivity(intent, options.toBundle())
        } catch (e: ActivityNotFoundException) {
            toast("Impossible d'ouvrir ${app.label}")
        } catch (e: SecurityException) {
            toast("Impossible d'ouvrir ${app.label}")
        }
    }

    private fun openAppInfo(app: AppEntry) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${app.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            toast("Informations indisponibles pour ${app.label}")
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private companion object {
        const val HERO_INTERVAL_MS = 9_000L
        const val HERO_DEBOUNCE_MS = 140L
    }
}
