package com.example.splendar.domain.game

import kotlinx.serialization.Serializable

@Serializable
data class BoardState(
    val cards: List<List<StaticCard>>,
    val nobles: List<StaticNoble>,
    val availableTokens: Map<GemType, Int>
)
