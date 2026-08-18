package com.fabricio.corinthianslive.data

import android.content.Context
import com.fabricio.corinthianslive.data.mock.MockRepository
import com.fabricio.corinthianslive.data.model.APP_ZONE_ID
import com.fabricio.corinthianslive.data.model.BracketGame
import com.fabricio.corinthianslive.data.model.BracketRound
import com.fabricio.corinthianslive.data.model.BracketTie
import com.fabricio.corinthianslive.data.model.CompetitionStats
import com.fabricio.corinthianslive.data.model.CompetitionTable
import com.fabricio.corinthianslive.data.model.DetailedMatchStats
import com.fabricio.corinthianslive.data.model.EventType
import com.fabricio.corinthianslive.data.model.LiveContent
import com.fabricio.corinthianslive.data.model.LiveMatch
import com.fabricio.corinthianslive.data.model.Match
import com.fabricio.corinthianslive.data.model.MatchEvent
import com.fabricio.corinthianslive.data.model.Player
import com.fabricio.corinthianslive.data.model.RepositoryResult
import com.fabricio.corinthianslive.data.model.Standing
import com.fabricio.corinthianslive.data.model.StandingGroup
import com.fabricio.corinthianslive.data.model.StatsSummary
import com.fabricio.corinthianslive.data.model.TeamMatchStats
import com.fabricio.corinthianslive.data.model.TeamSquad
import com.fabricio.corinthianslive.data.model.TeamStats
import com.fabricio.corinthianslive.data.model.kickoffAtManaus
import com.fabricio.corinthianslive.data.model.resolvedStatus
import com.fabricio.corinthianslive.data.model.resolvedStatusLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val LIVE_SEED_TTL_MS = 60_000L
private const val FIXTURES_TTL_MS = 15 * 60_000L

class CorinthiansRepository(private val context: Context) {
    private val mock = MockRepository()
    private val locale = Locale.forLanguageTag("pt-BR")
    private val dateFormatter = DateTimeFormatter.ofPattern("EEE, dd/MM", locale)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", locale)
    private val realtimeSource = RealtimeLiveDataSource()
    private var cachedLiveSeed: RepositoryResult<LiveContent>? = null
    private var cachedLiveSeedAt: Long = 0
    private var cachedFixtures: List<Match>? = null
    private var cachedFixturesAt: Long = 0

    suspend fun fixtures(): RepositoryResult<List<Match>> = withContext(Dispatchers.IO) {
        val payload = read("fixtures.json")
        val root = JSONObject(payload.text)
        val matches = root.optJSONArray("fixtures").mapObjects(::parseMatch)
        RepositoryResult(
            data = if (matches.isEmpty() && root.optString("source") == "demo") mock.nextMatches() else matches,
            source = root.optString("source", "desconhecida"),
            generatedAt = root.optString("generatedAt"),
            notice = payload.notice
        )
    }

    suspend fun live(): RepositoryResult<LiveContent> = withContext(Dispatchers.IO) {
        val payload = read("live.json")
        val root = JSONObject(payload.text)
        val liveItems = root.optJSONArray("liveMatches")
        val matchJson = root.optJSONObject("featuredMatch")
            ?: if (liveItems != null && liveItems.length() > 0) liveItems.optJSONObject(0) else null
        val match = matchJson?.let(::parseLiveMatch)
        val events = if (match == null) {
            emptyList()
        } else {
            root.optJSONObject("eventsByFixture")
                ?.optJSONArray(match.id.toString())
                .mapObjects(::parseEvent)
        }
        val squads = matchJson?.optJSONObject("squads")
        val statistics = matchJson?.optJSONObject("statistics")
        RepositoryResult(
            data = LiveContent(
                match = match,
                events = events.orEmpty(),
                homeSquad = squads?.optJSONObject("homeTeam")?.let { parseSquad(it, match?.home.orEmpty()) },
                awaySquad = squads?.optJSONObject("awayTeam")?.let { parseSquad(it, match?.away.orEmpty()) },
                homeStats = statistics?.optJSONObject("homeTeam")?.let(::parseTeamStats),
                awayStats = statistics?.optJSONObject("awayTeam")?.let(::parseTeamStats),
                lineupStatus = matchJson?.optString("lineupStatus").orEmpty()
            ),
            source = root.optString("source", "desconhecida"),
            generatedAt = root.optString("generatedAt"),
            notice = payload.notice
        )
    }

