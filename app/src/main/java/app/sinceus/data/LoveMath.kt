package app.sinceus.data

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

enum class MilestoneKind { YEARS, MONTHS, DAYS, WEEKS, MOMENT }

/** Bei [MilestoneKind.MOMENT] ist [value] die Anzahl Jahre und [title] der Name des Moments. */
data class Milestone(val kind: MilestoneKind, val value: Long, val date: LocalDate, val title: String? = null)

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

    /** Alle besonderen Anlässe, die genau auf [day] fallen. */
    fun milestonesOn(start: LocalDate, day: LocalDate): List<Milestone> {
        if (!day.isAfter(start)) return emptyList()
        val result = mutableListOf<Milestone>()
        // plusMonths klemmt auf das Monatsende (31.01. + 1 Monat = 28.02.), MONTHS.between
        // zählt dort aber noch einen Monat weniger, daher auch den Folgemonat prüfen
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

    /** Die nächsten [count] besonderen Tage nach [today] (exklusiv). */
    fun upcoming(start: LocalDate, today: LocalDate, count: Int = 4): List<Milestone> {
        val result = mutableListOf<Milestone>()
        var day = maxOf(today, start).plusDays(1)
        // Spätestens nach gut einem Monat gibt es den nächsten Monatstag
        val limit = day.plusYears(2)
        while (result.size < count && day.isBefore(limit)) {
            result += milestonesOn(start, day)
            day = day.plusDays(1)
        }
        return result.take(count)
    }

    /** Datum des letzten besonderen Tags bis einschließlich [today], sonst der Starttag. */
    fun previousMilestoneDate(start: LocalDate, today: LocalDate): LocalDate {
        var day = today
        while (day.isAfter(start)) {
            if (milestonesOn(start, day).isNotEmpty()) return day
            day = day.minusDays(1)
        }
        return start
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

fun formatNumber(n: Long): String = "%,d".format(java.util.Locale.getDefault(), n)

/** Sekundengenauer Abstand zwischen zwei Zeitpunkten, für den Live-Zähler. */
data class LiveSpan(
    val years: Int,
    val months: Int,
    val days: Int,
    val hours: Int,
    val minutes: Int,
    val seconds: Int,
    val totalMonths: Long,
    val totalDays: Long,
    val totalHours: Long,
    val totalMinutes: Long,
    val totalSeconds: Long,
)

fun liveSpan(
    a: java.time.LocalDateTime,
    b: java.time.LocalDateTime,
    zone: java.time.ZoneId = java.time.ZoneId.systemDefault(),
): LiveSpan {
    val from = minOf(a, b)
    val to = maxOf(a, b)
    // Uhrzeit-Anteil: wenn die Uhrzeit "zurückliegt", zählt der letzte Tag noch nicht voll
    var endDate = to.toLocalDate()
    var secsOfDay = to.toLocalTime().toSecondOfDay() - from.toLocalTime().toSecondOfDay()
    if (secsOfDay < 0) {
        endDate = endDate.minusDays(1)
        secsOfDay += 24 * 3600
    }
    val p = Period.between(from.toLocalDate(), endDate)
    val total = java.time.Duration.between(from.atZone(zone), to.atZone(zone)).seconds
    return LiveSpan(
        years = p.years,
        months = p.months,
        days = p.days,
        hours = secsOfDay / 3600,
        minutes = secsOfDay / 60 % 60,
        seconds = secsOfDay % 60,
        totalMonths = ChronoUnit.MONTHS.between(from, to),
        totalDays = ChronoUnit.DAYS.between(from, to),
        totalHours = total / 3600,
        totalMinutes = total / 60,
        totalSeconds = total,
    )
}
