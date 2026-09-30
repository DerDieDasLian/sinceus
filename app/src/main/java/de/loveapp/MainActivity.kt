package de.loveapp

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.loveapp.notify.Notifier
import de.loveapp.ui.HomeScreen
import de.loveapp.ui.OnboardingScreen
import de.loveapp.ui.PhotoEditorScreen
import de.loveapp.ui.LoveTheme
import de.loveapp.ui.LoveViewModel
import de.loveapp.ui.SettingsScreen
import de.loveapp.widget.LoveWidgetReceiver
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm: LoveViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LoveTheme {
                val settings by vm.settings.collectAsStateWithLifecycle()
                val today by vm.today.collectAsStateWithLifecycle()
                val scope = rememberCoroutineScope()
                var showSettings by rememberSaveable { mutableStateOf(false) }
                var editingPhoto by rememberSaveable { mutableStateOf(false) }
                var onboardingStep by rememberSaveable { mutableIntStateOf(0) }
                // Aendert sich bei jedem Zurueckkehren, damit der Berechtigungsstatus neu gelesen wird
                var resumeCount by rememberSaveable { mutableIntStateOf(0) }

                LifecycleResumeEffect(Unit) {
                    vm.refreshToday()
                    resumeCount++
                    onPauseOrDispose { }
                }
                val allowed = remember(resumeCount) { Notifier.canNotify(this@MainActivity) }

                val permission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { resumeCount++ }
                fun askPermission() {
                    if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                fun setNotifications(on: Boolean) {
                    vm.setNotifications(on)
                    if (on && !allowed) askPermission()
                }

                // Nach der Auswahl direkt den Bildausschnitt anpassen lassen
                val photoPicker = rememberLauncherForActivityResult(
                    ActivityResultContracts.PickVisualMedia(),
                ) { uri ->
                    if (uri != null) {
                        scope.launch {
                            vm.setPhoto(uri).join()
                            editingPhoto = true
                        }
                    }
                }
                fun pickPhoto() = photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )

                BackHandler(enabled = showSettings || editingPhoto) {
                    if (editingPhoto) editingPhoto = false else showSettings = false
                }

                val s = settings
                val screen = when {
                    s == null -> "loading"
                    editingPhoto && s.photoPath != null -> "editor"
                    !s.onboardingDone -> "onboarding"
                    showSettings -> "settings"
                    else -> "home"
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    AnimatedContent(
                        targetState = screen,
                        transitionSpec = {
                            if (targetState == "settings") {
                                (slideInHorizontally { it / 3 } + fadeIn()) togetherWith fadeOut()
                            } else if (initialState == "settings") {
                                fadeIn() togetherWith (slideOutHorizontally { it / 3 } + fadeOut())
                            } else {
                                fadeIn() togetherWith fadeOut()
                            }
                        },
                        label = "screen",
                    ) { target ->
                        if (s == null) return@AnimatedContent
                        when (target) {
                            "editor" -> PhotoEditorScreen(
                                settings = s,
                                onPickOther = ::pickPhoto,
                                onCancel = { editingPhoto = false },
                                onSave = { x, y, z ->
                                    vm.setPhotoFrame(x, y, z)
                                    editingPhoto = false
                                },
                            )
                            "onboarding" -> OnboardingScreen(
                                settings = s,
                                notificationsAllowed = allowed,
                                onNames = vm::setNames,
                                onStartDate = vm::setStartDate,
                                onPickPhoto = ::pickPhoto,
                                onAdjustPhoto = { editingPhoto = true },
                                onPreset = vm::setPreset,
                                onNotifications = ::setNotifications,
                                onNotifyTime = vm::setNotifyTime,
                                onFinish = {
                                    vm.setOnboardingDone(true)
                                    showSettings = false
                                },
                                step = onboardingStep,
                                onStep = { onboardingStep = it },
                            )
                            "settings" -> SettingsScreen(
                                settings = s,
                                notificationsAllowed = allowed,
                                onBack = { showSettings = false },
                                onNames = vm::setNames,
                                onStartDate = vm::setStartDate,
                                onPickPhoto = ::pickPhoto,
                                onPreset = vm::setPreset,
                                onResetPhoto = vm::resetPhoto,
                                onNotifications = ::setNotifications,
                                onNotifyTime = vm::setNotifyTime,
                                onTestNotification = {
                                    if (allowed) vm.sendTestNotification() else askPermission()
                                },
                                onAddWidget = ::pinWidget,
                                onAdjustPhoto = { editingPhoto = true },
                                onRestartOnboarding = {
                                    onboardingStep = 0
                                    vm.setOnboardingDone(false)
                                },
                            )
                            else -> HomeScreen(
                                s,
                                today,
                                onOpenSettings = { showSettings = true },
                                onPageChange = vm::setHomePage,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun pinWidget() {
        val manager = getSystemService(AppWidgetManager::class.java)
        if (manager.isRequestPinAppWidgetSupported) {
            manager.requestPinAppWidget(ComponentName(this, LoveWidgetReceiver::class.java), null, null)
        } else {
            Toast.makeText(
                this,
                "Lange auf den Startbildschirm drücken und unter „Widgets“ die Love App wählen",
                Toast.LENGTH_LONG,
            ).show()
        }
    }
}
