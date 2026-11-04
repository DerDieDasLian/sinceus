package app.sinceus.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CacheCleanupTest {
    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun deletesOnlyFilesOlderThanADay() {
        val now = 10L * 24 * 60 * 60 * 1000
        val old = temp.newFile("alt.sinceus").apply { setLastModified(now - 2L * 24 * 60 * 60 * 1000) }
        val fresh = temp.newFile("neu.sinceus").apply { setLastModified(now - 60_000) }
        assertEquals(1, CacheCleanup.clean(temp.root, now))
        assertFalse(old.exists())
        assertTrue(fresh.exists())
    }

    @Test
    fun missingFolderIsFine() {
        assertEquals(0, CacheCleanup.clean(temp.root.resolve("gibt-es-nicht"), 0))
    }
}
