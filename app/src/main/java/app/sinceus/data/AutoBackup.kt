package app.sinceus.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import java.time.LocalDate

/**
 * Automatische Sicherung: einmal einen Ordner wählen, dann legt die App dort jede Woche eine
 * Sicherungsdatei ab (ganz ohne Internet). Die drei neuesten bleiben liegen, ältere werden gelöscht.
 * Der Ordner gehört zu diesem Handy und steht deshalb in eigenen Einstellungen, die nicht gesichert werden.
 */
object AutoBackup {
    private const val PREFS = "auto_backup"
    private const val KEEP = 3
    private const val PREFIX = "since-us-auto-"
    private const val WEEK_MS = 7L * 24 * 60 * 60 * 1000

    /** Etwas Spielraum, damit der Wecker kurz nach Mitternacht nicht knapp eine Woche verpasst */
    private const val SLACK_MS = 2L * 60 * 60 * 1000

    data class State(val folder: Uri, val folderName: String, val last: Long)

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(context: Context): State? {
        val p = prefs(context)
        val folder = p.getString("folder", null)?.let(Uri::parse) ?: return null
        return State(folder, p.getString("name", null).orEmpty(), p.getLong("last", 0))
    }

    /** Ordner aus der Ordnerauswahl übernehmen und das Schreibrecht dauerhaft behalten */
    fun enable(context: Context, tree: Uri): State {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(tree, flags)
        val name = runCatching { displayName(context, tree) }.getOrNull().orEmpty()
        prefs(context).edit().putString("folder", tree.toString()).putString("name", name).putLong("last", 0).apply()
        return State(tree, name, 0)
    }

    fun disable(context: Context) {
        AutoBackupJob.cancel(context)
        val state = load(context)
        if (state != null) {
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    state.folder,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
        }
        prefs(context).edit().clear().apply()
    }

    fun isDue(state: State, now: Long): Boolean = now - state.last >= WEEK_MS - SLACK_MS

    /** Plant die Sicherung als Hintergrundaufgabe ([AutoBackupJob]), wenn eine Woche um ist */
    fun scheduleIfDue(context: Context, now: Long = System.currentTimeMillis()) {
        val state = load(context) ?: return
        if (isDue(state, now)) AutoBackupJob.schedule(context)
    }

    /** Sichert, wenn eine Woche um ist. true = gesichert */
    suspend fun runIfDue(context: Context, now: Long = System.currentTimeMillis()): Boolean {
        val state = load(context) ?: return false
        if (!isDue(state, now)) return false
        return run(context, state, now)
    }

    /** Legt jetzt eine Sicherung im gewählten Ordner an und räumt alte auf. true = gesichert */
    suspend fun run(context: Context, state: State, now: Long = System.currentTimeMillis()): Boolean {
        val resolver = context.contentResolver
        val file = runCatching {
            val parent = DocumentsContract.buildDocumentUriUsingTree(state.folder, DocumentsContract.getTreeDocumentId(state.folder))
            DocumentsContract.createDocument(resolver, parent, Backup.MIME, PREFIX + LocalDate.now() + ".zip")
        }.getOrNull() ?: return false
        val written = try {
            val out = resolver.openOutputStream(file) ?: throw java.io.IOException("no output stream")
            LoveRepository(context).exportBackup(out)
            true
        } catch (_: Exception) {
            // Auch beim Abbruch: nie eine halbe Sicherung im Ordner liegen lassen, die wie eine echte aussieht
            false
        }
        if (!written) {
            runCatching { DocumentsContract.deleteDocument(resolver, file) }
            return false
        }
        prefs(context).edit().putLong("last", now).apply()
        runCatching { cleanUp(context, state.folder) }
        return true
    }

    /** Nur die neuesten [KEEP] automatischen Sicherungen behalten, eigene Dateien bleiben unberührt */
    private fun cleanUp(context: Context, tree: Uri) {
        val resolver = context.contentResolver
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val found = mutableListOf<Pair<String, String>>()
        resolver.query(
            children,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null, null, null,
        )?.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0) ?: continue
                val name = c.getString(1) ?: continue
                if (name.startsWith(PREFIX)) found += id to name
            }
        }
        oldOnes(found.map { it.second }).forEach { old ->
            val id = found.first { it.second == old }.first
            runCatching { DocumentsContract.deleteDocument(resolver, DocumentsContract.buildDocumentUriUsingTree(tree, id)) }
        }
    }

    /** Welche automatischen Sicherungen weg können: alle außer den [KEEP] neuesten (Datum steht im Namen) */
    internal fun oldOnes(names: List<String>): List<String> = names.sortedDescending().drop(KEEP)

    private fun displayName(context: Context, tree: Uri): String? {
        val doc = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        return context.contentResolver.query(doc, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    }
}
