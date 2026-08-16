package com.fabricio.corinthianslive.data.model

import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

val APP_ZONE_ID: ZoneId = ZoneId.of("America/Manaus")

data class Match(
    val id: Long = 0,
    val competition: String,
    val home: String,
    val away: String,
    val date: String,
    val time: String,
    val stadium: String,
    val city: String,
    val statusShort: String = "NS",
    val statusLong: String = "Agendado",
    val scoreHome: Int? = null,
    val scoreAway: Int? = null,
    val kickoff: String = "",
    val round: String = "",
    val broadcasters: List<String> = emptyList(),
    val detailsUrl: String = ""
) {
    val isFriendly: Boolean
        get() = competition.contains("amist", ignoreCase = true)
}

fun Match.kickoffAtManaus(): ZonedDateTime? = runCatching {
    OffsetDateTime.parse(kickoff).atZoneSameInstant(APP_ZONE_ID)
}.getOrNull()

fun Match.resolvedStatus(now: ZonedDateTime = ZonedDateTime.now(APP_ZONE_ID)): String {
    if (statusShort !in setOf("NS", "TBD", "LIVE")) return statusShort
    val start = kickoffAtManaus() ?: return statusShort
    if (kickoff.contains("T00:00:00")) return statusShort
    return when {
        !now.isBefore(start) && now.isBefore(start.plusHours(4)) -> "LIVE"
        !now.isBefore(start.plusHours(4)) -> "AWAITING_RESULT"
        else -> statusShort
    }
}

fun Match.resolvedStatusLabel(now: ZonedDateTime = ZonedDateTime.now(APP_ZONE_ID)): String = when (resolvedStatus(now)) {
    "LIVE" -> "Partida em andamento"
    "AWAITING_RESULT" -> "Aguardando resultado"
    else -> statusLong
}

data class LiveMatch(
    val id: Long = 0,
    val competition: String,
    val stadium: String,
    val city: String,
    val home: String,
    val away: String,
    val scoreHome: Int,
    val scoreAway: Int,
    val minute: Int,
    val statusShort: String,
    val statusLong: String,
    val kickoff: String
)

data class Player(
    val name: String,
    val shirtNumber: String,
    val position: String
)

data class TeamSquad(
    val teamName: String,
    val formation: String,
    val coach: String,
    val starters: List<Player>,
    val bench: List<Player>
)

data class TeamMatchStats(
    val shotsOnGoal: Int = 0,
    val shotsOffGoal: Int = 0,
    val blockedShots: Int = 0,
    val ballPossession: Int = 0,
    val fouls: Int = 0,
    val corners: Int = 0,
    val offsides: Int = 0,
    val saves: Int = 0,
    val yellowCards: Int = 0,
    val redCards: Int = 0,
    val passes: Int = 0,
    val accuratePasses: Int = 0,
    val tackles: Int = 0
)

data class LiveContent(
    val match: LiveMatch?,
    val events: List<MatchEvent>,
    val homeSquad: TeamSquad? = null,
    val awaySquad: TeamSquad? = null,
    val homeStats: TeamMatchStats? = null,
    val awayStats: TeamMatchStats? = null,
    val lineupStatus: String = ""
)

data class RepositoryResult<T>(
    val data: T,
    val source: String,
    val generatedAt: String,
    val notice: String? = null
) {
    val isDemo: Boolean get() = source == "demo"
}
