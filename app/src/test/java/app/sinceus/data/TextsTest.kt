package app.sinceus.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.Period
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TextsTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val period = Period.between(LocalDate.of(2024, 1, 1), LocalDate.of(2025, 3, 2))
    private val day = LocalDate.of(2026, 3, 15)

    @Test
    @Config(qualifiers = "de")
    fun german() {
        Locale.setDefault(Locale.GERMANY)
        assertEquals("1 Jahr, 2 Monate und 1 Tag", Texts.period(context, period))
        assertEquals("5 Monaten", Texts.period(context, Period.ofMonths(5), dative = true))
        assertEquals("1. Jahrestag", Texts.milestoneTitle(context, Milestone(MilestoneKind.YEARS, 1, day)))
        assertEquals(
            "Heute seid ihr genau 5 Monate zusammen.",
            Texts.milestoneMessage(context, Milestone(MilestoneKind.MONTHS, 5, day)),
        )
        assertEquals("1.000 Tage", Texts.milestoneTitle(context, Milestone(MilestoneKind.DAYS, 1000, day)))
    }

    @Test
    @Config(qualifiers = "en")
    fun english() {
        Locale.setDefault(Locale.US)
        assertEquals("1 year, 2 months and 1 day", Texts.period(context, period))
        assertEquals("1-year anniversary", Texts.milestoneTitle(context, Milestone(MilestoneKind.YEARS, 1, day)))
        assertEquals(
            "Today you have been together for exactly 5 months.",
            Texts.milestoneMessage(context, Milestone(MilestoneKind.MONTHS, 5, day)),
        )
        assertEquals("1,000 days", Texts.milestoneTitle(context, Milestone(MilestoneKind.DAYS, 1000, day)))
    }
}
