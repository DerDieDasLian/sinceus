package app.sinceus.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
    /** Gehört zu dieser Beziehung, null = zu allen */
    val relationshipId: String? = null,
    /** Letzte Änderung in Millisekunden, entscheidet beim Abgleich, welche Fassung gilt */
    val updatedAt: Long = 0,
    /** Wer den Moment angelegt hat (ID einer [Person]), null = unbekannt */
    val addedBy: String? = null,
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
                    .put("remind", m.yearlyReminder)
                    .put("rel", m.relationshipId ?: JSONObject.NULL)
                    .put("updated", m.updatedAt)
                    .put("by", m.addedBy ?: JSONObject.NULL),
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
                    relationshipId = if (o.isNull("rel")) null else o.optString("rel").ifBlank { null },
                    updatedAt = o.optLong("updated", 0),
                    addedBy = if (o.isNull("by")) null else o.optString("by").ifBlank { null },
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

    /** Geplante Momente nach [today] (exklusiv), die nächsten zuerst. */
    fun planned(moments: List<Moment>, today: LocalDate, count: Int = 4): List<Milestone> =
        moments.filter { it.date.isAfter(today) }
            .sortedBy { it.date }
            .take(count)
            .map { Milestone(MilestoneKind.PLANNED, 0, it.date, it.title) }

    /**
     * Momente auf [day], die schon vorher eingetragen wurden, also wirklich geplant waren.
     * Ein Moment, den man am selben Tag nachträgt, ist keine Überraschung mehr.
     */
    fun plannedOn(moments: List<Moment>, day: LocalDate, zone: ZoneId = ZoneId.systemDefault()): List<Moment> =
        moments.filter { m ->
            m.date == day && m.updatedAt > 0 && Instant.ofEpochMilli(m.updatedAt).atZone(zone).toLocalDate().isBefore(day)
        }

    /** Tage zwischen [m] und [today]: positiv = vergangen, negativ = in der Zukunft. */
    fun daysSince(m: Moment, today: LocalDate): Long = ChronoUnit.DAYS.between(m.date, today)
}

/** Suche und Filter in der Zeitleiste, erst sinnvoll ab einigen Momenten */
object MomentFilter {
    const val MIN_MOMENTS = 8

    /**
     * Passt [m] zu Suchtext, Jahr und Beziehung? Leerer Text und null bedeuten jeweils „alles“.
     * Momente für alle Beziehungen passen zu jeder Beziehung.
     */
    fun matches(m: Moment, query: String, year: Int? = null, relationshipId: String? = null): Boolean {
        val q = query.trim()
        if (q.isNotEmpty() && !m.title.contains(q, ignoreCase = true) && !m.note.contains(q, ignoreCase = true)) return false
        if (year != null && m.date.year != year) return false
        if (relationshipId != null && m.relationshipId != null && m.relationshipId != relationshipId) return false
        return true
    }

    /** Jahre mit Momenten, das neueste zuerst */
    fun years(moments: List<Moment>): List<Int> = moments.map { it.date.year }.distinct().sortedDescending()
}

/** Rückblick auf ein Beziehungsjahr: Nummer des Jahres, Zeitraum und die Momente darin */
data class YearReview(val year: Int, val from: LocalDate, val to: LocalDate, val moments: List<Moment>)

object YearReviewMath {
    /** So viele Tage ab dem Jahrestag ist der Rückblick zu sehen */
    const val DAYS_SHOWN = 7

    /** Rückblick am Jahrestag und in der Woche danach, sonst null */
    fun reviewFor(start: LocalDate, today: LocalDate, moments: List<Moment>): YearReview? {
        var years = today.year - start.year
        // plusYears klemmt den 29.02. auf den 28.02., so wie die Jahrestage in der App
        if (start.plusYears(years.toLong()).isAfter(today)) years--
        if (years < 1) return null
        val anniversary = start.plusYears(years.toLong())
        if (ChronoUnit.DAYS.between(anniversary, today) >= DAYS_SHOWN) return null
        val from = start.plusYears(years - 1L)
        val inYear = moments.filter { !it.date.isBefore(from) && it.date.isBefore(anniversary) }.sortedBy { it.date }
        return YearReview(years, from, anniversary.minusDays(1), inYear)
    }
}
