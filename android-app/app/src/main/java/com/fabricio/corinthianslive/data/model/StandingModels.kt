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
