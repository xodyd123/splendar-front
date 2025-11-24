package com.example.splendar.domain

import kotlinx.serialization.Serializable

@Serializable
data class GameRooms(
    val roomName: String,
    val roomId: Long,
    val roomStatus: RoomStatus,
    val hostName: String,
    val playerCount: Int,
    val players: List<PlayerDto>,

)
