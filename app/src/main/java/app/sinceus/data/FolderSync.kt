package app.sinceus.data

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException

/**
 * Abgleich über einen Ordner: Man wählt einmal einen Ordner, den eine andere App (z. B. eine
 * Cloud- oder Ordner-Sync-App) mit den anderen Geräten abgleicht. Since Us selbst braucht dafür
 * kein Internet.
 *
 * Jedes Gerät schreibt nur seine eigene Datei ([fileName], mit einer zufälligen Geräte-Nummer)
 * und liest die Dateien aller anderen Geräte, die mit demselben Schlüssel gekoppelt sind. So können
 * auch mehrere Geräte pro Mensch mitmachen, und zwei Geräte schreiben nie in dieselbe Datei.
 * Die Dateien sind wie beim Abgleich per Datei mit dem Schlüssel aus dem QR-Code verschlüsselt.
 *
 * Ordner, Geräte-Nummer und was schon gelesen wurde gehören zu diesem Gerät und stehen deshalb in
 * eigenen Einstellungen, die nicht gesichert werden.
 */
object FolderSync {
    private const val PREFS = "folder_sync"
    private const val PREFIX = "since-us-sync-"
    private val NAME = Regex("^since-us-sync-([a-z0-9]{1,32})\\.${SyncCodec.EXTENSION}$")

    /** Etwas warten nach einer Änderung, damit mehrere Änderungen hintereinander nur eine Datei ergeben */
    const val AFTER_CHANGE_MS = 15_000L

    /** Nur ein Abgleich gleichzeitig (Knopf, Hintergrundaufgabe) */
    private val lock = Mutex()

    data class State(
        val folder: Uri,
        val folderName: String,
        /** Letzter erfolgreicher Abgleich in Millisekunden, 0 = noch keiner */
        val last: Long,
        /** Der letzte Versuch ist gescheitert, z. B. weil der Ordner nicht mehr da ist */
        val failed: Boolean,
    )

    /** Ergebnis eines Abgleichs: wie viele Dateien anderer Geräte gelesen und wie viele Momente dabei geändert wurden */
    data class Outcome(val read: Int, val changes: Int)

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(context: Context): State? {
        val p = prefs(context)
        val folder = p.getString("folder", null)?.let(Uri::parse) ?: return null
        return State(folder, p.getString("name", null).orEmpty(), p.getLong("last", 0), p.getBoolean("failed", false))
    }

    /** Zufällige Nummer dieses Geräts, bleibt auch beim Wechsel des Ordners gleich */
    fun deviceId(context: Context): String {
        val p = prefs(context)
        return p.getString("device", null) ?: newId().lowercase().also { p.edit().putString("device", it).apply() }
    }

    /** Name der Datei, die dieses Gerät in den Ordner schreibt */
    fun fileName(deviceId: String) = "$PREFIX$deviceId.${SyncCodec.EXTENSION}"

    /** true, wenn [name] die Datei eines anderen Geräts ist (Konfliktkopien von Ordner-Apps zählen nicht) */
    internal fun isOtherDevice(name: String, ownId: String): Boolean {
        val id = NAME.matchEntire(name)?.groupValues?.get(1) ?: return false
        return id != ownId
    }

    /** Ordner aus der Ordnerauswahl übernehmen und Lese- und Schreibrecht dauerhaft behalten */
    fun enable(context: Context, tree: Uri): State {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(tree, flags)
        val name = runCatching { displayName(context, tree) }.getOrNull().orEmpty()
        val old = load(context)?.folder
        if (old != null && old != tree) release(context, old)
        prefs(context).edit()
            .putString("folder", tree.toString())
            .putString("name", name)
            .putLong("last", 0)
            .putBoolean("failed", false)
            .remove("seen")
            .remove("written")
            .apply()
        return State(tree, name, 0, false)
    }

