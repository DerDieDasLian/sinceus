package app.sinceus.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.sinceus.notify.Notifier
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

/** Poly-Modus: mehrere Menschen und Beziehungen */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "de")
class PeopleTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private val poly = LoveSettings(
        people = listOf(Person("a", "Alex"), Person("s", "Sam", "they/them", LocalDate.of(1998, 8, 20)), Person("k", "Kim")),
        relationships = listOf(
            Relationship("r1", listOf("a", "s"), LocalDate.of(2025, 3, 15), LocalTime.of(20, 0)),
            Relationship("r2", listOf("a", "k"), LocalDate.of(2024, 6, 1), label = "Nesting", notify = false),
        ),
        selected = ALL_RELATIONSHIPS,
        widgetRelationship = "r2",
        discreet = true,
        onboardingDone = true,
        moments = listOf(Moment("m", "Erstes Date", LocalDate.of(2024, 5, 1), relationshipId = "r2")),
    )

    @After
    fun cleanUp(): Unit = runBlocking { LoveRepository(context).resetAll() }

    @Test
    fun namesAndInitials() {
        assertEquals("Alex & Sam", Names.join(listOf("Alex", "Sam")))
        assertEquals("Alex, Sam & Kim", Names.join(listOf("Alex", "Sam", "Kim")))
        assertEquals("Ö.", Names.initial("öznur"))
        assertEquals("", Names.initial("  "))
    }

    @Test
    fun derivedValues() {
        assertTrue(poly.showAll)
        assertTrue(poly.isPoly)
        assertEquals("r1", poly.relationship.id)
        assertEquals(listOf("A.", "K."), poly.forWidget().shownNames())
        assertEquals(LocalDate.of(2024, 6, 1), poly.forWidget().startDate)
        assertEquals(1, poly.visibleMoments.size)
        assertTrue(poly.copy(selected = "r1").visibleMoments.isEmpty())
        assertTrue(!LoveSettings.couple("A", "B", LocalDate.of(2025, 1, 1)).isPoly)
    }

    @Test
    fun codecKeepsEverything() {
        assertEquals(poly.people, PeopleCodec.decodePeople(PeopleCodec.encodePeople(poly.people)))
        assertEquals(poly.relationships, PeopleCodec.decodeRelationships(PeopleCodec.encodeRelationships(poly.relationships)))
        assertNull(PeopleCodec.decodePeople(null))
        assertEquals(poly.moments, MomentCodec.decode(MomentCodec.encode(poly.moments)))
    }

    @Test
    fun sanitizeDropsBrokenRelationships() {
        val (people, rels) = PeopleCodec.sanitize(
            poly.people,
            listOf(Relationship("x", listOf("a", "unknown"), LocalDate.of(2025, 1, 1))),
            LocalDate.of(2020, 1, 1),
        )
        assertEquals(3, people.size)
        // Keine gültige Beziehung übrig: alle zusammen ab dem Ersatzdatum
        assertEquals(listOf("a", "s", "k"), rels.single().members)
        assertEquals(LocalDate.of(2020, 1, 1), rels.single().startDate)
    }

    @Test
    fun backupKeepsPolyData() {
        val decoded = BackupCodec.decode(BackupCodec.encode(poly))
        assertEquals(poly, decoded)
    }

    @Test
    fun oldBackupBecomesCouple() {
        val s = BackupCodec.decode(
            """{"app":"app.sinceus","format":1,"name1":"Alex","name2":"Sam","startDate":${LocalDate.of(2025, 3, 15).toEpochDay()},"startTime":null}""",
        )
        assertEquals("Alex & Sam", s.names)
        assertEquals(LocalDate.of(2025, 3, 15), s.startDate)
        assertEquals(1, s.relationships.size)
    }

    @Test
    fun repositoryHandlesPeopleAndRelationships(): Unit = runBlocking {
        val repo = LoveRepository(context)
        repo.resetAll()
        repo.setNames(listOf("Alex", "Sam", "Kim"))
        repo.setStartDate(LocalDate.of(2025, 3, 15))
        var s = repo.current()
        assertEquals("Alex, Sam & Kim", s.names)
        assertEquals(LocalDate.of(2025, 3, 15), s.startDate)

        val kim = s.people[2]
        repo.saveRelationship(Relationship("r2", listOf(s.people[0].id, kim.id), LocalDate.of(2024, 6, 1)))
        repo.savePerson(kim.copy(pronouns = "sie/ihr"))
        s = repo.current()
        assertEquals(2, s.relationships.size)
        assertEquals("sie/ihr", s.person(kim.id)?.pronouns)

        // Ohne Kim bleibt nur die erste Beziehung (Alex & Sam)
        repo.deletePerson(kim.id)
        s = repo.current()
        assertEquals(1, s.relationships.size)
        assertEquals("Alex & Sam", s.names)
    }

    @Test
    fun notificationNamesBothRelationships() {
        // Beide Beziehungen haben am selben Tag einen Monatstag
        val s = poly.copy(
            relationships = listOf(
                Relationship("r1", listOf("a", "s"), LocalDate.of(2025, 3, 15)),
                Relationship("r2", listOf("a", "k"), LocalDate.of(2025, 1, 15)),
            ),
            discreet = false,
            moments = emptyList(),
        )
        val (title, lines) = Notifier.dueToday(context, s, LocalDate.of(2025, 5, 15))!!
        assertEquals("Alex, Sam & Kim", title)
        assertEquals(2, lines.size)
        assertTrue(lines[0].startsWith("Alex & Sam: "))
        assertTrue(lines[1].startsWith("Alex & Kim: "))

        // Ohne Mitteilungen für die zweite Beziehung bleibt nur die erste
        val one = s.copy(relationships = listOf(s.relationships[0], s.relationships[1].copy(notify = false)))
        assertEquals("Alex & Sam", Notifier.dueToday(context, one, LocalDate.of(2025, 5, 15))!!.first)
        assertNull(Notifier.dueToday(context, s, LocalDate.of(2025, 5, 16)))
    }

    @Test
    fun birthdays() {
        val leap = Person("l", "Lou", birthday = LocalDate.of(2000, 2, 29))
        val sam = poly.people[1]
        // Am 29.02. Geborene feiern in anderen Jahren am 28.02.
        assertEquals(LocalDate.of(2025, 2, 28), BirthdayMath.next(leap.birthday!!, LocalDate.of(2025, 1, 1)))
        assertEquals(LocalDate.of(2028, 2, 29), BirthdayMath.next(leap.birthday!!, LocalDate.of(2028, 1, 1)))
        val today = BirthdayMath.milestonesOn(listOf(leap, sam), LocalDate.of(2025, 2, 28)).single()
        assertEquals(MilestoneKind.BIRTHDAY, today.kind)
        assertEquals("Lou", today.title)
        assertEquals(25L, today.value)
        // Am Tag der Geburt selbst und ohne Datum gibt es nichts zu feiern
        assertTrue(BirthdayMath.milestonesOn(listOf(leap, poly.people[0]), LocalDate.of(2000, 2, 29)).isEmpty())

        val next = BirthdayMath.upcoming(listOf(leap, sam, poly.people[0]), LocalDate.of(2025, 3, 1))
        assertEquals(listOf("Sam", "Lou"), next.map { it.title })
        assertEquals(LocalDate.of(2025, 8, 20), next[0].date)

        // Mitteilung, im diskreten Modus nur mit Anfangsbuchstaben
        val s = poly.copy(moments = emptyList())
        val (_, lines) = Notifier.dueToday(context, s, LocalDate.of(2025, 8, 20))!!
        assertEquals(listOf("Heute hat S. Geburtstag."), lines)
        assertEquals("Geburtstag von Sam", Texts.milestoneTitle(context, next[0]))
        // Keine Mitteilung, wenn alle Beziehungen dieses Menschen stumm sind
        val quiet = s.copy(relationships = s.relationships.map { it.copy(notify = false) })
        assertNull(Notifier.dueToday(context, quiet, LocalDate.of(2025, 8, 20)))
    }
}
