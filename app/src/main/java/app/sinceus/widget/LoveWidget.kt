package app.sinceus.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
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
import androidx.glance.appwidget.GlanceAppWidgetManager
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
        /**
         * Aktualisiert alle Widgets der App.
         *
         * Bewusst nicht über updateAll(): Glance merkt sich dafür, welcher Receiver zu welcher
         * Widget-Klasse gehört, und zwar über den Klassennamen. Den verkürzt R8 in der fertigen App,
         * und nach einem Update können die Kurznamen vertauscht sein. Dann zeichnete updateAll() die
         * Liebeskarte als Zähler-Widget und die nächste Aktualisierung wieder richtig, das Widget
         * sprang hin und her. Hier geht es direkt über die Widget-IDs des jeweiligen Receivers.
         */
        suspend fun refresh(context: Context) {
            update(context, LoveWidgetReceiver::class.java, LoveWidget())
            update(context, PhotoWidgetReceiver::class.java, CardWidget())
        }

        private suspend fun update(context: Context, receiver: Class<*>, widget: GlanceAppWidget) {
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, receiver))
            val glance = GlanceAppWidgetManager(context)
            ids.forEach { id -> runCatching { widget.update(context, glance.getGlanceIdBy(id)) } }
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
            // Text in der App-Schrift als Bild, Widgets können selbst keine eigenen Schriften
            val heading = WidgetText.typeface(context, settings.font, heading = true)
            val body = WidgetText.typeface(context, settings.font, heading = false)
            val room = size.width.value - 36f
            // Gezeichnetes Herz statt Emoji
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Im diskreten Modus nur Anfangsbuchstaben; ab vier Menschen als eine Zeile ohne Herzen
                val names = settings.shownNames().let { if (it.size > 3) listOf(Names.join(it)) else it }
                var lines = names.map { WidgetText.render(context, it, heading, 13f, SOFT) }
                // Zu lange Namen verkleinern statt abschneiden
                val total = lines.sumOf { it.widthDp.toDouble() } + (names.size - 1) * 20
                if (total > room) {
                    val scale = (room / total).toFloat().coerceAtLeast(0.6f)
                    lines = names.map { WidgetText.render(context, it, heading, 13f, SOFT, scale = scale) }
                }
                lines.forEachIndexed { i, line ->
                    if (i > 0) {
                        Image(
                            ImageProvider(R.drawable.ic_heart_small),
                            contentDescription = null,
                            modifier = GlanceModifier.padding(horizontal = 4.dp).size(12.dp),
                        )
                    }
                    TextImage(line, names[i])
                }
            }
            Spacer(GlanceModifier.height(if (flat) 4.dp else 6.dp))
            val number = WidgetText.render(
                context,
                formatNumber(days),
                heading,
                if (flat) 30f else if (wide) 40f else 34f,
                WHITE,
                weight = 700,
                tight = true,
            )
            if (wide || flat) {
                Row(verticalAlignment = Alignment.Bottom) {
                    TextImage(number, formatNumber(days))
                    Spacer(GlanceModifier.width(8.dp))
                    TextImage(WidgetText.render(context, label, body, 14f, SOFT), label)
                }
            } else {
                // Schmale Karte: Beschriftung unter die Zahl
                TextImage(number, formatNumber(days))
                Spacer(GlanceModifier.height(4.dp))
                TextImage(WidgetText.render(context, label, body, 13f, SOFT), label)
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
                val nextText = context.getString(R.string.widget_next, Texts.milestoneTitle(context, next))
                TextImage(WidgetText.render(context, nextText, body, 11f, SOFT), nextText)
            }
        }
    }
}

private const val WHITE = 0xFFFFFFFF.toInt()
private const val SOFT = 0xD9FFE3E7.toInt()

/** Ein als Bild gezeichneter Text, für Bildschirmleser mit dem Text als Beschreibung */
@androidx.compose.runtime.Composable
private fun TextImage(line: WidgetText.Line, description: String) {
    Image(
        ImageProvider(line.bitmap),
        contentDescription = description,
        modifier = GlanceModifier.width(line.widthDp.dp).height(line.heightDp.dp),
    )
}

class LoveWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LoveWidget()
}

// Name bleibt „PhotoWidget…“, damit bereits platzierte Widgets nach dem Update weiter funktionieren
class PhotoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CardWidget()
}
