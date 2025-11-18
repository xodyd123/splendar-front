package com.example.splendar.domain.game

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.splendar.ui.getGemColor
import kotlinx.serialization.Serializable

@Serializable
data class GameState(
    val boardStateDto: BoardState,
    val playerStateDto: List<PlayerState>,
    val gameId: Int,
    val currentPlayer: GamePlayer
) {
    fun extractBoardData(): BoardData {
        // ⚠️ 실제 GameRoom DTO 구조에 맞춰 구현해야 합니다.
        // 여기서는 예시로 더미 데이터를 생성합니다.
        val cards: List<List<StaticCard>> = boardStateDto.cards
        val nobleTiles = boardStateDto.nobles
        val tokens = boardStateDto.availableTokens
        val uiToken = mapAvailableTokensToUI(tokens)


        return BoardData(
            nobleTiles = nobleTiles, // 실제 GameRoom에서 추출
            level3Cards = cards[2],
            level2Cards = cards[1],
            level1Cards = cards[0],
            tokens = uiToken,
            playerState = playerStateDto
        )
    }

    fun mapAvailableTokensToUI(availableTokens: Map<GemType, Int>): List<Tokens> {
        
        val TOKEN_SIZE: Dp = 48.dp

        // 2. Map을 순회하며 List<Tokens>로 변환합니다.
        return availableTokens
            // Map의 각 엔트리(GemType, Count)를 Tokens 객체로 매핑합니다.
            .map { (gemType, count) ->
                Tokens(
                    count = count, // Map의 Value (토큰 개수)
                    size = TOKEN_SIZE, // 고정된 UI 크기
                    color = getGemColor(gemType) // Map의 Key (GemType)를 Color로 변환
                )
            }
            .toList()
    }
}
