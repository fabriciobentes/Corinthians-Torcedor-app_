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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
    val competitions = allMatches.filter { !hideFriendlies || !it.isFriendly }
        .map { it.competition }.distinct().sorted()
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
    val weekGames = datedUpcoming.filter { (_, date) -> date != null && date > tomorrow && date <= weekEnd }.map { it.first }
    val monthGames = datedUpcoming.filter { (_, date) ->
        date != null && date > weekEnd && date.year == today.year && date.month == today.month
    }.map { it.first }
    val laterGames = datedUpcoming.filter { (_, date) ->
        date == null || (date > weekEnd && (date.year != today.year || date.month != today.month))
    }.map { it.first }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 18.dp)
    ) {
        item { CorinthiansTopBar("Jogos", "Agenda completa • horário de Manaus • onde assistir", onRefresh = { refresh++ }) }
        if (result != null) item { DataStatus(result.source, result.generatedAt, result.notice, result.isDemo) }

        when {
            state == null -> item { LoadingState() }
            state?.isFailure == true -> item {
                EmptyState("Não foi possível carregar", state?.exceptionOrNull()?.message ?: "Verifique a conexão.", onRetry = { refresh++ })
            }
            allMatches.isEmpty() -> item {
                EmptyState("Nenhum jogo encontrado", "A fonte ainda não publicou partidas para esta temporada.", onRetry = { refresh++ })
            }
            else -> {
                item {
                    CompetitionFilters(
                        competitions,
                        selectedCompetitions,
                        hideFriendlies,
                        onSelect = { competition ->
                            selectedCompetitions = when {
                                competition == null -> emptySet()
                                competition in selectedCompetitions -> selectedCompetitions - competition
                                else -> selectedCompetitions + competition
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
                    itemsIndexed(todayGames, key = { index, match -> "today-${match.id}-$index" }) { _, match ->
                        MatchCard(match, countdownLabel(match, today), now)
                    }
                }
                if (tomorrowGames.isNotEmpty()) {
                    item { AppSectionTitle(sectionTitle("Amanhã", tomorrowGames.size)) }
                    itemsIndexed(tomorrowGames, key = { index, match -> "tomorrow-${match.id}-$index" }) { _, match ->
                        MatchCard(match, "Amanhã é jogo", now)
                    }
                }
                if (weekGames.isNotEmpty()) {
                    item { AppSectionTitle(sectionTitle("Nesta semana", weekGames.size)) }
                    itemsIndexed(weekGames, key = { index, match -> "week-${match.id}-$index" }) { _, match ->
                        MatchCard(match, countdownLabel(match, today), now)
                    }
                }
                if (monthGames.isNotEmpty()) {
                    val monthName = today.month.getDisplayName(TextStyle.FULL, ptBr).replaceFirstChar { it.uppercase(ptBr) }
                    item { AppSectionTitle(sectionTitle("Ainda em $monthName", monthGames.size)) }
                    itemsIndexed(monthGames, key = { index, match -> "month-${match.id}-$index" }) { _, match ->
                        MatchCard(match, countdownLabel(match, today), now)
                    }
                }
                if (laterGames.isNotEmpty()) {
                    item { AppSectionTitle(sectionTitle("Próximos meses", laterGames.size)) }
                    itemsIndexed(laterGames, key = { index, match -> "later-${match.id}-$index" }) { _, match ->
                        MatchCard(match, countdownLabel(match, today), now)
                    }
                }
                if (finished.isNotEmpty()) {
                    item { AppSectionTitle("Últimos resultados") }
                    itemsIndexed(finished, key = { index, match -> "last-${match.id}-$index" }) { _, match ->
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
    Column(Modifier.padding(top = 12.dp)) {
        AppCard(accent = CorinthiansColors.Gold) {
            Text("PERSONALIZE SUA AGENDA", color = CorinthiansColors.Gold, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(4.dp))
            Text("Competições", style = MaterialTheme.typography.titleLarge)
            Text(
                "Selecione uma ou várias competições.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompetitionChip("Todas", selected.isEmpty()) { onSelect(null) }
                competitions.forEach { competition ->
                    CompetitionChip(competition, competition in selected) { onSelect(competition) }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Ocultar amistosos", fontWeight = FontWeight.ExtraBold)
                    Text(
                        "Filtro global para jogos, tabelas e estatísticas.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = hideFriendlies,
                    onCheckedChange = onFriendliesChanged,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = CorinthiansColors.Red,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }
        }
    }
}

@Composable
private fun CompetitionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontWeight = FontWeight.ExtraBold) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
private fun MatchCard(match: Match, countdown: String?, now: ZonedDateTime) {
    val status = match.resolvedStatus(now)
    val live = status == "LIVE"
    AppCard(accent = if (live) CorinthiansColors.Red else CorinthiansColors.Gold) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(match.competition.uppercase(), color = CorinthiansColors.Red, style = MaterialTheme.typography.labelLarge)
                if (match.round.isNotBlank()) {
                    Text(match.round, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.width(10.dp))
            when {
                live || status == "AWAITING_RESULT" -> Pill(match.resolvedStatusLabel(now), CorinthiansColors.Red, Color.White)
                countdown != null -> Pill(countdown, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CalendarMonth, null, tint = CorinthiansColors.Gold, modifier = Modifier.width(20.dp))
            Spacer(Modifier.width(8.dp))
            val displayTime = if (match.time == "--:--") "horário a definir" else match.time
            Text("${match.date} • $displayTime", fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            if (live) Pill("AO VIVO", CorinthiansColors.Red, Color.White)
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TeamBlock(match.home, match.home.equals("Corinthians", true), false, Modifier.weight(1f))
            Column(Modifier.width(76.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val score = if (match.scoreHome != null && match.scoreAway != null) {
                    "${match.scoreHome} × ${match.scoreAway}"
                } else {
                    "VS"
                }
                Text(score, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Spacer(Modifier.height(7.dp))
                Box(Modifier.width(48.dp).height(3.dp).clip(RoundedCornerShape(3.dp)).background(CorinthiansColors.Red))
            }
            TeamBlock(match.away, match.away.equals("Corinthians", true), true, Modifier.weight(1f))
        }
        Spacer(Modifier.height(20.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant).padding(13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.LocationOn, null, tint = CorinthiansColors.Red)
            Spacer(Modifier.width(9.dp))
            InfoItem("Estádio", match.stadium, Modifier.weight(1.35f))
            Spacer(Modifier.width(12.dp))
            InfoItem("Cidade", match.city.ifBlank { "A confirmar" }, Modifier.weight(.75f), true)
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp))
                .background(MaterialTheme.colorScheme.primaryContainer).padding(13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.width(38.dp).height(38.dp).clip(RoundedCornerShape(12.dp)).background(CorinthiansColors.Red),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.LiveTv, null, tint = Color.White)
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text("ONDE ASSISTIR", color = CorinthiansColors.Red, style = MaterialTheme.typography.labelSmall)
                Text(
                    match.broadcasters.joinToString(" • ").ifBlank { "Transmissão ainda não informada" },
                    fontWeight = FontWeight.ExtraBold,
                    color = if (match.broadcasters.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
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
