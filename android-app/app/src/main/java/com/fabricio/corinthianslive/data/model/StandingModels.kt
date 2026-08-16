package com.fabricio.corinthianslive.data.model

data class Standing(
    val position: Int,
    val teamName: String,
    val points: Int,
    val played: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val goalDifference: Int,
    val form: String = ""
)

data class StandingGroup(
    val name: String,
    val entries: List<Standing>
)

data class BracketGame(
    val id: Long,
    val date: String,
    val home: String,
    val away: String,
    val scoreHome: Int?,
    val scoreAway: Int?,
    val penaltyHome: Int? = null,
    val penaltyAway: Int? = null
)

data class BracketTie(
    val name: String,
    val games: List<BracketGame>
)

data class BracketRound(
    val name: String,
    val ties: List<BracketTie>
)

data class CompetitionTable(
    val name: String,
    val phase: String,
    val kind: String,
    val groups: List<StandingGroup>,
    val brackets: List<BracketRound>
)
