package app.sinceus.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.sinceus.R
import app.sinceus.data.Distance
import app.sinceus.data.LoveSettings
import app.sinceus.data.PeopleCodec
import app.sinceus.data.RelationKind
import app.sinceus.data.Relationship
import app.sinceus.data.Texts
import app.sinceus.data.Travel

/**
 * Eine Beziehung bearbeiten, als eigene Seite mit Abschnitten
 * (Allgemein, Davor, Fernbeziehung, Mitteilungen) statt in einem langen Fenster.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun RelationshipPage(
    settings: LoveSettings,
    relationship: Relationship,
    isNew: Boolean,
    onClose: () -> Unit,
    onDelete: () -> Unit,
    onWidget: (String) -> Unit,
    onSave: (Relationship) -> Unit,
) {
    var draft by remember { mutableStateOf(relationship) }
    var pick by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var askSave by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val canDelete = !isNew && settings.relationships.size > 1
    val valid = draft.members.size >= 2 && (draft.before == null || draft.earlier != null)
    val changed = draft != relationship
    val back = { if (changed) askSave = true else onClose() }
    // Bei Änderungen bleibt die Seite stehen und fragt nach
    val backPreview = rememberBackPreview(stays = changed, onBack = back)
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val clear = ListItemDefaults.colors(containerColor = Color.Transparent)

    Scaffold(
        modifier = Modifier.backPreview(backPreview).nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(if (isNew) R.string.add_relationship else R.string.edit_relationship)) },
                navigationIcon = {
                    IconButton(onClick = back) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(onClick = { onSave(draft) }, enabled = valid) { Text(stringResource(R.string.save)) }
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
            Section(stringResource(R.string.rel_section_general)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    Text(stringResource(R.string.relationship_kind), style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RelationKind.entries.forEach { k ->
                            FilterChip(
                                selected = draft.kind == k,
                                // Hat die frühere Phase dieselbe Art, tauschen beide
                                onClick = { draft = draft.copy(kind = k, before = if (draft.before == k) draft.kind else draft.before) },
                                label = { Text(stringResource(k.title)) },
                                leadingIcon = { Icon(k.icon, null, Modifier.size(18.dp)) },
                            )
                        }
                    }
                    OutlinedTextField(
                        draft.label,
                        { draft = draft.copy(label = it) },
                        label = { Text(stringResource(R.string.relationship_label)) },
                        placeholder = { Text(stringResource(R.string.relationship_label_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        draft.startTitle,
                        { draft = draft.copy(startTitle = it.take(PeopleCodec.MAX_TITLE)) },
                        label = { Text(stringResource(R.string.relationship_start_title)) },
                        placeholder = { Text(stringResource(R.string.relationship_start_title_hint)) },
                        supportingText = { Text(stringResource(R.string.relationship_start_title_help)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Divider()
                ListItem(
                    headlineContent = { Text(stringResource(R.string.together_since_date)) },
                    supportingContent = { Text(Texts.mediumDate(draft.startDate), color = MaterialTheme.colorScheme.primary) },
                    leadingContent = { Icon(Icons.Rounded.CalendarMonth, null) },
                    colors = clear,
                    modifier = Modifier.clickable { pick = "date" },
                )
                Divider()
                ListItem(
                    headlineContent = { Text(stringResource(R.string.start_time_optional)) },
                    supportingContent = {
                        Text(
                            draft.startTime?.let { Texts.time(context, it) } ?: stringResource(R.string.not_set),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                    leadingContent = { Icon(Icons.Rounded.Schedule, null) },
                    colors = clear,
                    modifier = Modifier.clickable { pick = "time" },
                )
            }

            // Frühere Phase, z. B. erst befreundet und dann ein Paar
            Section(stringResource(R.string.rel_section_before)) {
                val hasBefore = draft.before != null
                val toggleBefore = {
                    draft = if (hasBefore) {
                        draft.copy(before = null, beforeStart = null)
                    } else {
                        draft.copy(
                            before = RelationKind.entries.first { it != draft.kind },
                            beforeStart = draft.beforeStart ?: draft.startDate.minusYears(1),
                        )
                    }
                }
                ListItem(
                    headlineContent = { Text(stringResource(R.string.relationship_before)) },
                    supportingContent = { Text(stringResource(R.string.relationship_before_summary)) },
                    leadingContent = { Icon(Icons.Rounded.History, null) },
                    trailingContent = { Switch(checked = hasBefore, onCheckedChange = { toggleBefore() }) },
                    colors = clear,
                    modifier = Modifier.clickable { toggleBefore() },
                )
                if (hasBefore) {
                    FlowRow(
                        Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RelationKind.entries.filter { it != draft.kind }.forEach { k ->
                            FilterChip(
                                selected = draft.before == k,
                                onClick = { draft = draft.copy(before = k) },
                                label = { Text(stringResource(k.title)) },
                                leadingIcon = { Icon(k.icon, null, Modifier.size(18.dp)) },
                            )
                        }
                    }
                    val beforeStart = draft.beforeStart ?: draft.startDate
                    val ok = beforeStart.isBefore(draft.startDate)
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.relationship_before_since)) },
                        supportingContent = {
                            Text(
                                if (ok) Texts.mediumDate(beforeStart) else stringResource(R.string.relationship_before_invalid),
                                color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            )
                        },
                        leadingContent = { Icon(Icons.Rounded.CalendarMonth, null) },
                        colors = clear,
                        modifier = Modifier.clickable { pick = "before" },
                    )
                }
            }

            Section(stringResource(R.string.distance_mode)) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.distance_mode)) },
                    supportingContent = { Text(stringResource(R.string.distance_mode_summary)) },
                    leadingContent = { Icon(draft.travel.icon, null) },
                    trailingContent = { Switch(checked = draft.distance, onCheckedChange = { draft = draft.copy(distance = it) }) },
                    colors = clear,
                    modifier = Modifier.clickable { draft = draft.copy(distance = !draft.distance) },
                )
                if (draft.distance) {
                    Divider()
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.travel_title), style = MaterialTheme.typography.titleSmall)
                        TravelChips(draft.travel) { draft = draft.copy(travel = it) }
                    }
                    Divider()
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.distance_zone)) },
                        supportingContent = {
                            Text(
                                draft.farZone?.let(Distance::cityName) ?: stringResource(R.string.distance_zone_none),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        },
                        leadingContent = { Icon(Icons.Rounded.Public, null) },
                        colors = clear,
                        modifier = Modifier.clickable { pick = "zone" },
                    )
                    Divider()
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.distance_meeting)) },
                        supportingContent = {
                            Text(
                                draft.nextMeeting?.let(Texts::mediumDate) ?: stringResource(R.string.not_set),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        },
                        leadingContent = { Icon(Icons.Rounded.Event, null) },
                        colors = clear,
                        modifier = Modifier.clickable { pick = "meet" },
                    )
                }
            }

            Section(stringResource(R.string.section_notifications)) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.relationship_notify)) },
                    leadingContent = { Icon(Icons.Rounded.NotificationsActive, null) },
                    trailingContent = { Switch(checked = draft.notify, onCheckedChange = { draft = draft.copy(notify = it) }) },
                    colors = clear,
                    modifier = Modifier.clickable { draft = draft.copy(notify = !draft.notify) },
                )
                if (!isNew && settings.relationships.size > 1) {
                    val inWidget = settings.widget.id == relationship.id
                    Divider()
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.relationship_widget)) },
                        leadingContent = { Icon(Icons.Rounded.Widgets, null) },
                        trailingContent = {
                            RadioButton(selected = inWidget, onClick = { onWidget(relationship.id) })
                        },
                        colors = clear,
                        modifier = Modifier.clickable { onWidget(relationship.id) },
                    )
                }
            }

            if (canDelete) {
                TextButton(
                    onClick = { if (confirmDelete) onDelete() else confirmDelete = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text(
                        stringResource(
                            if (confirmDelete) R.string.remove_relationship_confirm else R.string.remove_relationship,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (askSave) {
        AlertDialog(
            onDismissRequest = { askSave = false },
            title = { Text(stringResource(R.string.unsaved_title)) },
            text = { Text(stringResource(if (valid) R.string.unsaved_text else R.string.unsaved_invalid)) },
            confirmButton = {
                if (valid) {
                    TextButton(onClick = {
                        askSave = false
                        onSave(draft)
                    }) { Text(stringResource(R.string.save)) }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    askSave = false
                    onClose()
                }) { Text(stringResource(R.string.discard)) }
            },
        )
    }

    when (pick) {
        "date" -> DateDialog(draft.startDate, onDismiss = { pick = null }) {
            draft = draft.copy(startDate = it)
            pick = null
        }
        "before" -> DateDialog(
            draft.beforeStart ?: draft.startDate.minusYears(1),
            onDismiss = { pick = null },
            title = R.string.relationship_before_since,
        ) {
            draft = draft.copy(beforeStart = it)
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
        "zone" -> ZoneDialog(current = draft.farZone, onDismiss = { pick = null }) {
            draft = draft.copy(farZone = it)
            pick = null
        }
        "meet" -> DateDialog(
            draft.nextMeeting ?: java.time.LocalDate.now().plusDays(7),
            onDismiss = { pick = null },
            title = R.string.distance_meeting,
        ) {
            draft = draft.copy(nextMeeting = it)
            pick = null
        }
    }
}

/** Auswahl des Verkehrsmittels, in der Beziehungsseite und in der Einrichtung */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TravelChips(selected: Travel, onPick: (Travel) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Travel.entries.forEach { t ->
            FilterChip(
                selected = selected == t,
                onClick = { onPick(t) },
                label = { Text(stringResource(t.title)) },
                leadingIcon = { Icon(t.icon, null, Modifier.size(18.dp)) },
            )
        }
    }
}
