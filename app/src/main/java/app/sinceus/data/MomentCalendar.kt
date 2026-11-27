package app.sinceus.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** Rechnungen für die Kalenderansicht der Momente */
object MomentCalendar {
    /** Ein Jahrestag eines Moments im gezeigten Monat */
    data class Anniversary(val moment: Moment, val date: LocalDate, val years: Int)

    /**
     * Felder eines Monats für ein Raster mit 7 Spalten, die Woche beginnt an [firstDay].
     * null = leeres Feld vor dem 1. oder nach dem letzten Tag.
     */
    fun cells(month: YearMonth, firstDay: DayOfWeek): List<LocalDate?> {
        val first = month.atDay(1)
        val leading = (first.dayOfWeek.value - firstDay.value + 7) % 7
        val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
        val filled = List(leading) { null } + days
        return filled + List((7 - filled.size % 7) % 7) { null }
    }

    /** Hat [moment] mindestens einen Tag in [month]? */
    fun overlaps(moment: Moment, month: YearMonth): Boolean =
        !moment.date.isAfter(month.atEndOfMonth()) && !moment.lastDay.isBefore(month.atDay(1))

    /** Die Tage von [moment] in [month], bei einem Zeitraum alle, sonst nur der eine (Issue #77) */
    fun daysIn(moment: Moment, month: YearMonth): List<LocalDate> {
        if (!overlaps(moment, month)) return emptyList()
        val from = maxOf(moment.date, month.atDay(1))
        val to = minOf(moment.lastDay, month.atEndOfMonth())
        return generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.toList()
    }

    /** Jahrestage der Momente mit Erinnerung in [month]; am 29. Februar fällt er sonst auf den 28. */
    fun anniversaries(moments: List<Moment>, month: YearMonth): List<Anniversary> =
        moments.filter { it.yearlyReminder && it.date.monthValue == month.monthValue && it.date.year < month.year }
            .map { Anniversary(it, it.date.withYear(month.year), month.year - it.date.year) }
            .sortedBy { it.date }
}
