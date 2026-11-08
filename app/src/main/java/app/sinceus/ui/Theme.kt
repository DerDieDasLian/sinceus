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

private val WineColors = AppColors(
    id = "",
    name = app.sinceus.R.string.colors_wine,
    light = lightColorScheme(
        primary = Wine,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFD9DC),
        onPrimaryContainer = Color(0xFF3F0010),
        secondary = Color(0xFF765659),
        secondaryContainer = Color(0xFFFFD9DC),
        onSecondaryContainer = Color(0xFF2C1517),
        tertiary = Color(0xFF7A5733),
        background = Color(0xFFFFF8F7),
        onBackground = Color(0xFF22191A),
        surface = Color(0xFFFFF8F7),
        onSurface = Color(0xFF22191A),
        surfaceVariant = Color(0xFFF4DDDE),
        onSurfaceVariant = Color(0xFF524344),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFFFF0F0),
        surfaceContainer = Color(0xFFFCEAEA),
        surfaceContainerHigh = Color(0xFFF6E4E4),
        surfaceContainerHighest = Color(0xFFF0DEDF),
        outlineVariant = Color(0xFFD7C1C2),
    ),
    dark = darkColorScheme(
        primary = Color(0xFFFFB2B9),
        onPrimary = Color(0xFF670020),
        primaryContainer = Color(0xFF8B1E31),
        onPrimaryContainer = Color(0xFFFFD9DC),
        secondary = Color(0xFFE5BDC0),
        secondaryContainer = Color(0xFF5D3F42),
        onSecondaryContainer = Color(0xFFFFD9DC),
        tertiary = Color(0xFFEBBE90),
        background = Color(0xFF1A1112),
        onBackground = Color(0xFFF0DEDF),
        surface = Color(0xFF1A1112),
        onSurface = Color(0xFFF0DEDF),
        surfaceVariant = Color(0xFF524344),
        onSurfaceVariant = Color(0xFFD7C1C2),
        surfaceContainerLowest = Color(0xFF140C0D),
        surfaceContainerLow = Color(0xFF22191A),
        surfaceContainer = Color(0xFF271D1E),
        surfaceContainerHigh = Color(0xFF322828),
        surfaceContainerHighest = Color(0xFF3D3233),
        outlineVariant = Color(0xFF524344),
    ),
    accent = Accent(
        strong = Wine,
        heart = Color(0xFFFF5C77),
        soft = Color(0xFFFFB2B9),
        glow = Color(0xFFFF8FA3),
        night = Color(0xFF120B0C),
        bar = Color(0xFF2A1D1F),
    ),
)

private val OceanColors = AppColors(
    id = "ocean",
    name = app.sinceus.R.string.colors_ocean,
    light = lightColorScheme(
        primary = Color(0xFF045672),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFC7EAFA),
        onPrimaryContainer = Color(0xFF00222E),
        secondary = Color(0xFF456572),
        secondaryContainer = Color(0xFFC7EAFA),
        onSecondaryContainer = Color(0xFF06202B),
        tertiary = Color(0xFF575B86),
        background = Color(0xFFF5FBFF),
        onBackground = Color(0xFF141D21),
        surface = Color(0xFFF5FBFF),
        onSurface = Color(0xFF141D21),
        surfaceVariant = Color(0xFFD3E7F1),
        onSurfaceVariant = Color(0xFF3C4A50),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFEAF6FD),
        surfaceContainer = Color(0xFFE2F2FA),
        surfaceContainerHigh = Color(0xFFDCECF4),
        surfaceContainerHighest = Color(0xFFD6E6ED),
        outlineVariant = Color(0xFFB7CBD4),
    ),
    dark = darkColorScheme(
        primary = Color(0xFF87D6F8),
        onPrimary = Color(0xFF003B4D),
        primaryContainer = Color(0xFF045672),
        onPrimaryContainer = Color(0xFFC7EAFA),
        secondary = Color(0xFFAACFE0),
        secondaryContainer = Color(0xFF2E4D5A),
        onSecondaryContainer = Color(0xFFC7EAFA),
        tertiary = Color(0xFFBDC2FB),
        background = Color(0xFF0C1519),
        onBackground = Color(0xFFD6E6ED),
        surface = Color(0xFF0C1519),
        onSurface = Color(0xFFD6E6ED),
        surfaceVariant = Color(0xFF3C4A50),
        onSurfaceVariant = Color(0xFFB7CBD4),
        surfaceContainerLowest = Color(0xFF081013),
        surfaceContainerLow = Color(0xFF141D21),
        surfaceContainer = Color(0xFF182226),
        surfaceContainerHigh = Color(0xFF242C31),
        surfaceContainerHighest = Color(0xFF2D373B),
        outlineVariant = Color(0xFF3C4A50),
    ),
    accent = Accent(
        strong = Color(0xFF045672),
        heart = Color(0xFF0FACDD),
        soft = Color(0xFF87D6F8),
        glow = Color(0xFF2AC8F4),
        night = Color(0xFF070E11),
        bar = Color(0xFF162328),
    ),
)

