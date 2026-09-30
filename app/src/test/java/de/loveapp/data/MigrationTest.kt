package de.loveapp.data

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MigrationTest {

    @Test
    fun freshInstallStaysEmpty() = runBlocking {
        val result = EarlyVersionMigration.migrate(emptyPreferences())
        assertNull(result[Keys.name1])
        assertNull(result[Keys.start])
        assertEquals(2, result[Keys.schema])
    }

    @Test
    fun earlyInstallKeepsItsValues() = runBlocking {
        val old = preferencesOf(Keys.onboardingDone to true, Keys.name2 to "Luna")
        assertTrue(EarlyVersionMigration.shouldMigrate(old))
        val result = EarlyVersionMigration.migrate(old)
        assertEquals("Alex", result[Keys.name1])
        assertEquals("Luna", result[Keys.name2])
        assertEquals(LocalDate.of(2025, 3, 15).toEpochDay(), result[Keys.start])
    }

    @Test
    fun runsOnlyOnce() = runBlocking {
        val migrated = EarlyVersionMigration.migrate(preferencesOf(Keys.notify to true))
        assertEquals(false, EarlyVersionMigration.shouldMigrate(migrated))
    }
}
