package app.sinceus.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FunFactsTest {
    private fun value(kind: FunFacts.Kind, start: LocalDate, today: LocalDate) =
        FunFacts.all(start, today).firstOrNull { it.kind == kind }?.value ?: 0

    @Test
    fun countsWeekendsHolidaysAndSeasons() {
        val start = LocalDate.of(2025, 3, 15) // ein Samstag
        val today = LocalDate.of(2026, 1, 1)
        // Der Starttag zählt nicht mit, erst die Samstage danach
        assertEquals(41L, value(FunFacts.Kind.WEEKENDS, start, today))
        assertEquals(1L, value(FunFacts.Kind.CHRISTMAS, start, today))
        assertEquals(1L, value(FunFacts.Kind.NEW_YEARS, start, today))
        // 20.03., 21.06., 22.09. und 21.12.
        assertEquals(4L, value(FunFacts.Kind.SEASONS, start, today))
        assertEquals(0L, value(FunFacts.Kind.VALENTINES, start, today))
        assertEquals(1L, value(FunFacts.Kind.PRIDE_MONTHS, start, today))
        assertEquals(1L, value(FunFacts.Kind.HALLOWEENS, start, today))
        // 2025 fiel nur der 13. Juni auf einen Freitag
        assertEquals(1L, value(FunFacts.Kind.FRIDAY_13TH, start, today))
        assertEquals(292L * FunFacts.EARTH_KM_PER_DAY, value(FunFacts.Kind.EARTH_KM, start, today))
    }

    @Test
    fun fullMoonsMatchTheCalendar() {
        // Vollmonde 2025: 13.01., 12.02., 14.03., 13.04., 12.05., 11.06., 10.07., 09.08., 07.09., 07.10., 05.11., 04.12.
        assertEquals(12L, FunFacts.fullMoons(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)))
        assertEquals(0L, FunFacts.fullMoons(LocalDate.of(2025, 1, 14), LocalDate.of(2025, 2, 10)))
    }

    @Test
    fun nothingBeforeTheStart() {
        assertTrue(FunFacts.all(LocalDate.of(2025, 3, 15), LocalDate.of(2025, 3, 15)).isEmpty())
        assertTrue(FunFacts.all(LocalDate.of(2025, 3, 15), LocalDate.of(2025, 3, 1)).isEmpty())
    }

    @Test
    fun heartbeatsAtSeventyPerMinute() {
        assertEquals(100_800L, value(FunFacts.Kind.HEARTBEATS, LocalDate.of(2025, 3, 15), LocalDate.of(2025, 3, 16)))
        assertEquals(23_040L, value(FunFacts.Kind.BREATHS, LocalDate.of(2025, 3, 15), LocalDate.of(2025, 3, 16)))
    }

    @Test
    fun valentinesOnlyForLove() {
        val start = LocalDate.of(2025, 3, 15)
        val today = LocalDate.of(2026, 3, 1)
        assertTrue(FunFacts.all(start, today).any { it.kind == FunFacts.Kind.VALENTINES })
        assertTrue(FunFacts.all(start, today, love = false).none { it.kind == FunFacts.Kind.VALENTINES })
    }

    @Test
    fun hiddenHolidaysAreLeftOut() {
        val start = LocalDate.of(2020, 3, 15)
        val today = LocalDate.of(2026, 1, 1)
        val kinds = FunFacts.all(start, today, hidden = setOf(FunFacts.Kind.CHRISTMAS, FunFacts.Kind.HALLOWEENS)).map { it.kind }
        assertTrue(FunFacts.Kind.CHRISTMAS !in kinds)
        assertTrue(FunFacts.Kind.HALLOWEENS !in kinds)
        assertTrue(FunFacts.Kind.VALENTINES in kinds)
        assertTrue(FunFacts.Kind.FULL_MOONS in kinds)
        // Nur Feiertage lassen sich gespeichert ausblenden, Unbekanntes fällt weg
        assertEquals(setOf(FunFacts.Kind.CHRISTMAS), FunFacts.holidaysOf(listOf("CHRISTMAS", "FULL_MOONS", "gibt es nicht")))
    }

    @Test
    fun shareOfLifeNeedsABirthdayBeforeTheStart() {
        val start = LocalDate.of(2020, 1, 1)
        val today = LocalDate.of(2030, 1, 1)
        val people = listOf(
            Person("a", "Alex", birthday = LocalDate.of(2000, 1, 1)),
            Person("b", "Sam"),
            Person("c", "Kim", birthday = LocalDate.of(2021, 1, 1)),
        )
        val shares = FunFacts.all(start, today, people = people).filter { it.kind == FunFacts.Kind.LIFE_SHARE }
        // 10 von 30 Jahren zusammen, abgerundet
        assertEquals(listOf("Alex" to 33L), shares.map { it.name to it.value })
        assertTrue(FunFacts.all(start, today, hidden = setOf(FunFacts.Kind.LIFE_SHARE), people = people).none { it.kind == FunFacts.Kind.LIFE_SHARE })
    }
}
