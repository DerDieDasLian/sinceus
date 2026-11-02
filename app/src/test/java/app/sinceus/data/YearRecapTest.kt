package app.sinceus.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.Month

class YearRecapTest {
    private val start = LocalDate.of(2025, 3, 15)
    private val concert = Moment(
        "a", "Erstes Konzert", LocalDate.of(2025, 4, 1),
        photos = listOf("/x/a.jpg", "/x/b.jpg"), tags = listOf("Konzert"), place = "Berlin",
    )
    private val sea = Moment("b", "Urlaub am Meer", LocalDate.of(2025, 7, 10), photos = listOf("/x/c.jpg"), tags = listOf("Urlaub"), place = "Rostock")
    private val festival = Moment("c", "Festival", LocalDate.of(2025, 7, 20), tags = listOf("konzert", "Urlaub"), place = "berlin ")
    private val moving = Moment("d", "Umzug", LocalDate.of(2026, 1, 5))
    private val later = Moment("e", "Danach", LocalDate.of(2026, 3, 20), tags = listOf("Urlaub"))
    private val all = listOf(later, moving, festival, sea, concert)

    @Test
    fun countsTheYear() {
        val review = YearReviewMath.reviewOf(start, 1, all)
        val w = YearRecapMath.of(start, review, LocalDate.of(2026, 3, 16))
        assertEquals(listOf(concert, sea, festival, moving), w.moments)
        assertEquals(365L, w.days)
        // 15.03.2025 und 14.03.2026 sind beide Samstage
        assertEquals(53L, w.weekends)
        assertEquals(13L, w.fullMoons)
        assertEquals(3, w.photos)
        assertEquals(Month.JULY, w.topMonth)
        assertEquals(2, w.topMonthCount)
        assertEquals(listOf("Konzert" to 2, "Urlaub" to 2), w.topTags)
        assertEquals(2, w.places)
        assertEquals("Berlin", w.topPlace)
        assertEquals(concert, w.first)
        assertEquals(moving, w.last)
        // Nach dem Jahrestag kommt als Nächstes der 400. Tag
        assertEquals(Milestone(MilestoneKind.DAYS, 400, LocalDate.of(2026, 4, 19)), w.next)
    }

    @Test
    fun emptyYearHasNoHighlights() {
        val w = YearRecapMath.of(start, YearReviewMath.reviewOf(start, 1, emptyList()), LocalDate.of(2026, 3, 15))
        assertEquals(0, w.photos)
        assertNull(w.topMonth)
        assertNull(w.topPlace)
        assertNull(w.first)
        assertNull(w.last)
        assertEquals(emptyList<Pair<String, Int>>(), w.topTags)
    }

    @Test
    fun noClearWinner() {
        // Je ein Moment im April und Juli, zwei Orte je einmal: kein Lieblingsmonat, kein Lieblingsort
        val w = YearRecapMath.of(start, YearReviewMath.reviewOf(start, 1, listOf(concert, sea)), LocalDate.of(2026, 3, 15))
        assertNull(w.topMonth)
        assertNull(w.topPlace)
        assertEquals(2, w.places)
    }

    @Test
    fun earlierYearsLookAheadFromToday() {
        val today = LocalDate.of(2027, 6, 1)
        assertEquals(2, YearReviewMath.fullYears(start, today))
        assertEquals(0, YearReviewMath.fullYears(start, LocalDate.of(2026, 3, 14)))
        val w = YearRecapMath.of(start, YearReviewMath.reviewOf(start, 1, all), today)
        assertEquals(LocalDate.of(2025, 3, 15), w.review.from)
        // Der Ausblick liegt nach heute, nicht im vergangenen Jahr
        assertEquals(true, w.next!!.date.isAfter(today))
    }

    @Test
    fun collageSpreadsOverTheYear() {
        val moments = (1..8).map { Moment("$it", "M$it", start.plusDays(it * 10L), photos = listOf("/x/$it.jpg")) }
        val photos = YearRecapMath.collage(moments)
        assertEquals(YearRecapMath.MAX_PHOTOS, photos.size)
        assertEquals("/x/1.jpg", photos.first())
        assertEquals("/x/8.jpg", photos.last())
        // Wenige Titelbilder: die weiteren Fotos füllen auf
        assertEquals(listOf("/x/a.jpg", "/x/c.jpg", "/x/b.jpg"), YearRecapMath.collage(listOf(concert, sea)))
    }
}
