package app.sinceus.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.drawToBitmap
import app.sinceus.data.LoveSettings
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
 * Gerät prüfen zu können: ./gradlew testDebugUnitTest
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// 1233 x 2460 px: Seitenverhältnis max. 2:1, wie vom Play Store verlangt
@Config(sdk = [35], qualifiers = "w411dp-h820dp-xxhdpi")
class ScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val settings = LoveSettings(
        name1 = "Alex",
        name2 = "Sam",
        startDate = LocalDate.of(2025, 3, 15),
        onboardingDone = true,
    )
    private val today = LocalDate.of(2025, 8, 15)

    @Test
    fun home() = shot("home") { HomeScreen(settings, today, {}, {}) }

    @Test
    fun homeGerman() = shot("home", lang = "de") { HomeScreen(settings, today, {}, {}) }

    @Test
    fun liveGerman() = shot("live", lang = "de") {
        LiveScreen(settings, active = false, now = java.time.LocalDateTime.of(2025, 8, 15, 12, 46, 33))
    }

    @Test
    fun settingsGerman() = shot("settings", lang = "de") {
        SettingsScreen(settings, true, {}, { _, _ -> }, {}, {}, {}, {}, {}, { _, _ -> }, {}, {}, {}, {}, {}, {})
    }

    @Test
    fun homeDark() = shot("home_dark", dark = true) { HomeScreen(settings.copy(presetIndex = 3), today, {}, {}) }

    @Test
    fun settings() = shot("settings") {
        SettingsScreen(settings, true, {}, { _, _ -> }, {}, {}, {}, {}, {}, { _, _ -> }, {}, {}, {}, {}, {}, {})
    }

    @Test
    fun live() = shot("live") {
        LiveScreen(settings, active = false, now = java.time.LocalDateTime.of(2025, 8, 15, 12, 46, 33))
    }

    @Test
    fun onboardingWelcome() = onboarding(0)

    @Test
    fun onboardingDarkGerman() = shot("onboarding_1_dark", dark = true, lang = "de") {
        OnboardingScreen(
            settings, false, { _, _ -> }, {}, {}, {}, {}, {}, { _, _ -> }, {}, {},
            step = 1, onStep = {},
        )
    }

    @Test
    fun onboardingNames() = onboarding(1)

    @Test
    fun onboardingDate() = onboarding(2)

    @Test
    fun onboardingPhoto() = onboarding(3)

    @Test
    fun onboardingNotify() = onboarding(4)

    private val withMoments = settings.copy(
        moments = listOf(
            app.sinceus.data.Moment("1", "Kennengelernt", LocalDate.of(2025, 1, 20), "Auf der Geburtstagsfeier von Mia"),
            app.sinceus.data.Moment("2", "Erstes Date", LocalDate.of(2025, 2, 14), "Kino und danach Pizza"),
            app.sinceus.data.Moment("3", "Erster Urlaub", LocalDate.of(2025, 7, 5), "Eine Woche an der Ostsee"),
        ),
    )

    @Test
    fun momentsEmpty() = shot("moments_empty", lang = "de") { MomentsScreen(settings, today, {}, {}) }

    @Test
    fun moments() = shot("moments", lang = "de") { MomentsScreen(withMoments, today, {}, {}) }

    @Test
    fun momentsEnglish() = shot("moments") {
        MomentsScreen(
            settings.copy(
                moments = listOf(
                    app.sinceus.data.Moment("1", "How we met", LocalDate.of(2025, 1, 20), "At Mia's birthday party"),
                    app.sinceus.data.Moment("2", "First date", LocalDate.of(2025, 2, 14), "Movies and pizza afterwards"),
                    app.sinceus.data.Moment("3", "First trip", LocalDate.of(2025, 7, 5), "A week by the sea"),
                ),
            ),
            today,
            {},
            {},
        )
    }

    @Test
    fun momentEditor()= shot("moment_editor", lang = "de") {
        MomentEditorScreen(withMoments.moments[1], null, today, {}, { _, _, _ -> }, {})
    }

    @Test
    fun homeWithMomentsTab() = shot("home_moments_tab", lang = "de") { HomeScreen(withMoments, today, {}, {}) }

    @Test
    fun editor() = shot("editor") { PhotoEditorScreen(settings, {}, {}, { _, _, _ -> }) }

    @Test
    fun onboardingPhotoGerman() = shot("onboarding_3", lang = "de") {
        OnboardingScreen(
            settings, false, { _, _ -> }, {}, {}, {}, {}, {}, { _, _ -> }, {}, {},
            step = 3, onStep = {},
        )
    }

    private fun onboarding(step: Int) = shot("onboarding_$step") {
        OnboardingScreen(
            settings, false, { _, _ -> }, {}, {}, {}, {}, {}, { _, _ -> }, {}, {},
            step = step, onStep = {},
        )
    }

    private fun shot(
        name: String,
        dark: Boolean = false,
        lang: String = "en",
        content: @androidx.compose.runtime.Composable () -> Unit,
    ) {
        java.util.Locale.setDefault(if (lang == "de") java.util.Locale.GERMANY else java.util.Locale.US)
        org.robolectric.RuntimeEnvironment.setQualifiers("+$lang")
        if (dark) org.robolectric.RuntimeEnvironment.setQualifiers("+night")
        compose.setContent { LoveTheme(content) }
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        val bitmap = compose.activity.window.decorView.drawToBitmap()
        val dir = File("build/screenshots/$lang").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
