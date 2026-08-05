package com.fabricio.corinthianslive.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fabricio.corinthianslive.data.CorinthiansRepository
import com.fabricio.corinthianslive.data.model.RepositoryResult
import com.fabricio.corinthianslive.data.model.Standing
import com.fabricio.corinthianslive.ui.components.*
import com.fabricio.corinthianslive.ui.theme.CorinthiansColors

@Composable fun ClassificacaoScreen(contentPadding: PaddingValues) {
    val context = LocalContext.current
    val repository = remember { CorinthiansRepository(context.applicationContext) }
    var refresh by remember { mutableIntStateOf(0) }
    val state by produceState<Result<RepositoryResult<List<Standing>>>?>(null, refresh) { value = runCatching { repository.standings() } }
    val result = state?.getOrNull()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = contentPadding.calculateTopPadding(), bottom = contentPadding.calculateBottomPadding() + 18.dp)) {
        item { CorinthiansTopBar("Tabela", "Brasileirão Série A", onRefresh = { refresh++ }) }
        if (result != null) item { DataStatus(result.source, result.generatedAt, result.notice, result.isDemo) }
        when {
            state == null -> item { LoadingState() }
            state?.isFailure == true -> item { EmptyState("Tabela indisponível", state?.exceptionOrNull()?.message ?: "Verifique a conexão.", onRetry = { refresh++ }) }
            result?.data.isNullOrEmpty() -> item { EmptyState("Tabela ainda não publicada", "A classificação aparecerá assim que a competição disponibilizar os dados.", onRetry = { refresh++ }) }
            else -> {
                item { AppSectionTitle("Classificação") }
                item {
                    AppCard(accent = CorinthiansColors.Red) {
                        TableHeader(); Spacer(Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            result!!.data.forEach { standing -> StandingRow(standing, standing.teamName.equals("Corinthians", true)) }
                        }
                    }
                }
                item { Text("P: pontos • J: jogos • V: vitórias • E: empates • D: derrotas • SG: saldo de gols", Modifier.padding(18.dp), color = CorinthiansColors.GrayText, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium) }
            }
        }
    }
}
