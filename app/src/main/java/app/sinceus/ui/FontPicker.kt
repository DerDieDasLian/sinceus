package app.sinceus.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sinceus.R

/**
 * Schriftauswahl als eigene Seite wie die anderen Einstellungen, damit die ganze Höhe genutzt wird.
 * Jede Schrift wird in sich selbst gezeigt. Ein Tipp wechselt die Schrift sofort in der ganzen App,
 * so sieht man direkt, wie sie wirkt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontPage(selected: String, onSelect: (String) -> Unit, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val current = AppFonts.find(selected).id
    val fonts = AppFonts.all
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.font_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 24.dp,
                end = 24.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
        ) {
            itemsIndexed(fonts, key = { _, font -> font.id }) { index, font ->
                Column {
                    if (index == 0 || fonts[index - 1].group != font.group) {
                        Text(
                            stringResource(font.group),
                            style = LabelCaps,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = if (index == 0) 0.dp else 20.dp, bottom = 4.dp),
                        )
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = font.id == current, role = Role.RadioButton) { onSelect(font.id) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = font.id == current, onClick = null)
                        Column(Modifier.padding(start = 16.dp)) {
                            Text(
                                font.label(),
                                fontFamily = font.heading,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = (24 * font.scale).sp,
                            )
                            Text(
                                stringResource(R.string.font_sample),
                                fontFamily = font.body ?: FontFamily.Default,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
