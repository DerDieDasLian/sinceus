package app.sinceus.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.MonthDay
import java.time.temporal.ChronoUnit
import kotlin.math.floor

/** Lustige Zahlen zu eurer gemeinsamen Zeit, nur aus dem Startdatum berechnet */
object FunFacts {
    enum class Kind { WEEKENDS, FULL_MOONS, CHRISTMAS, NEW_YEARS, SEASONS, HEARTBEATS }

    data class Fact(val kind: Kind, val value: Long)

    /** Dauer eines Mondzyklus in Tagen */
    private const val LUNAR_MONTH = 29.530588853

    /** Ein bekannter Vollmond als Bezugspunkt: 21. Januar 2000 */
    private val REFERENCE_FULL_MOON: LocalDate = LocalDate.of(2000, 1, 21)

    /** Ruhepuls, mit dem die Herzschläge geschätzt werden */
    const val BEATS_PER_MINUTE = 70L

    private val SEASON_STARTS = listOf(MonthDay.of(3, 20), MonthDay.of(6, 21), MonthDay.of(9, 22), MonthDay.of(12, 21))

    /** Alle Zahlen zwischen [start] und [today]; leer, wenn ihr noch nicht zusammen seid */
    fun all(start: LocalDate, today: LocalDate): List<Fact> {
        if (!today.isAfter(start)) return emptyList()
        val days = ChronoUnit.DAYS.between(start, today)
        return listOf(
            Fact(Kind.WEEKENDS, count(start, today) { it.dayOfWeek == DayOfWeek.SATURDAY }),
            Fact(Kind.FULL_MOONS, fullMoons(start, today)),
            Fact(Kind.CHRISTMAS, count(start, today) { it.monthValue == 12 && it.dayOfMonth == 24 }),
            Fact(Kind.NEW_YEARS, count(start, today) { it.dayOfYear == 1 }),
            Fact(Kind.SEASONS, count(start, today) { MonthDay.from(it) in SEASON_STARTS }),
            Fact(Kind.HEARTBEATS, days * 24 * 60 * BEATS_PER_MINUTE),
        ).filter { it.value > 0 }
    }

    /** Tage nach [start] bis einschließlich [today], die [match] erfüllen */
    private fun count(start: LocalDate, today: LocalDate, match: (LocalDate) -> Boolean): Long {
        var n = 0L
        var d = start.plusDays(1)
        while (!d.isAfter(today)) {
            if (match(d)) n++
            d = d.plusDays(1)
        }
        return n
    }

    /** Vollmonde nach [start] bis einschließlich [today], auf den Tag genau gerechnet */
    internal fun fullMoons(start: LocalDate, today: LocalDate): Long {
        fun cycles(d: LocalDate) = floor(ChronoUnit.DAYS.between(REFERENCE_FULL_MOON, d) / LUNAR_MONTH).toLong()
        return cycles(today) - cycles(start)
    }
}
