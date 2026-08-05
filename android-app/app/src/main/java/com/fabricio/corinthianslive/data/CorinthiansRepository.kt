package com.fabricio.corinthianslive.data

import android.content.Context
import com.fabricio.corinthianslive.BuildConfig
import com.fabricio.corinthianslive.data.mock.MockRepository
import com.fabricio.corinthianslive.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class CorinthiansRepository(private val context: Context) {
    private val mock = MockRepository()
    private val locale = Locale.forLanguageTag("pt-BR")
    private val dateFormatter = DateTimeFormatter.ofPattern("EEE, dd/MM", locale)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", locale)

    suspend fun fixtures(): RepositoryResult<List<Match>> = withContext(Dispatchers.IO) {
        val payload = read("fixtures.json")
        val root = JSONObject(payload.text)
        val items = root.optJSONArray("fixtures")
        val matches = buildList {
            if (items != null) for (index in 0 until items.length()) add(parseMatch(items.getJSONObject(index)))
        }
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
        val matchJson = if (liveItems != null && liveItems.length() > 0) liveItems.getJSONObject(0) else null
        val match = matchJson?.let {
            val home = it.getJSONObject("home")
            val away = it.getJSONObject("away")
            val score = it.getJSONObject("score")
            LiveMatch(
                id = it.getLong("id"), competition = it.optString("competition"), stadium = it.optString("stadium", "Local a definir"),
                home = home.optString("name"), away = away.optString("name"), scoreHome = score.optIntOrNull("home") ?: 0,
                scoreAway = score.optIntOrNull("away") ?: 0, minute = it.optIntOrNull("minute") ?: 0
            )
        }
        val events = buildList {
            if (match != null) {
                val array = root.optJSONObject("eventsByFixture")?.optJSONArray(match.id.toString())
                if (array != null) for (index in 0 until array.length()) add(parseEvent(array.getJSONObject(index)))
            }
        }
        RepositoryResult(LiveContent(match, events), root.optString("source", "desconhecida"), root.optString("generatedAt"), payload.notice)
    }

    suspend fun standings(): RepositoryResult<List<Standing>> = withContext(Dispatchers.IO) {
        val payload = read("standings.json")
        val root = JSONObject(payload.text)
        val tables = root.optJSONArray("tables")
        val first = if (tables != null && tables.length() > 0) tables.optJSONArray(0) else null
        val rows = buildList {
            if (first != null) for (index in 0 until first.length()) {
                val row = first.getJSONObject(index)
                add(Standing(row.getInt("position"), row.getString("teamName"), row.getInt("points"), row.getInt("played"), row.getInt("wins"), row.getInt("draws"), row.getInt("losses"), row.getInt("goalDifference"), row.optString("form")))
            }
        }
        RepositoryResult(if (rows.isEmpty() && root.optString("source") == "demo") mock.standings() else rows, root.optString("source", "desconhecida"), root.optString("generatedAt"), payload.notice)
    }

    suspend fun stats(): RepositoryResult<TeamStats> = withContext(Dispatchers.IO) {
        val payload = read("stats.json")
        val root = JSONObject(payload.text)
        val summary = root.getJSONObject("summary")
        val competitionItems = root.optJSONArray("competitions")
        val recentItems = root.optJSONArray("recentMatches")
        val formItems = root.optJSONArray("form")

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
            form = buildList {
                if (formItems != null) for (index in 0 until formItems.length()) add(formItems.optString(index))
            },
            competitions = buildList {
                if (competitionItems != null) for (index in 0 until competitionItems.length()) {
                    val item = competitionItems.getJSONObject(index)
                    add(
                        CompetitionStats(
                            name = item.optString("name"),
                            matches = item.optInt("matches"),
                            wins = item.optInt("wins"),
                            draws = item.optInt("draws"),
                            losses = item.optInt("losses"),
                            goalsFor = item.optInt("goalsFor"),
                            goalsAgainst = item.optInt("goalsAgainst")
                        )
                    )
                }
            },
            recentMatches = buildList {
                if (recentItems != null) for (index in 0 until recentItems.length()) add(parseMatch(recentItems.getJSONObject(index)))
            }
        )
        RepositoryResult(stats, root.optString("source", "desconhecida"), root.optString("generatedAt"), payload.notice)
    }

    private fun parseMatch(item: JSONObject): Match {
        val kickoff = runCatching { OffsetDateTime.parse(item.getString("kickoff")) }.getOrNull()
        val home = item.getJSONObject("home")
        val away = item.getJSONObject("away")
        val score = item.getJSONObject("score")
        return Match(
            id = item.getLong("id"), competition = item.optString("competition"), home = home.optString("name"), away = away.optString("name"),
            date = kickoff?.format(dateFormatter)?.replaceFirstChar { it.titlecase(locale) } ?: "Data a definir",
            time = kickoff?.format(timeFormatter) ?: "--:--", stadium = item.optString("stadium", "Local a definir"), city = item.optString("city"),
            statusShort = item.optString("statusShort", "NS"), statusLong = item.optString("statusLong", "Agendado"),
            scoreHome = score.optIntOrNull("home"), scoreAway = score.optIntOrNull("away"),
            kickoff = item.optString("kickoff")
        )
    }

    private fun parseEvent(item: JSONObject): MatchEvent {
        val type = when (item.optString("type").lowercase()) {
            "goal" -> EventType.Goal; "card" -> if (item.optString("detail").contains("Red", true)) EventType.RedCard else EventType.YellowCard
            "subst" -> EventType.Substitution; "var" -> EventType.Var; else -> EventType.Kickoff
        }
        val player = item.optString("player")
        val detail = item.optString("detail")
        return MatchEvent(item.optInt("minute"), item.optString("team"), type, listOf(player, detail).filter { it.isNotBlank() }.joinToString(" — "))
    }

    private data class Payload(val text: String, val notice: String?)

    private fun read(name: String): Payload {
        val base = DataSettings.getBaseUrl(context).ifBlank { BuildConfig.DATA_BASE_URL.trimEnd('/') }
        if (base.isNotBlank()) {
            try {
                val connection = URL("$base/$name").openConnection() as HttpURLConnection
                connection.connectTimeout = 8_000
                connection.readTimeout = 8_000
                connection.setRequestProperty("Accept", "application/json")
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { return Payload(it.readText(), null) }
            } catch (_: Exception) {
                return Payload(context.assets.open(name).bufferedReader().use { it.readText() }, "Sem conexão: exibindo a última cópia disponível.")
            }
        }
        return Payload(context.assets.open(name).bufferedReader().use { it.readText() }, "Fonte online ainda não configurada.")
    }
}

private fun JSONObject.optIntOrNull(name: String): Int? = if (isNull(name) || !has(name)) null else optInt(name)
