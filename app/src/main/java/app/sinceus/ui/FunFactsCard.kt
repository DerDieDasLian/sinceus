package app.sinceus.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.sinceus.R
import app.sinceus.data.FunFacts
import app.sinceus.data.formatNumber

/** Karte mit einer lustigen Zahl; Antippen zeigt die nächste */
@Composable
internal fun FunFactsCard(facts: List<FunFacts.Fact>) {
    if (facts.isEmpty()) return
    var index by rememberSaveable { mutableIntStateOf(0) }
    val fact = facts[index % facts.size]
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.facts_next)) { index = (index + 1) % facts.size },
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(
                stringResource(R.string.facts_title).uppercase(),
                style = LabelCaps,
                color = MaterialTheme.colorScheme.primary,
            )
            AnimatedContent(
                targetState = fact,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "fact",
            ) { f ->
                Column(Modifier.padding(top = 6.dp)) {
                    Text(
                        pluralStringResource(f.kind.text(), f.value.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), formatNumber(f.value)),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        stringResource(f.kind.sub()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (facts.size > 1) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    facts.indices.forEach { i ->
                        val active = i == index % facts.size
                        Box(
                            Modifier
                                .size(if (active) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        )
                    }
                }
            }
        }
    }
}

private fun FunFacts.Kind.text(): Int = when (this) {
    FunFacts.Kind.WEEKENDS -> R.plurals.fact_weekends
    FunFacts.Kind.FULL_MOONS -> R.plurals.fact_full_moons
    FunFacts.Kind.VALENTINES -> R.plurals.fact_valentines
    FunFacts.Kind.PRIDE_MONTHS -> R.plurals.fact_pride_months
    FunFacts.Kind.HALLOWEENS -> R.plurals.fact_halloweens
    FunFacts.Kind.CHRISTMAS -> R.plurals.fact_christmas
    FunFacts.Kind.NEW_YEARS -> R.plurals.fact_new_years
    FunFacts.Kind.FRIDAY_13TH -> R.plurals.fact_friday_13th
    FunFacts.Kind.SEASONS -> R.plurals.fact_seasons
    FunFacts.Kind.EARTH_KM -> R.plurals.fact_earth_km
    FunFacts.Kind.HEARTBEATS -> R.plurals.fact_heartbeats
    FunFacts.Kind.BREATHS -> R.plurals.fact_breaths
}

private fun FunFacts.Kind.sub(): Int = when (this) {
    FunFacts.Kind.WEEKENDS -> R.string.fact_weekends_sub
    FunFacts.Kind.FULL_MOONS -> R.string.fact_full_moons_sub
    FunFacts.Kind.VALENTINES -> R.string.fact_valentines_sub
    FunFacts.Kind.PRIDE_MONTHS -> R.string.fact_pride_months_sub
    FunFacts.Kind.HALLOWEENS -> R.string.fact_halloweens_sub
    FunFacts.Kind.CHRISTMAS -> R.string.fact_christmas_sub
    FunFacts.Kind.NEW_YEARS -> R.string.fact_new_years_sub
    FunFacts.Kind.FRIDAY_13TH -> R.string.fact_friday_13th_sub
    FunFacts.Kind.SEASONS -> R.string.fact_seasons_sub
    FunFacts.Kind.EARTH_KM -> R.string.fact_earth_km_sub
    FunFacts.Kind.HEARTBEATS -> R.string.fact_heartbeats_sub
    FunFacts.Kind.BREATHS -> R.string.fact_breaths_sub
}
