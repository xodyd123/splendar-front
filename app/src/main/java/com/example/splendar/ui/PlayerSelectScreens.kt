package com.example.splendar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.splendar.domain.AppScreen
import com.example.splendar.domain.game.ChoicePlayer
import com.example.splendar.domain.game.GameState
import com.example.splendar.domain.game.SplendorAction
import kotlinx.coroutines.launch

@Composable
fun GameChoiceScreen(
    gameState: GameState, // 게임 상태 전체를 받음
    currentRoomPlayerId: String, // 현재 로컬 플레이어 ID
    handleScreenChange: (AppScreen) -> Unit, // 화면 전환 콜백
    sendActionMessage: (ChoicePlayer) -> Unit, // STOMP 전송 함수
    latestReceivedMessage: String?,
    onMessageConsumed: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 현재 턴 플레이어 정보 추출
    val currentPlayer = gameState.currentPlayer
    val isLocalPlayerTurn = currentRoomPlayerId == currentPlayer.playerId


    val onActionSelected: (message: String) -> Unit = { message ->
        if (isLocalPlayerTurn) {
            val choicePlayer = ChoicePlayer(
                currentPlayer.playerId,
                gameState.gameId,
                splendorAction = message
            )
            // 1. STOMP 메시지 전송 (다른 유저에게 행동 알림)
            sendActionMessage(choicePlayer)

        }

    }

    LaunchedEffect(latestReceivedMessage) {
        if (latestReceivedMessage != null) {
            // 1. 모든 유저의 화면에 Snackbar 표시
            snackbarHostState.showSnackbar(
                message = latestReceivedMessage,
                duration = SnackbarDuration.Short
            )

            // 2. Snackbar가 사라진 후 (코루틴 재개 후) 화면 전환
            handleScreenChange(AppScreen.GAME_SCREEN)

            // 3. 상위 상태 초기화 (메시지 상태를 null로 만들어 다음 메시지를 받을 준비)
            onMessageConsumed()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {

            // ⭐️ 기존의 ChoicePlayer 로직을 여기에 배치
            ChoicePlayer(
                playerId = currentRoomPlayerId,
                currentTurnID = currentPlayer.playerId,
                playerName = currentPlayer.playerName,
                // ⭐️ 수정된 콜백 전달
                onActionSelected = onActionSelected
            )

            // ⚠️ TODO: 여기에 필요하다면 SafeGreetingWithBorders 등 다른 UI 요소도 배치 가능
        }
    }
}

@Composable
fun ChoicePlayer(
    playerId: String,
    currentTurnID: String,
    playerName: String,
    onActionSelected: (message: String) -> Unit
) {
    val isOnClick = playerId == currentTurnID
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. 안내 메시지 Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center, // 텍스트를 양 끝으로 분산
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("3가지 선택지중 한가지를 선택하세요")
                // offset 대신 padding이나 Arrangement.SpaceBetween을 활용하여 정렬하는 것이 좋습니다.
                Text("이번 차례 : ${playerName}님", Modifier.padding(start = 16.dp))
            }

            // 2. 버튼 Row (가장 핵심적인 수정 부분)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp), // 좌우 패딩을 주어 화면 끝에 붙지 않도록 함
                verticalAlignment = Alignment.CenterVertically,
                // ⭐️ 버튼들 사이에 균등한 공간을 배분
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 각 버튼에 weight(1f)를 적용하여 가로 공간을 1:1:1로 나눕니다.
                ChoiceButton(
                    text = "서로 다른 3가지 보석 토큰을 1개씩 가져오거나, 한 종류 보석 토큰 2개를 가져온다",
                    onEndTurn = { onActionSelected("보석 토큰을 가져오는 액션을 선택했습니다.") },
                    modifier = Modifier.weight(1f),
                    enabled = isOnClick
                )
                ChoiceButton(
                    text = "가지고 있는 보석 토큰을 내고 바닥에 놓인(혹은 자신이 보관하고 있는) 개발 카드 1장을 구입한다",
                    onEndTurn = { onActionSelected("개발 카드 1장을 구입하는 액션을 선택했습니다.") },
                    modifier = Modifier.weight(1f),
                    enabled = isOnClick
                )
                ChoiceButton(
                    text = "개발 카드 1장을 수중에 보관하고, 황금 토큰 1개를 받는다",
                    onEndTurn = { onActionSelected("개발 카드 1장을 보관하고 황금 토큰 1개를 받는 액션을 선택했습니다.") },
                    modifier = Modifier.weight(1f),
                    enabled = isOnClick
                )
            }
        }
    }
}

// 텍스트 정렬을 위해 버튼을 별도의 Composable로 분리
@Composable
fun ChoiceButton(
    text: String,
    onEndTurn: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean
) {
    Button(
        onClick = onEndTurn,
        modifier = modifier.heightIn(min = 60.dp),
        enabled = enabled
        // Button 내부에서는 'enabled: Boolean = true'와 같은 재선언을 하지 않음
    ) {
        // ⭐️ Text는 Button의 Content 람다 안에 위치
        Text(
            text = text,
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}
