package com.example.splendar.domain.game

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp


data class Tokens(
    val count: Int,  // 해당 색상 토큰의 남은 개수 (예: 7)
    val size: Dp,    // (UI 전용) 화면에 표시될 토큰의 크기 (예: 48.dp)
    val color: Color // (UI 전용) 화면에 표시될 토큰의 색상 (예: Color.Blue)
)

