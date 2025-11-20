package com.example.splendar.domain.game

import kotlinx.serialization.Serializable

@Serializable
data class ChoicePlayer(
    val currentTurnId: String,
    val roomId: Int,
    val splendorAction: String
) {

}
