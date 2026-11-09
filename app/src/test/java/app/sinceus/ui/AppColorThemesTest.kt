package app.sinceus.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppColorThemesTest {
    private fun contrast(a: Color, b: Color): Float {
        val (dark, light) = listOf(a.luminance(), b.luminance()).sorted()
        return (light + 0.05f) / (dark + 0.05f)
    }

    @Test
    fun idsAreUnique() {
        val ids = AppColorThemes.all.map { it.id } + AppColorThemes.SYSTEM
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun emptyOrUnknownFallsBackToWine() {
        assertEquals("", AppColorThemes.find("").id)
        assertEquals("", AppColorThemes.find("gibt-es-nicht").id)
        assertEquals("jade", AppColorThemes.find("jade").id)
    }

    @Test
    fun customHueIsStoredAndRead() {
        assertEquals("hue:200", AppColorThemes.customId(200))
        assertEquals("hue:10", AppColorThemes.customId(370))
        assertEquals(200, AppColorThemes.customHue("hue:200"))
        assertEquals(null, AppColorThemes.customHue("jade"))
        assertEquals(null, AppColorThemes.customHue("hue:abc"))
        assertEquals("hue:200", AppColorThemes.find("hue:200").id)
        assertEquals("", AppColorThemes.find("hue:abc").id)
    }

    @Test
    fun wineIsUnchanged() {
        val wine = AppColorThemes.find("")
        assertEquals(Color(0xFF8B1E31), wine.light.primary)
        assertEquals(Color(0xFFFFB2B9), wine.dark.primary)
        assertEquals(Color(0xFFFF5C77), wine.accent.heart)
    }

    @Test
    fun textStaysReadable() {
        (AppColorThemes.all + (0 until 360 step 15).map { AppColorThemes.custom(it) }).forEach { theme ->
            listOf(theme.light, theme.dark).forEach { s ->
                assertTrue("${theme.id}: Text auf Primärfarbe", contrast(s.primary, s.onPrimary) >= 4.5f)
                assertTrue("${theme.id}: Text auf Hintergrund", contrast(s.background, s.onBackground) >= 7f)
                assertTrue("${theme.id}: Nebentext", contrast(s.background, s.onSurfaceVariant) >= 4.5f)
            }
            assertTrue("${theme.id}: Auswahl auf Weiß", contrast(theme.accent.strong, Color.White) >= 4.5f)
            assertTrue("${theme.id}: Live-Zähler", contrast(theme.accent.soft, theme.accent.night) >= 4.5f)
        }
    }
}
