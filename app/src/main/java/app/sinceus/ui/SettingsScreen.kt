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
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Update
import app.sinceus.data.Texts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: LoveSettings,
    notificationsAllowed: Boolean,
    onBack: () -> Unit,
    onNames: (String, String) -> Unit,
    onStartDate: (LocalDate) -> Unit,
    onPickPhoto: () -> Unit,
    onPreset: (Int) -> Unit,
    onResetPhoto: () -> Unit,
    onNotifications: (Boolean) -> Unit,
    onNotifyTime: (Int, Int) -> Unit,
    onTestNotification: () -> Unit,
    onAddWidget: () -> Unit,
    onAdjustPhoto: () -> Unit,
    onRestartOnboarding: () -> Unit,
    onStartTime: (LocalTime?) -> Unit,
    onResetAll: () -> Unit,
    onUpdateCheck: (Boolean) -> Unit = {},
    onCheckUpdates: () -> Unit = {},
    onOpenUpdate: () -> Unit = {},
    onShowMoments: (Boolean) -> Unit = {},
    /** Download-Fortschritt eines Updates in Prozent (-1 = unbekannt), null = kein Download */
    updateProgress: Int? = null,
    onOpenLicenses: () -> Unit = {},
) {
    var dialog by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Section(stringResource(R.string.section_your_data)) {
                Row(Icons.Rounded.People, stringResource(R.string.names), settings.names) { dialog = "names" }
                Divider()
                Row(
                    Icons.Rounded.CalendarMonth,
                    stringResource(R.string.together_since_date),
                    Texts.mediumDate(settings.startDate),
                ) {
                    dialog = "date"
                }
                Divider()
                Row(
                    Icons.Rounded.Schedule,
                    stringResource(R.string.start_time_optional),
                    settings.startTime?.let { Texts.time(context, it) } ?: stringResource(R.string.not_set),
                ) { dialog = "startTime" }
            }
            Section(stringResource(R.string.section_photo)) {
                Row(Icons.Rounded.AddPhotoAlternate, stringResource(R.string.pick_own_photo), null, onClick = onPickPhoto)
                Divider()
                if (settings.photoPath != null) {
                    Row(Icons.Rounded.Crop, stringResource(R.string.adjust_crop), null, onClick = onAdjustPhoto)
                    Divider()
                }
                Row(Icons.Rounded.Collections, stringResource(R.string.pick_preset), null) { dialog = "presets" }
                Divider()
                Row(Icons.Rounded.RestartAlt, stringResource(R.string.reset_photo), null, onClick = onResetPhoto)
            }
            Section(stringResource(R.string.section_notifications)) {
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
                    Row(Icons.Rounded.Send, stringResource(R.string.send_test), null, onClick = onTestNotification)
                }
            }
            if (BuildConfig.UPDATE_CHECK) {
                Section(stringResource(R.string.section_updates)) {
                    if (settings.updateVersion != null) {
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
            Section(stringResource(R.string.section_misc)) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.moments_show)) },
                    supportingContent = { Text(stringResource(R.string.moments_show_summary)) },
                    leadingContent = { Icon(Icons.Rounded.AutoStories, null) },
                    trailingContent = { Switch(checked = settings.showMoments, onCheckedChange = onShowMoments) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onShowMoments(!settings.showMoments) },
                )
                Divider()
                Row(Icons.Rounded.Widgets, stringResource(R.string.add_widget), null, onClick = onAddWidget)
                Divider()
                Row(Icons.Rounded.AutoAwesome, stringResource(R.string.restart_setup), null, onClick = onRestartOnboarding)
                Divider()
                Row(Icons.Rounded.Shield, stringResource(R.string.privacy), null) { dialog = "privacy" }
                Divider()
                Row(Icons.Rounded.Description, stringResource(R.string.licenses), null, onClick = onOpenLicenses)
                Divider()
                Row(
                    Icons.Rounded.Info,
                    stringResource(R.string.version),
                    stringResource(R.string.version_value, BuildConfig.VERSION_NAME, BuildConfig.FLAVOR),
                ) {}
                Divider()
                ListItem(
                    headlineContent = { Text(stringResource(R.string.delete_all), color = MaterialTheme.colorScheme.error) },
                    supportingContent = { Text(stringResource(R.string.delete_all_summary)) },
                    leadingContent = { Icon(Icons.Rounded.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { dialog = "reset" },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    when (dialog) {
        "names" -> NamesDialog(settings, onDismiss = { dialog = null }) { a, b ->
            onNames(a, b)
            dialog = null
        }
        "date" -> DateDialog(settings.startDate, onDismiss = { dialog = null }) {
            onStartDate(it)
            dialog = null
        }
        "time" -> TimeDialog(
            stringResource(R.string.notify_time_title),
            LocalTime.of(settings.notifyHour, settings.notifyMinute),
            onDismiss = { dialog = null },
        ) {
            onNotifyTime(it.hour, it.minute)
            dialog = null
        }
        "startTime" -> TimeDialog(
            stringResource(R.string.start_time_optional),
            settings.startTime,
            onDismiss = { dialog = null },
            onClear = {
                onStartTime(null)
                dialog = null
            },
        ) {
            onStartTime(it)
            dialog = null
        }
        "reset" -> ResetDialog(onDismiss = { dialog = null }) {
            dialog = null
            onResetAll()
        }
        "presets" -> PresetDialog(settings.presetIndex, onDismiss = { dialog = null }) {
            onPreset(it)
            dialog = null
        }
        "privacy" -> AlertDialog(
            onDismissRequest = { dialog = null },
            icon = { Icon(Icons.Rounded.Shield, null) },
            title = { Text(stringResource(R.string.privacy)) },
            text = {
                Text(stringResource(R.string.privacy_text))
            },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.ok)) } },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Text(
        title.uppercase(),
        style = LabelCaps,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
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

@Composable
private fun NamesDialog(settings: LoveSettings, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var a by remember { mutableStateOf(settings.name1) }
    var b by remember { mutableStateOf(settings.name2) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.your_names)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(a, { a = it }, label = { Text(stringResource(R.string.name_1)) }, singleLine = true)
                OutlinedTextField(b, { b = it }, label = { Text(stringResource(R.string.name_2)) }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(a, b) }, enabled = a.isNotBlank() && b.isNotBlank()) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
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

@Composable
internal fun ResetDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.DeleteForever, null) },
        title = { Text(stringResource(R.string.delete_all_question)) },
        text = {
            Text(stringResource(R.string.delete_all_text))
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text(stringResource(R.string.delete_all_confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
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
                            .clickable { onPick(i) },
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
