package com.example.splendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.splendar.domain.game.GemType
import com.example.splendar.domain.game.PlayerState
import com.example.splendar.domain.game.StaticCard


@Composable
fun CardPurchaseConfirmationScreen(
    cardToBuy: StaticCard, // 구매할 카드 정보
    playerState: PlayerState, // 현재 플레이어의 자원 정보
    onConfirmPurchase: () -> Unit, // 구매 확정 시 호출될 액션
    onCancel: () -> Unit // 취소 시 호출될 액션
) {
    // 1. 필요한 비용 계산
    val costs = listOf(
        GemType.DIAMOND to cardToBuy.costDiamond,
        GemType.SAPPHIRE to cardToBuy.costSapphire,
        GemType.EMERALD to cardToBuy.costEmerald,
        GemType.RUBY to cardToBuy.costRuby,
        GemType.ONYX to cardToBuy.costOnyx
    ).filter { it.second > 0 } // 비용이 0인 것은 제외

    // 2. 할인 및 구매 가능 여부 계산
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

    // 3. UI 구성 (기존 CurrentTokenSelectScreen 레이아웃 재활용)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .background(Color(0xFFFFFBE5), RoundedCornerShape(12.dp)) // 구매 색상 테마로 변경
            .border(1.dp, Color(0xFFFDD835), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        // [상단 텍스트 변경]
        Text(
            text = "카드 구매 확정",
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFFE53935)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ⭐️ 구매할 카드 표시
        SplendorCard(card = cardToBuy, modifier = Modifier.size(width = 60.dp, height = 90.dp))

        Spacer(modifier = Modifier.height(12.dp))

        // ⭐️ 필요 비용 및 할인 목록 표시
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("지불해야 할 최종 비용:", fontSize = 12.sp, color = Color.Gray)

            // 비용 항목 나열
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                costs.forEach { (type, originalCost) ->
                    val discount = playerState.bonuses[type] ?: 0
                    val netCost = maxOf(0, originalCost - discount)

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // 아이콘
                        Box(contentAlignment = Alignment.Center) {
                            GemIcon(type = type, size = 32.dp)
                            // 할인 적용 시 작은 텍스트 추가
                            if (discount > 0) {
                                Text("-$discount", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        // 최종 비용 텍스트
                        Text(
                            text = netCost.toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black
                            //if (playerHas < netCost && type != GemType.GOLD) Color.Red else Color.Black
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ⭐️ 구매 가능 여부 메시지
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

        // ⭐️ 구매/취소 버튼
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = onConfirmPurchase,
                enabled = canAfford,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))
            ) {
                Text("구매 확정")
            }
            Button(onClick = onCancel, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                Text("취소")
            }
        }
    }
}