package com.fabricio.corinthianslive.data.model

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
    val scoreAway: Int? = null
)

data class LiveMatch(
    val id: Long = 0,
    val competition: String,
    val stadium: String,
    val home: String,
    val away: String,
    val scoreHome: Int,
    val scoreAway: Int,
    val minute: Int
)

data class LiveContent(
    val match: LiveMatch?,
    val events: List<MatchEvent>
)

data class RepositoryResult<T>(
    val data: T,
    val source: String,
    val generatedAt: String,
    val notice: String? = null
) {
    val isDemo: Boolean get() = source == "demo"
}
