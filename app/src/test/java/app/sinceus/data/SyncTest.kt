package app.sinceus.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import kotlin.random.Random

// Robolectric wegen org.json, Base64 und DataStore
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SyncTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @After
    fun cleanUp(): Unit = runBlocking { LoveRepository(context).resetAll() }

    private val key = ByteArray(32) { it.toByte() }

    private fun encrypt(data: ByteArray, k: ByteArray = key): ByteArray =
        ByteArrayOutputStream().also { SyncCrypto.encrypt(ByteArrayInputStream(data), it, k) }.toByteArray()

    private fun decrypt(data: ByteArray, k: ByteArray = key): ByteArray =
        ByteArrayOutputStream().also { SyncCrypto.decrypt(ByteArrayInputStream(data), it, k) }.toByteArray()

    @Test
    fun cryptoRoundtripForAllSizes() {
        // Leer, klein, genau ein Block, knapp darüber, mehrere Blöcke
        listOf(0, 1, 1000, 64 * 1024, 64 * 1024 + 1, 200_000).forEach { size ->
            val data = Random(size).nextBytes(size)
            assertArrayEquals("size $size", data, decrypt(encrypt(data)))
        }
    }

    @Test
    fun wrongKeyIsRejected() {
        val sealed = encrypt("hallo".toByteArray())
        val other = ByteArray(32) { 7 }
        assertFalse(SyncCrypto.belongsTo(sealed.copyOf(32), other))
        assertTrue(SyncCrypto.belongsTo(sealed.copyOf(32), key))
        assertThrows(Exception::class.java) { decrypt(sealed, other) }
    }

    @Test
    fun changedOrTruncatedFileIsRejected() {
        val sealed = encrypt(Random(1).nextBytes(150_000))
        // Ein Bit im letzten Block kippen
        val changed = sealed.copyOf().also { it[it.size - 5] = (it[it.size - 5].toInt() xor 1).toByte() }
        assertThrows(Exception::class.java) { decrypt(changed) }
        // Letzten Block abschneiden: Die Datei endet nach einem „nicht letzten“ Block
        val firstBlocks = sealed.size - (150_000 - 2 * 64 * 1024 + 16) - 4
        assertThrows(Exception::class.java) { decrypt(sealed.copyOf(firstBlocks)) }
        // Irgendwo mittendrin abschneiden
        assertThrows(Exception::class.java) { decrypt(sealed.copyOf(sealed.size / 2)) }
        // Etwas anhängen
        assertThrows(Exception::class.java) { decrypt(sealed + byteArrayOf(1)) }
    }

    @Test
    fun qrCodeCarriesTheKey() {
        val p = Pairing.create("a")
        assertEquals(32, p.key.size)
        assertArrayEquals(p.key, Pairing.keyFromQr(Pairing.qrText(p)))
        assertNull(Pairing.keyFromQr("https://example.com"))
        assertNull(Pairing.keyFromQr("sinceus:pair:1:abc"))
        assertNull(Pairing.keyFromQr(null))
    }

    @Test
    fun codecRoundtrip() {
        val data = SyncData(
            people = listOf(Person("a", "Alex"), Person("b", "Sam", "they/them")),
            relationships = listOf(Relationship("r", listOf("a", "b"), LocalDate.of(2025, 3, 15))),
            moments = listOf(
                Moment("m1", "Erstes Date", LocalDate.of(2025, 3, 15), photoPath = "/data/x/moments/m1.jpg", updatedAt = 5, addedBy = "a"),
            ),
            deleted = mapOf("old" to 42L),
            from = "b",
        )
        val back = SyncCodec.decode(SyncCodec.encode(data))
        assertEquals(data.people, back.people)
        assertEquals(data.relationships, back.relationships)
        // Nur der Dateiname des Fotos reist mit
        assertEquals(data.moments[0].copy(photoPath = "m1.jpg"), back.moments[0])
        assertEquals(data.deleted, back.deleted)
        assertEquals("b", back.from)
        assertThrows(Exception::class.java) { SyncCodec.decode("""{"app":"other"}""") }
    }

    private fun moment(id: String, updated: Long, title: String = id) =
        Moment(id, title, LocalDate.of(2025, 4, 1), updatedAt = updated)

    @Test
    fun newerChangeWinsAndDeletionsStay() {
        val now = System.currentTimeMillis()
        val local = listOf(moment("same", now - 10), moment("mine-newer", now, "hier"), moment("theirs-newer", now - 100, "alt"), moment("gone", now - 50))
        val remote = SyncData(
            people = listOf(Person("a", "Alex"), Person("k", "Kim")),
            relationships = emptyList(),
            moments = listOf(
                moment("same", now - 10),
                moment("mine-newer", now - 100, "dort"),
                moment("theirs-newer", now, "neu"),
                moment("new", now),
                moment("deleted-here", now - 100),
            ),
            deleted = mapOf("gone" to now - 1),
            from = "k",
        )
        val result = SyncMerge.merge(
            listOf(Person("a", "Alex"), Person("b", "Sam")),
            emptyList(),
            local,
            mapOf("deleted-here" to now - 5),
            remote,
        )
        val byId = result.moments.associateBy { it.id }
        assertEquals("hier", byId["mine-newer"]?.title)
        assertEquals("neu", byId["theirs-newer"]?.title)
        assertNotNull(byId["new"])
        assertNull(byId["gone"])
        assertNull(byId["deleted-here"])
        assertEquals(setOf("theirs-newer", "new"), result.taken.map { it.id }.toSet())
        assertEquals(1, result.added)
        assertEquals(1, result.changed)
        assertEquals(1, result.removed)
        // Fehlende Menschen kommen dazu, bestehende bleiben
        assertEquals(listOf("a", "b", "k"), result.people.map { it.id })
        assertTrue("gone" in result.deleted && "deleted-here" in result.deleted)
    }

    @Test
    fun repositoryExportsAndImports(): Unit = runBlocking {
        val repo = LoveRepository(context)
        repo.resetAll()
        repo.setNames(listOf("Alex", "Sam"))
        repo.saveMoment(Moment("m1", "Erstes Date", LocalDate.of(2025, 3, 15)), null, false)
        val pairing = Pairing(key, null, 1)
        val file = ByteArrayOutputStream().also { repo.exportSync(it, pairing) }.toByteArray()
        assertTrue(SyncCrypto.belongsTo(file.copyOf(32), key))

        // Moment hier löschen und neu anlegen lassen: Der Löschmerker gewinnt beim Zurückspielen
        repo.deleteMoment("m1")
        repo.importSync(ByteArrayInputStream(file), pairing)
        assertTrue(repo.current().moments.isEmpty())

        // Auf einem frischen Handy kommt der Moment an
        repo.resetAll()
        repo.setNames(listOf("Alex", "Sam"))
        val result = repo.importSync(ByteArrayInputStream(file), pairing)
        assertEquals(1, result.added)
        assertEquals("Erstes Date", repo.current().moments.single().title)

        // Fremder Schlüssel: nichts ändert sich
        assertThrows(Exception::class.java) {
            runBlocking { repo.importSync(ByteArrayInputStream(file), Pairing(ByteArray(32), null, 1)) }
        }
        assertEquals(1, repo.current().moments.size)
    }

    @Test
    fun missingBirthdayComesFromOtherPhone() {
        val birthday = LocalDate.of(1998, 8, 20)
        val remote = SyncData(
            people = listOf(Person("a", "Alex", birthday = birthday), Person("b", "Samuel", birthday = LocalDate.of(1990, 1, 1))),
            relationships = emptyList(),
            moments = emptyList(),
            deleted = emptyMap(),
            from = "b",
        )
        val sam = LocalDate.of(1999, 9, 9)
        val result = SyncMerge.merge(listOf(Person("a", "Alex"), Person("b", "Sam", birthday = sam)), emptyList(), emptyList(), emptyMap(), remote)
        assertEquals(birthday, result.people[0].birthday)
        // Was hier schon eingetragen ist, bleibt
        assertEquals("Sam", result.people[1].name)
        assertEquals(sam, result.people[1].birthday)
        assertEquals(remote.people, SyncCodec.decode(SyncCodec.encode(remote)).people)
    }
}
