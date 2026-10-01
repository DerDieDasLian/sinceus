package app.sinceus.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import androidx.core.graphics.PathParser
import app.sinceus.R
import app.sinceus.data.LoveMath
import app.sinceus.data.LoveSettings
import app.sinceus.data.Texts
import app.sinceus.data.formatNumber
import app.sinceus.widget.WidgetPhoto
import java.io.File
import java.time.LocalDate

/**
 * Teilen als Bild: euer Foto mit Namen, Tagen und gemeinsamer Zeit im Hochformat (4:5),
 * passend für Status und Stories. Gezeichnet mit Canvas, ohne sichtbares Fenster.
 */
object ShareCard {
    const val WIDTH = 1080
    const val HEIGHT = 1350
    private const val MARGIN = 88f

    fun render(context: Context, s: LoveSettings, today: LocalDate): Bitmap {
        val out = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val w = WIDTH.toFloat()
        val h = HEIGHT.toFloat()

        // Foto (mit dem Ausschnitt aus der App) oder Farbverlauf
        WidgetPhoto.render(context, s, w, h, maxSide = HEIGHT.toFloat())?.let {
            canvas.drawBitmap(it, null, RectF(0f, 0f, w, h), Paint(Paint.FILTER_BITMAP_FLAG))
        }
        // Verlauf von unten ins Weinrot, damit die Schrift auf jedem Foto lesbar ist
        canvas.drawRect(
            0f, 0f, w, h,
            Paint().apply {
                shader = LinearGradient(
                    0f, h * 0.30f, 0f, h,
                    intArrayOf(0x00000000, 0xB32A0710.toInt(), 0xF23A0712.toInt()),
                    floatArrayOf(0f, 0.55f, 1f),
                    Shader.TileMode.CLAMP,
                )
            },
        )
        canvas.drawRect(
            0f, 0f, w, h * 0.18f,
            Paint().apply {
                shader = LinearGradient(0f, 0f, 0f, h * 0.18f, 0x66000000, 0x00000000, Shader.TileMode.CLAMP)
            },
        )

        val serifBold = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        val sans = Typeface.create("sans-serif", Typeface.NORMAL)
        val sansMedium = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        val white = 0xFFFFFFFF.toInt()
        val soft = 0xE6FFE3E7.toInt()
        val pink = 0xFFFF8FA3.toInt()

        fun paint(size: Float, face: Typeface, color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            typeface = face
            this.color = color
        }

        // Kleiner Schriftzug oben, mit gezeichnetem Herz statt Emoji
        val brand = paint(40f, sansMedium, soft)
        canvas.drawText("Since Us", MARGIN, 120f, brand)
        drawHeart(canvas, MARGIN + brand.measureText("Since Us") + 12f, 120f - 30f, 34f, pink)

        val together = LoveMath.together(s.startDate, today)
        val future = s.startDate.isAfter(today)
        val maxWidth = w - 2 * MARGIN
        var y = h - MARGIN

        // Von unten nach oben: Datum, Zeitspanne, Tage, Namen
        val since = context.getString(R.string.together_since_long, Texts.longDate(s.startDate))
        canvas.drawText(since, MARGIN, y, paint(38f, sans, soft))
        y -= 64f

        val period = Texts.period(context, together.period)
        canvas.drawText(period, MARGIN, y, fit(paint(46f, sansMedium, white), period, maxWidth))
        y -= 72f

        val number = formatNumber(together.totalDays)
        val numberPaint = paint(230f, serifBold, white)
        canvas.drawText(number, MARGIN, y, numberPaint)
        val unit = if (future) {
            context.getString(R.string.widget_days_until)
        } else {
            context.resources.getQuantityString(R.plurals.unit_days_together, together.totalDays.toInt())
        }
        val unitX = MARGIN + numberPaint.measureText(number) + 24f
        canvas.drawText(unit, unitX, y - 18f, fit(paint(52f, sans, soft), unit, w - MARGIN - unitX))
        y -= 250f

        // Namen mit gezeichnetem Herz dazwischen; Platz fürs Herz wie zwei Leerzeichen plus Herzbreite
        val namePaint = paint(92f, serifBold, white)
        val heartSize = { namePaint.textSize * 0.62f }
        val gap = { namePaint.textSize * 0.25f }
        // Bei mehr als zwei Menschen steht zwischen allen Namen ein Herz
        val names = s.memberNames()
        val nameWidth = { names.sumOf { namePaint.measureText(it).toDouble() }.toFloat() + (names.size - 1) * (heartSize() + 2 * gap()) }
        while (nameWidth() > maxWidth && namePaint.textSize > 20f) namePaint.textSize -= 2f
        var x = MARGIN
        names.forEachIndexed { i, name ->
            if (i > 0) {
                drawHeart(canvas, x + gap(), y - namePaint.textSize * 0.62f, heartSize(), pink)
                x += heartSize() + 2 * gap()
            }
            canvas.drawText(name, x, y, namePaint)
            x += namePaint.measureText(name)
        }

        // Dünne Akzentlinie über den Namen
        canvas.drawRoundRect(RectF(MARGIN, y - 150f, MARGIN + 96f, y - 140f), 5f, 5f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pink })
        return out
    }

    /** Herz (wie das Icon in der App) mit linker oberer Ecke bei [x], [y] und Breite [size] */
    private fun drawHeart(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
        val path = PathParser.createPathFromPathData(HEART_PATH)
        val scale = size / 20f
        // Das Herz liegt im 24er-Raster bei x 2..22, y 3..21.35
        path.transform(Matrix().apply { setTranslate(-2f, -3f); postScale(scale, scale); postTranslate(x, y) })
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
    }

    private const val HEART_PATH =
        "M12,21.35l-1.45,-1.32C5.4,15.36 2,12.28 2,8.5 2,5.42 4.42,3 7.5,3c1.74,0 3.41,0.81 4.5,2.09" +
            "C13.09,3.81 14.76,3 16.5,3 19.58,3 22,5.42 22,8.5c0,3.78 -3.4,6.86 -8.55,11.54L12,21.35z"

    /** Schrift verkleinern, bis der Text in [maxWidth] passt. */
    private fun fit(p: Paint, text: String, maxWidth: Float): Paint {
        while (p.measureText(text) > maxWidth && p.textSize > 20f) p.textSize -= 2f
        return p
    }

    /** Bild erzeugen und über das Android-Teilen-Menü anbieten. [text] kommt als Begleittext mit. */
    fun share(context: Context, s: LoveSettings, today: LocalDate, text: String) {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, "since-us.png")
        file.outputStream().use { render(context, s, today).compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("image/png")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_TEXT, text)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri(null, uri)
        context.startActivity(
            Intent.createChooser(send, context.getString(R.string.share)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    /** Begleittext: „Alex & Sam sind schon 5 Monate zusammen“ */
    fun defaultText(context: Context, s: LoveSettings, today: LocalDate): String =
        context.getString(R.string.share_text, s.names, Texts.period(context, LoveMath.together(s.startDate, today).period))
}
