package com.lkboard.app.collaboration

import com.lkboard.app.data.BoardDocument
import com.lkboard.app.model.BoardElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.util.UUID

data class CollabSessionState(
    val isHosting: Boolean = false,
    val isConnectedAsClient: Boolean = false,
    val serverPort: Int = NetworkUtils.DEFAULT_PORT,
    val localIp: String = "127.0.0.1",
    val webShareLink: String = "",
    val appShareLink: String = "",
    val publicWebShareLink: String = "",
    val publicAppShareLink: String = "",
    val roomCode: String = "",
    val hostUrl: String = "",
    val statusMessage: String = "Совместная работа выключена",
    val connectedPeers: List<CollabPeer> = emptyList(),
    val remotePointers: Map<String, RemotePointer> = emptyMap(),
    val userName: String = "Хост"
) {
    val isActive: Boolean
        get() = isHosting || isConnectedAsClient
}

/**
 * Главный координатор совместной работы LKBoard.
 */
class CollabManager(
    private val snapshotProvider: () -> BoardDocument,
    private val onRemoteElementUpsert: (BoardElement) -> Unit,
    private val onRemoteElementDelete: (List<String>) -> Unit,
    private val onRemoteSnapshotReceived: (BoardDocument) -> Unit,
    private val onRemoteToggleStickyTodo: (noteId: String, lineIndex: Int) -> Unit
) {

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var pointerPruneJob: Job? = null
    private var cloudSyncJob: Job? = null
    private val httpClient = java.net.http.HttpClient.newBuilder()
        .connectTimeout(java.time.Duration.ofSeconds(4))
        .build()

    private val _state = MutableStateFlow(CollabSessionState())
    val state: StateFlow<CollabSessionState> = _state.asStateFlow()

    val myClientId: String = "user-${UUID.randomUUID().toString().take(6)}"

    private var server: CollabServer? = null
    private var client: CollabClient? = null

    init {
        // Фоновая очистка устаревших удаленных указателей (старше 3.5 секунд)
        pointerPruneJob = scope.launch {
            while (isActive) {
                delay(1000)
                val now = System.currentTimeMillis()
                _state.update { curr ->
                    val fresh = curr.remotePointers.filterValues { now - it.lastUpdated < 3500 }
                    if (fresh.size != curr.remotePointers.size) curr.copy(remotePointers = fresh) else curr
                }
            }
        }
    }

    /**
     * Запуск хостинга доски (создание сервера и генерация ссылок).
     */
    fun startHosting(
        port: Int = NetworkUtils.DEFAULT_PORT,
        boardTitle: String = "LKBoard",
        userName: String = "Хост"
    ) {
        stopHosting()
        disconnectClient()

        val primaryIp = NetworkUtils.getPrimaryLocalIp()
        val roomCode = NetworkUtils.generateRoomCode()

        val s = CollabServer(
            snapshotProvider = snapshotProvider,
            onActionReceived = ::handleRemoteAction
        )

        val actualPort = s.start(port = port, boardTitle = boardTitle, hostName = userName)
        server = s

        val webLink = NetworkUtils.buildWebLink(primaryIp, actualPort)
        val appLink = NetworkUtils.buildAppLink(primaryIp, actualPort)
        val publicWebLink = NetworkUtils.buildPublicWebLink(roomCode)
        val publicAppLink = NetworkUtils.buildPublicAppLink(roomCode)

        _state.update {
            it.copy(
                isHosting = true,
                isConnectedAsClient = false,
                serverPort = actualPort,
                localIp = primaryIp,
                webShareLink = webLink,
                appShareLink = appLink,
                publicWebShareLink = publicWebLink,
                publicAppShareLink = publicAppLink,
                roomCode = roomCode,
                userName = userName,
                statusMessage = "Трансляция активна (Локально + Облако)",
                connectedPeers = listOf(
                    CollabPeer(id = myClientId, name = userName, colorHex = 0xFF0284C7, isHost = true)
                )
            )
        }

        // Запуск облачной синхронизации для публичной веб-ссылки
        startCloudRelaySync(roomCode, userName)
    }

    /**
     * Остановка хостинга доски.
     */
    fun stopHosting() {
        cloudSyncJob?.cancel()
        cloudSyncJob = null
        server?.stop()
        server = null
        _state.update {
            it.copy(
                isHosting = false,
                statusMessage = "Совместная работа выключена",
                webShareLink = "",
                appShareLink = "",
                publicWebShareLink = "",
                publicAppShareLink = "",
                connectedPeers = emptyList(),
                remotePointers = emptyMap()
            )
        }
    }

    /**
     * Подключение к чужому хосту в качестве участника (клиента).
     */
    fun connectAsClient(hostUrl: String, userName: String = "Участник") {
        stopHosting()
        disconnectClient()

        val c = CollabClient()
        client = c

        _state.update {
            it.copy(
                isConnectedAsClient = false,
                hostUrl = hostUrl,
                userName = userName,
                statusMessage = "Подключение..."
            )
        }

        c.connect(
            hostUrl = hostUrl,
            userName = userName,
            onActionReceived = ::handleRemoteAction,
            onStatusChanged = { connected, message ->
                _state.update {
                    it.copy(
                        isConnectedAsClient = connected,
                        statusMessage = message
                    )
                }
            }
        )
    }

    /**
     * Отключение от хоста.
     */
    fun disconnectClient() {
        client?.disconnect()
        client = null
        _state.update {
            it.copy(
                isConnectedAsClient = false,
                statusMessage = "Отключено",
                remotePointers = emptyMap()
            )
        }
    }

    // --- Обработка входящих удаленных действий ---

    private fun handleRemoteAction(action: CollabAction) {
        when (action) {
            is CollabAction.Snapshot -> {
                onRemoteSnapshotReceived(action.document)
            }
            is CollabAction.UpsertElement -> {
                onRemoteElementUpsert(action.element)
            }
            is CollabAction.DeleteElements -> {
                onRemoteElementDelete(action.elementIds)
            }
            is CollabAction.ToggleStickyTodo -> {
                onRemoteToggleStickyTodo(action.noteId, action.lineIndex)
            }
            is CollabAction.PointerMove -> {
                _state.update { curr ->
                    val updated = curr.remotePointers.toMutableMap()
                    updated[action.senderId] = RemotePointer(
                        peerId = action.senderId,
                        peerName = action.senderName,
                        x = action.x,
                        y = action.y,
                        colorHex = action.colorHex,
                        isLaser = action.isLaser,
                        lastUpdated = System.currentTimeMillis()
                    )
                    curr.copy(remotePointers = updated)
                }
            }
            is CollabAction.PeerJoined -> {
                _state.update { curr ->
                    val existing = curr.connectedPeers.filter { it.id != action.peer.id }
                    curr.copy(connectedPeers = existing + action.peer)
                }
            }
            is CollabAction.PeerLeft -> {
                _state.update { curr ->
                    val existing = curr.connectedPeers.filter { it.id != action.peerId }
                    val pointers = curr.remotePointers.filterKeys { it != action.peerId }
                    curr.copy(connectedPeers = existing, remotePointers = pointers)
                }
            }
            is CollabAction.ClearBoard -> {}
        }
    }

    // --- Исходящее вещание локальных действий ---

    fun broadcastElementUpsert(element: BoardElement) {
        if (!_state.value.isActive) return
        val action = CollabAction.UpsertElement(
            element = element,
            senderId = myClientId,
            senderName = _state.value.userName
        )
        sendActionToAll(action)
    }

    fun broadcastElementDelete(elementIds: List<String>) {
        if (!_state.value.isActive || elementIds.isEmpty()) return
        val action = CollabAction.DeleteElements(
            elementIds = elementIds,
            senderId = myClientId
        )
        sendActionToAll(action)
    }

    fun broadcastPointer(worldX: Float, worldY: Float, isLaser: Boolean) {
        if (!_state.value.isActive) return
        val action = CollabAction.PointerMove(
            senderId = myClientId,
            senderName = _state.value.userName,
            x = worldX,
            y = worldY,
            colorHex = 0xFF0284C7,
            isLaser = isLaser
        )
        sendActionToAll(action)
    }

    fun broadcastToggleStickyTodo(noteId: String, lineIndex: Int) {
        if (!_state.value.isActive) return
        val action = CollabAction.ToggleStickyTodo(
            noteId = noteId,
            lineIndex = lineIndex,
            senderId = myClientId
        )
        sendActionToAll(action)
    }

    private fun sendActionToAll(action: CollabAction) {
        if (_state.value.isHosting) {
            server?.broadcastAction(action)
            sendActionToCloudRelay(_state.value.roomCode, action)
        } else if (_state.value.isConnectedAsClient) {
            client?.sendAction(action)
        }
    }

    private fun startCloudRelaySync(roomCode: String, userName: String) {
        cloudSyncJob?.cancel()
        cloudSyncJob = scope.launch(Dispatchers.IO) {
            // 1. Немедленно отправляем текущий снимок доски в облачный релей
            try {
                val doc = snapshotProvider()
                val snapshotAction = CollabAction.Snapshot(
                    document = doc,
                    senderId = myClientId,
                    senderName = userName
                )
                sendActionToCloudRelay(roomCode, snapshotAction)
            } catch (_: Exception) {}

            // 2. Периодическая дельта-синхронизация с облаком для получения правок из браузера
            var lastCloudVersion = 0L
            while (isActive) {
                try {
                    val url = "http://${NetworkUtils.PUBLIC_RELAY_IP}:${NetworkUtils.PUBLIC_RELAY_PORT}/api/sync?room=" +
                            java.net.URLEncoder.encode(roomCode, "UTF-8") + "&since=$lastCloudVersion"
                    val req = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(url))
                        .timeout(java.time.Duration.ofSeconds(4))
                        .GET()
                        .build()
                    val resp = httpClient.send(req, java.net.http.HttpResponse.BodyHandlers.ofString(Charsets.UTF_8))
                    if (resp.statusCode() == 200) {
                        val body = resp.body()
                        val syncData = com.lkboard.app.data.BoardSerializer.jsonFormat.decodeFromString<CollabSyncResponse>(body)
                        lastCloudVersion = maxOf(lastCloudVersion, syncData.currentVersion)
                        for (rec in syncData.actions) {
                            val act = rec.action
                            val isSelf = when (act) {
                                is CollabAction.UpsertElement -> act.senderId == myClientId
                                is CollabAction.DeleteElements -> act.senderId == myClientId
                                is CollabAction.PointerMove -> act.senderId == myClientId
                                is CollabAction.ToggleStickyTodo -> act.senderId == myClientId
                                is CollabAction.Snapshot -> act.senderId == myClientId
                                is CollabAction.ClearBoard -> act.senderId == myClientId
                                else -> false
                            }
                            if (!isSelf) {
                                handleRemoteAction(act)
                                server?.broadcastAction(act)
                            }
                        }
                    }
                } catch (_: Exception) {}
                delay(800)
            }
        }
    }

    private fun sendActionToCloudRelay(roomCode: String, action: CollabAction) {
        if (roomCode.isBlank()) return
        scope.launch(Dispatchers.IO) {
            try {
                val json = com.lkboard.app.data.BoardSerializer.jsonFormat.encodeToString<CollabAction>(action)
                val url = "http://${NetworkUtils.PUBLIC_RELAY_IP}:${NetworkUtils.PUBLIC_RELAY_PORT}/api/action?room=" +
                        java.net.URLEncoder.encode(roomCode, "UTF-8")
                val req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(5))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json, Charsets.UTF_8))
                    .build()
                httpClient.send(req, java.net.http.HttpResponse.BodyHandlers.discarding())
            } catch (_: Exception) {}
        }
    }
}
