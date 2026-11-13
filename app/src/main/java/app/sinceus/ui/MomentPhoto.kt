package app.sinceus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.sinceus.R
import app.sinceus.data.PhotoFrame
import coil3.compose.AsyncImage
import kotlin.math.max

/** Seitenverhältnis der Fotos in der Zeitleiste; danach richtet sich auch der Editor */
const val MOMENT_PHOTO_RATIO = 16f / 10f

/**
 * Foto mit gewähltem Ausschnitt (Issue #49). Ohne [frame] mittig, wie bisher.
 * Gleiche Rechnung wie beim Paarfoto und in [app.sinceus.widget.WidgetPhoto.crop],
 * damit App, Fotobuch und Teilen-Bild denselben Ausschnitt zeigen.
 */
@Composable
fun FramedPhoto(
    model: Any,
    frame: PhotoFrame?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    onImageSize: (Size) -> Unit = {},
) {
    val f = frame ?: PhotoFrame.CENTER
    Box(modifier.clipToBounds()) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            alignment = BiasAlignment(f.x, f.y),
            onSuccess = { onImageSize(it.painter.intrinsicSize) },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = f.zoom
                    scaleY = f.zoom
                    transformOrigin = TransformOrigin((f.x + 1) / 2, (f.y + 1) / 2)
                },
        )
    }
}

/**
 * Fotos in voller Größe ansehen: wischen zum nächsten Foto, mit zwei Fingern
 * oder doppeltem Tippen zoomen. Zurück oder das Kreuz schließt die Ansicht.
 */
@Composable
fun PhotoViewer(photos: List<Any>, start: Int, onClose: () -> Unit) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val pager = rememberPagerState(initialPage = start.coerceIn(0, (photos.size - 1).coerceAtLeast(0))) { photos.size }
        var zoomed by remember { mutableStateOf(false) }
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            HorizontalPager(
                pager,
                Modifier.fillMaxSize(),
                userScrollEnabled = !zoomed,
                key = { it },
            ) { page ->
                ZoomableImage(
                    model = photos[page],
                    active = page == pager.currentPage,
                    onZoomed = { if (page == pager.currentPage) zoomed = it },
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close), tint = Color.White)
                }
                if (photos.size > 1) {
                    Text(
                        stringResource(R.string.moment_photo_position, pager.currentPage + 1, photos.size),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        }
    }
}

/** Ein Foto ganz sichtbar; gezoomt lässt es sich verschieben, ungezoomt gehört das Wischen dem Pager */
@Composable
private fun ZoomableImage(model: Any, active: Boolean, onZoomed: (Boolean) -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var box by remember { mutableStateOf(IntSize.Zero) }
    // Beim Weiterwischen fängt das vorige Foto wieder ungezoomt an
    LaunchedEffect(active) {
        if (!active) {
            scale = 1f
            offset = Offset.Zero
        }
    }
    fun clamp(o: Offset, s: Float): Offset {
        val maxX = box.width * (s - 1) / 2
        val maxY = box.height * (s - 1) / 2
        return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
    }
    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { box = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tap ->
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = DOUBLE_TAP_ZOOM
                            // Die angetippte Stelle rückt in die Mitte
                            val center = Offset(box.width / 2f, box.height / 2f)
                            offset = clamp((center - tap) * DOUBLE_TAP_ZOOM, DOUBLE_TAP_ZOOM)
                        }
                        onZoomed(scale > 1f)
                    },
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        // Ein Finger ohne Zoom: nichts tun, damit der Pager wischen kann
                        if (event.changes.size > 1 || scale > 1f) {
                            val newScale = (scale * event.calculateZoom()).coerceIn(1f, MAX_VIEW_ZOOM)
                            offset = clamp(offset + event.calculatePan(), newScale)
                            scale = newScale
                            if (scale <= 1f) offset = Offset.Zero
                            onZoomed(scale > 1f)
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        )
    }
}

private const val DOUBLE_TAP_ZOOM = 2.5f
private const val MAX_VIEW_ZOOM = 5f

/**
 * Bildausschnitte der Fotos eines Moments wählen. [photos] sind die Einträge aus dem Editor
 * (Pfad oder content-Uri), [models] die passenden Bildquellen. [onDone] bekommt alle Ausschnitte,
 * mittige fallen weg.
 */
@Composable
fun PhotoFrameEditor(
    photos: List<String>,
    models: List<Any>,
    frames: Map<String, PhotoFrame>,
    start: Int,
    onCancel: () -> Unit,
    onDone: (Map<String, PhotoFrame>) -> Unit,
) {
    var edited by rememberSaveable { mutableStateOf(PhotoFrame.encodeMap(frames)) }
    val current = remember(edited) { PhotoFrame.decodeMap(edited) }
    var index by rememberSaveable { mutableIntStateOf(start.coerceIn(0, (photos.size - 1).coerceAtLeast(0))) }
    val key = photos.getOrNull(index) ?: return
    val frame = current[key] ?: PhotoFrame.CENTER
    fun set(f: PhotoFrame) {
        val clean = f.clean()
        edited = PhotoFrame.encodeMap(if (clean == PhotoFrame.CENTER) current - key else current + (key to clean))
    }
    var box by remember { mutableStateOf(IntSize.Zero) }
    var image by remember(key) { mutableStateOf(Size.Unspecified) }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(vertical = 16.dp),
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
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                )
            }

            // Rahmen im gleichen Format wie in der Zeitleiste
            FramedPhoto(
                model = models[index],
                frame = frame,
                onImageSize = { image = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .aspectRatio(MOMENT_PHOTO_RATIO)
                    .clip(RoundedCornerShape(20.dp))
                    .onSizeChanged { box = it }
                    .pointerInput(key) {
                        detectTransformGestures { _, pan, gestureZoom, _ ->
                            val old = PhotoFrame.decodeMap(edited)[key] ?: PhotoFrame.CENTER
                            val zoom = (old.zoom * gestureZoom).coerceIn(1f, PhotoFrame.MAX_ZOOM)
                            var x = old.x
                            var y = old.y
                            if (image.width > 0f && image.height > 0f && box != IntSize.Zero) {
                                // Wie weit ragt das (gezoomte) Bild über den Rahmen hinaus?
                                val base = max(box.width / image.width, box.height / image.height)
                                val overX = image.width * base * zoom - box.width
                                val overY = image.height * base * zoom - box.height
                                if (overX > 1f) x = (x - 2 * pan.x / overX).coerceIn(-1f, 1f)
                                if (overY > 1f) y = (y - 2 * pan.y / overY).coerceIn(-1f, 1f)
                            }
                            set(PhotoFrame(x, y, zoom))
                        }
                    },
            )

            Column {
                if (photos.size > 1) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) {
                        itemsIndexed(photos, key = { _, item -> item }) { i, item ->
                            val position = stringResource(R.string.moment_photo_position, i + 1, photos.size)
                            FramedPhoto(
                                model = models[i],
                                frame = current[item],
                                contentDescription = position,
                                modifier = Modifier
                                    .size(width = 80.dp, height = 50.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .then(
                                        if (i == index) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)) else Modifier,
                                    )
                                    .clickable { index = i },
                            )
                        }
                    }
                    Text(
                        stringResource(R.string.moment_crop_pick),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                    )
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(onClick = { set(PhotoFrame.CENTER) }, enabled = frame != PhotoFrame.CENTER) {
                        Text(stringResource(R.string.reset), color = Color.White)
                    }
                    TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.cancel), color = Color.White)
                    }
                    Button(onClick = { onDone(PhotoFrame.decodeMap(edited)) }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.done))
                    }
                }
            }
        }
    }
}
