package com.example.splendar.ui


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.splendar.domain.game.GamePlayer
import com.example.splendar.domain.token.GemType
import com.example.splendar.domain.game.PlayerState
import com.example.splendar.domain.card.StaticCard
import com.example.splendar.domain.card.StaticNoble
import com.example.splendar.domain.token.Tokens
import com.example.splendar.domain.game.request.SelectToken
import kotlin.collections.getOrNull

fun getGemColor(type: GemType): Color {
    return when (type) {
        GemType.DIAMOND -> Color(0xFFEEEEEE) // 흰색/회색 (다이아)
        GemType.SAPPHIRE -> Color(0xFF1E88E5) // 파랑 (사파이어)
        GemType.EMERALD -> Color(0xFF43A047) // 초록 (에메랄드)
        GemType.RUBY -> Color(0xFFE53935)    // 빨강 (루비)
        GemType.ONYX -> Color(0xFF424242)    // 검정/진회색 (오닉스)
        GemType.GOLD -> Color(0xFFFFD700)    // 노랑 (황금)
    }
}

fun getCardLevelColor(level: Int): Color {
    return when (level) {
        1 -> Color(0xFFA5D6A7) // 연한 초록색 (L1)
        2 -> Color(0xFFFFF59D) // 연한 노란색 (L2)
        3 -> Color(0xFF90CAF9) // 연한 파란색 (L3)
        else -> Color.Gray
    }
}

