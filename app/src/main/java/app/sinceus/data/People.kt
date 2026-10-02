package app.sinceus.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

/** Ein Mensch in der App, z. B. „Alex“ mit den Pronomen „sie/ihr“. */
data class Person(
    val id: String,
    val name: String,
    /** Freitext, leer = keine Angabe */
    val pronouns: String = "",
    /** Geburtstag, null = keine Angabe */
    val birthday: LocalDate? = null,
)

/**
 * Eine Beziehung zwischen zwei oder mehr Menschen mit eigenem Startdatum.
 * Ein Paar ist eine Beziehung mit zwei Menschen, eine Triade eine mit dreien. Bei poly Menschen
 * kann es mehrere Beziehungen geben, jede zählt dann ihre eigenen Tage.
 */
data class Relationship(
    val id: String,
    /** IDs der [Person]en, mindestens zwei */
    val members: List<String>,
    val startDate: LocalDate,
    /** Optionale Uhrzeit, zu der alles begann (nur für den Live-Zähler) */
    val startTime: LocalTime? = null,
    /** Eigene Bezeichnung, z. B. „Nesting-Partner“, leer = keine */
    val label: String = "",
    /** Mitteilungen zu den besonderen Tagen dieser Beziehung */
    val notify: Boolean = true,
    /** Fernbeziehung: Countdown zum nächsten Treffen und Uhrzeit am anderen Ort zeigen */
    val distance: Boolean = false,
    /** Zeitzone am anderen Ort, z. B. „America/New_York“, null = wie hier */
    val farZone: String? = null,
    /** Nächstes Treffen, null = keins eingetragen */
    val nextMeeting: LocalDate? = null,
) {
    val startDateTime: LocalDateTime get() = startDate.atTime(startTime ?: LocalTime.MIDNIGHT)
}

/** Höchstzahl Menschen und Beziehungen, damit Startseite und Widget lesbar bleiben */
const val MAX_PEOPLE = 8
const val MAX_RELATIONSHIPS = 12

/** Auswahl „alle Beziehungen“ auf der Startseite */
const val ALL_RELATIONSHIPS = "*"

fun newId(): String = UUID.randomUUID().toString().replace("-", "").take(12)

object Names {
    /** „Alex & Sam“, „Alex, Sam & Kim“ */
    fun join(names: List<String>): String = when (names.size) {
        0 -> ""
        1 -> names[0]
        else -> names.dropLast(1).joinToString(", ") + " & " + names.last()
    }

    /** Anfangsbuchstabe mit Punkt für den diskreten Modus: „Alex“ wird „A.“ */
    fun initial(name: String): String {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return ""
        val first = trimmed.codePointAt(0)
        return String(Character.toChars(Character.toUpperCase(first))) + "."
    }
}

/** Speichert Menschen und Beziehungen als JSON-Text. */
object PeopleCodec {
    fun encodePeople(people: List<Person>): String = JSONArray().apply {
        people.forEach {
            put(
                JSONObject()
                    .put("id", it.id)
                    .put("name", it.name)
                    .put("pronouns", it.pronouns)
                    .put("birthday", it.birthday?.toEpochDay() ?: JSONObject.NULL),
            )
        }
    }.toString()

    /** null, wenn nichts gespeichert ist; unlesbare Einträge werden übersprungen */
    fun decodePeople(json: String?): List<Person>? {
        val array = json?.let { runCatching { JSONArray(it) }.getOrNull() } ?: return null
        return (0 until array.length()).mapNotNull { i ->
            runCatching {
                val o = array.getJSONObject(i)
                Person(
                    id = o.getString("id").take(64),
                    name = o.getString("name").trim().take(MAX_NAME),
                    pronouns = o.optString("pronouns", "").trim().take(MAX_NAME),
                    birthday = if (o.isNull("birthday")) null else LocalDate.ofEpochDay(o.getLong("birthday")),
                )
            }.getOrNull()
        }.distinctBy { it.id }
    }

