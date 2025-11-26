package com.example.splendar.domain.game

import kotlinx.serialization.Serializable

@Serializable
data class CreateGameRoom(
    val roomName: String,
    val hostName: String,
    val isHosted: Boolean,
) {


}