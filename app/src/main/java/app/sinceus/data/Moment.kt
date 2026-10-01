package app.sinceus.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Ein wichtiger Moment der Beziehung, z. B. „Erstes Date“. */
data class Moment(
    val id: String,
    val title: String,
    val date: LocalDate,
    val note: String = "",
    /** Pfad zum Foto im App-Speicher (files/moments/), null = kein Foto */
    val photoPath: String? = null,
    /** Am Jahrestag eine Mitteilung schicken */
    val yearlyReminder: Boolean = true,
)

object MomentCodec {
    fun encode(moments: List<Moment>): String = JSONArray().apply {
        moments.forEach { m ->
            put(
                JSONObject()
                    .put("id", m.id)
                    .put("title", m.title)
                    .put("date", m.date.toEpochDay())
                    .put("note", m.note)
                    .put("photo", m.photoPath ?: JSONObject.NULL)
                    .put("remind", m.yearlyReminder),
            )
        }
    }.toString()

    /** Unlesbare Einträge werden übersprungen, statt alle Momente zu verlieren. */
    fun decode(json: String?): List<Moment> {
        if (json.isNullOrBlank()) return emptyList()
        val array = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
        return (0 until array.length()).mapNotNull { i ->
            runCatching {
                val o = array.getJSONObject(i)
                Moment(
                    id = o.getString("id"),
                    title = o.getString("title"),
                    date = LocalDate.ofEpochDay(o.getLong("date")),
                    note = o.optString("note", ""),
                    photoPath = if (o.isNull("photo")) null else o.optString("photo").ifBlank { null },
                    yearlyReminder = o.optBoolean("remind", true),
                )
            }.getOrNull()
        }
    }
}

object MomentMath {
    /**
     * Anzahl Jahre, wenn [day] ein Jahrestag von [m] ist, sonst null.
     * Ein Moment am 29.02. feiert in Nicht-Schaltjahren am 28.02.
     */
    fun anniversaryOn(m: Moment, day: LocalDate): Int? {
        val years = day.year - m.date.year
        return years.takeIf { it > 0 && m.date.plusYears(it.toLong()) == day }
    }

    /** Momente mit Erinnerung, die heute Jahrestag haben, mit Anzahl der Jahre. */
    fun remindersOn(moments: List<Moment>, day: LocalDate): List<Pair<Moment, Int>> =
        moments.filter { it.yearlyReminder }.mapNotNull { m -> anniversaryOn(m, day)?.let { m to it } }

    /** Nächster Jahrestag ab [today] (einschließlich heute). */
    fun nextAnniversary(m: Moment, today: LocalDate): LocalDate {
        var years = maxOf(1, today.year - m.date.year).toLong()
        var next = m.date.plusYears(years)
        while (next.isBefore(today)) next = m.date.plusYears(++years)
        return next
    }

    /** Jahrestage der Momente mit Erinnerung, die genau auf [day] fallen. */
    fun milestonesOn(moments: List<Moment>, day: LocalDate): List<Milestone> =
        remindersOn(moments, day).map { (m, years) -> Milestone(MilestoneKind.MOMENT, years.toLong(), day, m.title) }

    /** Die nächsten Jahrestage der Momente mit Erinnerung nach [today] (exklusiv), nach Datum sortiert. */
    fun upcoming(moments: List<Moment>, today: LocalDate, count: Int = 4): List<Milestone> =
        moments.filter { it.yearlyReminder }
            .map { m ->
                val next = nextAnniversary(m, today.plusDays(1))
                Milestone(MilestoneKind.MOMENT, (next.year - m.date.year).toLong(), next, m.title)
            }
            .sortedBy { it.date }
            .take(count)

    /** Tage zwischen [m] und [today]: positiv = vergangen, negativ = in der Zukunft. */
    fun daysSince(m: Moment, today: LocalDate): Long = ChronoUnit.DAYS.between(m.date, today)
}
