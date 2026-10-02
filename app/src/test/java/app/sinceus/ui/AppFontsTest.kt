package app.sinceus.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AppFontsTest {
    @Test
    fun idsAreUnique() {
        val ids = AppFonts.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun emptyOrUnknownFallsBackToDefault() {
        assertEquals(AppFonts.DEFAULT_ID, AppFonts.find("").id)
        assertEquals(AppFonts.DEFAULT_ID, AppFonts.find("gibt-es-nicht").id)
        assertEquals("funnel", AppFonts.find("funnel").id)
    }

    @Test
    fun everyFontHasWidgetFiles() {
        AppFonts.all.filter { it.id != AppFonts.CLASSIC_ID }.forEach { font ->
            assertNotNull("Keine Widget-Schrift für ${font.id}", AppFonts.files(font).first)
        }
    }
}
