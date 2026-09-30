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
import androidx.compose.runtime.LaunchedEffect
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
import de.loveapp.ui.LoveTheme
import de.loveapp.ui.LoveViewModel
import de.loveapp.ui.SettingsScreen
import de.loveapp.widget.LoveWidgetReceiver

class MainActivity : ComponentActivity() {
    private val vm: LoveViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LoveTheme {
                val settings by vm.settings.collectAsStateWithLifecycle()
                val today by vm.today.collectAsStateWithLifecycle()
                var showSettings by rememberSaveable { mutableStateOf(false) }
                // Aendert sich bei jedem Zurueckkehren, damit der Berechtigungsstatus neu gelesen wird
                var resumeCount by rememberSaveable { mutableIntStateOf(0) }

                LifecycleResumeEffect(Unit) {
                    vm.refreshToday()
                    resumeCount++
                    onPauseOrDispose { }
                }

                val permission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { resumeCount++ }
                fun askPermission() {
                    if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }

                val photoPicker = rememberLauncherForActivityResult(
                    ActivityResultContracts.PickVisualMedia(),
                ) { uri -> if (uri != null) vm.setPhoto(uri) }

                // Beim ersten Start einmal nach der Mitteilungs-Berechtigung fragen
                LaunchedEffect(settings?.notificationsEnabled) {
                    if (settings?.notificationsEnabled == true && !Notifier.canNotify(this@MainActivity)) {
                        askPermission()
                    }
                }

                BackHandler(enabled = showSettings) { showSettings = false }

                val s = settings
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    if (s != null) {
                        AnimatedContent(
                            targetState = showSettings,
                            transitionSpec = {
                                if (targetState) {
                                    (slideInHorizontally { it / 3 } + fadeIn()) togetherWith fadeOut()
                                } else {
                                    fadeIn() togetherWith (slideOutHorizontally { it / 3 } + fadeOut())
                                }
                            },
                            label = "screen",
                        ) { settingsOpen ->
                            if (settingsOpen) {
                                val allowed = remember(resumeCount) { Notifier.canNotify(this@MainActivity) }
                                SettingsScreen(
                                    settings = s,
                                    notificationsAllowed = allowed,
                                    onBack = { showSettings = false },
                                    onNames = vm::setNames,
                                    onStartDate = vm::setStartDate,
                                    onPickPhoto = {
                                        photoPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                        )
                                    },
                                    onPreset = vm::setPreset,
                                    onResetPhoto = vm::resetPhoto,
                                    onNotifications = { on ->
                                        vm.setNotifications(on)
                                        if (on && !allowed) askPermission()
                                    },
                                    onNotifyTime = vm::setNotifyTime,
                                    onTestNotification = {
                                        if (allowed) vm.sendTestNotification() else askPermission()
                                    },
                                    onAddWidget = ::pinWidget,
                                )
                            } else {
                                HomeScreen(s, today, onOpenSettings = { showSettings = true })
                            }
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
