package app.sinceus.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

// Robolectric wegen org.json und DataStore
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BackupTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private val sample = LoveSettings(
        name1 = "Alex",
        name2 = "Sam",
        startDate = LocalDate.of(2025, 3, 15),
        startTime = LocalTime.of(20, 30),
        photoPath = "/data/x/files/photos/photo_1.jpg",
        presetIndex = 2,
        notificationsEnabled = false,
        notifyHour = 8,
        notifyMinute = 15,
        focusX = 0.25f,
        focusY = -0.5f,
        zoom = 1.5f,
        onboardingDone = true,
        homePage = 1,
        updateCheck = false,
        moments = listOf(Moment("1", "Erstes Date", LocalDate.of(2025, 2, 14), "Kino", "/data/x/files/moments/1_5.jpg")),
        showMoments = true,
        showLive = false,
        slides = listOf("/data/x/files/slides/slide_1_0.jpg"),
    )

    @After
    fun cleanUp(): Unit = runBlocking { LoveRepository(context).resetAll() }

    @Test
    fun jsonKeepsAllDataWithFileNamesOnly() {
        val expected = sample.copy(
            photoPath = "photo_1.jpg",
            moments = sample.moments.map { it.copy(photoPath = "1_5.jpg") },
            slides = listOf("slide_1_0.jpg"),
        )
        assertEquals(expected, BackupCodec.decode(BackupCodec.encode(sample)))
    }

    @Test
    fun rejectsOtherFiles() {
        assertThrows(Exception::class.java) { BackupCodec.decode("kein json") }
        assertThrows(Exception::class.java) { BackupCodec.decode("""{"app":"other","format":1,"name1":"A","name2":"B","startDate":1}""") }
        assertThrows(Exception::class.java) { BackupCodec.decode("""{"app":"app.sinceus","format":99,"name1":"A","name2":"B","startDate":1}""") }
    }

    @Test
    fun onlySimpleFileNamesAreAllowed() {
        assertTrue(Backup.isSafeName("photo_123.jpg"))
        assertFalse(Backup.isSafeName("../evil.jpg"))
        assertFalse(Backup.isSafeName(".."))
        assertFalse(Backup.isSafeName(".hidden"))
        assertFalse(Backup.isSafeName("a/b.jpg"))
    }

    @Test
    fun exportAndImportRestoreEverything(): Unit = runBlocking {
        val repo = LoveRepository(context)
        repo.resetAll()
        repo.setNames("Alex", "Sam")
        repo.setStartDate(LocalDate.of(2025, 3, 15))
        repo.saveMoment(Moment("1", "Erstes Date", LocalDate.of(2025, 2, 14), "Kino"), null, false)
        val file = ByteArrayOutputStream()
        repo.exportBackup(file)

        repo.resetAll()
        assertEquals("", repo.current().name1)

        repo.importBackup(ByteArrayInputStream(file.toByteArray()))
        val s = repo.current()
        assertEquals("Alex & Sam", s.names)
        assertEquals(LocalDate.of(2025, 3, 15), s.startDate)
        assertEquals(listOf("Erstes Date"), s.moments.map { it.title })
        assertTrue(s.onboardingDone)
    }

    @Test
    fun importTakesPhotosButIgnoresPathsOutside(): Unit = runBlocking {
        val zip = ByteArrayOutputStream()
        ZipOutputStream(zip).use { z ->
            z.putNextEntry(ZipEntry(Backup.JSON))
            z.write(BackupCodec.encode(sample).toByteArray())
            z.closeEntry()
            z.putNextEntry(ZipEntry("photos/photo_1.jpg"))
            z.write(byteArrayOf(1, 2, 3))
            z.closeEntry()
            z.putNextEntry(ZipEntry("photos/../../evil.txt"))
            z.write(byteArrayOf(9))
            z.closeEntry()
        }
        val repo = LoveRepository(context)
        repo.importBackup(ByteArrayInputStream(zip.toByteArray()))

        val s = repo.current()
        assertEquals(File(File(context.filesDir, "photos"), "photo_1.jpg").absolutePath, s.photoPath)
        // Fotos, die in der Datei fehlen, fallen weg
        assertTrue(s.slides.isEmpty())
        assertNull(s.moments.single().photoPath)
        assertFalse(File(context.filesDir.parentFile, "evil.txt").exists())
        assertFalse(File(context.cacheDir.parentFile, "evil.txt").exists())
    }

    @Test
    fun brokenFileChangesNothing(): Unit = runBlocking {
        val repo = LoveRepository(context)
        repo.resetAll()
        repo.setNames("Alex", "Sam")
        assertThrows(Exception::class.java) {
            runBlocking { repo.importBackup(ByteArrayInputStream("kein zip".toByteArray())) }
        }
        assertEquals("Alex & Sam", repo.current().names)
    }
}
