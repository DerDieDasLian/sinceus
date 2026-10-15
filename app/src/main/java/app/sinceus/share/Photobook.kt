package app.sinceus.share

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import app.sinceus.R
import app.sinceus.data.LoveSettings
import app.sinceus.data.Moment
import app.sinceus.data.Names
import app.sinceus.data.Texts
import app.sinceus.ui.AppFonts
import app.sinceus.widget.WidgetPhoto
import app.sinceus.widget.WidgetText
import java.io.OutputStream
import java.time.LocalDate

/**
 * Fotobuch als PDF im Format A5: ein Titelblatt mit eurer Teilen-Karte, danach eine Seite pro Moment
 * mit Fotos, Datum, Titel, Ort, Schlagwörtern und Notiz. Alles entsteht auf dem Handy.
 */
object Photobook {
    const val MIME = "application/pdf"

    // A5 in Punkt (1/72 Zoll)
    private const val PAGE_W = 420
    private const val PAGE_H = 595
    private const val MARGIN = 36f
    private const val CONTENT_W = PAGE_W - 2 * MARGIN

    /** Fotos in etwa doppelter Auflösung der Seite: scharf beim Drucken, die Datei bleibt trotzdem handlich */
    private const val PHOTO_SCALE = 2

    private const val INK = 0xFF22191A.toInt()
    private const val SOFT_INK = 0xFF6B5A5C.toInt()
    private const val ACCENT = 0xFF8B1E31.toInt()
    private const val COVER_BG = 0xFF3A0712.toInt()
    private const val COVER_SOFT = 0xE6FFE3E7.toInt()

    fun fileName(context: Context, today: LocalDate) = "${context.getString(R.string.photobook_file)} $today.pdf"

    /** Schreibt das Fotobuch mit [moments] (in dieser Reihenfolge) nach [out] */
    fun write(context: Context, s: LoveSettings, moments: List<Moment>, today: LocalDate, out: OutputStream) {
        val heading = typeface(context, s, heading = true)
        val body = typeface(context, s, heading = false)
        val doc = PdfDocument()
        try {
            var number = 1
            doc.page(number++) { cover(context, s, moments, today, heading, body) }
            moments.forEach { m -> doc.page(number) { momentPage(context, m, number, heading, body) }; number++ }
            doc.writeTo(out)
        } finally {
            doc.close()
        }
    }

