package com.fabricio.corinthianslive.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 18.dp
        )
    ) {
        item { CorinthiansTopBar("Tabela", "Classificações e chaveamentos do Timão", onRefresh = { refresh++ }) }
        if (result != null) item { DataStatus(result.source, result.generatedAt, result.notice, result.isDemo) }
        when {
            state == null -> item { LoadingState() }
            state?.isFailure == true -> item {
                EmptyState(
                    "Tabelas indisponíveis",
                    state?.exceptionOrNull()?.message ?: "Verifique a conexão.",
                    onRetry = { refresh++ }
                )
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
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        competitions.forEach { competition ->
                            FilterChip(
                                selected = competition.name == selected.name,
                                onClick = { selectedName = competition.name },
                                label = { Text(competition.name) }
                            )
                        }
                    }
                }
                item {
                    AppSectionTitle(selected.name)
                    if (selected.phase.isNotBlank()) {
                        Text(
                            selected.phase,
                            modifier = Modifier.padding(horizontal = 18.dp),
                            color = CorinthiansColors.Red,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                selected.groups.forEach { group ->
                    item { AppSectionTitle(group.name) }
                    item {
                        AppCard(accent = CorinthiansColors.Red) {
                            TableHeader()
                            Spacer(Modifier.height(10.dp))
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
                            AppCard(accent = CorinthiansColors.Red) {
                                Text(tie.name, fontWeight = FontWeight.ExtraBold)
                                Spacer(Modifier.height(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    tie.games.forEach { game -> BracketGameRow(game) }
                                }
                            }
                        }
                    }
                }

                if (selected.groups.isEmpty() && selected.brackets.isEmpty()) {
                    item {
                        EmptyState(
                            "Formato ainda não divulgado",
                            "A competição já foi detectada e aparecerá aqui assim que publicar tabela ou confrontos."
                        )
                    }
                }
                item {
                    Text(
                        "P: pontos • J: jogos • SG: saldo de gols.",
                        Modifier.padding(18.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun BracketGameRow(game: BracketGame) {
    Column(Modifier.fillMaxWidth()) {
        if (game.date.isNotBlank()) {
            Text(
                game.date,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                game.home,
                modifier = Modifier.weight(1f),
                fontWeight = if (game.home.contains("Corinthians", true)) FontWeight.Black else FontWeight.SemiBold
            )
            Text(game.scoreHome?.toString() ?: "–", fontWeight = FontWeight.Black)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                game.away,
                modifier = Modifier.weight(1f),
                fontWeight = if (game.away.contains("Corinthians", true)) FontWeight.Black else FontWeight.SemiBold
            )
            Text(game.scoreAway?.toString() ?: "–", fontWeight = FontWeight.Black)
        }
        if (game.penaltyHome != null && game.penaltyAway != null) {
            Text(
                "Pênaltis: ${game.penaltyHome} x ${game.penaltyAway}",
                color = CorinthiansColors.Red,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
