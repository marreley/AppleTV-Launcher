package com.appletv.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/** Une application installée, prête à être affichée (visuels déjà rendus en bitmap). */
class AppEntry(
    val key: String,
    val label: String,
    val packageName: String,
    val component: ComponentName,
    val banner: Bitmap?,
    val icon: Bitmap,
    val accent: Int
)

/** Détecte les applications réellement installées sur la TV. */
object AppRepository {

    private const val BANNER_W = 480
    private const val BANNER_H = 270
    private const val ICON_SIZE = 144

    private val defaultAccent = 0xFF2A3350.toInt()

    // Les services de streaming courants passent en premier.
    private val priority = listOf(
        "com.apple.atve.androidtv.appletv",
        "com.netflix.ninja",
        "com.google.android.youtube.tv",
        "com.amazon.amazonvideo.livingroom",
        "com.disney.disneyplus",
        "com.canal.android.canal",
        "com.plexapp.android",
        "com.spotify.tv.android",
        "com.google.android.youtube.tvmusic"
    )

    // Les écrans d'accueil eux-mêmes n'ont rien à faire dans la liste.
    private val hidden = setOf(
        "com.google.android.tvlauncher",
        "com.google.android.apps.tv.launcherx",
        "com.google.android.leanbacklauncher"
    )

    // Cache : on ne re-rend pas les images d'une appli inchangée.
    private val cache = HashMap<String, Pair<Long, AppEntry>>()

    suspend fun load(context: Context): List<AppEntry> =
        withContext(Dispatchers.Default) { loadBlocking(context.applicationContext) }

    @Synchronized
    private fun loadBlocking(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val self = context.packageName

        val leanback = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val standard = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        // Sur Google TV, seules les applis « leanback » ont leur place dans un launcher.
        // Si aucune n'est trouvée (cas inhabituel), on se rabat sur les applis classiques.
        val leanbackApps = pm.queryIntentActivities(leanback, 0)
        val resolved = if (leanbackApps.isNotEmpty()) leanbackApps else pm.queryIntentActivities(standard, 0)

        val seenPackages = HashSet<String>()
        val result = ArrayList<AppEntry>()

        for (ri in resolved) {
            val pkg = ri.activityInfo?.packageName ?: continue
            if (pkg == self || pkg in hidden) continue
            if (!seenPackages.add(pkg)) continue
            try {
                result.add(entryFor(pm, ri))
            } catch (e: Exception) {
                // Une appli problématique ne doit pas empêcher d'afficher les autres.
            }
        }

        cache.keys.retainAll(result.map { it.key }.toSet())

        result.sortWith(
            compareBy<AppEntry>(
                { priorityOf(it.packageName) },
                { it.label.lowercase(Locale.getDefault()) }
            )
        )
        return result
    }

    private fun entryFor(pm: PackageManager, ri: ResolveInfo): AppEntry {
        val ai = ri.activityInfo
        val pkg = ai.packageName
        val key = "$pkg/${ai.name}"

        val version = try {
            File(ai.applicationInfo.sourceDir).lastModified()
        } catch (e: Exception) {
            0L
        }
        val cached = cache[key]
        if (cached != null && cached.first == version) return cached.second

        val label = ri.loadLabel(pm)?.toString()?.takeIf { it.isNotBlank() } ?: pkg

        val iconDrawable: Drawable? = try {
            ri.loadIcon(pm)
        } catch (e: Exception) {
            null
        }
        val bannerDrawable: Drawable? = try {
            ai.loadBanner(pm) ?: ai.applicationInfo.loadBanner(pm)
        } catch (e: Exception) {
            null
        }

        val icon = try {
            iconDrawable?.render(ICON_SIZE, ICON_SIZE)
        } catch (e: Exception) {
            null
        } ?: Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888)

        val banner = try {
            bannerDrawable?.render(BANNER_W, BANNER_H)
        } catch (e: Exception) {
            null
        }

        val entry = AppEntry(
            key = key,
            label = label,
            packageName = pkg,
            component = ComponentName(pkg, ai.name),
            banner = banner,
            icon = icon,
            accent = try {
                accentOf(banner ?: icon)
            } catch (e: Exception) {
                defaultAccent
            }
        )
        cache[key] = version to entry
        return entry
    }

    private fun priorityOf(pkg: String): Int {
        val index = priority.indexOf(pkg)
        return if (index >= 0) index else priority.size
    }

    private fun Drawable.render(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        setBounds(0, 0, width, height)
        draw(canvas)
        return bitmap
    }

    /** Couleur dominante assombrie, utilisée pour l'ambiance de fond. */
    private fun accentOf(bitmap: Bitmap): Int {
        val small = Bitmap.createScaledBitmap(bitmap, 16, 16, true)
        var r = 0L
        var g = 0L
        var b = 0L
        var n = 0
        for (y in 0 until 16) {
            for (x in 0 until 16) {
                val p = small.getPixel(x, y)
                if (Color.alpha(p) > 128) {
                    r += Color.red(p)
                    g += Color.green(p)
                    b += Color.blue(p)
                    n++
                }
            }
        }
        if (small !== bitmap) small.recycle()
        if (n == 0) return defaultAccent

        val hsv = FloatArray(3)
        Color.RGBToHSV((r / n).toInt(), (g / n).toInt(), (b / n).toInt(), hsv)
        hsv[1] = hsv[1].coerceIn(0.25f, 0.85f)
        hsv[2] = hsv[2].coerceIn(0.22f, 0.50f)
        return Color.HSVToColor(hsv)
    }
}
