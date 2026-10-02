package app.sinceus

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockTest {
    @Test
    fun locksAfterGraceTime() {
        assertFalse(AppLock.shouldLock(leftAt = 1_000L, now = 1_000L + AppLock.GRACE_MS - 1))
        assertTrue(AppLock.shouldLock(leftAt = 1_000L, now = 1_000L + AppLock.GRACE_MS))
    }
}
