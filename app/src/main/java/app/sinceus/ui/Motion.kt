package app.sinceus.ui

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState

/**
 * false, wenn am Handy unter Bedienungshilfen "Animationen entfernen" eingeschaltet ist
 * (Animationsfaktor 0). Dann bleiben Herzen still und es gibt kein Konfetti.
 */
@Composable
internal fun animationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
}

/**
 * true, solange die App sichtbar ist. Zähler, Diashow und Uhren laufen nur dann weiter,
 * im Hintergrund würden sie nur Akku kosten.
 */
@Composable
internal fun appVisible(): Boolean {
    val state by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    return state.isAtLeast(Lifecycle.State.STARTED)
}
