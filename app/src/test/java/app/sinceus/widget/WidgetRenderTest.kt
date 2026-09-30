package app.sinceus.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.FrameLayout
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.compose
import androidx.test.core.app.ApplicationProvider
import app.sinceus.data.LoveRepository
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

/** Rendert die Widgets als PNG nach app/build/screenshots/widgets. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "de-night-xxhdpi")
class WidgetRenderTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun setUp(): Unit = runBlocking {
        java.util.Locale.setDefault(java.util.Locale.GERMANY)
        val repo = LoveRepository(context)
        repo.setNames("Alex", "Sam")
        // 153 Tage vor heute, damit die Zahlen wie auf dem Handy aussehen
        repo.setStartDate(LocalDate.now().minusDays(153))
    }

    @Test
    fun days() = render(LoveWidget(), "days_2x2", DpSize(160.dp, 150.dp))

    @Test
    fun photo() = render(PhotoWidget(), "photo_3x2", DpSize(250.dp, 150.dp))

    @Test
    fun photoSmall() = render(PhotoWidget(), "photo_2x2", DpSize(160.dp, 150.dp))

    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    private fun render(widget: GlanceAppWidget, name: String, size: DpSize): Unit = runBlocking {
        setUp()
        val views = widget.compose(context, size = size)
        val density = context.resources.displayMetrics.density
        val w = (size.width.value * density).toInt()
        val h = (size.height.value * density).toInt()
        val parent = FrameLayout(context)
        val view = views.apply(context, parent)
        parent.addView(view)
        parent.measure(
            View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY),
        )
        parent.layout(0, 0, w, h)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(0xFF6E4A4E.toInt())
        parent.draw(canvas)
        val dir = File("build/screenshots/widgets").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
