package app.sinceus.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import app.sinceus.BuildConfig
import app.sinceus.R

/**
 * Fehler melden und Ideen teilen, ohne dass die App selbst ins Internet muss:
 * Es wird nur ein Link oder eine Mail-App geöffnet. Mitgeschickt werden nur
 * Version, Bezugsquelle und Handymodell, nie Namen, Daten oder Fotos.
 */
object Feedback {
    private const val REPO = "https://github.com/DerDieDasLian/sinceus"
    private const val MAIL = "contact@sinceus.de"
    const val WEBLATE = "https://hosted.weblate.org/engage/since-us/"
    const val SCOPE = "$REPO/blob/main/SCOPE.md"

    /** Bezugsquelle genau wie in der Auswahl der GitHub-Vorlage */
    private val source: String
        get() = when (BuildConfig.FLAVOR) {
            "fdroid" -> "F-Droid"
            "play" -> "Google Play"
            "github" -> "GitHub"
            else -> "Other"
        }

    private val device: String
        get() = "${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}"

    /** Neue Fehlermeldung auf GitHub, Version, Quelle und Handy sind schon ausgefüllt */
    fun bugUrl(): String = "$REPO/issues/new?template=bug_report.yml" +
        "&version=" + Uri.encode(BuildConfig.VERSION_NAME) +
        "&source=" + Uri.encode(source) +
        "&device=" + Uri.encode(device)

    fun ideaUrl(): String = "$REPO/issues/new?template=feature_request.yml"

    /** Öffnet die Mail-App mit einer vorbereiteten Fehlermeldung */
    fun mailBug(context: Context) {
        val info = "Since Us ${BuildConfig.VERSION_NAME} ($source)\n$device"
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(MAIL))
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.bug_mail_subject))
            putExtra(Intent.EXTRA_TEXT, context.getString(R.string.bug_mail_body, info))
        }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, context.getString(R.string.no_mail_app, MAIL), Toast.LENGTH_LONG).show()
        }
    }
}
