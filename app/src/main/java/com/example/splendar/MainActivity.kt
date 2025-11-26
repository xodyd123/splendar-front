// MainActivity.kt (수정된 버전)
package com.example.splendar

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.splendar.domain.game.AppScreen
import com.example.splendar.domain.game.AppScreen.*
import com.example.splendar.domain.game.CreateGameRoom
import com.example.splendar.domain.game.GameRoom
import com.example.splendar.domain.game.GameRooms
import com.example.splendar.domain.game.GameUser
import com.example.splendar.domain.game.PlayerDto
import com.example.splendar.domain.game.RoomStatus
import com.example.splendar.domain.card.SelectCard
import com.example.splendar.domain.game.request.ChoicePlayer
import com.example.splendar.domain.game.GameScreen
import com.example.splendar.domain.token.GemType
import com.example.splendar.domain.game.request.SelectStatus
import com.example.splendar.domain.game.request.SelectToken
import com.example.splendar.domain.game.response.GameResponse
import com.example.splendar.domain.game.response.SelectedPlayer
import com.example.splendar.domain.token.SelectedToken
import com.example.splendar.domain.token.SelectedTokenResponse
import com.example.splendar.ui.CardPurchaseConfirmationScreen
import com.example.splendar.ui.GameWaitingRoomScreen
import com.example.splendar.ui.RoomListScreen
import com.example.splendar.ui.CreateRoom
import com.example.splendar.ui.CurrentTokenSelectScreen
import com.example.splendar.ui.JoinRoom
import com.example.splendar.ui.SafeGreetingWithBorders
import com.example.splendar.ui.GameChoiceScreen
import com.example.splendar.ui.theme.SplendarTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.sendText
import org.hildan.krossbow.stomp.subscribeText
import org.hildan.krossbow.websocket.okhttp.OkHttpWebSocketClient
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

class MainActivity : ComponentActivity() {
    private var stompSession: StompSession? = null
    private var collectorJob: Job? = null
    private val json = Json { ignoreUnknownKeys = true }

    private var roomSubscriptionJob: Job? = null

    private var isConnected by mutableStateOf(false)
    private var currentScreen by mutableStateOf<AppScreen>(ROOM_LIST)

    private val gameRoomState = SnapshotStateList<GameRooms>()

    private var currentRooms: GameRooms? by mutableStateOf(null)

    private var currentRoomPlayerId: String? by mutableStateOf(null)

    private var gameState: GameResponse? by mutableStateOf(null)

    private var currentGameView: GameScreen by mutableStateOf(GameScreen.BOARD_VIEW)

    private var latestActionMessage: String? by mutableStateOf(null)

    private var playerSelectedToken: SelectedToken? by mutableStateOf(null)

    private var errorMessage: String? by mutableStateOf(value = null)