private val JadeColors = AppColors(
    id = "jade",
    name = app.sinceus.R.string.colors_jade,
    light = lightColorScheme(
        primary = Color(0xFF025D42),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFCCEDDB),
        onPrimaryContainer = Color(0xFF002516),
        secondary = Color(0xFF4A6858),
        secondaryContainer = Color(0xFFCCEDDB),
        onSecondaryContainer = Color(0xFF092218),
        tertiary = Color(0xFF276977),
        background = Color(0xFFF5FCF9),
        onBackground = Color(0xFF161E19),
        surface = Color(0xFFF5FCF9),
        onSurface = Color(0xFF161E19),
        surfaceVariant = Color(0xFFD5E9DF),
        onSurfaceVariant = Color(0xFF3E4B44),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFEBF8F1),
        surfaceContainer = Color(0xFFE3F3EC),
        surfaceContainerHigh = Color(0xFFDDEDE6),
        surfaceContainerHighest = Color(0xFFD8E7DF),
        outlineVariant = Color(0xFFB9CDC2),
    ),
    dark = darkColorScheme(
        primary = Color(0xFF93DDB8),
        onPrimary = Color(0xFF014029),
        primaryContainer = Color(0xFF025D42),
        onPrimaryContainer = Color(0xFFCCEDDB),
        secondary = Color(0xFFAFD2BF),
        secondaryContainer = Color(0xFF335041),
        onSecondaryContainer = Color(0xFFCCEDDB),
        tertiary = Color(0xFF89D5E8),
        background = Color(0xFF0E1611),
        onBackground = Color(0xFFD8E7DF),
        surface = Color(0xFF0E1611),
        onSurface = Color(0xFFD8E7DF),
        surfaceVariant = Color(0xFF3E4B44),
        onSurfaceVariant = Color(0xFFB9CDC2),
        surfaceContainerLowest = Color(0xFF09100C),
        surfaceContainerLow = Color(0xFF161E19),
        surfaceContainer = Color(0xFF1A221D),
        surfaceContainerHigh = Color(0xFF242D29),
        surfaceContainerHighest = Color(0xFF2E3832),
        outlineVariant = Color(0xFF3E4B44),
    ),
    accent = Accent(
        strong = Color(0xFF025D42),
        heart = Color(0xFF09B981),
        soft = Color(0xFF93DDB8),
        glow = Color(0xFF5ED094),
        night = Color(0xFF090F0B),
        bar = Color(0xFF19241D),
    ),
)

private val LavenderColors = AppColors(
    id = "lavender",
    name = app.sinceus.R.string.colors_lavender,
    light = lightColorScheme(
        primary = Color(0xFF5B358F),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE6DFFE),
        onPrimaryContainer = Color(0xFF240F43),
        secondary = Color(0xFF625B76),
        secondaryContainer = Color(0xFFE6DFFE),
        onSecondaryContainer = Color(0xFF1F192C),
        tertiary = Color(0xFF7D4F65),
        background = Color(0xFFFBF8FE),
        onBackground = Color(0xFF1C1A22),
        surface = Color(0xFFFBF8FE),
        onSurface = Color(0xFF1C1A22),
        surfaceVariant = Color(0xFFE5E0F3),
        onSurfaceVariant = Color(0xFF484551),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFF6F2FE),
        surfaceContainer = Color(0xFFF1ECFA),
        surfaceContainerHigh = Color(0xFFEBE6F4),
        surfaceContainerHighest = Color(0xFFE4E1EF),
        outlineVariant = Color(0xFFC9C4D6),
    ),
    dark = darkColorScheme(
        primary = Color(0xFFCFBEFF),
        onPrimary = Color(0xFF3E1E6E),
        primaryContainer = Color(0xFF5B358F),
        onPrimaryContainer = Color(0xFFE6DFFE),
        secondary = Color(0xFFCBC3E4),
        secondaryContainer = Color(0xFF4A445D),
        onSecondaryContainer = Color(0xFFE6DFFE),
        tertiary = Color(0xFFF0B3CE),
        background = Color(0xFF14121A),
        onBackground = Color(0xFFE4E1EF),
        surface = Color(0xFF14121A),
        onSurface = Color(0xFFE4E1EF),
        surfaceVariant = Color(0xFF484551),
        onSurfaceVariant = Color(0xFFC9C4D6),
        surfaceContainerLowest = Color(0xFF0F0D14),
        surfaceContainerLow = Color(0xFF1C1A22),
        surfaceContainer = Color(0xFF201F27),
        surfaceContainerHigh = Color(0xFF2C2931),
        surfaceContainerHighest = Color(0xFF36343D),
        outlineVariant = Color(0xFF484551),
    ),
    accent = Accent(
        strong = Color(0xFF5B358F),
        heart = Color(0xFFAB80FD),
        soft = Color(0xFFCFBEFF),
        glow = Color(0xFFB9A6FE),
        night = Color(0xFF0D0C12),
        bar = Color(0xFF211F2A),
    ),
)

/** Auswahl an Farbthemen. "system" nimmt ab Android 12 die Farben des Hintergrundbilds (Material You). */
object AppColorThemes {
    const val SYSTEM = "system"
    val all = listOf(WineColors, OceanColors, JadeColors, LavenderColors)

    /** Systemfarben gibt es erst ab Android 12 */
    val systemAvailable: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    fun find(id: String): AppColors = all.firstOrNull { it.id == id } ?: WineColors
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
