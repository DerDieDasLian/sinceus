package app.sinceus.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.TimeZone

/** Fernbeziehung: Tage bis zum nächsten Treffen und die Uhrzeit am anderen Ort */
object Distance {
    /** Eine Stadt zur Auswahl: Zeitzone, Stadtname und Name der Zeitzone */
    data class City(val zone: String, val name: String, val zoneName: String)

    private val REGIONS = setOf("Africa", "America", "Antarctica", "Asia", "Atlantic", "Australia", "Europe", "Indian", "Pacific")

    fun isZone(id: String?): Boolean = id != null && runCatching { ZoneId.of(id) }.isSuccess

    /** Tage bis zum Treffen: 0 = heute, null = keins eingetragen oder schon vorbei */
    fun daysUntil(meeting: LocalDate?, today: LocalDate): Long? =
        meeting?.takeIf { !it.isBefore(today) }?.let { ChronoUnit.DAYS.between(today, it) }

    /** Unterschied in Minuten zwischen [far] und [here] im Moment [now], positiv = dort ist es später */
    fun offsetMinutes(far: ZoneId, here: ZoneId, now: Instant): Int =
        (far.rules.getOffset(now).totalSeconds - here.rules.getOffset(now).totalSeconds) / 60

    /** „America/New_York“ wird „New York“ */
    fun cityName(zone: String): String = zone.substringAfterLast('/').replace('_', ' ')

    /** Alle Städte mit eigener Zeitzone, nach Namen sortiert */
    fun cities(locale: Locale): List<City> = ZoneId.getAvailableZoneIds()
        .filter { id -> id.substringBefore('/') in REGIONS && id.count { it == '/' } >= 1 }
        .map { id -> City(id, cityName(id), TimeZone.getTimeZone(id).getDisplayName(false, TimeZone.LONG, locale)) }
        .distinctBy { it.name to it.zoneName }
        .sortedBy { it.name.lowercase(locale) }

    /** Passt die Stadt zur Suche? Gesucht wird im Stadtnamen, im Namen der Zeitzone und in der ID */
    fun matches(city: City, query: String): Boolean {
        val q = query.trim()
        return q.isEmpty() || listOf(city.name, city.zoneName, city.zone).any { it.contains(q, ignoreCase = true) }
    }
}
