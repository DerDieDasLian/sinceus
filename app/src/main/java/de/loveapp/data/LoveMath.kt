package de.loveapp.data

import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

/** Wie lange ihr schon zusammen seid, in verschiedenen Einheiten. */
data class Together(
    val period: Period,
    val totalMonths: Long,
    val totalWeeks: Long,
    val totalDays: Long,
)

enum class MilestoneKind { YEARS, MONTHS, DAYS, WEEKS }

data class Milestone(val kind: MilestoneKind, val value: Long, val date: LocalDate) {
    val title: String
        get() = when (kind) {
            MilestoneKind.YEARS -> if (value == 1L) "1. Jahrestag" else "$value. Jahrestag"
            MilestoneKind.MONTHS -> plural(value, "Monat", "Monate")
            MilestoneKind.DAYS -> "${formatNumber(value)} Tage"
            MilestoneKind.WEEKS -> "${formatNumber(value)} Wochen"
        }

    val message: String
        get() = when (kind) {
            MilestoneKind.YEARS -> "Heute ist euer $title! Alles Liebe zu ${plural(value, "Jahr", "Jahren")} zusammen."
            MilestoneKind.MONTHS -> "Heute seid ihr genau ${plural(value, "Monat", "Monate")} zusammen."
            MilestoneKind.DAYS -> "Heute seid ihr ${formatNumber(value)} Tage zusammen."
            MilestoneKind.WEEKS -> "Heute seid ihr ${formatNumber(value)} Wochen zusammen."
        }
}

object LoveMath {

    fun together(start: LocalDate, today: LocalDate): Together {
        val from = minOf(start, today)
        val to = maxOf(start, today)
        return Together(
            period = Period.between(from, to),
            totalMonths = ChronoUnit.MONTHS.between(from, to),
            totalWeeks = ChronoUnit.WEEKS.between(from, to),
            totalDays = ChronoUnit.DAYS.between(from, to),
        )
    }

    /** Alle besonderen Anlaesse, die genau auf [day] fallen. */
    fun milestonesOn(start: LocalDate, day: LocalDate): List<Milestone> {
        if (!day.isAfter(start)) return emptyList()
        val result = mutableListOf<Milestone>()
        // plusMonths klemmt auf das Monatsende (31.01. + 1 Monat = 28.02.), MONTHS.between
        // zaehlt dort aber noch einen Monat weniger, daher auch den Folgemonat pruefen
        val months = ChronoUnit.MONTHS.between(start, day).let { m ->
            if (start.plusMonths(m + 1) == day) m + 1 else m
        }
        if (months > 0 && start.plusMonths(months) == day) {
            if (months % 12 == 0L) {
                result += Milestone(MilestoneKind.YEARS, months / 12, day)
            } else {
                result += Milestone(MilestoneKind.MONTHS, months, day)
            }
        }
        val days = ChronoUnit.DAYS.between(start, day)
        if (isSpecialDayCount(days)) result += Milestone(MilestoneKind.DAYS, days, day)
        if (days % 7 == 0L && isSpecialWeekCount(days / 7)) {
            result += Milestone(MilestoneKind.WEEKS, days / 7, day)
        }
        return result
    }

    /** Die naechsten [count] besonderen Tage nach [today] (exklusiv). */
    fun upcoming(start: LocalDate, today: LocalDate, count: Int = 4): List<Milestone> {
        val result = mutableListOf<Milestone>()
        var day = maxOf(today, start).plusDays(1)
        // Spaetestens nach gut einem Monat gibt es den naechsten Monatstag
        val limit = day.plusYears(2)
        while (result.size < count && day.isBefore(limit)) {
            result += milestonesOn(start, day)
            day = day.plusDays(1)
        }
        return result.take(count)
    }

    fun isSpecialDayCount(days: Long): Boolean {
        if (days <= 0) return false
        if (days == 50L) return true
        if (days % 100 == 0L) return true
        return days >= 111 && isRepdigit(days)
    }

    fun isSpecialWeekCount(weeks: Long): Boolean =
        weeks > 0 && weeks % 50 == 0L

    private fun isRepdigit(n: Long): Boolean {
        val s = n.toString()
        return s.length > 1 && s.all { it == s[0] }
    }
}

fun plural(n: Long, one: String, many: String) = "${formatNumber(n)} ${if (n == 1L) one else many}"

fun formatNumber(n: Long): String = "%,d".format(java.util.Locale.GERMANY, n)

/** "1 Jahr, 5 Monate und 3 Tage", mit [dative] passend fuer "seit 5 Monaten". */
fun Period.toGermanText(dative: Boolean = false): String {
    val n = if (dative) "n" else ""
    val parts = buildList {
        if (years > 0) add(plural(years.toLong(), "Jahr", "Jahre$n"))
        if (months > 0) add(plural(months.toLong(), "Monat", "Monate$n"))
        if (days > 0 || isEmpty()) add(plural(days.toLong(), "Tag", "Tage$n"))
    }
    return when (parts.size) {
        1 -> parts[0]
        else -> parts.dropLast(1).joinToString(", ") + " und " + parts.last()
    }
}
