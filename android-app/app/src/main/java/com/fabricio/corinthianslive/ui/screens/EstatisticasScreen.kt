package com.fabricio.corinthianslive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.fabricio.corinthianslive.data.model.TeamStats
import com.fabricio.corinthianslive.ui.components.AppCard
import com.fabricio.corinthianslive.ui.components.AppSectionTitle
import com.fabricio.corinthianslive.ui.components.CompactEventRow
import com.fabricio.corinthianslive.ui.components.ComparisonStat
import com.fabricio.corinthianslive.ui.components.CorinthiansTopBar
import com.fabricio.corinthianslive.ui.components.DataStatus
import com.fabricio.corinthianslive.ui.components.EmptyState
import com.fabricio.corinthianslive.ui.components.LoadingState
import com.fabricio.corinthianslive.ui.components.Pill
import com.fabricio.corinthianslive.ui.theme.CorinthiansColors

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
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 18.dp)
    ) {
        item { CorinthiansTopBar("Estatísticas", "Desempenho, comparativos e protagonistas", onRefresh = { refresh++ }) }
        if (result != null) item { DataStatus(result.source, result.generatedAt, result.notice, result.isDemo) }
        when {
            state == null -> item { LoadingState() }
            state?.isFailure == true -> item {
                EmptyState("Estatísticas indisponíveis", state?.exceptionOrNull()?.message ?: "Verifique a conexão.", onRetry = { refresh++ })
            }
            stats == null || matches.isEmpty() -> item {
                EmptyState("Sem jogos suficientes", "As estatísticas aparecerão após os primeiros resultados.", onRetry = { refresh++ })
            }
            else -> {
                item { AppSectionTitle("Raio-X dos últimos ${matches.size} jogos") }
                item { PerformanceOverview(summary) }

                val visibleCompetitions = stats.competitions.filter {
                    !hideFriendlies || !it.name.contains("amist", ignoreCase = true)
                }
                if (visibleCompetitions.isNotEmpty()) {
                    item { AppSectionTitle("Desempenho por competição") }
                    items(visibleCompetitions, key = { it.name }) { competition -> CompetitionStatsCard(competition) }
                }

                item { AppSectionTitle("Partida por partida") }
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
private fun PerformanceOverview(summary: VisibleSummary) {
    AppCard(accent = CorinthiansColors.Gold) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(CorinthiansColors.Red, CorinthiansColors.DeepRed))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.EmojiEvents, null, tint = Color.White, modifier = Modifier.size(29.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text("APROVEITAMENTO", color = CorinthiansColors.Gold, style = MaterialTheme.typography.labelSmall)
                Text("${summary.pointsPercentage}%", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
            }
            Pill("${summary.wins}V • ${summary.draws}E • ${summary.losses}D", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            MetricTile("Gols pró", summary.goalsFor.toString(), CorinthiansColors.Success, Modifier.weight(1f))
            MetricTile("Gols contra", summary.goalsAgainst.toString(), CorinthiansColors.Red, Modifier.weight(1f))
            MetricTile("Saldo", signed(summary.goalsFor - summary.goalsAgainst), CorinthiansColors.Gold, Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        Text("SEQUÊNCIA RECENTE", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(9.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            summary.form.forEachIndexed { index, code -> FormBadge(code, index + 1) }
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(17.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, accent.copy(alpha = .2f), RoundedCornerShape(17.dp))
            .padding(horizontal = 8.dp, vertical = 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = accent, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
    }
}

@Composable
private fun FormBadge(code: String, index: Int) {
    val (label, color) = when (code) {
        "W" -> "V" to CorinthiansColors.Success
        "D" -> "E" to CorinthiansColors.Yellow
        else -> "D" to CorinthiansColors.Red
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(color.copy(alpha = .16f))
                .border(1.dp, color.copy(alpha = .55f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = color, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelLarge)
        }
        Text(index.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun CompetitionStatsCard(stats: CompetitionStats) {
    val percentage = if (stats.matches == 0) 0 else ((stats.wins * 3 + stats.draws) * 100) / (stats.matches * 3)
    AppCard(accent = CorinthiansColors.Red) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.EmojiEvents, null, tint = CorinthiansColors.Red)
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(stats.name, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${stats.matches} jogos", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Text("$percentage%", color = CorinthiansColors.Gold, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniMetric("Vitórias", stats.wins, CorinthiansColors.Success, Modifier.weight(1f))
            MiniMetric("Empates", stats.draws, CorinthiansColors.Yellow, Modifier.weight(1f))
            MiniMetric("Derrotas", stats.losses, CorinthiansColors.Red, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Text("Gols ${stats.goalsFor} × ${stats.goalsAgainst}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MiniMetric(label: String, value: Int, color: Color, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = .08f)).padding(vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value.toString(), color = color, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun DetailedGameCard(detail: DetailedMatchStats) {
    val match = detail.match
    val importantEvents = detail.events.filter {
        it.type in setOf(EventType.Goal, EventType.YellowCard, EventType.RedCard, EventType.Substitution, EventType.Penalty)
    }
    val goals = importantEvents.count { it.type == EventType.Goal || it.type == EventType.Penalty }
    val cards = importantEvents.count { it.type == EventType.YellowCard || it.type == EventType.RedCard }
    val substitutions = importantEvents.count { it.type == EventType.Substitution }
    val homeIsCorinthians = match.home.contains("Corinthians", true)

    AppCard(accent = CorinthiansColors.Red) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(match.competition.uppercase(), color = CorinthiansColors.Red, style = MaterialTheme.typography.labelSmall)
                Text(match.date, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Pill("ENCERRADO", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(match.home, modifier = Modifier.weight(1f), fontWeight = if (homeIsCorinthians) FontWeight.Black else FontWeight.Bold)
            Box(
                Modifier.clip(RoundedCornerShape(15.dp)).background(MaterialTheme.colorScheme.background).padding(horizontal = 15.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${match.scoreHome ?: "–"}  ×  ${match.scoreAway ?: "–"}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
            }
            Text(
                match.away,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
                fontWeight = if (!homeIsCorinthians) FontWeight.Black else FontWeight.Bold
            )
        }

        if (detail.homeStats != null && detail.awayStats != null) {
            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(15.dp))
            Text("COMPARATIVO DA PARTIDA", color = CorinthiansColors.Gold, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(5.dp))
            ComparisonStat("Chutes no gol", detail.homeStats.shotsOnGoal, detail.awayStats.shotsOnGoal, homeIsCorinthians = homeIsCorinthians)
            ComparisonStat("Chutes para fora", detail.homeStats.shotsOffGoal, detail.awayStats.shotsOffGoal, homeIsCorinthians = homeIsCorinthians)
            ComparisonStat("Chutes bloqueados", detail.homeStats.blockedShots, detail.awayStats.blockedShots, homeIsCorinthians = homeIsCorinthians)
            ComparisonStat("Posse de bola", detail.homeStats.ballPossession, detail.awayStats.ballPossession, "%", homeIsCorinthians)
            ComparisonStat("Faltas", detail.homeStats.fouls, detail.awayStats.fouls, homeIsCorinthians = homeIsCorinthians)
            ComparisonStat("Escanteios", detail.homeStats.corners, detail.awayStats.corners, homeIsCorinthians = homeIsCorinthians)
            ComparisonStat("Impedimentos", detail.homeStats.offsides, detail.awayStats.offsides, homeIsCorinthians = homeIsCorinthians)
            ComparisonStat("Defesas", detail.homeStats.saves, detail.awayStats.saves, homeIsCorinthians = homeIsCorinthians)
            ComparisonStat("Cartões amarelos", detail.homeStats.yellowCards, detail.awayStats.yellowCards, homeIsCorinthians = homeIsCorinthians)
            ComparisonStat("Cartões vermelhos", detail.homeStats.redCards, detail.awayStats.redCards, homeIsCorinthians = homeIsCorinthians)
        }

        Spacer(Modifier.height(18.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(15.dp))
        Text("MOMENTOS DECISIVOS", color = CorinthiansColors.Gold, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(11.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EventCount("Gols", goals, Icons.Default.SportsSoccer, CorinthiansColors.Red, Modifier.weight(1f))
            EventCount("Cartões", cards, Icons.Default.Style, CorinthiansColors.Yellow, Modifier.weight(1f))
            EventCount("Trocas", substitutions, Icons.Default.SwapHoriz, Color(0xFF5F9CFF), Modifier.weight(1f))
        }
        if (importantEvents.isEmpty()) {
            Text(
                "Nenhum gol, cartão ou substituição foi registrado.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 12.dp)
            )
        } else {
            importantEvents.forEach { event -> CompactEventRow(event) }
        }
    }
}

@Composable
private fun EventCount(label: String, value: Int, icon: ImageVector, color: Color, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(15.dp)).background(color.copy(alpha = .08f))
            .border(1.dp, color.copy(alpha = .16f), RoundedCornerShape(15.dp)).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(19.dp))
        Text(value.toString(), color = color, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun RecentResultCard(match: Match) {
    val homeIsCorinthians = match.home.contains("Corinthians", true)
    AppCard(accent = MaterialTheme.colorScheme.outlineVariant) {
        Text(match.competition.uppercase(), color = CorinthiansColors.Red, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(match.home, Modifier.weight(1f), fontWeight = if (homeIsCorinthians) FontWeight.Black else FontWeight.Bold)
            Pill("${match.scoreHome ?: "–"} × ${match.scoreAway ?: "–"}", MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.onBackground)
            Text(match.away, Modifier.weight(1f), textAlign = TextAlign.End, fontWeight = if (!homeIsCorinthians) FontWeight.Black else FontWeight.Bold)
        }
        Spacer(Modifier.height(7.dp))
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
