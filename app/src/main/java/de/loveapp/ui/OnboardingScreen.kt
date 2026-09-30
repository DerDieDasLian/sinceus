package de.loveapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.loveapp.data.LoveSettings
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

private const val STEPS = 5

/** Einrichtung beim ersten Start: Namen, Datum, Foto, Mitteilungen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    settings: LoveSettings,
    notificationsAllowed: Boolean,
    onNames: (String, String) -> Unit,
    onStartDate: (LocalDate) -> Unit,
    onPickPhoto: () -> Unit,
    onAdjustPhoto: () -> Unit,
    onPreset: (Int) -> Unit,
    onNotifications: (Boolean) -> Unit,
    onNotifyTime: (Int, Int) -> Unit,
    onFinish: () -> Unit,
    onStartTime: (LocalTime?) -> Unit,
    step: Int,
    onStep: (Int) -> Unit,
) {
    var name1 by rememberSaveable { mutableStateOf(settings.name1) }
    var name2 by rememberSaveable { mutableStateOf(settings.name2) }
    var presets by remember { mutableStateOf(false) }
    var time by remember { mutableStateOf(false) }
    var startTime by remember { mutableStateOf(false) }
    val date = rememberDatePickerState(
        initialSelectedDateMillis = settings.startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )

    BackHandler(enabled = step > 0) { onStep(step - 1) }

    fun next() {
        when (step) {
            1 -> onNames(name1, name2)
            2 -> date.selectedDateMillis?.let {
                onStartDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
            }
        }
        if (step == STEPS - 1) onFinish() else onStep(step + 1)
    }

    val progress by animateFloatAsState((step + 1f) / STEPS, label = "progress")

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding(),
    ) {
        if (step > 0) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            )
        }
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally { dir * it / 3 } + fadeIn()) togetherWith
                    (slideOutHorizontally { -dir * it / 3 } + fadeOut())
            },
            label = "step",
            modifier = Modifier.weight(1f),
        ) { s ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (s) {
                    0 -> Welcome()
                    1 -> Step("Wie heißt ihr?", "Die Namen stehen groß auf eurem Foto.") {
                        val caps = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                        OutlinedTextField(
                            name1, { name1 = it }, label = { Text("Name 1") }, singleLine = true,
                            keyboardOptions = caps, modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            name2, { name2 = it }, label = { Text("Name 2") }, singleLine = true,
                            keyboardOptions = caps, modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    2 -> Step("Seit wann seid ihr zusammen?", "Ab diesem Tag zählt die App.") {
                        Card(
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        ) { DatePicker(state = date, title = null, headline = null, showModeToggle = true) }
                        FilledTonalButton(
                            onClick = { startTime = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                        ) {
                            Icon(Icons.Rounded.Schedule, null)
                            Text(
                                settings.startTime?.let { "Uhrzeit: %02d:%02d Uhr".format(it.hour, it.minute) }
                                    ?: "Uhrzeit hinzufügen (optional)",
                                Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                    3 -> Step("Euer Foto", "Wählt ein Bild von euch beiden. Den Ausschnitt könnt ihr danach anpassen.") {
                        Box(
                            Modifier
                                .fillMaxWidth(0.75f)
                                .aspectRatio(0.8f)
                                .clip(RoundedCornerShape(28.dp)),
                        ) {
                            CouplePhoto(settings, Modifier.fillMaxSize())
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(PhotoScrim),
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                        Button(onClick = onPickPhoto, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Rounded.AddPhotoAlternate, null)
                            Text("Foto auswählen", Modifier.padding(start = 8.dp))
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilledTonalButton(onClick = { presets = true }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.Collections, null)
                                Text("Motive", Modifier.padding(start = 8.dp))
                            }
                            if (settings.photoPath != null) {
                                FilledTonalButton(onClick = onAdjustPhoto, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Rounded.OpenWith, null)
                                    Text("Anpassen", Modifier.padding(start = 8.dp))
                                }
                            }
                        }
                    }
                    else -> Step(
                        "Besondere Tage nicht verpassen",
                        "Die App meldet sich an Monatstagen, Jahrestagen, runden Tagen wie 100, 200, 300 " +
                            "und Schnapszahlen wie 222.",
                    ) {
                        Icon(
                            Icons.Rounded.NotificationsActive,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(72.dp),
                        )
                        Spacer(Modifier.height(24.dp))
                        if (settings.notificationsEnabled && notificationsAllowed) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                                Text("Mitteilungen sind aktiv", Modifier.padding(start = 8.dp))
                            }
                        } else {
                            Button(onClick = { onNotifications(true) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Mitteilungen erlauben")
                            }
                        }
                        FilledTonalButton(
                            onClick = { time = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                        ) {
                            Icon(Icons.Rounded.Schedule, null)
                            Text(
                                "Uhrzeit: %02d:%02d Uhr".format(settings.notifyHour, settings.notifyMinute),
                                Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Namen und Datum sind Pflicht, nur das Foto kann warten
            if (step == 3) {
                TextButton(onClick = { onStep(step + 1) }) { Text("Überspringen") }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = ::next,
                enabled = step != 1 || (name1.isNotBlank() && name2.isNotBlank()),
            ) {
                Text(
                    when (step) {
                        0 -> "Los geht's"
                        STEPS - 1 -> "Fertig"
                        else -> "Weiter"
                    },
                )
            }
        }
    }

    if (presets) {
        PresetDialog(settings.presetIndex, onDismiss = { presets = false }) {
            onPreset(it)
            presets = false
        }
    }
    if (time) {
        TimeDialog(
            "Uhrzeit der Mitteilung",
            LocalTime.of(settings.notifyHour, settings.notifyMinute),
            onDismiss = { time = false },
        ) {
            onNotifyTime(it.hour, it.minute)
            time = false
        }
    }
    if (startTime) {
        TimeDialog(
            "Uhrzeit (optional)",
            settings.startTime,
            onDismiss = { startTime = false },
            onClear = {
                onStartTime(null)
                startTime = false
            },
        ) {
            onStartTime(it)
            startTime = false
        }
    }
}

@Composable
private fun Welcome() {
    Spacer(Modifier.height(96.dp))
    Box(
        Modifier
            .size(160.dp)
            .clip(RoundedCornerShape(48.dp))
            .background(Presets[0].brush),
        contentAlignment = Alignment.Center,
    ) {
        BeatingHeart(size = 80.dp)
    }
    Text(
        "Willkommen",
        style = MaterialTheme.typography.displayMedium,
        modifier = Modifier.padding(top = 40.dp),
    )
    Text(
        "Zählt eure gemeinsame Zeit und erinnert euch an alle besonderen Tage.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp),
    )
}

@Composable
private fun Step(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 24.dp),
    )
    Text(
        subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp, bottom = 28.dp),
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, content = content)
}
