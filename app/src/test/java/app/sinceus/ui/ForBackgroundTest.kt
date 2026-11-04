package app.sinceus.ui

import app.sinceus.data.LoveSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.LocalDate

class ForBackgroundTest {
    private val base = LoveSettings.couple("Alex", "Sam", LocalDate.of(2025, 3, 15))

    @Test
    fun appOnlyChangesDoNotRedrawWidgets() {
        val changed = base.copy(homePage = 1, newestFirst = true, changelogSeen = 48, updateVersion = "9.9.9", celebrate = false)
        assertEquals(base.forBackground(), changed.forBackground())
    }

    @Test
    fun widgetChangesRedrawWidgets() {
        assertNotEquals(base.forBackground(), base.copy(discreet = true).forBackground())
        assertNotEquals(base.forBackground(), base.copy(font = "inter").forBackground())
        assertNotEquals(base.forBackground(), base.copy(notifyHour = 7).forBackground())
        assertNotEquals(base.forBackground(), base.copy(showMoments = false).forBackground())
    }
}
