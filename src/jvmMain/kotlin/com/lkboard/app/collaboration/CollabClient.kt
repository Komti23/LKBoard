package com.lkboard.app.collaboration

import com.lkboard.app.data.BoardSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.UUID

/**
 * Сетевой клиент LKBoard для подключения к совместной сессии хоста.
 * Использует стандартный java.net.http.HttpClient для получения SSE-потока и отправки действий.
 */
class CollabClient {

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(6))
        .build()

    private val clientScope = CoroutineScope(Dispatchers.IO + Job())
    private var streamJob: Job? = null

    val clientId: String = "desktop-${UUID.randomUUID().toString().take(8)}"
    var currentHostUrl: String = ""
        private set
    var isConnected: Boolean = false
        private set

    fun connect(
        hostUrl: String,
        userName: String,
        onActionReceived: (CollabAction) -> Unit,
        onStatusChanged: (connected: Boolean, message: String) -> Unit
    ) {
        disconnect()

        val normalizedUrl = normalizeUrl(hostUrl)
        currentHostUrl = normalizedUrl

        streamJob = clientScope.launch {
            try {
                onStatusChanged(false, "Подключение к $normalizedUrl...")

                // 1. Проверяем доступность хоста и получаем снимок доски
                val snapshotReq = HttpRequest.newBuilder()
                    .uri(URI.create(buildEndpointUrl(normalizedUrl, "/api/snapshot")))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build()

                val snapshotResp = httpClient.send(snapshotReq, HttpResponse.BodyHandlers.ofString(Charsets.UTF_8))
                if (snapshotResp.statusCode() != 200) {
                    onStatusChanged(false, "Ошибка подключения: HTTP ${snapshotResp.statusCode()}")
                    return@launch
                }

                val doc = BoardSerializer.deserialize(snapshotResp.body())
                onActionReceived(
                    CollabAction.Snapshot(
                        document = doc,
                        senderId = "host",
                        senderName = "Хост"
                    )
                )

                // 2. Отправляем уведомление о подключении участника
                val joinAction = CollabAction.PeerJoined(
                    CollabPeer(
                        id = clientId,
                        name = userName,
                        colorHex = 0xFF0284C7,
                        isHost = false,
                        clientType = "Desktop"
                    )
                )
                sendAction(joinAction)

                isConnected = true
                onStatusChanged(true, "Подключено к доске")

                // 3. Подключаемся к потоку Server-Sent Events (SSE)
                val eventsReq = HttpRequest.newBuilder()
                    .uri(URI.create(buildEndpointUrl(normalizedUrl, "/api/events")))
                    .header("Accept", "text/event-stream")
                    .timeout(Duration.ofHours(24))
                    .GET()
                    .build()

                val eventsResp = httpClient.send(eventsReq, HttpResponse.BodyHandlers.ofLines())
                val linesStream = eventsResp.body()

                linesStream.forEach { line ->
                    if (!isActive) return@forEach
                    if (line.startsWith("data: ")) {
                        val json = line.removePrefix("data: ").trim()
                        if (json.isNotEmpty()) {
                            try {
                                val action = BoardSerializer.jsonFormat.decodeFromString<CollabAction>(json)
                                // Игнорируем собственные эхо-сообщения
                                val isSelf = when (action) {
                                    is CollabAction.UpsertElement -> action.senderId == clientId
                                    is CollabAction.DeleteElements -> action.senderId == clientId
                                    is CollabAction.PointerMove -> action.senderId == clientId
                                    is CollabAction.ToggleStickyTodo -> action.senderId == clientId
                                    is CollabAction.ClearBoard -> action.senderId == clientId
                                    else -> false
                                }
                                if (!isSelf) {
                                    onActionReceived(action)
                                }
                            } catch (_: Exception) {}
                        }
                    }
                }
            } catch (e: Exception) {
                isConnected = false
                onStatusChanged(false, "Отключено: ${e.message ?: "соединение разорвано"}")
            }
        }
    }

    /**
     * Отправляет локальное действие участника хосту для синхронизации со всеми.
     */
    fun sendAction(action: CollabAction) {
        val host = currentHostUrl
        if (host.isEmpty()) return

        clientScope.launch {
            try {
                val json = BoardSerializer.jsonFormat.encodeToString(action)
                val req = HttpRequest.newBuilder()
                    .uri(URI.create(buildEndpointUrl(host, "/api/action")))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(6))
                    .POST(HttpRequest.BodyPublishers.ofString(json, Charsets.UTF_8))
                    .build()
                httpClient.send(req, HttpResponse.BodyHandlers.discarding())
            } catch (_: Exception) {}
        }
    }

    fun disconnect() {
        if (isConnected) {
            sendAction(CollabAction.PeerLeft(clientId))
        }
        streamJob?.cancel()
        streamJob = null
        isConnected = false
        currentHostUrl = ""
    }

    private fun buildEndpointUrl(base: String, endpoint: String): String {
        return try {
            val uri = URI(base)
            val query = uri.query
            val scheme = uri.scheme ?: "http"
            val authority = uri.authority ?: base.removePrefix("http://").removePrefix("https://").substringBefore("/")
            val basePath = "$scheme://$authority$endpoint"
            if (!query.isNullOrBlank()) "$basePath?$query" else basePath
        } catch (_: Exception) {
            "$base$endpoint"
        }
    }

    private fun normalizeUrl(url: String): String {
        var u = url.trim()
        if (u.startsWith("lkboard://connect?host=")) {
            // lkboard://connect?host=192.168.1.5&port=8765
            val query = u.substringAfter("?")
            val params = query.split("&").associate {
                val parts = it.split("=")
                parts[0] to parts.getOrElse(1) { "" }
            }
            val host = params["host"] ?: "localhost"
            val port = params["port"] ?: "${NetworkUtils.DEFAULT_PORT}"
            return "http://$host:$port"
        }
        if (!u.startsWith("http://") && !u.startsWith("https://")) {
            u = "http://$u"
        }
        return u.removeSuffix("/")
    }
}
