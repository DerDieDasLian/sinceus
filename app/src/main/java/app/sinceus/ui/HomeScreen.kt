package app.sinceus.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import app.sinceus.data.Milestone
import app.sinceus.data.MilestoneKind
import app.sinceus.data.formatNumber
import app.sinceus.data.Texts
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
) {
    val pager = rememberPagerState(initialPage = settings.homePage.coerceIn(0, 1)) { 2 }
    val scope = rememberCoroutineScope()
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.drop(1).collect(onPageChange)
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
            if (page == 0) Overview(settings, today) else LiveScreen(settings, active = pager.currentPage == 1)
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
            ModeSwitch(pager.currentPage) { scope.launch { pager.animateScrollToPage(it) } }
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

private val BarItemHeight = 40.dp
private val BarPadding = 4.dp

@Composable
private fun ModeSwitch(selected: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        listOf(stringResource(R.string.tab_overview), stringResource(R.string.tab_live)).forEachIndexed { i, label ->
            val active = i == selected
            Box(
                Modifier
                    .height(BarItemHeight)
                    .clip(CircleShape)
                    .background(if (active) Color.White else Color.Transparent)
                    .clickable { onSelect(i) }
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
private fun Overview(settings: LoveSettings, today: LocalDate) {
    val start = settings.startDate
    val together = LoveMath.together(start, today)
    val future = start.isAfter(today)
    val todays = LoveMath.milestonesOn(start, today)
    val upcoming = LoveMath.upcoming(start, today, 4)
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Hero(settings)

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
                )
            } else {
                CounterCard(
                    label = stringResource(if (future) R.string.starts_in else R.string.together_for),
                    big = formatNumber(together.totalDays),
                    unit = pluralStringResource(R.plurals.unit_days_dative, together.totalDays.toInt()),
                    sub = Texts.period(context, together.period, dative = true),
                )
            }

            if (todays.isNotEmpty()) TodayBanner(todays)

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
private fun Hero(settings: LoveSettings) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(heroHeight()),
    ) {
        CouplePhoto(settings, Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .background(PhotoScrim),
        )
        NamesOverlay(settings, Modifier.align(Alignment.BottomStart))
    }
}

@Composable
fun NamesOverlay(settings: LoveSettings, modifier: Modifier = Modifier) {
    Row(
        modifier.padding(start = 24.dp, end = 24.dp, bottom = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(settings.name1, style = MaterialTheme.typography.displayMedium, color = Color.White, maxLines = 1)
        BeatingHeart(Modifier.padding(horizontal = 12.dp))
        Text(settings.name2, style = MaterialTheme.typography.displayMedium, color = Color.White, maxLines = 1)
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
    Icon(
        Icons.Rounded.Favorite,
        contentDescription = null,
        tint = Color(0xFFFF5C77),
        modifier = modifier
            .size(size)
            .scale(beat),
    )
}

@Composable
private fun CounterCard(label: String, big: String, unit: String, sub: String) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
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
private fun TodayBanner(milestones: List<Milestone>) {
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
                val context = LocalContext.current
                milestones.forEach { Text(Texts.milestoneMessage(context, it), style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}

@Composable
private fun UpcomingCard(upcoming: List<Milestone>, today: LocalDate) {
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
            upcoming.forEach { m ->
                val inDays = ChronoUnit.DAYS.between(today, m.date)
                Row(
                    Modifier
                        .fillMaxWidth()
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
                            Texts.dateWithWeekday(m.date),
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
}
