package app.sinceus.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import app.sinceus.MainActivity
import app.sinceus.R
import app.sinceus.data.LoveRepository
import app.sinceus.data.LoveSettings
import app.sinceus.data.Moment
import app.sinceus.data.Texts
import app.sinceus.data.formatNumber
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Countdown bis zum nächsten geplanten Moment, im Stil der Liebeskarte */
class CountdownWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = LoveRepository(context).current().forWidget()
        provideContent { CountdownContent(settings) }
    }

    companion object {
        /** Der nächste Moment ab heute (heute eingeschlossen), null = nichts geplant */
        fun next(moments: List<Moment>, today: LocalDate): Moment? =
            moments.filter { !it.date.isBefore(today) }.minWithOrNull(compareBy({ it.date }, { it.title }))
    }
}

@androidx.compose.runtime.Composable
private fun CountdownContent(settings: LoveSettings) {
    val context = LocalContext.current
    val today = LocalDate.now()
    val next = CountdownWidget.next(settings.visibleMoments, today)
    val size = LocalSize.current
    val wide = size.width >= 200.dp
    val flat = size.height < 110.dp
    val heading = WidgetText.typeface(context, settings.font, heading = true)
    val body = WidgetText.typeface(context, settings.font, heading = false)
    val room = size.width.value - 36f

    // Zu lange Texte verkleinern statt abschneiden
    fun line(text: String, typeface: android.graphics.Typeface, sizeSp: Float, color: Int, weight: Int = 400, tight: Boolean = false): WidgetText.Line {
        val first = WidgetText.render(context, text, typeface, sizeSp, color, weight, tight)
        if (first.widthDp <= room) return first
        val scale = (room / first.widthDp).coerceAtLeast(0.55f)
        return WidgetText.render(context, text, typeface, sizeSp, color, weight, tight, scale)
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(28.dp)
            .background(ImageProvider(R.drawable.widget_card_bg))
            .clickable(actionStartActivity<MainActivity>()),
    ) {
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
            if (next == null) {
                val title = context.getString(R.string.widget_countdown_empty)
                val hint = context.getString(R.string.widget_countdown_empty_hint)
                TextImage(line(title, heading, 16f, WHITE, weight = 600), title)
                Spacer(GlanceModifier.height(4.dp))
                TextImage(line(hint, body, 12f, SOFT), hint)
                return@Column
            }
            val days = ChronoUnit.DAYS.between(today, next.date)
            TextImage(line(next.title, heading, 13f, SOFT), next.title)
            Spacer(GlanceModifier.height(if (flat) 4.dp else 6.dp))
            val numberText = if (days == 0L) context.getString(R.string.widget_today) else formatNumber(days)
            val number = line(numberText, heading, if (flat) 30f else if (wide) 40f else 34f, WHITE, weight = 700, tight = true)
            if (days == 0L) {
                TextImage(number, numberText)
            } else {
                val label = context.resources.getQuantityString(R.plurals.widget_countdown_days, days.toInt())
                if (wide || flat) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        TextImage(number, numberText)
                        Spacer(GlanceModifier.width(8.dp))
                        TextImage(WidgetText.render(context, label, body, 14f, SOFT), label)
                    }
                } else {
                    TextImage(number, numberText)
                    Spacer(GlanceModifier.height(4.dp))
                    TextImage(WidgetText.render(context, label, body, 13f, SOFT), label)
                }
            }
            if (!flat) {
                Spacer(GlanceModifier.height(8.dp))
                val date = Texts.mediumDate(next.date)
                TextImage(line(date, body, 11f, SOFT), date)
            }
        }
    }
}

class CountdownWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CountdownWidget()
}
