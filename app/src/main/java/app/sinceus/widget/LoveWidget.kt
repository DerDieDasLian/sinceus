package app.sinceus.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import app.sinceus.MainActivity
import app.sinceus.R
import app.sinceus.data.LoveMath
import app.sinceus.data.LoveRepository
import app.sinceus.data.LoveSettings
import app.sinceus.data.Names
import app.sinceus.data.Texts
import app.sinceus.data.formatNumber
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Kompaktes Widget in den Systemfarben (Material You): Tage zusammen + Weg zum nächsten besonderen Tag. */
class LoveWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = LoveRepository(context).current().forWidget()
        provideContent { GlanceTheme { DaysContent(settings) } }
    }

    companion object {
        /** Aktualisiert alle Widgets der App. */
        suspend fun refresh(context: Context) {
            LoveWidget().updateAll(context)
            CardWidget().updateAll(context)
        }
    }
}

@androidx.compose.runtime.Composable
private fun DaysContent(settings: LoveSettings) {
    val context = LocalContext.current
    val today = LocalDate.now()
    val days = LoveMath.together(settings.startDate, today).totalDays
    val future = settings.startDate.isAfter(today)
    val next = LoveMath.upcoming(settings.startDate, today, 1).firstOrNull()
    val tall = LocalSize.current.height >= 110.dp
    val colors = GlanceTheme.colors

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(28.dp)
            .background(colors.widgetBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                ImageProvider(R.drawable.ic_notification),
                contentDescription = null,
                colorFilter = androidx.glance.ColorFilter.tint(colors.primary),
                modifier = GlanceModifier.size(26.dp),
            )
            Spacer(GlanceModifier.width(8.dp))
            Text(
                formatNumber(days),
                style = TextStyle(color = colors.onSurface, fontSize = 38.sp, fontWeight = FontWeight.Bold),
            )
        }
        Text(
            if (future) {
                context.getString(R.string.widget_days_until)
            } else {
                context.resources.getQuantityString(R.plurals.unit_days_together, days.toInt())
            },
            style = TextStyle(color = colors.onSurfaceVariant, fontSize = 14.sp),
            maxLines = 1,
        )
        if (tall && next != null && !future) {
            // Fortschritt vom letzten bis zum nächsten besonderen Tag
            val previous = LoveMath.previousMilestoneDate(settings.startDate, today)
            val total = ChronoUnit.DAYS.between(previous, next.date).coerceAtLeast(1)
            val done = ChronoUnit.DAYS.between(previous, today)
            Spacer(GlanceModifier.height(10.dp))
            LinearProgressIndicator(
                progress = done.toFloat() / total,
                modifier = GlanceModifier.width(90.dp).height(4.dp),
                color = colors.primary,
                backgroundColor = colors.surfaceVariant,
            )
            Spacer(GlanceModifier.height(6.dp))
            Text(
                context.getString(R.string.widget_next, Texts.milestoneTitle(context, next)),
                style = TextStyle(color = colors.onSurfaceVariant, fontSize = 11.sp),
                maxLines = 1,
            )
        }
    }
}

/**
 * Große Widget-Karte ohne Foto: Namen, Tage zusammen und der Weg zum nächsten besonderen Tag
 * auf einem Weinrot-Verlauf. Ohne Bild bleibt das Update klein und das Widget lädt immer zuverlässig.
 */
class CardWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = LoveRepository(context).current().forWidget()
        provideContent { CardContent(settings) }
    }
}