@Composable
fun TokenStackComponent(
    token: Tokens,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    // Box를 사용하여 원형 모양과 텍스트를 포함
    Box(
        modifier = modifier
            .size(token.size) // Tokens 데이터의 size를 활용
            .background(token.color, CircleShape)
            .clickable(enabled = onClick != null) {
                onClick?.invoke() // ⭐️ 클릭 이벤트 발생 시 함수를 호출
            }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        // 남은 개수 표시
        Text(
            text = token.count.toString(),
            color = if (token.color == Color.Black || token.color == Color(0xFF424242)) Color.White else Color.Black,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun HorizontalStackCirclesPreview(
    tokens: List<Tokens>,
    pickToken: ((gemType: GemType) -> Unit)?
) {
    // Column을 사용하여 토큰 더미들을 수직으로 배치

    Row(
// 수평 공간 분배: 토큰 더미 간의 간격 설정
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        // 수직 정렬: 토큰 더미들을 중앙 또는 상단에 맞춤
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 토큰 리스트를 순회하며 각 더미를 렌더링
        tokens.forEachIndexed { index, token ->
            TokenStackComponent(
                token = token,
                onClick = {
                    pickToken?.invoke(token.gemType)
                }
            )
        }
    }
}

// 💎 보석 아이콘 (원형)
@Composable
fun GemIcon(type: GemType, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(getGemColor(type), CircleShape)
            .border(1.dp, Color.Black.copy(alpha = 0.2f), CircleShape), // 테두리 추가
        contentAlignment = Alignment.Center
    ) {
        // 필요하다면 여기에 이미지나 텍스트 첫 글자 등을 넣을 수 있음
    }
}

// 💰 비용 표시 Row (하단 비용 목록)
@Composable
fun CostListDisplay(
    diamond: Int,
    sapphire: Int,
    emerald: Int,
    ruby: Int,
    onyx: Int,
    isVertical: Boolean = true // 세로/가로 배치 여부
) {
    // StaticCard의 개별 필드를 (GemType, Cost) 쌍의 리스트로 만듭니다.
    val costs = listOf(
        GemType.DIAMOND to diamond,
        GemType.SAPPHIRE to sapphire,
        GemType.EMERALD to emerald,
        GemType.RUBY to ruby,
        GemType.ONYX to onyx
    )

    val content = @Composable {
        costs.filter { it.second > 0 }.forEach { (type, cost) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!isVertical) { // 귀족 타일의 가로 정렬 시
                    Text(text = cost.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                GemIcon(type = type, size = 12.dp)
                if (isVertical) { // 카드 하단 비용의 세로 정렬 시
                    Text(text = cost.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (isVertical) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), horizontalAlignment = Alignment.End) {
            content()
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            content()
        }
    }
}

@Composable
fun SplendorCard(
    card: StaticCard,
    modifier: Modifier = Modifier,
    onClick: (Int) -> Unit
) {
    Box(
        modifier = modifier
            .size(width = 70.dp, height = 100.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(2.dp, getCardLevelColor(card.level), RoundedCornerShape(8.dp))
            .clickable(onClick = { onClick(card.id) })
            .padding(6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (card.points > 0) {
                    Text(
                        text = card.points.toString(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                } else {
                    Spacer(modifier = Modifier.size(10.dp))
                }
                GemIcon(type = card.bonusGem, size = 20.dp)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                CostListDisplay(
                    diamond = card.costDiamond,
                    sapphire = card.costSapphire,
                    emerald = card.costEmerald,
                    ruby = card.costRuby,
                    onyx = card.costOnyx,
                    isVertical = true,
                )
            }
        }
    }
}

@Composable
fun NobleTile(
    noble: StaticNoble,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(70.dp)
            .background(Color(0xFFFFF8E1), RoundedCornerShape(8.dp))
            .border(2.dp, Color(0xFFFFD700), RoundedCornerShape(8.dp))
            .padding(6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 점수
            Text(
                text = "${noble.points}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.wrapContentWidth()
            ) {

                val costs = listOf(
                    GemType.DIAMOND to noble.costDiamond,
                    GemType.SAPPHIRE to noble.costSapphire,
                    GemType.EMERALD to noble.costEmerald,
                    GemType.RUBY to noble.costRuby,
                    GemType.ONYX to noble.costOnyx
                )
                costs.filter { it.second > 0 }.forEach { (type, cost) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = cost.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        GemIcon(type = type, size = 12.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun CardRow(
    levelText: String,
    cards: List<StaticCard>,
    levelColor: Color,
    onClick: ((Int) -> Unit)?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 80.dp, height = 80.dp)
                .background(levelColor, RoundedCornerShape(8.dp))
                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = levelText, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        cards.forEach { card ->
            SplendorCard(card = card, onClick = { onClick?.invoke(card.id) })
        }
    }
}

@Composable
fun PlayerStatusPanel(
    playerState: PlayerState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(110.dp)
            .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = playerState.player.playerName,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            text = "점수: ${playerState.score}",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp
        )

        Spacer(Modifier.height(8.dp))

        Text("카드 보너스:", fontSize = 11.sp, color = Color.Gray)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            playerState.bonuses.filter { it.value > 0 }.forEach { (type, count) ->
                GemIcon(type = type, size = 10.dp)

            }
        }

        Spacer(Modifier.height(8.dp))
        Spacer(Modifier.height(8.dp))
        Text("보유 토큰:", fontSize = 12.sp, fontWeight = FontWeight.Bold)

        if (playerState.tokens.values.sum() == 0) {
            Text("- 없음 -", fontSize = 10.sp, color = Color.Gray)
        } else {
            playerState.tokens.filter { it.value > 0 }.forEach { (type, count) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 1.dp)
                ) {
                    GemIcon(type = type, size = 14.dp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("x $count", fontSize = 12.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        val totalTokens = playerState.tokens.values.sum()
        Text(
            text = "총: $totalTokens / 10",
            fontSize = 10.sp,
            color = if (totalTokens > 10) Color.Red else Color.Gray,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SafeGreetingWithBorders(
    nobleTiles: List<StaticNoble>,
    level3Cards: List<StaticCard>,
    level2Cards: List<StaticCard>,
    level1Cards: List<StaticCard>,
    tokens: List<Tokens>,
    pickToken: ((GemType) -> Unit)?,
    pickCard: ((Int) -> Unit)?,
    players: List<PlayerState>,
    endTurn: () -> Unit,
    currentSelectToken: (@Composable (
    ) -> Unit)? = null,
    currentSelectCard: (@Composable (
        cardToBuy: StaticCard,
        playerState: PlayerState,
        onCancel: () -> Unit
    ) -> Unit)? = null,


) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color(0xFFF5F5F5))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            players.getOrNull(0)?.let { pState ->
                PlayerStatusPanel(playerState = pState)
            } ?: Spacer(modifier = Modifier.width(100.dp))

            Column(
                Modifier.weight(1f), Arrangement.spacedBy(16.dp), Alignment.CenterHorizontally
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    nobleTiles.forEach { noble ->
                        NobleTile(noble = noble)
                    }
                }
                CardRow(
                    levelText = "L3",
                    cards = level3Cards,
                    levelColor = getCardLevelColor(3),
                    pickCard
                )
                CardRow(
                    levelText = "L2",
                    cards = level2Cards,
                    levelColor = getCardLevelColor(2),
                    pickCard
                )
                CardRow(
                    levelText = "L1",
                    cards = level1Cards,
                    levelColor = getCardLevelColor(1),
                    pickCard
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalStackCirclesPreview(tokens = tokens, pickToken = pickToken)
            }
            players.getOrNull(1)?.let { pState ->
                PlayerStatusPanel(playerState = pState)
            } ?: Spacer(modifier = Modifier.width(100.dp))
        }
        currentSelectToken?.invoke()
        currentSelectCard?.invoke(StaticCard(1, GemType.GOLD, 3, 1 ,
                3,5,4,6,7),
            PlayerState(GamePlayer("fr" ,"22"),5, mapOf(GemType.GOLD to 1) ,  mapOf(GemType.GOLD to 1) ),
            { print("Cc") }, )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = endTurn,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6A1B9A),
                disabledContainerColor = Color.Gray
            )
        ) {
            Text(
                text = "턴 넘기기",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