    val onMessageConsumed: () -> Unit = {
        latestActionMessage = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            connectToStomp()
        }
        setContent {
            SplendarTheme {
                if (!isConnected) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("서버에 연결하고 방 목록을 불러오는 중...")
                        CircularProgressIndicator()
                    }
                } else {
                    val handleRoomClick: (CreateGameRoom) -> Unit = { createRoomObject ->
                        lifecycleScope.launch {
                            val jsonString = json.encodeToString(createRoomObject)
                            stompSession?.sendText("/app/add/room", jsonString)
                        }
                    }

                    val userRoomClick: (CreateGameRoom, Int) -> Unit =
                        { createRoomObject, roomId ->
                            lifecycleScope.launch {
                                val jsonString = json.encodeToString(createRoomObject)
                                val destination = "/app/join/room/${roomId}"
                                stompSession?.sendText(destination, jsonString)
                            }

                        }

                    val userReadyClick: (String, Int) -> Unit = { currentRoomPlayerId, roomId ->
                        lifecycleScope.launch {
                            val jsonString = json.encodeToString(currentRoomPlayerId)
                            val destination = "/app/ready/room/${roomId}/${currentRoomPlayerId}"
                            stompSession?.sendText(destination, jsonString)
                        }
                    }

                    val userGameClick: (Int) -> Unit = { roomId ->
                        lifecycleScope.launch {
                            val jsonString = json.encodeToString(roomId)
                            val destination = "/app/game-screen/${roomId}"
                            stompSession?.sendText(destination, jsonString)
                        }
                    }

                    val sendChoiceAction: (ChoicePlayer) -> Unit = { choicePlayer ->
                        lifecycleScope.launch {
                            val jsonString = json.encodeToString(choicePlayer)
                            val destination = "/app/game-choice-screen/${choicePlayer.roomId}"
                            stompSession?.sendText(destination, jsonString)
                        }
                    }


                    when (currentScreen) {
                        ROOM_LIST -> RoomListScreen(
                            rooms = gameRoomState,
                            onCreateRoomClick = {
                                currentScreen = CREATE_ROOM
                            },
                            onJoinRoomClick = { roomToJoin ->
                                currentRooms = roomToJoin
                                currentScreen = JOIN_ROOM
                            }
                        )

                        CREATE_ROOM -> CreateRoom(onRoomCreated = { nickname, roomTitle ->
                            val createGameRoom = CreateGameRoom(roomTitle, nickname, true)
                            handleRoomClick(createGameRoom)

                            currentRooms = GameRooms(
                                roomName = roomTitle,
                                roomId = 0,
                                roomStatus = RoomStatus.WAITING,
                                hostName = nickname,
                                playerCount = 1,
                                players = listOf(
                                    PlayerDto(
                                        nickname = nickname,
                                        isReady = false,
                                    )
                                ),
                            )

                            currentScreen = WAITING_ROOM
                        })


                        WAITING_ROOM -> {
                            val room = currentRooms
                            if (room != null) {
                                val users = room.players.map { player ->
                                    GameUser(
                                        id = "playerId",
                                        username = player.nickname,
                                        isReady = player.isReady
                                    )
                                }

                                GameWaitingRoomScreen(
                                    roomName = room.roomName,
                                    users = users,
                                    onStartGameClick = {
                                        userReadyClick(
                                            currentRoomPlayerId as String,
                                            room.roomId
                                        )
                                    },
                                    onChangeGameScreen = {
                                        userGameClick(room.roomId)
                                        currentScreen = GAME_SCREEN
                                    }
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("방 정보를 서버에서 불러오는 중...")
                                    CircularProgressIndicator()
                                }
                            }
                        }

                        JOIN_ROOM -> {
                            val room = currentRooms
                            if (room != null) {
                                JoinRoom(onRoomJoined = { nickname ->
                                    val createGameRoom =
                                        CreateGameRoom(room.roomName, nickname, false)

                                    userRoomClick(createGameRoom, room.roomId)

                                    subscribeToRoom(room.roomId)

                                    currentScreen = WAITING_ROOM
                                })
                            }
                        }

                        GAME_SCREEN -> {
                            val game = gameState
                            val playerId = currentRoomPlayerId


                            val targetRoomId = game?.data?.gameId ?: currentRooms?.roomId

                            LaunchedEffect(Unit) {
                                if (targetRoomId != null) {
                                    Log.d("GameScreen", "구독 시작: $targetRoomId")
                                    subscribeGameRoom(targetRoomId)
                                }
                            }

                            LaunchedEffect(key1 = currentGameView, key2 = targetRoomId) {
                                if (currentGameView == GameScreen.TAKE_TOKENS && targetRoomId != null) {
                                    selectedToken(targetRoomId)
                                }
                            }


                            if (game?.data != null && playerId != null) {

                                val boardData = game.data.extractBoardData()

                                val handlePickToken: (GemType) -> Unit = { tokenType ->
                                    lifecycleScope.launch {
                                        val selectToken = SelectToken(
                                            roomId = game.data.gameId,
                                            playerId = playerId,
                                            token = tokenType,
                                            selectStatus = SelectStatus.IS_SELECT
                                        )
                                        val jsonString = json.encodeToString(selectToken)
                                        stompSession?.sendText(
                                            "/app/game-select-token/${game.data.gameId}",
                                            jsonString
                                        )
                                    }
                                }

                                val removeToken: (SelectToken) -> Unit = { token ->
                                    lifecycleScope.launch {
                                        val jsonString = json.encodeToString(token)
                                        stompSession?.sendText(
                                            "/app/game-select-token/${game.data.gameId}",
                                            jsonString
                                        )
                                    }
                                }

                                val handlePickCard: (Int) -> Unit = { id ->
                                    lifecycleScope.launch {
                                        val selectCard = SelectCard(
                                            game.data.gameId,
                                            playerId = playerId,
                                            cardId = id,
                                            selectStatus = SelectStatus.IS_SELECT
                                        )
                                        val jsonString = json.encodeToString(selectCard)
                                        stompSession?.sendText(
                                            "/app/game-select-card/${game.data.gameId}",
                                            jsonString
                                        )
                                    }
                                }

                                val endTurn: () -> Unit = {
                                    lifecycleScope.launch {
                                        val jsonString = json.encodeToString(game.data.gameId)
                                        stompSession?.sendText(
                                            "/app/game-end-turn/${game.data.gameId}",
                                            jsonString
                                        )
                                    }
                                    Log.d("endTurn", "endTurn 메서드 실행")
                                }



                                if (currentGameView == GameScreen.BOARD_VIEW) {

                                    LaunchedEffect(Unit) {
                                        delay(3000L)
                                        currentGameView = GameScreen.PLAYER_CHOICE
                                        currentScreen = AppScreen.USER_GAME_CHOICE_SCREEN
                                    }

                                    SafeGreetingWithBorders(
                                        nobleTiles = boardData.nobleTiles,
                                        level3Cards = boardData.level3Cards,
                                        level2Cards = boardData.level2Cards,
                                        level1Cards = boardData.level1Cards,
                                        tokens = boardData.tokens,
                                        pickToken = handlePickToken,
                                        pickCard = null,
                                        players = boardData.playerState,
                                        endTurn = {},
                                    )
                                }

                                if (currentGameView == GameScreen.TAKE_TOKENS) {
                                    SafeGreetingWithBorders(
                                        nobleTiles = boardData.nobleTiles,
                                        level3Cards = boardData.level3Cards,
                                        level2Cards = boardData.level2Cards,
                                        level1Cards = boardData.level1Cards,
                                        tokens = boardData.tokens,
                                        pickToken = handlePickToken,
                                        pickCard = null,
                                        players = boardData.playerState,
                                        endTurn = endTurn,
                                        currentSelectToken = {
                                            CurrentTokenSelectScreen(
                                                playerSelectedToken = playerSelectedToken,
                                                onRemoveToken = removeToken,
                                                errorMessage = errorMessage,
                                                playerId = game.data.currentPlayer.playerId,
                                                roomId = game.data.gameId,
                                            )
                                        }
                                    )
                                }

                                if (currentGameView == GameScreen.BUY_CARD) {
                                    SafeGreetingWithBorders(
                                        nobleTiles = boardData.nobleTiles,
                                        level3Cards = boardData.level3Cards,
                                        level2Cards = boardData.level2Cards,
                                        level1Cards = boardData.level1Cards,
                                        tokens = boardData.tokens,
                                        players = boardData.playerState,
                                        endTurn = endTurn,
                                        pickToken = null,
                                        pickCard = handlePickCard,
                                        currentSelectCard = { card, state, onDismiss ->
                                            CardPurchaseConfirmationScreen(
                                                cardToBuy = card,
                                                playerState = state,
                                                onDismiss = onDismiss
                                            )
                                        }
                                    )
                                }

                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("게임 데이터 준비 중...")
                                        Spacer(modifier = Modifier.run { height(16.dp) })
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }

                        USER_GAME_CHOICE_SCREEN -> {
                            val game = gameState
                            val playerId = currentRoomPlayerId
                            if (game?.data != null) {
                                LaunchedEffect(game.data.gameId) {
                                    subscribeToChoiceGame(game.data.gameId)
                                }

                            }


                            if (game?.data != null && playerId != null) {
                                GameChoiceScreen(
                                    gameState = game.data,
                                    currentRoomPlayerId = playerId,
                                    handleScreenChange = { screen -> currentScreen = screen },
                                    sendActionMessage = sendChoiceAction,
                                    latestReceivedMessage = latestActionMessage,
                                    onMessageConsumed = onMessageConsumed,
                                    screen = AppScreen.GAME_SCREEN
                                )
                            }
                        }

                    }
                }
            }
        }
    }

    // 통합된 구독 함수
    private fun subscribeGameRoom(roomId: Int) {
        if (roomSubscriptionJob != null) {
            Log.w("WS_CANCEL", "기존 Job 취소됨: Job 상태=${roomSubscriptionJob?.isActive}")
        }
        roomSubscriptionJob?.cancel()

        roomSubscriptionJob = lifecycleScope.launch {
            val specificRoomTopic = "/topic/game-screen/$roomId"

            try {
                stompSession?.subscribeText(specificRoomTopic)?.collect { message: String ->
                    Log.d("WebSocket", "메시지 수신: $message")

                    val response = json.decodeFromString<GameResponse>(message)

                    if (response.status == "SUCCESS") {
                        gameState = response

                        errorMessage = null
                    }

                    if (response.message != null) {
                        errorMessage = response.message
                    }
                }
            } catch (e: CancellationException) {
                Log.e("WS_CANCEL", "LaunchedEffect 외부 요인으로 Job 취소됨!")
                throw e
            } catch (e: Exception) {
                Log.e("Error", "구독 중 에러 발생: ${e.message}")
            }
        }
    }

    private fun subscribeToRoom(roomId: Int) {
        roomSubscriptionJob?.cancel()


        roomSubscriptionJob = lifecycleScope.launch {
            val specificRoomTopic = "/topic/rooms/$roomId"

            try {
                stompSession?.subscribeText(specificRoomTopic)?.collect { message: String ->
                    val updatedSpecificRoom = json.decodeFromString<GameRoom>(message)
                    val gameRooms = GameRooms(
                        updatedSpecificRoom.roomName,
                        updatedSpecificRoom.roomId,
                        updatedSpecificRoom.roomStatus,
                        updatedSpecificRoom.hostName,
                        updatedSpecificRoom.playerCount,
                        updatedSpecificRoom.players
                    )

                    if (currentRoomPlayerId == null) {
                        currentRoomPlayerId = updatedSpecificRoom.playerId
                    }

                    currentRooms = gameRooms
                }
            } catch (e: CancellationException) {
                Log.d("Info", "구독이 정상적으로 취소되었습니다.")
                throw e
            } catch (e: Exception) {
                Log.e("Error", "에러 발생: ${e.message}")
            }
        }
    }

    private fun subscribeToChoiceGame(roomId: Int) {
        roomSubscriptionJob?.cancel()


        roomSubscriptionJob = lifecycleScope.launch {
            val specificRoomTopic = "/topic/game-choice-screen/$roomId"

            try {
                stompSession?.subscribeText(specificRoomTopic)?.collect { message: String ->
                    val selectedPlayer = json.decodeFromString<SelectedPlayer>(message)
                    latestActionMessage = selectedPlayer.splendorAction
                    currentGameView = selectedPlayer.playerAction

                }
            } catch (e: CancellationException) {
                Log.d("Info", "구독이 정상적으로 취소되었습니다.")
                throw e
            } catch (e: Exception) {
                Log.e("Error", "진짜 에러 발생: ${e.message}")
            }
        }
    }

    private fun selectedToken(roomId: Int) {
        roomSubscriptionJob = lifecycleScope.launch {
            val specificRoomTopic = "/topic/game-select-token/$roomId"

            try {
                stompSession?.subscribeText(specificRoomTopic)?.collect { message: String ->

                    val selectedToken = json.decodeFromString<SelectedTokenResponse>(message)
                    val message = selectedToken.message
                    if (selectedToken.status == "SUCCESS") {
                        playerSelectedToken = selectedToken.data
                        errorMessage = null
                    }
                    if (message != null) {
                        errorMessage = message
                    }

                }
            } catch (e: CancellationException) {
                Log.d("Info", "구독이 정상적으로 취소되었습니다.")
                throw e
            } catch (e: Exception) {
                Log.e("Error", "진짜 에러 발생: ${e.message}")
            }
        }
    }


    private suspend fun connectToStomp() {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()

        val stompClient = StompClient(OkHttpWebSocketClient(okHttpClient))

        val url = "ws://10.0.2.2:8080/ws-connect"
        val session: StompSession = stompClient.connect(url)

        stompSession = session

        collectorJob = lifecycleScope.launch {
            session.subscribeText("/topic/rooms").collect { message: String ->
                val updatedRoom = json.decodeFromString<MutableList<GameRooms>>(message)
                gameRoomState.clear()
                gameRoomState.addAll(updatedRoom)
            }
        }

        lifecycleScope.launch {
            session.subscribeText("/topic/update/rooms").collect { message: String ->
                val createdRooms = json.decodeFromString<GameRoom>(message)
                val gameRooms = GameRooms(
                    createdRooms.roomName,
                    createdRooms.roomId,
                    createdRooms.roomStatus,
                    createdRooms.hostName,
                    createdRooms.playerCount,
                    createdRooms.players
                )

                gameRoomState.add(gameRooms)

                if (currentScreen == WAITING_ROOM && currentRooms?.roomId == 0) {
                    subscribeToRoom(createdRooms.roomId)

                    currentRooms = gameRooms
                    if (currentRoomPlayerId == null) {
                        currentRoomPlayerId = createdRooms.playerId
                    }


                }
            }
        }
        session.sendText("/app/rooms", "")
        isConnected = true

    }

    override fun onDestroy() {
        super.onDestroy()
        collectorJob?.cancel()
        lifecycleScope.launch {
            try {
                stompSession?.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}