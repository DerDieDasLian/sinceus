package app.sinceus.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.sinceus.R

/** Wofür das Geld auf Open Collective genau gebraucht wird, als Liste. */
@Composable
fun SupportCosts() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(R.string.support_cost_apple, R.string.support_cost_web, R.string.support_cost_address).forEach { cost ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("•", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(cost), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Erklärung in den Einstellungen, bevor Open Collective geöffnet wird. */
@Composable
fun SupportDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.VolunteerActivism, null) },
        title = { Text(stringResource(R.string.support_project)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.support_dialog_intro), style = MaterialTheme.typography.bodyMedium)
                SupportCosts()
                Text(stringResource(R.string.support_dialog_outro), style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            TextButton(onClick = { onDismiss(); Feedback.openWeb(context, Feedback.SUPPORT) }) { Text(stringResource(R.string.support_open)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

/** Große Hinweis-Box oben im Neuigkeiten-Fenster (nur einmal, bei einer Version). */
@Composable
fun SupportNotice() {
    val context = LocalContext.current
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.VolunteerActivism, null)
                Text(
                    stringResource(R.string.support_notice_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(stringResource(R.string.support_notice_text), style = MaterialTheme.typography.bodyMedium)
            SupportCosts()
            Text(stringResource(R.string.support_notice_where), style = MaterialTheme.typography.bodySmall)
            Button(onClick = { Feedback.openWeb(context, Feedback.SUPPORT) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.support_open))
            }
        }
    }
}
