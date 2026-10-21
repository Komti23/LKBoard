package com.lkboard.app.collaboration

import java.io.File

/**
 * Генератор HTML/JS клиента для веб-браузеров.
 * Загружает чистый оптимизированный шаблон client.html из ресурсов или файла.
 */
object WebClientHtml {

    fun generateHtml(boardTitle: String, hostName: String, port: Int): String {
        // 1. Проверяем наличие ресурса в classpath (для сборки в JAR)
        try {
            val stream = WebClientHtml::class.java.getResourceAsStream("/client.html")
            if (stream != null) {
                val content = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                return content.replace("LK-ROOM-PLACEHOLDER", "LK-LOCAL")
                    .replace("LKBoard Live", boardTitle)
            }
        } catch (_: Exception) {}

        // 2. Проверяем файл в рабочей директории приложения
        try {
            val file = File("client.html")
            if (file.exists()) {
                val content = file.readText(Charsets.UTF_8)
                return content.replace("LK-ROOM-PLACEHOLDER", "LK-LOCAL")
                    .replace("LKBoard Live", boardTitle)
            }
        } catch (_: Exception) {}

        // 3. Резервный минимальный HTML
        return """
<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <title>$boardTitle — LKBoard</title>
</head>
<body>
    <canvas id="canvas"></canvas>
</body>
</html>
""".trimIndent()
    }
}
