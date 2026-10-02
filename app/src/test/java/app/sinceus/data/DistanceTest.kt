package app.sinceus.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

// JSON braucht die echte Android-Umgebung, daher Robolectric
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DistanceTest {
    private val today = LocalDate.of(2026, 10, 3)

    @Test
    fun daysUntilTheMeeting() {
        assertEquals(12L, Distance.daysUntil(today.plusDays(12), today))
        assertEquals(0L, Distance.daysUntil(today, today))
        assertNull(Distance.daysUntil(today.minusDays(1), today))
        assertNull(Distance.daysUntil(null, today))
    }

    @Test
    fun offsetFollowsSummerTime() {
        val berlin = ZoneId.of("Europe/Berlin")
        val newYork = ZoneId.of("America/New_York")
        // Im Sommer sechs Stunden, im Herbst kurz fünf, weil die USA später auf Winterzeit gehen
        assertEquals(-360, Distance.offsetMinutes(newYork, berlin, Instant.parse("2026-07-01T12:00:00Z")))
        assertEquals(-300, Distance.offsetMinutes(newYork, berlin, Instant.parse("2026-10-27T12:00:00Z")))
        assertEquals(210, Distance.offsetMinutes(ZoneId.of("Asia/Kolkata"), berlin, Instant.parse("2026-07-01T12:00:00Z")))
    }

    @Test
    fun citiesCanBeFound() {
        val cities = Distance.cities(Locale.GERMANY)
        assertTrue(cities.any { it.zone == "America/New_York" && it.name == "New York" })
        assertTrue(cities.filter { Distance.matches(it, "tokyo") }.any { it.zone == "Asia/Tokyo" })
        assertTrue(cities.none { it.zone.startsWith("Etc/") })
    }

    @Test
    fun relationshipKeepsDistanceSettings() {
        val r = Relationship("r", listOf("a", "b"), today, distance = true, farZone = "Asia/Tokyo", nextMeeting = today.plusDays(5))
        val back = PeopleCodec.decodeRelationships(PeopleCodec.encodeRelationships(listOf(r)))!!.single()
        assertEquals(r, back)
        val old = PeopleCodec.decodeRelationships("""[{"id":"r","members":["a","b"],"start":0}]""")!!.single()
        assertEquals(false, old.distance)
        assertNull(old.farZone)
        assertNull(old.nextMeeting)
    }
}
