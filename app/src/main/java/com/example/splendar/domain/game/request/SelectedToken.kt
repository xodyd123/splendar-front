package com.example.splendar.domain.game.request

import com.example.splendar.domain.game.GemType
import kotlinx.serialization.Serializable

@Serializable
data class SelectedToken(
    val playerId: String,
    val currentTurnId: String,
    val token: GemType
) {

}