    fun encodeRelationships(relationships: List<Relationship>): String = JSONArray().apply {
        relationships.forEach { r ->
            put(
                JSONObject()
                    .put("id", r.id)
                    .put("members", JSONArray().apply { r.members.forEach { put(it) } })
                    .put("start", r.startDate.toEpochDay())
                    .put("time", r.startTime?.toSecondOfDay() ?: JSONObject.NULL)
                    .put("label", r.label)
                    .put("notify", r.notify)
                    .put("distance", r.distance)
                    .put("farZone", r.farZone ?: JSONObject.NULL)
                    .put("meeting", r.nextMeeting?.toEpochDay() ?: JSONObject.NULL),
            )
        }
    }.toString()

    fun decodeRelationships(json: String?): List<Relationship>? {
        val array = json?.let { runCatching { JSONArray(it) }.getOrNull() } ?: return null
        return (0 until array.length()).mapNotNull { i ->
            runCatching {
                val o = array.getJSONObject(i)
                val members = o.getJSONArray("members")
                Relationship(
                    id = o.getString("id").take(64),
                    members = (0 until members.length()).map { members.getString(it) }.distinct(),
                    startDate = LocalDate.ofEpochDay(o.getLong("start")),
                    startTime = if (o.isNull("time")) null else LocalTime.ofSecondOfDay(o.getLong("time")),
                    label = o.optString("label", "").trim().take(MAX_NAME),
                    notify = o.optBoolean("notify", true),
                    distance = o.optBoolean("distance", false),
                    farZone = if (o.isNull("farZone")) null else o.optString("farZone").takeIf { Distance.isZone(it) },
                    nextMeeting = if (o.isNull("meeting")) null else LocalDate.ofEpochDay(o.getLong("meeting")),
                )
            }.getOrNull()
        }.distinctBy { it.id }
    }

    /**
     * Bringt Menschen und Beziehungen in einen gültigen Zustand: Beziehungen nennen nur bekannte
     * Menschen und haben mindestens zwei davon. Bleibt keine Beziehung übrig, gehören alle zu einer.
     */
    fun sanitize(
        people: List<Person>,
        relationships: List<Relationship>,
        fallbackStart: LocalDate,
    ): Pair<List<Person>, List<Relationship>> {
        val persons = people.distinctBy { it.id }.take(MAX_PEOPLE).toMutableList()
        while (persons.size < 2) persons += Person(newId(), "")
        val ids = persons.map { it.id }.toSet()
        val rels = relationships
            .map { r -> r.copy(members = r.members.filter { it in ids }.distinct()) }
            .filter { it.members.size >= 2 }
            .distinctBy { it.id }
            .take(MAX_RELATIONSHIPS)
            .ifEmpty { listOf(Relationship(MAIN_ID, persons.map { it.id }, fallbackStart)) }
        return persons to rels
    }

    const val MAX_NAME = 40

    /** IDs für Daten aus der Zeit vor dem Poly-Modus */
    const val MAIN_ID = "main"
    const val FIRST_ID = "a"
    const val SECOND_ID = "b"
}

object BirthdayMath {
    /** Nächster Geburtstag ab [from] (einschließlich). Wer am 29.02. geboren ist, feiert sonst am 28.02. */
    fun next(birthday: LocalDate, from: LocalDate): LocalDate {
        var years = maxOf(0, from.year - birthday.year).toLong()
        var next = birthday.plusYears(years)
        while (next.isBefore(from)) next = birthday.plusYears(++years)
        return next
    }

    /** Geburtstage, die genau auf [day] fallen; [title] ist der Name, [value] das neue Alter. */
    fun milestonesOn(people: List<Person>, day: LocalDate, name: (Person) -> String = { it.name }): List<Milestone> =
        people.mapNotNull { p ->
            val b = p.birthday ?: return@mapNotNull null
            if (!day.isAfter(b) || next(b, day) != day) return@mapNotNull null
            Milestone(MilestoneKind.BIRTHDAY, (day.year - b.year).toLong(), day, name(p))
        }

    /** Die nächsten Geburtstage nach [today] (exklusiv), nach Datum sortiert. */
    fun upcoming(people: List<Person>, today: LocalDate, count: Int = 4, name: (Person) -> String = { it.name }): List<Milestone> =
        people.mapNotNull { p ->
            val b = p.birthday ?: return@mapNotNull null
            val next = next(b, today.plusDays(1))
            if (next == b) return@mapNotNull null
            Milestone(MilestoneKind.BIRTHDAY, (next.year - b.year).toLong(), next, name(p))
        }
            .sortedBy { it.date }
            .take(count)
}
