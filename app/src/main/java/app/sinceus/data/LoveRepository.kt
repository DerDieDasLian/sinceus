package app.sinceus.data

import app.sinceus.BuildConfig
import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private val Context.dataStore by preferencesDataStore(name = "love")

/** Mögliche Vorab-Erinnerungen in Tagen, 0 = aus */
val REMIND_OPTIONS = listOf(0, 1, 7)

/** Die meisten Paarfotos haben die Gesichter im oberen Bilddrittel */
const val DEFAULT_FOCUS_Y = -0.6f

data class LoveSettings(
    /** Alle Menschen, mindestens zwei */
    val people: List<Person> = listOf(Person(PeopleCodec.FIRST_ID, ""), Person(PeopleCodec.SECOND_ID, "")),
    /** Alle Beziehungen, mindestens eine. Bei einem Paar gibt es genau eine. */
    val relationships: List<Relationship> = listOf(
        Relationship(PeopleCodec.MAIN_ID, people.map { it.id }, LocalDate.now()),
    ),
    /** Auf der Startseite gezeigte Beziehung, [ALL_RELATIONSHIPS] = alle zusammen */
    val selected: String = relationships.first().id,
    /** Beziehung für die Widgets, leer = die erste */
    val widgetRelationship: String = "",
    /** Diskreter Modus: Widgets und Mitteilungen zeigen nur Anfangsbuchstaben */
    val discreet: Boolean = false,
    /** Pfad zum eigenen Foto im App-Speicher, null = Standardmotiv */
    val photoPath: String? = null,
    val presetIndex: Int = 0,
    val notificationsEnabled: Boolean = true,
    val notifyHour: Int = 9,
    val notifyMinute: Int = 0,
    /** Zusätzlich so viele Tage vorher erinnern (1 oder 7), 0 = nur am Tag selbst */
    val remindBefore: Int = 0,
    /** Gewählte Schrift (ID aus AppFonts), "" = Standardschrift der App */
    val font: String = "",
    /** Bildausschnitt: Fokuspunkt (-1..1) und Zoom (1..4) */
    val focusX: Float = 0f,
    val focusY: Float = DEFAULT_FOCUS_Y,
    val zoom: Float = 1f,
    val onboardingDone: Boolean = false,
    /** 0 = Übersicht, 1 = Live-Zähler */
    val homePage: Int = 0,
    /** Nur GitHub-Variante: automatisch nach neuen Versionen suchen */
    val updateCheck: Boolean = true,
    /** Neuere Version auf GitHub, falls gefunden */
    val updateVersion: String? = null,
    val updateUrl: String? = null,
    /** Direkter Download der neuen APK, falls das Release eine enthält */
    val updateApkUrl: String? = null,
    /** Wichtige Momente der Beziehung, sortiert nach Datum */
    val moments: List<Moment> = emptyList(),
    /** Bereich „Momente“ anzeigen (lässt sich ausblenden, die Daten bleiben erhalten) */
    val showMoments: Boolean = true,
    /** Live-Zähler anzeigen (lässt sich ausblenden) */
    val showLive: Boolean = true,
    /** Lustige Zahlen auf der Übersicht (z. B. Wochenenden und Vollmonde zusammen) */
    val showFacts: Boolean = true,
    /** Kleine Konfetti-Feier an Jahrestagen und runden Tagen */
    val celebrate: Boolean = true,
    /** Weitere Fotos für die Diashow im Titelbild (Pfade im App-Speicher, files/slides/) */
    val slides: List<String> = emptyList(),
    /** Version (versionCode), deren Neuigkeiten schon gezeigt wurden, 0 = unbekannt */
    val changelogSeen: Int = 0,
    /** Nach einem Update zeigen, was neu ist */
    val showChangelog: Boolean = true,
) {
    /** Die gerade gezeigte Beziehung (bei „alle“ die erste) */
    val relationship: Relationship
        get() = relationships.firstOrNull { it.id == selected } ?: relationships.first()

    /** true, wenn die Startseite alle Beziehungen zusammen zeigt */
    val showAll: Boolean get() = selected == ALL_RELATIONSHIPS && relationships.size > 1

    /** Mehr als zwei Menschen oder mehrere Beziehungen */
    val isPoly: Boolean get() = people.size > 2 || relationships.size > 1

    fun person(id: String): Person? = people.firstOrNull { it.id == id }

    fun members(r: Relationship = relationship): List<Person> = r.members.mapNotNull(::person)

    fun memberNames(r: Relationship = relationship): List<String> = members(r).map { it.name }

    fun namesOf(r: Relationship): String = Names.join(memberNames(r))

    /** Namen für Widgets und Mitteilungen, im diskreten Modus nur Anfangsbuchstaben */
    fun shownNames(r: Relationship = relationship): List<String> =
        memberNames(r).map { if (discreet) Names.initial(it) else it }

    val names: String get() = namesOf(relationship)
    val name1: String get() = memberNames().getOrElse(0) { "" }
    val name2: String get() = memberNames().getOrElse(1) { "" }
    val startDate: LocalDate get() = relationship.startDate
    val startTime: LocalTime? get() = relationship.startTime
    val startDateTime: LocalDateTime get() = relationship.startDateTime

    /** Beziehung der Widgets */
    val widget: Relationship get() = relationships.firstOrNull { it.id == widgetRelationship } ?: relationships.first()

    /** Dieselben Daten mit der Beziehung der Widgets als gezeigter Beziehung */
    fun forWidget(): LoveSettings = copy(selected = widget.id)

    /** Momente einer Beziehung: ihre eigenen und die für alle */
    fun momentsOf(r: Relationship): List<Moment> =
        moments.filter { it.relationshipId == null || it.relationshipId == r.id || relationships.none { x -> x.id == it.relationshipId } }

    /** Momente der gezeigten Beziehung, bei „alle“ sämtliche */
    val visibleMoments: List<Moment> get() = if (showAll) moments else momentsOf(relationship)

    companion object {
        /** Ein Paar mit einer Beziehung, z. B. für Tests und Vorschauen */
        fun couple(name1: String, name2: String, startDate: LocalDate, startTime: LocalTime? = null) = LoveSettings(
            people = listOf(Person(PeopleCodec.FIRST_ID, name1), Person(PeopleCodec.SECOND_ID, name2)),
            relationships = listOf(
                Relationship(
                    PeopleCodec.MAIN_ID,
                    listOf(PeopleCodec.FIRST_ID, PeopleCodec.SECOND_ID),
                    startDate,
                    startTime,
                ),
            ),
        )
    }
}

