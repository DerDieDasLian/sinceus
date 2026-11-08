package app.sinceus.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sinceus.R
import app.sinceus.data.LoveSettings
import app.sinceus.data.Moment
import app.sinceus.data.Names
import app.sinceus.data.Texts
import app.sinceus.data.YearRecap
import app.sinceus.data.YearRecapMath
import app.sinceus.data.formatNumber
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Die Seiten des Rückblicks; Seiten ohne Inhalt fallen weg */
private enum class RecapPage { Intro, Time, Moments, Tags, Photos, FirstLast, Next }

private fun pagesOf(w: YearRecap, withMoments: Boolean): List<RecapPage> = buildList {
    add(RecapPage.Intro)
    add(RecapPage.Time)
    if (withMoments) {
        add(RecapPage.Moments)
        if (w.topTags.isNotEmpty() || w.places > 0) add(RecapPage.Tags)
        if (w.photos > 0) add(RecapPage.Photos)
        if (w.first != null) add(RecapPage.FirstLast)
    }
    add(RecapPage.Next)
}

/**
 * Großer Jahresrückblick zum Durchwischen, Seite für Seite. Tippen rechts geht weiter, links zurück.
 * Ohne Momente (ausgeblendet oder keine gespeichert) bleiben nur Zeit und Ausblick.
 */
@Composable
fun YearRecapScreen(settings: LoveSettings, recap: YearRecap, today: LocalDate, onShare: () -> Unit, onClose: () -> Unit) {
    val backPreview = rememberBackPreview(onBack = onClose)
    val pages = remember(recap, settings.showMoments) { pagesOf(recap, settings.showMoments) }
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val first = settings.presetIndex.coerceIn(0, Presets.lastIndex)

    Box(
        Modifier
            .fillMaxSize()
            .backPreview(backPreview)
            .background(Color.Black),
    ) {
        HorizontalPager(pager, Modifier.fillMaxSize()) { index ->
            val preset = Presets[(first + index) % Presets.size]
            Box(
                Modifier
                    .fillMaxSize()
                    .background(preset.brush)
                    .background(Color.Black.copy(alpha = 0.25f))
                    // Tippen links geht zurück, rechts weiter, so wie bei Geschichten
                    .tapHalves(
                        onBack = { scope.launch { pager.animateScrollToPage((index - 1).coerceAtLeast(0)) } },
                        onNext = { scope.launch { if (index < pages.lastIndex) pager.animateScrollToPage(index + 1) } },
                    )
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 28.dp, vertical = 72.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                FadeIn(active = pager.currentPage == index) {
                    when (pages[index]) {
                        RecapPage.Intro -> IntroPage(settings, recap)
                        RecapPage.Time -> TimePage(recap)
                        RecapPage.Moments -> MomentsPage(recap)
                        RecapPage.Tags -> TagsPage(recap)
                        RecapPage.Photos -> PhotosPage(recap)
                        RecapPage.FirstLast -> FirstLastPage(recap)
                        RecapPage.Next -> NextPage(recap, today, onShare, onClose)
                    }
                }
            }
        }

        // Fortschritt oben, Schließen daneben
        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(start = 20.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                pages.indices.forEach { i ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = if (i <= pager.currentPage) 0.95f else 0.35f)),
                    )
                }
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, stringResource(R.string.recap_close), tint = Color.White)
            }
        }
        Text(
            stringResource(R.string.recap_page, pager.currentPage + 1, pages.size),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(bottom = 20.dp),
        )
    }
}

/** Tippen im linken Drittel geht zurück, sonst weiter; Wischen bleibt beim Pager, Knöpfe gehen vor */
private fun Modifier.tapHalves(onBack: () -> Unit, onNext: () -> Unit): Modifier =
    pointerInput(Unit) {
        detectTapGestures { offset -> if (offset.x < size.width / 3f) onBack() else onNext() }
    }

/** Inhalt einer Seite gleitet sanft herein, sobald sie gezeigt wird */
@Composable
private fun FadeIn(active: Boolean, content: @Composable () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(active) {
        if (active) progress.animateTo(1f, tween(600))
    }
    Box(
        Modifier.graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * 40.dp.toPx()
        },
    ) { content() }
}

@Composable
private fun Label(text: String) {
    Text(text.uppercase(), style = LabelCaps, color = Color.White.copy(alpha = 0.8f))
}

