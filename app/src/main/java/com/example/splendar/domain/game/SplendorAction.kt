package com.example.splendar.domain.game

enum class SplendorAction(
    // 서버로 전송할 때 사용할 수 있는 고유한 문자열 ID
    val actionId: String,
    // UI에 표시되는 한국어 설명 텍스트
    val description: String
) {
    // 1번 액션: 토큰 가져오기
    TAKE_TOKENS(
        actionId = "TAKE_TOKENS",
        description = "서로 다른 3가지 보석 토큰을 1개씩 가져오거나, 한 종류 보석 토큰 2개를 가져온다"
    ),

    // 2번 액션: 카드 구입
    BUY_CARD(
        actionId = "BUY_CARD",
        description = "가지고 있는 보석 토큰을 내고 바닥에 놓인(혹은 자신이 보관하고 있는) 개발 카드 1장을 구입한다"
    ),

    // 3번 액션: 카드 예약
    RESERVE_CARD(
        actionId = "RESERVE_CARD",
        description = "개발 카드 1장을 수중에 보관하고, 황금 토큰 1개를 받는다"
    )
}