package com.appletv.launcher

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/** Un film ou une série mis en avant dans l'espace supérieur. */
class Featured(
    val title: String,
    val subtitle: String,
    val overview: String,
    val backdropUrl: String?,
    val posterUrl: String?
)

/**
 * Tendances films/séries via l'API TMDB.
 * Sans clé (TMDB_API_KEY vide), aucune requête n'est faite et l'espace supérieur
 * affiche à la place la bannière de l'application sélectionnée.
 * Ce produit utilise l'API TMDB mais n'est ni approuvé ni certifié par TMDB.
 */
object FeaturedRepository {

    private const val IMAGE_BASE = "https://image.tmdb.org/t/p/"

    suspend fun fetch(): List<Featured> = withContext(Dispatchers.IO) {
        val key = BuildConfig.TMDB_API_KEY
        if (key.isBlank()) return@withContext emptyList<Featured>()
        try {
            val url = URL("https://api.themoviedb.org/3/trending/all/week?language=fr-FR&api_key=$key")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            val body = try {
                connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                connection.disconnect()
            }

            val results = JSONObject(body).getJSONArray("results")
            val out = ArrayList<Featured>()
            for (i in 0 until results.length()) {
                val o = results.getJSONObject(i)
                val type = o.optString("media_type")
                if (type != "movie" && type != "tv") continue
                if (o.isNull("backdrop_path")) continue

                val title = o.optString("title").ifEmpty { o.optString("name") }
                if (title.isEmpty()) continue

                val date = o.optString("release_date").ifEmpty { o.optString("first_air_date") }
                val parts = ArrayList<String>()
                parts.add(if (type == "movie") "Film" else "Série")
                if (date.length >= 4) parts.add(date.substring(0, 4))
                val rating = o.optDouble("vote_average", 0.0)
                if (rating > 0.0) parts.add("★ " + String.format(Locale.FRANCE, "%.1f", rating))

                val overview = o.optString("overview").trim()
                out.add(
                    Featured(
                        title = title,
                        subtitle = parts.joinToString("  ·  "),
                        overview = if (overview.length > 240) overview.take(237) + "…" else overview,
                        backdropUrl = IMAGE_BASE + "w1280" + o.getString("backdrop_path"),
                        posterUrl = if (o.isNull("poster_path")) null
                        else IMAGE_BASE + "w342" + o.getString("poster_path")
                    )
                )
                if (out.size >= 8) break
            }
            out
        } catch (e: Exception) {
            emptyList<Featured>()
        }
    }
}
