package com.example.splendar.domain

data class GameUser(
    val id: String,
    val username: String,
    val isReady: Boolean = false,

)
