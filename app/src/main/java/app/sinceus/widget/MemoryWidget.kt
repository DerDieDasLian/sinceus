package app.sinceus.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.Icon
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
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import app.sinceus.MainActivity
import app.sinceus.R
import app.sinceus.data.LoveRepository
import app.sinceus.data.LoveSettings
import app.sinceus.data.MemoryPick
import app.sinceus.data.Moment
import app.sinceus.data.formatNumber
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.roundToInt

/** Erinnerung: jeden Tag ein anderer vergangener Moment, mit Foto und wie lange er her ist */
class MemoryWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = LoveRepository(context).current().forWidget()
        provideContent { MemoryContent(settings) }
    }

    companion object {
        /** Längste Seite des Fotos in Pixeln: scharf genug fürs Widget, klein genug für die Übertragung */
        private const val MAX_SIDE = 640f

        /**
         * Foto des Moments mit Abdunklung unten, als JPEG-Icon. Ein großes Bitmap direkt würde das
         * Widget-Update zu groß machen, dann lädt das Widget nicht mehr. Als JPEG bleibt es weit unter 400 KB.
         */
        internal fun photo(context: Context, path: String, widthDp: Float, heightDp: Float): ImageProvider? =
            jpeg(context, path, widthDp, heightDp)?.let { ImageProvider(Icon.createWithData(it, 0, it.size)) }

        internal fun jpeg(context: Context, path: String, widthDp: Float, heightDp: Float): ByteArray? {
            val density = context.resources.displayMetrics.density
            val scale = minOf(1f, MAX_SIDE / max(widthDp * density, heightDp * density))
            val w = (widthDp * density * scale).roundToInt().coerceAtLeast(1)
            val h = (heightDp * density * scale).roundToInt().coerceAtLeast(1)
            val bmp = WidgetPhoto.photo(path, w, h) ?: return null
            shade(bmp)
            val bytes = ByteArrayOutputStream().use {
                bmp.compress(Bitmap.CompressFormat.JPEG, 85, it)
                it.toByteArray()
            }
            bmp.recycle()
            return bytes
        }

        /** Verlauf von unten ins Weinrot, damit die Schrift auf jedem Foto lesbar ist */
        private fun shade(bmp: Bitmap) {
            val h = bmp.height.toFloat()
            Canvas(bmp).drawRect(
                0f, 0f, bmp.width.toFloat(), h,
                Paint().apply {
                    shader = LinearGradient(
                        0f, h * 0.35f, 0f, h,
                        intArrayOf(0x00000000, 0x992A0710.toInt(), 0xE63A0712.toInt()),
                        floatArrayOf(0f, 0.55f, 1f),
                        Shader.TileMode.CLAMP,
                    )
                },
            )
        }

        /** „vor 2 Jahren“, „vor 3 Monaten“ oder „vor 5 Tagen“ */
        fun ago(context: Context, m: Moment, today: LocalDate): String {
            val (span, n) = MemoryPick.ago(m.date, today)
            val res = when (span) {
                MemoryPick.Span.YEARS -> R.plurals.memory_years_ago
                MemoryPick.Span.MONTHS -> R.plurals.memory_months_ago
                MemoryPick.Span.DAYS -> R.plurals.days_ago
            }
            return context.resources.getQuantityString(res, n, formatNumber(n.toLong()))
        }
    }
}

@androidx.compose.runtime.Composable
private fun MemoryContent(settings: LoveSettings) {
    val context = LocalContext.current
    val today = LocalDate.now()
    val moment = MemoryPick.pick(settings.visibleMoments, today)
    val size = LocalSize.current
    val flat = size.height < 110.dp
    val heading = WidgetText.typeface(context, settings.font, heading = true)
    val body = WidgetText.typeface(context, settings.font, heading = false)
    val room = size.width.value - 36f
    // Im diskreten Modus keine Fotos auf dem Startbildschirm
    val photo = moment?.photoPath?.takeIf { !settings.discreet }
        ?.let { MemoryWidget.photo(context, it, size.width.value, size.height.value) }

    // Zu lange Texte verkleinern statt abschneiden
    fun line(text: String, typeface: android.graphics.Typeface, sizeSp: Float, color: Int, weight: Int = 400): WidgetText.Line {
        val first = WidgetText.render(context, text, typeface, sizeSp, color, weight)
        if (first.widthDp <= room) return first
        val scale = (room / first.widthDp).coerceAtLeast(0.55f)
        return WidgetText.render(context, text, typeface, sizeSp, color, weight, scale = scale)
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(28.dp)
            .background(ImageProvider(R.drawable.widget_card_bg))
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        if (photo != null) {
            Image(photo, contentDescription = null, contentScale = ContentScale.Crop, modifier = GlanceModifier.fillMaxSize())
        } else {
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                Image(
                    ImageProvider(settings.relationship.kind.widgetDeco),
                    contentDescription = null,
                    modifier = GlanceModifier.size(if (flat) 70.dp else 96.dp),
                )
            }
        }
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(horizontal = 18.dp, vertical = if (flat) 10.dp else 14.dp),
            verticalAlignment = if (photo != null) Alignment.Bottom else Alignment.CenterVertically,
        ) {
            if (moment == null) {
                val title = context.getString(R.string.widget_memory_empty)
                val hint = context.getString(R.string.widget_memory_empty_hint)
                TextImage(line(title, heading, 16f, WHITE, weight = 600), title)
                Spacer(GlanceModifier.height(4.dp))
                TextImage(line(hint, body, 12f, SOFT), hint)
                return@Column
            }
            val ago = MemoryWidget.ago(context, moment, today)
            TextImage(line(ago, body, 12f, SOFT), ago)
            Spacer(GlanceModifier.height(2.dp))
            TextImage(line(moment.title, heading, if (flat) 18f else 22f, WHITE, weight = 700), moment.title)
        }
    }
}

class MemoryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MemoryWidget()
}
