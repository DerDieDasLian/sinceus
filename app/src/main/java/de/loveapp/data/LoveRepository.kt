package de.loveapp.data

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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

private val Context.dataStore by preferencesDataStore(name = "love")

/** Die meisten Paarfotos haben die Gesichter im oberen Bilddrittel */
const val DEFAULT_FOCUS_Y = -0.6f

data class LoveSettings(
    val name1: String = "",
    val name2: String = "",
    val startDate: LocalDate = LocalDate.now(),
    /** Optionale Uhrzeit, zu der alles begann (nur fuer den Live-Zaehler) */
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
    /** 0 = Uebersicht, 1 = Live-Zaehler */
    val homePage: Int = 0,
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
}

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

    /** Loescht alle Daten und das Foto, danach startet die Einrichtung neu. */
    suspend fun resetAll() {
        deletePhotos()
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

    /** Kopiert das gewaehlte Bild in den privaten App-Speicher. */
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

    suspend fun setHomePage(page: Int) = context.dataStore.edit { it[Keys.homePage] = page }

    /** true, wenn fuer [day] noch keine Mitteilung verschickt wurde (und merkt ihn sich). */
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
