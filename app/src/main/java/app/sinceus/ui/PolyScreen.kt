package app.sinceus.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.sinceus.R
import app.sinceus.data.ALL_RELATIONSHIPS
import app.sinceus.data.BirthdayMath
import app.sinceus.data.LoveMath
import app.sinceus.data.LoveSettings
import app.sinceus.data.Milestone
import app.sinceus.data.MilestoneKind
import app.sinceus.data.MomentMath
import app.sinceus.data.Names
import app.sinceus.data.Relationship
import app.sinceus.data.Texts
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** Farben der Beziehungen in der Übersicht, damit Linien und Karten zusammenpassen */
private val RelationshipColors = listOf(
    Color(0xFFFF8FA3),
    Color(0xFFB39DDB),
    Color(0xFF80CBC4),
    Color(0xFFFFCC80),
    Color(0xFF90CAF9),
    Color(0xFFF48FB1),
)

private fun colorOf(index: Int) = RelationshipColors[index % RelationshipColors.size]

/** Name einer Beziehung für Auswahl und Listen: die eigene Bezeichnung oder die Namen */
internal fun LoveSettings.titleOf(r: Relationship): String = r.label.ifBlank { namesOf(r) }

/** Auswahl oben im Titelbild, wenn es mehrere Beziehungen gibt */
@Composable
internal fun RelationshipSwitch(settings: LoveSettings, onSelect: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SwitchChip(stringResource(R.string.poly_all), settings.showAll, showIcon = true) { onSelect(ALL_RELATIONSHIPS) }
        settings.relationships.forEach { r ->
            SwitchChip(settings.titleOf(r), !settings.showAll && settings.relationship.id == r.id) { onSelect(r.id) }
        }
    }
}

@Composable
private fun SwitchChip(label: String, active: Boolean, showIcon: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier
            .height(36.dp)
            .clip(CircleShape)
            .background(if (active) Color.White else Color.Black.copy(alpha = 0.35f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showIcon) {
            Icon(
                Icons.Rounded.Groups,
                contentDescription = null,
                tint = if (active) Wine else Color.White,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(18.dp),
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (active) Wine else Color.White,
            maxLines = 1,
        )
    }
}

/** Übersicht über alle Beziehungen: wer mit wem, seit wann und was als Nächstes ansteht */
@Composable
internal fun PolyOverview(settings: LoveSettings, today: LocalDate, onSelect: (String) -> Unit) {
    val context = LocalContext.current
    val moments = if (settings.showMoments) settings.moments else emptyList()
    val todays = settings.relationships.flatMap { r ->
        LoveMath.milestonesOn(r.startDate, today).map { "${settings.namesOf(r)}: ${Texts.milestoneMessage(context, it)}" }
    } + (
        MomentMath.milestonesOn(moments, today) + BirthdayMath.milestonesOn(settings.people, today) +
            MomentMath.plannedOn(moments, today).map { Milestone(MilestoneKind.PLANNED, 0, today, it.title) }
        )
        .map { Texts.milestoneMessage(context, it) }
    val upcoming = (
        settings.relationships.flatMap { r -> LoveMath.upcoming(r.startDate, today, 3).map { it to settings.namesOf(r) } } +
            MomentMath.upcoming(moments, today, 3).map { it to null } +
            BirthdayMath.upcoming(settings.people, today, 3).map { it to null } +
            MomentMath.planned(moments, today, 3).map { it to null }
        )
        .sortedBy { it.first.date }
        .take(6)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Hero(settings, onSelect)
        Column(
            Modifier
                .offset(y = (-28).dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PolyculeCard(settings)
            if (todays.isNotEmpty()) TodayBanner(todays)
            if (LocalPrideMonth.current) {
                RainbowCard(stringResource(R.string.pride_title), stringResource(R.string.all_couples_text))
            }
            settings.relationships.forEachIndexed { i, r ->
                RelationshipCard(settings, r, colorOf(i), today) { onSelect(r.id) }
            }
            if (upcoming.isNotEmpty()) UpcomingCard(upcoming, today)
        }
        Spacer(Modifier.height(72.dp))
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

/** Alle Menschen im Kreis, Linien in der Farbe ihrer Beziehung verbinden sie */
@Composable
private fun PolyculeCard(settings: LoveSettings) {
    val people = settings.people
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            stringResource(R.string.poly_title).uppercase(),
            style = LabelCaps,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
        )
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(if (people.size > 4) 280.dp else 230.dp)
                .padding(8.dp),
        ) {
            val density = LocalDensity.current
            val w = constraints.maxWidth.toFloat()
            val h = constraints.maxHeight.toFloat()
            val node = with(density) { NODE.toPx() }
            val labelSpace = with(density) { 40.dp.toPx() }
            val columnWidth = with(density) { LABEL_WIDTH.toPx() }
            val cx = w / 2
            val cy = (h - labelSpace) / 2 + node / 4
            val radius = min(w / 2 - columnWidth / 2, (h - labelSpace) / 2 - node / 4).coerceAtLeast(node)
            val positions = people.indices.associate { i ->
                val angle = if (people.size == 2) PI * i else -PI / 2 + 2 * PI * i / people.size
                people[i].id to Offset(cx + radius * cos(angle).toFloat(), cy + radius * sin(angle).toFloat())
            }
            val stroke = with(density) { 5.dp.toPx() }
            Canvas(Modifier.fillMaxSize()) {
                settings.relationships.forEachIndexed { k, r ->
                    val points = r.members.mapNotNull { positions[it] }
                    for (a in points.indices) {
                        for (b in a + 1 until points.size) {
                            drawLine(colorOf(k), points[a], points[b], strokeWidth = stroke, cap = StrokeCap.Round)
                        }
                    }
                }
            }
            people.forEach { p ->
                val pos = positions.getValue(p.id)
                Column(
                    Modifier
                        .offset { IntOffset((pos.x - columnWidth / 2).roundToInt(), (pos.y - node / 2).roundToInt()) }
                        .width(LABEL_WIDTH),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .size(NODE)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            Names.initial(p.name).dropLast(1),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Text(
                        p.name,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    if (p.pronouns.isNotBlank()) {
                        Text(
                            p.pronouns,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

private val NODE = 48.dp
private val LABEL_WIDTH = 88.dp

@Composable
private fun RelationshipCard(settings: LoveSettings, r: Relationship, color: Color, today: LocalDate, onClick: () -> Unit) {
    val context = LocalContext.current
    val days = LoveMath.together(r.startDate, today).totalDays
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
            ) {
                Text(settings.namesOf(r), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    listOfNotNull(
                        r.label.ifBlank { null },
                        stringResource(R.string.together_since_long, Texts.mediumDate(r.startDate)),
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                if (r.startDate.isAfter(today)) {
                    context.resources.getQuantityString(R.plurals.in_days, days.toInt(), app.sinceus.data.formatNumber(days))
                } else {
                    Texts.count(context, R.plurals.days, days)
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
