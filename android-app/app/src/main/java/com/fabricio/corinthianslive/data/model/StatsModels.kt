package com.fabricio.corinthianslive.data.model

data class StatsSummary(
    val matches: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val goalDifference: Int,
    val cleanSheets: Int,
    val scoringGames: Int,
    val pointsPercentage: Int,
    val averageGoalsFor: Double,
    val averageGoalsAgainst: Double,
    val currentStreak: String
)

data class CompetitionStats(
    val name: String,
    val matches: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val goalsFor: Int,
    val goalsAgainst: Int
)

data class DetailedMatchStats(
    val match: Match,
    val homeStats: TeamMatchStats?,
    val awayStats: TeamMatchStats?,
    val events: List<MatchEvent>
)

data class TeamStats(
    val window: Int,
    val summary: StatsSummary,
    val form: List<String>,
    val competitions: List<CompetitionStats>,
    val recentMatches: List<Match>,
    val matchDetails: List<DetailedMatchStats> = emptyList()
)