@Composable
private fun Big(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.displayMedium,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun Line(text: String, strong: Boolean = false) {
    Text(
        text,
        style = if (strong) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
        fontWeight = if (strong) FontWeight.SemiBold else FontWeight.Normal,
        color = Color.White.copy(alpha = if (strong) 1f else 0.85f),
        modifier = Modifier.padding(top = 10.dp),
    )
}

@Composable
private fun IntroPage(settings: LoveSettings, w: YearRecap) {
    Column {
        Icon(settings.relationship.kind.icon, null, tint = Color.White, modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(20.dp))
        Label(stringResource(R.string.recap_intro_label))
        Big(stringResource(R.string.year_review_title, w.review.year.toString()))
        Line(Names.join(settings.memberNames()), strong = true)
        Line(stringResource(R.string.year_review_range, Texts.longDate(w.review.from), Texts.longDate(w.review.to)))
    }
}

@Composable
private fun TimePage(w: YearRecap) {
    Column {
        Label(stringResource(R.string.recap_time_label))
        Big(formatNumber(w.days))
        Line(pluralStringResource(R.plurals.recap_days, w.days.toInt()), strong = true)
        Spacer(Modifier.height(24.dp))
        Line(pluralStringResource(R.plurals.recap_weekends, w.weekends.toInt(), formatNumber(w.weekends)))
        Line(pluralStringResource(R.plurals.recap_full_moons, w.fullMoons.toInt(), formatNumber(w.fullMoons)))
    }
}

@Composable
private fun MomentsPage(w: YearRecap) {
    Column {
        Label(stringResource(R.string.recap_moments_label))
        if (w.moments.isEmpty()) {
            Line(stringResource(R.string.recap_no_moments), strong = true)
            return@Column
        }
        Big(formatNumber(w.moments.size.toLong()))
        Line(pluralStringResource(R.plurals.recap_moments, w.moments.size), strong = true)
        if (w.photos > 0) Line(pluralStringResource(R.plurals.recap_photos, w.photos, formatNumber(w.photos.toLong())))
        w.topMonth?.let { month ->
            val name = month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
            Line(pluralStringResource(R.plurals.recap_top_month, w.topMonthCount, name, formatNumber(w.topMonthCount.toLong())))
        }
    }
}

@Composable
private fun TagsPage(w: YearRecap) {
    Column {
        if (w.topTags.isNotEmpty()) {
            Label(stringResource(R.string.recap_tags_label))
            w.topTags.forEachIndexed { i, (tag, count) ->
                Row(Modifier.padding(top = if (i == 0) 12.dp else 6.dp), verticalAlignment = Alignment.Bottom) {
                    Text(
                        tag,
                        style = if (i == 0) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text(
                        pluralStringResource(R.plurals.recap_times, count, formatNumber(count.toLong())),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(start = 12.dp, bottom = 4.dp),
                    )
                }
            }
        }
        if (w.places > 0) {
            if (w.topTags.isNotEmpty()) Spacer(Modifier.height(36.dp))
            Label(stringResource(R.string.recap_places_label))
            Line(pluralStringResource(R.plurals.recap_places, w.places, formatNumber(w.places.toLong())), strong = true)
            w.topPlace?.let { Line(stringResource(R.string.recap_top_place, it)) }
        }
    }
}

@Composable
private fun PhotosPage(w: YearRecap) {
    val photos = remember(w) { YearRecapMath.collage(w.moments) }
    Column {
        Label(stringResource(R.string.recap_photos_label))
        Spacer(Modifier.height(16.dp))
        // Zwei Spalten, damit die Fotos groß genug bleiben
        photos.chunked(2).forEach { row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { path ->
                    AsyncImage(
                        model = File(path),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(if (photos.size <= 2) 0.8f else 1.3f)
                            .clip(RoundedCornerShape(16.dp)),
                    )
                }
                if (row.size == 1 && photos.size > 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FirstLastPage(w: YearRecap) {
    Column {
        w.first?.let { MomentLine(stringResource(R.string.recap_first_label), it) }
        w.last?.let {
            Spacer(Modifier.height(40.dp))
            MomentLine(stringResource(R.string.recap_last_label), it)
        }
    }
}

@Composable
private fun MomentLine(label: String, m: Moment) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        m.photoPath?.let { path ->
            AsyncImage(
                model = File(path),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )
            Spacer(Modifier.width(16.dp))
        }
        Column {
            Label(label)
            Text(
                m.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                listOfNotNull(Texts.longDate(m.date), m.place.trim().ifEmpty { null }).joinToString(" · "),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun NextPage(w: YearRecap, today: LocalDate, onShare: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Center) {
        w.next?.let { next ->
            Label(stringResource(R.string.recap_next_label))
            Big(Texts.milestoneTitle(context, next))
            Line(Texts.longDate(next.date), strong = true)
            val days = ChronoUnit.DAYS.between(today, next.date)
            if (days > 0) Line(pluralStringResource(R.plurals.recap_in_days, days.toInt(), formatNumber(days)))
            Spacer(Modifier.height(40.dp))
        }
        Text(
            stringResource(R.string.recap_end),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            fontSize = 26.sp,
        )
        Row(Modifier.padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onShare,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Wine),
            ) {
                Icon(Icons.Rounded.Share, null)
                Text(stringResource(R.string.year_review_share), Modifier.padding(start = 8.dp))
            }
            TextButton(onClick = onClose) {
                Text(stringResource(R.string.recap_close), color = Color.White)
            }
        }
    }
}
