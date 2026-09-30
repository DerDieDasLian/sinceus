package app.sinceus.update

import android.content.Context
import app.sinceus.BuildConfig
import app.sinceus.data.LoveRepository
import app.sinceus.notify.Notifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Sucht in der GitHub-Variante nach einer neueren Version unter den GitHub Releases.
 * Es wird nur die öffentliche Release-Liste abgefragt, es werden keine Daten gesendet.
 * Heruntergeladen wird erst, wenn man auf „Installieren“ tippt (siehe [UpdateInstaller]).
 */
object UpdateChecker {
    /** [url] = Release-Seite, [apkUrl] = direkter Download der APK (falls vorhanden) */
    data class Release(val version: String, val url: String, val apkUrl: String? = null)

    private const val INTERVAL_MS = 20 * 60 * 60 * 1000L
    private const val TIMEOUT_MS = 8000

    /** Einmal pro Tag, wenn in den Einstellungen erlaubt. */
    suspend fun checkIfDue(context: Context) {
        if (!BuildConfig.UPDATE_CHECK) return
        val repo = LoveRepository(context)
        if (!repo.current().updateCheck) return
        if (System.currentTimeMillis() - repo.lastUpdateCheck() < INTERVAL_MS) return
        check(context)
    }

    /** Sucht sofort; liefert die neuere Version oder null, wirft bei Netzwerkfehlern. */
    suspend fun check(context: Context): Release? {
        if (!BuildConfig.UPDATE_CHECK) return null
        val repo = LoveRepository(context)
        val latest = fetchLatest()
        val newer = latest?.takeIf { isNewer(it.version, BuildConfig.VERSION_NAME) }
        repo.setUpdateResult(newer?.version, newer?.url, newer?.apkUrl)
        if (newer != null && repo.markUpdateNotified(newer.version)) {
            Notifier.showUpdate(context, newer)
        }
        return newer
    }

    private suspend fun fetchLatest(): Release? = withContext(Dispatchers.IO) {
        val url = URL("https://api.github.com/repos/${BuildConfig.GITHUB_REPO}/releases/latest")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Accept", "application/vnd.github+json")
        }
        try {
            // 404: noch kein Release veröffentlicht
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            parse(json)
        } finally {
            connection.disconnect()
        }
    }

    fun parse(json: JSONObject): Release {
        val assets = json.optJSONArray("assets")
        val apk = (0 until (assets?.length() ?: 0))
            .map { assets!!.getJSONObject(it).optString("browser_download_url") }
            .firstOrNull { UpdateInstaller.isAllowed(it) }
        return Release(json.getString("tag_name").removePrefix("v"), json.getString("html_url"), apk)
    }

    /** Vergleicht Versionen wie "2.10.1" und "2.9" Zahl für Zahl. */
    fun isNewer(remote: String, current: String): Boolean {
        val a = remote.split('.', '-').map { it.toIntOrNull() ?: 0 }
        val b = current.split('.', '-').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}
