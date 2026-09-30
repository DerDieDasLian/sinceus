package de.loveapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
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
import de.loveapp.data.LoveSettings
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val DateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")

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
) {
    var dialog by remember { mutableStateOf<String?>(null) }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Einstellungen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zurück")
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
            Section("Eure Daten") {
                Row(Icons.Rounded.People, "Namen", settings.names) { dialog = "names" }
                Divider()
                Row(Icons.Rounded.CalendarMonth, "Zusammen seit dem", settings.startDate.format(DateFmt)) {
                    dialog = "date"
                }
            }
            Section("Foto") {
                Row(Icons.Rounded.AddPhotoAlternate, "Eigenes Foto auswählen", null, onClick = onPickPhoto)
                Divider()
                if (settings.photoPath != null) {
                    Row(Icons.Rounded.Crop, "Bildausschnitt anpassen", null, onClick = onAdjustPhoto)
                    Divider()
                }
                Row(Icons.Rounded.Collections, "Aus Standardmotiven auswählen", null) { dialog = "presets" }
                Divider()
                Row(Icons.Rounded.RestartAlt, "Foto zurücksetzen", null, onClick = onResetPhoto)
            }
            Section("Mitteilungen") {
                ListItem(
                    headlineContent = { Text("Mitteilung an besonderen Tagen") },
                    supportingContent = {
                        Text(
                            if (settings.notificationsEnabled && !notificationsAllowed) {
                                "Mitteilungen sind in den Android-Einstellungen blockiert"
                            } else {
                                "Monatstage, Jahrestage, 100er-Tage und mehr"
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
                        "Uhrzeit",
                        "%02d:%02d Uhr".format(settings.notifyHour, settings.notifyMinute),
                    ) { dialog = "time" }
                    Divider()
                    Row(Icons.Rounded.Send, "Testmitteilung senden", null, onClick = onTestNotification)
                }
            }
            Section("Verschiedenes") {
                Row(Icons.Rounded.Widgets, "Widget zum Startbildschirm hinzufügen", null, onClick = onAddWidget)
                Divider()
                Row(Icons.Rounded.AutoAwesome, "Einrichtung erneut starten", null, onClick = onRestartOnboarding)
                Divider()
                Row(Icons.Rounded.Shield, "Datenschutz", null) { dialog = "privacy" }
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
        "time" -> TimeDialog(settings, onDismiss = { dialog = null }) { h, m ->
            onNotifyTime(h, m)
            dialog = null
        }
        "presets" -> PresetDialog(settings.presetIndex, onDismiss = { dialog = null }) {
            onPreset(it)
            dialog = null
        }
        "privacy" -> AlertDialog(
            onDismissRequest = { dialog = null },
            icon = { Icon(Icons.Rounded.Shield, null) },
            title = { Text("Datenschutz") },
            text = {
                Text(
                    "Diese App ist nur für euch. Namen, Datum und Foto werden ausschließlich " +
                        "auf diesem Gerät gespeichert. Die App hat keinen Internetzugriff, " +
                        "sendet keine Daten und enthält kein Tracking und keine Werbung.",
                )
            },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text("Okay") } },
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
        title = { Text("Eure Namen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(a, { a = it }, label = { Text("Name 1") }, singleLine = true)
                OutlinedTextField(b, { b = it }, label = { Text("Name 2") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(a, b) }, enabled = a.isNotBlank() && b.isNotBlank()) {
                Text("Speichern")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDialog(current: LocalDate, onDismiss: () -> Unit, onSave: (LocalDate) -> Unit) {
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
            }) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    ) {
        DatePicker(state = state, title = { Text("Zusammen seit", Modifier.padding(start = 24.dp, top = 16.dp)) })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimeDialog(settings: LoveSettings, onDismiss: () -> Unit, onSave: (Int, Int) -> Unit) {
    val state = rememberTimePickerState(settings.notifyHour, settings.notifyMinute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Uhrzeit der Mitteilung") },
        text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state) } },
        confirmButton = { TextButton(onClick = { onSave(state.hour, state.minute) }) { Text("Speichern") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

@Composable
internal fun PresetDialog(selected: Int, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Standardmotive") },
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
                                p.name,
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
        confirmButton = { TextButton(onClick = onDismiss) { Text("Schließen") } },
    )
}
