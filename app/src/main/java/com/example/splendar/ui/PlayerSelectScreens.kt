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
import com.example.splendar.domain.game.AppScreen
import com.example.splendar.domain.game.request.ChoicePlayer
import com.example.splendar.domain.game.GameState

@Composable
fun GameChoiceScreen(
    gameState: GameState,
    currentRoomPlayerId: String,
    handleScreenChange: (AppScreen) -> Unit,
    sendActionMessage: (ChoicePlayer) -> Unit,
    latestReceivedMessage: String?,
    onMessageConsumed: () -> Unit,
    screen: AppScreen
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val currentPlayer = gameState.currentPlayer
    val isLocalPlayerTurn = currentRoomPlayerId == currentPlayer.playerId


    val onActionSelected: (message: String) -> Unit = { message ->
        if (isLocalPlayerTurn) {
            val choicePlayer = ChoicePlayer(
                currentPlayer.playerId,
                gameState.gameId,
                splendorAction = message
            )
            sendActionMessage(choicePlayer)

        }

    }

    LaunchedEffect(latestReceivedMessage) {
        if (latestReceivedMessage != null) {
            snackbarHostState.showSnackbar(
                message = latestReceivedMessage,
                duration = SnackbarDuration.Short
            )

            handleScreenChange(screen)

            onMessageConsumed()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            ChoicePlayer(
                playerId = currentRoomPlayerId,
                currentTurnID = currentPlayer.playerId,
                playerName = currentPlayer.playerName,
                onActionSelected = onActionSelected
            )
        }
    }
}

@Composable
fun ChoicePlayer(
    playerId: String,
    currentTurnID: String,
    playerName: String,
    onActionSelected: (message: String) -> Unit,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("3가지 선택지중 한가지를 선택하세요")
                Text("이번 차례 : ${playerName}님", Modifier.padding(start = 16.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}