private object Keys {
    val name1 = stringPreferencesKey("name1")
    val name2 = stringPreferencesKey("name2")
    val start = longPreferencesKey("start_epoch_day")
    val startTime = intPreferencesKey("start_second_of_day")
    val photo = stringPreferencesKey("photo_path")
    val preset = intPreferencesKey("preset")
    val notify = booleanPreferencesKey("notify")
    val notifyHour = intPreferencesKey("notify_hour")
    val notifyMinute = intPreferencesKey("notify_minute")
    val remindBefore = intPreferencesKey("remind_before")
    val font = stringPreferencesKey("font")
    val changelogSeen = intPreferencesKey("changelog_seen")
    val showChangelog = booleanPreferencesKey("show_changelog")
    val lastNotified = longPreferencesKey("last_notified_epoch_day")
    val focusX = floatPreferencesKey("focus_x")
    val focusY = floatPreferencesKey("focus_y")
    val zoom = floatPreferencesKey("zoom")
    val onboardingDone = booleanPreferencesKey("onboarding_done")
    val homePage = intPreferencesKey("home_page")
    val updateCheck = booleanPreferencesKey("update_check")
    val lastUpdateCheck = longPreferencesKey("last_update_check")
    val updateVersion = stringPreferencesKey("update_version")
    val updateUrl = stringPreferencesKey("update_url")
    val updateApkUrl = stringPreferencesKey("update_apk_url")
    val updateNotified = stringPreferencesKey("update_notified")
    val moments = stringPreferencesKey("moments")
    val showMoments = booleanPreferencesKey("show_moments")
    val showLive = booleanPreferencesKey("show_live")
    val showFacts = booleanPreferencesKey("show_facts")
    val celebrate = booleanPreferencesKey("celebrate")
    val slides = stringPreferencesKey("slides")
    val people = stringPreferencesKey("people")
    val relationships = stringPreferencesKey("relationships")
    val selected = stringPreferencesKey("selected_relationship")
    val widgetRelationship = stringPreferencesKey("widget_relationship")
    val discreet = booleanPreferencesKey("discreet")
    val deleted = stringPreferencesKey("deleted_moments")
}

/**
 * Menschen und Beziehungen aus den gespeicherten Daten. Ältere Versionen kannten nur zwei Namen
 * und ein Datum, daraus wird hier ein Paar mit einer Beziehung.
 */
