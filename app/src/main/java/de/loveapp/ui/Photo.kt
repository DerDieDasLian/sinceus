package de.loveapp.ui

import de.loveapp.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import de.loveapp.data.DEFAULT_FOCUS_Y
import de.loveapp.data.LoveSettings
import java.io.File
import kotlin.math.max

/** Hoehe des Titelbilds: 60 % des Bildschirms, damit Hochformat-Fotos gut passen. */
@Composable
fun heroHeight(): Dp = with(LocalDensity.current) {
    (LocalWindowInfo.current.containerSize.height * 0.6f).toDp()
}

/** Euer Foto (mit gespeichertem Ausschnitt) oder das gewaehlte Standardmotiv. */
@Composable
fun CouplePhoto(
    settings: LoveSettings,
    modifier: Modifier = Modifier,
    focusX: Float = settings.focusX,
    focusY: Float = settings.focusY,
    zoom: Float = settings.zoom,
    onImageSize: (Size) -> Unit = {},
) {
    Box(modifier.clipToBounds()) {
        if (settings.photoPath != null) {
            AsyncImage(
                model = File(settings.photoPath),
                contentDescription = stringResource(R.string.your_photo),
                contentScale = ContentScale.Crop,
                alignment = BiasAlignment(focusX, focusY),
                onSuccess = { onImageSize(it.painter.intrinsicSize) },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoom
                        scaleY = zoom
                        transformOrigin = TransformOrigin((focusX + 1) / 2, (focusY + 1) / 2)
                    },
            )
        } else {
            PresetBackground(Presets.getOrElse(settings.presetIndex) { Presets[0] }, Modifier.fillMaxSize())
        }
    }
}

@Composable
fun PresetBackground(preset: Preset, modifier: Modifier = Modifier) {
    Box(modifier.background(preset.brush)) {
        Icon(
            Icons.Rounded.Favorite,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.10f),
            modifier = Modifier
                .align(Alignment.Center)
                .size(220.dp),
        )
    }
}

/** Verlauf, damit Schrift auf jedem Foto lesbar bleibt */
val PhotoScrim = Brush.verticalGradient(
    0f to Color.Black.copy(alpha = 0.35f),
    0.2f to Color.Transparent,
    0.6f to Color.Transparent,
    1f to Color.Black.copy(alpha = 0.7f),
)

/**
 * Bildausschnitt anpassen: zeigt das Titelbild in Originalgroesse, verschieben
 * mit einem Finger, zoomen mit zwei Fingern.
 */
@Composable
fun PhotoEditorScreen(
    settings: LoveSettings,
    onPickOther: () -> Unit,
    onCancel: () -> Unit,
    onSave: (Float, Float, Float) -> Unit,
) {
    var fx by remember(settings.photoPath) { mutableFloatStateOf(settings.focusX) }
    var fy by remember(settings.photoPath) { mutableFloatStateOf(settings.focusY) }
    var zoom by remember(settings.photoPath) { mutableFloatStateOf(settings.zoom) }
    var frame by remember { mutableStateOf(IntSize.Zero) }
    var image by remember { mutableStateOf(Size.Unspecified) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(heroHeight())
                .onSizeChanged { frame = it }
                .pointerInput(settings.photoPath) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        zoom = (zoom * gestureZoom).coerceIn(1f, 4f)
                        if (image.isSpecified() && frame != IntSize.Zero) {
                            // Wie weit ragt das (gezoomte) Bild ueber den Rahmen hinaus?
                            val base = max(frame.width / image.width, frame.height / image.height)
                            val overX = image.width * base * zoom - frame.width
                            val overY = image.height * base * zoom - frame.height
                            if (overX > 1f) fx = (fx - 2 * pan.x / overX).coerceIn(-1f, 1f)
                            if (overY > 1f) fy = (fy - 2 * pan.y / overY).coerceIn(-1f, 1f)
                        }
                    }
                },
        ) {
            CouplePhoto(settings, Modifier.fillMaxSize(), fx, fy, zoom, onImageSize = { image = it })
            Box(
                Modifier
                    .fillMaxSize()
                    .background(PhotoScrim),
            )
            NamesOverlay(settings, Modifier.align(Alignment.BottomStart))
        }

        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.OpenWith, null, tint = Color.White.copy(alpha = 0.8f))
                Text(
                    stringResource(R.string.adjust_crop),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    stringResource(R.string.editor_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onPickOther) { Text(stringResource(R.string.other_photo), color = Color.White) }
                    OutlinedButton(onClick = {
                        fx = 0f
                        fy = DEFAULT_FOCUS_Y
                        zoom = 1f
                    }) { Text(stringResource(R.string.reset), color = Color.White) }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.cancel), color = Color.White)
                    }
                    Button(onClick = { onSave(fx, fy, zoom) }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.done))
                    }
                }
            }
        }
    }
}

private fun Size.isSpecified() = this != Size.Unspecified && width > 0f && height > 0f
