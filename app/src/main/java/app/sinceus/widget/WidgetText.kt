package app.sinceus.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import app.sinceus.ui.AppFonts
import kotlin.math.ceil

/**
 * Widgets können keine eigenen Schriften. Deshalb wird Text in der gewählten App-Schrift
 * als kleines Bild gezeichnet, so sieht das Widget aus wie die App.
 */
object WidgetText {
    class Line(val bitmap: Bitmap, val widthDp: Float, val heightDp: Float)

    /** Schrift für Überschriften (heading = true) oder normalen Text; null = Systemschrift */
    fun typeface(context: Context, fontId: String, heading: Boolean): Typeface {
        val font = AppFonts.find(fontId)
        val (head, body) = AppFonts.files(font)
        val res = if (heading) head else body
        val loaded = res?.let { runCatching { ResourcesCompat.getFont(context, it) }.getOrNull() }
        return loaded ?: if (heading && font.id == AppFonts.CLASSIC_ID) Typeface.SERIF else Typeface.DEFAULT
    }

    /**
     * Zeichnet [text] in [sizeSp] und [weight] (100 bis 900). [tight] schneidet oben und unten
     * den Leerraum ab, für große Zahlen ohne Unterlängen.
     */
    fun render(
        context: Context,
        text: String,
        typeface: Typeface,
        sizeSp: Float,
        color: Int,
        weight: Int = 400,
        tight: Boolean = false,
        scale: Float = 1f,
    ): Line {
        val metrics = context.resources.displayMetrics
        val density = metrics.density
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = sizeSp * scale * density * context.resources.configuration.fontScale
            this.color = color
            // Variable Schriften: Stärke über die Achse "wght"; bei festen Schriften passiert nichts
            fontVariationSettings = "'wght' $weight"
        }
        val width = ceil(paint.measureText(text)).toInt().coerceAtLeast(1) + 2
        val top: Float
        val height: Int
        if (tight) {
            val bounds = Rect()
            paint.getTextBounds(text, 0, text.length, bounds)
            top = -bounds.top.toFloat() + 1
            height = bounds.height() + 2
        } else {
            val fm = paint.fontMetrics
            top = -fm.ascent
            height = ceil(fm.descent - fm.ascent).toInt()
        }
        val bitmap = Bitmap.createBitmap(width, height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawText(text, 1f, top, paint)
        return Line(bitmap, width / density, height / density)
    }
}
