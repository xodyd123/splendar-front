package com.example.splendar.domain.game.response

import com.example.splendar.domain.game.GameScreen
import kotlinx.serialization.Serializable

@Serializable
data class SelectedPlayer(
    val splendorAction: String,
    val playerAction: GameScreen
)
