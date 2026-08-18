package com.fabricio.corinthianslive.data

import com.fabricio.corinthianslive.data.model.EventType
import com.fabricio.corinthianslive.data.model.LiveContent
import com.fabricio.corinthianslive.data.model.LiveMatch
import com.fabricio.corinthianslive.data.model.MatchEvent
import com.fabricio.corinthianslive.data.model.Player
import com.fabricio.corinthianslive.data.model.TeamMatchStats
import com.fabricio.corinthianslive.data.model.TeamSquad
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer
import java.time.OffsetDateTime
import java.time.ZonedDateTime

internal class RealtimeLiveDataSource {
    fun fetch(detailsUrl: String, fallback: LiveContent): LiveContent {
        val baseMatch = requireNotNull(fallback.match)
        val separator = if (detailsUrl.contains('?')) '&' else '?'
        val connection = (URL(detailsUrl + separator + "corinthiansRealtime=" + System.currentTimeMillis())
            .openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 10_000
            useCaches = false
            setRequestProperty("Accept", "text/html,application/xhtml+xml")
            setRequestProperty("Cache-Control", "no-cache, no-store")
            setRequestProperty("Pragma", "no-cache")
            setRequestProperty("User-Agent", "Corinthians-Torcedor-app/3.1 (Android)")
        }

        val html = try {
            if (connection.responseCode !in 200..299) error("Fonte ao vivo respondeu HTTP ${connection.responseCode}")
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }

        val anchor = html.indexOf("window.trv2")
        if (anchor < 0) error("Dados em tempo real não encontrados")
        val matchJson = extractObject(html, "\"match\":", anchor)
            ?: error("Partida em tempo real não encontrada")
        val plays = extractArray(html, "plays: Array.from(", anchor) ?: JSONArray()
        val statistics = extractObject(html, "statistics:", anchor)
        val currentTime = Regex("\\\"currentTime\\\":\\\"([^\\\"]+)\\\"")
            .find(html.substring(anchor, minOf(html.length, anchor + 2_500)))
            ?.groupValues
            ?.getOrNull(1)
            .orEmpty()

        val liveMatch = parseLiveMatch(matchJson, baseMatch, currentTime)
        val directEvents = parseEvents(plays)
        val squads = matchJson.optJSONObject("squads")
        val homeSquad = squads?.optJSONObject("homeTeam")?.let { parseSquad(it, liveMatch.home) }
        val awaySquad = squads?.optJSONObject("awayTeam")?.let { parseSquad(it, liveMatch.away) }
        val mergedEvents = (fallback.events + directEvents)
            .associateBy { it.id }
            .values
            .sortedWith(compareBy<MatchEvent> { it.createdAt.ifBlank { "9999" } }.thenBy { it.minute })
        val hasOfficialLineup = !homeSquad?.starters.isNullOrEmpty() && !awaySquad?.starters.isNullOrEmpty()

        return LiveContent(
            match = liveMatch,
            events = mergedEvents,
            homeSquad = homeSquad ?: fallback.homeSquad,
            awaySquad = awaySquad ?: fallback.awaySquad,
            homeStats = statistics?.optJSONObject("homeTeam")?.let(::parseTeamStats) ?: fallback.homeStats,
            awayStats = statistics?.optJSONObject("awayTeam")?.let(::parseTeamStats) ?: fallback.awayStats,
            lineupStatus = if (hasOfficialLineup) "Escalação oficial" else fallback.lineupStatus
        )
    }

