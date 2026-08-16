package com.fabricio.corinthianslive.data.mock

import com.fabricio.corinthianslive.data.model.EventType
import com.fabricio.corinthianslive.data.model.LiveMatch
import com.fabricio.corinthianslive.data.model.Match
import com.fabricio.corinthianslive.data.model.MatchEvent
import com.fabricio.corinthianslive.data.model.Standing

class MockRepository {
    fun nextMatches(): List<Match> = listOf(
        Match(
            competition = "Campeonato Paulista",
            home = "Corinthians",
            away = "Santos",
            date = "Sáb, 01/03",
            time = "17:30",
            stadium = "Neo Química Arena",
            city = "São Paulo"
        ),
        Match(
            competition = "Copa do Brasil",
            home = "Flamengo",
            away = "Corinthians",
            date = "Qua, 05/03",
            time = "20:30",
            stadium = "Maracanã",
            city = "Rio de Janeiro"
        )
    )

    fun liveMatch(): LiveMatch = LiveMatch(
        competition = "Brasileirão Série A",
        stadium = "Neo Química Arena",
        city = "São Paulo",
        home = "Corinthians",
        away = "São Paulo",
        scoreHome = 2,
        scoreAway = 1,
        minute = 67,
        statusShort = "LIVE",
        statusLong = "Ao vivo",
        kickoff = ""
    )

    fun liveEvents(): List<MatchEvent> = listOf(
        MatchEvent("demo-1", 1, "01:00", "1T", "Corinthians", EventType.Kickoff, "Bola rolando"),
        MatchEvent("demo-2", 12, "12:00", "1T", "Corinthians", EventType.Goal, "Yuri Alberto finaliza de primeira (1–0)")
    )

    fun standings(): List<Standing> = listOf(
        Standing(1, "Flamengo", 25, 10, 8, 1, 1, 16),
        Standing(2, "Palmeiras", 23, 10, 7, 2, 1, 11),
        Standing(3, "Corinthians", 20, 10, 6, 2, 2, 6),
        Standing(4, "Atlético-MG", 18, 10, 5, 3, 2, 4),
        Standing(5, "São Paulo", 17, 10, 5, 2, 3, 1)
    )
}
