package com.appletv.launcher

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** Chargeur d'images minimal (cache mémoire LRU, RGB_565 pour rester léger sur la TV). */
object ImageLoader {

    private val cache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 6).toInt().coerceAtMost(48 * 1024 * 1024)
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    suspend fun load(url: String): Bitmap? {
        cache.get(url)?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 8_000
                connection.readTimeout = 10_000
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                val bitmap = try {
                    connection.inputStream.use { BitmapFactory.decodeStream(it, null, options) }
                } finally {
                    connection.disconnect()
                }
                if (bitmap != null) cache.put(url, bitmap)
                bitmap
            } catch (e: Exception) {
                null
            }
        }
    }
}