    private fun parseLiveMatch(item: JSONObject, fallback: LiveMatch, currentTime: String): LiveMatch {
        val home = item.optJSONObject("homeTeam")
        val away = item.optJSONObject("awayTeam")
        val location = item.optJSONObject("location")
        val scoreboard = item.optJSONObject("scoreboard")
        val detailed = item.optJSONObject("detailedScoreboard")
        val status = parseStatus(item, fallback)
        return fallback.copy(
            stadium = location?.optString("popularName")?.ifBlank { fallback.stadium } ?: fallback.stadium,
            city = cityFromLocation(location).ifBlank { fallback.city },
            home = home?.optString("popularName")?.ifBlank { home.optString("name") }
                ?.ifBlank { fallback.home } ?: fallback.home,
            away = away?.optString("popularName")?.ifBlank { away.optString("name") }
                ?.ifBlank { fallback.away } ?: fallback.away,
            scoreHome = scoreboard?.optIntOrNull("home")
                ?: detailed?.optIntOrNull("firstParticipantScore")
                ?: fallback.scoreHome,
            scoreAway = scoreboard?.optIntOrNull("away")
                ?: detailed?.optIntOrNull("secondParticipantScore")
                ?: fallback.scoreAway,
            minute = currentTime.substringBefore(':').toIntOrNull() ?: fallback.minute,
            statusShort = status.first,
            statusLong = status.second
        )
    }

    private fun parseStatus(item: JSONObject, fallback: LiveMatch): Pair<String, String> {
        val transmission = item.optJSONObject("transmission")
        val broadcast = transmission?.optJSONObject("broadcastStatus")
        val statusText = listOf(
            broadcast?.optString("id"),
            broadcast?.optString("label"),
            item.optString("status"),
            item.optString("moment")
        ).joinToString(" ").simplify()
        val moment = item.optString("moment").uppercase()
        return when {
            moment == "PAST" || "encerrad" in statusText || "finaliz" in statusText -> "FT" to "Encerrado"
            "intervalo" in statusText -> "HT" to "Intervalo"
            "adiad" in statusText -> "PST" to "Adiado"
            "cancel" in statusText -> "CANC" to "Cancelado"
            moment == "NOW" || "andamento" in statusText || "ao vivo" in statusText -> "LIVE" to "Ao vivo"
            isInsideMatchWindow(fallback.kickoff) -> "LIVE" to "Partida em andamento"
            else -> fallback.statusShort to fallback.statusLong
        }
    }

    private fun isInsideMatchWindow(kickoff: String): Boolean {
        val start = runCatching { OffsetDateTime.parse(kickoff).toZonedDateTime() }.getOrNull() ?: return false
        val now = ZonedDateTime.now(start.zone)
        return !now.isBefore(start) && now.isBefore(start.plusHours(4))
    }

    private fun parseEvents(items: JSONArray): List<MatchEvent> = buildList {
        for (index in 0 until items.length()) {
            val play = items.optJSONObject(index) ?: continue
            val playType = play.optJSONObject("playType")?.optString("id").orEmpty().uppercase()
            if (playType in IGNORED_EVENT_TYPES) continue
            val description = eventDescription(play)
            val plain = description.simplify()
            val type = when {
                playType == "GOAL" -> EventType.Goal
                playType == "RED_CARD" -> EventType.RedCard
                playType == "YELLOW_CARD" || playType == "CARD" && "vermelh" !in plain -> EventType.YellowCard
                playType == "CARD" -> EventType.RedCard
                playType in setOf("SUBSTITUTION", "SUBST") -> EventType.Substitution
                playType == "SHOT" || "chute" in plain || "finaliza" in plain || "cabec" in plain -> EventType.Shot
                playType == "FOUL" || "falta" in plain -> EventType.Foul
                playType == "CORNER" || "escanteio" in plain -> EventType.Corner
                playType == "OFFSIDE" || "impedimento" in plain -> EventType.Offside
                playType == "SAVE" || "defesa" in plain -> EventType.Save
                playType == "PENALTY" || "penalti" in plain -> EventType.Penalty
                playType == "VAR" || "var" in plain -> EventType.Var
                playType == "KICKOFF" -> EventType.Kickoff
                else -> EventType.Other
            }
            val clock = play.optString("moment")
            val period = play.optJSONObject("period")
            val team = play.optJSONObject("details")?.optJSONObject("team")
            val teamName = team?.optString("popularName")
                ?.ifBlank { team.optString("abbreviation") }
                ?.ifBlank { team.optString("name") }
                .orEmpty()
            val createdAt = play.optString("createdAt")
            val fallbackId = listOf(period?.optString("abbreviation"), clock, teamName, description.hashCode())
                .joinToString("-")
            add(
                MatchEvent(
                    id = play.optString("id").ifBlank { fallbackId },
                    minute = clock.substringBefore(':').toIntOrNull() ?: 0,
                    clock = clock,
                    period = period?.optString("abbreviation")
                        ?.ifBlank { period.optString("label") }
                        .orEmpty(),
                    team = teamName,
                    type = type,
                    description = description,
                    createdAt = createdAt
                )
            )
        }
    }

