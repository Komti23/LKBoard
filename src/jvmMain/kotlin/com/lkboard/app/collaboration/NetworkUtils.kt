package com.lkboard.app.collaboration

import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import kotlin.random.Random

object NetworkUtils {

    const val DEFAULT_PORT = 8765
    const val PUBLIC_RELAY_IP = "164.37.224.219"
    const val PUBLIC_RELAY_PORT = 8765

    /**
     * Создает общедоступную интернет-ссылку на облачный сервер LKBoard.
     */
    fun buildPublicWebLink(roomCode: String): String {
        return "http://$PUBLIC_RELAY_IP:$PUBLIC_RELAY_PORT/?room=$roomCode"
    }

    /**
     * Создает интернет-ссылку подключения для приложения LKBoard.
     */
    fun buildPublicAppLink(roomCode: String): String {
        return "lkboard://connect?host=$PUBLIC_RELAY_IP&port=$PUBLIC_RELAY_PORT&room=$roomCode"
    }

    /**
     * Создает полноценную длинную ссылку для режима графического планшета на смартфоне
     * с индивидуальным ID, именем, цветом пера и кодом комнаты.
     */
    fun buildTabletWebLink(
        localIp: String,
        port: Int,
        id: String? = null,
        name: String? = null,
        colorHex: Long? = null,
        roomCode: String? = null
    ): String {
        val base = "http://$localIp:$port/tablet"
        val params = mutableListOf<String>()
        val finalId = if (!id.isNullOrBlank()) id else "teacher_tablet"
        params.add("id=" + java.net.URLEncoder.encode(finalId, java.nio.charset.StandardCharsets.UTF_8))
        val finalName = if (!name.isNullOrBlank()) name else "Преподаватель"
        params.add("name=" + java.net.URLEncoder.encode(finalName, java.nio.charset.StandardCharsets.UTF_8))
        if (colorHex != null) {
            params.add("color=$colorHex")
        } else {
            params.add("color=0xFF2563EB")
        }
        if (!roomCode.isNullOrBlank()) {
            params.add("room=" + java.net.URLEncoder.encode(roomCode, java.nio.charset.StandardCharsets.UTF_8))
        }
        return "$base?${params.joinToString("&")}"
    }

    /**
     * Возвращает список доступных локальных IPv4 адресов машины (Wi-Fi, Ethernet).
     */
    fun getLocalIpAddresses(): List<String> {
        val ips = mutableListOf<String>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue

                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress && !addr.isLinkLocalAddress) {
                        ips.add(addr.hostAddress)
                    }
                }
            }
        } catch (_: Exception) {}

        if (ips.isEmpty()) {
            ips.add("127.0.0.1")
        }
        return ips
    }

    /**
     * Возвращает основной локальный IP адрес для ссылки.
     */
    fun getPrimaryLocalIp(): String {
        val all = getLocalIpAddresses()
        // Предпочитаем адреса частных подсетей (192.168.x.x, 10.x.x.x, 172.16-31.x.x)
        return all.firstOrNull { it.startsWith("192.168.") }
            ?: all.firstOrNull { it.startsWith("10.") }
            ?: all.firstOrNull { it.startsWith("172.") }
            ?: all.firstOrNull()
            ?: "127.0.0.1"
    }

    /**
     * Проверяет, свободен ли указанный порт, либо находит ближайший свободный.
     */
    fun findAvailablePort(preferredPort: Int = DEFAULT_PORT): Int {
        for (port in preferredPort..(preferredPort + 50)) {
            try {
                ServerSocket(port).use { return port }
            } catch (_: Exception) {}
        }
        return preferredPort
    }

    /**
     * Генерирует запоминающийся 6-значный код комнаты (например, LK-5824).
     */
    fun generateRoomCode(): String {
        val number = Random.nextInt(1000, 9999)
        return "LK-$number"
    }

    /**
     * Создает прямую веб-ссылку для браузера.
     */
    fun buildWebLink(ipOrHost: String, port: Int): String {
        return "http://$ipOrHost:$port/"
    }

    /**
     * Создает ссылку протокола для быстрого подключения второго клиента LKBoard.
     */
    fun buildAppLink(ipOrHost: String, port: Int): String {
        return "lkboard://connect?host=$ipOrHost&port=$port"
    }
}
