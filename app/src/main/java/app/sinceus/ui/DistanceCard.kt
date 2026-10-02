package app.sinceus.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.sinceus.R
import app.sinceus.data.Distance
import app.sinceus.data.Relationship
import app.sinceus.data.Texts
import app.sinceus.data.formatNumber
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

/** Fernbeziehung: Tage bis zum nächsten Treffen und die Uhrzeit am anderen Ort */
@Composable
internal fun DistanceCard(r: Relationship, today: LocalDate) {
    val context = LocalContext.current
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(20_000)
            now = Instant.now()
        }
    }
    val days = Distance.daysUntil(r.nextMeeting, today)
    val far = r.farZone?.takeIf(Distance::isZone)?.let(ZoneId::of)
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Flight, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text(
                    stringResource(R.string.distance_mode).uppercase(),
                    style = LabelCaps,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Text(
                when (days) {
                    null -> stringResource(R.string.distance_no_meeting)
                    0L -> stringResource(R.string.distance_today)
                    else -> pluralStringResource(R.plurals.distance_days, days.toInt(), formatNumber(days))
                },
                style = if (days == null) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.headlineSmall,
                fontWeight = if (days == null) FontWeight.Normal else FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(top = 6.dp),
            )
            if (far != null) {
                val here = ZoneId.systemDefault()
                val time = Texts.time(context, now.atZone(far).toLocalTime())
                val offset = Distance.offsetMinutes(far, here, now)
                val diff = when {
                    offset == 0 -> stringResource(R.string.distance_same_time)
                    else -> {
                        val h = abs(offset) / 60
                        val m = abs(offset) % 60
                        val amount = listOfNotNull(
                            h.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.hours, it, formatNumber(it.toLong())) },
                            m.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.minutes, it, formatNumber(it.toLong())) },
                        ).joinToString(" ")
                        stringResource(if (offset > 0) R.string.distance_later else R.string.distance_earlier, amount)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                    Icon(Icons.Rounded.Public, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
                    Column(Modifier.padding(start = 8.dp)) {
                        Text(
                            stringResource(R.string.distance_time, Distance.cityName(far.id), time),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(diff, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
        }
    }
}
