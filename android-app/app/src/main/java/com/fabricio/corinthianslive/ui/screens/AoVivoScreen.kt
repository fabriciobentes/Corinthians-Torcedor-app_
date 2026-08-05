package com.fabricio.corinthianslive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fabricio.corinthianslive.data.CorinthiansRepository
import com.fabricio.corinthianslive.data.model.LiveContent
import com.fabricio.corinthianslive.data.model.RepositoryResult
import com.fabricio.corinthianslive.ui.components.*
import com.fabricio.corinthianslive.ui.theme.CorinthiansColors

@Composable fun AoVivoScreen(contentPadding: PaddingValues) {
    val context = LocalContext.current
    val repository = remember { CorinthiansRepository(context.applicationContext) }
    var refresh by remember { mutableIntStateOf(0) }
    val state by produceState<Result<RepositoryResult<LiveContent>>?>(null, refresh) { value = runCatching { repository.live() } }
    val result = state?.getOrNull()
    val live = result?.data?.match

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = contentPadding.calculateTopPadding(), bottom = contentPadding.calculateBottomPadding() + 18.dp)) {
        item { CorinthiansTopBar("Ao vivo", "Placar e eventos da partida", onRefresh = { refresh++ }) }
        if (result != null) item { DataStatus(result.source, result.generatedAt, result.notice, result.isDemo) }
        when {
            state == null -> item { LoadingState() }
            state?.isFailure == true -> item { EmptyState("Não foi possível atualizar", state?.exceptionOrNull()?.message ?: "Verifique a conexão.", onRetry = { refresh++ }) }
            live == null -> item { EmptyState("Nenhum jogo ao vivo", "Quando o Corinthians entrar em campo, o placar aparecerá aqui.", onRetry = { refresh++ }) }
            else -> {
                item {
                    AppSectionTitle("Partida em andamento")
                    AppCard(accent = CorinthiansColors.Red) {
                        Text(live.competition, color = CorinthiansColors.Red, fontWeight = FontWeight.ExtraBold)
                        Text(live.stadium, color = CorinthiansColors.GrayText, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            TeamBlock(live.home, live.home.equals("Corinthians", true), false, Modifier.weight(1f))
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Pill("${live.minute}' • AO VIVO", CorinthiansColors.Red, CorinthiansColors.White)
                                Spacer(Modifier.height(8.dp)); Text("${live.scoreHome}  x  ${live.scoreAway}", fontWeight = FontWeight.Black)
                                Spacer(Modifier.height(8.dp)); Box(Modifier.height(2.dp).width(72.dp).background(CorinthiansColors.Red))
                            }
                            TeamBlock(live.away, live.away.equals("Corinthians", true), true, Modifier.weight(1f))
                        }
                    }
                }
                item { AppSectionTitle("Lances") }
                if (result.data.events.isEmpty()) item { EmptyState("Lances indisponíveis", "O placar está ao vivo, mas a competição ainda não forneceu os eventos.") }
                else items(result.data.events, key = { "${it.minute}-${it.team}-${it.description}" }) { EventCard(it) }
            }
        }
    }
}
