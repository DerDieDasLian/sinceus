package app.sinceus.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private val Context.dataStore by preferencesDataStore(name = "love")

/** Die meisten Paarfotos haben die Gesichter im oberen Bilddrittel */
const val DEFAULT_FOCUS_Y = -0.6f

data class LoveSettings(
    val name1: String = "",
    val name2: String = "",
    val startDate: LocalDate = LocalDate.now(),
    /** Optionale Uhrzeit, zu der alles begann (nur für den Live-Zähler) */
    val startTime: LocalTime? = null,
    /** Pfad zum eigenen Foto im App-Speicher, null = Standardmotiv */
    val photoPath: String? = null,
    val presetIndex: Int = 0,
    val notificationsEnabled: Boolean = true,
    val notifyHour: Int = 9,
    val notifyMinute: Int = 0,
    /** Bildausschnitt: Fokuspunkt (-1..1) und Zoom (1..4) */
    val focusX: Float = 0f,
    val focusY: Float = DEFAULT_FOCUS_Y,
    val zoom: Float = 1f,
    val onboardingDone: Boolean = false,
    /** 0 = Übersicht, 1 = Live-Zähler */
    val homePage: Int = 0,
    /** Nur GitHub-Variante: automatisch nach neuen Versionen suchen */
    val updateCheck: Boolean = true,
    /** Neuere Version auf GitHub, falls gefunden */
    val updateVersion: String? = null,
    val updateUrl: String? = null,
    /** Direkter Download der neuen APK, falls das Release eine enthält */
    val updateApkUrl: String? = null,
    /** Wichtige Momente der Beziehung, sortiert nach Datum */
    val moments: List<Moment> = emptyList(),
    /** Bereich „Momente“ anzeigen (lässt sich ausblenden, die Daten bleiben erhalten) */
    val showMoments: Boolean = true,
    /** Live-Zähler anzeigen (lässt sich ausblenden) */
    val showLive: Boolean = true,
    /** Weitere Fotos für die Diashow im Titelbild (Pfade im App-Speicher, files/slides/) */
    val slides: List<String> = emptyList(),
) {
    val names: String get() = "$name1 & $name2"
    val startDateTime: LocalDateTime get() = startDate.atTime(startTime ?: LocalTime.MIDNIGHT)
}

private object Keys {
    val name1 = stringPreferencesKey("name1")
    val name2 = stringPreferencesKey("name2")
    val start = longPreferencesKey("start_epoch_day")
    val startTime = intPreferencesKey("start_second_of_day")
    val photo = stringPreferencesKey("photo_path")
    val preset = intPreferencesKey("preset")
    val notify = booleanPreferencesKey("notify")
    val notifyHour = intPreferencesKey("notify_hour")
    val notifyMinute = intPreferencesKey("notify_minute")
    val lastNotified = longPreferencesKey("last_notified_epoch_day")
    val focusX = floatPreferencesKey("focus_x")
    val focusY = floatPreferencesKey("focus_y")
    val zoom = floatPreferencesKey("zoom")
    val onboardingDone = booleanPreferencesKey("onboarding_done")
    val homePage = intPreferencesKey("home_page")
    val updateCheck = booleanPreferencesKey("update_check")
    val lastUpdateCheck = longPreferencesKey("last_update_check")
    val updateVersion = stringPreferencesKey("update_version")
    val updateUrl = stringPreferencesKey("update_url")
    val updateApkUrl = stringPreferencesKey("update_apk_url")
    val updateNotified = stringPreferencesKey("update_notified")
    val moments = stringPreferencesKey("moments")
    val showMoments = booleanPreferencesKey("show_moments")
    val showLive = booleanPreferencesKey("show_live")
    val slides = stringPreferencesKey("slides")
}

/** Höchstzahl weiterer Fotos in der Diashow */
const val MAX_SLIDES = 20

class LoveRepository(private val context: Context) {

    val settings: Flow<LoveSettings> = context.dataStore.data.map { it.toSettings() }

    suspend fun current(): LoveSettings = settings.first()

