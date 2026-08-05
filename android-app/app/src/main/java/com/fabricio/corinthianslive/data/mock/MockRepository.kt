package com.fabricio.corinthianslive.data.mock

import com.fabricio.corinthianslive.data.model.*

class MockRepository {

    fun nextMatches(): List<Match> = listOf(
        Match(
            competition = "Campeonato Paulista",
            home = "Corinthians",
            away = "Santos",
            date = "Sáb, 01/03",
            time = "18:30",
            stadium = "Neo Química Arena",
            city = "São Paulo"
        ),
        Match(
            competition = "Copa do Brasil",
            home = "Flamengo",
            away = "Corinthians",
            date = "Qua, 05/03",
            time = "21:30",
            stadium = "Maracanã",
            city = "Rio de Janeiro"
        ),
        Match(
            competition = "Brasileirão Série A",
            home = "Corinthians",
            away = "Palmeiras",
            date = "Dom, 09/03",
            time = "16:00",
            stadium = "Neo Química Arena",
            city = "São Paulo"
        ),
        Match(
            competition = "Sul-Americana",
            home = "Cerro Porteño",
            away = "Corinthians",
            date = "Qui, 13/03",
            time = "19:00",
            stadium = "Defensores del Chaco",
            city = "Assunção"
        )
    )

    fun liveMatch(): LiveMatch = LiveMatch(
        competition = "Brasileirão Série A",
        stadium = "Neo Química Arena",
        home = "Corinthians",
        away = "São Paulo",
        scoreHome = 2,
        scoreAway = 1,
        minute = 67
    )

    fun liveEvents(): List<MatchEvent> = listOf(
        MatchEvent(1, "Corinthians", EventType.Kickoff, "Bola rolando"),
        MatchEvent(12, "Corinthians", EventType.Goal, "Yuri Alberto finaliza de primeira (1–0)"),
        MatchEvent(28, "São Paulo", EventType.YellowCard, "Falta dura no meio-campo"),
        MatchEvent(41, "São Paulo", EventType.Goal, "Cabeceio após escanteio (1–1)"),
        MatchEvent(55, "Corinthians", EventType.Substitution, "Entra Wesley, sai Romero"),
        MatchEvent(63, "Corinthians", EventType.Goal, "Contra-ataque rápido (2–1)"),
        MatchEvent(66, "Corinthians", EventType.YellowCard, "Reclamação com a arbitragem"),
        MatchEvent(67, "VAR", EventType.Var, "Checagem de possível pênalti (nada marcado)")
    )

    fun standings(): List<Standing> = listOf(
        Standing(1, "Flamengo", 25, 10, 8, 1, 1, 16),
        Standing(2, "Palmeiras", 23, 10, 7, 2, 1, 11),
        Standing(3, "Corinthians", 20, 10, 6, 2, 2, 6),
        Standing(4, "Atlético-MG", 18, 10, 5, 3, 2, 4),
        Standing(5, "São Paulo", 17, 10, 5, 2, 3, 1),
        Standing(6, "Grêmio", 15, 10, 4, 3, 3, 2),
        Standing(7, "Internacional", 14, 10, 4, 2, 4, 1),
        Standing(8, "Botafogo", 13, 10, 3, 4, 3, 0)
    )
}