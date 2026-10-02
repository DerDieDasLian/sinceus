package app.sinceus.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.sinceus.BuildConfig
import app.sinceus.R

/** Was ist neu: einmal nach einem Update, mit der Möglichkeit, es dauerhaft abzuschalten */
@Composable
fun ChangelogDialog(lines: List<String>, onClose: () -> Unit, onNeverAgain: (() -> Unit)?) {
    AlertDialog(
        onDismissRequest = onClose,
        icon = { Icon(Icons.Rounded.NewReleases, null) },
        title = { Text(stringResource(R.string.changelog_title, BuildConfig.VERSION_NAME)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                lines.forEach { line ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("•", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                        Text(line, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.changelog_ok)) } },
        dismissButton = onNeverAgain?.let { { TextButton(onClick = it) { Text(stringResource(R.string.changelog_never)) } } },
    )
}
