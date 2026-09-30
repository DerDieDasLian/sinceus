package app.sinceus.ui

import androidx.annotation.PluralsRes
import app.sinceus.R
import app.sinceus.data.Texts
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.OutlinedButton
import app.sinceus.share.ShareCard
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sinceus.data.LiveSpan
import app.sinceus.data.LoveSettings
import app.sinceus.data.liveSpan
import kotlinx.coroutines.delay
import java.time.LocalDateTime

/** So lange bleibt eine angetippte Gesamtzahl stehen */
private const val FOCUS_MS = 4000L

private data class LiveRow(val key: String, val value: Long, @PluralsRes val unit: Int, val total: Long)

private fun LiveSpan.rows(): List<LiveRow> = buildList {
    if (years > 0) add(LiveRow("y", years.toLong(), R.plurals.years, years.toLong()))
    add(LiveRow("mo", months.toLong(), R.plurals.months, totalMonths))
    add(LiveRow("d", days.toLong(), R.plurals.days, totalDays))
    add(LiveRow("h", hours.toLong(), R.plurals.hours, totalHours))
    add(LiveRow("mi", minutes.toLong(), R.plurals.minutes, totalMinutes))
    add(LiveRow("s", seconds.toLong(), R.plurals.seconds, totalSeconds))
}

/** Sekundengenauer Live-Zähler auf eurem (weichgezeichneten) Foto. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LiveScreen(settings: LoveSettings, active: Boolean, now: LocalDateTime? = null) {
    var current by remember { mutableStateOf(now ?: LocalDateTime.now()) }
    LaunchedEffect(active, now) {
        while (active && now == null) {
            current = LocalDateTime.now()
            delay(1000 - System.currentTimeMillis() % 1000)
        }
    }
    val start = settings.startDateTime
    val future = start.isAfter(current)
    val span = liveSpan(start, current)
    // Angetippte Zeile zeigt kurz die Gesamtzahl, die anderen werden solange weichgezeichnet
    var focused by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(focused) {
        if (focused != null) {
            delay(FOCUS_MS)
            focused = null
        }
    }
    val context = LocalContext.current
    // Teilen als Bild, mit den aktuellen Live-Werten als Begleittext
    val shareLive = {
        val text = context.getString(
            R.string.share_text,
            settings.names,
            span.rows().filter { it.value > 0 }.joinToString(", ") { Texts.count(context, it.unit, it.value) },
        )
        ShareCard.share(context, settings, current.toLocalDate(), text)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF120B0C)),
    ) {
        CouplePhoto(
            settings,
            Modifier
                .fillMaxSize()
                .blur(28.dp)
                .alpha(0.55f),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f)),
        )
        FloatingHearts()

        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 88.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(settings.name1, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                BeatingHeart(Modifier.padding(horizontal = 10.dp), size = 22.dp)
                Text(settings.name2, style = MaterialTheme.typography.headlineSmall, color = Color.White)
            }
            Text(
                stringResource(if (future) R.string.live_until else R.string.live_together).uppercase(),
                style = LabelCaps,
                color = Color(0xFFFFB2B9),
                modifier = Modifier.padding(top = 32.dp, bottom = 16.dp),
            )
            span.rows().forEach { row ->
                val total = focused == row.key
                val dimmed = focused != null && !total
                // Weichzeichnen: die Schrift wird unsichtbar, nur ihr weicher Schatten bleibt stehen.
                // Funktioniert auf allen Android-Versionen (Modifier.blur erst ab Android 12).
                val soft by animateFloatAsState(if (dimmed) 1f else 0f, tween(350), label = "soft")
                val text = Texts.count(context, row.unit, if (total) row.total else row.value)
                AnimatedContent(
                    targetState = text,
                    transitionSpec = {
                        (slideInVertically { it / 2 } + fadeIn()) togetherWith
                            (slideOutVertically { -it / 2 } + fadeOut())
                    },
                    label = row.key,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { focused = if (total) null else row.key },
                            onLongClick = shareLive,
                        ),
                ) { t ->
                    val base = if (total) Color(0xFFFFB2B9) else Color.White
                    Text(
                        t,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontSize = 40.sp,
                            shadow = if (soft > 0f) {
                                Shadow(base.copy(alpha = 0.85f * soft), Offset.Zero, blurRadius = 42f * soft)
                            } else {
                                null
                            },
                        ),
                        fontWeight = FontWeight.SemiBold,
                        color = base.copy(alpha = 1f - soft),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    )
                }
            }
            OutlinedButton(
                onClick = shareLive,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                modifier = Modifier.padding(top = 32.dp),
            ) {
                Icon(Icons.Rounded.IosShare, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.share), color = Color.White, modifier = Modifier.padding(start = 8.dp))
            }
            Text(
                stringResource(R.string.live_hint),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

/** Langsam aufsteigende, halbtransparente Herzen im Hintergrund. */
@Composable
private fun FloatingHearts() {
    val transition = rememberInfiniteTransition(label = "hearts")
    // x-Position (Anteil der Breite), Größe, Dauer, Versatz
    val hearts = remember {
        listOf(
            listOf(0.08f, 34f, 14000f, 0.0f), listOf(0.26f, 22f, 11000f, 0.5f),
            listOf(0.45f, 44f, 17000f, 0.2f), listOf(0.63f, 26f, 12500f, 0.75f),
            listOf(0.82f, 38f, 15500f, 0.35f), listOf(0.92f, 20f, 10000f, 0.9f),
            listOf(0.35f, 18f, 9000f, 0.65f), listOf(0.72f, 30f, 13000f, 0.1f),
        )
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        hearts.forEachIndexed { i, (x, size, duration, phase) ->
            val progress by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(duration.toInt(), easing = LinearEasing), RepeatMode.Restart),
                label = "heart$i",
            )
            val p = (progress + phase) % 1f
            Icon(
                Icons.Rounded.Favorite,
                contentDescription = null,
                tint = Color(0xFFFF8FA3).copy(alpha = 0.16f * (1f - p)),
                modifier = Modifier
                    .offset(x = w * x, y = h * (1f - p) - size.dp)
                    .size(size.dp),
            )
        }
    }
}
