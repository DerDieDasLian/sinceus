package app.sinceus.share

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import app.sinceus.data.LoveSettings
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import java.util.Locale

/** Rendert das Teilen-Bild nach app/build/screenshots/share. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ShareCardTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val settings = LoveSettings.couple("Alex", "Sam", LocalDate.of(2025, 3, 15))
    private val today = LocalDate.of(2025, 8, 15)

    @Test
    @Config(qualifiers = "de")
    fun german() = save("de", Locale.GERMANY)

    @Test
    @Config(qualifiers = "en")
    fun english() = save("en", Locale.US)

    @Test
    @Config(qualifiers = "de")
    fun longNamesStayInside() {
        Locale.setDefault(Locale.GERMANY)
        val bitmap = ShareCard.render(context, LoveSettings.couple("Maximiliane-Sophie", "Konstantin-Alexander", LocalDate.of(2025, 3, 15)), today)
        assertEquals(ShareCard.WIDTH, bitmap.width)
        save("de_long", Locale.GERMANY, LoveSettings.couple("Maximiliane-Sophie", "Konstantin-Alexander", LocalDate.of(2025, 3, 15)))
    }

    private fun save(name: String, locale: Locale, s: LoveSettings = settings) {
        Locale.setDefault(locale)
        val bitmap = ShareCard.render(context, s, today)
        assertEquals(ShareCard.HEIGHT, bitmap.height)
        val dir = File("build/screenshots/share").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
