package com.fabricio.corinthianslive.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.fabricio.corinthianslive.data.model.APP_ZONE_ID
import com.fabricio.corinthianslive.data.model.LiveContent
import com.fabricio.corinthianslive.data.model.LiveMatch
import com.fabricio.corinthianslive.data.model.Player
import com.fabricio.corinthianslive.data.model.RepositoryResult
import com.fabricio.corinthianslive.data.model.TeamMatchStats
import com.fabricio.corinthianslive.data.model.TeamSquad
import com.fabricio.corinthianslive.ui.components.AppCard
import com.fabricio.corinthianslive.ui.components.AppSectionTitle
import com.fabricio.corinthianslive.ui.components.ComparisonStat
import com.fabricio.corinthianslive.ui.components.CorinthiansTopBar
import com.fabricio.corinthianslive.ui.components.DataStatus
import com.fabricio.corinthianslive.ui.components.EmptyState
import com.fabricio.corinthianslive.ui.components.EventCard
import com.fabricio.corinthianslive.ui.components.LoadingState
import com.fabricio.corinthianslive.ui.components.Pill
import com.fabricio.corinthianslive.ui.components.TeamBlock
import com.fabricio.corinthianslive.ui.theme.CorinthiansColors
import com.fabricio.corinthianslive.notifications.GameNotificationManager
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZonedDateTime

@Composable
fun AoVivoScreen(contentPadding: PaddingValues) {
    val context = LocalContext.current
    val repository = remember { CorinthiansRepository(context.applicationContext) }
    var refresh by remember { mutableIntStateOf(0) }
    val state by produceState<Result<RepositoryResult<LiveContent>>?>(null, refresh) {
        while (true) {
            value = runCatching { repository.liveRealtime() }
            delay(10_000)
        }
    }
    val result = state?.getOrNull()
    val rawLive = result?.data?.match
    val live = rawLive?.takeUnless {
        AppSettings.hideFriendlies(context) && it.competition.contains("amist", ignoreCase = true)
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 18.dp
        )
    ) {
        item { CorinthiansTopBar("Ao vivo", "Sincronização direta a cada 10 segundos", onRefresh = { refresh++ }) }
        if (result != null) item { DataStatus(result.source, result.generatedAt, result.notice, result.isDemo) }
        when {
            state == null -> item { LoadingState() }
            state?.isFailure == true -> item {
                EmptyState(
                    "Não foi possível atualizar",
                    state?.exceptionOrNull()?.message ?: "Verifique a conexão.",
                    onRetry = { refresh++ }
                )
            }
            live == null -> item {
                EmptyState(
                    "Nenhum jogo no momento",
                    "O pré-jogo aparecerá aqui antes da partida e continuará durante toda a transmissão.",
                    onRetry = { refresh++ }
                )
            }
            else -> {
                item { LiveScoreCard(live) }

                val homeSquad = result?.data?.homeSquad
                val awaySquad = result?.data?.awaySquad
                if (homeSquad != null || awaySquad != null) {
                    item {
                        AppSectionTitle(result?.data?.lineupStatus.ifNullOrBlank("Escalações"))
                    }
                    homeSquad?.let { squad -> item { TacticalField(squad) } }
                    awaySquad?.let { squad -> item { TacticalField(squad) } }
                } else {
                    item {
                        AppSectionTitle("Escalações e relacionados")
                        EmptyState(
                            "Aguardando divulgação",
                            "A provável escalação, os relacionados e a escalação oficial aparecerão assim que forem publicados."
                        )
                    }
                }

                val homeStats = result?.data?.homeStats
                val awayStats = result?.data?.awayStats
                if (homeStats != null && awayStats != null) {
                    item { AppSectionTitle("Estatísticas da partida") }
                    item { MatchStatsCard(live, homeStats, awayStats) }
                }

                item { AppSectionTitle("Lances minuto a minuto") }
                if (result?.data?.events.isNullOrEmpty()) {
                    item {
                        EmptyState(
                            "Aguardando lances",
                            "Os chutes, faltas, cartões, gols, substituições e demais eventos aparecerão automaticamente."
                        )
                    }
                } else {
                    items(
                        result!!.data.events.asReversed(),
                        key = { it.id }
                    ) { event -> EventCard(event) }
                }
            }
        }
    }
}

@Composable
private fun LiveScoreCard(live: LiveMatch) {
    val isLive = live.isLiveNow()
    AppSectionTitle(if (isLive) "Partida em andamento" else "Pré-jogo")
    AppCard(accent = CorinthiansColors.Red) {
        Text(live.competition, color = CorinthiansColors.Red, fontWeight = FontWeight.ExtraBold)
        Text(
            listOf(live.stadium, live.city).filter { it.isNotBlank() }.joinToString(" • "),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TeamBlock(live.home, live.home.equals("Corinthians", true), false, Modifier.weight(1f))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Pill(
                    if (isLive) "${live.minute}' • AO VIVO" else live.statusLong.ifBlank { "PRÉ-JOGO" },
                    if (isLive) CorinthiansColors.Red else CorinthiansColors.Black,
                    CorinthiansColors.White
                )
                Spacer(Modifier.height(8.dp))
                Text("${live.scoreHome}  x  ${live.scoreAway}", fontWeight = FontWeight.Black)
            }
            TeamBlock(live.away, live.away.equals("Corinthians", true), true, Modifier.weight(1f))
        }
    }
}

