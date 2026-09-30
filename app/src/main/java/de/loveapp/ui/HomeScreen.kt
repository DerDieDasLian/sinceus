package de.loveapp.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import de.loveapp.data.LoveMath
import de.loveapp.data.LoveSettings
import de.loveapp.data.Milestone
import de.loveapp.data.MilestoneKind
import de.loveapp.data.formatNumber
import de.loveapp.data.toGermanText
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val LongDate = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMAN)
private val ShortDate = DateTimeFormatter.ofPattern("EE, d. MMM yyyy", Locale.GERMAN)

@Composable
fun HomeScreen(settings: LoveSettings, today: LocalDate, onOpenSettings: () -> Unit) {
    val start = settings.startDate
    val together = LoveMath.together(start, today)
    val future = start.isAfter(today)
    val todays = LoveMath.milestonesOn(start, today)
    val upcoming = LoveMath.upcoming(start, today, 4)

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        Hero(settings, onOpenSettings)

        Column(
            Modifier
                .offset(y = (-28).dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (together.totalDays == 0L) {
                CounterCard("Ihr seid zusammen seit", "heute", "", "Der Anfang von allem ❤")
            } else {
                CounterCard(
                    label = if (future) "Es geht los in" else "Ihr seid zusammen seit",
                    big = formatNumber(together.totalDays),
                    unit = if (together.totalDays == 1L) "Tag" else "Tagen",
                    sub = together.period.toGermanText(dative = true),
                )
            }

            if (todays.isNotEmpty()) TodayBanner(todays)

            if (!future) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(Modifier.weight(1f), formatNumber(together.totalMonths), "Monate")
                    StatTile(Modifier.weight(1f), formatNumber(together.totalWeeks), "Wochen")
                    StatTile(Modifier.weight(1f), formatNumber(together.totalDays * 24), "Stunden")
                }
            }

            if (upcoming.isNotEmpty()) UpcomingCard(upcoming, today)

            Text(
                "Zusammen seit ${start.format(LongDate)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun Hero(settings: LoveSettings, onOpenSettings: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(460.dp),
    ) {
        if (settings.photoPath != null) {
            AsyncImage(
                model = File(settings.photoPath),
                contentDescription = "Euer Foto",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            PresetBackground(Presets.getOrElse(settings.presetIndex) { Presets[0] }, Modifier.fillMaxSize())
        }
        // Verlauf, damit Schrift auf jedem Foto lesbar bleibt
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.35f),
                        0.25f to Color.Transparent,
                        0.55f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.7f),
                    ),
                ),
        )
        FilledIconButton(
            onClick = onOpenSettings,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color.Black.copy(alpha = 0.3f),
                contentColor = Color.White,
            ),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(12.dp),
        ) {
            Icon(Icons.Rounded.Settings, contentDescription = "Einstellungen")
        }
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, end = 24.dp, bottom = 48.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(settings.name1, style = MaterialTheme.typography.displayMedium, color = Color.White)
                BeatingHeart(Modifier.padding(horizontal = 12.dp))
                Text(settings.name2, style = MaterialTheme.typography.displayMedium, color = Color.White)
            }
        }
    }
}

@Composable
private fun BeatingHeart(modifier: Modifier = Modifier) {
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
            .size(30.dp)
            .scale(beat),
    )
}

@Composable
fun PresetBackground(preset: Preset, modifier: Modifier = Modifier) {
    Box(modifier.background(preset.brush)) {
        Icon(
            Icons.Rounded.Favorite,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.10f),
            modifier = Modifier
                .align(Alignment.Center)
                .size(220.dp),
        )
    }
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
                Text("Heute ist ein besonderer Tag!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                milestones.forEach { Text(it.message, style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}

@Composable
private fun UpcomingCard(upcoming: List<Milestone>, today: LocalDate) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 16.dp)) {
            Text(
                "NÄCHSTE BESONDERE TAGE",
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
                        Text(m.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            m.date.format(ShortDate),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        when (inDays) {
                            1L -> "morgen"
                            2L -> "übermorgen"
                            else -> "in ${formatNumber(inDays)} Tagen"
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
