package com.fabricio.corinthianslive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fabricio.corinthianslive.data.CorinthiansRepository
import com.fabricio.corinthianslive.data.model.Match
import com.fabricio.corinthianslive.data.model.RepositoryResult
import com.fabricio.corinthianslive.ui.components.*
import com.fabricio.corinthianslive.ui.theme.CorinthiansColors

@Composable fun JogosScreen(contentPadding: PaddingValues) {
    val context = LocalContext.current
    val repository = remember { CorinthiansRepository(context.applicationContext) }
    var refresh by remember { mutableIntStateOf(0) }
    val state by produceState<Result<RepositoryResult<List<Match>>>?>(null, refresh) { value = runCatching { repository.fixtures() } }
    val result = state?.getOrNull()
    val finishedCodes = setOf("FT", "AET", "PEN", "WO")
    val liveCodes = setOf("1H", "HT", "2H", "ET", "BT", "P", "SUSP", "INT")
    val upcoming = result?.data?.filter { it.statusShort !in finishedCodes && it.statusShort !in liveCodes }?.take(20).orEmpty()
    val finished = result?.data?.filter { it.statusShort in finishedCodes }?.takeLast(10)?.reversed().orEmpty()
    val competitions = result?.data?.map { it.competition }?.distinct().orEmpty()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = contentPadding.calculateTopPadding(), bottom = contentPadding.calculateBottomPadding() + 18.dp)) {
        item { CorinthiansTopBar("Jogos", "Todas as competições do Timão", onRefresh = { refresh++ }) }
        if (result != null) item { DataStatus(result.source, result.generatedAt, result.notice, result.isDemo) }
        when {
            state == null -> item { LoadingState() }
            state?.isFailure == true -> item { EmptyState("Não foi possível carregar", state?.exceptionOrNull()?.message ?: "Verifique a conexão.", onRetry = { refresh++ }) }
            upcoming.isEmpty() && finished.isEmpty() -> item { EmptyState("Nenhum jogo encontrado", "A fonte ainda não publicou partidas para esta temporada.", onRetry = { refresh++ }) }
            else -> {
                if (competitions.isNotEmpty()) {
                    item {
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            competitions.forEach { competition -> Pill(competition, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
                if (upcoming.isNotEmpty()) { item { AppSectionTitle("Próximos jogos") }; itemsIndexed(upcoming, key = { index, match -> "next-${match.id}-$index" }) { _, match -> MatchCard(match) } }
                if (finished.isNotEmpty()) { item { AppSectionTitle("Últimos resultados") }; itemsIndexed(finished, key = { index, match -> "last-${match.id}-$index" }) { _, match -> MatchCard(match) } }
            }
        }
    }
}

@Composable private fun MatchCard(match: Match) {
    AppCard(accent = CorinthiansColors.Red) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(match.competition, color = CorinthiansColors.Red, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp)); Pill("${match.date} • ${match.time}", MaterialTheme.colorScheme.onSurface.copy(alpha = .08f), MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            TeamBlock(match.home, match.home.equals("Corinthians", true), false, Modifier.weight(1f))
            Column(Modifier.padding(horizontal = 10.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                val score = if (match.scoreHome != null && match.scoreAway != null) "${match.scoreHome}  x  ${match.scoreAway}" else "vs"
                Text(score, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(6.dp)); Box(Modifier.height(2.dp).width(44.dp).background(CorinthiansColors.Red))
            }
            TeamBlock(match.away, match.away.equals("Corinthians", true), true, Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            InfoItem("Local", match.stadium, Modifier.weight(1f)); Spacer(Modifier.width(12.dp)); InfoItem("Cidade", match.city.ifBlank { "—" }, Modifier.weight(1f), true)
        }
    }
}
