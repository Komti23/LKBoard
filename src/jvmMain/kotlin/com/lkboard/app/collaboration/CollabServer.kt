package com.lkboard.app.collaboration

import com.lkboard.app.data.BoardDocument
import com.lkboard.app.data.BoardSerializer
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import java.io.OutputStream
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Встроенный сервер совместной работы на базе стандартного JDK HttpServer.
 * Обеспечивает веб-клиент для браузеров и SSE-поток реального времени для Desktop и Web.
 */
class CollabServer(
    private var snapshotProvider: () -> BoardDocument,
    private val onActionReceived: (CollabAction) -> Unit
) {

    private var server: HttpServer? = null
    private val sseClients = ConcurrentHashMap<String, OutputStream>()
    private val serverScope = CoroutineScope(Dispatchers.IO + Job())
    private var heartbeatJob: Job? = null

    var actualPort: Int = NetworkUtils.DEFAULT_PORT
        private set

    val connectedClientsCount: Int
        get() = sseClients.size

    @Synchronized
    fun start(port: Int = NetworkUtils.DEFAULT_PORT, boardTitle: String = "LKBoard", hostName: String = "Хост"): Int {
        if (server != null) return actualPort

        actualPort = NetworkUtils.findAvailablePort(port)
        val s = HttpServer.create(InetSocketAddress(actualPort), 0)
        s.executor = Executors.newCachedThreadPool()

        // 1. Корневой маршрут — веб-клиент LKBoard для браузеров
        s.createContext("/") { exchange ->
            try {
                if (exchange.requestMethod.equals("GET", ignoreCase = true) && exchange.requestURI.path == "/") {
                    val html = WebClientHtml.generateHtml(boardTitle, hostName, actualPort)
                    val bytes = html.toByteArray(Charsets.UTF_8)
                    exchange.responseHeaders.set("Content-Type", "text/html; charset=UTF-8")
                    exchange.responseHeaders.set("Access-Control-Allow-Origin", "*")
                    exchange.sendResponseHeaders(200, bytes.size.toLong())
                    exchange.responseBody.use { it.write(bytes) }
                } else if (exchange.requestURI.path != "/") {
                    sendNotFound(exchange)
                }
            } catch (_: Exception) {
                sendNotFound(exchange)
            }
        }

        // 2. Снимок доски в формате JSON
        s.createContext("/api/snapshot") { exchange ->
            handleCors(exchange)
            if (exchange.requestMethod.equals("OPTIONS", ignoreCase = true)) {
                exchange.sendResponseHeaders(204, -1)
                return@createContext
            }
            try {
                val doc = snapshotProvider()
                val json = BoardSerializer.serialize(doc)
                val bytes = json.toByteArray(Charsets.UTF_8)
                exchange.responseHeaders.set("Content-Type", "application/json; charset=UTF-8")
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            } catch (_: Exception) {
                sendInternalError(exchange)
            }
        }

        // 3. Получение действий (actions) от участников
        s.createContext("/api/action") { exchange ->
            handleCors(exchange)
            if (exchange.requestMethod.equals("OPTIONS", ignoreCase = true)) {
                exchange.sendResponseHeaders(204, -1)
                return@createContext
            }
            if (exchange.requestMethod.equals("POST", ignoreCase = true)) {
                try {
                    val body = exchange.requestBody.reader(Charsets.UTF_8).readText()
                    val action = BoardSerializer.jsonFormat.decodeFromString<CollabAction>(body)
                    
                    // Применяем локально у хоста
                    onActionReceived(action)

                    // Рассылаем всем остальным подключенным клиентам
                    broadcastAction(action)

                    val resp = """{"status":"ok"}""".toByteArray()
                    exchange.responseHeaders.set("Content-Type", "application/json")
                    exchange.sendResponseHeaders(200, resp.size.toLong())
                    exchange.responseBody.use { it.write(resp) }
                } catch (e: Exception) {
                    val err = """{"status":"error","message":"${e.message}"}""".toByteArray()
                    exchange.responseHeaders.set("Content-Type", "application/json")
                    exchange.sendResponseHeaders(400, err.size.toLong())
                    exchange.responseBody.use { it.write(err) }
                }
            } else {
                exchange.sendResponseHeaders(405, -1)
            }
        }

        // 4. Поток событий реального времени Server-Sent Events (SSE)
        s.createContext("/api/events") { exchange ->
            handleCors(exchange)
            if (exchange.requestMethod.equals("OPTIONS", ignoreCase = true)) {
                exchange.sendResponseHeaders(204, -1)
                return@createContext
            }
            try {
                exchange.responseHeaders.set("Content-Type", "text/event-stream; charset=UTF-8")
                exchange.responseHeaders.set("Cache-Control", "no-cache")
                exchange.responseHeaders.set("Connection", "keep-alive")
                exchange.sendResponseHeaders(200, 0) // chunked transfer

                val clientId = "client-${System.currentTimeMillis()}-${(1000..9999).random()}"
                val os = exchange.responseBody
                sseClients[clientId] = os

                // Отправляем начальный снимок подключившемуся клиенту
                val initialSnapshot = CollabAction.Snapshot(
                    document = snapshotProvider(),
                    senderId = "host",
                    senderName = hostName
                )
                val json = BoardSerializer.jsonFormat.encodeToString(initialSnapshot)
                val sseData = "data: $json\n\n".toByteArray(Charsets.UTF_8)
                os.write(sseData)
                os.flush()
            } catch (_: Exception) {}
        }

        // 5. Статус сервера
        s.createContext("/api/status") { exchange ->
            handleCors(exchange)
            val json = """{"active":true,"clients":${sseClients.size},"port":$actualPort}""".toByteArray()
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, json.size.toLong())
            exchange.responseBody.use { it.write(json) }
        }

        s.start()
        server = s

        // Heartbeat корутина для поддержания SSE соединений
        heartbeatJob = serverScope.launch {
            while (isActive) {
                delay(15_000)
                val ping = ": ping\n\n".toByteArray(Charsets.UTF_8)
                val deadClients = mutableListOf<String>()
                for ((id, os) in sseClients) {
                    try {
                        os.write(ping)
                        os.flush()
                    } catch (_: Exception) {
                        deadClients.add(id)
                    }
                }
                deadClients.forEach { sseClients.remove(it) }
            }
        }

        return actualPort
    }

    /**
     * Рассылает действие всем подключенным клиентам SSE.
     */
    fun broadcastAction(action: CollabAction) {
        val json = BoardSerializer.jsonFormat.encodeToString(action)
        val sseData = "data: $json\n\n".toByteArray(Charsets.UTF_8)
        val dead = mutableListOf<String>()
        for ((id, os) in sseClients) {
            try {
                os.write(sseData)
                os.flush()
            } catch (_: Exception) {
                dead.add(id)
            }
        }
        dead.forEach { sseClients.remove(it) }
    }

    fun updateSnapshotProvider(newProvider: () -> BoardDocument) {
        snapshotProvider = newProvider
    }

    @Synchronized
    fun stop() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        for ((_, os) in sseClients) {
            try { os.close() } catch (_: Exception) {}
        }
        sseClients.clear()
        server?.stop(0)
        server = null
    }

    private fun handleCors(exchange: HttpExchange) {
        exchange.responseHeaders.set("Access-Control-Allow-Origin", "*")
        exchange.responseHeaders.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        exchange.responseHeaders.set("Access-Control-Allow-Headers", "Content-Type, Authorization")
    }

    private fun sendNotFound(exchange: HttpExchange) {
        val msg = "Not Found".toByteArray()
        exchange.sendResponseHeaders(404, msg.size.toLong())
        exchange.responseBody.use { it.write(msg) }
    }

    private fun sendInternalError(exchange: HttpExchange) {
        val msg = "Internal Server Error".toByteArray()
        exchange.sendResponseHeaders(500, msg.size.toLong())
        exchange.responseBody.use { it.write(msg) }
    }
}