private fun Preferences.model(): Pair<List<Person>, List<Relationship>> {
    val start = this[Keys.start]?.let(LocalDate::ofEpochDay) ?: LocalDate.now()
    val people = PeopleCodec.decodePeople(this[Keys.people]) ?: listOf(
        Person(PeopleCodec.FIRST_ID, this[Keys.name1].orEmpty()),
        Person(PeopleCodec.SECOND_ID, this[Keys.name2].orEmpty()),
    )
    val relationships = PeopleCodec.decodeRelationships(this[Keys.relationships]) ?: listOf(
        Relationship(
            PeopleCodec.MAIN_ID,
            listOf(PeopleCodec.FIRST_ID, PeopleCodec.SECOND_ID),
            start,
            this[Keys.startTime]?.let { LocalTime.ofSecondOfDay(it.toLong()) },
        ),
    )
    return PeopleCodec.sanitize(people, relationships, start)
}

private fun MutablePreferences.writeModel(people: List<Person>, relationships: List<Relationship>) {
    val (p, r) = PeopleCodec.sanitize(people, relationships, LocalDate.now())
    this[Keys.people] = PeopleCodec.encodePeople(p)
    this[Keys.relationships] = PeopleCodec.encodeRelationships(r)
    // Die alten Einträge stehen jetzt in den neuen
    remove(Keys.name1)
    remove(Keys.name2)
    remove(Keys.start)
    remove(Keys.startTime)
}

/** Höchstzahl weiterer Fotos in der Diashow */
const val MAX_SLIDES = 20

class LoveRepository(private val context: Context) {

    val settings: Flow<LoveSettings> = context.dataStore.data.map { it.toSettings() }

    suspend fun current(): LoveSettings = settings.first()

    private fun Preferences.toSettings(): LoveSettings {
        val d = LoveSettings()
        val (people, relationships) = model()
        return LoveSettings(
            people = people,
            relationships = relationships,
            selected = this[Keys.selected]
                ?.takeIf { id -> id == ALL_RELATIONSHIPS || relationships.any { it.id == id } }
                ?: relationships.first().id,
            widgetRelationship = this[Keys.widgetRelationship].orEmpty(),
            discreet = this[Keys.discreet] ?: false,
            photoPath = this[Keys.photo]?.takeIf { File(it).exists() },
            presetIndex = this[Keys.preset] ?: d.presetIndex,
            notificationsEnabled = this[Keys.notify] ?: d.notificationsEnabled,
            notifyHour = this[Keys.notifyHour] ?: d.notifyHour,
            notifyMinute = this[Keys.notifyMinute] ?: d.notifyMinute,
            remindBefore = this[Keys.remindBefore] ?: d.remindBefore,
            font = this[Keys.font] ?: d.font,
            focusX = this[Keys.focusX] ?: d.focusX,
            focusY = this[Keys.focusY] ?: d.focusY,
            zoom = this[Keys.zoom] ?: d.zoom,
            onboardingDone = this[Keys.onboardingDone] ?: d.onboardingDone,
            homePage = this[Keys.homePage] ?: d.homePage,
            updateCheck = this[Keys.updateCheck] ?: d.updateCheck,
            updateVersion = this[Keys.updateVersion],
            updateUrl = this[Keys.updateUrl],
            updateApkUrl = this[Keys.updateApkUrl],
            moments = MomentCodec.decode(this[Keys.moments]).sortedBy { it.date },
            showMoments = this[Keys.showMoments] ?: d.showMoments,
            showLive = this[Keys.showLive] ?: d.showLive,
            showFacts = this[Keys.showFacts] ?: d.showFacts,
            celebrate = this[Keys.celebrate] ?: d.celebrate,
            slides = PathListCodec.decode(this[Keys.slides]).filter { File(it).exists() },
            changelogSeen = this[Keys.changelogSeen] ?: d.changelogSeen,
            showChangelog = this[Keys.showChangelog] ?: d.showChangelog,
        )
    }

