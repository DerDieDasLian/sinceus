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
import app.sinceus.notify.DailyScheduler
import app.sinceus.notify.Notifier
import app.sinceus.widget.LoveWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

    fun refreshToday() {
        _today.value = LocalDate.now()
    }

    fun setNames(a: String, b: String) = viewModelScope.launch { repo.setNames(a, b) }
    fun setStartDate(date: LocalDate) = viewModelScope.launch { repo.setStartDate(date) }
    fun setStartTime(time: java.time.LocalTime?) = viewModelScope.launch { repo.setStartTime(time) }
    fun resetAll() = viewModelScope.launch { repo.resetAll() }
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
     * Lädt das Update und öffnet den Installationsdialog.
     * Ohne APK im Release (oder wenn der Download scheitert) wird die Release-Seite geöffnet.
     */
    fun installUpdate() = viewModelScope.launch {
        val app = getApplication<Application>()
        val s = repo.current()
        val apk = s.updateApkUrl
        if (apk == null) {
            openUpdatePage(s.updateUrl)
            return@launch
        }
        Toast.makeText(app, app.getString(R.string.update_downloading), Toast.LENGTH_SHORT).show()
        try {
            UpdateInstaller.downloadAndInstall(app, apk)
        } catch (_: Exception) {
            Toast.makeText(app, app.getString(R.string.update_download_failed), Toast.LENGTH_LONG).show()
            openUpdatePage(s.updateUrl)
        }
    }

    private fun openUpdatePage(url: String?) {
        if (url == null) return
        getApplication<Application>().startActivity(
            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    fun sendTestNotification() {
        settings.value?.let { Notifier.showTest(getApplication(), it) }
    }
}
