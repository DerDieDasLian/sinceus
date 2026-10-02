package app.sinceus.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.widget.Toast
import app.sinceus.R
import app.sinceus.data.LoveRepository
import app.sinceus.update.UpdateChecker
import app.sinceus.update.UpdateInstaller
import app.sinceus.data.LoveSettings
import app.sinceus.data.Pairing
import app.sinceus.data.PairingStore
import app.sinceus.data.SyncCodec
import app.sinceus.notify.DailyScheduler
import app.sinceus.notify.Notifier
import app.sinceus.widget.LoveWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class LoveViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = LoveRepository(app)

    /** null, solange die gespeicherten Daten noch geladen werden */
    val settings: StateFlow<LoveSettings?> =
        repo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _today = MutableStateFlow(LocalDate.now())
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    init {
        // Bei jeder Änderung: Wecker neu planen und Widget aktualisieren
        viewModelScope.launch {
            settings.drop(1).collect { s ->
                if (s != null) {
                    DailyScheduler.schedule(app, s)
                    LoveWidget.refresh(app)
                }
            }
        }
    }

    /** Gewünschter Sprung von einer App-Abkürzung: "moment" oder "live", null = keiner */
    val jump = MutableStateFlow<String?>(null)

    fun refreshToday() {
        _today.value = LocalDate.now()
    }

    fun setNames(names: List<String>) = viewModelScope.launch { repo.setNames(names) }
    fun savePerson(person: app.sinceus.data.Person) = viewModelScope.launch { repo.savePerson(person) }
    fun deletePerson(id: String) = viewModelScope.launch { repo.deletePerson(id) }
    fun saveRelationship(r: app.sinceus.data.Relationship) = viewModelScope.launch { repo.saveRelationship(r) }
    fun deleteRelationship(id: String) = viewModelScope.launch { repo.deleteRelationship(id) }
    fun setSelected(id: String) = viewModelScope.launch { repo.setSelected(id) }
    fun setWidgetRelationship(id: String) = viewModelScope.launch { repo.setWidgetRelationship(id) }
    fun setDiscreet(on: Boolean) = viewModelScope.launch { repo.setDiscreet(on) }
    fun setStartDate(date: LocalDate) = viewModelScope.launch { repo.setStartDate(date) }
    fun setStartTime(time: java.time.LocalTime?) = viewModelScope.launch { repo.setStartTime(time) }
    fun resetAll() = viewModelScope.launch {
        repo.resetAll()
        _pairing.value = null
    }
    fun setNotifications(enabled: Boolean) = viewModelScope.launch { repo.setNotifications(enabled) }
    fun setNotifyTime(h: Int, m: Int) = viewModelScope.launch { repo.setNotifyTime(h, m) }
    fun setPhoto(uri: Uri) = viewModelScope.launch { repo.setPhoto(uri) }
    fun setPreset(index: Int) = viewModelScope.launch { repo.setPreset(index) }
    fun resetPhoto() = viewModelScope.launch { repo.resetPhoto() }
    fun setPhotoFrame(x: Float, y: Float, zoom: Float) = viewModelScope.launch { repo.setPhotoFrame(x, y, zoom) }
    fun setOnboardingDone(done: Boolean) = viewModelScope.launch { repo.setOnboardingDone(done) }
    fun setHomePage(page: Int) = viewModelScope.launch { repo.setHomePage(page) }

    fun saveMoment(moment: app.sinceus.data.Moment, photo: Uri?, removePhoto: Boolean) =
        viewModelScope.launch { repo.saveMoment(moment, photo, removePhoto) }
    fun deleteMoment(id: String) = viewModelScope.launch { repo.deleteMoment(id) }
    fun setShowMoments(show: Boolean) = viewModelScope.launch { repo.setShowMoments(show) }
    fun setShowLive(show: Boolean) = viewModelScope.launch { repo.setShowLive(show) }
    fun addSlides(uris: List<Uri>) = viewModelScope.launch { repo.addSlides(uris) }
    fun removeSlide(path: String) = viewModelScope.launch { repo.removeSlide(path) }

    /** Speichert alle Daten mit Fotos in die gewählte Datei. */
    fun exportBackup(uri: Uri) = viewModelScope.launch {
        val app = getApplication<Application>()
        val ok = try {
            val out = withContext(Dispatchers.IO) { app.contentResolver.openOutputStream(uri) }
                ?: throw java.io.IOException("no output stream")
            repo.exportBackup(out)
            true
        } catch (e: Exception) {
            android.util.Log.w("SinceUs", "Sicherung fehlgeschlagen", e)
            false
        }
        Toast.makeText(app, app.getString(if (ok) R.string.backup_saved else R.string.backup_save_failed), Toast.LENGTH_SHORT).show()
    }

    /** Ersetzt alle Daten durch die aus der gewählten Sicherungsdatei. */
    fun importBackup(uri: Uri) = viewModelScope.launch {
        val app = getApplication<Application>()
        val ok = try {
            val input = withContext(Dispatchers.IO) { app.contentResolver.openInputStream(uri) }
                ?: throw java.io.IOException("no input stream")
            input.use { repo.importBackup(it) }
            true
        } catch (e: Exception) {
            android.util.Log.w("SinceUs", "Wiederherstellung fehlgeschlagen", e)
            false
        }
        Toast.makeText(app, app.getString(if (ok) R.string.backup_restored else R.string.backup_restore_failed), Toast.LENGTH_LONG).show()
    }

    /** Kopplung mit dem Handy des anderen Menschen für den Abgleich, null = nicht gekoppelt */
    private val _pairing = MutableStateFlow(PairingStore.load(app))
    val pairing: StateFlow<Pairing?> = _pairing.asStateFlow()

    /** Neue Kopplung anlegen, deren QR-Code dann gezeigt wird */
    fun createPairing(me: String?) {
        val p = Pairing.create(me)
        PairingStore.save(getApplication(), p)
        _pairing.value = p
    }

    /** Gescannten QR-Code übernehmen; false, wenn es kein Kopplungscode von Since Us war */
    fun acceptPairing(qr: String?, me: String?): Boolean {
        val key = Pairing.keyFromQr(qr) ?: return false
        val p = Pairing(key, me, System.currentTimeMillis())
        PairingStore.save(getApplication(), p)
        _pairing.value = p
        return true
    }

    /** Wer auf diesem Handy die App nutzt */
    fun setMe(me: String?) {
        val p = _pairing.value?.copy(me = me) ?: return
        PairingStore.save(getApplication(), p)
        _pairing.value = p
    }

    fun unpair() {
        PairingStore.clear(getApplication())
        _pairing.value = null
    }

    /** Abgleich-Datei erstellen und über das Teilen-Menü verschicken (z. B. per Messenger) */
    fun shareSync() = viewModelScope.launch {
        val app = getApplication<Application>()
        val p = _pairing.value ?: return@launch
        try {
            val dir = java.io.File(app.cacheDir, "share").apply { mkdirs() }
            dir.listFiles { f -> f.name.endsWith(".${SyncCodec.EXTENSION}") }?.forEach { it.delete() }
            val file = java.io.File(dir, SyncCodec.fileName(today.value))
            repo.exportSync(file.outputStream(), p)
            val uri = androidx.core.content.FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", file)
            val send = android.content.Intent(android.content.Intent.ACTION_SEND)
                .setType(SyncCodec.MIME)
                .putExtra(android.content.Intent.EXTRA_STREAM, uri)
                .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            send.clipData = android.content.ClipData.newRawUri(null, uri)
            app.startActivity(
                android.content.Intent.createChooser(send, app.getString(R.string.sync_send))
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (e: Exception) {
            android.util.Log.w("SinceUs", "Abgleich-Datei fehlgeschlagen", e)
            Toast.makeText(app, app.getString(R.string.sync_send_failed), Toast.LENGTH_LONG).show()
        }
    }

    /** Abgleich-Datei vom anderen Handy übernehmen */
    fun importSync(uri: Uri) = viewModelScope.launch {
        val app = getApplication<Application>()
        val p = _pairing.value
        if (p == null) {
            Toast.makeText(app, app.getString(R.string.sync_not_paired), Toast.LENGTH_LONG).show()
            return@launch
        }
        val message = try {
            val input = withContext(Dispatchers.IO) { app.contentResolver.openInputStream(uri) }
                ?: throw java.io.IOException("no input stream")
            val result = input.use { repo.importSync(it, p) }
            _pairing.value = PairingStore.load(app)
            app.getString(R.string.sync_done, result.added, result.changed, result.removed)
        } catch (e: Exception) {
            android.util.Log.w("SinceUs", "Abgleich fehlgeschlagen", e)
            app.getString(R.string.sync_failed)
        }
        Toast.makeText(app, message, Toast.LENGTH_LONG).show()
    }

    fun setUpdateCheck(enabled: Boolean) = viewModelScope.launch { repo.setUpdateCheck(enabled) }

    /** Sucht sofort nach Updates und meldet das Ergebnis kurz als Hinweis. */
    fun checkUpdatesNow() = viewModelScope.launch {
        val app = getApplication<Application>()
        val message = try {
            val release = UpdateChecker.check(app)
            if (release == null) app.getString(R.string.update_none) else app.getString(R.string.update_available, release.version)
        } catch (_: Exception) {
            app.getString(R.string.update_failed)
        }
        Toast.makeText(app, message, Toast.LENGTH_SHORT).show()
    }

    /** Download-Fortschritt eines Updates in Prozent, null = kein Download */
    val updateProgress: StateFlow<Int?> = UpdateInstaller.progress

    /**
     * Lädt das Update und öffnet direkt den Installationsdialog von Android.
     * Vorher wird kurz neu gesucht, damit immer die neueste APK geladen wird (gespeicherte Treffer
     * können veraltet sein oder von einer älteren App-Version ohne Download-Link stammen).
     */
    fun installUpdate() = viewModelScope.launch {
        val app = getApplication<Application>()
        Toast.makeText(app, app.getString(R.string.update_downloading), Toast.LENGTH_SHORT).show()
        try {
            val apk = UpdateChecker.check(app)?.apkUrl
            if (apk == null) {
                Toast.makeText(app, app.getString(R.string.update_none), Toast.LENGTH_SHORT).show()
                return@launch
            }
            UpdateInstaller.downloadAndInstall(app, apk)
        } catch (e: Exception) {
            android.util.Log.w("SinceUs", "Update fehlgeschlagen", e)
            Toast.makeText(app, app.getString(R.string.update_download_failed), Toast.LENGTH_LONG).show()
        }
    }

    fun sendTestNotification() {
        settings.value?.let { Notifier.showTest(getApplication(), it) }
    }
}