    suspend fun liveRealtime(matchId: Long? = null): RepositoryResult<LiveContent> = withContext(Dispatchers.IO) {
        val requestTime = System.currentTimeMillis()
        val published = if (cachedLiveSeed == null || requestTime - cachedLiveSeedAt >= LIVE_SEED_TTL_MS) {
            runCatching { live() }.getOrElse { cachedLiveSeed ?: throw it }.also {
                cachedLiveSeed = it
                cachedLiveSeedAt = requestTime
            }
        } else {
            requireNotNull(cachedLiveSeed)
        }
        val fixtures = if (cachedFixtures == null || requestTime - cachedFixturesAt >= FIXTURES_TTL_MS) {
            runCatching { fixtures().data }.getOrElse { cachedFixtures.orEmpty() }.also {
                cachedFixtures = it
                cachedFixturesAt = requestTime
            }
        } else {
            cachedFixtures.orEmpty()
        }
        val now = ZonedDateTime.now(APP_ZONE_ID)
        val publishedMatch = published.data.match?.takeIf { matchId == null || it.id == matchId }
        val fixture = when {
            matchId != null -> fixtures.firstOrNull { it.id == matchId }
            publishedMatch != null -> fixtures.firstOrNull { it.id == publishedMatch.id }
            else -> fixtures.firstOrNull { candidate ->
                val kickoff = candidate.kickoffAtManaus() ?: return@firstOrNull false
                candidate.resolvedStatus(now) == "LIVE" || Duration.between(now, kickoff).toMinutes() in -300..60
            }
        }
        val fallbackMatch = publishedMatch ?: fixture?.toLiveMatch(now)
        if (fallbackMatch == null) return@withContext published
        val fallbackContent = if (published.data.match?.id == fallbackMatch.id) {
            published.data
        } else {
            LiveContent(match = fallbackMatch, events = emptyList())
        }
        val detailsUrl = fixture?.detailsUrl.orEmpty()
        if (detailsUrl.isBlank()) {
            return@withContext RepositoryResult(
                data = fallbackContent,
                source = published.source,
                generatedAt = published.generatedAt,
                notice = published.notice
            )
        }

        runCatching { realtimeSource.fetch(detailsUrl, fallbackContent) }
            .fold(
                onSuccess = { content ->
                    RepositoryResult(
                        data = content,
                        source = "ge-corinthians-direto",
                        generatedAt = Instant.now().toString(),
                        notice = null
                    )
                },
                onFailure = {
                    RepositoryResult(
                        data = fallbackContent,
                        source = published.source,
                        generatedAt = published.generatedAt,
                        notice = "Fonte direta temporariamente indisponível; exibindo a última atualização publicada."
                    )
                }
            )
    }

    private fun Match.toLiveMatch(now: ZonedDateTime): LiveMatch {
        val status = resolvedStatus(now)
        val kickoffAt = kickoffAtManaus()
        val elapsedMinutes = if (status == "LIVE" && kickoffAt != null) {
            Duration.between(kickoffAt, now).toMinutes().coerceIn(0, 180).toInt()
        } else {
            0
        }
        return LiveMatch(
            id = id,
            competition = competition,
            stadium = stadium,
            city = city,
            home = home,
            away = away,
            scoreHome = scoreHome ?: 0,
            scoreAway = scoreAway ?: 0,
            minute = elapsedMinutes,
            statusShort = status,
            statusLong = resolvedStatusLabel(now),
            kickoff = kickoff
        )
    }

    suspend fun standings(): RepositoryResult<List<CompetitionTable>> = withContext(Dispatchers.IO) {
        val payload = read("standings.json")
        val root = JSONObject(payload.text)
        val competitions = root.optJSONArray("competitions").mapObjects(::parseCompetitionTable).toMutableList()
        if (competitions.isEmpty()) {
            val tables = root.optJSONArray("tables")
            val first = if (tables != null && tables.length() > 0) tables.optJSONArray(0) else null
            val rows = first.mapObjects(::parseStanding)
            if (rows.isNotEmpty()) {
                competitions += CompetitionTable(
                    name = root.optJSONObject("competition")?.optString("name", "Brasileirão Série A")
                        ?: "Brasileirão Série A",
                    phase = "Classificação",
                    kind = "league",
                    groups = listOf(StandingGroup("Classificação", rows)),
                    brackets = emptyList()
                )
            }
        }
        if (competitions.isEmpty() && root.optString("source") == "demo") {
            competitions += CompetitionTable(
                name = "Brasileirão Série A",
                phase = "Classificação",
                kind = "league",
                groups = listOf(StandingGroup("Classificação", mock.standings())),
                brackets = emptyList()
            )
        }
        RepositoryResult(
            data = competitions,
            source = root.optString("source", "desconhecida"),
            generatedAt = root.optString("generatedAt"),
            notice = payload.notice
        )
    }

