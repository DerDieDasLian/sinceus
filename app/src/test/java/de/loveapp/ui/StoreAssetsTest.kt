package de.loveapp.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.drawToBitmap
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Erzeugt die Grafiken fuer den Play Store nach app/build/store (mdpi: 1 dp = 1 px). */
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

    @Test
    @Config(sdk = [35], qualifiers = "w1024dp-h500dp-mdpi")
    fun featureGraphicEnglish() = featureGraphic("en", "Counts your time together")

    @Test
    @Config(sdk = [35], qualifiers = "w1024dp-h500dp-mdpi")
    fun featureGraphicGerman() = featureGraphic("de", "Zählt eure gemeinsame Zeit")

    private fun featureGraphic(lang: String, tagline: String) = render("feature-graphic-1024x500-$lang") {
        Box(
            Modifier
                .fillMaxSize()
                .background(Presets[0].brush),
        ) {
            Icon(
                Icons.Rounded.Favorite,
                null,
                tint = Color.White.copy(alpha = 0.08f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 40.dp)
                    .size(420.dp),
            )
            Row(
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 80.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BeatingHeart(size = 120.dp)
                Column(Modifier.padding(start = 40.dp)) {
                    Text("Since Us", style = MaterialTheme.typography.displayLarge.copy(fontSize = 88.sp), color = Color.White)
                    Text(
                        tagline,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }
        }
    }

    private fun render(name: String, content: @Composable () -> Unit) {
        compose.setContent { LoveTheme(content) }
        compose.mainClock.autoAdvance = false
        compose.waitForIdle()
        val bitmap = compose.activity.window.decorView.drawToBitmap()
        val dir = File("build/store").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
