package app.sinceus.ui

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.runtime.remember
import androidx.compose.runtime.Immutable
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import java.time.LocalDate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

val Wine = Color(0xFF8B1E31)

/**
 * Farben, die nicht im Material-Farbschema stecken: Herzen, der Live-Zähler und die Leiste unten.
 * strong steht auf Weiß, soft und glow auf dunklem Grund (night, bar).
 */
@Immutable
data class Accent(
    val strong: Color,
    val heart: Color,
    val soft: Color,
    val glow: Color,
    val night: Color,
    val bar: Color,
)

/** Ein Farbthema der App mit hellem und dunklem Schema. */
class AppColors(
    val id: String,
    @StringRes val name: Int,
    val light: ColorScheme,
    val dark: ColorScheme,
    val accent: Accent,
)

/** Farbton (OKLCH) der Weinfarbe, von ihm aus werden die anderen Themen gedreht */
private const val WINE_HUE = 16.6f

/**
 * Baut ein Thema aus dem Wein-Schema: jede Farbe wird im OKLCH-Farbraum um [shift] Grad gedreht.
 * Helligkeit bleibt gleich, deshalb bleibt alles genauso gut lesbar wie beim Original.
 */
private fun wine(id: String, @StringRes name: Int, shift: Float): AppColors {
    val c: (Long) -> Color = { if (shift == 0f) Color(it) else Oklch.rotate(Color(it), shift) }
    return AppColors(
        id = id,
        name = name,
        light = lightColorScheme(
            primary = c(0xFF8B1E31),
            onPrimary = Color.White,
            primaryContainer = c(0xFFFFD9DC),
            onPrimaryContainer = c(0xFF3F0010),
            secondary = c(0xFF765659),
            secondaryContainer = c(0xFFFFD9DC),
            onSecondaryContainer = c(0xFF2C1517),
            tertiary = c(0xFF7A5733),
            background = c(0xFFFFF8F7),
            onBackground = c(0xFF22191A),
            surface = c(0xFFFFF8F7),
            onSurface = c(0xFF22191A),
            surfaceVariant = c(0xFFF4DDDE),
            onSurfaceVariant = c(0xFF524344),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = c(0xFFFFF0F0),
            surfaceContainer = c(0xFFFCEAEA),
            surfaceContainerHigh = c(0xFFF6E4E4),
            surfaceContainerHighest = c(0xFFF0DEDF),
            outlineVariant = c(0xFFD7C1C2),
        ),
        dark = darkColorScheme(
            primary = c(0xFFFFB2B9),
            onPrimary = c(0xFF670020),
            primaryContainer = c(0xFF8B1E31),
            onPrimaryContainer = c(0xFFFFD9DC),
            secondary = c(0xFFE5BDC0),
            secondaryContainer = c(0xFF5D3F42),
            onSecondaryContainer = c(0xFFFFD9DC),
            tertiary = c(0xFFEBBE90),
            background = c(0xFF1A1112),
            onBackground = c(0xFFF0DEDF),
            surface = c(0xFF1A1112),
            onSurface = c(0xFFF0DEDF),
            surfaceVariant = c(0xFF524344),
            onSurfaceVariant = c(0xFFD7C1C2),
            surfaceContainerLowest = c(0xFF140C0D),
            surfaceContainerLow = c(0xFF22191A),
            surfaceContainer = c(0xFF271D1E),
            surfaceContainerHigh = c(0xFF322828),
            surfaceContainerHighest = c(0xFF3D3233),
            outlineVariant = c(0xFF524344),
        ),
        accent = Accent(
            strong = c(0xFF8B1E31),
            heart = c(0xFFFF5C77),
            soft = c(0xFFFFB2B9),
            glow = c(0xFFFF8FA3),
            night = c(0xFF120B0C),
            bar = c(0xFF2A1D1F),
        ),
    )
}

/** Umrechnung in den OKLCH-Farbraum (Helligkeit, Buntheit, Farbton), um Farben gleichmäßig zu drehen */
internal object Oklch {
    private fun toLinear(v: Float) = if (v <= 0.04045f) v / 12.92f else ((v + 0.055f) / 1.055f).pow(2.4f)
    private fun toSrgb(v: Float) = if (v <= 0.0031308f) 12.92f * v else 1.055f * v.pow(1f / 2.4f) - 0.055f

    /** Farbton der Farbe in Grad */
    fun hue(color: Color): Float = toLch(color)[2]

    fun rotate(color: Color, degrees: Float): Color {
        val (l, chroma, h) = toLch(color)
        return fromLch(l, chroma, (h + degrees).mod(360f), color.alpha)
    }

    private fun toLch(color: Color): FloatArray {
        val r = toLinear(color.red)
        val g = toLinear(color.green)
        val b = toLinear(color.blue)
        val l = cbrt(0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b)
        val m = cbrt(0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b)
        val s = cbrt(0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b)
        val lightness = 0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s
        val a = 1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s
        val bb = 0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s
        return floatArrayOf(lightness, hypot(a, bb), Math.toDegrees(atan2(bb, a).toDouble()).toFloat())
    }

