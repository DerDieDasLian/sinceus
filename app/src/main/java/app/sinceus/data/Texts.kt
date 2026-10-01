package app.sinceus.data

import android.content.Context
import android.text.format.DateFormat
import androidx.annotation.PluralsRes
import app.sinceus.R
import java.time.LocalDate
import java.time.LocalTime
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date

/** Alle sprachabhängigen Texte, die aus Zahlen und Daten zusammengesetzt werden. */
object Texts {

    fun count(context: Context, @PluralsRes id: Int, n: Long): String =
        context.resources.getQuantityString(id, n.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), formatNumber(n))

    /** "1 Jahr, 5 Monate und 3 Tage", mit [dative] passend für "seit 5 Monaten". */
    fun period(context: Context, p: Period, dative: Boolean = false): String {
        val parts = buildList {
            if (p.years > 0) add(count(context, if (dative) R.plurals.years_dative else R.plurals.years, p.years.toLong()))
            if (p.months > 0) add(count(context, if (dative) R.plurals.months_dative else R.plurals.months, p.months.toLong()))
            if (p.days > 0 || isEmpty()) {
                add(count(context, if (dative) R.plurals.days_dative else R.plurals.days, p.days.toLong()))
            }
        }
        return if (parts.size == 1) {
            parts[0]
        } else {
            parts.dropLast(1).joinToString(", ") + context.getString(R.string.and_word) + parts.last()
        }
    }

    fun milestoneTitle(context: Context, m: Milestone): String = when (m.kind) {
        MilestoneKind.YEARS -> context.getString(R.string.anniversary_title, formatNumber(m.value))
        MilestoneKind.MONTHS -> count(context, R.plurals.months, m.value)
        MilestoneKind.DAYS -> count(context, R.plurals.days, m.value)
        MilestoneKind.WEEKS -> count(context, R.plurals.weeks, m.value)
        MilestoneKind.MOMENT -> context.getString(
            R.string.moment_anniversary_title,
            m.title.orEmpty(),
            count(context, R.plurals.years, m.value),
        )
    }

    fun milestoneMessage(context: Context, m: Milestone): String = when (m.kind) {
        MilestoneKind.YEARS -> context.getString(
            R.string.msg_anniversary,
            milestoneTitle(context, m),
            count(context, R.plurals.years_dative, m.value),
        )
        MilestoneKind.MONTHS -> context.getString(R.string.msg_exact, milestoneTitle(context, m))
        MilestoneKind.MOMENT -> context.getString(
            R.string.notify_moment,
            count(context, R.plurals.years_dative, m.value),
            m.title.orEmpty(),
        )
        else -> context.getString(R.string.msg_together, milestoneTitle(context, m))
    }

    /** „Heute vor 2 Jahren: Erstes Date“ */
    fun momentMessage(context: Context, m: Moment, years: Int): String =
        context.getString(R.string.notify_moment, count(context, R.plurals.years_dative, years.toLong()), m.title)

    /** Uhrzeit im Format des Geräts (12/24 Stunden), z. B. "09:00 Uhr" oder "9:00 AM". */
    fun time(context: Context, t: LocalTime): String {
        val date = Date.from(LocalDate.now().atTime(t).atZone(ZoneId.systemDefault()).toInstant())
        return context.getString(R.string.time_value, DateFormat.getTimeFormat(context).format(date))
    }

    fun longDate(d: LocalDate): String = d.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))

    fun mediumDate(d: LocalDate): String = d.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

    /** Kurzes Datum mit Wochentag, z. B. "Fr., 30. Okt. 2026" oder "Fri, Oct 30, 2026". */
    fun dateWithWeekday(d: LocalDate): String {
        val locale = java.util.Locale.getDefault()
        return d.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "EEEdMMMyyyy"), locale))
    }
}
