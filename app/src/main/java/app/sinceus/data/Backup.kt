package app.sinceus.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.time.LocalDate
import java.time.LocalTime

/** Aufbau der Sicherungsdatei (ZIP): backup.json plus die Fotos in photos/, slides/ und moments/. */
object Backup {
    const val MIME = "application/zip"
    const val JSON = "backup.json"
    const val PHOTOS = "photos"
    const val SLIDES = "slides"
    const val MOMENTS = "moments"
    val FOLDERS = setOf(PHOTOS, SLIDES, MOMENTS)

    /** Größte erlaubte backup.json, schützt vor kaputten oder untergeschobenen Dateien */
    private const val MAX_JSON_BYTES = 5 * 1024 * 1024

    private val SAFE_NAME = Regex("[A-Za-z0-9_-][A-Za-z0-9._-]{0,127}")

    fun fileName(today: LocalDate) = "since-us-backup-$today.zip"

    /** Nur einfache Dateinamen ohne Ordner oder „..“, damit nichts außerhalb des App-Speichers landet */
    fun isSafeName(name: String) = SAFE_NAME.matches(name) && name != "." && name != ".."

    internal fun readLimited(input: InputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            out.write(buffer, 0, n)
            if (out.size() > MAX_JSON_BYTES) throw IOException("backup.json too large")
        }
        return out.toByteArray()
    }
}

/**
 * Wandelt die Daten in das JSON der Sicherung um und zurück. Fotos stehen darin nur mit
 * Dateinamen, die Pfade im App-Speicher setzt erst die Wiederherstellung.
 */
object BackupCodec {
    private const val APP = "app.sinceus"
    /** 2 = mit Menschen und Beziehungen (Poly-Modus), 1 = nur zwei Namen und ein Datum */
    const val FORMAT = 2

    fun encode(s: LoveSettings): String = JSONObject()
        .put("app", APP)
        .put("format", FORMAT)
        .put("people", JSONArray(PeopleCodec.encodePeople(s.people)))
        .put("relationships", JSONArray(PeopleCodec.encodeRelationships(s.relationships)))
        .put("selected", s.selected)
        .put("widgetRelationship", s.widgetRelationship)
        .put("discreet", s.discreet)
        .put("photo", s.photoPath?.let { File(it).name } ?: JSONObject.NULL)
        .put("preset", s.presetIndex)
        .put("focusX", s.focusX.toDouble())
        .put("focusY", s.focusY.toDouble())
        .put("zoom", s.zoom.toDouble())
        .put("notifications", s.notificationsEnabled)
        .put("notifyHour", s.notifyHour)
        .put("notifyMinute", s.notifyMinute)
        .put("remindBefore", s.remindBefore)
        .put("font", s.font)
        .put("homePage", s.homePage)
        .put("updateCheck", s.updateCheck)
        .put("showMoments", s.showMoments)
        .put("showLive", s.showLive)
        .put("showFacts", s.showFacts)
        .put("celebrate", s.celebrate)
        .put("slides", JSONArray().apply { s.slides.forEach { put(File(it).name) } })
        .put(
            "moments",
            JSONArray(MomentCodec.encode(s.moments.map { m -> m.copy(photoPath = m.photoPath?.let { File(it).name }) })),
        )
        .toString(2)

    /** Liest eine Sicherung; wirft eine Exception, wenn es keine (passende) Sicherung von Since Us ist. */
    fun decode(json: String): LoveSettings {
        val o = JSONObject(json)
        require(o.optString("app") == APP) { "not a Since Us backup" }
        require(o.optInt("format", 0) in 1..FORMAT) { "unsupported backup format" }
        val d = LoveSettings()
        val people: List<Person>
        val relationships: List<Relationship>
        if (o.has("people")) {
            people = PeopleCodec.decodePeople(o.optJSONArray("people")?.toString()).orEmpty()
            relationships = PeopleCodec.decodeRelationships(o.optJSONArray("relationships")?.toString()).orEmpty()
        } else {
            // Format 1: ein Paar
            people = listOf(
                Person(PeopleCodec.FIRST_ID, o.optString("name1").trim()),
                Person(PeopleCodec.SECOND_ID, o.optString("name2").trim()),
            )
            relationships = listOf(
                Relationship(
                    PeopleCodec.MAIN_ID,
                    listOf(PeopleCodec.FIRST_ID, PeopleCodec.SECOND_ID),
                    LocalDate.ofEpochDay(o.getLong("startDate")),
                    if (o.isNull("startTime")) null else LocalTime.ofSecondOfDay(o.getLong("startTime")),
                ),
            )
        }
        require(people.size >= 2 && people.all { it.name.isNotEmpty() }) { "names missing" }
        require(relationships.any { r -> r.members.count { id -> people.any { it.id == id } } >= 2 }) { "relationship missing" }
        val (cleanPeople, cleanRelationships) = PeopleCodec.sanitize(people, relationships, LocalDate.now())
        val selected = o.optString("selected")
            .takeIf { id -> id == ALL_RELATIONSHIPS || cleanRelationships.any { it.id == id } }
            ?: cleanRelationships.first().id
        fun fileName(value: String?) = value?.takeIf { Backup.isSafeName(it) }
        val slides = o.optJSONArray("slides")
        return LoveSettings(
            people = cleanPeople,
            relationships = cleanRelationships,
            selected = selected,
            widgetRelationship = o.optString("widgetRelationship").takeIf { id -> cleanRelationships.any { it.id == id } }.orEmpty(),
            discreet = o.optBoolean("discreet", false),
            photoPath = if (o.isNull("photo")) null else fileName(o.optString("photo")),
            presetIndex = o.optInt("preset", d.presetIndex),
            notificationsEnabled = o.optBoolean("notifications", d.notificationsEnabled),
            notifyHour = o.optInt("notifyHour", d.notifyHour).coerceIn(0, 23),
            notifyMinute = o.optInt("notifyMinute", d.notifyMinute).coerceIn(0, 59),
            remindBefore = o.optInt("remindBefore", d.remindBefore).takeIf { it in REMIND_OPTIONS } ?: 0,
            font = o.optString("font", d.font).take(40),
            focusX = o.optDouble("focusX", d.focusX.toDouble()).toFloat().coerceIn(-1f, 1f),
            focusY = o.optDouble("focusY", d.focusY.toDouble()).toFloat().coerceIn(-1f, 1f),
            zoom = o.optDouble("zoom", d.zoom.toDouble()).toFloat().coerceIn(1f, 4f),
            onboardingDone = true,
            homePage = o.optInt("homePage", d.homePage).coerceAtLeast(0),
            updateCheck = o.optBoolean("updateCheck", d.updateCheck),
            moments = MomentCodec.decode(o.optJSONArray("moments")?.toString())
                .map { m -> m.copy(photoPath = fileName(m.photoPath)) }
                .sortedBy { it.date },
            showMoments = o.optBoolean("showMoments", d.showMoments),
            showLive = o.optBoolean("showLive", d.showLive),
            showFacts = o.optBoolean("showFacts", d.showFacts),
            celebrate = o.optBoolean("celebrate", d.celebrate),
            slides = (0 until (slides?.length() ?: 0))
                .mapNotNull { fileName(slides?.optString(it)) }
                .distinct()
                .take(MAX_SLIDES),
        )
    }
}

/** Liste von Dateipfaden als JSON-Text, für die Diashow-Fotos */
object PathListCodec {
    fun encode(paths: List<String>): String = JSONArray().apply { paths.forEach { put(it) } }.toString()

    fun decode(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        val array = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
        return (0 until array.length()).mapNotNull { array.optString(it).ifBlank { null } }
    }
}
