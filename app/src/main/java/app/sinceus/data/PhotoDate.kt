package app.sinceus.data

import android.content.Context
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Aufnahmedatum eines Fotos, damit ein neuer Moment gleich das richtige Datum bekommt */
object PhotoDate {
    /** Aus den Foto-Metadaten (EXIF), sonst aus der Galerie; null = unbekannt oder unplausibel */
    suspend fun read(context: Context, uri: Uri, today: LocalDate = LocalDate.now()): LocalDate? = withContext(Dispatchers.IO) {
        val exif = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val e = ExifInterface(input)
                e.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) ?: e.getAttribute(ExifInterface.TAG_DATETIME)
            }
        }.getOrNull()
        val date = parse(exif) ?: runCatching { fromGallery(context, uri) }.getOrNull()
        date?.takeIf { plausible(it, today) }
    }

    /** EXIF schreibt "2025:03:15 20:30:00"; kaputte Werte wie "0000:00:00" ergeben null */
    internal fun parse(value: String?): LocalDate? {
        val m = value?.trim()?.let { Regex("""^(\d{4})[:\-](\d{2})[:\-](\d{2})""").find(it) } ?: return null
        return runCatching { LocalDate.of(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt()) }.getOrNull()
    }

    /** Keine Daten aus der Zukunft oder aus Kamera-Standardwerten (z. B. 1970) */
    internal fun plausible(date: LocalDate, today: LocalDate): Boolean =
        date.year >= 1990 && !date.isAfter(today)

    @Suppress("InlinedApi")
    private fun fromGallery(context: Context, uri: Uri): LocalDate? =
        context.contentResolver.query(uri, arrayOf(MediaStore.Images.Media.DATE_TAKEN), null, null, null)?.use { c ->
            if (!c.moveToFirst() || c.isNull(0)) return null
            val millis = c.getLong(0).takeIf { it > 0 } ?: return null
            Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        }
}
