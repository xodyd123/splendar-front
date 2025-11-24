package com.example.splendar.domain.token

import com.example.splendar.domain.game.GemType
import kotlinx.serialization.Serializable

@Serializable
data class SelectedToken(
    val token: Map<GemType, Int>
)
