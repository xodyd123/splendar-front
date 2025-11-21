package com.example.splendar.domain

enum class PlayerAction {
    // 1. 토큰 가져오기 (3개 다른 색 or 2개 같은 색)
    TAKE_TOKENS,

    // 2. 카드 구입 (개발 카드 구매)
    BUY_CARD,

    // 3. 카드 예약 (보드 or 덱에서 찜하기 + 황금 토큰 획득)
    RESERVE_CARD,

}