package com.example.splendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.splendar.domain.token.GemType
import com.example.splendar.domain.game.PlayerState
import com.example.splendar.domain.card.StaticCard


@Composable
fun CardPurchaseConfirmationScreen(
    cardToBuy: StaticCard,
    playerState: PlayerState,
    onDismiss: () -> Unit
) {
    // 1. 필요한 비용 계산
    val costs = listOf(
        GemType.DIAMOND to cardToBuy.costDiamond,
        GemType.SAPPHIRE to cardToBuy.costSapphire,
        GemType.EMERALD to cardToBuy.costEmerald,
        GemType.RUBY to cardToBuy.costRuby,
        GemType.ONYX to cardToBuy.costOnyx
    ).filter { it.second > 0 }

    var totalMissingTokens = 0
    costs.forEach { (type, cost) ->
        val discount = playerState.bonuses[type] ?: 0
        val netCost = maxOf(0, cost - discount)
        val playerHas = playerState.tokens[type] ?: 0
        if (playerHas < netCost) {
            totalMissingTokens += (netCost - playerHas)
        }
    }
    val playerGold = playerState.tokens[GemType.GOLD] ?: 0
    val canAfford = playerGold >= totalMissingTokens


    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .background(Color(0xFFFFFBE5), RoundedCornerShape(12.dp)) // 구매 색상 테마로 변경
            .border(1.dp, Color(0xFFFDD835), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))


        SplendorCard(
            card = cardToBuy,
            onClick = { clickedCardId ->

                println("인라인 클릭: $clickedCardId")
            }
        )
        Text("아이콘을 클릭하면 다시 내려놓습니다", fontSize = 10.sp, color = Color.Gray)


        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("지불해야 할 최종 비용:", fontSize = 12.sp, color = Color.Gray)

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                costs.forEach { (type, originalCost) ->
                    val discount = playerState.bonuses[type] ?: 0
                    val netCost = maxOf(0, originalCost - discount)

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(contentAlignment = Alignment.Center) {
                            GemIcon(type = type, size = 32.dp)
                            if (discount > 0) {
                                Text("-$discount", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(
                            text = netCost.toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = when {
                canAfford -> "구매 가능! 토큰이 자동 차감됩니다."
                !canAfford && playerGold > 0 -> "황금 ${playerGold}개 보유. ${totalMissingTokens}개 부족."
                else -> "자원 부족!"
            },
            fontSize = 12.sp,
            color = if (canAfford) Color(0xFF43A047) else Color.Red,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

    }
}