    private fun Preferences.toSettings(): LoveSettings {
        val d = LoveSettings()
        return LoveSettings(
            name1 = this[Keys.name1] ?: d.name1,
            name2 = this[Keys.name2] ?: d.name2,
            startDate = this[Keys.start]?.let(LocalDate::ofEpochDay) ?: d.startDate,
            startTime = this[Keys.startTime]?.let { LocalTime.ofSecondOfDay(it.toLong()) },
            photoPath = this[Keys.photo]?.takeIf { File(it).exists() },
            presetIndex = this[Keys.preset] ?: d.presetIndex,
            notificationsEnabled = this[Keys.notify] ?: d.notificationsEnabled,
            notifyHour = this[Keys.notifyHour] ?: d.notifyHour,
            notifyMinute = this[Keys.notifyMinute] ?: d.notifyMinute,
            focusX = this[Keys.focusX] ?: d.focusX,
            focusY = this[Keys.focusY] ?: d.focusY,
            zoom = this[Keys.zoom] ?: d.zoom,
            onboardingDone = this[Keys.onboardingDone] ?: d.onboardingDone,
            homePage = this[Keys.homePage] ?: d.homePage,
            updateCheck = this[Keys.updateCheck] ?: d.updateCheck,
            updateVersion = this[Keys.updateVersion],
            updateUrl = this[Keys.updateUrl],
            updateApkUrl = this[Keys.updateApkUrl],
            moments = MomentCodec.decode(this[Keys.moments]).sortedBy { it.date },
            showMoments = this[Keys.showMoments] ?: d.showMoments,
            showLive = this[Keys.showLive] ?: d.showLive,
            slides = PathListCodec.decode(this[Keys.slides]).filter { File(it).exists() },
        )
    }

    suspend fun setNames(name1: String, name2: String) = context.dataStore.edit {
        it[Keys.name1] = name1.trim()
        it[Keys.name2] = name2.trim()
    }

    suspend fun setStartDate(date: LocalDate) = context.dataStore.edit {
        it[Keys.start] = date.toEpochDay()
    }

    suspend fun setStartTime(time: LocalTime?) = context.dataStore.edit {
        if (time == null) it.remove(Keys.startTime) else it[Keys.startTime] = time.toSecondOfDay()
    }

    /**
     * Speichert einen neuen oder geänderten Moment. [newPhoto] ersetzt das bisherige Foto,
     * [removePhoto] entfernt es.
     */
    suspend fun saveMoment(moment: Moment, newPhoto: Uri?, removePhoto: Boolean) {
        val old = current().moments.firstOrNull { it.id == moment.id }
        var photo = if (removePhoto) null else old?.photoPath
        if (newPhoto != null) {
            photo = withContext(Dispatchers.IO) {
                val file = File(momentDir(), "${moment.id}_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(newPhoto)?.use { input ->
                    file.outputStream().use { input.copyTo(it) }
                } ?: return@withContext photo
                file.absolutePath
            }
        }
        if (old?.photoPath != null && old.photoPath != photo) deleteFile(old.photoPath)
        val saved = moment.copy(photoPath = photo)
        context.dataStore.edit { prefs ->
            val list = MomentCodec.decode(prefs[Keys.moments]).filterNot { it.id == moment.id } + saved
            prefs[Keys.moments] = MomentCodec.encode(list)
        }
    }

    suspend fun setShowMoments(show: Boolean) = context.dataStore.edit { it[Keys.showMoments] = show }

    suspend fun setShowLive(show: Boolean) = context.dataStore.edit { it[Keys.showLive] = show }

    suspend fun deleteMoment(id: String) {
        val old = current().moments.firstOrNull { it.id == id } ?: return
        old.photoPath?.let { deleteFile(it) }
        context.dataStore.edit { prefs ->
            prefs[Keys.moments] = MomentCodec.encode(MomentCodec.decode(prefs[Keys.moments]).filterNot { it.id == id })
        }
    }

    private fun momentDir() = File(context.filesDir, "moments").apply { mkdirs() }

    private suspend fun deleteFile(path: String) = withContext(Dispatchers.IO) { File(path).delete() }

