package com.example.splendar.domain.game

import kotlinx.serialization.Serializable

@Serializable
data class StaticCard(
    val level: Int,
    val bonusGem: GemType,
    val id: Int,
    val points: Int,
    val costDiamond: Int,
    val costSapphire: Int,
    val costEmerald: Int,
    val costRuby: Int,
    val costOnyx: Int
)
