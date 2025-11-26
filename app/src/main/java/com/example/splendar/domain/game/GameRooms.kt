package com.example.splendar.domain.game

import kotlinx.serialization.Serializable

@Serializable
data class GameRooms(
    val roomName: String,
    val roomId: Int,
    val roomStatus: RoomStatus,
    val hostName: String,
    val playerCount: Int,
    val players: List<PlayerDto>,

)
