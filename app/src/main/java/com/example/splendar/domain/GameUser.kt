package com.example.splendar.domain

data class GameUser(
    val id: String,
    val username: String,
    val isReady: Boolean = false // 유저가 '준비' 버튼을 눌렀는지 여부
)
