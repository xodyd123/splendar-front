package com.example.splendar.domain.game.request

import com.example.splendar.domain.game.GemType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SelectToken(
    val roomId: Int,
    val playerId: String,
    //val currentTurnId: String,
    val token: GemType,
    val selectStatus : SelectStatus
) {

}
