package com.example.splendar.domain.card

import kotlinx.serialization.Serializable

@Serializable
data class StaticNoble(
    val id: Int,
    val points: Int,
    val costDiamond: Int,
    val costSapphire: Int,
    val costEmerald: Int,
    val costRuby: Int,
    val costOnyx: Int
)