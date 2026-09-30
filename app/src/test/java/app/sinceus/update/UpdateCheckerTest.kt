package app.sinceus.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {
    @Test
    fun comparesVersionsNumerically() {
        assertTrue(UpdateChecker.isNewer("2.10.0", "2.9.3"))
        assertTrue(UpdateChecker.isNewer("2.0.1", "2.0"))
        assertTrue(UpdateChecker.isNewer("3", "2.99"))
        assertFalse(UpdateChecker.isNewer("2.0.1", "2.0.1"))
        assertFalse(UpdateChecker.isNewer("2.0", "2.0.1"))
        assertFalse(UpdateChecker.isNewer("1.9", "2.0"))
    }
}
