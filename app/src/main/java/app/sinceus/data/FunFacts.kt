package app.sinceus.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.MonthDay
import java.time.temporal.ChronoUnit
import kotlin.math.floor

/** Lustige Zahlen zu eurer gemeinsamen Zeit, nur aus dem Startdatum berechnet */
object FunFacts {
    enum class Kind {
        WEEKENDS, FULL_MOONS, VALENTINES, PRIDE_MONTHS, HALLOWEENS, CHRISTMAS, NEW_YEARS,
        FRIDAY_13TH, SEASONS, EARTH_KM, HEARTBEATS, BREATHS,
    }

    data class Fact(val kind: Kind, val value: Long)

    /** Dauer eines Mondzyklus in Tagen */
    private const val LUNAR_MONTH = 29.530588853

    /** Ein bekannter Vollmond als Bezugspunkt: 21. Januar 2000 */
    private val REFERENCE_FULL_MOON: LocalDate = LocalDate.of(2000, 1, 21)

    /** Ruhepuls, mit dem die Herzschläge geschätzt werden */
    const val BEATS_PER_MINUTE = 70L

    /** Ruhige Atemzüge pro Minute */
    const val BREATHS_PER_MINUTE = 16L

    /** So viele Kilometer legt die Erde pro Tag auf ihrer Bahn um die Sonne zurück */
    const val EARTH_KM_PER_DAY = 2_573_000L

    private val SEASON_STARTS = listOf(MonthDay.of(3, 20), MonthDay.of(6, 21), MonthDay.of(9, 22), MonthDay.of(12, 21))

    /** Alle Zahlen zwischen [start] und [today]; leer, wenn ihr noch nicht zusammen seid */
    fun all(start: LocalDate, today: LocalDate): List<Fact> {
        if (!today.isAfter(start)) return emptyList()
        val days = ChronoUnit.DAYS.between(start, today)
        return listOf(
            Fact(Kind.WEEKENDS, count(start, today) { it.dayOfWeek == DayOfWeek.SATURDAY }),
            Fact(Kind.FULL_MOONS, fullMoons(start, today)),
            Fact(Kind.VALENTINES, count(start, today) { it.monthValue == 2 && it.dayOfMonth == 14 }),
            Fact(Kind.PRIDE_MONTHS, count(start, today) { it.monthValue == 6 && it.dayOfMonth == 1 }),
            Fact(Kind.HALLOWEENS, count(start, today) { it.monthValue == 10 && it.dayOfMonth == 31 }),
            Fact(Kind.CHRISTMAS, count(start, today) { it.monthValue == 12 && it.dayOfMonth == 24 }),
            Fact(Kind.NEW_YEARS, count(start, today) { it.dayOfYear == 1 }),
            Fact(Kind.FRIDAY_13TH, count(start, today) { it.dayOfMonth == 13 && it.dayOfWeek == DayOfWeek.FRIDAY }),
            Fact(Kind.SEASONS, count(start, today) { MonthDay.from(it) in SEASON_STARTS }),
            Fact(Kind.EARTH_KM, days * EARTH_KM_PER_DAY),
            Fact(Kind.HEARTBEATS, days * 24 * 60 * BEATS_PER_MINUTE),
            Fact(Kind.BREATHS, days * 24 * 60 * BREATHS_PER_MINUTE),
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
