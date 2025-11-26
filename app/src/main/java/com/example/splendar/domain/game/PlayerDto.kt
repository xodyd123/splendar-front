package com.example.splendar.domain.game

import kotlinx.serialization.Serializable

@Serializable
data class PlayerDto(val nickname: String, val isReady: Boolean)