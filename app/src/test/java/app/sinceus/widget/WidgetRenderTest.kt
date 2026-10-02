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
        java.util.Locale.setDefault(
            if (context.resources.configuration.locales[0].language == "de") java.util.Locale.GERMANY else java.util.Locale.US,
        )
        val repo = LoveRepository(context)
        repo.setNames(listOf("Alex", "Sam"))
        // Gut 5 Monate vor heute: Der Balken zum nächsten Monatstag ist dann etwa halb voll
        repo.setStartDate(LocalDate.now().minusMonths(5).minusDays(15))
    }

    @Test
    fun days() = render(LoveWidget(), "days_2x2", DpSize(160.dp, 150.dp))

    @Test
    @Config(qualifiers = "en-night-xxhdpi")
    fun daysEnglish() = render(LoveWidget(), "days_2x2_en", DpSize(160.dp, 150.dp))

    @Test
    fun card() = render(CardWidget(), "card_3x2", DpSize(250.dp, 150.dp))

    @Test
    @Config(qualifiers = "en-night-xxhdpi")
    fun cardEnglish() = render(CardWidget(), "card_3x2_en", DpSize(250.dp, 150.dp))

    @Test
    fun cardSmall() = render(CardWidget(), "card_2x2", DpSize(160.dp, 150.dp))

    @Test
    fun cardLarge() = render(CardWidget(), "card_4x2", DpSize(330.dp, 180.dp))

    @Test
    fun cardFlat() = render(CardWidget(), "card_4x1", DpSize(330.dp, 90.dp))

    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    private fun render(widget: GlanceAppWidget, name: String, size: DpSize): Unit = runBlocking {
        setUp()
        val views = widget.compose(context, size = size)
        val parcel = android.os.Parcel.obtain()
        views.writeToParcel(parcel, 0)
        val kb = parcel.dataSize() / 1024
        parcel.recycle()
        println("WIDGET $name parcel=$kb KB")
        // Über ca. 1 MB verwirft Android das Update, dann fehlt das Bild im Widget
        org.junit.Assert.assertTrue("Widget-Update zu groß: $kb KB", kb < 400)
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
        // Durchsichtiger Hintergrund, damit die runden Ecken auch auf der Website und als Vorschau sauber sind
        canvas.drawColor(android.graphics.Color.TRANSPARENT)
        parent.draw(canvas)
        val dir = File("build/screenshots/widgets").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
