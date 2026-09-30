package de.loveapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LoveMathTest {

    private val start = LocalDate.of(2025, 3, 15)

    @Test
    fun matchesOriginalApp() {
        // 15.03. bis 15.08.: genau 5 Monate, 21 Wochen, 153 Tage
        val t = LoveMath.together(start, LocalDate.of(2026, 9, 30))
        assertEquals(5, t.totalMonths)
        assertEquals(21, t.totalWeeks)
        assertEquals(153, t.totalDays)
        assertEquals("5 Monate", t.period.toGermanText())
    }

    @Test
    fun monthlyAnniversary() {
        val m = LoveMath.milestonesOn(start, LocalDate.of(2026, 9, 30))
        assertEquals(listOf(Milestone(MilestoneKind.MONTHS, 5, LocalDate.of(2026, 9, 30))), m)
    }

    @Test
    fun monthEndIsClamped() {
        val s = LocalDate.of(2026, 1, 31)
        assertEquals(MilestoneKind.MONTHS, LoveMath.milestonesOn(s, LocalDate.of(2026, 2, 28)).single().kind)
        assertTrue(LoveMath.milestonesOn(s, LocalDate.of(2026, 3, 1)).isEmpty())
    }

    @Test
    fun yearlyAnniversary() {
        val m = LoveMath.milestonesOn(start, LocalDate.of(2027, 4, 30))
        assertEquals(MilestoneKind.YEARS, m.first().kind)
        assertEquals(1, m.first().value)
        assertEquals("1. Jahrestag", m.first().title)
    }

    @Test
    fun hundredDays() {
        val m = LoveMath.milestonesOn(start, start.plusDays(200))
        assertTrue(m.any { it.kind == MilestoneKind.DAYS && it.value == 200L })
    }

    @Test
    fun upcomingIsSortedAndInFuture() {
        val today = LocalDate.of(2026, 9, 30)
        val next = LoveMath.upcoming(start, today, 3)
        assertEquals(3, next.size)
        assertTrue(next.all { it.date.isAfter(today) })
        assertEquals(next.sortedBy { it.date }, next)
        assertEquals(LocalDate.of(2026, 10, 30), next.first().date)
    }

    @Test
    fun germanText() {
        val t = LoveMath.together(LocalDate.of(2024, 1, 1), LocalDate.of(2025, 3, 2))
        assertEquals("1 Jahr, 2 Monate und 1 Tag", t.period.toGermanText())
    }
}
