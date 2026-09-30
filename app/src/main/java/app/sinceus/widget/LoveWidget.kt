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
import app.sinceus.data.Texts
import app.sinceus.data.formatNumber
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Kompaktes Widget in den Systemfarben (Material You): Tage zusammen + Weg zum nächsten besonderen Tag. */
class LoveWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = LoveRepository(context).current()
        provideContent { GlanceTheme { DaysContent(settings) } }
    }

    companion object {
        /** Aktualisiert alle Widgets der App. */
        suspend fun refresh(context: Context) {
            LoveWidget().updateAll(context)
            PhotoWidget().updateAll(context)
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

/** Großes Foto-Widget: euer Bild mit Namen und Tagen, der Ausschnitt passt sich der Widget-Größe an. */
class PhotoWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = LoveRepository(context).current()
        provideContent {
            val size = LocalSize.current
            val density = context.resources.displayMetrics.density
            val photo = androidx.compose.runtime.remember(settings, size) {
                WidgetPhoto.renderIcon(context, settings, size.width.value * density, size.height.value * density)
            }
            PhotoContent(settings, photo)
        }
    }
}

@androidx.compose.runtime.Composable
private fun PhotoContent(settings: LoveSettings, photo: android.graphics.drawable.Icon?) {
    val context = LocalContext.current
    val today = LocalDate.now()
    val days = LoveMath.together(settings.startDate, today).totalDays
    val future = settings.startDate.isAfter(today)
    val size = LocalSize.current
    val wide = size.width >= 200.dp
    val white = ColorProvider(Color.White)
    val soft = ColorProvider(Color(0xDDFFFFFF))

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(28.dp)
            .background(Color(0xFF2A1A1D))
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        if (photo != null) {
            Image(
                ImageProvider(photo),
                contentDescription = context.getString(R.string.your_photo),
                contentScale = androidx.glance.layout.ContentScale.Crop,
                modifier = GlanceModifier.fillMaxSize(),
            )
        }
        // Verlauf von unten für lesbare Schrift
        Image(
            ImageProvider(R.drawable.widget_scrim),
            contentDescription = null,
            contentScale = androidx.glance.layout.ContentScale.FillBounds,
            modifier = GlanceModifier.fillMaxSize(),
        )
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(14.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            val label = if (future) {
                context.getString(R.string.widget_days_until)
            } else {
                context.resources.getQuantityString(R.plurals.unit_days_together, days.toInt())
            }
            val number = TextStyle(color = white, fontSize = if (wide) 40.sp else 32.sp, fontWeight = FontWeight.Bold)
            if (wide) {
                Row(verticalAlignment = Alignment.Bottom, modifier = GlanceModifier.fillMaxWidth()) {
                    Text(formatNumber(days), style = number)
                    Spacer(GlanceModifier.width(6.dp))
                    Text(
                        label,
                        style = TextStyle(color = soft, fontSize = 13.sp),
                        maxLines = 1,
                        modifier = GlanceModifier.padding(bottom = 6.dp),
                    )
                }
            } else {
                // Schmales Widget: Beschriftung unter die Zahl
                Text(formatNumber(days), style = number)
                Text(label, style = TextStyle(color = soft, fontSize = 13.sp), maxLines = 1)
                Spacer(GlanceModifier.height(4.dp))
            }
            Text(
                "${settings.name1} ❤ ${settings.name2}",
                style = TextStyle(color = soft, fontSize = 14.sp, fontFamily = FontFamily.Serif),
                maxLines = 1,
            )
        }
    }
}

class LoveWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LoveWidget()
}

class PhotoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PhotoWidget()
}
