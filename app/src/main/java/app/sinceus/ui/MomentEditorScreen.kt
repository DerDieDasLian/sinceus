package app.sinceus.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.sinceus.R
import app.sinceus.data.Moment
import app.sinceus.data.Texts
import coil3.compose.AsyncImage
import java.io.File
import java.time.LocalDate
import java.util.UUID
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Close
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Neuen Moment anlegen ([existing] = null, optional mit [suggestedTitle]) oder bestehenden bearbeiten.
 * [onSave] bekommt den Moment und die Fotos in gewünschter Reihenfolge: Pfade bisheriger Fotos
 * und content-Uris neu gewählter Fotos. Das erste Foto ist das Titelbild.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MomentEditorScreen(
    existing: Moment?,
    suggestedTitle: String?,
    defaultDate: LocalDate,
    onClose: () -> Unit,
    onSave: (Moment, List<String>) -> Unit,
    onDelete: (Moment) -> Unit,
    /** Beziehungen zur Auswahl als ID und Titel; nur bei mehreren wird gefragt, zu wem der Moment gehört */
    relationships: List<Pair<String, String>> = emptyList(),
    /** Vorauswahl für neue Momente, null = für alle */
    defaultRelationship: String? = null,
    /** Gespeicherten Moment als Bild teilen */
    onShare: (Moment) -> Unit = {},
) {
    var title by rememberSaveable { mutableStateOf(existing?.title ?: suggestedTitle.orEmpty()) }
    var epochDay by rememberSaveable { mutableStateOf((existing?.date ?: defaultDate).toEpochDay()) }
    var note by rememberSaveable { mutableStateOf(existing?.note.orEmpty()) }
    var remind by rememberSaveable { mutableStateOf(existing?.yearlyReminder ?: true) }
    var relationship by rememberSaveable {
        mutableStateOf(if (existing != null) existing.relationshipId else defaultRelationship)
    }
    // Bisherige Fotos als Pfad, neu gewählte als content-Uri
    var photos by rememberSaveable { mutableStateOf(ArrayList(existing?.photos.orEmpty())) }
    var dateDialog by rememberSaveable { mutableStateOf(false) }
    var deleteDialog by rememberSaveable { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        if (uris.isNotEmpty()) photos = ArrayList((photos + uris.map { it.toString() }).distinct())
    }
    fun pick() = picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    BackHandler(onBack = onClose)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (existing == null) R.string.moment_new else R.string.moment_edit)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (existing != null) {
                        IconButton(onClick = { onShare(existing) }) {
                            Icon(Icons.Rounded.Share, contentDescription = stringResource(R.string.moment_share))
                        }
                        IconButton(onClick = { deleteDialog = true }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = stringResource(R.string.moment_delete))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.moment_field_title)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.moment_field_date)) },
                supportingContent = {
                    Text(Texts.longDate(LocalDate.ofEpochDay(epochDay)), color = MaterialTheme.colorScheme.primary)
                },
                leadingContent = { Icon(Icons.Rounded.CalendarMonth, null) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { dateDialog = true },
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.moment_field_note)) },
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            if (photos.isNotEmpty()) {
                AsyncImage(
                    model = photoModel(photos.first()),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f)
                        .clip(RoundedCornerShape(20.dp)),
                )
                if (photos.size > 1) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(photos, key = { _, item -> item }) { index, item ->
                            PhotoThumb(
                                model = photoModel(item),
                                isCover = index == 0,
                                position = stringResource(R.string.moment_photo_position, index + 1, photos.size),
                                onClick = { photos = ArrayList(listOf(item) + (photos - item)) },
                                onRemove = { photos = ArrayList(photos - item) },
                            )
                        }
                    }
                    Text(
                        stringResource(R.string.moment_cover_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = ::pick) {
                        Icon(Icons.Rounded.AddPhotoAlternate, null)
                        Text(stringResource(R.string.moment_more_photos), Modifier.padding(start = 8.dp))
                    }
                    if (photos.size == 1) {
                        TextButton(onClick = { photos = ArrayList() }) { Text(stringResource(R.string.moment_remove_photo)) }
                    }
                }
            } else {
                FilledTonalButton(onClick = ::pick, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.AddPhotoAlternate, null)
                    Text(stringResource(R.string.moment_add_photo), Modifier.padding(start = 8.dp))
                }
            }

            if (relationships.size > 1) {
                Column {
                    Text(
                        stringResource(R.string.moment_for),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = relationship == null,
                            onClick = { relationship = null },
                            label = { Text(stringResource(R.string.poly_all)) },
                        )
                        relationships.forEach { (id, name) ->
                            FilterChip(
                                selected = relationship == id,
                                onClick = { relationship = id },
                                label = { Text(name) },
                            )
                        }
                    }
                }
            }

            ListItem(
                headlineContent = { Text(stringResource(R.string.moment_remind)) },
                supportingContent = { Text(stringResource(R.string.moment_remind_summary)) },
                trailingContent = { Switch(checked = remind, onCheckedChange = { remind = it }) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { remind = !remind },
            )

            Button(
                onClick = {
                    val moment = Moment(
                        id = existing?.id ?: UUID.randomUUID().toString(),
                        title = title.trim(),
                        date = LocalDate.ofEpochDay(epochDay),
                        note = note.trim(),
                        yearlyReminder = remind,
                        relationshipId = relationship?.takeIf { id -> relationships.any { it.first == id } },
                    )
                    onSave(moment, photos)
                },
                enabled = title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            ) { Text(stringResource(R.string.save)) }
        }
    }

    if (dateDialog) {
        DateDialog(
            LocalDate.ofEpochDay(epochDay),
            onDismiss = { dateDialog = false },
            title = R.string.moment_field_date,
        ) {
            epochDay = it.toEpochDay()
            dateDialog = false
        }
    }
    if (deleteDialog && existing != null) {
        AlertDialog(
            onDismissRequest = { deleteDialog = false },
            icon = { Icon(Icons.Rounded.DeleteOutline, null) },
            title = { Text(stringResource(R.string.moment_delete_question)) },
            text = { Text(stringResource(R.string.moment_delete_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteDialog = false
                        onDelete(existing)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { deleteDialog = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

/** Bisherige Fotos liegen als Pfad im App-Speicher, neu gewählte sind content-Uris */
private fun photoModel(item: String): Any = if (item.startsWith("/")) File(item) else Uri.parse(item)

/** Vorschaubild in der Fotoleiste: Tippen macht es zum Titelbild, das Kreuz entfernt es */
@Composable
private fun PhotoThumb(model: Any, isCover: Boolean, position: String, onClick: () -> Unit, onRemove: () -> Unit) {
    val makeCover = stringResource(R.string.moment_make_cover)
    Box(
        Modifier
            .size(84.dp)
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (isCover) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp)) else Modifier,
            )
            .semantics { contentDescription = position }
            .clickable(enabled = !isCover, onClickLabel = makeCover, onClick = onClick),
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(36.dp),
        ) {
            Box(
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.moment_remove_photo),
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
