package app.sinceus.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

// Robolectric wegen org.json (in reinen JVM-Tests nur als Attrappe vorhanden)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MomentTest {
    private val firstDate = Moment("a", "Erstes Date", LocalDate.of(2024, 5, 10), "Kino", "/x/a.jpg", true)
    private val leap = Moment("b", "Schaltjahr", LocalDate.of(2024, 2, 29), yearlyReminder = true)
    private val quiet = Moment("c", "Ohne Erinnerung", LocalDate.of(2024, 5, 10), yearlyReminder = false)

    @Test
    fun jsonRoundTrip() {
        val list = listOf(firstDate, leap, quiet)
        assertEquals(list, MomentCodec.decode(MomentCodec.encode(list)))
    }

    @Test
    fun brokenJsonKeepsReadableEntries() {
        assertEquals(emptyList<Moment>(), MomentCodec.decode("kein json"))
        assertEquals(emptyList<Moment>(), MomentCodec.decode(null))
        val mixed = """[{"id":"a","title":"Ok","date":19000},{"title":"ohne id"}]"""
        assertEquals(listOf("Ok"), MomentCodec.decode(mixed).map { it.title })
    }

    @Test
    fun anniversaries() {
        assertEquals(2, MomentMath.anniversaryOn(firstDate, LocalDate.of(2026, 5, 10)))
        assertNull(MomentMath.anniversaryOn(firstDate, LocalDate.of(2024, 5, 10)))
        assertNull(MomentMath.anniversaryOn(firstDate, LocalDate.of(2026, 5, 11)))
        // 29.02. wird in Nicht-Schaltjahren am 28.02. gefeiert
        assertEquals(1, MomentMath.anniversaryOn(leap, LocalDate.of(2025, 2, 28)))
        assertEquals(4, MomentMath.anniversaryOn(leap, LocalDate.of(2028, 2, 29)))
    }

    @Test
    fun remindersOnlyForEnabledMoments() {
        val hits = MomentMath.remindersOn(listOf(firstDate, quiet), LocalDate.of(2026, 5, 10))
        assertEquals(listOf(firstDate to 2), hits)
    }

    @Test
    fun nextAnniversary() {
        assertEquals(LocalDate.of(2026, 5, 10), MomentMath.nextAnniversary(firstDate, LocalDate.of(2025, 9, 30)))
        assertEquals(LocalDate.of(2026, 5, 10), MomentMath.nextAnniversary(firstDate, LocalDate.of(2026, 5, 10)))
        assertEquals(LocalDate.of(2025, 5, 10), MomentMath.nextAnniversary(firstDate, LocalDate.of(2024, 6, 1)))
        assertTrue(MomentMath.nextAnniversary(leap, LocalDate.of(2025, 3, 1)) == LocalDate.of(2026, 2, 28))
    }

    @Test
    fun upcomingAnniversariesForOverview() {
        val list = listOf(firstDate, leap, quiet)
        val next = MomentMath.upcoming(list, LocalDate.of(2026, 1, 1))
        // Ohne Erinnerung taucht nicht auf, Schaltjahr-Moment feiert am 28.02.
        assertEquals(
            listOf(
                Milestone(MilestoneKind.MOMENT, 2, LocalDate.of(2026, 2, 28), "Schaltjahr"),
                Milestone(MilestoneKind.MOMENT, 2, LocalDate.of(2026, 5, 10), "Erstes Date"),
            ),
            next,
        )
        // Heute zählt nicht als "nächster" Jahrestag, sondern steht im Heute-Hinweis
        assertEquals(LocalDate.of(2027, 5, 10), MomentMath.upcoming(list, LocalDate.of(2026, 5, 10)).last().date)
        assertEquals(
            listOf(Milestone(MilestoneKind.MOMENT, 2, LocalDate.of(2026, 5, 10), "Erstes Date")),
            MomentMath.milestonesOn(list, LocalDate.of(2026, 5, 10)),
        )
    }

    @Test
    fun filterBySearchYearAndRelationship() {
        val forR1 = firstDate.copy(id = "d", relationshipId = "r1")
        val forR2 = quiet.copy(id = "e", date = LocalDate.of(2025, 1, 2), relationshipId = "r2")
        assertTrue(MomentFilter.matches(firstDate, "kino"))
        assertTrue(MomentFilter.matches(firstDate, " erstes "))
        assertTrue(!MomentFilter.matches(firstDate, "Urlaub"))
        assertTrue(MomentFilter.matches(leap, "", year = 2024))
        assertTrue(!MomentFilter.matches(forR2, "", year = 2024))
        assertTrue(MomentFilter.matches(forR1, "", relationshipId = "r1"))
        assertTrue(!MomentFilter.matches(forR2, "", relationshipId = "r1"))
        // Momente für alle Beziehungen passen überall
        assertTrue(MomentFilter.matches(firstDate, "", relationshipId = "r2"))
        assertEquals(listOf(2025, 2024), MomentFilter.years(listOf(firstDate, leap, forR2)))
    }
}
