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

    /**
     * Öffnet einen Link im Browser. Ist die GitHub-App installiert, fängt sie github.com-Links ab
     * und zeigt statt der ausgefüllten Vorlage nur die Startseite. Deshalb zuerst gezielt einen
     * Browser fragen, erst danach jede App, die den Link kann.
     */
    fun openWeb(context: Context, url: String) {
        val view = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE)
        val inBrowser = Intent(view).apply {
            // Nur Apps, die jeden https-Link öffnen, also Browser
            selector = Intent(Intent.ACTION_VIEW, Uri.parse("https://")).addCategory(Intent.CATEGORY_BROWSABLE)
        }
        if (start(context, inBrowser) || start(context, view)) return
        Toast.makeText(context, context.getString(R.string.no_browser, url), Toast.LENGTH_LONG).show()
    }

    /**
     * Öffnet die Mail-App mit einer vorbereiteten Fehlermeldung. Empfänger, Betreff und Text stehen
     * im mailto-Link selbst, denn viele Mail-Apps (z. B. Tuta, Proton, K-9) lesen die Zusatzangaben
     * im Intent nicht und öffneten bisher nur eine leere Mail oder gar nichts.
     */
    fun mailBug(context: Context) {
        val info = "Since Us ${BuildConfig.VERSION_NAME} ($source)\n$device"
        val subject = context.getString(R.string.bug_mail_subject)
        val body = context.getString(R.string.bug_mail_body, info)
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(mailto(subject, body))).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(MAIL))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        if (start(context, intent)) return
        Toast.makeText(context, context.getString(R.string.no_mail_app, MAIL), Toast.LENGTH_LONG).show()
    }

    /** mailto-Link mit Empfänger, Betreff und Text */
    fun mailto(subject: String, body: String): String =
        "mailto:$MAIL?subject=" + Uri.encode(subject) + "&body=" + Uri.encode(body)

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
