package de.loveapp.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.os.Build
import androidx.compose.ui.graphics.toArgb
import de.loveapp.data.LoveSettings
import de.loveapp.ui.Presets
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Bereitet das Bild fuers Widget vor: passend zur Widget-Groesse zugeschnitten (mit dem
 * in der App gewaehlten Ausschnitt) und klein genug fuer das Widget-System.
 */
object WidgetPhoto {
    /** Widgets duerfen nur begrenzt grosse Bilder uebertragen */
    private const val MAX_SIDE = 720f

    fun render(context: Context, s: LoveSettings, widthPx: Float, heightPx: Float): Bitmap? {
        if (widthPx <= 0f || heightPx <= 0f) return null
        val scale = minOf(1f, MAX_SIDE / max(widthPx, heightPx))
        val w = (widthPx * scale).roundToInt().coerceAtLeast(1)
        val h = (heightPx * scale).roundToInt().coerceAtLeast(1)
        val path = s.photoPath ?: return gradient(s.presetIndex, w, h)
        val source = decode(File(path), max(w, h)) ?: return gradient(s.presetIndex, w, h)
        return crop(source, w, h, s.focusX, s.focusY, s.zoom)
    }

    private fun decode(file: File, targetSide: Int): Bitmap? = try {
        if (Build.VERSION.SDK_INT >= 28) {
            // ImageDecoder beachtet die Drehung aus den Foto-Metadaten (EXIF)
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
                val side = max(info.size.width, info.size.height)
                decoder.setTargetSampleSize(max(1, side / (targetSide * 2)))
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            val side = max(bounds.outWidth, bounds.outHeight)
            BitmapFactory.decodeFile(
                file.path,
                BitmapFactory.Options().apply { inSampleSize = max(1, side / (targetSide * 2)) },
            )
        }
    } catch (_: Exception) {
        null
    }

    /** Gleiche Logik wie in der App: Fokuspunkt -1..1 und Zoom, dann auf w x h skalieren. */
    internal fun crop(src: Bitmap, w: Int, h: Int, fx: Float, fy: Float, zoom: Float): Bitmap {
        val target = w.toFloat() / h
        var cw = src.width.toFloat()
        var ch = cw / target
        if (ch > src.height) {
            ch = src.height.toFloat()
            cw = ch * target
        }
        cw /= zoom
        ch /= zoom
        val left = ((src.width - cw) * (fx + 1) / 2).roundToInt()
        val top = ((src.height - ch) * (fy + 1) / 2).roundToInt()
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(
            src,
            Rect(left, top, left + cw.roundToInt(), top + ch.roundToInt()),
            Rect(0, 0, w, h),
            Paint(Paint.FILTER_BITMAP_FLAG),
        )
        return out
    }

    private fun gradient(index: Int, w: Int, h: Int): Bitmap {
        val colors = Presets.getOrElse(index) { Presets[0] }.colors.map { it.toArgb() }.toIntArray()
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val paint = Paint().apply {
            shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), colors, null, Shader.TileMode.CLAMP)
        }
        Canvas(out).drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        return out
    }
}
