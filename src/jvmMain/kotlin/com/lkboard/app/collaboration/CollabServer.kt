package com.lkboard.app.collaboration

import com.lkboard.app.data.BoardDocument
import com.lkboard.app.data.BoardSerializer
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.OutputStream
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong


/**
 * Встроенный высокоскоростной сервер совместной работы на базе JDK HttpServer.
 * Обеспечивает веб-клиент для браузеров, непрерывный SSE-поток реального времени и дельта-синхронизацию.
 */
class CollabServer(
    private var snapshotProvider: () -> BoardDocument,
    private val onActionReceived: (CollabAction) -> Unit,
    private val viewportProvider: () -> ViewportInfo = { ViewportInfo() },
    private val recentActionsProvider: () -> List<TabletActionRecord> = { emptyList() }
) {

    private var server: HttpServer? = null
    private val sseClients = ConcurrentHashMap<String, OutputStream>()
    private val clientLocks = ConcurrentHashMap<String, Any>()
    @Volatile
    private var isRunning = false

    private val actionCounter = AtomicLong(0)
    private val actionHistory = ConcurrentLinkedQueue<CollabActionRecord>()
    private val maxHistorySize = 500

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
        isRunning = true

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

        // 1.5. Режим графического планшета для телефона (Stylus Digitizer)
        s.createContext("/tablet") { exchange ->
            try {
                if (exchange.requestMethod.equals("GET", ignoreCase = true)) {
                    val primaryIp = NetworkUtils.getPrimaryLocalIp()
                    val html = TabletClientHtml.generateHtml(boardTitle, actualPort, primaryIp)
                    val bytes = html.toByteArray(Charsets.UTF_8)
                    exchange.responseHeaders.set("Content-Type", "text/html; charset=UTF-8")
                    exchange.responseHeaders.set("Access-Control-Allow-Origin", "*")
                    exchange.sendResponseHeaders(200, bytes.size.toLong())
                    exchange.responseBody.use { it.write(bytes) }
                } else {
                    sendNotFound(exchange)
                }
            } catch (_: Exception) {
                sendNotFound(exchange)
            }
        }

        // 1.6. Геометрия текущего экрана ПК для точного позиционирования штрихов
        s.createContext("/api/viewport") { exchange ->
            handleCors(exchange)
            if (exchange.requestMethod.equals("OPTIONS", ignoreCase = true)) {
                exchange.sendResponseHeaders(204, -1)
                return@createContext
            }
            try {
                val vp = viewportProvider()
                val json = kotlinx.serialization.json.Json.encodeToString(vp)
                val bytes = json.toByteArray(Charsets.UTF_8)
                exchange.responseHeaders.set("Content-Type", "application/json; charset=UTF-8")
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            } catch (_: Exception) {
                sendInternalError(exchange)
            }
        }

        // 1.7. Последние 3 действия на планшетах в реальном времени
        s.createContext("/api/recent-actions") { exchange ->
            handleCors(exchange)
            if (exchange.requestMethod.equals("OPTIONS", ignoreCase = true)) {
                exchange.sendResponseHeaders(204, -1)
                return@createContext
            }
            try {
                val actions = recentActionsProvider()
                val json = kotlinx.serialization.json.Json.encodeToString(actions)
                val bytes = json.toByteArray(Charsets.UTF_8)
                exchange.responseHeaders.set("Content-Type", "application/json; charset=UTF-8")
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            } catch (_: Exception) {
                sendInternalError(exchange)
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

                    // Фиксируем в истории
                    recordAction(action)

                    // Применяем локально у хоста
                    onActionReceived(action)

                    // Рассылаем всем остальным подключенным клиентам SSE
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

        // 4. Дельта-синхронизация по номеру версии (гарантирует получение данных даже при сбое SSE)
        s.createContext("/api/sync") { exchange ->
            handleCors(exchange)
            if (exchange.requestMethod.equals("OPTIONS", ignoreCase = true)) {
                exchange.sendResponseHeaders(204, -1)
                return@createContext
            }
            try {
                val query = exchange.requestURI.query ?: ""
                val since = query.split("&")
                    .firstOrNull { it.startsWith("since=") }
                    ?.substringAfter("since=")
                    ?.toLongOrNull() ?: 0L

                val currentVer = actionCounter.get()
                val newActions = actionHistory.filter { it.id > since }
                val respObj = CollabSyncResponse(
                    currentVersion = currentVer,
                    actions = newActions
                )
                val json = BoardSerializer.jsonFormat.encodeToString(respObj)
                val bytes = json.toByteArray(Charsets.UTF_8)
                exchange.responseHeaders.set("Content-Type", "application/json; charset=UTF-8")
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            } catch (_: Exception) {
                sendInternalError(exchange)
            }
        }

        // 5. Поток событий реального времени Server-Sent Events (SSE)
        s.createContext("/api/events") { exchange ->
            handleCors(exchange)
            if (exchange.requestMethod.equals("OPTIONS", ignoreCase = true)) {
                exchange.sendResponseHeaders(204, -1)
                return@createContext
            }

            val clientId = "client-${System.currentTimeMillis()}-${(1000..9999).random()}"
            val clientLock = Any()
            clientLocks[clientId] = clientLock

            try {
                exchange.responseHeaders.set("Content-Type", "text/event-stream; charset=UTF-8")
                exchange.responseHeaders.set("Cache-Control", "no-cache, no-transform")
                exchange.responseHeaders.set("Connection", "keep-alive")
                exchange.responseHeaders.set("X-Accel-Buffering", "no")
                exchange.sendResponseHeaders(200, 0) // chunked transfer

                val os = exchange.responseBody
                sseClients[clientId] = os

                // Отправляем начальный снимок подключившемуся клиенту
                val initialSnapshot = CollabAction.Snapshot(
                    document = snapshotProvider(),
                    senderId = "host",
                    senderName = hostName
                )
                val json = BoardSerializer.jsonFormat.encodeToString(initialSnapshot)
                synchronized(os) {
                    os.write("data: $json\n\n".toByteArray(Charsets.UTF_8))
                    os.flush()
                }

                // ВАЖНО: удерживаем поток открытым, чтобы HttpServer не закрыл соединение!
                synchronized(clientLock) {
                    while (isRunning && sseClients.containsKey(clientId)) {
                        try {
                            (clientLock as java.lang.Object).wait(15_000)
                        } catch (_: InterruptedException) {
                            break
                        }

                        if (!isRunning || !sseClients.containsKey(clientId)) break

                        // Периодический keep-alive heartbeat
                        try {
                            synchronized(os) {
                                os.write(": ping\n\n".toByteArray(Charsets.UTF_8))
                                os.flush()
                            }
                        } catch (_: Exception) {
                            break
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                sseClients.remove(clientId)
                clientLocks.remove(clientId)
                try { exchange.responseBody.close() } catch (_: Exception) {}
                try { exchange.close() } catch (_: Exception) {}
            }
        }

        // 6. Статус сервера
        s.createContext("/api/status") { exchange ->
            handleCors(exchange)
            val json = """{"active":true,"clients":${sseClients.size},"port":$actualPort,"version":${actionCounter.get()}}""".toByteArray()
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, json.size.toLong())
            exchange.responseBody.use { it.write(json) }
        }

        s.start()
        server = s
        return actualPort
    }

    private fun recordAction(action: CollabAction) {
        val id = actionCounter.incrementAndGet()
        actionHistory.add(CollabActionRecord(id = id, action = action))
        while (actionHistory.size > maxHistorySize) {
            actionHistory.poll()
        }
    }

    /**
     * Рассылает действие всем подключенным клиентам SSE.
     */
    fun broadcastAction(action: CollabAction) {
        recordAction(action)
        val json = BoardSerializer.jsonFormat.encodeToString(action)
        val sseData = "data: $json\n\n".toByteArray(Charsets.UTF_8)
        val dead = mutableListOf<String>()

        for ((id, os) in sseClients) {
            try {
                synchronized(os) {
                    os.write(sseData)
                    os.flush()
                }
            } catch (_: Exception) {
                dead.add(id)
            }
        }

        for (id in dead) {
            sseClients.remove(id)
            clientLocks.remove(id)?.let { lock ->
                synchronized(lock) {
                    try { (lock as java.lang.Object).notifyAll() } catch (_: Exception) {}
                }
            }
        }
    }

    fun updateSnapshotProvider(newProvider: () -> BoardDocument) {
        snapshotProvider = newProvider
    }

    @Synchronized
    fun stop() {
        isRunning = false

        for ((id, lock) in clientLocks) {
            synchronized(lock) {
                try { (lock as java.lang.Object).notifyAll() } catch (_: Exception) {}
            }
        }
        clientLocks.clear()

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
