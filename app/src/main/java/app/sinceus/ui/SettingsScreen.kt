package app.sinceus.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.sinceus.data.LoveSettings
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import app.sinceus.BuildConfig
import app.sinceus.R
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Update
import app.sinceus.data.Texts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CardGiftcard
import app.sinceus.data.REMIND_OPTIONS
import app.sinceus.data.AutoBackup
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Slideshow
import app.sinceus.data.MAX_SLIDES
import coil3.compose.AsyncImage
import java.io.File
import app.sinceus.AppLanguage
import app.sinceus.data.MAX_PEOPLE
import app.sinceus.data.MAX_RELATIONSHIPS
import app.sinceus.data.Names
import app.sinceus.data.Person
import app.sinceus.data.Pairing
import androidx.compose.material.icons.rounded.Sync
import app.sinceus.data.Relationship
import app.sinceus.data.newId
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.FilterChip
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: LoveSettings,
    notificationsAllowed: Boolean,
    onBack: () -> Unit = {},
    onPickPhoto: () -> Unit = {},
    onPreset: (Int) -> Unit = {},
    onResetPhoto: () -> Unit = {},
    onNotifications: (Boolean) -> Unit = {},
    onNotifyTime: (Int, Int) -> Unit = { _, _ -> },
    onRemindBefore: (Int) -> Unit = {},
    onTestNotification: () -> Unit = {},
    onAddWidget: () -> Unit = {},
    onAdjustPhoto: () -> Unit = {},
    onRestartOnboarding: () -> Unit = {},
    onResetAll: () -> Unit = {},
    onUpdateCheck: (Boolean) -> Unit = {},
    onCheckUpdates: () -> Unit = {},
    onOpenUpdate: () -> Unit = {},
    onShowMoments: (Boolean) -> Unit = {},
    onShowLive: (Boolean) -> Unit = {},
    /** Download-Fortschritt eines Updates in Prozent (-1 = unbekannt), null = kein Download */
    updateProgress: Int? = null,
    onOpenLicenses: () -> Unit = {},
    onAddSlides: () -> Unit = {},
    onRemoveSlide: (String) -> Unit = {},
    onExportBackup: () -> Unit = {},
    onImportBackup: () -> Unit = {},
    /** Automatische Sicherung, null = aus */
    autoBackup: AutoBackup.State? = null,
    onAutoBackup: (Boolean) -> Unit = {},
    onShowChangelog: (Boolean) -> Unit = {},
    /** Neuigkeiten dieser Version zeigen */
    onOpenChangelog: () -> Unit = {},
    /** Gewählte App-Sprache, "" = wie das Handy */
    language: String = "",
    onLanguage: (String) -> Unit = {},
    onSavePerson: (Person) -> Unit = {},
    onDeletePerson: (String) -> Unit = {},
    onSaveRelationship: (Relationship) -> Unit = {},
    onDeleteRelationship: (String) -> Unit = {},
    onWidgetRelationship: (String) -> Unit = {},
    onDiscreet: (Boolean) -> Unit = {},
    onFont: (String) -> Unit = {},
    /** App-Sperre mit Fingerabdruck, Gesicht oder PIN */
    appLock: Boolean = false,
    onAppLock: (Boolean) -> Unit = {},
    /** Kopplung für den Abgleich, null = nicht gekoppelt */
    pairing: Pairing? = null,
    sync: SyncActions = SyncActions(),
    /** Geöffnete Unterseite beim Start (für Tests), null = Übersicht */
    initialPage: SettingsPage? = null,
) {
    var dialog by remember { mutableStateOf<String?>(null) }
    var page by rememberSaveable { mutableStateOf(initialPage) }
    BackHandler(enabled = page != null) { page = null }
    val context = LocalContext.current
    val uri = androidx.compose.ui.platform.LocalUriHandler.current
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(page?.title ?: R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = { if (page != null) page = null else onBack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        // Jede Seite startet oben
        key(page) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (page) {
                    null -> Overview(
                        settings,
                        notificationsAllowed,
                        pairing,
                        appLock = appLock,
                        languageLabel = languageLabel(language),
                        onLanguage = { dialog = "language" },
                        onOpen = { page = it },
                    )
                    SettingsPage.Couple -> {
                        if (!settings.isPoly) {
                            Text(
                                stringResource(R.string.poly_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                        Section(stringResource(R.string.section_people)) {
                            settings.people.forEachIndexed { i, p ->
                                if (i > 0) Divider()
                                ListItem(
                                    headlineContent = { Text(p.name.ifBlank { "?" }) },
                                    supportingContent = listOfNotNull(p.pronouns.ifBlank { null }, p.birthday?.let(Texts::mediumDate))
                                        .joinToString(" · ").ifBlank { null }?.let { { Text(it) } },
                                    leadingContent = { Avatar(p.name) },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    modifier = Modifier.clickable { dialog = "person:${p.id}" },
                                )
                            }
                            if (settings.people.size < MAX_PEOPLE) {
                                Divider()
                                Row(Icons.Rounded.PersonAdd, stringResource(R.string.add_person), null) { dialog = "person:" }
                            }
                        }
                        Section(stringResource(R.string.section_relationships)) {
                            settings.relationships.forEachIndexed { i, r ->
                                if (i > 0) Divider()
                                ListItem(
                                    headlineContent = { Text(settings.namesOf(r)) },
                                    supportingContent = {
                                        Text(
                                            listOfNotNull(
                                                r.label.ifBlank { null },
                                                stringResource(R.string.together_since_long, Texts.mediumDate(r.startDate)),
                                            ).joinToString(" · "),
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    },
                                    leadingContent = { Icon(Icons.Rounded.Favorite, null) },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    modifier = Modifier.clickable { dialog = "rel:${r.id}" },
                                )
                            }
                            if (settings.relationships.size < MAX_RELATIONSHIPS) {
                                Divider()
                                Row(Icons.Rounded.Add, stringResource(R.string.add_relationship), null) { dialog = "rel:" }
                            }
                        }
                    }
                    SettingsPage.Photo -> Section(null) {
                        Row(Icons.Rounded.AddPhotoAlternate, stringResource(R.string.pick_own_photo), null, onClick = onPickPhoto)
                        Divider()
                        if (settings.photoPath != null) {
                            Row(Icons.Rounded.Crop, stringResource(R.string.adjust_crop), null, onClick = onAdjustPhoto)
                            Divider()
                        }
                        Row(Icons.Rounded.Collections, stringResource(R.string.pick_preset), null) { dialog = "presets" }
                        Divider()
                        Row(
                            Icons.Rounded.Slideshow,
                            stringResource(R.string.slideshow),
                            if (settings.slides.isEmpty()) {
                                stringResource(R.string.slideshow_off)
                            } else {
                                pluralStringResource(R.plurals.slideshow_count, settings.slides.size, settings.slides.size.toString())
                            },
                        ) { dialog = "slides" }
                        Divider()
                        Row(Icons.Rounded.RestartAlt, stringResource(R.string.reset_photo), null, onClick = onResetPhoto)
                        Divider()
                        Row(Icons.Rounded.TextFields, stringResource(R.string.font_title), AppFonts.find(settings.font).label()) { dialog = "font" }
                    }
                    SettingsPage.Privacy -> Section(null) {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.discreet)) },
                            supportingContent = { Text(stringResource(R.string.discreet_summary)) },
                            leadingContent = { Icon(Icons.Rounded.VisibilityOff, null) },
                            trailingContent = { Switch(checked = settings.discreet, onCheckedChange = onDiscreet) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { onDiscreet(!settings.discreet) },
                        )
                        Divider()
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.app_lock)) },
                            supportingContent = { Text(stringResource(R.string.app_lock_summary)) },
                            leadingContent = { Icon(Icons.Rounded.Lock, null) },
                            trailingContent = { Switch(checked = appLock, onCheckedChange = onAppLock) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { onAppLock(!appLock) },
                        )
                    }
                    SettingsPage.Notifications -> Section(null) {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.notify_special)) },
                            supportingContent = {
                                Text(
                                    if (settings.notificationsEnabled && !notificationsAllowed) {
                                        stringResource(R.string.notify_blocked)
                                    } else {
                                        stringResource(R.string.notify_summary)
                                    },
                                )
                            },
                            leadingContent = { Icon(Icons.Rounded.NotificationsActive, null) },
                            trailingContent = {
                                Switch(checked = settings.notificationsEnabled, onCheckedChange = onNotifications)
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { onNotifications(!settings.notificationsEnabled) },
                        )
                        if (settings.notificationsEnabled) {
                            Divider()
                            Row(
                                Icons.Rounded.Schedule,
                                stringResource(R.string.notify_time),
                                Texts.time(context, LocalTime.of(settings.notifyHour, settings.notifyMinute)),
                            ) { dialog = "time" }
                            Divider()
                            Row(
                                Icons.Rounded.Alarm,
                                stringResource(R.string.remind_before),
                                stringResource(remindLabel(settings.remindBefore)),
                            ) { dialog = "remind" }
                            Divider()
                            Row(Icons.Rounded.Send, stringResource(R.string.send_test), null, onClick = onTestNotification)
                        }
                    }
                    SettingsPage.Home -> Section(null) {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.moments_show)) },
                            supportingContent = { Text(stringResource(R.string.moments_show_summary)) },
                            leadingContent = { Icon(Icons.Rounded.AutoStories, null) },
                            trailingContent = { Switch(checked = settings.showMoments, onCheckedChange = onShowMoments) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { onShowMoments(!settings.showMoments) },
                        )
                        Divider()
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.live_show)) },
                            supportingContent = { Text(stringResource(R.string.live_show_summary)) },
                            leadingContent = { Icon(Icons.Rounded.Timer, null) },
                            trailingContent = { Switch(checked = settings.showLive, onCheckedChange = onShowLive) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { onShowLive(!settings.showLive) },
                        )
                        Divider()
                        Row(Icons.Rounded.Widgets, stringResource(R.string.add_widget), null, onClick = onAddWidget)
                    }
                    SettingsPage.Sync -> SyncPage(settings, pairing, sync)
                    SettingsPage.Backup -> Section(null) {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.backup_export)) },
                            supportingContent = { Text(stringResource(R.string.backup_export_summary)) },
                            leadingContent = { Icon(Icons.Rounded.Backup, null) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable(onClick = onExportBackup),
                        )
                        Divider()
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.backup_import)) },
                            supportingContent = { Text(stringResource(R.string.backup_import_summary)) },
                            leadingContent = { Icon(Icons.Rounded.Restore, null) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { dialog = "import" },
                        )
                        Divider()
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.backup_auto)) },
                            supportingContent = {
                                Text(
                                    when {
                                        autoBackup == null -> stringResource(R.string.backup_auto_summary)
                                        autoBackup.last > 0 -> stringResource(
                                            R.string.backup_auto_last,
                                            autoBackup.folderName,
                                            Texts.mediumDate(
                                                java.time.Instant.ofEpochMilli(autoBackup.last).atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
                                            ),
                                        )
                                        else -> stringResource(R.string.backup_auto_folder, autoBackup.folderName)
                                    },
                                )
                            },
                            leadingContent = { Icon(Icons.Rounded.EventRepeat, null) },
                            trailingContent = { Switch(checked = autoBackup != null, onCheckedChange = onAutoBackup) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { onAutoBackup(autoBackup == null) },
                        )
                    }
                    SettingsPage.About -> {
                        if (BuildConfig.UPDATE_CHECK) {
                            Section(stringResource(R.string.section_updates)) {
                            // Nach einem Update kann noch die alte Meldung gespeichert sein
                            if (settings.updateVersion != null &&
                                app.sinceus.update.UpdateChecker.isNewer(settings.updateVersion, BuildConfig.VERSION_NAME)
                            ) {
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            stringResource(R.string.update_available, settings.updateVersion),
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    },
                                    supportingContent = {
                                        Text(
                                            when {
                                                updateProgress == null -> stringResource(R.string.update_tap_to_open)
                                                updateProgress < 0 -> stringResource(R.string.update_downloading)
                                                else -> stringResource(R.string.update_downloading_percent, updateProgress)
                                            },
                                        )
                                    },
                                    leadingContent = { Icon(Icons.Rounded.SystemUpdate, null, tint = MaterialTheme.colorScheme.primary) },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    modifier = Modifier.clickable(enabled = updateProgress == null, onClick = onOpenUpdate),
                                )
                                Divider()
                            }
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.update_auto)) },
                                supportingContent = { Text(stringResource(R.string.update_auto_summary)) },
                                leadingContent = { Icon(Icons.Rounded.Update, null) },
                                trailingContent = { Switch(checked = settings.updateCheck, onCheckedChange = onUpdateCheck) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable { onUpdateCheck(!settings.updateCheck) },
                            )
                            Divider()
                            Row(Icons.Rounded.Refresh, stringResource(R.string.update_check_now), null, onClick = onCheckUpdates)
                            }
                        }
                        Section(if (BuildConfig.UPDATE_CHECK) stringResource(R.string.settings_about) else null) {
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.changelog_setting)) },
                                supportingContent = { Text(stringResource(R.string.changelog_setting_summary)) },
                                leadingContent = { Icon(Icons.Rounded.NewReleases, null) },
                                trailingContent = { Switch(checked = settings.showChangelog, onCheckedChange = onShowChangelog) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable { onShowChangelog(!settings.showChangelog) },
                            )
                            Divider()
                            Row(Icons.Rounded.Public, stringResource(R.string.website), null) { uri.openUri(Web.page("index", context)) }
                            Divider()
                            Row(Icons.Rounded.Shield, stringResource(R.string.privacy), null) { dialog = "privacy" }
                            Divider()
                            Row(Icons.Rounded.Gavel, stringResource(R.string.imprint), null) { uri.openUri(Web.page("imprint", context)) }
                            Divider()
                            Row(Icons.Rounded.Description, stringResource(R.string.licenses), null, onClick = onOpenLicenses)
                            Divider()
                            Row(
                                Icons.Rounded.Info,
                                stringResource(R.string.version),
                                stringResource(R.string.version_value, BuildConfig.VERSION_NAME, BuildConfig.FLAVOR),
                                onClick = onOpenChangelog,
                            )
                        }
                    }
                    SettingsPage.Reset -> {
                        Text(
                            stringResource(R.string.reset_intro),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                        Section(null) {
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.backup_export)) },
                                supportingContent = { Text(stringResource(R.string.reset_backup_summary)) },
                                leadingContent = { Icon(Icons.Rounded.Backup, null) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable(onClick = onExportBackup),
                            )
                        }
                        Section(null) {
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.restart_setup)) },
                                supportingContent = { Text(stringResource(R.string.restart_setup_summary)) },
                                leadingContent = { Icon(Icons.Rounded.AutoAwesome, null) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable { dialog = "restart" },
                            )
                            Divider()
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.delete_all), color = MaterialTheme.colorScheme.error) },
                                supportingContent = { Text(stringResource(R.string.delete_all_summary)) },
                                leadingContent = { Icon(Icons.Rounded.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable { dialog = "reset" },
                            )
                        }
                    }
                }
                if (page == null) {
                    Spacer(Modifier.height(8.dp))
                    RainbowCard(stringResource(R.string.all_couples), stringResource(R.string.all_couples_text))
                    Credit()
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    val current = dialog
    when {
        current == null -> {}
        current.startsWith("person:") -> {
            val id = current.removePrefix("person:")
            val fresh = remember(current) { Person(newId(), "") }
            PersonDialog(
                person = settings.person(id) ?: fresh,
                isNew = settings.person(id) == null,
                canDelete = settings.people.size > 2 && settings.person(id) != null,
                onDismiss = { dialog = null },
                onDelete = {
                    onDeletePerson(id)
                    dialog = null
                },
            ) {
                onSavePerson(it)
                dialog = null
            }
        }
        current.startsWith("rel:") -> {
            val id = current.removePrefix("rel:")
            val existing = settings.relationships.firstOrNull { it.id == id }
            val fresh = remember(current) { Relationship(newId(), emptyList(), LocalDate.now()) }
            RelationshipDialog(
                settings = settings,
                relationship = existing ?: fresh,
                isNew = existing == null,
                onDismiss = { dialog = null },
                onDelete = {
                    onDeleteRelationship(id)
                    dialog = null
                },
                onWidget = onWidgetRelationship,
            ) {
                onSaveRelationship(it)
                dialog = null
            }
        }
    }
    when (current) {
        "remind" -> AlertDialog(
            onDismissRequest = { dialog = null },
            icon = { Icon(Icons.Rounded.Alarm, null) },
            title = { Text(stringResource(R.string.remind_before)) },
            text = {
                Column {
                    REMIND_OPTIONS.forEach { days ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(selected = settings.remindBefore == days, role = Role.RadioButton) {
                                    onRemindBefore(days)
                                    dialog = null
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = settings.remindBefore == days, onClick = null)
                            Text(stringResource(remindLabel(days)), Modifier.padding(start = 16.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.close)) } },
        )
        "time" -> TimeDialog(
            stringResource(R.string.notify_time_title),
            LocalTime.of(settings.notifyHour, settings.notifyMinute),
            onDismiss = { dialog = null },
        ) {
            onNotifyTime(it.hour, it.minute)
            dialog = null
        }
        "reset" -> ResetDialog(onDismiss = { dialog = null }, onBackup = onExportBackup) {
            dialog = null
            onResetAll()
        }
        "restart" -> AlertDialog(
            onDismissRequest = { dialog = null },
            icon = { Icon(Icons.Rounded.AutoAwesome, null) },
            title = { Text(stringResource(R.string.restart_setup_question)) },
            text = { Text(stringResource(R.string.restart_setup_text)) },
            confirmButton = {
                TextButton(onClick = {
                    dialog = null
                    onRestartOnboarding()
                }) { Text(stringResource(R.string.restart_setup_confirm)) }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.cancel)) } },
        )
        "language" -> LanguageDialog(language, onDismiss = { dialog = null }) {
            dialog = null
            onLanguage(it)
        }
        "font" -> FontDialog(settings.font, onSelect = onFont, onDismiss = { dialog = null })
        "presets" -> PresetDialog(settings.presetIndex, onDismiss = { dialog = null }) {
            onPreset(it)
            dialog = null
        }
        "slides" -> SlidesDialog(
            slides = settings.slides,
            onAdd = onAddSlides,
            onRemove = onRemoveSlide,
            onDismiss = { dialog = null },
        )
        "import" -> AlertDialog(
            onDismissRequest = { dialog = null },
            icon = { Icon(Icons.Rounded.Restore, null) },
            title = { Text(stringResource(R.string.backup_import_question)) },
            text = { Text(stringResource(R.string.backup_import_text)) },
            confirmButton = {
                TextButton(onClick = {
                    dialog = null
                    onImportBackup()
                }) { Text(stringResource(R.string.backup_import_confirm)) }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.cancel)) } },
        )
        "privacy" -> AlertDialog(
            onDismissRequest = { dialog = null },
            icon = { Icon(Icons.Rounded.Shield, null) },
            title = { Text(stringResource(R.string.privacy)) },
            text = {
                Text(stringResource(R.string.privacy_text))
            },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.ok)) } },
            dismissButton = {
                TextButton(onClick = { uri.openUri(Web.page("privacy", context)) }) { Text(stringResource(R.string.privacy_full)) }
            },
        )
    }
}

