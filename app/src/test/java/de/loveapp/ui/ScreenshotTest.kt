package de.loveapp.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.drawToBitmap
import de.loveapp.data.LoveSettings
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

/**
 * Rendert die Screens als PNG nach app/build/screenshots, um das Design ohne
 * Geraet pruefen zu koennen: ./gradlew testDebugUnitTest
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val settings = LoveSettings()
    private val today = LocalDate.of(2026, 9, 30)

    @Test
    fun home() = shot("home") { HomeScreen(settings, today) {} }

    @Test
    fun homeDark() = shot("home_dark", dark = true) { HomeScreen(settings.copy(presetIndex = 3), today) {} }

    @Test
    fun settings() = shot("settings") {
        SettingsScreen(settings, true, {}, { _, _ -> }, {}, {}, {}, {}, {}, { _, _ -> }, {}, {})
    }

    private fun shot(name: String, dark: Boolean = false, content: @androidx.compose.runtime.Composable () -> Unit) {
        if (dark) org.robolectric.RuntimeEnvironment.setQualifiers("+night")
        compose.setContent { LoveTheme(content) }
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        val bitmap = compose.activity.window.decorView.drawToBitmap()
        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