    /**
     * Namen aus der Einrichtung: bestehende Menschen behalten ihre ID (und Pronomen), weitere kommen
     * dazu, überzählige fallen weg. Gibt es nur eine Beziehung, gehören alle Genannten dazu.
     */
    suspend fun setNames(names: List<String>) = context.dataStore.edit { prefs ->
        val (people, relationships) = prefs.model()
        val cleaned = names.map { it.trim().take(PeopleCodec.MAX_NAME) }.filter { it.isNotEmpty() }.take(MAX_PEOPLE)
        val updated = cleaned.mapIndexed { i, name ->
            people.getOrNull(i)?.copy(name = name) ?: Person(newId(), name)
        }
        val rels = if (relationships.size == 1) {
            listOf(relationships[0].copy(members = updated.map { it.id }))
        } else {
            relationships
        }
        prefs.writeModel(updated, rels)
    }

    /** Startdatum der ersten Beziehung (aus der Einrichtung) */
    suspend fun setStartDate(date: LocalDate) = context.dataStore.edit { prefs ->
        val (people, relationships) = prefs.model()
        prefs.writeModel(people, listOf(relationships[0].copy(startDate = date)) + relationships.drop(1))
    }

    /** Uhrzeit der ersten Beziehung (aus der Einrichtung) */
    suspend fun setStartTime(time: LocalTime?) = context.dataStore.edit { prefs ->
        val (people, relationships) = prefs.model()
        prefs.writeModel(people, listOf(relationships[0].copy(startTime = time)) + relationships.drop(1))
    }

    /** Neuer oder geänderter Mensch */
    suspend fun savePerson(person: Person) = context.dataStore.edit { prefs ->
        val (people, relationships) = prefs.model()
        val clean = person.copy(
            name = person.name.trim().take(PeopleCodec.MAX_NAME),
            pronouns = person.pronouns.trim().take(PeopleCodec.MAX_NAME),
        )
        val list = if (people.any { it.id == clean.id }) {
            people.map { if (it.id == clean.id) clean else it }
        } else {
            people + clean
        }
        prefs.writeModel(list, relationships)
    }

    /** Entfernt einen Menschen; Beziehungen mit weniger als zwei Menschen fallen dabei weg. */
    suspend fun deletePerson(id: String) = context.dataStore.edit { prefs ->
        val (people, relationships) = prefs.model()
        if (people.size <= 2) return@edit
        prefs.writeModel(people.filterNot { it.id == id }, relationships)
    }

    /** Neue oder geänderte Beziehung */
    suspend fun saveRelationship(relationship: Relationship) = context.dataStore.edit { prefs ->
        val (people, relationships) = prefs.model()
        val clean = relationship.copy(label = relationship.label.trim().take(PeopleCodec.MAX_NAME))
        val list = if (relationships.any { it.id == clean.id }) {
            relationships.map { if (it.id == clean.id) clean else it }
        } else {
            relationships + clean
        }
        prefs.writeModel(people, list)
    }

    /** Entfernt eine Beziehung (nie die letzte); ihre Momente gelten danach für alle. */
    suspend fun deleteRelationship(id: String) = context.dataStore.edit { prefs ->
        val (people, relationships) = prefs.model()
        if (relationships.size <= 1) return@edit
        prefs.writeModel(people, relationships.filterNot { it.id == id })
        prefs[Keys.moments] = MomentCodec.encode(
            MomentCodec.decode(prefs[Keys.moments]).map { if (it.relationshipId == id) it.copy(relationshipId = null) else it },
        )
        if (prefs[Keys.selected] == id) prefs.remove(Keys.selected)
        if (prefs[Keys.widgetRelationship] == id) prefs.remove(Keys.widgetRelationship)
    }

    suspend fun setSelected(id: String) = context.dataStore.edit { it[Keys.selected] = id }

    suspend fun setWidgetRelationship(id: String) = context.dataStore.edit { it[Keys.widgetRelationship] = id }

    suspend fun setDiscreet(on: Boolean) = context.dataStore.edit { it[Keys.discreet] = on }

    /**
     * Speichert einen neuen oder geänderten Moment. [newPhoto] ersetzt das bisherige Foto,
     * [removePhoto] entfernt es.
     */
    suspend fun saveMoment(moment: Moment, newPhoto: Uri?, removePhoto: Boolean) {
        val old = current().moments.firstOrNull { it.id == moment.id }
        var photo = if (removePhoto) null else old?.photoPath
        if (newPhoto != null) {
            photo = withContext(Dispatchers.IO) {
                val file = File(momentDir(), "${moment.id}_${System.currentTimeMillis()}.jpg")
                if (!PhotoShrink.copy(context, newPhoto, file)) return@withContext photo
                file.absolutePath
            }
        }
        val saved = moment.copy(
            photoPath = photo,
            updatedAt = System.currentTimeMillis(),
            addedBy = old?.addedBy ?: moment.addedBy ?: PairingStore.load(context)?.me,
        )
        // Erst dataStore aktualisieren, dann alte Datei löschen
        context.dataStore.edit { prefs ->
            val list = MomentCodec.decode(prefs[Keys.moments]).filterNot { it.id == moment.id } + saved
            prefs[Keys.moments] = MomentCodec.encode(list)
        }
        if (old?.photoPath != null && old.photoPath != photo) deleteFile(old.photoPath)
    }

