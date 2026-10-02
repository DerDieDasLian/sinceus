package app.sinceus.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.IconButton
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sinceus.data.LoveMath
import app.sinceus.data.LoveSettings
import app.sinceus.data.BirthdayMath
import app.sinceus.data.Milestone
import app.sinceus.data.MilestoneKind
import app.sinceus.data.MomentMath
import app.sinceus.data.formatNumber
import app.sinceus.data.Texts
import app.sinceus.share.ShareCard
import androidx.compose.material.icons.rounded.IosShare
import app.sinceus.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import java.time.LocalDate
import java.time.temporal.ChronoUnit


@Composable
fun HomeScreen(
    settings: LoveSettings,
    today: LocalDate,
    onOpenSettings: () -> Unit,
    onPageChange: (Int) -> Unit,
    onAddMoment: (String?) -> Unit = {},
    onOpenMoment: (app.sinceus.data.Moment) -> Unit = {},
    /** Andere Beziehung zeigen (Poly-Modus), [app.sinceus.data.ALL_RELATIONSHIPS] = alle */
    onSelect: (String) -> Unit = {},
    /** Von einer App-Abkürzung: "live" springt zum Live-Zähler */
    jumpTo: String? = null,
    onJumped: () -> Unit = {},
) {
    // Live und Momente lassen sich in den Einstellungen ausblenden
    val pages = listOfNotNull(
        HomePage.Overview,
        HomePage.Live.takeIf { settings.showLive },
        HomePage.Moments.takeIf { settings.showMoments },
    )
    val pageCount = pages.size
    val pager = rememberPagerState(initialPage = settings.homePage.coerceIn(0, pageCount - 1)) { pageCount }
    val scope = rememberCoroutineScope()
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.drop(1).collect(onPageChange)
    }
    LaunchedEffect(jumpTo) {
        if (jumpTo == null) return@LaunchedEffect
        val index = if (jumpTo == "live") pages.indexOf(HomePage.Live) else -1
        if (index >= 0) pager.scrollToPage(index)
        onJumped()
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
            when (pages[page]) {
                HomePage.Overview -> if (settings.showAll) {
                    PolyOverview(settings, today, onSelect)
                } else {
                    Overview(settings, today, onSelect)
                }
                HomePage.Live -> LiveScreen(settings, active = pager.currentPage == page)
                HomePage.Moments -> MomentsScreen(settings, today, onAdd = onAddMoment, onOpen = onOpenMoment)
            }
        }
        // Schwebende Leiste unten, damit oben nichts das Foto verdeckt.
        // Alle Elemente sind gleich hoch und haben rundherum denselben Abstand,
        // so laufen die Rundungen von Leiste und Knöpfen parallel.
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 16.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(Color(0xFF2A1D1F))
                .padding(BarPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (pages.size > 1) {
                ModeSwitch(pager.currentPage, pages) { scope.launch { pager.animateScrollToPage(it) } }
            }
            Box(
                Modifier
                    .size(BarItemHeight)
                    .clip(CircleShape)
                    .clickable(onClick = onOpenSettings),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Settings,
                    contentDescription = stringResource(R.string.settings),
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

private enum class HomePage(@androidx.annotation.StringRes val label: Int) {
    Overview(R.string.tab_overview),
    Live(R.string.tab_live),
    Moments(R.string.tab_moments),
}

private val BarItemHeight = 40.dp
private val BarPadding = 4.dp

@Composable
private fun ModeSwitch(selected: Int, pages: List<HomePage>, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        pages.map { stringResource(it.label) }.forEachIndexed { i, label ->
            val active = i == selected
            // Als Reiter vorgelesen, mit "ausgewählt"; bei großer Schrift wächst der Knopf mit
            Box(
                Modifier
                    .heightIn(min = BarItemHeight)
                    .clip(CircleShape)
                    .background(if (active) Color.White else Color.Transparent)
                    .selectable(selected = active, role = Role.Tab) { onSelect(i) }
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) Wine else Color.White,
                )
            }
        }
    }
}

@Composable
private fun Overview(settings: LoveSettings, today: LocalDate, onSelect: (String) -> Unit) {
    val start = settings.startDate
    val together = LoveMath.together(start, today)
    val future = start.isAfter(today)
    // Jahrestage der Momente erscheinen mit, solange die Momente nicht ausgeblendet sind
    val moments = if (settings.showMoments) settings.visibleMoments else emptyList()
    val members = settings.members()
    val todays = LoveMath.milestonesOn(start, today) + MomentMath.milestonesOn(moments, today) +
        BirthdayMath.milestonesOn(members, today)
    val upcoming = (
        LoveMath.upcoming(start, today, 4) + MomentMath.upcoming(moments, today, 4) + BirthdayMath.upcoming(members, today, 4)
        )
        .sortedBy { it.date }
        .take(4)
        .map { it to (null as String?) }
    val context = LocalContext.current
    val share = { ShareCard.share(context, settings, today, ShareCard.defaultText(context, settings, today)) }

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
            if (together.totalDays == 0L) {
                CounterCard(
                    stringResource(R.string.together_for),
                    stringResource(R.string.today_word),
                    "",
                    stringResource(R.string.beginning),
                    onShare = share,
                )
            } else {
                CounterCard(
                    label = stringResource(if (future) R.string.starts_in else R.string.together_for),
                    big = formatNumber(together.totalDays),
                    unit = pluralStringResource(R.plurals.unit_days_dative, together.totalDays.toInt()),
                    sub = Texts.period(context, together.period, dative = true),
                    onShare = share,
                )
            }

            if (todays.isNotEmpty()) TodayBanner(todays.map { Texts.milestoneMessage(context, it) })

            if (LocalPrideMonth.current) RainbowCard(stringResource(R.string.pride_title), stringResource(R.string.all_couples_text))

            if (!future) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(Modifier.weight(1f), formatNumber(together.totalMonths), stringResource(R.string.stat_months))
                    StatTile(Modifier.weight(1f), formatNumber(together.totalWeeks), stringResource(R.string.stat_weeks))
                    StatTile(Modifier.weight(1f), formatNumber(together.totalDays * 24), stringResource(R.string.stat_hours))
                }
            }

            if (upcoming.isNotEmpty()) UpcomingCard(upcoming, today)

            Text(
                stringResource(R.string.together_since_long, Texts.longDate(start)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }
        Spacer(Modifier.height(72.dp))
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
internal fun Hero(settings: LoveSettings, onSelect: (String) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(heroHeight()),
    ) {
        HeroPhoto(settings, Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .background(PhotoScrim),
        )
        NamesOverlay(settings, Modifier.align(Alignment.BottomStart))
        if (settings.relationships.size > 1) RelationshipSwitch(settings, onSelect)
    }
}

