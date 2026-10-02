package app.sinceus.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import app.sinceus.data.MomentFilter
import app.sinceus.data.MomentCalendar
import androidx.compose.foundation.border
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.ui.platform.LocalConfiguration
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.sinceus.R
import app.sinceus.data.LoveSettings
import app.sinceus.data.Moment
import app.sinceus.data.MomentMath
import app.sinceus.data.Texts
import app.sinceus.data.formatNumber
import coil3.compose.AsyncImage
import java.io.File
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Vorschläge im leeren Zustand, in der Reihenfolge einer typischen Beziehung */
val MomentSuggestions = listOf(
    R.string.suggest_met,
    R.string.suggest_first_date,
    R.string.suggest_first_kiss,
    R.string.suggest_i_love_you,
    R.string.suggest_first_trip,
    R.string.suggest_moved_in,
)

/** Eintrag der Zeitleiste: ein gespeicherter Moment oder euer Starttag */
private sealed interface TimelineEntry {
    val date: LocalDate

    /** Namen der Beziehung, wenn es mehrere gibt, sonst null */
    val who: String?

    data class Saved(val moment: Moment, override val who: String? = null) : TimelineEntry {
        override val date get() = moment.date
    }

    data class Start(override val date: LocalDate, override val who: String? = null) : TimelineEntry
}

@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MomentsScreen(
    settings: LoveSettings,
    today: LocalDate,
    onAdd: (String?) -> Unit,
    onOpen: (Moment) -> Unit,
) {
    val context = LocalContext.current
    // Mit mehreren Beziehungen steht an jedem Eintrag, zu wem er gehört
    val multi = settings.relationships.size > 1
    fun who(id: String?) = settings.relationships.firstOrNull { it.id == id }?.takeIf { multi }?.let(settings::namesOf)
    val moments = settings.visibleMoments
    // Suche und Filter erst, wenn sich genug Momente angesammelt haben
    val searchable = moments.size >= MomentFilter.MIN_MOMENTS
    var query by rememberSaveable { mutableStateOf("") }
    var year by rememberSaveable { mutableStateOf<Int?>(null) }
    var relationship by rememberSaveable { mutableStateOf<String?>(null) }
    val filterRelationships = multi && settings.showAll
    // Ansicht: Zeitleiste oder Kalender
    var calendar by rememberSaveable { mutableStateOf(false) }
    var monthIndex by rememberSaveable { mutableStateOf(today.year * 12 + today.monthValue - 1) }
    val filtering = searchable && (query.isNotBlank() || year != null || (filterRelationships && relationship != null))
    val shown = if (filtering) {
        moments.filter { MomentFilter.matches(it, query, year, relationship.takeIf { filterRelationships }) }
    } else {
        moments
    }
    val starts = when {
        filtering -> emptyList()
        settings.showAll -> settings.relationships
        else -> listOf(settings.relationship)
    }
    val entries = (
        shown.map { m ->
            // Nach einem Abgleich steht dabei, wer den Moment angelegt hat
            val by = m.addedBy?.let(settings::person)?.name?.let { context.getString(R.string.moment_added_by, it) }
            TimelineEntry.Saved(m, listOfNotNull(who(m.relationshipId), by).joinToString(" · ").ifBlank { null })
        } +
            starts.map { TimelineEntry.Start(it.startDate, who(it.id)) }
        )
        .sortedBy { it.date }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp),
    ) {
        Text(
            stringResource(R.string.moments_title),
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.padding(top = 28.dp),
        )
        Text(
            if (moments.isEmpty()) {
                ""
            } else {
                pluralStringResource(R.plurals.moments_count, moments.size, formatNumber(moments.size.toLong()))
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
        )

        if (moments.isEmpty()) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Presets[0].brush),
                        contentAlignment = Alignment.Center,
                    ) {
                        BeatingHeart(size = 34.dp)
                    }
                    Text(
                        stringResource(R.string.moments_empty_title),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    Text(
                        stringResource(R.string.moments_empty_sub),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        MomentSuggestions.forEach { id ->
                            val label = stringResource(id)
                            SuggestionChip(onClick = { onAdd(label) }, label = { Text(label) })
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        FilledTonalButton(
            onClick = { onAdd(null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
        ) {
            Icon(Icons.Rounded.Add, null)
            Text(stringResource(R.string.moment_add), Modifier.padding(start = 8.dp))
        }

        if (moments.isNotEmpty()) {
            SingleChoiceSegmentedButtonRow(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            ) {
                listOf(false to R.string.moments_view_list, true to R.string.moments_view_calendar).forEachIndexed { i, (value, label) ->
                    SegmentedButton(
                        selected = calendar == value,
                        onClick = { calendar = value },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                        icon = {
                            SegmentedButtonDefaults.Icon(calendar == value) {
                                Icon(
                                    if (value) Icons.Rounded.CalendarMonth else Icons.AutoMirrored.Rounded.ViewList,
                                    null,
                                    modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                                )
                            }
                        },
                    ) { Text(stringResource(label)) }
                }
            }
        }

        if (searchable) {
            OutlinedTextField(
                query,
                { query = it },
                placeholder = { Text(stringResource(R.string.moments_search)) },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.moments_search_clear))
                        }
                    }
                } else {
                    null
                },
                singleLine = true,
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (filterRelationships) {
                    settings.relationships.forEach { r ->
                        FilterChip(
                            selected = relationship == r.id,
                            onClick = { relationship = if (relationship == r.id) null else r.id },
                            label = { Text(settings.namesOf(r)) },
                        )
                    }
                }
                MomentFilter.years(moments).forEach { y ->
                    FilterChip(
                        selected = year == y,
                        onClick = { year = if (year == y) null else y },
                        label = { Text(y.toString()) },
                    )
                }
            }
            if (filtering && shown.isEmpty()) {
                Text(
                    stringResource(R.string.moments_no_results),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                )
            }
        }

        if (calendar && moments.isNotEmpty()) {
            val month = YearMonth.of(monthIndex / 12, monthIndex % 12 + 1)
            CalendarView(
                month = month,
                today = today,
                entries = entries,
                anniversaries = MomentCalendar.anniversaries(shown, month),
                onMonth = { monthIndex += it },
                onToday = { monthIndex = today.year * 12 + today.monthValue - 1 },
                onOpen = onOpen,
            )
        } else {
            entries.forEachIndexed { index, entry ->
                TimelineItem(
                    entry = entry,
                    today = today,
                    isFirst = index == 0,
                    isLast = index == entries.lastIndex,
                    onClick = (entry as? TimelineEntry.Saved)?.let { { onOpen(it.moment) } },
                    relative = relativeText(context, entry.date, today),
                )
            }
        }

        // Platz für die schwebende Leiste
        Spacer(Modifier.height(96.dp))
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

