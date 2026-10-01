package app.sinceus

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Sprache der App unabhängig vom Handy. Ab Android 13 übernimmt das System die Einstellung
 * (sie steht dann auch in den App-Infos), davor merkt sich die App die Wahl selbst.
 */
object AppLanguage {
    /** "" = wie das Handy */
    val OPTIONS = listOf("", "de", "en")

    private const val PREFS = "language"
    private const val KEY = "tag"

    fun current(context: Context): String =
        if (Build.VERSION.SDK_INT >= 33) {
            val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
            if (locales.isEmpty) "" else locales[0].language
        } else {
            stored(context)
        }.takeIf { it in OPTIONS }.orEmpty()

    fun set(activity: Activity, tag: String) {
        if (Build.VERSION.SDK_INT >= 33) {
            // Das System startet die Activity danach selbst neu
            activity.getSystemService(LocaleManager::class.java).applicationLocales =
                if (tag.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
        } else {
            activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, tag).apply()
            if (tag.isEmpty()) Locale.setDefault(Resources.getSystem().configuration.locales[0])
            activity.recreate()
        }
    }

    /** Vor Android 13: Kontext mit der gewählten Sprache, sonst unverändert */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= 33) return base
        val tag = stored(base).takeIf { it in OPTIONS && it.isNotEmpty() } ?: return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }

    private fun stored(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "").orEmpty()
}
