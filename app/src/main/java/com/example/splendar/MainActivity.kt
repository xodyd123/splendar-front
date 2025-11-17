package com.example.splendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.splendar.domain.AppScreen
import com.example.splendar.domain.AppScreen.CREATE_ROOM

import com.example.splendar.domain.AppScreen.JOIN_ROOM
import com.example.splendar.domain.AppScreen.ROOM_LIST
import com.example.splendar.domain.AppScreen.WAITING_ROOM
import com.example.splendar.domain.GameRoom
import com.example.splendar.domain.GameRooms
import com.example.splendar.domain.GameUser
import com.example.splendar.domain.PlayerDto
import com.example.splendar.domain.RoomStatus
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
    private var currentScreen by  mutableStateOf<AppScreen>(ROOM_LIST)

    private val gameRoomState = SnapshotStateList<GameRooms>()

    private var currentRoom: GameRooms? by mutableStateOf(null)

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
                        CircularProgressIndicator() // 로딩 인디케이터 추가
                    }
                } else {
                    val handleRoomClick : (GameRoom ) -> Unit = { createRoomObject ->
                        lifecycleScope.launch {
                            val jsonString = json.encodeToString(createRoomObject)
                            stompSession?.sendText("/app/add/room", jsonString)
                        }
                    }

                    val userRoomClick : (GameRoom ,Long) -> Unit = { createRoomObject , roomId ->
                        println("gameRoom , $createRoomObject")
                        lifecycleScope.launch {
                            val jsonString = json.encodeToString(createRoomObject)
                            val destination = "/app/join/room/${roomId}"
                            stompSession?.sendText(destination , jsonString)
                        }

                    }

                    when(currentScreen){
                        ROOM_LIST -> RoomListScreen(
                            gameRoomState,
                            onCreateRoomClick = { currentScreen = CREATE_ROOM },
                            onJoinRoomClick = { roomToJoin ->
                                currentRoom = roomToJoin
                                currentScreen = JOIN_ROOM
                            }
                        )

                        CREATE_ROOM -> CreateRoom(onRoomCreated = { nickname, roomTitle ->
                            println("새 방 생성 요청! 닉네임: $nickname, 방 제목: $roomTitle")
                            val gameRoom = GameRoom(roomTitle, nickname , true)
                            handleRoomClick(gameRoom) // 1. 서버에 생성 요청

                            currentRoom = GameRooms(
                                roomName = roomTitle,
                                roomId = 0L,
                                roomStatus = RoomStatus.WAITING,
                                hostName = nickname,
                                playerCount = 1,
                                players = listOf(PlayerDto(nickname = nickname, isReady = false))
                            )

                            currentScreen = WAITING_ROOM
                        })


                        WAITING_ROOM -> {
                            val room = currentRoom
                            if(room != null){
                                val users = room.players.map { player ->
                                    GameUser(
                                        id = 0L,
                                        username = player.nickname,
                                        isReady = player.isReady
                                    )
                                }

                                GameWaitingRoomScreen(
                                    roomName = room.roomName,
                                    users = users,
                                    onStartGameClick = { /* 게임 시작 로직 */ }
                                )
                            } else {
                                // currentRoom이 null이면 로딩 표시 (방 생성/참여 응답 대기 중)
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("방 정보를 서버에서 불러오는 중...")
                                    CircularProgressIndicator()
                                }
                            }
                        }

                        JOIN_ROOM -> {
                            val room = currentRoom
                            if (room != null){
                                JoinRoom(onRoomCreated = { nickname ->
                                    val gameRoom = GameRoom(room.roomName, nickname, false)

                                    userRoomClick(gameRoom, room.roomId)

                                    subscribeToRoom(room.roomId)

                                    currentScreen = WAITING_ROOM
                                })
                            }
                        }

                    }
                }
            }
        }
    }

    private fun subscribeToRoom(roomId: Long) {
        roomSubscriptionJob?.cancel()

        roomSubscriptionJob = lifecycleScope.launch {
            val specificRoomTopic = "/topic/rooms/$roomId"

            try {
                stompSession?.subscribeText(specificRoomTopic)?.collect { message: String ->
                    val updatedSpecificRoom = json.decodeFromString<GameRooms>(message)
                    currentRoom = updatedSpecificRoom
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
                val updatedRoom  = json.decodeFromString<MutableList<GameRooms>>(message)
                gameRoomState.clear()
                gameRoomState.addAll(updatedRoom)
            }
        }

        lifecycleScope.launch {
            session.subscribeText("/topic/update/rooms").collect { message: String ->
                val createdRoom = json.decodeFromString<GameRooms>(message)
                gameRoomState.add(createdRoom)

                if (currentScreen == WAITING_ROOM && currentRoom?.roomId == 0L) {
                    subscribeToRoom(createdRoom.roomId)

                    currentRoom = createdRoom
                }
            }
        }

        session.sendText("/app/rooms" , "")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameWaitingRoomScreen(
    roomName: String,
    users: List<GameUser>,
    onStartGameClick: () -> Unit,
) {
    val isGameStartEnabled = users.size == 2 && users.all { it.isReady }
    println("roomName , $roomName" )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(roomName, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                PlayerSlots(users = users)
            }


            Text(
                text = if (isGameStartEnabled) "모두 준비 완료!" else "플레이어를 기다리는 중...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(vertical = 8.dp)
            )


            Button(
                onClick = onStartGameClick,
                enabled = isGameStartEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(text = "게임 시작", fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun PlayerCard(
    user: GameUser?,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        user == null -> MaterialTheme.colorScheme.outline
        user.isReady -> Color(0xFF4CAF50)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = modifier
            .padding(8.dp)
            .wrapContentHeight()
            .widthIn(min = 120.dp, max = 220.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(3.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 아바타 크기 축소 (원래 80.dp -> 64.dp)
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Player Avatar",
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = user?.username ?: "대기 중",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (user?.isReady == true) "준비 완료" else if (user != null) "준비 중..." else "자리 비어있음",
                color = borderColor,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PlayerSlots(users: List<GameUser>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        users.map { user -> PlayerCard(user , modifier = Modifier.weight(1f))}

    }
}


@Composable
fun  RoomListScreen(
    rooms: MutableList<GameRooms>,
    onCreateRoomClick: (Enum<AppScreen>) -> Unit,
    onJoinRoomClick: (GameRooms) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Row(){
                Text(
                    text = "게임 방 목록",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Button(
                    onClick = {
                        //println("방만들기 버튼 클릭")
                        onCreateRoomClick(CREATE_ROOM)
                    },
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    Text("방 만들기")
                }

            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (rooms.isEmpty()) {

                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "현재 개설된 방이 없습니다.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    items(
                        items = rooms,
                        key = { room -> room.roomId }
                    ) { room ->
                        RoomCard(room = room, onClick = {onJoinRoomClick(room)})
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoom(onRoomCreated:  (String, String) -> Unit) {

    var nickname by remember { mutableStateOf("") }

    var roomTitle by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("방 만들기", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = nickname,
            onValueChange = { nickname = it },
            label = { Text("닉네임을 입력해주세요") },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))


        TextField(
            value = roomTitle,
            onValueChange = { roomTitle = it },
            label = { Text("방 제목을 입력해주세요") },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        val isButtonEnabled = nickname.isNotBlank() && roomTitle.isNotBlank()

        Button(
            onClick = {

                onRoomCreated(nickname, roomTitle)
            },
            enabled = isButtonEnabled
        ) {
            Text("방 만들기")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinRoom(onRoomCreated: (String) -> Unit) {

    var nickname by remember { mutableStateOf("") }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            value = nickname,
            onValueChange = { nickname = it },
            label = { Text("닉네임을 입력해주세요") },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        val isButtonEnabled = nickname.isNotBlank()

        Button(
            onClick = {
                onRoomCreated(nickname)
            },
            enabled = isButtonEnabled
        ) {
            Text("방 접속")
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomCard(room: GameRooms , onClick: () -> Unit) {
    Card(
        onClick = onClick, // Material3 Card의 onClick 사용
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "방 제목 : ${room.roomName}",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "방장 : ${room.hostName}" ,
                style = MaterialTheme.typography.bodyMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text =  "참여 인원 : ${room.playerCount}/2", // 나중에 수정
                    style = MaterialTheme.typography.bodySmall,
                )

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "입장"
                )
            }
        }
    }

    @Composable
    fun PlayerCard(user: GameUser?) {

        val borderColor = when {
            user == null -> MaterialTheme.colorScheme.outline
            user.isReady -> Color(0xFF4CAF50)
            else -> MaterialTheme.colorScheme.primary
        }

        Card(
            modifier = Modifier.size(width = 150.dp, height = 200.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(3.dp, borderColor),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (user != null) {
                    Image(
                        painter = rememberVectorPainter(Icons.Default.Person),
                        contentDescription = "Player Avatar",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = user.username,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (user.isReady) "준비 완료" else "준비 중...",
                        color = borderColor,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "플레이어\n대기 중...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

}
