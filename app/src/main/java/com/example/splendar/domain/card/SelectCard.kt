package com.example.splendar.domain.card

import com.example.splendar.domain.game.request.SelectStatus
import kotlinx.serialization.Serializable

@Serializable
data class SelectCard(
    val roomId: Int,
    val playerId: String,
    val cardId: Int,
    val selectStatus: SelectStatus
)
