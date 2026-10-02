package app.sinceus.ui

import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Wurde in diesem App-Start schon gefeiert? Dann nicht bei jedem Wechsel der Seite erneut */
private var celebratedThisLaunch = false

private val ConfettiColors = listOf(
    Color(0xFFFF8FA3), Color(0xFFFFB2B9), Color(0xFFFFE3E7), Color(0xFFE8506E), Color(0xFFFFFFFF), Color(0xFF8C2A45),
)

private val HeartPath = PathParser()
    .parsePathString(
        "M12,21.35l-1.45,-1.32C5.4,15.36 2,12.28 2,8.5 2,5.42 4.42,3 7.5,3c1.74,0 3.41,0.81 4.5,2.09" +
            "C13.09,3.81 14.76,3 16.5,3 19.58,3 22,5.42 22,8.5c0,3.78 -3.4,6.86 -8.55,11.54L12,21.35z",
    )
    .toPath()

private class Piece(
    val angle: Float,
    val speed: Float,
    val spin: Float,
    val turn: Float,
    val width: Float,
    val height: Float,
    val color: Color,
    val heart: Boolean,
)

/**
 * Konfetti, das einmal aus der Mitte oben platzt und nach unten fällt (etwa drei Sekunden).
 * Läuft nur einmal pro App-Start und gar nicht, wenn am Handy Animationen ausgeschaltet sind.
 */
@Composable
internal fun Confetti(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val animationsOff = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    if (celebratedThisLaunch || animationsOff) return
    val pieces = remember {
        val random = Random(System.nanoTime())
        List(110) {
            Piece(
                angle = (-Math.PI + random.nextDouble() * Math.PI).toFloat(),
                speed = 0.45f + random.nextFloat() * 0.75f,
                spin = random.nextFloat() * 6f,
                turn = -8f + random.nextFloat() * 16f,
                width = 5f + random.nextFloat() * 5f,
                height = 9f + random.nextFloat() * 8f,
                color = ConfettiColors[random.nextInt(ConfettiColors.size)],
                heart = random.nextFloat() < 0.25f,
            )
        }
    }
    var time by remember { mutableFloatStateOf(0f) }
    val duration = 3.2f
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (time < duration) {
            withFrameNanos { time = (it - start) / 1_000_000_000f }
        }
        celebratedThisLaunch = true
    }
    if (time >= duration) return
    Canvas(modifier.fillMaxSize()) {
        val origin = Offset(size.width / 2, size.height * 0.32f)
        val fade = if (time > duration * 0.8f) 1f - (time - duration * 0.8f) / (duration * 0.2f) else 1f
        val gravity = size.height * 0.75f
        pieces.forEach { p ->
            val reach = size.width * p.speed
            val x = origin.x + cos(p.angle) * reach * time
            val y = origin.y + sin(p.angle) * reach * time + gravity * time * time
            translate(x, y) {
                rotate(p.spin * 57f + p.turn * time * 57f, Offset.Zero) {
                    if (p.heart) {
                        val s = 12f * density / 24f
                        scale(s, s, Offset.Zero) {
                            translate(-12f, -12f) { drawPath(HeartPath, p.color.copy(alpha = fade)) }
                        }
                    } else {
                        // Wackeln wie ein fallendes Papierstück
                        val h = p.height * density * abs(cos(time * 6f + p.spin)).coerceAtLeast(0.1f)
                        drawRect(
                            p.color.copy(alpha = fade),
                            topLeft = Offset(-p.width * density / 2, -h / 2),
                            size = Size(p.width * density, h),
                        )
                    }
                }
            }
        }
    }
}
