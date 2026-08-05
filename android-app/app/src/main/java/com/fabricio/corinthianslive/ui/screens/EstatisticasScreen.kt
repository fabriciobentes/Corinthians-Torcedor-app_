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
import androidx.compose.foundation.layout.width
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
import com.fabricio.corinthianslive.data.CorinthiansRepository
import com.fabricio.corinthianslive.data.model.CompetitionStats
import com.fabricio.corinthianslive.data.model.Match
import com.fabricio.corinthianslive.data.model.RepositoryResult
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

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 18.dp
        )
    ) {
        item { CorinthiansTopBar("Estatísticas", "Desempenho recente do Timão", onRefresh = { refresh++ }) }
        if (result != null) item { DataStatus(result.source, result.generatedAt, result.notice, result.isDemo) }
        when {
            state == null -> item { LoadingState() }
            state?.isFailure == true -> item {
                EmptyState("Estatísticas indisponíveis", state?.exceptionOrNull()?.message ?: "Verifique a conexão.", onRetry = { refresh++ })
            }
            stats == null || stats.summary.matches == 0 -> item {
                EmptyState("Sem jogos suficientes", "As estatísticas aparecerão após os primeiros resultados.", onRetry = { refresh++ })
            }
            else -> {
                item { AppSectionTitle("Últimos ${stats.window} jogos") }
                item {
                    AppCard(accent = CorinthiansColors.Red) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Campanha", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${stats.summary.wins}V  •  ${stats.summary.draws}E  •  ${stats.summary.losses}D", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                            }
                            Pill("${stats.summary.pointsPercentage}%", CorinthiansColors.Red.copy(alpha = .14f), CorinthiansColors.Red)
                        }
                        Spacer(Modifier.height(18.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricTile("Gols pró", stats.summary.goalsFor.toString(), Modifier.weight(1f))
                            MetricTile("Gols contra", stats.summary.goalsAgainst.toString(), Modifier.weight(1f))
                            MetricTile("Saldo", signed(stats.summary.goalDifference), Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricTile("Média pró", decimal(stats.summary.averageGoalsFor), Modifier.weight(1f))
                            MetricTile("Sem sofrer", stats.summary.cleanSheets.toString(), Modifier.weight(1f))
                            MetricTile("Marcou em", stats.summary.scoringGames.toString(), Modifier.weight(1f))
                        }
                    }
                }

                item { AppSectionTitle("Forma recente") }
                item {
                    AppCard(accent = CorinthiansColors.Black) {
                        Text("Do jogo mais antigo para o mais recente", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            stats.form.forEach { FormBadge(it) }
                        }
                    }
                }

                item { AppSectionTitle("Por competição") }
                items(stats.competitions, key = { it.name }) { competition -> CompetitionStatsCard(competition) }

                item { AppSectionTitle("Resultados analisados") }
                items(stats.recentMatches.asReversed(), key = { it.id }) { match -> RecentResultCard(match) }
            }
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
private fun FormBadge(code: String) {
    val (label, color) = when (code) {
        "W" -> "V" to Color(0xFF198754)
        "D" -> "E" to Color(0xFFD18A00)
        else -> "D" to CorinthiansColors.Red
    }
    Box(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(color), contentAlignment = Alignment.Center) {
        Text(label, color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun CompetitionStatsCard(stats: CompetitionStats) {
    AppCard(accent = CorinthiansColors.Red) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stats.name, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(10.dp))
            Text("${stats.matches} jogos", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.height(8.dp))
        Text("${stats.wins}V • ${stats.draws}E • ${stats.losses}D   |   Gols ${stats.goalsFor}–${stats.goalsAgainst}", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RecentResultCard(match: Match) {
    val corinthiansHome = match.home.equals("Corinthians", true)
    val goalsFor = if (corinthiansHome) match.scoreHome else match.scoreAway
    val goalsAgainst = if (corinthiansHome) match.scoreAway else match.scoreHome
    val (resultLabel, resultColor) = when {
        goalsFor == null || goalsAgainst == null -> "—" to MaterialTheme.colorScheme.onSurfaceVariant
        goalsFor > goalsAgainst -> "V" to Color(0xFF198754)
        goalsFor == goalsAgainst -> "E" to Color(0xFFD18A00)
        else -> "D" to CorinthiansColors.Red
    }
    AppCard(accent = resultColor) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(resultColor), contentAlignment = Alignment.Center) {
                Text(resultLabel, color = Color.White, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(match.competition, color = CorinthiansColors.Red, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${match.home}  ${match.scoreHome ?: "–"} x ${match.scoreAway ?: "–"}  ${match.away}", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Text(match.date.substringAfter(", "), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun signed(value: Int): String = if (value > 0) "+$value" else value.toString()
private fun decimal(value: Double): String = String.format(Locale.forLanguageTag("pt-BR"), "%.1f", value)
