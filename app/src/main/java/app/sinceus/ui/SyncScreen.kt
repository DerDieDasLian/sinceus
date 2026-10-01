package app.sinceus.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.sinceus.R
import app.sinceus.data.LoveSettings
import app.sinceus.data.Pairing
import app.sinceus.data.Texts
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.time.Instant
import java.time.ZoneId

/** Aktionen der Seite „Abgleich“ */
class SyncActions(
    /** Neue Kopplung anlegen (mit dem Menschen, der dieses Handy nutzt) */
    val onCreate: (String?) -> Unit = {},
    /** QR-Code des anderen Handys scannen */
    val onScan: (String?) -> Unit = {},
    val onMe: (String?) -> Unit = {},
    val onUnpair: () -> Unit = {},
    val onSend: () -> Unit = {},
    val onOpen: () -> Unit = {},
)

/** Abgleich mit dem Handy des anderen Menschen: koppeln per QR-Code, dann Dateien austauschen */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SyncPage(settings: LoveSettings, pairing: Pairing?, actions: SyncActions) {
    var me by remember(pairing) { mutableStateOf(pairing?.me) }
    var showQr by remember { mutableStateOf(false) }
    var confirmUnpair by remember { mutableStateOf(false) }

    Text(
        stringResource(if (pairing == null) R.string.sync_intro else R.string.sync_intro_paired),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )

    SyncCard {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.sync_who), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                settings.people.forEach { p ->
                    FilterChip(
                        selected = me == p.id,
                        onClick = {
                            me = p.id
                            if (pairing != null) actions.onMe(p.id)
                        },
                        label = { Text(p.name) },
                    )
                }
            }
        }
    }

    if (pairing == null) {
        SyncCard {
            SyncRow(Icons.Rounded.QrCode2, stringResource(R.string.sync_show_qr), stringResource(R.string.sync_show_qr_summary)) {
                actions.onCreate(me)
                showQr = true
            }
            Line()
            SyncRow(Icons.Rounded.QrCodeScanner, stringResource(R.string.sync_scan), stringResource(R.string.sync_scan_summary)) {
                actions.onScan(me)
            }
        }
    } else {
        SyncCard {
            SyncRow(
                Icons.Rounded.Sync,
                stringResource(R.string.sync_paired),
                if (pairing.lastSync > 0) {
                    stringResource(R.string.sync_last, Texts.mediumDate(day(pairing.lastSync)))
                } else {
                    stringResource(R.string.sync_paired_since, Texts.mediumDate(day(pairing.since)))
                },
                onClick = null,
            )
            Line()
            SyncRow(Icons.Rounded.Send, stringResource(R.string.sync_send), stringResource(R.string.sync_send_summary), onClick = actions.onSend)
            Line()
            SyncRow(Icons.Rounded.FileOpen, stringResource(R.string.sync_open), stringResource(R.string.sync_open_summary), onClick = actions.onOpen)
        }
        SyncCard {
            SyncRow(Icons.Rounded.QrCode2, stringResource(R.string.sync_show_qr_again), stringResource(R.string.sync_show_qr_again_summary)) {
                showQr = true
            }
            Line()
            ListItem(
                headlineContent = { Text(stringResource(R.string.sync_unpair), color = MaterialTheme.colorScheme.error) },
                leadingContent = { Icon(Icons.Rounded.LinkOff, null, tint = MaterialTheme.colorScheme.error) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { confirmUnpair = true },
            )
        }
    }

    if (showQr && pairing != null) {
        AlertDialog(
            onDismissRequest = { showQr = false },
            title = { Text(stringResource(R.string.sync_show_qr)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.sync_qr_text))
                    QrCode(
                        Pairing.qrText(pairing),
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp)),
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showQr = false }) { Text(stringResource(R.string.done)) } },
        )
    }
    if (confirmUnpair) {
        AlertDialog(
            onDismissRequest = { confirmUnpair = false },
            icon = { Icon(Icons.Rounded.LinkOff, null) },
            title = { Text(stringResource(R.string.sync_unpair_question)) },
            text = { Text(stringResource(R.string.sync_unpair_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmUnpair = false
                        actions.onUnpair()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.sync_unpair)) }
            },
            dismissButton = { TextButton(onClick = { confirmUnpair = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

private fun day(millis: Long) = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

@Composable
private fun SyncCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) { Column { content() } }
}

@Composable
private fun Line() = HorizontalDivider(
    Modifier.padding(horizontal = 16.dp),
    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
)

@Composable
private fun SyncRow(icon: ImageVector, title: String, summary: String, onClick: (() -> Unit)?) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        leadingContent = { Icon(icon, null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
    )
}

/** QR-Code als schwarze Quadrate auf Weiß */
@Composable
internal fun QrCode(text: String, modifier: Modifier = Modifier) {
    val matrix = remember(text) {
        QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.MARGIN to 2))
    }
    Canvas(
        modifier
            .aspectRatio(1f)
            .background(Color.White),
    ) {
        val cell = size.width / matrix.width
        for (x in 0 until matrix.width) {
            for (y in 0 until matrix.height) {
                if (matrix[x, y]) {
                    // Etwas überlappen, damit keine hellen Linien zwischen den Feldern entstehen
                    drawRect(Color.Black, Offset(x * cell, y * cell), Size(cell + 0.5f, cell + 0.5f))
                }
            }
        }
    }
}