    /** Ordner-Abgleich ausschalten; die Dateien im Ordner bleiben liegen */
    fun disable(context: Context) {
        FolderSyncJob.cancel(context)
        load(context)?.let { release(context, it.folder) }
        // Die Geräte-Nummer bleibt, damit ein späterer Ordner-Abgleich dieselbe Datei weiterschreibt
        val device = prefs(context).getString("device", null)
        val editor = prefs(context).edit().clear()
        if (device != null) editor.putString("device", device)
        editor.apply()
    }

    private fun release(context: Context, folder: Uri) {
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                folder,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
    }

    /** Plant einen Abgleich im Hintergrund, wenn ein Ordner gewählt und das Gerät gekoppelt ist */
    fun schedule(context: Context, delayMs: Long = 0) {
        if (load(context) == null || PairingStore.load(context) == null) return
        FolderSyncJob.schedule(context, delayMs)
    }

    /**
     * Gleicht jetzt ab: neue Dateien anderer Geräte einlesen, danach die eigene Datei schreiben,
     * falls sich etwas geändert hat. null = kein Ordner gewählt oder nicht gekoppelt.
     * Wirft eine Exception, wenn der Ordner nicht erreichbar ist.
     */
    suspend fun run(context: Context): Outcome? = lock.withLock {
        val state = load(context) ?: return null
        val pairing = PairingStore.load(context) ?: return null
        try {
            val outcome = withContext(Dispatchers.IO) { sync(context, state, pairing) }
            prefs(context).edit().putLong("last", System.currentTimeMillis()).putBoolean("failed", false).apply()
            outcome
        } catch (e: Exception) {
            prefs(context).edit().putBoolean("failed", true).apply()
            throw e
        }
    }