    suspend fun stats(): RepositoryResult<TeamStats> = withContext(Dispatchers.IO) {
        val payload = read("stats.json")
        val root = JSONObject(payload.text)
        val summary = root.optJSONObject("summary") ?: JSONObject()
        val recentMatches = root.optJSONArray("recentMatches").mapObjects(::parseMatch)
        val matchesById = recentMatches.associateBy { it.id }
        val stats = TeamStats(
            window = root.optInt("window", 10),
            summary = StatsSummary(
                matches = summary.optInt("matches"),
                wins = summary.optInt("wins"),
                draws = summary.optInt("draws"),
                losses = summary.optInt("losses"),
                goalsFor = summary.optInt("goalsFor"),
                goalsAgainst = summary.optInt("goalsAgainst"),
                goalDifference = summary.optInt("goalDifference"),
                cleanSheets = summary.optInt("cleanSheets"),
                scoringGames = summary.optInt("scoringGames"),
                pointsPercentage = summary.optInt("pointsPercentage"),
                averageGoalsFor = summary.optDouble("averageGoalsFor"),
                averageGoalsAgainst = summary.optDouble("averageGoalsAgainst"),
                currentStreak = summary.optString("currentStreak")
            ),
            form = root.optJSONArray("form").toStringList(),
            competitions = root.optJSONArray("competitions").mapObjects { item ->
                CompetitionStats(
                    name = item.optString("name"),
                    matches = item.optInt("matches"),
                    wins = item.optInt("wins"),
                    draws = item.optInt("draws"),
                    losses = item.optInt("losses"),
                    goalsFor = item.optInt("goalsFor"),
                    goalsAgainst = item.optInt("goalsAgainst")
                )
            },
            recentMatches = recentMatches,
            matchDetails = root.optJSONArray("matchDetails").mapObjects { item ->
                val matchId = item.optLong("matchId")
                val match = matchesById[matchId]
                if (match == null) {
                    null
                } else {
                    val statistics = item.optJSONObject("statistics")
                    DetailedMatchStats(
                        match = match,
                        homeStats = statistics?.optJSONObject("homeTeam")?.let(::parseTeamStats),
                        awayStats = statistics?.optJSONObject("awayTeam")?.let(::parseTeamStats),
                        events = item.optJSONArray("events").mapObjects(::parseEvent)
                    )
                }
            }
        )
        RepositoryResult(
            data = stats,
            source = root.optString("source", "desconhecida"),
            generatedAt = root.optString("generatedAt"),
            notice = payload.notice
        )
    }

