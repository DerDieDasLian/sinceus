package app.sinceus.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.sinceus.R

/** Name des gewählten Farbthemas für die Zeile in den Einstellungen */
@Composable
fun colorsLabel(id: String): String =
    if (id == AppColorThemes.SYSTEM && AppColorThemes.systemAvailable) {
        stringResource(R.string.colors_system)
    } else {
        stringResource(AppColorThemes.find(id).name)
    }

/**
 * Auswahl des Farbthemas. Ein Tipp färbt die ganze App sofort um, so sieht man direkt, wie es wirkt.
 * Die Systemfarben (Material You) gibt es erst ab Android 12.
 */
@Composable
fun ColorsDialog(selected: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val current = if (selected == AppColorThemes.SYSTEM && AppColorThemes.systemAvailable) {
        AppColorThemes.SYSTEM
    } else {
        AppColorThemes.find(selected).id
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Palette, null) },
        title = { Text(stringResource(R.string.colors_title)) },
        text = {
            Column {
                AppColorThemes.all.forEach { theme ->
                    val scheme = if (dark) theme.dark else theme.light
                    ColorsOption(
                        name = stringResource(theme.name),
                        summary = null,
                        background = scheme.primaryContainer,
                        heart = theme.accent.heart,
                        selected = theme.id == current,
                        onClick = { onSelect(theme.id) },
                    )
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val scheme = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                    ColorsOption(
                        name = stringResource(R.string.colors_system),
                        summary = stringResource(R.string.colors_system_summary),
                        background = scheme.primaryContainer,
                        heart = scheme.primary,
                        selected = current == AppColorThemes.SYSTEM,
                        onClick = { onSelect(AppColorThemes.SYSTEM) },
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } },
    )
}

@Composable
private fun ColorsOption(
    name: String,
    summary: String?,
    background: Color,
    heart: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Box(
            Modifier
                .padding(start = 12.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(background)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Favorite, null, tint = heart, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.padding(start = 12.dp)) {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            if (summary != null) {
                Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
