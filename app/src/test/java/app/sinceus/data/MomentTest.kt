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
}