/** Ganz unten in den Einstellungen */
@Composable
private fun Credit() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val style = MaterialTheme.typography.bodySmall
        val color = MaterialTheme.colorScheme.onSurfaceVariant
        Text(stringResource(R.string.credit_before), style = style, color = color)
        Icon(
            Icons.Rounded.Favorite,
            contentDescription = stringResource(R.string.credit_heart),
            tint = if (LocalPrideMonth.current) Color.White else Color(0xFFFF5C77),
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .size(14.dp)
                .then(if (LocalPrideMonth.current) Modifier.rainbow() else Modifier),
        )
        Text(stringResource(R.string.credit_after), style = style, color = color)
    }
}

/** Unterseiten der Einstellungen */
/** Bereiche der Einstellungen, auf der Übersicht in Gruppen sortiert (Reihenfolge = Anzeige) */
enum class SettingsPage(
    @androidx.annotation.StringRes val title: Int,
    val icon: ImageVector,
    @androidx.annotation.StringRes val group: Int,
) {
    Couple(R.string.settings_couple, Icons.Rounded.People, R.string.settings_group_love),
    Notifications(R.string.section_notifications, Icons.Rounded.NotificationsActive, R.string.settings_group_love),
    Photo(R.string.section_photo, Icons.Rounded.Palette, R.string.settings_group_app),
    Home(R.string.settings_home, Icons.Rounded.Dashboard, R.string.settings_group_app),
    Privacy(R.string.section_privacy, Icons.Rounded.Lock, R.string.settings_group_app),
    Backup(R.string.section_backup, Icons.Rounded.Backup, R.string.settings_group_data),
    Sync(R.string.section_sync, Icons.Rounded.Sync, R.string.settings_group_data),
    Reset(R.string.section_reset, Icons.Rounded.RestartAlt, R.string.settings_group_data),
    About(R.string.settings_about, Icons.Rounded.Info, R.string.settings_group_info),
}