    suspend fun setShowMoments(show: Boolean) = context.dataStore.edit { it[Keys.showMoments] = show }

    suspend fun setShowLive(show: Boolean) = context.dataStore.edit { it[Keys.showLive] = show }

    suspend fun setShowFacts(show: Boolean) = context.dataStore.edit { it[Keys.showFacts] = show }

    suspend fun setCelebrate(on: Boolean) = context.dataStore.edit { it[Keys.celebrate] = on }

    suspend fun deleteMoment(id: String) {
        val old = current().moments.firstOrNull { it.id == id } ?: return
        old.photoPath?.let { deleteFile(it) }
        context.dataStore.edit { prefs ->
            prefs[Keys.moments] = MomentCodec.encode(MomentCodec.decode(prefs[Keys.moments]).filterNot { it.id == id })
            // Merker fürs Löschen, damit der Abgleich den Moment nicht wiederbringt
            prefs[Keys.deleted] = encodeDeleted(decodeDeleted(prefs[Keys.deleted]) + (id to System.currentTimeMillis()))
        }
    }

    /**
     * Schreibt eine verschlüsselte Abgleich-Datei für das gekoppelte Handy: Menschen, Beziehungen,
     * Momente mit Fotos und gelöschte Momente. Der Stream wird danach geschlossen.
     */
    suspend fun exportSync(out: OutputStream, pairing: Pairing) {
        val s = current()
        val deleted = decodeDeleted(context.dataStore.data.first()[Keys.deleted])
        withContext(Dispatchers.IO) {
            val zipFile = File(context.cacheDir, "sync-out.zip")
            try {
                ZipOutputStream(zipFile.outputStream().buffered()).use { zip ->
                    zip.putNextEntry(ZipEntry(SyncCodec.JSON))
                    zip.write(
                        SyncCodec.encode(SyncData(s.people, s.relationships, s.moments, deleted, pairing.me))
                            .toByteArray(Charsets.UTF_8),
                    )
                    zip.closeEntry()
                    val written = mutableSetOf<String>()
                    s.moments.forEach { m ->
                        val file = m.photoPath?.let(::File)?.takeIf { it.isFile } ?: return@forEach
                        if (!written.add(file.name)) return@forEach
                        zip.putNextEntry(ZipEntry("${Backup.MOMENTS}/${file.name}"))
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
                out.buffered().use { o -> zipFile.inputStream().buffered().use { SyncCrypto.encrypt(it, o, pairing.key) } }
            } finally {
                if (zipFile.exists()) zipFile.delete()
            }
        }
    }

    /**
     * Übernimmt eine Abgleich-Datei vom gekoppelten Handy und führt sie mit den eigenen Daten
     * zusammen. Gehört die Datei zu keiner Kopplung mit diesem Handy oder ist sie beschädigt,
     * wird eine Exception geworfen und nichts verändert.
     */
    suspend fun importSync(input: InputStream, pairing: Pairing): MergeResult {
        val dir = File(context.cacheDir, "sync-in")
        val remote = withContext(Dispatchers.IO) {
            dir.deleteRecursively()
            dir.mkdirs()
            val zipFile = File(dir, "sync.zip")
            zipFile.outputStream().buffered().use { SyncCrypto.decrypt(input.buffered(), it, pairing.key) }
            var json: String? = null
            ZipInputStream(zipFile.inputStream().buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) continue
                    val parts = entry.name.split('/')
                    if (entry.name == SyncCodec.JSON) {
                        json = Backup.readLimited(zip).toString(Charsets.UTF_8)
                    } else if (parts.size == 2 && parts[0] == Backup.MOMENTS && Backup.isSafeName(parts[1])) {
                        File(File(dir, Backup.MOMENTS).apply { mkdirs() }, parts[1]).outputStream().use { zip.copyTo(it) }
                    }
                }
            }
            zipFile.delete()
            SyncCodec.decode(json ?: throw IOException("sync.json missing"))
        }

        val local = current()
        val result = SyncMerge.merge(
            local.people,
            local.relationships,
            local.moments,
            decodeDeleted(context.dataStore.data.first()[Keys.deleted]),
            remote,
        )
        // Fotos der übernommenen Momente in den App-Speicher holen, alte Fotos aufräumen
        val takenIds = result.taken.map { it.id }.toSet()
        val moments = withContext(Dispatchers.IO) {
            val merged = result.moments.map { m ->
                if (m.id !in takenIds) return@map m
                val source = m.photoPath?.let { File(File(dir, Backup.MOMENTS), it) }?.takeIf { it.isFile }
                val target = source?.let { File(momentDir(), it.name) }
                if (source != null && target != null) source.copyTo(target, overwrite = true)
                m.copy(photoPath = target?.absolutePath)
            }
            val keep = merged.mapNotNull { it.photoPath }.toSet()
            local.moments.mapNotNull { it.photoPath }.filter { it !in keep }.forEach { File(it).delete() }
            dir.deleteRecursively()
            merged
        }
        context.dataStore.edit { prefs ->
            prefs.writeModel(result.people, result.relationships)
            prefs[Keys.moments] = MomentCodec.encode(moments)
            prefs[Keys.deleted] = encodeDeleted(result.deleted)
        }
        PairingStore.save(context, pairing.copy(lastSync = System.currentTimeMillis()))
        return result
    }