@Composable
fun NamesOverlay(settings: LoveSettings, modifier: Modifier = Modifier) {
    val names = if (settings.showAll) settings.people.map { it.name } else settings.memberNames()
    NamesRow(
        names,
        if (names.size > 2) MaterialTheme.typography.displaySmall else MaterialTheme.typography.displayMedium,
        heart = if (names.size > 2) 24.dp else 30.dp,
        modifier = modifier.padding(start = 24.dp, end = 24.dp, bottom = 48.dp),
    )
}

/** Namen mit schlagendem Herz dazwischen; bei mehr als zwei Menschen mit Zeilenumbruch */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NamesRow(names: List<String>, style: TextStyle, heart: Dp, modifier: Modifier = Modifier) {
    FlowRow(modifier, itemVerticalAlignment = Alignment.CenterVertically) {
        names.forEachIndexed { i, name ->
            if (i > 0) BeatingHeart(Modifier.padding(horizontal = heart * 0.4f), size = heart)
            Text(name, style = style, color = Color.White, maxLines = 1)
        }
    }
}

@Composable
fun BeatingHeart(modifier: Modifier = Modifier, size: Dp = 30.dp) {
    val beat by rememberInfiniteTransition(label = "heart").animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "beat",
    )
    val pride = LocalPrideMonth.current
    Icon(
        Icons.Rounded.Favorite,
        contentDescription = null,
        tint = if (pride) Color.White else Color(0xFFFF5C77),
        modifier = modifier
            .size(size)
            .scale(beat)
            .then(if (pride) Modifier.rainbow() else Modifier),
    )
}

/** Karte mit Regenbogenstreifen: Pride-Gruß auf der Startseite, "Für jede Liebe" in den Einstellungen */
@Composable
internal fun RainbowCard(title: String, text: String) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Brush.horizontalGradient(PrideColors)),
        )
        Column(Modifier.padding(20.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun CounterCard(label: String, big: String, unit: String, sub: String, onShare: () -> Unit) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(label.uppercase(), style = LabelCaps, color = MaterialTheme.colorScheme.primary)
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 6.dp)) {
                    Text(
                        big,
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        unit,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp, bottom = 12.dp),
                    )
                }
                Text(sub, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(
                onClick = onShare,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
            ) {
                Icon(
                    Icons.Rounded.IosShare,
                    contentDescription = stringResource(R.string.share),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun StatTile(modifier: Modifier, value: String, label: String) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun TodayBanner(lines: List<String>) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Celebration, contentDescription = null, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(stringResource(R.string.special_today), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                lines.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}

/** Die nächsten besonderen Tage, jeweils mit den Namen der Beziehung, wenn es mehrere gibt */
@Composable
internal fun UpcomingCard(upcoming: List<Pair<Milestone, String?>>, today: LocalDate) {
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 16.dp)) {
            Text(
                stringResource(R.string.upcoming).uppercase(),
                style = LabelCaps,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            upcoming.forEach { (m, who) ->
                val inDays = ChronoUnit.DAYS.between(today, m.date)
                // Bildschirmleser lesen Anlass, Datum und Abstand als eine Zeile vor
                Row(
                    Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {}
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            m.kind.icon(),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                    ) {
                        Text(Texts.milestoneTitle(context, m), style = MaterialTheme.typography.titleMedium)
                        Text(
                            listOfNotNull(who, Texts.dateWithWeekday(m.date)).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        when (inDays) {
                            1L -> stringResource(R.string.tomorrow)
                            2L -> stringResource(R.string.day_after_tomorrow)
                            else -> pluralStringResource(R.plurals.in_days, inDays.toInt(), formatNumber(inDays))
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

private fun MilestoneKind.icon(): ImageVector = when (this) {
    MilestoneKind.YEARS -> Icons.Rounded.Cake
    MilestoneKind.MONTHS -> Icons.Rounded.Favorite
    MilestoneKind.DAYS -> Icons.Rounded.Star
    MilestoneKind.WEEKS -> Icons.Rounded.CalendarMonth
    MilestoneKind.MOMENT -> Icons.Rounded.AutoAwesome
    MilestoneKind.BIRTHDAY -> Icons.Rounded.CardGiftcard
}
