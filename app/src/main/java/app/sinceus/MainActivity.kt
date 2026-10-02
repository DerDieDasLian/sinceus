package app.sinceus

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.IntentCompat
import app.sinceus.ui.SyncActions
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import android.os.Bundle
import android.os.SystemClock
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
import androidx.compose.runtime.CompositionLocalProvider
import app.sinceus.ui.LocalPrideMonth
import app.sinceus.ui.isPrideMonth
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
import app.sinceus.data.Backup
import app.sinceus.ui.ChangelogDialog
import app.sinceus.data.Changelog
import app.sinceus.data.MAX_SLIDES
import app.sinceus.notify.Notifier
import app.sinceus.share.ShareCard
import app.sinceus.ui.HomeScreen
import app.sinceus.ui.LicensesScreen
import app.sinceus.ui.LockScreen
import app.sinceus.ui.MomentEditorScreen
import app.sinceus.ui.OnboardingScreen
import app.sinceus.ui.PhotoEditorScreen
import app.sinceus.ui.LoveTheme
import app.sinceus.ui.LoveViewModel
import app.sinceus.ui.SettingsScreen
import app.sinceus.widget.PhotoWidgetReceiver
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm: LoveViewModel by viewModels()

    // App-Sperre: an/aus, gerade gesperrt, wann die App zuletzt in den Hintergrund ging
    private var lockOn by mutableStateOf(false)
    private var locked by mutableStateOf(false)
    private var leftAt = 0L
    private var autoPrompted = false

    // Bis Android 10 fragt der Bildschirm der Displaysperre nach PIN oder Fingerabdruck
    private val credential = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) locked = false
    }

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lockOn = AppLock.isEnabled(this)
        // Nach dem Drehen entsperrt bleiben, nach langer Pause (App vom System beendet) wieder sperren
        val away = System.currentTimeMillis() - (savedInstanceState?.getLong(KEY_SAVED_AT) ?: 0L)
        locked = lockOn && (savedInstanceState?.getBoolean(KEY_LOCKED) != false || away >= AppLock.GRACE_MS)
        AppLock.hideInRecents(this, lockOn)
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            LoveTheme(font = settings?.font.orEmpty()) {
                val today by vm.today.collectAsStateWithLifecycle()
                val updateProgress by vm.updateProgress.collectAsStateWithLifecycle()
                val pairing by vm.pairing.collectAsStateWithLifecycle()
                val autoBackup by vm.autoBackup.collectAsStateWithLifecycle()
                val scope = rememberCoroutineScope()
                var showSettings by rememberSaveable { mutableStateOf(false) }
                var editingPhoto by rememberSaveable { mutableStateOf(false) }
                var showLicenses by rememberSaveable { mutableStateOf(false) }
                // Moment-Editor: null = zu, "" = neuer Moment, sonst ID; dazu optional ein vorgeschlagener Titel
                var editingMoment by rememberSaveable { mutableStateOf<String?>(null) }
                var momentSuggestion by rememberSaveable { mutableStateOf<String?>(null) }
                var onboardingStep by rememberSaveable { mutableIntStateOf(0) }
                // Ändert sich bei jedem Zurückkehren, damit der Berechtigungsstatus neu gelesen wird
                var resumeCount by rememberSaveable { mutableIntStateOf(0) }

                LifecycleResumeEffect(Unit) {
                    vm.refreshToday()
                    resumeCount++
                    onPauseOrDispose { }
                }
                val allowed = remember(resumeCount) { Notifier.canNotify(this@MainActivity) }

                // Nach einem Update einmal zeigen, was neu ist (abschaltbar); null = kein Dialog
                var changelog by remember { mutableStateOf<List<String>?>(null) }
                // true = nach einem Update von selbst geöffnet, false = aus den Einstellungen
                var changelogAuto by remember { mutableStateOf(true) }
                LaunchedEffect(settings?.onboardingDone, settings?.changelogSeen, settings?.showChangelog) {
                    val st = settings ?: return@LaunchedEffect
                    val current = BuildConfig.VERSION_CODE
                    if (!st.onboardingDone || st.changelogSeen >= current) return@LaunchedEffect
                    val lines = if (st.showChangelog) Changelog.linesSince(this@MainActivity, st.changelogSeen, current) else emptyList()
                    if (lines.isEmpty()) {
                        vm.setChangelogSeen(current)
                    } else {
                        changelogAuto = true
                        changelog = lines
                    }
                }

                // App-Abkürzungen passend zu den eingeschalteten Bereichen (und in der gewählten Sprache)
                LaunchedEffect(settings?.onboardingDone, settings?.showMoments, settings?.showLive) {
                    settings?.let { Shortcuts.publish(this@MainActivity, it) }
                }
                // Tipp auf eine Abkürzung: neuen Moment anlegen oder zum Live-Zähler
                val jump by vm.jump.collectAsStateWithLifecycle()
                var homeJump by remember { mutableStateOf<String?>(null) }
                LaunchedEffect(jump, settings?.onboardingDone) {
                    val target = jump ?: return@LaunchedEffect
                    if (settings?.onboardingDone != true) return@LaunchedEffect
                    vm.jump.value = null
                    showSettings = false
                    showLicenses = false
                    editingPhoto = false
                    if (target == "moment") {
                        momentSuggestion = null
                        editingMoment = ""
                    } else {
                        editingMoment = null
                        homeJump = target
                    }
                }

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

                // Weitere Fotos für die Diashow im Titelbild
                val slidePicker = rememberLauncherForActivityResult(
                    ActivityResultContracts.PickMultipleVisualMedia(MAX_SLIDES),
                ) { uris -> if (uris.isNotEmpty()) vm.addSlides(uris) }
                fun pickSlides() = slidePicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )

                // Sicherung als Datei über den Dateidialog von Android, ganz ohne Internet
                val backupSaver = rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument(Backup.MIME),
                ) { uri -> if (uri != null) vm.exportBackup(uri) }
                val backupLoader = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument(),
                ) { uri -> if (uri != null) vm.importBackup(uri) }
                // Abgleich: QR-Code des anderen Handys scannen und Abgleich-Dateien von Hand öffnen
                var scanningFor by rememberSaveable { mutableStateOf<String?>(null) }
                val scanner = rememberLauncherForActivityResult(ScanContract()) { result ->
                    if (result.contents != null) {
                        val ok = vm.acceptPairing(result.contents, scanningFor)
                        Toast.makeText(
                            this@MainActivity,
                            getString(if (ok) R.string.sync_paired_now else R.string.sync_bad_qr),
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
                fun scanPairing(me: String?) {
                    scanningFor = me
                    scanner.launch(
                        ScanOptions()
                            .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                            .setPrompt(getString(R.string.sync_scan_prompt))
                            .setBeepEnabled(false)
                            .setOrientationLocked(false),
                    )
                }
                // Ordner für die automatische Sicherung
                val folderPicker = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocumentTree(),
                ) { uri -> if (uri != null) vm.enableAutoBackup(uri) }
                val syncOpener = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument(),
                ) { uri -> if (uri != null) vm.importSync(uri) }

                fun saveBackup() = backupSaver.launch(Backup.fileName(today))
                // Alle Dateien zeigen: manche Dateimanager melden ZIP-Dateien mit seltsamem Typ,
                // ob es wirklich eine Sicherung ist, prüft die App beim Laden selbst
                fun loadBackup() = backupLoader.launch(arrayOf("*/*"))

                BackHandler(enabled = showSettings || editingPhoto || editingMoment != null || showLicenses) {
                    when {
                        showLicenses -> showLicenses = false
                        editingMoment != null -> editingMoment = null
                        editingPhoto -> editingPhoto = false
                        else -> showSettings = false
                    }
                }

                val s = settings
                val screen = when {
                    s == null -> "loading"
                    editingPhoto && s.photoPath != null -> "editor"
                    editingMoment != null && s.onboardingDone -> "moment"
                    !s.onboardingDone -> "onboarding"
                    showSettings && showLicenses -> "licenses"
                    showSettings -> "settings"
                    else -> "home"
                }
                // Im Juni (Pride Month) ist die App ein bisschen bunter
                CompositionLocalProvider(LocalPrideMonth provides isPrideMonth(today)) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    if (locked) {
                        LockScreen(onUnlock = ::unlock)
                    } else {
                        AnimatedContent(
                            targetState = screen,
                            transitionSpec = {
                                // Tiefer hinein (Einstellungen, Lizenzen) gleitet von rechts herein, zurück wieder hinaus
                                if (targetState == "licenses" || (targetState == "settings" && initialState != "licenses")) {
                                    (slideInHorizontally { it / 3 } + fadeIn()) togetherWith fadeOut()
                                } else if (initialState == "settings" || initialState == "licenses") {
                                    fadeIn() togetherWith (slideOutHorizontally { it / 3 } + fadeOut())
                                } else {
                                    fadeIn() togetherWith fadeOut()
                                }
                            },
                            label = "screen",
                        ) { target ->
                            if (s == null) return@AnimatedContent
                            when (target) {
                                "licenses" -> LicensesScreen(onBack = { showLicenses = false })
                                "moment" -> MomentEditorScreen(
                                    existing = s.moments.firstOrNull { it.id == editingMoment },
                                    suggestedTitle = momentSuggestion,
                                    defaultDate = today,
                                    onClose = { editingMoment = null },
                                    onSave = { moment, photo, remove ->
                                        vm.saveMoment(moment, photo, remove)
                                        editingMoment = null
                                    },
                                    onDelete = {
                                        vm.deleteMoment(it.id)
                                        editingMoment = null
                                    },
                                    relationships = s.relationships.map { it.id to s.namesOf(it) },
                                    defaultRelationship = if (s.showAll) null else s.relationship.id,
                                    onShare = { ShareCard.shareMoment(this@MainActivity, s, it, today) },
                                )
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
                                    onStartTime = vm::setStartTime,
                                    onFinish = {
                                        vm.setOnboardingDone(true)
                                        showSettings = false
                                    },
                                    step = onboardingStep,
                                    onStep = { onboardingStep = it },
                                    onRestoreBackup = ::loadBackup,
                                )
                                "settings" -> SettingsScreen(
                                    settings = s,
                                    notificationsAllowed = allowed,
                                    onBack = { showSettings = false },
                                    onPickPhoto = ::pickPhoto,
                                    onPreset = vm::setPreset,
                                    onResetPhoto = vm::resetPhoto,
                                    onNotifications = ::setNotifications,
                                    onNotifyTime = vm::setNotifyTime,
                                    onRemindBefore = vm::setRemindBefore,
                                onFont = vm::setFont,
                                    onTestNotification = {
                                        if (allowed) vm.sendTestNotification() else askPermission()
                                    },
                                    onAddWidget = ::pinWidget,
                                    onAdjustPhoto = { editingPhoto = true },
                                    onRestartOnboarding = {
                                        onboardingStep = 0
                                        vm.setOnboardingDone(false)
                                    },
                                    onResetAll = {
                                        setAppLock(false)
                                        onboardingStep = 0
                                        showSettings = false
                                        vm.resetAll()
                                    },
                                    onUpdateCheck = vm::setUpdateCheck,
                                    onShowMoments = vm::setShowMoments,
                                    onShowLive = vm::setShowLive,
                                    onCheckUpdates = vm::checkUpdatesNow,
                                    onOpenUpdate = vm::installUpdate,
                                    updateProgress = updateProgress,
                                    onOpenLicenses = { showLicenses = true },
                                    onAddSlides = ::pickSlides,
                                    onRemoveSlide = vm::removeSlide,
                                    onExportBackup = ::saveBackup,
                                    onImportBackup = ::loadBackup,
                                    autoBackup = autoBackup,
                                    onShowChangelog = vm::setShowChangelog,
                                    onOpenChangelog = {
                                        val lines = Changelog.linesSince(this@MainActivity, BuildConfig.VERSION_CODE - 1, BuildConfig.VERSION_CODE)
                                        if (lines.isNotEmpty()) {
                                            changelogAuto = false
                                            changelog = lines
                                        }
                                    },
                                    onAutoBackup = { on -> if (on) folderPicker.launch(null) else vm.disableAutoBackup() },
                                    language = remember { AppLanguage.current(this@MainActivity) },
                                    onLanguage = { AppLanguage.set(this@MainActivity, it) },
                                    onSavePerson = vm::savePerson,
                                    onDeletePerson = vm::deletePerson,
                                    onSaveRelationship = vm::saveRelationship,
                                    onDeleteRelationship = vm::deleteRelationship,
                                    onWidgetRelationship = vm::setWidgetRelationship,
                                    onDiscreet = vm::setDiscreet,
                                    appLock = lockOn,
                                    onAppLock = ::setAppLock,
                                    pairing = pairing,
                                    sync = SyncActions(
                                        onCreate = vm::createPairing,
                                        onScan = ::scanPairing,
                                        onMe = vm::setMe,
                                        onUnpair = vm::unpair,
                                        onSend = { vm.shareSync() },
                                        onOpen = { syncOpener.launch(arrayOf("*/*")) },
                                    ),
                                )
                                else -> HomeScreen(
                                    s,
                                    today,
                                    onOpenSettings = { showSettings = true },
                                    onPageChange = vm::setHomePage,
                                    onAddMoment = { title ->
                                        momentSuggestion = title
                                        editingMoment = ""
                                    },
                                    onOpenMoment = {
                                        momentSuggestion = null
                                        editingMoment = it.id
                                    },
                                    onSelect = vm::setSelected,
                                    jumpTo = homeJump,
                                    onJumped = { homeJump = null },
                                )
                            }
                        }
                        changelog?.let { lines ->
                            val close = {
                                vm.setChangelogSeen(BuildConfig.VERSION_CODE)
                                changelog = null
                            }
                            ChangelogDialog(
                                lines,
                                onClose = close,
                                onNeverAgain = if (changelogAuto) {
                                    {
                                        vm.setShowChangelog(false)
                                        close()
                                    }
                                } else {
                                    null
                                },
                            )
                        }
                    }
                }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_LOCKED, locked)
        outState.putLong(KEY_SAVED_AT, System.currentTimeMillis())
    }

    override fun onStop() {
        super.onStop()
        leftAt = SystemClock.elapsedRealtime()
    }

    override fun onStart() {
        super.onStart()
        // Nach mehr als einer Minute im Hintergrund wieder sperren
        if (lockOn && leftAt != 0L && AppLock.shouldLock(leftAt)) {
            locked = true
            autoPrompted = false
        }
    }

    override fun onResume() {
        super.onResume()
        // Einmal von selbst fragen; wer abbricht, kann über den Knopf erneut entsperren
        if (locked && !autoPrompted) {
            autoPrompted = true
            unlock()
        }
    }

    private fun unlock() {
        if (!AppLock.canLock(this)) {
            // Displaysperre am Handy entfernt: dann kann auch die App nicht mehr gesperrt bleiben
            setAppLock(false)
            return
        }
        if (AppLock.prompt(this) { locked = false }) return
        // Bis Android 10 oder wenn der Systemdialog nicht geht: Displaysperre des Handys.
        // Geht auch das nicht, lieber entsperren als die App unbenutzbar zu machen.
        val intent = AppLock.credentialIntent(this)
        if (intent == null || runCatching { credential.launch(intent) }.isFailure) locked = false
    }

    private fun setAppLock(on: Boolean) {
        if (on && !AppLock.canLock(this)) {
            Toast.makeText(this, getString(R.string.lock_no_screen_lock), Toast.LENGTH_LONG).show()
            return
        }
        AppLock.setEnabled(this, on)
        lockOn = on
        if (!on) locked = false
        AppLock.hideInRecents(this, on)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /**
     * Tipp auf die Update-Mitteilung: Update direkt laden und installieren.
     * Geöffnete oder geteilte Abgleich-Datei: mit den eigenen Daten zusammenführen.
     * App-Abkürzung: neuer Moment oder Live-Zähler.
     */
    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            Notifier.ACTION_INSTALL_UPDATE -> vm.installUpdate()
            Shortcuts.ACTION_ADD_MOMENT -> vm.jump.value = "moment"
            Shortcuts.ACTION_LIVE -> vm.jump.value = "live"
            Intent.ACTION_VIEW -> intent.data?.let(vm::importSync)
            Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                ?.let(vm::importSync)
        }
    }

    private companion object {
        const val KEY_LOCKED = "locked"
        const val KEY_SAVED_AT = "saved_at"
    }

    private fun pinWidget() {
        val manager = getSystemService(AppWidgetManager::class.java)
        if (manager.isRequestPinAppWidgetSupported) {
            manager.requestPinAppWidget(ComponentName(this, PhotoWidgetReceiver::class.java), null, null)
        } else {
            Toast.makeText(
                this,
                getString(R.string.widget_manual_hint),
                Toast.LENGTH_LONG,
            ).show()
        }
    }
}