/** Startseite der Einstellungen: ein Eintrag pro Bereich mit kurzer Zusammenfassung */
@Composable
private fun Overview(
    settings: LoveSettings,
    notificationsAllowed: Boolean,
    pairing: Pairing?,
    appLock: Boolean,
    languageLabel: String,
    onLanguage: () -> Unit,
    onOpen: (SettingsPage) -> Unit,
) {
    val context = LocalContext.current
    val updateReady = BuildConfig.UPDATE_CHECK && settings.updateVersion != null &&
        app.sinceus.update.UpdateChecker.isNewer(settings.updateVersion, BuildConfig.VERSION_NAME)
    val photo = buildList {
        add(
            if (settings.photoPath != null) {
                stringResource(R.string.photo_own)
            } else {
                stringResource(Presets.getOrElse(settings.presetIndex) { Presets[0] }.name)
            },
        )
        if (settings.slides.isNotEmpty()) {
            add(pluralStringResource(R.plurals.slideshow_count, settings.slides.size, settings.slides.size.toString()))
        }
    }.joinToString(", ")
    val summaries = mapOf(
        SettingsPage.Couple to if (settings.relationships.size > 1) {
            pluralStringResource(R.plurals.relationships_count, settings.relationships.size, settings.relationships.size.toString()) +
                ", " + Names.join(settings.people.map { it.name })
        } else {
            "${settings.names}, ${Texts.mediumDate(settings.startDate)}"
        },
        SettingsPage.Photo to photo + ", " + AppFonts.find(settings.font).label(),
        SettingsPage.Privacy to listOfNotNull(
            stringResource(if (appLock) R.string.app_lock_on else R.string.app_lock_off),
            if (settings.discreet) stringResource(R.string.discreet) else null,
        ).joinToString(", "),
        SettingsPage.Notifications to when {
            !settings.notificationsEnabled -> stringResource(R.string.notify_off)
            !notificationsAllowed -> stringResource(R.string.notify_blocked)
            else -> stringResource(
                R.string.notify_daily_at,
                Texts.time(context, LocalTime.of(settings.notifyHour, settings.notifyMinute)),
            )
        },
        SettingsPage.Home to stringResource(R.string.settings_home_summary),
        SettingsPage.Backup to stringResource(R.string.settings_backup_summary),
        SettingsPage.Sync to stringResource(if (pairing != null) R.string.sync_paired else R.string.sync_summary),
        SettingsPage.Reset to stringResource(R.string.reset_summary),
        SettingsPage.About to if (updateReady) {
            stringResource(R.string.update_available, settings.updateVersion.orEmpty())
        } else {
            stringResource(R.string.version_value, BuildConfig.VERSION_NAME, BuildConfig.FLAVOR)
        },
    )
    SettingsPage.entries.groupBy { it.group }.forEach { (group, pages) ->
      Section(stringResource(group)) {
        pages.forEachIndexed { i, p ->
            if (i > 0) Divider()
            val highlight = p == SettingsPage.About && updateReady
            ListItem(
                headlineContent = { Text(stringResource(p.title)) },
                supportingContent = {
                    Text(
                        summaries.getValue(p),
                        color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                leadingContent = { Icon(p.icon, null) },
                trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { onOpen(p) },
            )
        }
        // Sprache direkt hier, ohne eigene Unterseite
        if (group == R.string.settings_group_app) {
            Divider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.language)) },
                supportingContent = { Text(languageLabel, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingContent = { Icon(Icons.Rounded.Language, null) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable(onClick = onLanguage),
            )
        }
      }
    }
}

@Composable
private fun Section(title: String?, content: @Composable () -> Unit) {
    if (title != null) {
        Text(
            title.uppercase(),
            style = LabelCaps,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
        )
    } else {
        Spacer(Modifier.height(8.dp))
    }
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) { Column { content() } }
}

