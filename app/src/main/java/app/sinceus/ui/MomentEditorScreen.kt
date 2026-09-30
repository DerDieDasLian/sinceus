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

/**
 * Neuen Moment anlegen ([existing] = null, optional mit [suggestedTitle]) oder bestehenden bearbeiten.
 * [onSave] bekommt den Moment, ein neu gewähltes Foto und ob das alte Foto entfernt werden soll.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentEditorScreen(
    existing: Moment?,
    suggestedTitle: String?,
    defaultDate: LocalDate,
    onClose: () -> Unit,
    onSave: (Moment, Uri?, Boolean) -> Unit,
    onDelete: (Moment) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(existing?.title ?: suggestedTitle.orEmpty()) }
    var epochDay by rememberSaveable { mutableStateOf((existing?.date ?: defaultDate).toEpochDay()) }
    var note by rememberSaveable { mutableStateOf(existing?.note.orEmpty()) }
    var remind by rememberSaveable { mutableStateOf(existing?.yearlyReminder ?: true) }
    var newPhoto by rememberSaveable { mutableStateOf<String?>(null) }
    var removePhoto by rememberSaveable { mutableStateOf(false) }
    var dateDialog by rememberSaveable { mutableStateOf(false) }
    var deleteDialog by rememberSaveable { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            newPhoto = uri.toString()
            removePhoto = false
        }
    }
    val shownPhoto: Any? = when {
        newPhoto != null -> Uri.parse(newPhoto)
        !removePhoto && existing?.photoPath != null -> File(existing.photoPath)
        else -> null
    }

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

            if (shownPhoto != null) {
                AsyncImage(
                    model = shownPhoto,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f)
                        .clip(RoundedCornerShape(20.dp)),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text(stringResource(R.string.moment_change_photo)) }
                    TextButton(onClick = {
                        newPhoto = null
                        removePhoto = true
                    }) { Text(stringResource(R.string.moment_remove_photo)) }
                }
            } else {
                FilledTonalButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.AddPhotoAlternate, null)
                    Text(stringResource(R.string.moment_add_photo), Modifier.padding(start = 8.dp))
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
                        photoPath = existing?.photoPath,
                        yearlyReminder = remind,
                    )
                    onSave(moment, newPhoto?.let(Uri::parse), removePhoto)
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
