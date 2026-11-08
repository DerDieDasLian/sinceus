package app.sinceus.ui

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Vorschau beim Zurückwischen (Predictive Back) für Seiten innerhalb der App.
 *
 * Während man vom Rand wischt, wird die Seite etwas kleiner, bekommt runde Ecken
 * und rückt ein Stück mit. Lässt man los, geht es zurück; bricht man ab,
 * wächst die Seite wieder auf volle Größe.
 */
@Stable
class BackPreview internal constructor() {
    internal val progress = Animatable(0f)
    internal var fromLeft by mutableStateOf(true)
}

/**
 * Ersetzt BackHandler und merkt sich, wie weit gerade gewischt wird.
 *
 * @param stays true, wenn die Seite nach dem Zurück sichtbar bleibt (z. B. eine
 *   Unterseite der Einstellungen wird geschlossen oder es erscheint eine Rückfrage).
 *   Dann springt die Vorschau danach zurück auf volle Größe. Bei false bleibt die
 *   Seite verkleinert, während sie ausgeblendet wird.
 */
@Composable
fun rememberBackPreview(enabled: Boolean = true, stays: Boolean = false, onBack: () -> Unit): BackPreview {
    val preview = remember { BackPreview() }
    val scope = rememberCoroutineScope()
    val currentOnBack by rememberUpdatedState(onBack)
    val currentStays by rememberUpdatedState(stays)
    PredictiveBackHandler(enabled) { events ->
        try {
            events.collect { event ->
                preview.fromLeft = event.swipeEdge == BackEventCompat.EDGE_LEFT
                preview.progress.snapTo(event.progress)
            }
            val keep = currentStays
            currentOnBack()
            if (keep) preview.progress.snapTo(0f)
        } catch (e: CancellationException) {
            // Geste abgebrochen: sanft zurück auf volle Größe
            scope.launch { preview.progress.animateTo(0f, tween(200)) }
            throw e
        }
    }
    return preview
}

/** Zeichnet die Seite passend zur laufenden Zurück-Geste. */
@Composable
fun Modifier.backPreview(preview: BackPreview): Modifier {
    val background = MaterialTheme.colorScheme.background
    return this
        .drawBehind {
            val p = preview.progress.value
            if (p > 0f) drawRect(Color.Black.copy(alpha = 0.3f * p))
        }
        .graphicsLayer {
            val p = preview.progress.value
            val scale = 1f - 0.1f * p
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin.Center
            translationX = (if (preview.fromLeft) 1f else -1f) * 8.dp.toPx() * p
            shape = RoundedCornerShape(28.dp * p)
            clip = p > 0f
        }
        .background(background)
}
