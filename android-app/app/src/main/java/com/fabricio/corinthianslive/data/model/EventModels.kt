package com.fabricio.corinthianslive.data.model

enum class EventType {
    Kickoff,
    Goal,
    YellowCard,
    RedCard,
    Substitution,
    Shot,
    Foul,
    Corner,
    Offside,
    Save,
    Penalty,
    Var,
    Other
}

data class MatchEvent(
    val id: String,
    val minute: Int,
    val clock: String,
    val period: String,
    val team: String,
    val type: EventType,
    val description: String,
    val createdAt: String = ""
)
