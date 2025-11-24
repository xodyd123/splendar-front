package com.example.splendar.domain.game.response

import com.example.splendar.domain.game.GameState
import kotlinx.serialization.Serializable

@Serializable
data class GameResponse(
    // 서버 응답의 최상위 필드 1: "status"
    val status: String,

    // 서버 응답의 최상위 필드 2: "data"
    // ⭐️ 이 필드 안에 GameState의 실제 내용이 들어 있습니다.
    val data: GameState,

    val message: String?,
)