    suspend fun testConnection(): String = withContext(Dispatchers.IO) {
        val base = DataSettings.getBaseUrl(context)
        val connection = openConnection(base + "/fixtures.json?connectionTest=" + System.currentTimeMillis())
        try {
            val code = connection.responseCode
            if (code !in 200..299) error("GitHub respondeu com o código " + code + ".")
            val root = connection.inputStream.bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
            if (root.optJSONArray("fixtures") == null) error("O arquivo de jogos não está no formato esperado.")
            if (root.optString("generatedAt").isBlank()) {
                "Conexão com o GitHub funcionando."
            } else {
                "Conexão com o GitHub funcionando. Dados encontrados."
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseMatch(item: JSONObject): Match {
        val kickoff = runCatching { OffsetDateTime.parse(item.optString("kickoff")) }.getOrNull()
        val localKickoff = kickoff?.atZoneSameInstant(APP_ZONE_ID)
        val home = item.optJSONObject("home") ?: JSONObject()
        val away = item.optJSONObject("away") ?: JSONObject()
        val score = item.optJSONObject("score") ?: JSONObject()
        return Match(
            id = item.optLong("id"),
            competition = item.optString("competition", "Competição a definir"),
            home = home.optString("name", "A definir"),
            away = away.optString("name", "A definir"),
            date = localKickoff?.format(dateFormatter)?.replaceFirstChar { it.titlecase(locale) } ?: "Data a definir",
            time = if (item.optString("kickoff").contains("T00:00:00")) "--:--" else localKickoff?.format(timeFormatter) ?: "--:--",
            stadium = item.optString("stadium", "Local a definir"),
            city = item.optString("city"),
            statusShort = item.optString("statusShort", "NS"),
            statusLong = item.optString("statusLong", "Agendado"),
            scoreHome = score.optIntOrNull("home"),
            scoreAway = score.optIntOrNull("away"),
            kickoff = item.optString("kickoff"),
            round = item.optString("round"),
            broadcasters = item.optJSONArray("broadcasters").toStringList(),
            detailsUrl = item.optString("detailsUrl")
        )
    }

    private fun parseLiveMatch(item: JSONObject): LiveMatch {
        val home = item.optJSONObject("home") ?: JSONObject()
        val away = item.optJSONObject("away") ?: JSONObject()
        val score = item.optJSONObject("score") ?: JSONObject()
        return LiveMatch(
            id = item.optLong("id"),
            competition = item.optString("competition"),
            stadium = item.optString("stadium", "Local a definir"),
            city = item.optString("city"),
            home = home.optString("name"),
            away = away.optString("name"),
            scoreHome = score.optIntOrNull("home") ?: 0,
            scoreAway = score.optIntOrNull("away") ?: 0,
            minute = item.optInt("minute"),
            statusShort = item.optString("statusShort", "NS"),
            statusLong = item.optString("statusLong", "Agendado"),
            kickoff = item.optString("kickoff")
        )
    }

    private fun parseSquad(item: JSONObject, teamName: String): TeamSquad = TeamSquad(
        teamName = teamName,
        formation = item.optString("formation"),
        coach = item.optJSONObject("coach")?.optString("popularName")
            ?.ifBlank { item.optJSONObject("coach")?.optString("name").orEmpty() }
            .orEmpty(),
        starters = item.optJSONArray("lineUp").mapObjects(::parsePlayer),
        bench = item.optJSONArray("bench").mapObjects(::parsePlayer)
    )

    private fun parsePlayer(item: JSONObject): Player = Player(
        name = item.optString("popularName").ifBlank { item.optString("name") },
        shirtNumber = item.optString("shirtNumber"),
        position = item.optJSONObject("position")?.optString("description").orEmpty()
    )

    private fun parseTeamStats(item: JSONObject): TeamMatchStats = TeamMatchStats(
        shotsOnGoal = item.statTotal("goalFinish"),
        shotsOffGoal = item.statTotal("wrongFinish") + item.statTotal("ballOutFinish"),
        blockedShots = item.statTotal("blockedFinish"),
        ballPossession = item.statTotal("ballPossession"),
        fouls = item.statTotal("foulMade"),
        corners = item.statTotal("cornerKick"),
        offsides = item.statTotal("offSide"),
        saves = item.statTotal("defense"),
        yellowCards = item.statTotal("yellowCardReceived"),
        redCards = item.statTotal("redCardReceived"),
        passes = item.statTotal("totalPasses"),
        accuratePasses = item.statTotal("rightPasses"),
        tackles = item.statTotal("tackle")
    )

    private fun parseEvent(item: JSONObject): MatchEvent {
        val rawType = item.optString("type").uppercase()
        val description = item.optString("description")
        val type = when {
            rawType == "GOAL" -> EventType.Goal
            rawType == "RED_CARD" -> EventType.RedCard
            rawType == "YELLOW_CARD" || rawType == "CARD" -> EventType.YellowCard
            rawType == "SUBSTITUTION" || rawType == "SUBST" -> EventType.Substitution
            rawType == "SHOT" -> EventType.Shot
            rawType == "FOUL" -> EventType.Foul
            rawType == "CORNER" -> EventType.Corner
            rawType == "OFFSIDE" -> EventType.Offside
            rawType == "SAVE" -> EventType.Save
            rawType == "PENALTY" -> EventType.Penalty
            rawType == "VAR" -> EventType.Var
            rawType == "KICKOFF" -> EventType.Kickoff
            description.contains("chute", true) || description.contains("finaliza", true) -> EventType.Shot
            description.contains("falta", true) -> EventType.Foul
            description.contains("escanteio", true) -> EventType.Corner
            description.contains("impedimento", true) -> EventType.Offside
            description.contains("defesa", true) -> EventType.Save
            else -> EventType.Other
        }
        val fallbackId = listOf(
            item.optString("period"),
            item.optString("clock"),
            item.optString("team"),
            description.hashCode().toString()
        ).joinToString("-")
        return MatchEvent(
            id = item.optString("id").ifBlank { fallbackId },
            minute = item.optInt("minute"),
            clock = item.optString("clock").ifBlank { item.optInt("minute").toString() },
            period = item.optString("period"),
            team = item.optString("team"),
            type = type,
            description = description,
            createdAt = item.optString("createdAt")
        )
    }

    private fun parseCompetitionTable(item: JSONObject): CompetitionTable = CompetitionTable(
        name = item.optString("name", "Competição"),
        phase = item.optString("phase"),
        kind = item.optString("kind", "league"),
        groups = item.optJSONArray("groups").mapObjects { group ->
            StandingGroup(
                name = group.optString("name", "Classificação"),
                entries = group.optJSONArray("entries").mapObjects(::parseStanding)
            )
        },
        brackets = item.optJSONArray("brackets").mapObjects { round ->
            BracketRound(
                name = round.optString("name", "Mata-mata"),
                ties = round.optJSONArray("ties").mapObjects { tie ->
                    BracketTie(
                        name = tie.optString("name"),
                        games = tie.optJSONArray("games").mapObjects { game ->
                            BracketGame(
                                id = game.optLong("id"),
                                date = game.optString("date"),
                                home = game.optString("home"),
                                away = game.optString("away"),
                                scoreHome = game.optIntOrNull("scoreHome"),
                                scoreAway = game.optIntOrNull("scoreAway"),
                                penaltyHome = game.optIntOrNull("penaltyHome"),
                                penaltyAway = game.optIntOrNull("penaltyAway")
                            )
                        }
                    )
                }
            )
        }
    )

    private fun parseStanding(row: JSONObject): Standing = Standing(
        position = row.optInt("position"),
        teamName = row.optString("teamName"),
        points = row.optInt("points"),
        played = row.optInt("played"),
        wins = row.optInt("wins"),
        draws = row.optInt("draws"),
        losses = row.optInt("losses"),
        goalDifference = row.optInt("goalDifference"),
        form = row.optString("form")
    )

    private data class Payload(val text: String, val notice: String?)

    private fun read(name: String): Payload {
        val base = DataSettings.getBaseUrl(context)
        return try {
            val connection = openConnection(base + "/" + name + "?ts=" + System.currentTimeMillis())
            try {
                if (connection.responseCode !in 200..299) error("HTTP " + connection.responseCode)
                val text = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                validateJson(text)
                saveCache(name, text)
                Payload(text, null)
            } finally {
                connection.disconnect()
            }
        } catch (_: Exception) {
            val cached = readCache(name)
            if (cached != null) {
                Payload(cached, "GitHub indisponível: exibindo a última cópia válida.")
            } else {
                val bundled = context.assets.open(name).bufferedReader(Charsets.UTF_8).use { it.readText() }
                validateJson(bundled)
                Payload(bundled, "GitHub indisponível: exibindo a cópia incluída no app.")
            }
        }
    }

    private fun validateJson(text: String) {
        if (text.contains("<<<<<<<") || text.contains("=======") || text.contains(">>>>>>>")) {
            error("O arquivo contém marcadores de conflito do Git.")
        }
        JSONObject(text)
    }

    private fun cacheFile(name: String): File =
        File(context.filesDir, "corinthians-data").resolve(name)

    private fun saveCache(name: String, text: String) {
        runCatching {
            val target = cacheFile(name)
            target.parentFile?.mkdirs()
            val temporary = File(target.parentFile, target.name + ".tmp")
            temporary.writeText(text, Charsets.UTF_8)
            if (!temporary.renameTo(target)) {
                target.writeText(text, Charsets.UTF_8)
                temporary.delete()
            }
        }
    }

    private fun readCache(name: String): String? = runCatching {
        cacheFile(name).takeIf(File::isFile)?.readText(Charsets.UTF_8)?.also(::validateJson)
    }.getOrNull()

    private fun openConnection(address: String): HttpURLConnection =
        (URL(address).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 12_000
            useCaches = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Cache-Control", "no-cache")
        }
}

private fun <T : Any> JSONArray?.mapObjects(transform: (JSONObject) -> T?): List<T> = buildList {
    val source = this@mapObjects ?: return@buildList
    for (index in 0 until source.length()) {
        source.optJSONObject(index)?.let(transform)?.let(::add)
    }
}

private fun JSONArray?.toStringList(): List<String> = buildList {
    val source = this@toStringList ?: return@buildList
    for (index in 0 until source.length()) {
        source.optString(index).takeIf { it.isNotBlank() }?.let(::add)
    }

}

private fun JSONObject.optIntOrNull(name: String): Int? =
    if (isNull(name) || !has(name)) null else optInt(name)

private fun JSONObject.statTotal(name: String): Int =
    optJSONObject(name)?.optInt("total") ?: 0
