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
import com.example.splendar.domain.game.GameState
import com.example.splendar.domain.game.PlayerState
import com.example.splendar.ui.GameWaitingRoomScreen // ⭐️ 분리된 UI import
import com.example.splendar.ui.RoomListScreen // ⭐️ 분리된 UI import
import com.example.splendar.ui.CreateRoom // ⭐️ 분리된 UI import
import com.example.splendar.ui.JoinRoom // ⭐️ 분리된 UI import
import com.example.splendar.ui.SafeGreetingWithBorders
import com.example.splendar.ui.theme.SplendarTheme
import kotlinx.coroutines.Job
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

class MainActivity : ComponentActivity() {
    private var stompSession: StompSession? = null
    private var collectorJob: Job? = null
    private val json = Json { ignoreUnknownKeys = true }

    private var roomSubscriptionJob: Job? = null

    private var isConnected by mutableStateOf(false)
    private var currentScreen by mutableStateOf<AppScreen>(ROOM_LIST)

    private val gameRoomState = SnapshotStateList<GameRooms>()

    private var currentRooms: GameRooms? by mutableStateOf(null)

    private var currentRoom: GameRoom? by mutableStateOf(null)

    private var currentRoomPlayerId: String? by mutableStateOf(null)

    private var GameState: GameState? by mutableStateOf(null)


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

                        CREATE_ROOM -> CreateRoom(onRoomCreated = { nickname, roomTitle -> // ⭐️ 분리된 UI 사용
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
                            val game = GameState
                            if (game != null) {
                                // 1. GameRoom에서 보드 데이터 추출
                                val boardData = game.extractBoardData()

                                // 2. 토큰 줍기 클릭 이벤트 핸들러 정의 (임시)
                                val handlePickToken: (Int) -> Unit = { tokenIndex ->
                                    // TODO: STOMP 메시지 전송 로직 구현 (토큰 줍기)
                                    // 예: stompSession?.sendText("/app/pick/token/${room.roomId}", tokenIndex.toString())
                                    Log.d("GameScreen", "Picked token at index: $tokenIndex")
                                }

                                // 3. SafeGreetingWithBorders 컴포넌트 호출
                                SafeGreetingWithBorders(
                                    nobleTiles = boardData.nobleTiles,
                                    level3Cards = boardData.level3Cards,
                                    level2Cards = boardData.level2Cards,
                                    level1Cards = boardData.level1Cards,
                                    tokens = boardData.tokens,
                                    pickToken = handlePickToken,
                                    players = boardData.playerState
                                )
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
                    val gamePlayerState = json.decodeFromString<GameState>(message)
                    GameState = gamePlayerState

                }
            } catch (e: Exception) {
                println("구독 중 예외 발생: ${e.message}")
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
            } catch (e: Exception) {
                println("구독 중 예외 발생: ${e.message}")
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