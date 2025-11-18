
package com.example.splendar.ui
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.splendar.domain.GameRooms // GameRooms data class 임포트 필요
import com.example.splendar.domain.GameUser // GameUser data class 임포트 필요
import kotlinx.coroutines.delay



@Composable
fun SimpleCountDownTimer(
    totalSeconds: Int = 5,
    onFinished: () -> Unit
) {
    var timeLeft by remember { mutableIntStateOf(totalSeconds) }

    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(1000L)
            timeLeft--
        }
        onFinished()
    }

    Text(
        text = if (timeLeft > 0) "게임 시작까지 ${timeLeft}초" else "게임 시작!",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = if (timeLeft <= 3) Color.Red else Color.Black
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameWaitingRoomScreen(
    roomName: String,
    users: List<GameUser>,
    onStartGameClick: () -> Unit,
    onChangeGameScreen: () -> Unit
) {
    val isGameStartEnabled = (users.size > 1) && users.all { user -> user.isReady }
    val isGameButton = users.isNotEmpty()

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

            Button(
                onClick = onStartGameClick,
                enabled = isGameButton && !isGameStartEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (isGameStartEnabled) {
                    SimpleCountDownTimer(
                        totalSeconds = 5,
                        onFinished = onChangeGameScreen
                    )
                } else {
                    Text(
                        text = "게임 준비",
                        fontSize = 18.sp
                    )
                }
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
        users.map { user -> PlayerCard(user, modifier = Modifier.weight(1f)) }
    }
}

@Composable
fun RoomListScreen(
    rooms: List<GameRooms>, // SnapshotStateList 대신 일반 List를 받는 것이 UI 분리에 더 적합
    onCreateRoomClick: () -> Unit, // Enum<AppScreen> 대신 Unit을 받도록 콜백 변경 (화면 이동 로직은 Activity에 남김)
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "게임 방 목록",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Button(
                    onClick = onCreateRoomClick, // 콜백 호출
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .padding(bottom = 24.dp) // 텍스트와 높이를 맞춤
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
                        RoomCard(room = room, onClick = { onJoinRoomClick(room) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoom(onRoomCreated: (String, String) -> Unit) {

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
            onClick = { onRoomCreated(nickname, roomTitle) },
            enabled = isButtonEnabled
        ) {
            Text("방 만들기")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinRoom(onRoomJoined: (String) -> Unit) { // 콜백 이름 변경 권장 (onRoomCreated -> onRoomJoined)

    var nickname by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("방 접속", style = MaterialTheme.typography.headlineSmall) // 제목 추가
        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = nickname,
            onValueChange = { nickname = it },
            label = { Text("닉네임을 입력해주세요") },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        val isButtonEnabled = nickname.isNotBlank()

        Button(
            onClick = { onRoomJoined(nickname) },
            enabled = isButtonEnabled
        ) {
            Text("방 접속")
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomCard(room: GameRooms, onClick: () -> Unit) {
    Card(
        onClick = onClick,
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
                text = "방장 : ${room.hostName}",
                style = MaterialTheme.typography.bodyMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "참여 인원 : ${room.playerCount}/2",
                    style = MaterialTheme.typography.bodySmall,
                )

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "입장"
                )
            }
        }
    }


}