@androidx.compose.runtime.Composable
private fun CardContent(settings: LoveSettings) {
    val context = LocalContext.current
    val today = LocalDate.now()
    val days = LoveMath.together(settings.startDate, today).totalDays
    val future = settings.startDate.isAfter(today)
    val next = LoveMath.upcoming(settings.startDate, today, 1).firstOrNull()
    val size = LocalSize.current
    val wide = size.width >= 200.dp
    // Flache Karte (z. B. 4x1): nur Namen und Zahl, ab mittlerer Höhe auch der nächste besondere Tag
    val flat = size.height < 110.dp
    val tall = size.height >= 140.dp
    val white = ColorProvider(Color.White)
    val soft = ColorProvider(Color(0xD9FFE3E7))
    val pink = ColorProvider(Color(0xFFFFB2B9))
    val label = if (future) {
        context.getString(R.string.widget_days_until)
    } else {
        context.resources.getQuantityString(R.plurals.unit_days_together, days.toInt())
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(28.dp)
            .background(ImageProvider(R.drawable.widget_card_bg))
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        // Großes, kaum sichtbares Herz als Dekoration unten rechts
        Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
            Image(
                ImageProvider(R.drawable.widget_heart_deco),
                contentDescription = null,
                modifier = GlanceModifier.size(if (flat) 70.dp else if (wide) 110.dp else 84.dp),
            )
        }
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(horizontal = 18.dp, vertical = if (flat) 10.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Gezeichnetes Herz statt Emoji
            Row(verticalAlignment = Alignment.CenterVertically) {
                val nameStyle = TextStyle(color = soft, fontSize = 13.sp, fontFamily = FontFamily.Serif)
                // Im diskreten Modus nur Anfangsbuchstaben; ab vier Menschen als eine Zeile ohne Herzen
                val names = settings.shownNames()
                if (names.size > 3) {
                    Text(Names.join(names), style = nameStyle, maxLines = 1)
                } else {
                    names.forEachIndexed { i, name ->
                        if (i > 0) {
                            Image(
                                ImageProvider(R.drawable.ic_heart_small),
                                contentDescription = null,
                                modifier = GlanceModifier.padding(horizontal = 4.dp).size(12.dp),
                            )
                        }
                        Text(name, style = nameStyle, maxLines = 1)
                    }
                }
            }
            Spacer(GlanceModifier.height(if (flat) 0.dp else 4.dp))
            if (wide || flat) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        formatNumber(days),
                        style = TextStyle(color = white, fontSize = if (flat) 32.sp else 40.sp, fontWeight = FontWeight.Bold),
                    )
                    Spacer(GlanceModifier.width(8.dp))
                    Text(
                        label,
                        style = TextStyle(color = soft, fontSize = 14.sp),
                        maxLines = 1,
                        modifier = GlanceModifier.padding(bottom = if (flat) 5.dp else 7.dp),
                    )
                }
            } else {
                // Schmale Karte: Beschriftung unter die Zahl
                Text(
                    formatNumber(days),
                    style = TextStyle(color = white, fontSize = 34.sp, fontWeight = FontWeight.Bold),
                )
                Text(label, style = TextStyle(color = soft, fontSize = 13.sp), maxLines = 1)
            }
            if (tall && next != null && !future) {
                // Fortschritt vom letzten bis zum nächsten besonderen Tag
                val previous = LoveMath.previousMilestoneDate(settings.startDate, today)
                val total = ChronoUnit.DAYS.between(previous, next.date).coerceAtLeast(1)
                val done = ChronoUnit.DAYS.between(previous, today)
                Spacer(GlanceModifier.height(10.dp))
                LinearProgressIndicator(
                    progress = done.toFloat() / total,
                    modifier = GlanceModifier.fillMaxWidth().height(4.dp),
                    color = pink,
                    backgroundColor = ColorProvider(Color(0x33FFFFFF)),
                )
                Spacer(GlanceModifier.height(5.dp))
                Text(
                    context.getString(R.string.widget_next, Texts.milestoneTitle(context, next)),
                    style = TextStyle(color = soft, fontSize = 11.sp),
                    maxLines = 1,
                )
            }
        }
    }
}

class LoveWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LoveWidget()
}

// Name bleibt „PhotoWidget…“, damit bereits platzierte Widgets nach dem Update weiter funktionieren
class PhotoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CardWidget()
}
