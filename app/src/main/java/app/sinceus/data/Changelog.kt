package app.sinceus.data

import android.content.Context

/**
 * „Neu in dieser Version“: Die Änderungslisten liegen beim Bauen unter assets/changelogs/de|en/<versionCode>.txt
 * (aus fastlane/metadata, siehe build.gradle.kts). Nach einem Update zeigt die App die neuen Punkte einmal an.
 */
object Changelog {
    /** Höchstens so viele Versionen auf einmal, falls lange kein Update installiert wurde */
    private const val MAX_VERSIONS = 3

    /** Punkte aller Versionen nach [seen] bis einschließlich [current], die neueste zuerst; leer = nichts Neues */
    fun linesSince(context: Context, seen: Int, current: Int): List<String> {
        if (current <= seen) return emptyList()
        val lang = if (context.resources.configuration.locales[0].language == "de") "de" else "en"
        return (current downTo seen + 1).asSequence()
            .map { code -> read(context, lang, code) }
            .filter { it.isNotEmpty() }
            .take(MAX_VERSIONS)
            .flatten()
            .toList()
    }

    private fun read(context: Context, lang: String, code: Int): List<String> = runCatching {
        context.assets.open("changelogs/$lang/$code.txt").bufferedReader().use { parse(it.readLines()) }
    }.getOrDefault(emptyList())

    /** Eine Zeile pro Punkt, ohne Aufzählungszeichen und Leerzeilen */
    internal fun parse(lines: List<String>): List<String> =
        lines.map { it.trim().removePrefix("•").removePrefix("-").trim() }.filter { it.isNotEmpty() }
}
