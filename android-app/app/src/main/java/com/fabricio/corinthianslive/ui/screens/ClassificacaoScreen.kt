package com.fabricio.corinthianslive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.unit.dp
import com.fabricio.corinthianslive.data.AppSettings
import com.fabricio.corinthianslive.data.CorinthiansRepository
import com.fabricio.corinthianslive.data.model.BracketGame
import com.fabricio.corinthianslive.data.model.CompetitionTable
import com.fabricio.corinthianslive.data.model.RepositoryResult
import com.fabricio.corinthianslive.ui.components.AppCard
import com.fabricio.corinthianslive.ui.components.AppSectionTitle
import com.fabricio.corinthianslive.ui.components.CorinthiansTopBar
import com.fabricio.corinthianslive.ui.components.DataStatus
import com.fabricio.corinthianslive.ui.components.EmptyState
import com.fabricio.corinthianslive.ui.components.LoadingState
import com.fabricio.corinthianslive.ui.components.Pill
import com.fabricio.corinthianslive.ui.components.StandingRow
import com.fabricio.corinthianslive.ui.components.TableHeader
import com.fabricio.corinthianslive.ui.theme.CorinthiansColors

@Composable
fun ClassificacaoScreen(contentPadding: PaddingValues) {
    val context = LocalContext.current
    val repository = remember { CorinthiansRepository(context.applicationContext) }
    var refresh by remember { mutableIntStateOf(0) }
    var selectedName by remember { mutableStateOf<String?>(null) }
    val state by produceState<Result<RepositoryResult<List<CompetitionTable>>>?>(null, refresh) {
        value = runCatching { repository.standings() }
    }
    val result = state?.getOrNull()
    val hideFriendlies = AppSettings.hideFriendlies(context)
    val competitions = result?.data.orEmpty().filter {
        !hideFriendlies || !it.name.contains("amist", ignoreCase = true)
    }
    val selected = competitions.firstOrNull { it.name == selectedName } ?: competitions.firstOrNull()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 18.dp)
    ) {
        item { CorinthiansTopBar("Tabela", "Classificações, fases e caminhos até a taça", onRefresh = { refresh++ }) }
        if (result != null) item { DataStatus(result.source, result.generatedAt, result.notice, result.isDemo) }
        when {
            state == null -> item { LoadingState() }
            state?.isFailure == true -> item {
                EmptyState("Tabelas indisponíveis", state?.exceptionOrNull()?.message ?: "Verifique a conexão.", onRetry = { refresh++ })
            }
            competitions.isEmpty() -> item {
                EmptyState(
                    "Tabelas ainda não publicadas",
                    "As competições aparecerão automaticamente assim que divulgarem classificação ou chaveamento.",
                    onRetry = { refresh++ }
                )
            }
            selected != null -> {
                item {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        competitions.forEach { competition ->
                            val active = competition.name == selected.name
                            FilterChip(
                                selected = active,
                                onClick = { selectedName = competition.name },
                                label = { Text(competition.name, fontWeight = FontWeight.ExtraBold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
                item { CompetitionHero(selected) }

                selected.groups.forEach { group ->
                    item { AppSectionTitle(group.name) }
                    item {
                        AppCard(accent = CorinthiansColors.Red) {
                            TableHeader()
                            Spacer(Modifier.height(11.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                group.entries.forEach { standing ->
                                    StandingRow(standing, standing.teamName.contains("Corinthians", true))
                                }
                            }
                        }
                    }
                }

                selected.brackets.forEach { round ->
                    item { AppSectionTitle(round.name) }
                    round.ties.forEach { tie ->
                        item(key = "tie-${selected.name}-${round.name}-${tie.name}") {
                            AppCard(accent = CorinthiansColors.Gold) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.EmojiEvents, null, tint = CorinthiansColors.Gold)
                                    }
                                    Spacer(Modifier.padding(horizontal = 6.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(tie.name.ifBlank { "Confronto" }, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                                        Text("${tie.games.size} partida(s)", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    tie.games.forEach { game -> BracketGameRow(game) }
                                }
                            }
                        }
                    }
                }

                if (selected.groups.isEmpty() && selected.brackets.isEmpty()) {
                    item {
                        EmptyState("Formato ainda não divulgado", "A competição aparecerá aqui assim que publicar tabela ou confrontos.")
                    }
                }
                item {
                    Text(
                        "P: pontos • J: jogos • SG: saldo de gols",
                        Modifier.fillMaxWidth().padding(18.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun CompetitionHero(competition: CompetitionTable) {
    AppCard(accent = CorinthiansColors.Red) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(CorinthiansColors.Red),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.EmojiEvents, null, tint = Color.White, modifier = Modifier.size(29.dp))
            }
            Spacer(Modifier.padding(horizontal = 7.dp))
            Column(Modifier.weight(1f)) {
                Text("COMPETIÇÃO", color = CorinthiansColors.Gold, style = MaterialTheme.typography.labelSmall)
                Text(competition.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                if (competition.phase.isNotBlank()) {
                    Text(competition.phase, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
            Pill(
                if (competition.brackets.isNotEmpty()) "MATA-MATA" else "PONTOS",
                CorinthiansColors.Wine,
                CorinthiansColors.Red
            )
        }
    }
}

@Composable
private fun BracketGameRow(game: BracketGame) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(17.dp))
            .padding(13.dp)
    ) {
        if (game.date.isNotBlank()) {
            Text(game.date.uppercase(), color = CorinthiansColors.Gold, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(7.dp))
        }
        TeamScoreLine(game.home, game.scoreHome, game.home.contains("Corinthians", true))
        HorizontalDivider(Modifier.padding(vertical = 7.dp), color = MaterialTheme.colorScheme.outlineVariant)
        TeamScoreLine(game.away, game.scoreAway, game.away.contains("Corinthians", true))
        if (game.penaltyHome != null && game.penaltyAway != null) {
            Spacer(Modifier.height(9.dp))
            Pill("Pênaltis ${game.penaltyHome} × ${game.penaltyAway}", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun TeamScoreLine(name: String, score: Int?, highlight: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(name, Modifier.weight(1f), fontWeight = if (highlight) FontWeight.Black else FontWeight.SemiBold)
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(11.dp))
                .background(if (highlight) CorinthiansColors.Red else MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                score?.toString() ?: "–",
                fontWeight = FontWeight.Black,
                color = if (highlight) Color.White else MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
