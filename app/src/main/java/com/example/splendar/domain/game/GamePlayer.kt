package com.example.splendar.domain.game

import kotlinx.serialization.Serializable

@Serializable
data class GamePlayer(
    val playerName: String, val playerId: String
)