@Composable
private fun TacticalField(squad: TeamSquad) {
    val formationCounts = squad.formation.split("-").mapNotNull { it.toIntOrNull() }
    val starters = squad.starters
    val rows = buildList {
        if (starters.isNotEmpty()) add(starters.take(1))
        var cursor = 1
        formationCounts.forEach { count ->
            add(starters.drop(cursor).take(count))
            cursor += count
        }
        if (cursor < starters.size) add(starters.drop(cursor))
    }

    AppCard(accent = CorinthiansColors.Black) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(squad.teamName, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                if (squad.coach.isNotBlank()) {
                    Text(
                        "Técnico: ${squad.coach}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (squad.formation.isNotBlank()) {
                Pill(squad.formation, CorinthiansColors.Red, CorinthiansColors.White)
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier.fillMaxWidth().height(390.dp).clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF237A3B))
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val white = Color.White.copy(alpha = .65f)
                drawRect(white, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))
                drawLine(white, start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f), end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2f), strokeWidth = 3f)
                drawCircle(white, radius = size.width * .13f, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))
            }
            Column(
                Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                rows.forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { player -> PlayerMarker(player) }
                    }
                }
            }
        }
        if (squad.bench.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("Relacionados no banco", fontWeight = FontWeight.ExtraBold)
            Text(
                squad.bench.joinToString(" • ") { player ->
                    listOf(player.shirtNumber, player.name).filter { it.isNotBlank() }.joinToString(" ")
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun PlayerMarker(player: Player) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.size(width = 62.dp, height = 58.dp)) {
        Box(
            Modifier.size(30.dp).clip(CircleShape).background(CorinthiansColors.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                player.shirtNumber.ifBlank { "•" },
                color = Color.White,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.labelSmall
            )
        }
        Text(
            player.name.substringBefore(" "),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MatchStatsCard(home: LiveMatch, homeStats: TeamMatchStats, awayStats: TeamMatchStats) {
    AppCard(accent = CorinthiansColors.Red) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(home.home, modifier = Modifier.weight(1f), fontWeight = FontWeight.Black)
            Text(home.away, modifier = Modifier.weight(1f), fontWeight = FontWeight.Black, textAlign = TextAlign.End)
        }
        Spacer(Modifier.height(14.dp))
        val homeIsCorinthians = home.home.contains("Corinthians", true)
        ComparisonStat("Chutes no gol", homeStats.shotsOnGoal, awayStats.shotsOnGoal, homeIsCorinthians = homeIsCorinthians)
        ComparisonStat("Chutes para fora", homeStats.shotsOffGoal, awayStats.shotsOffGoal, homeIsCorinthians = homeIsCorinthians)
        ComparisonStat("Chutes bloqueados", homeStats.blockedShots, awayStats.blockedShots, homeIsCorinthians = homeIsCorinthians)
        ComparisonStat("Posse de bola", homeStats.ballPossession, awayStats.ballPossession, "%", homeIsCorinthians)
        ComparisonStat("Faltas", homeStats.fouls, awayStats.fouls, homeIsCorinthians = homeIsCorinthians)
        ComparisonStat("Escanteios", homeStats.corners, awayStats.corners, homeIsCorinthians = homeIsCorinthians)
        ComparisonStat("Impedimentos", homeStats.offsides, awayStats.offsides, homeIsCorinthians = homeIsCorinthians)
        ComparisonStat("Defesas", homeStats.saves, awayStats.saves, homeIsCorinthians = homeIsCorinthians)
        ComparisonStat("Cartões amarelos", homeStats.yellowCards, awayStats.yellowCards, homeIsCorinthians = homeIsCorinthians)
        ComparisonStat("Cartões vermelhos", homeStats.redCards, awayStats.redCards, homeIsCorinthians = homeIsCorinthians)
    }
}

private fun LiveMatch.isLiveNow(): Boolean {
    if (statusShort in setOf("LIVE", "1H", "HT", "2H", "ET", "P", "INT")) return true
    val start = runCatching { OffsetDateTime.parse(kickoff).atZoneSameInstant(APP_ZONE_ID) }.getOrNull() ?: return false
    val now = ZonedDateTime.now(APP_ZONE_ID)
    return !now.isBefore(start) && now.isBefore(start.plusHours(4))
}

private fun String?.ifNullOrBlank(fallback: String): String =
    if (this.isNullOrBlank()) fallback else this