    private fun decodeDeleted(json: String?): Map<String, Long> {
        val o = json?.let { runCatching { JSONObject(it) }.getOrNull() } ?: return emptyMap()
        return o.keys().asSequence().associateWith { o.optLong(it) }
    }

    private fun encodeDeleted(map: Map<String, Long>): String =
        JSONObject().apply { map.forEach { (id, time) -> put(id, time) } }.toString()

    private fun momentDir() = File(context.filesDir, "moments").apply { mkdirs() }

    private suspend fun deleteFile(path: String) = withContext(Dispatchers.IO) { File(path).delete() }

    /** Fügt weitere Fotos für die Diashow hinzu (höchstens [MAX_SLIDES] insgesamt). */
    suspend fun addSlides(uris: List<Uri>) {
        val room = MAX_SLIDES - current().slides.size
        if (room <= 0 || uris.isEmpty()) return
        val stamp = System.currentTimeMillis()
        val added = withContext(Dispatchers.IO) {
            uris.take(room).mapIndexedNotNull { i, uri ->
                val file = File(slideDir(), "slide_${stamp}_$i.jpg")
                if (PhotoShrink.copy(context, uri, file)) file.absolutePath else null
            }
        }
        context.dataStore.edit { prefs ->
            prefs[Keys.slides] = PathListCodec.encode(PathListCodec.decode(prefs[Keys.slides]) + added)
        }
    }