@Composable
private fun Divider() = HorizontalDivider(
    Modifier.padding(horizontal = 16.dp),
    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
)

@Composable
private fun Row(icon: ImageVector, title: String, value: String?, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = value?.let { { Text(it, color = MaterialTheme.colorScheme.primary) } },
        leadingContent = { Icon(icon, null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

/** Runder Anfangsbuchstabe als Bild für einen Menschen */
@Composable
private fun Avatar(name: String) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            Names.initial(name).dropLast(1),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/** Name, Pronomen und Geburtstag eines Menschen bearbeiten */
@Composable
private fun PersonDialog(
    person: Person,
    isNew: Boolean,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSave: (Person) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(person.name) }
    var pronouns by rememberSaveable { mutableStateOf(person.pronouns) }
    var birthday by rememberSaveable { mutableStateOf(person.birthday?.toEpochDay()) }
    var pickBirthday by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val caps = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (isNew) R.string.add_person else R.string.edit_person)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    name, { name = it },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true,
                    keyboardOptions = caps,
                )
                OutlinedTextField(
                    pronouns, { pronouns = it },
                    label = { Text(stringResource(R.string.pronouns)) },
                    placeholder = { Text(stringResource(R.string.pronouns_hint)) },
                    singleLine = true,
                )
                // Geburtstag ist freiwillig und wird nur hier gefragt, nicht beim Einrichten
                ListItem(
                    headlineContent = { Text(stringResource(R.string.birthday_optional)) },
                    supportingContent = {
                        Text(
                            birthday?.let { Texts.mediumDate(LocalDate.ofEpochDay(it)) } ?: stringResource(R.string.not_set),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                    leadingContent = { Icon(Icons.Rounded.CardGiftcard, null) },
                    trailingContent = birthday?.let {
                        {
                            IconButton(onClick = { birthday = null }) {
                                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.birthday_remove))
                            }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { pickBirthday = true },
                )
                if (canDelete) {
                    TextButton(
                        onClick = { if (confirmDelete) onDelete() else confirmDelete = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text(stringResource(if (confirmDelete) R.string.remove_person_confirm else R.string.remove_person))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(person.copy(name = name, pronouns = pronouns, birthday = birthday?.let(LocalDate::ofEpochDay))) },
                enabled = name.isNotBlank(),
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
    if (pickBirthday) {
        DateDialog(
            birthday?.let(LocalDate::ofEpochDay) ?: LocalDate.now().minusYears(25),
            onDismiss = { pickBirthday = false },
            title = R.string.birthday,
        ) {
            birthday = it.toEpochDay()
            pickBirthday = false
        }
    }
}

/** Wer gehört zur Beziehung, seit wann, wie heißt sie und gibt es Mitteilungen */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RelationshipDialog(
    settings: LoveSettings,
    relationship: Relationship,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onWidget: (String) -> Unit,
    onSave: (Relationship) -> Unit,
) {
    var draft by remember { mutableStateOf(relationship) }
    var pick by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val canDelete = !isNew && settings.relationships.size > 1
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (isNew) R.string.add_relationship else R.string.edit_relationship)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.relationship_members), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    settings.people.forEach { p ->
                        val selected = p.id in draft.members
                        FilterChip(
                            selected = selected,
                            onClick = {
                                draft = draft.copy(members = if (selected) draft.members - p.id else draft.members + p.id)
                            },
                            label = { Text(p.name) },
                        )
                    }
                }
                OutlinedTextField(
                    draft.label,
                    { draft = draft.copy(label = it) },
                    label = { Text(stringResource(R.string.relationship_label)) },
                    placeholder = { Text(stringResource(R.string.relationship_label_hint)) },
                    singleLine = true,
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.together_since_date)) },
                    supportingContent = { Text(Texts.mediumDate(draft.startDate), color = MaterialTheme.colorScheme.primary) },
                    leadingContent = { Icon(Icons.Rounded.CalendarMonth, null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { pick = "date" },
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.start_time_optional)) },
                    supportingContent = {
                        Text(
                            draft.startTime?.let { Texts.time(context, it) } ?: stringResource(R.string.not_set),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                    leadingContent = { Icon(Icons.Rounded.Schedule, null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { pick = "time" },
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.relationship_notify)) },
                    leadingContent = { Icon(Icons.Rounded.NotificationsActive, null) },
                    trailingContent = { Switch(checked = draft.notify, onCheckedChange = { draft = draft.copy(notify = it) }) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { draft = draft.copy(notify = !draft.notify) },
                )
                if (!isNew && settings.relationships.size > 1) {
                    val inWidget = settings.widget.id == relationship.id
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.relationship_widget)) },
                        leadingContent = { Icon(Icons.Rounded.Widgets, null) },
                        trailingContent = {
                            RadioButton(selected = inWidget, onClick = { onWidget(relationship.id) })
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { onWidget(relationship.id) },
                    )
                }
                if (canDelete) {
                    TextButton(
                        onClick = { if (confirmDelete) onDelete() else confirmDelete = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text(
                            stringResource(
                                if (confirmDelete) R.string.remove_relationship_confirm else R.string.remove_relationship,
                            ),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(draft) }, enabled = draft.members.size >= 2) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
    when (pick) {
        "date" -> DateDialog(draft.startDate, onDismiss = { pick = null }) {
            draft = draft.copy(startDate = it)
            pick = null
        }
        "time" -> TimeDialog(
            stringResource(R.string.start_time_optional),
            draft.startTime,
            onDismiss = { pick = null },
            onClear = {
                draft = draft.copy(startTime = null)
                pick = null
            },
        ) {
            draft = draft.copy(startTime = it)
            pick = null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateDialog(
    current: LocalDate,
    onDismiss: () -> Unit,
    @androidx.annotation.StringRes title: Int = R.string.together_since_title,
    onSave: (LocalDate) -> Unit,
) {
    // Der DatePicker rechnet in UTC-Millisekunden
    val state = rememberDatePickerState(
        initialSelectedDateMillis = current.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let {
                    onSave(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                }
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    ) {
        DatePicker(state = state, title = { Text(stringResource(title), Modifier.padding(start = 24.dp, top = 16.dp)) })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimeDialog(
    title: String,
    time: LocalTime?,
    onDismiss: () -> Unit,
    onClear: (() -> Unit)? = null,
    onSave: (LocalTime) -> Unit,
) {
    val state = rememberTimePickerState(
        time?.hour ?: 12,
        time?.minute ?: 0,
        is24Hour = android.text.format.DateFormat.is24HourFormat(LocalContext.current),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state) } },
        confirmButton = {
            TextButton(onClick = { onSave(LocalTime.of(state.hour, state.minute)) }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            Row {
                if (onClear != null && time != null) TextButton(onClick = onClear) { Text(stringResource(R.string.remove)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}

/** Alles löschen: erst eine Sicherung anbieten, dann nur durch langes Drücken bestätigen */
@Composable
internal fun ResetDialog(onDismiss: () -> Unit, onBackup: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(stringResource(R.string.delete_all_question)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.delete_all_text))
                OutlinedButton(onClick = onBackup, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Backup, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.delete_all_backup_first))
                }
                HoldButton(stringResource(R.string.delete_all_hold), onConfirm)
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Wie lange der Löschen-Knopf gedrückt gehalten werden muss */
internal const val HOLD_MS = 3000

/** Knopf, der sich beim Gedrückthalten füllt und erst nach [HOLD_MS] auslöst */
@Composable
internal fun HoldButton(text: String, onConfirm: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val confirm by rememberUpdatedState(onConfirm)
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.errorContainer)
            .semantics {
                role = Role.Button
                // Mit TalkBack ist ein Doppeltipp schon Bestätigung genug
                onClick(label = text) {
                    confirm()
                    true
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    val hold = scope.launch {
                        progress.animateTo(1f, tween(HOLD_MS, easing = LinearEasing))
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        confirm()
                    }
                    tryAwaitRelease()
                    if (progress.value < 1f) {
                        hold.cancel()
                        scope.launch { progress.animateTo(0f, tween(250)) }
                    }
                })
            }
            .testTag("hold_button"),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.value)
                .background(MaterialTheme.colorScheme.error),
        )
        Text(
            text,
            color = if (progress.value > 0.5f) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onErrorContainer,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

/** Auswahl der App-Sprache */
@Composable
private fun LanguageDialog(current: String, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Language, null) },
        title = { Text(stringResource(R.string.language)) },
        text = {
            Column {
                AppLanguage.OPTIONS.forEach { tag ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onPick(tag) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = tag == current, onClick = { onPick(tag) })
                        Text(languageLabel(tag))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Sprachen stehen immer in ihrer eigenen Sprache da, damit man sie auch in der falschen Sprache findet */
@Composable
private fun languageLabel(tag: String) = when (tag) {
    "de" -> "Deutsch"
    "en" -> "English"
    else -> stringResource(R.string.language_system)
}

@Composable
internal fun PresetDialog(selected: Int, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backgrounds)) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                itemsIndexed(Presets) { i, p ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        border = if (i == selected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .aspectRatio(0.8f)
                            .clip(RoundedCornerShape(16.dp))
                            .selectable(selected = i == selected, role = Role.RadioButton) { onPick(i) },
                    ) {
                        Box {
                            PresetBackground(p, Modifier.fillMaxSize())
                            Text(
                                stringResource(p.name),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(10.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

/** Weitere Fotos für die Diashow im Titelbild: ansehen, hinzufügen, entfernen */
@Composable
private fun SlidesDialog(slides: List<String>, onAdd: () -> Unit, onRemove: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Slideshow, null) },
        title = { Text(stringResource(R.string.slideshow)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(if (slides.isEmpty()) R.string.slideshow_empty else R.string.slideshow_hint))
                if (slides.isNotEmpty()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(slides, key = { it }) { path ->
                            Box(
                                Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp)),
                            ) {
                                AsyncImage(
                                    model = File(path),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                IconButton(
                                    onClick = { onRemove(path) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.55f)),
                                ) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = stringResource(R.string.slideshow_remove),
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onAdd, enabled = slides.size < MAX_SLIDES) { Text(stringResource(R.string.slideshow_add)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

/** Text für die Vorab-Erinnerung: aus, 1 Tag oder 1 Woche vorher */
private fun remindLabel(days: Int): Int = when (days) {
    1 -> R.string.remind_day
    7 -> R.string.remind_week
    else -> R.string.remind_off
}

/** Seiten der Website, auf Deutsch oder Englisch passend zur App-Sprache */
private object Web {
    private const val BASE = "https://derdiedaslian.github.io/sinceus/"

    fun page(name: String, context: android.content.Context): String {
        val de = context.resources.configuration.locales[0].language == "de"
        return BASE + when (name) {
            "privacy" -> if (de) "privacy-de.html" else "privacy-en.html"
            "imprint" -> if (de) "impressum.html" else "legal-notice.html"
            else -> if (de) "index-de.html" else ""
        }
    }
}
