package app.sinceus.update

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import app.sinceus.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Lädt in der GitHub-Variante die neue APK aus dem GitHub Release und öffnet den Installationsdialog von Android.
 * Android installiert das Update nur, wenn es mit demselben Schlüssel signiert ist wie die installierte App.
 */
object UpdateInstaller {
    /** null = kein Download aktiv, sonst Fortschritt in Prozent (-1 = unbekannt) */
    private val _progress = MutableStateFlow<Int?>(null)
    val progress: StateFlow<Int?> = _progress.asStateFlow()

    private const val TIMEOUT_MS = 15000

    /** Nur APKs aus den Releases des eigenen Repositorys werden geladen. */
    fun isAllowed(url: String): Boolean =
        url.startsWith("https://github.com/${BuildConfig.GITHUB_REPO}/releases/download/") && url.endsWith(".apk")

    /** Lädt die APK und startet die Installation. Wirft bei Netzwerkfehlern oder ungültiger Datei. */
    suspend fun downloadAndInstall(context: Context, apkUrl: String) {
        require(isAllowed(apkUrl)) { "Unerwartete Download-Adresse" }
        if (_progress.value != null) return
        _progress.value = -1
        try {
            val file = download(context, apkUrl)
            check(isValidUpdate(context, file)) { "Keine gültige neuere Version" }
            install(context, file)
        } finally {
            _progress.value = null
        }
    }

    private suspend fun download(context: Context, apkUrl: String): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "update").apply {
            deleteRecursively()
            mkdirs()
        }
        val file = File(dir, "SinceUs.apk")
        // GitHub leitet auf seinen Download-Server weiter, beides über https
        val connection = (URL(apkUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = true
        }
        try {
            check(connection.responseCode == HttpURLConnection.HTTP_OK) { "HTTP ${connection.responseCode}" }
            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        done += read
                        if (total > 0) _progress.value = (done * 100 / total).toInt()
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
        file
    }

    /** Gleiche App und höhere Versionsnummer als die installierte. */
    private fun isValidUpdate(context: Context, file: File): Boolean {
        val pm = context.packageManager
        val archive = pm.getPackageArchiveInfo(file.path, 0) ?: return false
        val installed = pm.getPackageInfo(context.packageName, 0)
        return archive.packageName == context.packageName && versionCode(archive) > versionCode(installed)
    }

    @Suppress("DEPRECATION")
    private fun versionCode(info: android.content.pm.PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()

    private fun install(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
