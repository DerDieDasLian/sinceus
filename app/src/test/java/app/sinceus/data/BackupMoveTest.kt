package app.sinceus.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BackupMoveTest {
    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun `moves the file and replaces an existing one`() {
        val source = temp.newFile("new.jpg").apply { writeText("new") }
        val target = File(temp.newFolder("photos"), "photo.jpg").apply { writeText("old") }
        Backup.move(source, target)
        assertEquals("new", target.readText())
        assertFalse(source.exists())
    }

    @Test
    fun `creates the target folder when it is missing`() {
        val source = temp.newFile("new.jpg").apply { writeText("new") }
        val target = File(File(temp.root, "moments"), "photo.jpg")
        Backup.move(source, target)
        assertEquals("new", target.readText())
    }
}
