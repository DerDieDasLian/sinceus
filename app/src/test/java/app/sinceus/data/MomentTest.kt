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
    private val firstDate = Moment("a", "Erstes Date", LocalDate.of(2024, 5, 10), "Kino", listOf("/x/a.jpg", "/x/b.jpg"), true)
    private val leap = Moment("b", "Schaltjahr", LocalDate.of(2024, 2, 29), yearlyReminder = true)
    private val quiet = Moment("c", "Ohne Erinnerung", LocalDate.of(2024, 5, 10), yearlyReminder = false)

    @Test
    fun jsonRoundTrip() {
        val list = listOf(firstDate, leap, quiet)
        assertEquals(list, MomentCodec.decode(MomentCodec.encode(list)))
    }

    @Test
    fun tagsAndPlaceRoundTrip() {
        val trip = firstDate.copy(tags = listOf("Urlaub", "Date"), place = "Lissabon")
        assertEquals(listOf(trip), MomentCodec.decode(MomentCodec.encode(listOf(trip))))
        // Ältere Daten ohne Schlagwörter und Ort
        val old = MomentCodec.decode("""[{"id":"a","title":"Alt","date":19000}]""")[0]
        assertEquals(emptyList<String>(), old.tags)
        assertEquals("", old.place)
    }

    @Test
    fun tagsAreCleaned() {
        assertEquals(listOf("Urlaub", "Date"), MomentTags.clean(listOf(" Urlaub ", "", "urlaub", "Date")))
        val a = firstDate.copy(id = "1", tags = listOf("Date"))
        val b = firstDate.copy(id = "2", tags = listOf("Urlaub", "date"))
        assertEquals(listOf("Date", "Urlaub"), MomentTags.all(listOf(a, b)))
    }

    @Test
    fun filterByTagAndPlace() {
        val trip = firstDate.copy(tags = listOf("Urlaub"), place = "Lissabon")
        assertTrue(MomentFilter.matches(trip, "", tag = "urlaub"))
        assertTrue(!MomentFilter.matches(firstDate, "", tag = "Urlaub"))
        assertTrue(MomentFilter.matches(trip, "lissa"))
        assertTrue(MomentFilter.matches(trip, "urlau"))
    }

    @Test
    fun memoryPrefersAnniversaryThenPhotos() {
        val today = LocalDate.of(2026, 5, 10)
        val planned = Moment("f", "Zukunft", LocalDate.of(2026, 8, 1), photos = listOf("/x/f.jpg"))
        // firstDate hat heute Jahrestag
        assertEquals(firstDate, MemoryPick.pick(listOf(planned, leap, firstDate), today))
        val other = LocalDate.of(2026, 5, 11)
        // Ohne Jahrestag nur Momente mit Foto, solange es welche gibt
        assertEquals(firstDate, MemoryPick.pick(listOf(leap, quiet, firstDate), other))
        assertNull(MemoryPick.pick(listOf(planned), other))
        // Ohne Fotos kommt trotzdem ein Moment
        assertTrue(MemoryPick.pick(listOf(leap, quiet), other) in listOf(leap, quiet))
    }

    @Test
    fun memoryChangesDaily() {
        val moments = (1..5).map { Moment("m$it", "M$it", LocalDate.of(2020, 1, it), photos = listOf("/x/$it.jpg")) }
        val picks = (0L until 5L).map { MemoryPick.pick(moments, LocalDate.of(2026, 6, 1).plusDays(it)) }.toSet()
        assertTrue(picks.size > 1)
    }

    @Test
    fun memoryAgo() {
        val today = LocalDate.of(2026, 5, 10)
        assertEquals(MemoryPick.Span.YEARS to 2, MemoryPick.ago(LocalDate.of(2024, 5, 10), today))
        assertEquals(MemoryPick.Span.MONTHS to 11, MemoryPick.ago(LocalDate.of(2025, 6, 1), today))
        assertEquals(MemoryPick.Span.DAYS to 9, MemoryPick.ago(LocalDate.of(2026, 5, 1), today))
    }

    @Test
    fun firstPhotoIsCover() {
        assertEquals("/x/a.jpg", firstDate.photoPath)
        assertNull(leap.photoPath)
    }

    @Test
    fun oldJsonWithOnePhotoStillWorks() {
        val old = """[{"id":"a","title":"Alt","date":19000,"photo":"/x/alt.jpg"},{"id":"b","title":"Ohne","date":19000,"photo":null}]"""
        val moments = MomentCodec.decode(old)
        assertEquals(listOf("/x/alt.jpg"), moments[0].photos)
        assertEquals(emptyList<String>(), moments[1].photos)
    }

    @Test
    fun newJsonKeepsCoverForOlderVersions() {
        // Ältere Versionen lesen nur "photo" und sehen so wenigstens das Titelbild
        val json = org.json.JSONArray(MomentCodec.encode(listOf(firstDate))).getJSONObject(0)
        assertEquals("/x/a.jpg", json.getString("photo"))
        assertEquals(2, json.getJSONArray("photos").length())
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

    @Test
    fun plannedMoments() {
        val today = LocalDate.of(2026, 5, 1)
        val zone = java.time.ZoneOffset.UTC
        val millis = { d: LocalDate -> d.atStartOfDay(zone).toInstant().toEpochMilli() }
        val trip = Moment("t", "Urlaub", LocalDate.of(2026, 5, 20), updatedAt = millis(LocalDate.of(2026, 4, 1)))
        val party = Moment("p", "Party", LocalDate.of(2026, 5, 3), updatedAt = millis(LocalDate.of(2026, 4, 2)))
        val list = MomentMath.planned(listOf(firstDate, trip, party), today)
        assertEquals(listOf("Party", "Urlaub"), list.map { it.title })
        assertEquals(MilestoneKind.PLANNED, list[0].kind)
        // Am Tag selbst nur, wenn der Moment vorher eingetragen wurde
        assertEquals(listOf(trip), MomentMath.plannedOn(listOf(trip), trip.date, zone))
        val sameDay = trip.copy(updatedAt = millis(trip.date) + 3_600_000)
        assertTrue(MomentMath.plannedOn(listOf(sameDay), trip.date, zone).isEmpty())
    }

    @Test
    fun yearReview() {
        val start = LocalDate.of(2024, 3, 15)
        val inFirstYear = Moment("a", "Erstes Date", LocalDate.of(2024, 5, 10))
        val inSecondYear = Moment("b", "Umzug", LocalDate.of(2025, 4, 1))
        val moments = listOf(inSecondYear, inFirstYear)
        val review = YearReviewMath.reviewFor(start, LocalDate.of(2025, 3, 15), moments)!!
        assertEquals(1, review.year)
        assertEquals(listOf(inFirstYear), review.moments)
        assertEquals(LocalDate.of(2025, 3, 14), review.to)
        // Noch eine Woche lang zu sehen, danach nicht mehr
        assertEquals(1, YearReviewMath.reviewFor(start, LocalDate.of(2025, 3, 21), moments)?.year)
        assertNull(YearReviewMath.reviewFor(start, LocalDate.of(2025, 3, 22), moments))
        assertNull(YearReviewMath.reviewFor(start, LocalDate.of(2024, 3, 20), moments))
        // Start am 29.02.: Jahrestag am 28.02.
        assertEquals(1, YearReviewMath.reviewFor(LocalDate.of(2024, 2, 29), LocalDate.of(2025, 2, 28), moments)?.year)
    }

    @Test
    fun autoBackupKeepsNewestThree() {
        val names = listOf("since-us-auto-2026-09-01.zip", "since-us-auto-2026-09-22.zip", "since-us-auto-2026-09-08.zip", "since-us-auto-2026-09-15.zip")
        assertEquals(listOf("since-us-auto-2026-09-01.zip"), AutoBackup.oldOnes(names))
        assertTrue(AutoBackup.oldOnes(names.take(3)).isEmpty())
    }

    @Test
    fun changelogLines() {
        assertEquals(
            listOf("Geburtstage", "Suche ab 8 Momenten"),
            Changelog.parse(listOf("• Geburtstage", "", "  • Suche ab 8 Momenten  ")),
        )
    }
}