    suspend fun removeSlide(path: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.slides] = PathListCodec.encode(PathListCodec.decode(prefs[Keys.slides]) - path)
        }
        deleteFile(path)
    }

    private fun slideDir() = File(context.filesDir, "slides").apply { mkdirs() }

    /**
     * Schreibt alle Daten mit Fotos als ZIP-Datei in [out]: backup.json plus die Ordner
     * photos/, slides/ und moments/. Der Stream wird danach geschlossen.
     */
    suspend fun exportBackup(out: OutputStream) {
        val s = current()
        withContext(Dispatchers.IO) {
            ZipOutputStream(out.buffered()).use { zip ->
                zip.putNextEntry(ZipEntry(Backup.JSON))
                zip.write(BackupCodec.encode(s).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                val written = mutableSetOf<String>()
                fun add(folder: String, path: String?) {
                    val file = path?.let(::File)?.takeIf { it.isFile } ?: return
                    val name = "$folder/${file.name}"
                    if (!written.add(name)) return
                    zip.putNextEntry(ZipEntry(name))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
                add(Backup.PHOTOS, s.photoPath)
                s.slides.forEach { add(Backup.SLIDES, it) }
                s.moments.forEach { add(Backup.MOMENTS, it.photoPath) }
            }
        }
    }

    /**
     * Stellt eine Sicherung aus [input] wieder her und ersetzt dabei alle bisherigen Daten.
     * Ist die Datei keine gültige Sicherung, wird eine Exception geworfen und nichts verändert.
     */
    suspend fun importBackup(input: InputStream) {
        val restore = File(context.cacheDir, "restore")
        val data = withContext(Dispatchers.IO) {
            restore.deleteRecursively()
            var json: String? = null
            ZipInputStream(input.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) continue
                    val parts = entry.name.split('/')
                    if (entry.name == Backup.JSON) {
                        json = Backup.readLimited(zip).toString(Charsets.UTF_8)
                    } else if (parts.size == 2 && parts[0] in Backup.FOLDERS && Backup.isSafeName(parts[1])) {
                        // Nur einfache Dateinamen in den bekannten Ordnern, nie Pfade nach außerhalb
                        val target = File(File(restore, parts[0]).apply { mkdirs() }, parts[1])
                        target.outputStream().use { zip.copyTo(it) }
                    }
                }
            }
            BackupCodec.decode(json ?: throw IOException("backup.json missing"))
        }

        // Ab hier ist die Sicherung gültig: alte Dateien durch die aus der Sicherung ersetzen
        val restored = withContext(Dispatchers.IO) {
            fun move(folder: String, dir: File, name: String?): String? {
                val source = name?.let { File(File(restore, folder), it) }?.takeIf { it.isFile } ?: return null
                val target = File(dir, name)
                source.copyTo(target, overwrite = true)
                return target.absolutePath
            }
            // Neue Dateien zuerst schreiben
            val result = data.copy(
                photoPath = move(Backup.PHOTOS, photoDir(), data.photoPath),
                slides = data.slides.mapNotNull { move(Backup.SLIDES, slideDir(), it) },
                moments = data.moments.map { it.copy(photoPath = move(Backup.MOMENTS, momentDir(), it.photoPath)) },
            )
            // Dann alle alten Fotos löschen (die nicht überschrieben wurden)
            deletePhotos()
            slideDir().listFiles()?.forEach { File(it.absolutePath).delete() }
            momentDir().listFiles()?.forEach { File(it.absolutePath).delete() }
            restore.deleteRecursively()
            result
        }

        context.dataStore.edit { p ->
            // Merker für schon verschickte Mitteilungen behalten, damit nichts doppelt kommt
            val lastNotified = p[Keys.lastNotified]
            p.clear()
            lastNotified?.let { p[Keys.lastNotified] = it }
            p.writeModel(restored.people, restored.relationships)
            p[Keys.selected] = restored.selected
            if (restored.widgetRelationship.isNotEmpty()) p[Keys.widgetRelationship] = restored.widgetRelationship
            p[Keys.discreet] = restored.discreet
            restored.photoPath?.let { p[Keys.photo] = it }
            p[Keys.preset] = restored.presetIndex
            p[Keys.notify] = restored.notificationsEnabled
            p[Keys.notifyHour] = restored.notifyHour
            p[Keys.notifyMinute] = restored.notifyMinute
            p[Keys.remindBefore] = restored.remindBefore
            p[Keys.font] = restored.font
            p[Keys.focusX] = restored.focusX
            p[Keys.focusY] = restored.focusY
            p[Keys.zoom] = restored.zoom
            p[Keys.onboardingDone] = true
            if (p[Keys.changelogSeen] == null) p[Keys.changelogSeen] = BuildConfig.VERSION_CODE
            p[Keys.homePage] = restored.homePage
            p[Keys.updateCheck] = restored.updateCheck
            p[Keys.moments] = MomentCodec.encode(restored.moments)
            p[Keys.showMoments] = restored.showMoments
            p[Keys.showLive] = restored.showLive
            p[Keys.showFacts] = restored.showFacts
            p[Keys.celebrate] = restored.celebrate
            p[Keys.slides] = PathListCodec.encode(restored.slides)
        }
    }

    /** Löscht alle Daten und das Foto, danach startet die Einrichtung neu. */
    suspend fun resetAll() {
        deletePhotos()
        withContext(Dispatchers.IO) {
            momentDir().listFiles()?.forEach { it.delete() }
            slideDir().listFiles()?.forEach { it.delete() }
        }
        context.dataStore.edit {
            it.clear()
        }
        // Mit allen Daten verschwindet auch die Kopplung mit dem anderen Handy und die automatische Sicherung
        PairingStore.clear(context)
        AutoBackup.disable(context)
    }

    suspend fun setNotifications(enabled: Boolean) = context.dataStore.edit {
        it[Keys.notify] = enabled
    }

    suspend fun setNotifyTime(hour: Int, minute: Int) = context.dataStore.edit {
        it[Keys.notifyHour] = hour
        it[Keys.notifyMinute] = minute
    }

    suspend fun setPreset(index: Int) {
        deletePhotos()
        context.dataStore.edit {
            it.remove(Keys.photo)
            it[Keys.preset] = index
        }
    }

    suspend fun resetPhoto() = setPreset(0)

    /** Kopiert das gewählte Bild in den privaten App-Speicher. */
    suspend fun setPhoto(uri: Uri) {
        val target = withContext(Dispatchers.IO) {
            deletePhotos()
            // Neuer Dateiname pro Foto, damit der Bild-Cache sicher aktualisiert
            val file = File(photoDir(), "photo_${System.currentTimeMillis()}.jpg")
            if (!PhotoShrink.copy(context, uri, file)) return@withContext null
            file
        } ?: return
        context.dataStore.edit {
            it[Keys.photo] = target.absolutePath
            // Neues Foto startet mit dem Standard-Ausschnitt
            it.remove(Keys.focusX)
            it.remove(Keys.focusY)
            it.remove(Keys.zoom)
        }
    }

    suspend fun setPhotoFrame(focusX: Float, focusY: Float, zoom: Float) = context.dataStore.edit {
        it[Keys.focusX] = focusX
        it[Keys.focusY] = focusY
        it[Keys.zoom] = zoom
    }

    suspend fun setOnboardingDone(done: Boolean) = context.dataStore.edit {
        it[Keys.onboardingDone] = done
        // Wer gerade erst eingerichtet hat, braucht keine Neuigkeiten zu dieser Version
        if (done && it[Keys.changelogSeen] == null) it[Keys.changelogSeen] = BuildConfig.VERSION_CODE
    }

    suspend fun setChangelogSeen(code: Int) = context.dataStore.edit { it[Keys.changelogSeen] = code }

    suspend fun setShowChangelog(on: Boolean) = context.dataStore.edit { it[Keys.showChangelog] = on }

    suspend fun setUpdateCheck(enabled: Boolean) = context.dataStore.edit { it[Keys.updateCheck] = enabled }

    suspend fun lastUpdateCheck(): Long = context.dataStore.data.first()[Keys.lastUpdateCheck] ?: 0L

    /** Speichert das Ergebnis einer Update-Suche; [version] null = aktuell. */
    suspend fun setUpdateResult(version: String?, url: String?, apkUrl: String? = null) = context.dataStore.edit {
        it[Keys.lastUpdateCheck] = System.currentTimeMillis()
        if (version == null || url == null) {
            it.remove(Keys.updateVersion)
            it.remove(Keys.updateUrl)
            it.remove(Keys.updateApkUrl)
        } else {
            it[Keys.updateVersion] = version
            it[Keys.updateUrl] = url
            if (apkUrl != null) it[Keys.updateApkUrl] = apkUrl else it.remove(Keys.updateApkUrl)
        }
    }

    /** true, wenn für [version] noch keine Update-Mitteilung gezeigt wurde (und merkt es sich). */
    suspend fun markUpdateNotified(version: String): Boolean {
        var fresh = false
        context.dataStore.edit {
            if (it[Keys.updateNotified] != version) {
                it[Keys.updateNotified] = version
                fresh = true
            }
        }
        return fresh
    }

    suspend fun setRemindBefore(days: Int) = context.dataStore.edit { it[Keys.remindBefore] = days }

    suspend fun setFont(id: String) = context.dataStore.edit { it[Keys.font] = id }

    suspend fun setHomePage(page: Int) = context.dataStore.edit { it[Keys.homePage] = page }

    /** true, wenn für [day] noch keine Mitteilung verschickt wurde (und merkt ihn sich). */
    suspend fun markNotified(day: LocalDate): Boolean {
        var fresh = false
        context.dataStore.edit {
            if ((it[Keys.lastNotified] ?: Long.MIN_VALUE) < day.toEpochDay()) {
                it[Keys.lastNotified] = day.toEpochDay()
                fresh = true
            }
        }
        return fresh
    }

    private fun photoDir() = File(context.filesDir, "photos").apply { mkdirs() }

    /** Verkleinert große Fotos aus älteren Versionen, aus Sicherungen oder vom Abgleich */
    suspend fun shrinkPhotos() = withContext(Dispatchers.IO) {
        PhotoShrink.shrinkAll(listOf(photoDir(), slideDir(), momentDir()))
    }

    private suspend fun deletePhotos() = withContext(Dispatchers.IO) {
        photoDir().listFiles()?.forEach { it.delete() }
    }
}
