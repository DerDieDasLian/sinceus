package app.sinceus.widget

import app.sinceus.data.Moment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class CountdownWidgetTest {
    private val today = LocalDate.of(2025, 8, 15)

    @Test
    fun picksTheNextMomentIncludingToday() {
        val moments = listOf(
            Moment("past", "Erstes Date", LocalDate.of(2025, 2, 14)),
            Moment("later", "Urlaub", LocalDate.of(2025, 9, 1)),
            Moment("soon", "Konzert", LocalDate.of(2025, 8, 20)),
        )
        assertEquals("soon", CountdownWidget.next(moments, today)?.id)
        assertEquals("today", CountdownWidget.next(moments + Moment("today", "Kino", today), today)?.id)
    }

    @Test
    fun nothingPlanned() {
        assertNull(CountdownWidget.next(listOf(Moment("past", "Erstes Date", LocalDate.of(2025, 2, 14))), today))
        assertNull(CountdownWidget.next(emptyList(), today))
    }
}
