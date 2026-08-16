package com.fabricio.corinthianslive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fabricio.corinthianslive.data.AppSettings
import com.fabricio.corinthianslive.data.CorinthiansRepository
import com.fabricio.corinthianslive.data.model.CompetitionStats
import com.fabricio.corinthianslive.data.model.DetailedMatchStats
import com.fabricio.corinthianslive.data.model.EventType
import com.fabricio.corinthianslive.data.model.Match
import com.fabricio.corinthianslive.data.model.RepositoryResult
import com.fabricio.corinthianslive.data.model.TeamMatchStats
import com.fabricio.corinthianslive.data.model.TeamStats
import com.fabricio.corinthianslive.ui.components.AppCard
import com.fabricio.corinthianslive.ui.components.AppSectionTitle
import com.fabricio.corinthianslive.ui.components.CorinthiansTopBar
import com.fabricio.corinthianslive.ui.components.DataStatus
import com.fabricio.corinthianslive.ui.components.EmptyState
import com.fabricio.corinthianslive.ui.components.LoadingState
import com.fabricio.corinthianslive.ui.components.Pill
import com.fabricio.corinthianslive.ui.theme.CorinthiansColors
import java.util.Locale

@Composable
fun EstatisticasScreen(contentPadding: PaddingValues) {
    val context = LocalContext.current
    val repository = remember { CorinthiansRepository(context.applicationContext) }
    var refresh by remember { mutableIntStateOf(0) }
    val state by produceState<Result<RepositoryResult<TeamStats>>?>(null, refresh) {
        value = runCatching { repository.stats() }
    }
    val result = state?.getOrNull()
    val stats = result?.data
    val hideFriendlies = AppSettings.hideFriendlies(context)
    val matches = stats?.recentMatches.orEmpty().filter { !hideFriendlies || !it.isFriendly }
    val details = stats?.matchDetails.orEmpty().filter { !hideFriendlies || !it.match.isFriendly }
    val summary = remember(matches) { calculateSummary(matches) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 18.dp
        )
    ) {
        item { CorinthiansTopBar("Estatísticas", "Números e autores dos lances", onRefresh = { refresh++ }) }
        if (result != null) item { DataStatus(result.source, result.generatedAt, result.notice, result.isDemo) }
        when {
            state == null -> item { LoadingState() }
            state?.isFailure == true -> item {
                EmptyState(
                    "Estatísticas indisponíveis",
                    state?.exceptionOrNull()?.message ?: "Verifique a conexão.",
                    onRetry = { refresh++ }
                )
            }
            stats == null || matches.isEmpty() -> item {
                EmptyState(
                    "Sem jogos suficientes",
                    "As estatísticas aparecerão após os primeiros resultados.",
                    onRetry = { refresh++ }
                )
            }
            else -> {
                item { AppSectionTitle("Resumo dos últimos ${matches.size} jogos") }
                item {
                    AppCard(accent = CorinthiansColors.Red) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Campanha",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "${summary.wins}V  •  ${summary.draws}E  •  ${summary.losses}D",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Pill("${summary.pointsPercentage}%", CorinthiansColors.Red.copy(alpha = .14f), CorinthiansColors.Red)
                        }
                        Spacer(Modifier.height(18.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricTile("Gols pró", summary.goalsFor.toString(), Modifier.weight(1f))
                            MetricTile("Gols contra", summary.goalsAgainst.toString(), Modifier.weight(1f))
                            MetricTile("Saldo", signed(summary.goalsFor - summary.goalsAgainst), Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            summary.form.forEach { FormBadge(it) }
                        }
                    }
                }

                val visibleCompetitions = stats.competitions.filter {
                    !hideFriendlies || !it.name.contains("amist", ignoreCase = true)
                }
                if (visibleCompetitions.isNotEmpty()) {
                    item { AppSectionTitle("Por competição") }
                    items(visibleCompetitions, key = { it.name }) { competition ->
                        CompetitionStatsCard(competition)
                    }
                }

                item { AppSectionTitle("Estatísticas por partida") }
                if (details.isEmpty()) {
                    items(matches.asReversed(), key = { it.id }) { match -> RecentResultCard(match) }
                } else {
                    items(details.asReversed(), key = { it.match.id }) { detail -> DetailedGameCard(detail) }
                }
            }
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun FormBadge(code: String) {
    val (label, color) = when (code) {
        "W" -> "V" to Color(0xFF198754)
        "D" -> "E" to Color(0xFFD18A00)
        else -> "D" to CorinthiansColors.Red
    }
    Box(
        Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun CompetitionStatsCard(stats: CompetitionStats) {
    AppCard(accent = CorinthiansColors.Red) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stats.name,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${stats.matches} jogos",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "${stats.wins}V • ${stats.draws}E • ${stats.losses}D   |   Gols ${stats.goalsFor}–${stats.goalsAgainst}",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DetailedGameCard(detail: DetailedMatchStats) {
    val match = detail.match
    val importantEvents = detail.events.filter {
        it.type in setOf(
            EventType.Goal,
            EventType.YellowCard,
            EventType.RedCard,
            EventType.Substitution,
            EventType.Penalty
        )
    }
    AppCard(accent = CorinthiansColors.Red) {
        Text(match.competition, color = CorinthiansColors.Red, fontWeight = FontWeight.ExtraBold)
        Text(
            "${match.home}  ${match.scoreHome ?: "–"} x ${match.scoreAway ?: "–"}  ${match.away}",
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            match.date,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium
        )

        if (detail.homeStats != null && detail.awayStats != null) {
            Spacer(Modifier.height(14.dp))
            ComparisonLine("Chutes no gol", detail.homeStats.shotsOnGoal, detail.awayStats.shotsOnGoal)
            ComparisonLine("Chutes para fora", detail.homeStats.shotsOffGoal, detail.awayStats.shotsOffGoal)
            ComparisonLine("Chutes bloqueados", detail.homeStats.blockedShots, detail.awayStats.blockedShots)
            ComparisonLine("Posse", detail.homeStats.ballPossession, detail.awayStats.ballPossession, "%")
            ComparisonLine("Faltas", detail.homeStats.fouls, detail.awayStats.fouls)
            ComparisonLine("Escanteios", detail.homeStats.corners, detail.awayStats.corners)
            ComparisonLine("Impedimentos", detail.homeStats.offsides, detail.awayStats.offsides)
            ComparisonLine("Defesas", detail.homeStats.saves, detail.awayStats.saves)
            ComparisonLine("Cartões amarelos", detail.homeStats.yellowCards, detail.awayStats.yellowCards)
            ComparisonLine("Cartões vermelhos", detail.homeStats.redCards, detail.awayStats.redCards)
        }

        Spacer(Modifier.height(14.dp))
        Text("Gols, cartões e substituições", fontWeight = FontWeight.ExtraBold)
        if (importantEvents.isEmpty()) {
            Text(
                "Nenhum lance desse tipo foi registrado.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            importantEvents.forEach { event ->
                Text(
                    "${event.clock.ifBlank { event.minute.toString() }} ${event.period} • ${event.team.ifBlank { "Jogo" }} — ${event.description}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun ComparisonLine(label: String, home: Int, away: Int, suffix: String = "") {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("$home$suffix", Modifier.weight(1f), fontWeight = FontWeight.Black)
        Text(
            label,
            Modifier.weight(2f),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
        Text("$away$suffix", Modifier.weight(1f), textAlign = TextAlign.End, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun RecentResultCard(match: Match) {
    AppCard(accent = CorinthiansColors.Black) {
        Text(match.competition, color = CorinthiansColors.Red, fontWeight = FontWeight.Bold)
        Text(
            "${match.home}  ${match.scoreHome ?: "–"} x ${match.scoreAway ?: "–"}  ${match.away}",
            fontWeight = FontWeight.Bold
        )
        Text(match.date, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
    }
}

private data class VisibleSummary(
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val pointsPercentage: Int,
    val form: List<String>
)

private fun calculateSummary(matches: List<Match>): VisibleSummary {
    var wins = 0
    var draws = 0
    var losses = 0
    var goalsFor = 0
    var goalsAgainst = 0
    val form = matches.map { match ->
        val home = match.home.contains("Corinthians", true)
        val own = (if (home) match.scoreHome else match.scoreAway) ?: 0
        val rival = (if (home) match.scoreAway else match.scoreHome) ?: 0
        goalsFor += own
        goalsAgainst += rival
        when {
            own > rival -> "W".also { wins++ }
            own == rival -> "D".also { draws++ }
            else -> "L".also { losses++ }
        }
    }
    val percentage = if (matches.isEmpty()) 0 else ((wins * 3 + draws) * 100) / (matches.size * 3)
    return VisibleSummary(wins, draws, losses, goalsFor, goalsAgainst, percentage, form)
}

private fun signed(value: Int): String = if (value > 0) "+$value" else value.toString()
