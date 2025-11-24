// MainActivity.kt (수정된 버전)
package com.example.splendar

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.splendar.domain.AppScreen
import com.example.splendar.domain.AppScreen.*
import com.example.splendar.domain.CreateGameRoom
import com.example.splendar.domain.GameRoom
import com.example.splendar.domain.GameRooms
import com.example.splendar.domain.GameUser
import com.example.splendar.domain.PlayerDto
import com.example.splendar.domain.RoomStatus
import com.example.splendar.domain.game.request.ChoicePlayer
import com.example.splendar.domain.game.GameScreen
import com.example.splendar.domain.game.GameState
import com.example.splendar.domain.game.GemType
import com.example.splendar.domain.game.request.SelectStatus
import com.example.splendar.domain.game.request.SelectToken
import com.example.splendar.domain.game.response.GameResponse
import com.example.splendar.domain.game.response.SelectedPlayer
import com.example.splendar.domain.token.SelectedToken
import com.example.splendar.domain.token.SelectedTokenResponse
import com.example.splendar.ui.CardPurchaseConfirmationScreen
import com.example.splendar.ui.GameWaitingRoomScreen // ⭐️ 분리된 UI import
import com.example.splendar.ui.RoomListScreen // ⭐️ 분리된 UI import
import com.example.splendar.ui.CreateRoom // ⭐️ 분리된 UI import
import com.example.splendar.ui.CurrentTokenSelectScreen
import com.example.splendar.ui.JoinRoom // ⭐️ 분리된 UI import
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

    private var errorMessage : String? by mutableStateOf(value = null)

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

                    val userRoomClick: (CreateGameRoom, Long) -> Unit =
                        { createRoomObject, roomId ->
                            lifecycleScope.launch {
                                val jsonString = json.encodeToString(createRoomObject)
                                val destination = "/app/join/room/${roomId}"
                                stompSession?.sendText(destination, jsonString)
                            }

                        }

                    val userReadyClick: (String, Long) -> Unit = { currentRoomPlayerId, roomId ->
                        lifecycleScope.launch {
                            val jsonString = json.encodeToString(currentRoomPlayerId)
                            val destination = "/app/ready/room/${roomId}/${currentRoomPlayerId}"
                            stompSession?.sendText(destination, jsonString)
                        }
                    }

                    val userGameClick: (Long) -> Unit = { roomId ->
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
                                roomId = 0L,
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
                                        currentScreen = GAME_SCREEN
                                        userGameClick(room.roomId)
                                        initialGame(room.roomId)
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
                            if (game != null && playerId != null) {
                                // 1. GameRoom에서 보드 데이터 추출
                                val boardData = game.data.extractBoardData()

                                val handlePickToken: (GemType) -> Unit = { tokenType ->
                                    lifecycleScope.launch {
                                        val selectToken = SelectToken(
                                            roomId = game.data.gameId,
                                            playerId = playerId,
//                                            game.data.currentPlayer.playerId,
                                            token = tokenType,
                                            selectStatus = SelectStatus.IS_SELECT
                                        )
                                        val jsonString = json.encodeToString(selectToken)

                                        val destination =
                                            "/app/game-select-token/${game.data.gameId}"
                                        stompSession?.sendText(destination, jsonString)
                                    }

                                }

                                val removeToken: (SelectToken) -> Unit = { token ->
                                    lifecycleScope.launch {

                                        val jsonString = json.encodeToString(token)

                                        val destination =
                                            "/app/game-select-token/${game.data.gameId}"
                                        stompSession?.sendText(destination, jsonString)
                                    }

                                }



                                if (currentGameView == GameScreen.BOARD_VIEW) {
                                    // 3. SafeGreetingWithBorders 컴포넌트 호출

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
                                        players = boardData.playerState,
                                        endTurn = {},

                                        )

                                }

                                if (currentGameView == GameScreen.TAKE_TOKENS) {
                                    selectedToken(game.data.gameId)
                                    SafeGreetingWithBorders(
                                        nobleTiles = boardData.nobleTiles,
                                        level3Cards = boardData.level3Cards,
                                        level2Cards = boardData.level2Cards,
                                        level1Cards = boardData.level1Cards,
                                        tokens = boardData.tokens,
                                        pickToken = handlePickToken,
                                        players = boardData.playerState,
                                        endTurn = {},
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
                                        pickToken = handlePickToken,
                                        players = boardData.playerState,
                                        endTurn = {},
                                        // 상태랑
                                        // 메제시로 받은 값
                                        null,
                                        currentSelectCard = { a, b, c, d ->
                                            CardPurchaseConfirmationScreen(
                                                cardToBuy = a,
                                                playerState = b,
                                                onDismiss = c
                                            )
                                        }
                                    )

                                }


                            } else {
                                // GameRoom 데이터가 없을 경우
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("게임 데이터 준비 중...")
                                    CircularProgressIndicator()
                                }
                            }

                        }

                        USER_GAME_CHOICE_SCREEN -> {
                            val game = gameState
                            val playerId = currentRoomPlayerId

                            LaunchedEffect(game?.data?.gameId) { // gameId가 바뀌면 재구독
                                if (game != null) {
                                    subscribeToChoiceGame(game.data.gameId)
                                }
                            }

                            if (game != null && playerId != null) {
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

    private fun initialGame(roomId: Long) {
        roomSubscriptionJob?.cancel()


        roomSubscriptionJob = lifecycleScope.launch {
            val specificRoomTopic = "/topic/game-screen/$roomId"

            try {
                stompSession?.subscribeText(specificRoomTopic)?.collect { message: String ->
                    val gamePlayerState = json.decodeFromString<GameResponse>(message)
                    gameState = gamePlayerState

                }
            } catch (e: CancellationException) {
                // 코루틴 취소는 정상적인 종료이므로 무시하거나 디버그 로그만 남김
                Log.d("Info", "구독이 정상적으로 취소되었습니다.")
                throw e // ⭐️ 중요: 코루틴 시스템이 취소를 인지하도록 다시 던져줘야 함
            } catch (e: Exception) {
                // 진짜 에러만 여기서 처리
                Log.e("Error", "진짜 에러 발생 initialGame: ${e.message}")
            }
        }
    }

    private fun subscribeToRoom(roomId: Long) {
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
                // 코루틴 취소는 정상적인 종료이므로 무시하거나 디버그 로그만 남김
                Log.d("Info", "구독이 정상적으로 취소되었습니다.")
                throw e // ⭐️ 중요: 코루틴 시스템이 취소를 인지하도록 다시 던져줘야 함
            } catch (e: Exception) {
                // 진짜 에러만 여기서 처리
                Log.e("Error", "진짜 에러 발생: ${e.message}")
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
                // 코루틴 취소는 정상적인 종료이므로 무시하거나 디버그 로그만 남김
                Log.d("Info", "구독이 정상적으로 취소되었습니다.")
                throw e // ⭐️ 중요: 코루틴 시스템이 취소를 인지하도록 다시 던져줘야 함
            } catch (e: Exception) {
                // 진짜 에러만 여기서 처리
                Log.e("Error", "진짜 에러 발생: ${e.message}")
            }
        }
    }

    private fun selectedToken(roomId: Int) {
        roomSubscriptionJob?.cancel()


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
                // 코루틴 취소는 정상적인 종료이므로 무시하거나 디버그 로그만 남김
                Log.d("Info", "구독이 정상적으로 취소되었습니다.")
                throw e // ⭐️ 중요: 코루틴 시스템이 취소를 인지하도록 다시 던져줘야 함
            } catch (e: Exception) {
                // 진짜 에러만 여기서 처리
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

                if (currentScreen == WAITING_ROOM && currentRooms?.roomId == 0L) {
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