    /** Zurück nach sRGB; liegt die Farbe außerhalb, wird sie schrittweise weniger bunt, bis sie passt */
    private fun fromLch(lightness: Float, chroma: Float, hue: Float, alpha: Float): Color {
        val rad = Math.toRadians(hue.toDouble())
        var c = chroma
        while (true) {
            val a = c * cos(rad).toFloat()
            val b = c * sin(rad).toFloat()
            val l = (lightness + 0.3963377774f * a + 0.2158037573f * b).let { it * it * it }
            val m = (lightness - 0.1055613458f * a - 0.0638541728f * b).let { it * it * it }
            val s = (lightness - 0.0894841775f * a - 1.2914855480f * b).let { it * it * it }
            val r = 4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s
            val g = -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s
            val bl = -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s
            val inside = listOf(r, g, bl).all { it in -0.0001f..1.0001f }
            if (inside || c < 0.0001f) {
                return Color(
                    toSrgb(r.coerceIn(0f, 1f)).coerceIn(0f, 1f),
                    toSrgb(g.coerceIn(0f, 1f)).coerceIn(0f, 1f),
                    toSrgb(bl.coerceIn(0f, 1f)).coerceIn(0f, 1f),
                    alpha,
                )
            }
            c *= 0.97f
        }
    }
}

private val WineColors = wine("", app.sinceus.R.string.colors_wine, 0f)

/** Auswahl an Farbthemen. "system" nimmt ab Android 12 die Farben des Hintergrundbilds (Material You). */
object AppColorThemes {
    const val SYSTEM = "system"

    /** Eigene Farbe: "hue:" und der Farbton in Grad, z. B. "hue:200" */
    private const val HUE = "hue:"

    val all = listOf(
        WineColors,
        wine("ocean", app.sinceus.R.string.colors_ocean, 230f - WINE_HUE),
        wine("jade", app.sinceus.R.string.colors_jade, 165f - WINE_HUE),
        wine("lavender", app.sinceus.R.string.colors_lavender, 300f - WINE_HUE),
    )

    /** Systemfarben gibt es erst ab Android 12 */
    val systemAvailable: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    fun customId(hue: Int) = HUE + hue.mod(360)

    /** Farbton einer eigenen Farbe, null = keine eigene Farbe */
    fun customHue(id: String): Int? = id.removePrefix(HUE).takeIf { id.startsWith(HUE) }?.toIntOrNull()?.mod(360)

    /** Thema aus einem frei gewählten Farbton */
    fun custom(hue: Int): AppColors = wine(customId(hue), app.sinceus.R.string.colors_custom, hue - WINE_HUE)

    fun find(id: String): AppColors =
        customHue(id)?.let(::custom) ?: all.firstOrNull { it.id == id } ?: WineColors
}

/** Akzentfarben des gewählten Themas */
val LocalAccent = staticCompositionLocalOf { WineColors.accent }

val LabelCaps = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.4.sp)

@Composable
fun LoveTheme(font: String = "", colors: String = "", content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val (scheme, accent) = remember(colors, dark) {
        if (colors == AppColorThemes.SYSTEM && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val light = dynamicLightColorScheme(context)
            val night = dynamicDarkColorScheme(context)
            (if (dark) night else light) to Accent(
                strong = light.primary,
                heart = lerp(light.primary, night.primary, 0.5f),
                soft = night.primary,
                glow = night.primary,
                night = night.surfaceContainerLowest,
                bar = night.surfaceContainerHigh,
            )
        } else {
            val theme = AppColorThemes.find(colors)
            (if (dark) theme.dark else theme.light) to theme.accent
        }
    }
    val typography = remember(font) { AppFonts.typography(AppFonts.find(font)) }
    MaterialTheme(colorScheme = scheme, typography = typography) {
        // Standard-Textfarbe passend zum Hintergrund, sonst ist Text ohne eigene Farbe im Dunkelmodus schwarz
        CompositionLocalProvider(
            LocalContentColor provides scheme.onBackground,
            LocalAccent provides accent,
            content = content,
        )
    }
}

/** Standardmotive, falls kein eigenes Foto gewählt ist. */
data class Preset(@androidx.annotation.StringRes val name: Int, val colors: List<Color>) {
    val brush: Brush get() = Brush.linearGradient(colors)
}

val Presets = listOf(
    Preset(app.sinceus.R.string.preset_wine, listOf(Color(0xFF4A0B18), Color(0xFF8B1E31), Color(0xFFE0566B))),
    Preset(app.sinceus.R.string.preset_sunset, listOf(Color(0xFF3B1C4A), Color(0xFFC2466B), Color(0xFFF7A26B))),
    Preset(app.sinceus.R.string.preset_lavender, listOf(Color(0xFF2E2A5A), Color(0xFF7B63B8), Color(0xFFE6B7E0))),
    Preset(app.sinceus.R.string.preset_night, listOf(Color(0xFF05070F), Color(0xFF1B2450), Color(0xFF6A4C93))),
    Preset(app.sinceus.R.string.preset_rose, listOf(Color(0xFF8C4A5A), Color(0xFFE8A0A8), Color(0xFFFBE3DA))),
    Preset(app.sinceus.R.string.preset_ocean, listOf(Color(0xFF062B3A), Color(0xFF1C6E8C), Color(0xFF8FD3D1))),
)

/** Im Juni (Pride Month) ist die App ein bisschen bunter: Herzen in Regenbogenfarben und ein Gruß */
val LocalPrideMonth = staticCompositionLocalOf { false }

fun isPrideMonth(day: LocalDate) = day.monthValue == 6

/** Farben der Regenbogenflagge */
val PrideColors = listOf(
    Color(0xFFE40303),
    Color(0xFFFF8C00),
    Color(0xFFFFED00),
    Color(0xFF008026),
    Color(0xFF004DFF),
    Color(0xFF750787),
)

/** Färbt den Inhalt (z. B. ein Herz-Icon) mit einem Regenbogenverlauf */
fun Modifier.rainbow(): Modifier = graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(Brush.linearGradient(PrideColors), blendMode = BlendMode.SrcIn)
    }
