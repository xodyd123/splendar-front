package com.example.splendar.domain

import android.R
import kotlinx.serialization.Serializable

@Serializable
data class GameRoom(val roomName: String,
                    val hostName : String ,
                    val isHosted : Boolean ,
    ) {


}