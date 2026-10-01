package app.sinceus.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sinceus.BuildConfig
import app.sinceus.R

/** Eine verwendete Bibliothek (alle stehen unter der Apache License 2.0). */
private data class Library(val name: String, val author: String, val url: String)

private val Libraries = listOf(
    Library("Android Jetpack (AndroidX, Jetpack Compose, Material 3, Glance, DataStore)", "The Android Open Source Project", "https://developer.android.com/jetpack"),
    Library("Kotlin, kotlinx.coroutines", "JetBrains", "https://kotlinlang.org"),
    Library("Compose Multiplatform", "JetBrains", "https://github.com/JetBrains/compose-multiplatform"),
    Library("Coil", "Coil Contributors", "https://coil-kt.github.io/coil/"),
    Library("ZXing, ZXing Android Embedded", "ZXing authors, JourneyApps", "https://github.com/journeyapps/zxing-android-embedded"),
    Library("Okio", "Square, Inc.", "https://square.github.io/okio/"),
    Library("Guava ListenableFuture, Accompanist", "Google", "https://github.com/google/guava"),
    Library("JSpecify", "The JSpecify Authors", "https://jspecify.dev"),
    Library("Material Symbols", "Google", "https://fonts.google.com/icons"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val uri = LocalUriHandler.current
    var showText by rememberSaveable { mutableStateOf(false) }
    val apacheText = remember {
        context.resources.openRawResource(R.raw.apache_license_2_0).bufferedReader().use { it.readText() }
    }

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.licenses)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.licenses_app),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                "github.com/${BuildConfig.GITHUB_REPO}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { uri.openUri("https://github.com/${BuildConfig.GITHUB_REPO}") },
            )
            Text(
                stringResource(R.string.licenses_libraries),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
            Libraries.forEach { lib ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { uri.openUri(lib.url) },
                ) {
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                        Text(lib.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${lib.author} · Apache License 2.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            OutlinedButton(onClick = { showText = !showText }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (showText) R.string.licenses_hide_text else R.string.licenses_show_text))
            }
            if (showText) {
                Text(
                    apacheText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