    private suspend fun sync(context: Context, state: State, pairing: Pairing): Outcome {
        val resolver = context.contentResolver
        val repo = LoveRepository(context)
        val own = deviceId(context)
        val tree = state.folder
        val treeId = DocumentsContract.getTreeDocumentId(tree)
        val files = mutableListOf<Entry>()
        resolver.query(
            DocumentsContract.buildChildDocumentsUriUsingTree(tree, treeId),
            arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_LAST_MODIFIED, Document.COLUMN_SIZE),
            null, null, null,
        )?.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0) ?: continue
                val name = c.getString(1) ?: continue
                files += Entry(id, name, "${if (c.isNull(2)) 0 else c.getLong(2)}:${if (c.isNull(3)) -1 else c.getLong(3)}")
            }
        } ?: throw IOException("folder not readable")

        // Dateien der anderen Geräte, die sich seit dem letzten Mal geändert haben
        val seen = readSeen(context)
        var read = 0
        var changes = 0
        files.filter { isOtherDevice(it.name, own) && seen[it.name] != it.stamp }.forEach { f ->
            val uri = DocumentsContract.buildDocumentUriUsingTree(tree, f.id)
            val header = runCatching {
                resolver.openInputStream(uri)?.use { input -> ByteArray(HEADER).also { java.io.DataInputStream(input).readFully(it) } }
            }.getOrNull() ?: return@forEach
            if (!SyncCrypto.belongsTo(header, pairing.key)) {
                // Gehört zu einer anderen Kopplung: nicht jedes Mal neu versuchen
                seen[f.name] = f.stamp
                return@forEach
            }
            // Wird die Datei gerade noch hochgeladen, schlägt das Entschlüsseln fehl und sie kommt beim nächsten Mal dran
            val result = runCatching {
                resolver.openInputStream(uri)?.use { repo.importSync(it, pairing) }
            }.onFailure { android.util.Log.w("SinceUs", "Ordner-Abgleich: Datei übersprungen", it) }.getOrNull()
                ?: return@forEach
            seen[f.name] = f.stamp
            read++
            changes += result.added + result.changed + result.removed
        }
        // Dateien, die nicht mehr da sind, vergessen
        seen.keys.retainAll(files.map { it.name }.toSet())
        writeSeen(context, seen)

        // Eigene Datei nur neu schreiben, wenn sich etwas geändert hat oder sie fehlt
        val name = fileName(own)
        val existing = files.firstOrNull { it.name == name }
        val fingerprint = repo.syncFingerprint(pairing)
        if (existing == null || prefs(context).getString("written", null) != fingerprint) {
            write(context, tree, treeId, existing?.id, name, pairing, repo)
            prefs(context).edit().putString("written", fingerprint).apply()
        }
        return Outcome(read, changes)
    }

    /**
     * Schreibt die eigene Datei neu. Die alte wird vorher gelöscht statt überschrieben, weil manche
     * Ordner beim Überschreiben alte Reste am Ende stehen lassen.
     */
    private suspend fun write(
        context: Context,
        tree: Uri,
        treeId: String,
        oldId: String?,
        name: String,
        pairing: Pairing,
        repo: LoveRepository,
    ) {
        val resolver = context.contentResolver
        if (oldId != null) {
            runCatching { DocumentsContract.deleteDocument(resolver, DocumentsContract.buildDocumentUriUsingTree(tree, oldId)) }
        }
        val parent = DocumentsContract.buildDocumentUriUsingTree(tree, treeId)
        var file = DocumentsContract.createDocument(resolver, parent, "application/octet-stream", name)
            ?: throw IOException("cannot create sync file")
        // Hat der Ordner einen anderen Namen vergeben (z. B. „… (1)“), den richtigen Namen erzwingen
        if (displayNameOf(context, file) != name) {
            file = runCatching { DocumentsContract.renameDocument(resolver, file, name) }.getOrNull() ?: file
        }
        try {
            val out = resolver.openOutputStream(file) ?: throw IOException("no output stream")
            repo.exportSync(out, pairing)
        } catch (e: Exception) {
            // Nie eine halbe Datei liegen lassen
            runCatching { DocumentsContract.deleteDocument(resolver, file) }
            throw e
        }
    }

    private class Entry(val id: String, val name: String, val stamp: String)

    /** Zuletzt gelesener Stand (Änderungszeit und Größe) je Datei anderer Geräte */
    private fun readSeen(context: Context): MutableMap<String, String> {
        val o = prefs(context).getString("seen", null)?.let { runCatching { JSONObject(it) }.getOrNull() } ?: return mutableMapOf()
        return o.keys().asSequence().associateWith { o.optString(it) }.toMutableMap()
    }

    private fun writeSeen(context: Context, seen: Map<String, String>) {
        prefs(context).edit().putString("seen", JSONObject(seen).toString()).apply()
    }

    /** Länge des Dateianfangs, an dem man erkennt, zu welcher Kopplung eine Datei gehört */
    private const val HEADER = 21

    private fun displayName(context: Context, tree: Uri): String? =
        displayNameOf(context, DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree)))

    private fun displayNameOf(context: Context, doc: Uri): String? =
        context.contentResolver.query(doc, arrayOf(Document.COLUMN_DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
}

/**
 * Führt den Ordner-Abgleich als Hintergrundaufgabe aus, damit er auch fertig wird, wenn die App
 * gerade geschlossen wurde. Android startet ihn neu, falls er unterbrochen wird.
 */
class FolderSyncJob : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var running: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        running = scope.launch {
            runCatching { FolderSync.run(applicationContext) }
                .onFailure { android.util.Log.w("SinceUs", "Ordner-Abgleich fehlgeschlagen", it) }
            if (isActive) jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        running?.cancel()
        running = null
        return true
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        /** Neben der automatischen Sicherung (0x5153_0001), weit weg von den Nummern von WorkManager */
        private const val JOB_ID = 0x5153_0002

        /** Plant den Abgleich in [delayMs]; ein schon geplanter wird dabei ersetzt */
        fun schedule(context: Context, delayMs: Long) {
            val job = JobInfo.Builder(JOB_ID, ComponentName(context, FolderSyncJob::class.java))
                .setMinimumLatency(delayMs)
                .setOverrideDeadline(delayMs + 60_000)
                .build()
            runCatching { context.getSystemService(JobScheduler::class.java).schedule(job) }
        }

        fun cancel(context: Context) {
            runCatching { context.getSystemService(JobScheduler::class.java).cancel(JOB_ID) }
        }
    }
}
