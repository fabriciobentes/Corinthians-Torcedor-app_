package com.fabricio.corinthianslive.data.model

enum class EventType {
    Kickoff,
    Goal,
    YellowCard,
    RedCard,
    Substitution,
    Var
}

data class MatchEvent(
    val minute: Int,
    val team: String,
    val type: EventType,
    val description: String
)