/** Monatskalender: Tage mit Momenten sind gefüllt, Jahrestage haben einen Punkt, darunter die Einträge des Monats */
@Composable
private fun CalendarView(
    month: YearMonth,
    today: LocalDate,
    entries: List<TimelineEntry>,
    anniversaries: List<MomentCalendar.Anniversary>,
    onMonth: (Int) -> Unit,
    onToday: () -> Unit,
    onOpen: (Moment) -> Unit,
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val inMonth = entries.filter { YearMonth.from(it.date) == month }
    val byDay = inMonth.groupBy { it.date }
    val anniversaryDays = anniversaries.map { it.date }.toSet()
    val colors = MaterialTheme.colorScheme

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onMonth(-1) }) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.calendar_previous))
                }
                val title = month.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
                    .replaceFirstChar { it.titlecase(locale) }
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .clickable(onClick = onToday)
                        .padding(vertical = 8.dp),
                )
                IconButton(onClick = { onMonth(1) }) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.calendar_next))
                }
            }
            val firstDay = WeekFields.of(locale).firstDayOfWeek
            Row(Modifier.padding(top = 4.dp)) {
                (0L until 7L).forEach { i ->
                    Text(
                        firstDay.plus(i).getDisplayName(java.time.format.TextStyle.NARROW, locale),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            MomentCalendar.cells(month, firstDay).chunked(7).forEach { week ->
                Row(Modifier.padding(top = 4.dp)) {
                    week.forEach { day ->
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (day != null) {
                                val saved = byDay[day].orEmpty()
                                val filled = saved.isNotEmpty()
                                val first = saved.firstNotNullOfOrNull { (it as? TimelineEntry.Saved)?.moment }
                                Box(
                                    Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                saved.any { it is TimelineEntry.Start } -> colors.primary
                                                filled -> colors.primaryContainer
                                                else -> androidx.compose.ui.graphics.Color.Transparent
                                            },
                                        )
                                        .then(
                                            if (day == today) Modifier.border(2.dp, colors.primary, CircleShape) else Modifier,
                                        )
                                        .then(if (first != null) Modifier.clickable { onOpen(first) } else Modifier),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        day.dayOfMonth.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (filled || day == today) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            saved.any { it is TimelineEntry.Start } -> colors.onPrimary
                                            filled -> colors.onPrimaryContainer
                                            else -> colors.onSurface
                                        },
                                    )
                                    if (day in anniversaryDays) {
                                        Box(
                                            Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = 4.dp)
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(colors.primary),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (anniversaries.isNotEmpty()) {
        Text(
            stringResource(R.string.calendar_anniversaries).uppercase(),
            style = LabelCaps,
            color = colors.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        anniversaries.forEach { a ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surfaceContainerLow)
                    .clickable { onOpen(a.moment) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Icon(Icons.Rounded.NotificationsActive, null, tint = colors.primary, modifier = Modifier.size(18.dp))
                Column(Modifier.padding(start = 12.dp)) {
                    Text(
                        stringResource(
                            R.string.moment_anniversary_title,
                            a.moment.title,
                            context.resources.getQuantityString(R.plurals.years, a.years, formatNumber(a.years.toLong())),
                        ),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(Texts.dateWithWeekday(a.date), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (inMonth.isEmpty() && anniversaries.isEmpty()) {
        Text(
            stringResource(R.string.calendar_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        )
    }
    inMonth.forEachIndexed { index, entry ->
        TimelineItem(
            entry = entry,
            today = today,
            isFirst = index == 0,
            isLast = index == inMonth.lastIndex,
            onClick = (entry as? TimelineEntry.Saved)?.let { { onOpen(it.moment) } },
            relative = relativeText(context, entry.date, today),
        )
    }
}

private fun relativeText(context: android.content.Context, date: LocalDate, today: LocalDate): String {
    val days = ChronoUnit.DAYS.between(date, today)
    return when {
        days == 0L -> context.getString(R.string.today_word)
        days > 0 -> context.resources.getQuantityString(R.plurals.days_ago, days.toInt(), formatNumber(days))
        else -> context.resources.getQuantityString(R.plurals.in_days, (-days).toInt(), formatNumber(-days))
    }
}

@Composable
private fun TimelineItem(
    entry: TimelineEntry,
    today: LocalDate,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: (() -> Unit)?,
    relative: String,
) {
    val line = MaterialTheme.colorScheme.outlineVariant
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        // Linie mit Punkt links
        Box(
            Modifier
                .width(28.dp)
                .fillMaxHeight(),
        ) {
            // Linie verbindet die Punkte: beim ersten Eintrag ab dem Punkt, beim letzten bis zum Punkt
            if (!(isFirst && isLast)) {
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = if (isFirst) 22.dp else 0.dp)
                        .width(2.dp)
                        .then(if (isLast) Modifier.height(22.dp) else Modifier.fillMaxHeight())
                        .background(line),
                )
            }
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(
                        if (entry is TimelineEntry.Start) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    ),
            )
        }
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (entry is TimelineEntry.Start) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
            ),
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp, bottom = 14.dp)
                .clip(RoundedCornerShape(24.dp))
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        ) {
            val moment = (entry as? TimelineEntry.Saved)?.moment
            if (moment?.photoPath != null) {
                AsyncImage(
                    model = File(moment.photoPath),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f),
                )
            }
            Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                Text(
                    "${Texts.longDate(entry.date)} · $relative".uppercase(),
                    style = LabelCaps,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    moment?.title ?: stringResource(R.string.moment_together_start),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp),
                )
                entry.who?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                if (!moment?.note.isNullOrBlank()) {
                    Text(
                        moment!!.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                if (moment != null && moment.yearlyReminder && !moment.date.isAfter(today)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        Icon(
                            Icons.Rounded.NotificationsActive,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            stringResource(
                                R.string.moment_next_anniversary,
                                Texts.dateWithWeekday(MomentMath.nextAnniversary(moment, today)),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            }
        }
    }
}
