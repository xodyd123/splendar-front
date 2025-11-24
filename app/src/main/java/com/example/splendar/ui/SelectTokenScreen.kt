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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.splendar.domain.game.GemType
import com.example.splendar.domain.game.request.SelectStatus
import com.example.splendar.domain.game.request.SelectToken
import com.example.splendar.domain.token.SelectedToken
import kotlin.collections.forEach

@Composable
fun CurrentTokenSelectScreen(
    playerSelectedToken: SelectedToken?,
    onRemoveToken: (SelectToken) -> Unit,
    errorMessage: String?,
    playerId: String,
    roomId: Int

) {
    val size: Int = playerSelectedToken?.token?.size ?: 0
    // 1. 텍스트 내용 결정: 에러 메시지가 null이 아니면 에러 메시지, 아니면 기본 텍스트
    val displayText = errorMessage ?: "이번 턴에 가져올 토큰 (${size}개/최대 3개)"

    // 2. 색상 결정: 에러 메시지가 null이 아니면 빨간색, 아니면 회색
    val displayColor = if (errorMessage != null) Color.Red else Color.Gray

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .background(Color(0xFFE3F2FD), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF2196F3), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            text = displayText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = displayColor
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 선택한 토큰 아이콘 나열
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            playerSelectedToken?.token?.forEach { type ->
                // ⭐️ 아이콘을 Box로 감싸고 clickable 추가
                Box(
                    modifier = Modifier
                        .clickable {
                            onRemoveToken(
                                SelectToken(
                                    roomId, playerId, type.key,
                                    SelectStatus.NO_SELECT
                                )
                            )
                        } // 클릭 시 해당 토큰 제거 요청
                ) {
                    GemIcon(type = type.key, size = 32.dp)
                    // (선택 사항) 우측 상단에 작은 '-' 나 'x' 표시를 겹쳐서 보여주면 더 직관적입니다.
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // (안내 문구 추가)
        Text("아이콘을 클릭하면 다시 내려놓습니다", fontSize = 10.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(8.dp))

    }
}
