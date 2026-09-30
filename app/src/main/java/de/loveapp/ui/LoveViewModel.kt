package de.loveapp.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.loveapp.data.LoveRepository
import de.loveapp.data.LoveSettings
import de.loveapp.notify.DailyScheduler
import de.loveapp.notify.Notifier
import de.loveapp.widget.LoveWidget
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
        // Bei jeder Aenderung: Wecker neu planen und Widget aktualisieren
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
    fun setNotifications(enabled: Boolean) = viewModelScope.launch { repo.setNotifications(enabled) }
    fun setNotifyTime(h: Int, m: Int) = viewModelScope.launch { repo.setNotifyTime(h, m) }
    fun setPhoto(uri: Uri) = viewModelScope.launch { repo.setPhoto(uri) }
    fun setPreset(index: Int) = viewModelScope.launch { repo.setPreset(index) }
    fun resetPhoto() = viewModelScope.launch { repo.resetPhoto() }

    fun sendTestNotification() {
        settings.value?.let { Notifier.showTest(getApplication(), it) }
    }
}
