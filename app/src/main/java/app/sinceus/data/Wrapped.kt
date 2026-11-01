package app.sinceus.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.temporal.ChronoUnit

/**
 * Alles für den großen Jahresrückblick („Wrapped“), nur aus den Daten auf dem Handy berechnet.
 * Zahlen, die es in dem Jahr nicht gibt (keine Fotos, keine Orte), sind 0, leer oder null.
 */
data class Wrapped(
    val review: YearReview,
    /** Tage in diesem Beziehungsjahr, meist 365 oder 366 */
    val days: Long,
    val weekends: Long,
    val fullMoons: Long,
    val photos: Int,
    /** Monat mit den meisten Momenten, nur wenn es einen klaren gibt */
    val topMonth: Month?,
    val topMonthCount: Int,
    /** Häufigste Schlagwörter, höchstens drei, mit Anzahl */
    val topTags: List<Pair<String, Int>>,
    /** Verschiedene Orte in diesem Jahr */
    val places: Int,
    /** Ort, der am öftesten vorkommt (mindestens zweimal) */
    val topPlace: String?,
    /** Der nächste große Tag nach dem Ende des Jahres, z. B. 1.000 Tage oder der nächste Jahrestag */
    val next: Milestone?,
) {
    val moments: List<Moment> get() = review.moments
    val first: Moment? get() = moments.firstOrNull()
    val last: Moment? get() = moments.lastOrNull()?.takeIf { moments.size > 1 }
}

object WrappedMath {
    /** So viele Fotos zeigt die Collage höchstens */
    const val MAX_PHOTOS = 6

    /** Rückblick auf [review]; der Ausblick schaut ab [today] nach vorn, frühestens ab dem Jahrestag */
    fun of(start: LocalDate, review: YearReview, today: LocalDate): Wrapped {
        val moments = review.moments
        // Der Tag vor dem Jahr zählt nicht mit, so wie bei den lustigen Zahlen
        val before = review.from.minusDays(1)
        val months = moments.groupingBy { it.date.month }.eachCount()
        val best = months.maxByOrNull { it.value }
        val clearMonth = best?.takeIf { b -> b.value >= 2 && months.count { it.value == b.value } == 1 }
        val tags = moments.flatMap { it.tags.distinctBy { t -> t.lowercase() } }
            .groupBy { it.lowercase() }
            .map { (_, same) -> same.first() to same.size }
            .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first.lowercase() })
            .take(3)
        val places = moments.map { it.place.trim() }.filter { it.isNotEmpty() }
            .groupBy { it.lowercase() }
        val topPlace = places.values.maxByOrNull { it.size }?.takeIf { top -> top.size >= 2 && places.values.count { it.size == top.size } == 1 }
        return Wrapped(
            review = review,
            days = ChronoUnit.DAYS.between(review.from, review.to) + 1,
            weekends = FunFacts.count(before, review.to) { it.dayOfWeek == DayOfWeek.SATURDAY },
            fullMoons = FunFacts.fullMoons(before, review.to),
            photos = moments.sumOf { it.photos.size },
            topMonth = clearMonth?.key,
            topMonthCount = clearMonth?.value ?: 0,
            topTags = tags,
            places = places.size,
            topPlace = topPlace?.first(),
            next = nextBigDay(start, maxOf(today, review.to.plusDays(1))),
        )
    }

    /** Der erste Jahrestag oder runde Tag nach [after] (ohne [after]); Monatstage sind zu klein für einen Ausblick */
    fun nextBigDay(start: LocalDate, after: LocalDate): Milestone? =
        LoveMath.upcoming(start, after, 30).firstOrNull { it.kind == MilestoneKind.YEARS || it.kind == MilestoneKind.DAYS }

    /** Bis zu [MAX_PHOTOS] Fotos, aus möglichst vielen verschiedenen Momenten, über das Jahr verteilt */
    fun collage(moments: List<Moment>): List<String> {
        val covers = moments.mapNotNull { it.photoPath }
        val rest = moments.flatMap { it.photos.drop(1) }
        val all = (spread(covers, MAX_PHOTOS) + rest).distinct()
        return all.take(MAX_PHOTOS)
    }

    /** Nimmt [n] Einträge gleichmäßig verteilt aus [list], damit nicht nur der Anfang des Jahres drankommt */
    private fun <T> spread(list: List<T>, n: Int): List<T> {
        if (list.size <= n) return list
        return (0 until n).map { list[it * (list.size - 1) / (n - 1)] }
    }
}
