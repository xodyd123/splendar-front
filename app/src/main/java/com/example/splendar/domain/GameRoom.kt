package com.example.splendar.domain

import kotlinx.serialization.Serializable

@Serializable
data class GameRoom(val roomName: String,
                    val userName : String ) {


}