package app.sinceus.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Fotos werden beim Hinzufügen auf gute Handyqualität verkleinert (längste Seite [MAX_SIDE] Pixel).
 * Das Original bleibt in der Galerie. So bleiben Sicherungen und Abgleich-Dateien klein.
 */
object PhotoShrink {
    const val MAX_SIDE = 2048
    private const val QUALITY = 88

    /** Kleine Dateien bleiben unverändert, damit nichts unnötig neu gespeichert wird */
    private const val SMALL_BYTES = 1_500_000L

    /**
     * Kopiert das Bild von [uri] verkleinert nach [target]. Klappt das Verkleinern nicht,
     * wird es unverändert kopiert. false = gar nicht lesbar.
     */
    fun copy(context: Context, uri: Uri, target: File): Boolean {
        val resolver = context.contentResolver
        val copied = runCatching {
            resolver.openInputStream(uri)?.use { input -> target.outputStream().use { input.copyTo(it) } } != null
        }.getOrDefault(false)
        if (!copied) {
            target.delete()
            return false
        }
        shrink(target)
        return true
    }

    /** Verkleinert ältere, große Fotos im App-Speicher. Liefert die Zahl der verkleinerten Fotos. */
    fun shrinkAll(dirs: List<File>): Int {
        val files = dirs.flatMap { it.listFiles()?.toList().orEmpty() }.filter { it.isFile }
        // Reste eines abgebrochenen Verkleinerns wegräumen
        files.filter { it.name.endsWith(".tmp") }.forEach { it.delete() }
        return files.filterNot { it.name.endsWith(".tmp") }.count { runCatching { shrink(it) }.getOrDefault(false) }
    }

    /**
     * Verkleinert die Datei an Ort und Stelle, wenn sie groß ist. Geschrieben wird erst in eine
     * Zwischendatei, damit das Foto bei einem Absturz nie kaputt ist. true = verkleinert.
     */
    internal fun shrink(file: File): Boolean {
        if (file.length() <= SMALL_BYTES) return false
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || max(bounds.outWidth, bounds.outHeight) <= MAX_SIDE) return false
        val bitmap = runCatching { decode(file) }.getOrNull() ?: return false
        val temp = File(file.parentFile, file.name + ".tmp")
        val ok = runCatching {
            temp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, it) }
        }.getOrDefault(false)
        bitmap.recycle()
        if (!ok || temp.length() == 0L || !temp.renameTo(file)) {
            temp.delete()
            return false
        }
        return true
    }

    /** Neue Größe, bei der die längste Seite höchstens [MAX_SIDE] ist */
    internal fun targetSize(width: Int, height: Int): Pair<Int, Int> {
        val scale = minOf(1f, MAX_SIDE.toFloat() / max(width, height))
        return (width * scale).roundToInt().coerceAtLeast(1) to (height * scale).roundToInt().coerceAtLeast(1)
    }

    private fun decode(file: File): Bitmap? {
        if (Build.VERSION.SDK_INT >= 28) {
            runCatching {
                // ImageDecoder dreht das Bild passend zu den Foto-Metadaten (EXIF)
                return ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
                    val (w, h) = targetSize(info.size.width, info.size.height)
                    decoder.setTargetSize(w, h)
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        val raw = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return null
        val (w, h) = targetSize(raw.width, raw.height)
        val matrix = Matrix().apply {
            postScale(w.toFloat() / raw.width, h.toFloat() / raw.height)
            postRotate(rotation(file))
        }
        val result = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
        if (result !== raw) raw.recycle()
        return result
    }

    /** Drehung aus den Foto-Metadaten, die BitmapFactory selbst nicht beachtet */
    private fun rotation(file: File): Float = when (
        runCatching { ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1) }.getOrDefault(1)
    ) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
}
