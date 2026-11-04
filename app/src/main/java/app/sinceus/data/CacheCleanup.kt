package app.sinceus.data

import android.content.Context
import java.io.File

/**
 * Räumt alte Dateien aus dem Zwischenspeicher: geteilte Bilder und Abgleich-Dateien (cache/share)
 * und geladene Updates (cache/update). Die Abgleich-Datei enthält alle Fotos und kann groß sein.
 * Erst nach einem Tag, damit ein Messenger oder die Installation sie bis dahin sicher lesen kann.
 */
object CacheCleanup {
    private const val MAX_AGE_MS = 24L * 60 * 60 * 1000
    private val FOLDERS = listOf("share", "update")

    /** Liefert die Zahl der gelöschten Dateien */
    fun run(context: Context, now: Long = System.currentTimeMillis()): Int =
        FOLDERS.sumOf { clean(File(context.cacheDir, it), now) }

    internal fun clean(dir: File, now: Long): Int =
        dir.listFiles().orEmpty().count { it.isFile && now - it.lastModified() > MAX_AGE_MS && it.delete() }
}