    /** Fügt weitere Fotos für die Diashow hinzu (höchstens [MAX_SLIDES] insgesamt). */
    suspend fun addSlides(uris: List<Uri>) {
        val room = MAX_SLIDES - current().slides.size
        if (room <= 0 || uris.isEmpty()) return
        val stamp = System.currentTimeMillis()
        val added = withContext(Dispatchers.IO) {
            uris.take(room).mapIndexedNotNull { i, uri ->
                val file = File(slideDir(), "slide_${stamp}_$i.jpg")
                val copied = runCatching {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        file.outputStream().use { input.copyTo(it) }
                    } != null
                }.getOrDefault(false)
                if (copied) {
                    file.absolutePath
                } else {
                    file.delete()
                    null
                }
            }
        }
        context.dataStore.edit { prefs ->
            prefs[Keys.slides] = PathListCodec.encode(PathListCodec.decode(prefs[Keys.slides]) + added)
        }
    }

    suspend fun removeSlide(path: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.slides] = PathListCodec.encode(PathListCodec.decode(prefs[Keys.slides]) - path)
        }
        deleteFile(path)
    }

    private fun slideDir() = File(context.filesDir, "slides").apply { mkdirs() }

    /**
     * Schreibt alle Daten mit Fotos als ZIP-Datei in [out]: backup.json plus die Ordner
     * photos/, slides/ und moments/. Der Stream wird danach geschlossen.
     */
    suspend fun exportBackup(out: OutputStream) {
        val s = current()
        withContext(Dispatchers.IO) {
            ZipOutputStream(out.buffered()).use { zip ->
                zip.putNextEntry(ZipEntry(Backup.JSON))
                zip.write(BackupCodec.encode(s).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                val written = mutableSetOf<String>()
                fun add(folder: String, path: String?) {
                    val file = path?.let(::File)?.takeIf { it.isFile } ?: return
                    val name = "$folder/${file.name}"
                    if (!written.add(name)) return
                    zip.putNextEntry(ZipEntry(name))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
                add(Backup.PHOTOS, s.photoPath)
                s.slides.forEach { add(Backup.SLIDES, it) }
                s.moments.forEach { add(Backup.MOMENTS, it.photoPath) }
            }
        }
    }

    /**
     * Stellt eine Sicherung aus [input] wieder her und ersetzt dabei alle bisherigen Daten.
     * Ist die Datei keine gültige Sicherung, wird eine Exception geworfen und nichts verändert.
     */
    suspend fun importBackup(input: InputStream) {
        val restore = File(context.cacheDir, "restore")
        val data = withContext(Dispatchers.IO) {
            restore.deleteRecursively()
            var json: String? = null
            ZipInputStream(input.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) continue
                    val parts = entry.name.split('/')
                    if (entry.name == Backup.JSON) {
                        json = Backup.readLimited(zip).toString(Charsets.UTF_8)
                    } else if (parts.size == 2 && parts[0] in Backup.FOLDERS && Backup.isSafeName(parts[1])) {
                        // Nur einfache Dateinamen in den bekannten Ordnern, nie Pfade nach außerhalb
                        val target = File(File(restore, parts[0]).apply { mkdirs() }, parts[1])
                        target.outputStream().use { zip.copyTo(it) }
                    }
                }
            }
            BackupCodec.decode(json ?: throw IOException("backup.json missing"))
        }

        // Ab hier ist die Sicherung gültig: alte Dateien durch die aus der Sicherung ersetzen
        val restored = withContext(Dispatchers.IO) {
            fun move(folder: String, dir: File, name: String?): String? {
                val source = name?.let { File(File(restore, folder), it) }?.takeIf { it.isFile } ?: return null
                val target = File(dir, name)
                source.copyTo(target, overwrite = true)
                return target.absolutePath
            }
            deletePhotos()
            slideDir().listFiles()?.forEach { it.delete() }
            momentDir().listFiles()?.forEach { it.delete() }
            val result = data.copy(
                photoPath = move(Backup.PHOTOS, photoDir(), data.photoPath),
                slides = data.slides.mapNotNull { move(Backup.SLIDES, slideDir(), it) },
                moments = data.moments.map { it.copy(photoPath = move(Backup.MOMENTS, momentDir(), it.photoPath)) },
            )
            restore.deleteRecursively()
            result
        }

        context.dataStore.edit { p ->
            // Merker für schon verschickte Mitteilungen behalten, damit nichts doppelt kommt
            val lastNotified = p[Keys.lastNotified]
            p.clear()
            lastNotified?.let { p[Keys.lastNotified] = it }
            p[Keys.name1] = restored.name1
            p[Keys.name2] = restored.name2
            p[Keys.start] = restored.startDate.toEpochDay()
            restored.startTime?.let { p[Keys.startTime] = it.toSecondOfDay() }
            restored.photoPath?.let { p[Keys.photo] = it }
            p[Keys.preset] = restored.presetIndex
            p[Keys.notify] = restored.notificationsEnabled
            p[Keys.notifyHour] = restored.notifyHour
            p[Keys.notifyMinute] = restored.notifyMinute
            p[Keys.focusX] = restored.focusX
            p[Keys.focusY] = restored.focusY
            p[Keys.zoom] = restored.zoom
            p[Keys.onboardingDone] = true
            p[Keys.homePage] = restored.homePage
            p[Keys.updateCheck] = restored.updateCheck
            p[Keys.moments] = MomentCodec.encode(restored.moments)
            p[Keys.showMoments] = restored.showMoments
            p[Keys.showLive] = restored.showLive
            p[Keys.slides] = PathListCodec.encode(restored.slides)
        }
    }

    /** Löscht alle Daten und das Foto, danach startet die Einrichtung neu. */
    suspend fun resetAll() {
        deletePhotos()
        withContext(Dispatchers.IO) {
            momentDir().listFiles()?.forEach { it.delete() }
            slideDir().listFiles()?.forEach { it.delete() }
        }
        context.dataStore.edit {
            it.clear()
        }
    }

    suspend fun setNotifications(enabled: Boolean) = context.dataStore.edit {
        it[Keys.notify] = enabled
    }

    suspend fun setNotifyTime(hour: Int, minute: Int) = context.dataStore.edit {
        it[Keys.notifyHour] = hour
        it[Keys.notifyMinute] = minute
    }

    suspend fun setPreset(index: Int) {
        deletePhotos()
        context.dataStore.edit {
            it.remove(Keys.photo)
            it[Keys.preset] = index
        }
    }

    suspend fun resetPhoto() = setPreset(0)

    /** Kopiert das gewählte Bild in den privaten App-Speicher. */
    suspend fun setPhoto(uri: Uri) {
        val target = withContext(Dispatchers.IO) {
            deletePhotos()
            // Neuer Dateiname pro Foto, damit der Bild-Cache sicher aktualisiert
            val file = File(photoDir(), "photo_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { input.copyTo(it) }
            } ?: return@withContext null
            file
        } ?: return
        context.dataStore.edit {
            it[Keys.photo] = target.absolutePath
            // Neues Foto startet mit dem Standard-Ausschnitt
            it.remove(Keys.focusX)
            it.remove(Keys.focusY)
            it.remove(Keys.zoom)
        }
    }

    suspend fun setPhotoFrame(focusX: Float, focusY: Float, zoom: Float) = context.dataStore.edit {
        it[Keys.focusX] = focusX
        it[Keys.focusY] = focusY
        it[Keys.zoom] = zoom
    }

    suspend fun setOnboardingDone(done: Boolean) = context.dataStore.edit { it[Keys.onboardingDone] = done }

    suspend fun setUpdateCheck(enabled: Boolean) = context.dataStore.edit { it[Keys.updateCheck] = enabled }

    suspend fun lastUpdateCheck(): Long = context.dataStore.data.first()[Keys.lastUpdateCheck] ?: 0L

    /** Speichert das Ergebnis einer Update-Suche; [version] null = aktuell. */
    suspend fun setUpdateResult(version: String?, url: String?, apkUrl: String? = null) = context.dataStore.edit {
        it[Keys.lastUpdateCheck] = System.currentTimeMillis()
        if (version == null || url == null) {
            it.remove(Keys.updateVersion)
            it.remove(Keys.updateUrl)
            it.remove(Keys.updateApkUrl)
        } else {
            it[Keys.updateVersion] = version
            it[Keys.updateUrl] = url
            if (apkUrl != null) it[Keys.updateApkUrl] = apkUrl else it.remove(Keys.updateApkUrl)
        }
    }

    /** true, wenn für [version] noch keine Update-Mitteilung gezeigt wurde (und merkt es sich). */
    suspend fun markUpdateNotified(version: String): Boolean {
        var fresh = false
        context.dataStore.edit {
            if (it[Keys.updateNotified] != version) {
                it[Keys.updateNotified] = version
                fresh = true
            }
        }
        return fresh
    }

    suspend fun setHomePage(page: Int) = context.dataStore.edit { it[Keys.homePage] = page }

    /** true, wenn für [day] noch keine Mitteilung verschickt wurde (und merkt ihn sich). */
    suspend fun markNotified(day: LocalDate): Boolean {
        var fresh = false
        context.dataStore.edit {
            if ((it[Keys.lastNotified] ?: Long.MIN_VALUE) < day.toEpochDay()) {
                it[Keys.lastNotified] = day.toEpochDay()
                fresh = true
            }
        }
        return fresh
    }

    private fun photoDir() = File(context.filesDir, "photos").apply { mkdirs() }

    private suspend fun deletePhotos() = withContext(Dispatchers.IO) {
        photoDir().listFiles()?.forEach { it.delete() }
    }
}
