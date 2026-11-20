package com.appletv.launcher

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Garde la trace du dernier plantage pour l'afficher à l'écran au lancement suivant
 * (pratique sur une TV, où l'on n'a pas accès aux journaux système).
 */
object CrashReporter {

    private var installed = false

    private fun file(context: Context) = File(context.filesDir, "last_crash.txt")

    fun install(context: Context) {
        if (installed) return
        installed = true
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                record(app, throwable)
            } catch (e: Throwable) {
                // Rien de plus à faire : on laisse le plantage suivre son cours.
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun record(context: Context, throwable: Throwable) {
        val writer = StringWriter()
        throwable.printStackTrace(PrintWriter(writer))
        file(context).writeText("Thread: ${Thread.currentThread().name}\n$writer")
    }

    fun pending(context: Context): String? {
        val f = file(context)
        return if (f.exists()) f.readText().takeIf { it.isNotBlank() } else null
    }

    fun clear(context: Context) {
        file(context).delete()
    }
}
