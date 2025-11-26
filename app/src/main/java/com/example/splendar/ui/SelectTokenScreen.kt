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
    val displayText = errorMessage ?: "이번 턴에 가져올 토큰 (${size}개/최대 3개)"


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
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            playerSelectedToken?.token?.forEach { type ->

                Box(
                    modifier = Modifier
                        .clickable {
                            onRemoveToken(
                                SelectToken(
                                    roomId, playerId, type.key,
                                    SelectStatus.NO_SELECT
                                )
                            )
                        }
                ) {
                    GemIcon(type = type.key, size = 32.dp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("아이콘을 클릭하면 다시 내려놓습니다", fontSize = 10.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(8.dp))

    }
}
