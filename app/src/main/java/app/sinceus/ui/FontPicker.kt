package app.sinceus.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sinceus.R

/**
 * Schriftauswahl: jede Schrift wird in sich selbst gezeigt. Ein Tipp wechselt die Schrift sofort
 * in der ganzen App, so sieht man direkt, wie sie wirkt.
 */
@Composable
fun FontDialog(selected: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val current = AppFonts.find(selected).id
    val fonts = AppFonts.all
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.TextFields, null) },
        title = { Text(stringResource(R.string.font_title)) },
        text = {
            // Normale Spalte statt LazyColumn: Dialoge vertragen keine Lazy-Listen
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                fonts.forEachIndexed { index, font ->
                    if (index == 0 || fonts[index - 1].group != font.group) {
                        Text(
                            stringResource(font.group),
                            style = LabelCaps,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = if (index == 0) 0.dp else 16.dp, bottom = 4.dp),
                        )
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = font.id == current, role = Role.RadioButton) { onSelect(font.id) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = font.id == current, onClick = null)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(
                                font.name,
                                fontFamily = font.heading,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = (22 * font.scale).sp,
                            )
                            Text(
                                stringResource(R.string.font_sample),
                                fontFamily = font.body ?: FontFamily.Default,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } },
    )
}
