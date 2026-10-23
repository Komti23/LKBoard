package com.lkboard.app.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI

/**
 * Неблокирующий сервис проверки и получения обновлений LKBoard.
 */
object UpdateChecker {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Выполняет неблокирующую проверку наличия обновлений на сервере.
     * Возвращает UpdateInfo, если версия на сервере новее текущей, либо null.
     */
    suspend fun checkForUpdates(urlStr: String = AppVersion.UPDATE_URL): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URI.create(urlStr).toURL()
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3500
                readTimeout = 3500
                setRequestProperty("User-Agent", "LKBoard/${AppVersion.VERSION_NAME} (Windows; Inking)")
                setRequestProperty("Accept", "application/json")
            }

            if (connection.responseCode != 200) {
                return@withContext null
            }

            val content = BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use {
                it.readText()
            }

            val manifest = json.decodeFromString<UpdateInfo>(content)
            if (isNewerVersion(manifest.version, manifest.versionCode)) {
                manifest
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Сравнивает версию на сервере с локальной версией приложения.
     */
    fun isNewerVersion(remoteVersion: String, remoteVersionCode: Int): Boolean {
        if (remoteVersionCode > AppVersion.VERSION_CODE) {
            return true
        }

        val remoteParts = remoteVersion.trim().removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = AppVersion.VERSION_NAME.trim().removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }

        return false
    }
}
