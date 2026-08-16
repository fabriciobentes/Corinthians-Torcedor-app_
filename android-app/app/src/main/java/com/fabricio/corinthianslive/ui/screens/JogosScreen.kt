package com.fabricio.corinthianslive.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fabricio.corinthianslive.data.AppSettings
import com.fabricio.corinthianslive.data.CorinthiansRepository
import com.fabricio.corinthianslive.data.model.APP_ZONE_ID
import com.fabricio.corinthianslive.data.model.Match
import com.fabricio.corinthianslive.data.model.RepositoryResult
import com.fabricio.corinthianslive.data.model.kickoffAtManaus
import com.fabricio.corinthianslive.data.model.resolvedStatus
import com.fabricio.corinthianslive.data.model.resolvedStatusLabel
import com.fabricio.corinthianslive.ui.components.AppCard
import com.fabricio.corinthianslive.ui.components.AppSectionTitle
import com.fabricio.corinthianslive.ui.components.CorinthiansTopBar
import com.fabricio.corinthianslive.ui.components.DataStatus
import com.fabricio.corinthianslive.ui.components.EmptyState
import com.fabricio.corinthianslive.ui.components.InfoItem
import com.fabricio.corinthianslive.ui.components.LoadingState
import com.fabricio.corinthianslive.ui.components.Pill
import com.fabricio.corinthianslive.ui.components.TeamBlock
import com.fabricio.corinthianslive.ui.theme.CorinthiansColors
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private val ptBr: Locale = Locale.forLanguageTag("pt-BR")

@Composable
fun JogosScreen(contentPadding: PaddingValues) {
    val context = LocalContext.current
    val repository = remember { CorinthiansRepository(context.applicationContext) }
    var refresh by remember { mutableIntStateOf(0) }
    var selectedCompetitions by remember { mutableStateOf(emptySet<String>()) }
    var hideFriendlies by remember { mutableStateOf(AppSettings.hideFriendlies(context)) }
    val state by produceState<Result<RepositoryResult<List<Match>>>?>(null, refresh) {
        value = runCatching { repository.fixtures() }
    }
    val result = state?.getOrNull()
    val allMatches = result?.data.orEmpty()
    val competitions = allMatches
        .filter { !hideFriendlies || !it.isFriendly }
        .map { it.competition }
        .distinct()
        .sorted()
    val visibleMatches = allMatches.filter { match ->
        (!hideFriendlies || !match.isFriendly) &&
            (selectedCompetitions.isEmpty() || match.competition in selectedCompetitions)
    }
    val now = ZonedDateTime.now(APP_ZONE_ID)
    val finishedCodes = setOf("FT", "AET", "PEN", "WO", "CANC")
    val upcoming = visibleMatches.filter { it.resolvedStatus(now) !in finishedCodes }
    val finished = visibleMatches.filter { it.resolvedStatus(now) in finishedCodes }.takeLast(12).reversed()
    val today = now.toLocalDate()
    val tomorrow = today.plusDays(1)
    val weekEnd = today.plusDays(7)
    val datedUpcoming = upcoming.map { it to it.kickoffAtManaus()?.toLocalDate() }
    val todayGames = datedUpcoming.filter { it.second == today }.map { it.first }
    val tomorrowGames = datedUpcoming.filter { it.second == tomorrow }.map { it.first }
    val weekGames = datedUpcoming
        .filter { (_, date) -> date != null && date > tomorrow && date <= weekEnd }
        .map { it.first }
    val monthGames = datedUpcoming
        .filter { (_, date) -> date != null && date > weekEnd && date.year == today.year && date.month == today.month }
        .map { it.first }
    val laterGames = datedUpcoming
        .filter { (_, date) -> date == null || (date > weekEnd && (date.year != today.year || date.month != today.month)) }
        .map { it.first }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 18.dp
        )
    ) {
        item { CorinthiansTopBar("Jogos", "Horários de Manaus e onde assistir", onRefresh = { refresh++ }) }
        if (result != null) item { DataStatus(result.source, result.generatedAt, result.notice, result.isDemo) }

        when {
            state == null -> item { LoadingState() }
            state?.isFailure == true -> item {
                EmptyState(
                    "Não foi possível carregar",
                    state?.exceptionOrNull()?.message ?: "Verifique a conexão.",
                    onRetry = { refresh++ }
                )
            }
            allMatches.isEmpty() -> item {
                EmptyState(
                    "Nenhum jogo encontrado",
                    "A fonte ainda não publicou partidas para esta temporada.",
                    onRetry = { refresh++ }
                )
            }
            else -> {
                item {
                    CompetitionFilters(
                        competitions = competitions,
                        selected = selectedCompetitions,
                        hideFriendlies = hideFriendlies,
                        onSelect = { competition ->
                            selectedCompetitions = if (competition == null) {
                                emptySet()
                            } else if (competition in selectedCompetitions) {
                                selectedCompetitions - competition
                            } else {
                                selectedCompetitions + competition
                            }
                        },
                        onFriendliesChanged = { hidden ->
                            hideFriendlies = hidden
                            AppSettings.setHideFriendlies(context, hidden)
                            if (hidden) {
                                selectedCompetitions = selectedCompetitions.filterNot {
                                    it.contains("amist", ignoreCase = true)
                                }.toSet()
                            }
                        }
                    )
                }

                if (visibleMatches.isEmpty()) {
                    item { EmptyState("Nenhum jogo neste filtro", "Selecione outra competição ou exiba os amistosos.") }
                }
                if (todayGames.isNotEmpty()) {
                    item { AppSectionTitle(sectionTitle("Hoje tem Corinthians", todayGames.size)) }
                    itemsIndexed(todayGames, key = { index, match -> "today-${match.id}-${index}" }) { _, match ->
                        MatchCard(match, countdownLabel(match, today), now)
                    }
                }
                if (tomorrowGames.isNotEmpty()) {
                    item { AppSectionTitle(sectionTitle("Amanhã", tomorrowGames.size)) }
                    itemsIndexed(tomorrowGames, key = { index, match -> "tomorrow-${match.id}-${index}" }) { _, match ->
                        MatchCard(match, "Amanhã é jogo", now)
                    }
                }
                if (weekGames.isNotEmpty()) {
                    item { AppSectionTitle(sectionTitle("Nesta semana", weekGames.size)) }
                    itemsIndexed(weekGames, key = { index, match -> "week-${match.id}-${index}" }) { _, match ->
                        MatchCard(match, countdownLabel(match, today), now)
                    }
                }
                if (monthGames.isNotEmpty()) {
                    val monthName = today.month.getDisplayName(TextStyle.FULL, ptBr)
                        .replaceFirstChar { it.uppercase(ptBr) }
                    item { AppSectionTitle(sectionTitle("Ainda em $monthName", monthGames.size)) }
                    itemsIndexed(monthGames, key = { index, match -> "month-${match.id}-${index}" }) { _, match ->
                        MatchCard(match, countdownLabel(match, today), now)
                    }
                }
                if (laterGames.isNotEmpty()) {
                    item { AppSectionTitle(sectionTitle("Próximos meses", laterGames.size)) }
                    itemsIndexed(laterGames, key = { index, match -> "later-${match.id}-${index}" }) { _, match ->
                        MatchCard(match, countdownLabel(match, today), now)
                    }
                }
                if (finished.isNotEmpty()) {
                    item { AppSectionTitle("Últimos resultados") }
                    itemsIndexed(finished, key = { index, match -> "last-${match.id}-${index}" }) { _, match ->
                        MatchCard(match, null, now)
                    }
                }
            }
        }
    }
}

