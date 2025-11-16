package com.example.splendar.domain

data class GameUser(
    val id: Long,
    val username: String,
    val isReady: Boolean = false ,

)
