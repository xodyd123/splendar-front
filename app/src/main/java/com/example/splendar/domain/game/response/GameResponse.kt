package com.example.splendar.domain.game.response

import com.example.splendar.domain.game.GameState
import kotlinx.serialization.Serializable

@Serializable
data class GameResponse(

    val status: String,


    val data: GameState?,

    val message: String?,
)
