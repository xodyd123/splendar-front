package com.example.splendar.domain

import kotlinx.serialization.Serializable

@Serializable
data class PlayerDto(val nickname: String, val isReady: Boolean)