    private inline fun PdfDocument.page(number: Int, draw: Canvas.() -> Unit) {
        val page = startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, number).create())
        page.canvas.draw()
        finishPage(page)
    }

    private fun typeface(context: Context, s: LoveSettings, heading: Boolean): Typeface {
        val classic = AppFonts.find(s.font).id == AppFonts.CLASSIC_ID
        if (classic) return if (heading) Typeface.create(Typeface.SERIF, Typeface.BOLD) else Typeface.DEFAULT
        return WidgetText.typeface(context, s.font, heading)
    }

    private fun paint(size: Float, face: Typeface, color: Int, weight: Int = 400) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        typeface = face
        this.color = color
        fontVariationSettings = "'wght' $weight"
    }

    /** Titelblatt: eure Teilen-Karte, darunter Titel, Zeitraum und Namen */
    private fun Canvas.cover(context: Context, s: LoveSettings, moments: List<Moment>, today: LocalDate, heading: Typeface, body: Typeface) {
        drawColor(COVER_BG)
        val card = ShareCard.render(context, s, today)
        val cardH = PAGE_W * ShareCard.HEIGHT / ShareCard.WIDTH.toFloat()
        drawBitmap(card, null, RectF(0f, 0f, PAGE_W.toFloat(), cardH), Paint(Paint.FILTER_BITMAP_FLAG))
        card.recycle()

        var y = cardH + 30f
        val title = context.getString(R.string.photobook_moments_title)
        drawText(title, MARGIN, y, fit(paint(22f, heading, 0xFFFFFFFF.toInt(), 700), title))
        y += 18f
        val first = moments.minOfOrNull { it.date }
        val last = moments.maxOfOrNull { it.date }
        val range = when {
            first == null || last == null -> Names.join(s.memberNames())
            first == last -> Texts.longDate(first)
            else -> context.getString(R.string.year_review_range, Texts.longDate(first), Texts.longDate(last))
        }
        drawText(range, MARGIN, y, fit(paint(10f, body, COVER_SOFT), range))
        val made = context.getString(R.string.photobook_made_with)
        drawText(made, MARGIN, PAGE_H - 18f, paint(7f, body, COVER_SOFT))
    }

    /** Eine Seite pro Moment */
    private fun Canvas.momentPage(context: Context, m: Moment, number: Int, heading: Typeface, body: Typeface) {
        drawColor(0xFFFFFFFF.toInt())
        var y = MARGIN
        val photos = m.photos
        if (photos.isNotEmpty()) {
            val h = CONTENT_W * 0.66f
            photo(photos[0], MARGIN, y, CONTENT_W, h)
            y += h
            val rest = photos.drop(1).take(3)
            if (rest.isNotEmpty()) {
                val gap = 8f
                val w = (CONTENT_W - gap * (rest.size - 1)) / rest.size
                val th = minOf(w * 0.75f, 110f)
                y += gap
                rest.forEachIndexed { i, p -> photo(p, MARGIN + i * (w + gap), y, w, th) }
                y += th
            }
            y += 22f
        } else {
            y += 12f
        }

        val date = Texts.longDate(m.date).uppercase()
        drawText(date, MARGIN, y, paint(8.5f, body, ACCENT, 600).apply { letterSpacing = 0.12f })
        y += 8f

        val title = layout(m.title, paint(22f, heading, INK, 700), maxLines = 3, lineSpacing = 1.1f)
        y = draw(title, y)

        val details = listOfNotNull(m.place.ifBlank { null }, m.tags.joinToString(" · ").ifBlank { null })
        details.forEach { line ->
            y += 4f
            y = draw(layout(line, paint(10f, body, SOFT_INK, 500), maxLines = 2), y)
        }

        if (m.note.isNotBlank()) {
            y += 10f
            val notePaint = paint(11f, body, INK)
            val room = PAGE_H - 48f - y
            val lineHeight = notePaint.fontSpacing * 1.3f
            val lines = (room / lineHeight).toInt()
            if (lines > 0) draw(layout(m.note, notePaint, maxLines = lines, lineSpacing = 1.3f), y)
        }

        // Fußzeile mit Seitenzahl
        val footer = paint(7.5f, body, SOFT_INK)
        drawText("Since Us", MARGIN, PAGE_H - 22f, footer)
        val n = number.toString()
        drawText(n, PAGE_W - MARGIN - footer.measureText(n), PAGE_H - 22f, footer)
    }

    /** Foto mittig zugeschnitten mit runden Ecken; fehlt die Datei, bleibt eine helle Fläche */
    private fun Canvas.photo(path: String, x: Float, y: Float, w: Float, h: Float) {
        val rect = RectF(x, y, x + w, y + h)
        val bmp = WidgetPhoto.photo(path, (w * PHOTO_SCALE).toInt(), (h * PHOTO_SCALE).toInt())
        save()
        clipPath(android.graphics.Path().apply { addRoundRect(rect, 10f, 10f, android.graphics.Path.Direction.CW) })
        if (bmp != null) {
            drawBitmap(bmp, null, rect, Paint(Paint.FILTER_BITMAP_FLAG))
            bmp.recycle()
        } else {
            drawRect(rect, Paint().apply { color = 0xFFFCEAEA.toInt() })
        }
        restore()
    }

    private fun layout(text: String, paint: TextPaint, maxLines: Int, lineSpacing: Float = 1.15f): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, CONTENT_W.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, lineSpacing)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()

    /** Zeichnet [layout] ab [y] und gibt die Unterkante zurück */
    private fun Canvas.draw(layout: StaticLayout, y: Float): Float {
        save()
        translate(MARGIN, y)
        layout.draw(this)
        restore()
        return y + layout.height
    }

    private fun fit(p: TextPaint, text: String): TextPaint {
        while (p.measureText(text) > CONTENT_W && p.textSize > 6f) p.textSize -= 0.5f
        return p
    }
}