@Composable
private fun CompetitionFilters(
    competitions: List<String>,
    selected: Set<String>,
    hideFriendlies: Boolean,
    onSelect: (String?) -> Unit,
    onFriendliesChanged: (Boolean) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Text(
            "Filtrar competições",
            modifier = Modifier.padding(horizontal = 16.dp),
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selected.isEmpty(),
                onClick = { onSelect(null) },
                label = { Text("Todas") }
            )
            competitions.forEach { competition ->
                FilterChip(
                    selected = competition in selected,
                    onClick = { onSelect(competition) },
                    label = { Text(competition) }
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Ocultar amistosos", fontWeight = FontWeight.Bold)
                Text(
                    "Aplica também às tabelas e estatísticas.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(checked = hideFriendlies, onCheckedChange = onFriendliesChanged)
        }
    }
}

@Composable
private fun MatchCard(match: Match, countdown: String?, now: ZonedDateTime) {
    val status = match.resolvedStatus(now)
    AppCard(accent = if (status == "LIVE") CorinthiansColors.Red else CorinthiansColors.Black) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Text(match.competition, color = CorinthiansColors.Red, fontWeight = FontWeight.ExtraBold)
                if (match.round.isNotBlank()) {
                    Text(match.round, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                if (status == "LIVE" || status == "AWAITING_RESULT") {
                    Pill(
                        match.resolvedStatusLabel(now),
                        CorinthiansColors.Red,
                        CorinthiansColors.White
                    )
                    Spacer(Modifier.height(7.dp))
                } else if (countdown != null) {
                    Pill(countdown, CorinthiansColors.Red.copy(alpha = .14f), CorinthiansColors.Red)
                    Spacer(Modifier.height(7.dp))
                }
                val displayTime = if (match.time == "--:--") "horário a definir" else match.time
                Pill(
                    "${match.date} • $displayTime",
                    MaterialTheme.colorScheme.onSurface.copy(alpha = .08f),
                    MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TeamBlock(match.home, match.home.equals("Corinthians", true), false, Modifier.weight(1f))
            Column(Modifier.padding(horizontal = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val score = if (match.scoreHome != null && match.scoreAway != null) {
                    "${match.scoreHome}  x  ${match.scoreAway}"
                } else {
                    "vs"
                }
                Text(score, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(6.dp))
                Box(Modifier.height(2.dp).width(44.dp).background(CorinthiansColors.Red))
            }
            TeamBlock(match.away, match.away.equals("Corinthians", true), true, Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            InfoItem("Estádio", match.stadium, Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            InfoItem("Cidade", match.city.ifBlank { "A confirmar" }, Modifier.weight(1f), true)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Onde assistir",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Text(
            match.broadcasters.joinToString(" • ").ifBlank { "Transmissão ainda não informada" },
            fontWeight = FontWeight.SemiBold,
            color = if (match.broadcasters.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun countdownLabel(match: Match, today: LocalDate): String? {
    val date = match.kickoffAtManaus()?.toLocalDate() ?: return null
    return when (val days = ChronoUnit.DAYS.between(today, date)) {
        0L -> "É hoje!"
        1L -> "Amanhã é jogo"
        in 2L..5L -> "Faltam $days dias"
        else -> null
    }
}

private fun sectionTitle(title: String, count: Int): String =
    "$title • $count " + if (count == 1) "jogo" else "jogos"
