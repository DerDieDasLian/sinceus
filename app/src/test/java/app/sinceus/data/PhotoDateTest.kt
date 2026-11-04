package app.sinceus.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PhotoDateTest {
    @Test
    fun readsExifDates() {
        assertEquals(LocalDate.of(2025, 3, 15), PhotoDate.parse("2025:03:15 20:30:00"))
        assertEquals(LocalDate.of(2025, 3, 15), PhotoDate.parse("2025-03-15T20:30:00"))
    }

    @Test
    fun brokenValuesGiveNull() {
        assertNull(PhotoDate.parse(null))
        assertNull(PhotoDate.parse(""))
        assertNull(PhotoDate.parse("0000:00:00 00:00:00"))
        assertNull(PhotoDate.parse("2025:13:40 00:00:00"))
    }

    @Test
    fun onlyPlausibleDates() {
        val today = LocalDate.of(2026, 11, 4)
        assertTrue(PhotoDate.plausible(LocalDate.of(2025, 3, 15), today))
        assertFalse(PhotoDate.plausible(LocalDate.of(1970, 1, 1), today))
        assertFalse(PhotoDate.plausible(today.plusDays(1), today))
    }
}
