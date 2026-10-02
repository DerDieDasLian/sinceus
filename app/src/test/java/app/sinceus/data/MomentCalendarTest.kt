package app.sinceus.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class MomentCalendarTest {
    @Test
    fun monthStartsOnTheRightWeekday() {
        // 1. Oktober 2026 ist ein Donnerstag
        val cells = MomentCalendar.cells(YearMonth.of(2026, 10), DayOfWeek.MONDAY)
        assertNull(cells[2])
        assertEquals(LocalDate.of(2026, 10, 1), cells[3])
        assertEquals(0, cells.size % 7)
        assertEquals(31, cells.count { it != null })
        // Mit Sonntag als erstem Tag rückt alles eins weiter
        assertEquals(LocalDate.of(2026, 10, 1), MomentCalendar.cells(YearMonth.of(2026, 10), DayOfWeek.SUNDAY)[4])
    }

    @Test
    fun anniversariesOnlyAfterTheFirstYear() {
        val moments = listOf(
            Moment("a", "Erstes Date", LocalDate.of(2024, 2, 29)),
            Moment("b", "Urlaub", LocalDate.of(2026, 2, 10)),
            Moment("c", "Ohne Erinnerung", LocalDate.of(2023, 2, 1), yearlyReminder = false),
        )
        val list = MomentCalendar.anniversaries(moments, YearMonth.of(2026, 2))
        assertEquals(listOf("a"), list.map { it.moment.id })
        assertEquals(LocalDate.of(2026, 2, 28), list[0].date)
        assertEquals(2, list[0].years)
    }
}