    private fun eventDescription(play: JSONObject): String {
        val blocks = play.optJSONObject("body")?.optJSONArray("blocks")
        val text = buildList {
            if (blocks != null) {
                for (index in 0 until blocks.length()) {
                    blocks.optJSONObject(index)?.optString("text")?.takeIf { it.isNotBlank() }?.let(::add)
                }
            }
        }.joinToString("\n")
        return text.ifBlank {
            play.optString("title").ifBlank {
                play.optJSONObject("playType")?.optString("label").orEmpty().ifBlank { "Lance da partida" }
            }
        }
    }

    private fun parseSquad(item: JSONObject, teamName: String): TeamSquad = TeamSquad(
        teamName = teamName,
        formation = item.optString("formation"),
        coach = item.optJSONObject("coach")?.optString("popularName")
            ?.ifBlank { item.optJSONObject("coach")?.optString("name").orEmpty() }
            .orEmpty(),
        starters = item.optJSONArray("lineUp").players(),
        bench = item.optJSONArray("bench").players()
    )

    private fun JSONArray?.players(): List<Player> = buildList {
        val array = this@players ?: return@buildList
        for (index in 0 until array.length()) {
            val player = array.optJSONObject(index) ?: continue
            add(
                Player(
                    name = player.optString("popularName")
                        .ifBlank { player.optString("nickName") }
                        .ifBlank { player.optString("name") },
                    shirtNumber = player.optString("shirtNumber"),
                    position = player.optJSONObject("position")?.optString("description").orEmpty()
                )
            )
        }
    }

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

    private fun extractObject(source: String, marker: String, fromIndex: Int): JSONObject? =
        extractJsonValue(source, marker, fromIndex)?.let(::JSONObject)

    private fun extractArray(source: String, marker: String, fromIndex: Int): JSONArray? =
        extractJsonValue(source, marker, fromIndex)?.let(::JSONArray)

    private fun extractJsonValue(source: String, marker: String, fromIndex: Int): String? {
        val markerIndex = source.indexOf(marker, fromIndex)
        if (markerIndex < 0) return null
        val objectStart = source.indexOf('{', markerIndex + marker.length)
        val arrayStart = source.indexOf('[', markerIndex + marker.length)
        val start = listOf(objectStart, arrayStart).filter { it >= 0 }.minOrNull() ?: return null
        val opening = source[start]
        val closing = if (opening == '{') '}' else ']'
        var depth = 0
        var inString = false
        var escaped = false
        for (index in start until source.length) {
            val char = source[index]
            if (inString) {
                when {
                    escaped -> escaped = false
                    char == '\\' -> escaped = true
                    char == '"' -> inString = false
                }
                continue
            }
            when (char) {
                '"' -> inString = true
                opening -> depth++
                closing -> {
                    depth--
                    if (depth == 0) return source.substring(start, index + 1)
                }
            }
        }
        return null
    }

    private fun cityFromLocation(location: JSONObject?): String {
        val regionParts = location?.optString("region")
            .orEmpty()
            .split(',')
            .map(String::trim)
            .filter(String::isNotBlank)
        return regionParts.getOrNull(1).orEmpty()
    }

    private fun JSONObject.optIntOrNull(name: String): Int? =
        if (!has(name) || isNull(name)) null else optInt(name)

    private fun JSONObject.statTotal(name: String): Int =
        optJSONObject(name)?.optInt("total") ?: 0

    private fun String.simplify(): String = Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase()

    private companion object {
        val IGNORED_EVENT_TYPES = setOf(
            "POSTGAME",
            "POSTGAME_HIGHLIGHT",
            "SUMMARY_AUTOMATIC",
            "STANDOUT_PLAYER"
        )
    }
}
