package app.sinceus.notify

import app.sinceus.data.LoveSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class NotifierTest {
    private val settings = LoveSettings.couple("Alex", "Sam", LocalDate.of(2025, 3, 15)).copy(
        onboardingDone = true,
        notifyHour = 9,
        notifyMinute = 30,
    )

    private fun at(hour: Int, minute: Int) = LocalDateTime.of(2026, 3, 15, hour, minute)

    @Test
    fun `not before the chosen time`() {
        assertFalse(Notifier.isTime(settings, at(9, 29)))
        assertFalse(Notifier.isTime(settings, at(0, 1)))
    }

    @Test
    fun `at the chosen time and later the same day`() {
        assertTrue(Notifier.isTime(settings, at(9, 30)))
        // Handy war morgens aus: Die Mitteilung wird später am Tag nachgeholt
        assertTrue(Notifier.isTime(settings, at(18, 0)))
    }

    @Test
    fun `never when notifications are off or the app is not set up`() {
        assertFalse(Notifier.isTime(settings.copy(notificationsEnabled = false), at(18, 0)))
        assertFalse(Notifier.isTime(settings.copy(onboardingDone = false), at(18, 0)))
    }
}
