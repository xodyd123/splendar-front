package com.example.splendar.domain.game

import com.example.splendar.domain.token.GemType
import kotlinx.serialization.Serializable

@Serializable
data class PlayerState(
    val player: GamePlayer,
    val score: Int,
    val tokens: Map<GemType, Int>,
    val bonuses: Map<GemType, Int>
)
