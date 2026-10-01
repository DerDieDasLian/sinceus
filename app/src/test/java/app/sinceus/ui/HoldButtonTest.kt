package app.sinceus.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** "Alle Daten löschen" darf nur nach langem Drücken auslösen */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HoldButtonTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var confirmed = false

    private fun show() = compose.setContent { HoldButton("Hold") { confirmed = true } }

    @Test
    fun shortPressDoesNothing() {
        show()
        compose.onNodeWithTag("hold_button").performTouchInput {
            down(center)
            advanceEventTime(1000)
            up()
        }
        compose.mainClock.advanceTimeBy(HOLD_MS + 1000L)
        compose.waitForIdle()
        assertFalse(confirmed)
    }

    @Test
    fun longPressConfirms() {
        show()
        compose.onNodeWithTag("hold_button").performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(HOLD_MS + 500L)
        compose.waitForIdle()
        assertTrue(confirmed)
        compose.onNodeWithTag("hold_button").performTouchInput { up() }
    }
}
