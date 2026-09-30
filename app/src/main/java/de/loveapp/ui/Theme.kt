package de.loveapp.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Wine = Color(0xFF8B1E31)

private val Light = lightColorScheme(
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
)

private val Dark = darkColorScheme(
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
)

private val base = Typography()
private val serif = FontFamily.Serif

private val LoveTypography = base.copy(
    displayLarge = base.displayLarge.copy(fontFamily = serif, fontWeight = FontWeight.SemiBold),
    displayMedium = base.displayMedium.copy(fontFamily = serif, fontWeight = FontWeight.SemiBold),
    headlineLarge = base.headlineLarge.copy(fontFamily = serif),
    headlineMedium = base.headlineMedium.copy(fontFamily = serif),
    headlineSmall = base.headlineSmall.copy(fontFamily = serif),
    titleLarge = base.titleLarge.copy(fontFamily = serif),
)

val LabelCaps = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.4.sp)

@Composable
fun LoveTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = LoveTypography,
        content = content,
    )
}

/** Standardmotive, falls kein eigenes Foto gewaehlt ist. */
data class Preset(val name: String, val brush: Brush)

val Presets = listOf(
    Preset("Weinrot", Brush.linearGradient(listOf(Color(0xFF4A0B18), Color(0xFF8B1E31), Color(0xFFE0566B)))),
    Preset("Abendrot", Brush.linearGradient(listOf(Color(0xFF3B1C4A), Color(0xFFC2466B), Color(0xFFF7A26B)))),
    Preset("Lavendel", Brush.linearGradient(listOf(Color(0xFF2E2A5A), Color(0xFF7B63B8), Color(0xFFE6B7E0)))),
    Preset("Nachthimmel", Brush.linearGradient(listOf(Color(0xFF05070F), Color(0xFF1B2450), Color(0xFF6A4C93)))),
    Preset("Rosé", Brush.linearGradient(listOf(Color(0xFF8C4A5A), Color(0xFFE8A0A8), Color(0xFFFBE3DA)))),
    Preset("Meer", Brush.linearGradient(listOf(Color(0xFF062B3A), Color(0xFF1C6E8C), Color(0xFF8FD3D1)))),
)
