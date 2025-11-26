package com.example.splendar.domain.game

import com.example.splendar.domain.card.StaticCard
import com.example.splendar.domain.card.StaticNoble
import com.example.splendar.domain.token.GemType
import kotlinx.serialization.Serializable

@Serializable
data class BoardState(
    val cards: List<List<StaticCard>>,
    val nobles: List<StaticNoble>,
    val availableTokens: Map<GemType, Int>
)
