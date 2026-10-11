package app.sinceus.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.core.view.drawToBitmap
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Erzeugt das Icon für den Play Store nach app/build/store (mdpi: 1 dp = 1 px). Die übrigen Store-Bilder macht .github/scripts/store-images.py. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StoreAssetsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    @Config(sdk = [35], qualifiers = "w512dp-h512dp-mdpi")
    fun icon() = render("icon-512") {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF7A1A2C)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Favorite, null, tint = Color.White, modifier = Modifier.size(280.dp))
        }
    }

    private fun render(name: String, content: @Composable () -> Unit) {
        compose.setContent { LoveTheme(content = content) }
        compose.mainClock.autoAdvance = false
        compose.waitForIdle()
        val bitmap = compose.activity.window.decorView.drawToBitmap()
        val dir = File("build/store").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
