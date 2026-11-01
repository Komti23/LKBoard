package com.lkboard.app.data

import com.lkboard.app.model.BoardState
import com.lkboard.app.model.GridType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * Метаданные сохраненной доски для отображения на Главном экране (Dashboard).
 */
@Serializable
data class BoardItemInfo(
    val id: String,
    val title: String,
    val fileName: String,
    val elementCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val backgroundColorHex: Long = 0xFFFFFFFFL,
    val gridType: GridType = GridType.DOTS,
    val templateType: String = "BLANK"
)

/**
 * Индекс всех проектов пользователя.
 */
@Serializable
data class BoardsIndex(
    val boards: List<BoardItemInfo> = emptyList(),
    val lastActiveBoardId: String? = null
)

/**
 * Менеджер управления проектами и множественными досками Lkboard:
 * - Хранит файлы проектов в `%APPDATA%/LKBoard/boards/`
 * - Ведет индекс с быстрым доступом `boards_index.json`
 * - Обеспечивает плавную миграцию существующих автосохранений
 * - Поддерживает создание по шаблонам (включая Roadmaps), дублирование, удаление и экспорт
 */
class BoardManager(
    private val appDataDir: File = AppPreferences.getAppDataDir()
) {
    private val boardsDir = File(appDataDir, "boards").apply {
        if (!exists()) mkdirs()
    }

    val thumbnailsDir: File = File(boardsDir, "thumbnails").apply {
        if (!exists()) mkdirs()
    }

    fun getThumbnailFile(boardId: String): File {
        return File(thumbnailsDir, "thumb_$boardId.png")
    }

    private val indexFile = File(boardsDir, "boards_index.json")

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Synchronized
    fun getIndex(): BoardsIndex {
        return try {
            if (indexFile.exists()) {
                val content = indexFile.readText(Charsets.UTF_8)
                val idx = json.decodeFromString<BoardsIndex>(content)
                // Дедупликация досок по id
                val uniqueBoards = idx.boards.distinctBy { it.id }
                val hasDuplicates = uniqueBoards.size != idx.boards.size

                // Самовосстановление: проверяем, нет ли на диске .lkboard файлов, отсутствующих в индексе
                val diskFiles = boardsDir.listFiles { _, name -> name.endsWith(".lkboard") } ?: emptyArray()
                val knownFileNames = uniqueBoards.map { it.fileName }.toSet()
                val knownIds = uniqueBoards.map { it.id }.toSet()
                val missingFiles = diskFiles.filter { it.name !in knownFileNames }
                if (missingFiles.isNotEmpty() || hasDuplicates) {
                    val recovered = missingFiles.mapNotNull { f ->
                        try {
                            val state = BoardSerializer.load(f)
                            val id = f.nameWithoutExtension.removePrefix("board_")
                            if (id in knownIds) return@mapNotNull null
                            BoardItemInfo(
                                id = id,
                                title = state.title.ifBlank { "Моя доска" },
                                fileName = f.name,
                                elementCount = state.elements.size,
                                updatedAt = f.lastModified(),
                                createdAt = f.lastModified(),
                                backgroundColorHex = state.backgroundColorHex,
                                gridType = state.gridType,
                                templateType = "BLANK"
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    val updatedList = (uniqueBoards + recovered).distinctBy { it.id }.sortedByDescending { it.updatedAt }
                    val updated = idx.copy(boards = updatedList)
                    saveIndex(updated)
                    updated
                } else {
                    idx.copy(boards = uniqueBoards)
                }
            } else {
                initDefaultBoards()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            initDefaultBoards()
        }
    }

    @Synchronized
    private fun saveIndex(index: BoardsIndex) {
        try {
            val content = json.encodeToString(index)
            indexFile.writeText(content, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Инициализация при первом запуске: восстановление существующих файлов, миграция autosave или чистая доска.
     */
    private fun initDefaultBoards(): BoardsIndex {
        val list = mutableListOf<BoardItemInfo>()

        // 1. Проверяем наличие существующих .lkboard файлов на диске (чтобы никогда не потерять доски пользователя)
        val diskFiles = boardsDir.listFiles { _, name -> name.endsWith(".lkboard") } ?: emptyArray()
        for (f in diskFiles) {
            try {
                val state = BoardSerializer.load(f)
                val id = f.nameWithoutExtension.removePrefix("board_")
                list.add(
                    BoardItemInfo(
                        id = id,
                        title = state.title.ifBlank { "Моя доска" },
                        fileName = f.name,
                        elementCount = state.elements.size,
                        updatedAt = f.lastModified(),
                        createdAt = f.lastModified(),
                        backgroundColorHex = state.backgroundColorHex,
                        gridType = state.gridType,
                        templateType = "BLANK"
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        var initialActiveId: String? = list.maxByOrNull { it.updatedAt }?.id

        // 2. Если досок на диске нет, проверяем autosave_board
        if (list.isEmpty()) {
            val autoSaveFile = AppPreferences.getAutoSaveFile()
            if (autoSaveFile.exists() && autoSaveFile.length() > 0) {
                try {
                    val state = BoardSerializer.load(autoSaveFile)
                    val id = UUID.randomUUID().toString()
                    val fileName = "board_$id.lkboard"
                    val targetFile = File(boardsDir, fileName)
                    BoardSerializer.save(targetFile, state)

                    val item = BoardItemInfo(
                        id = id,
                        title = state.title.ifBlank { "Моя доска" },
                        fileName = fileName,
                        elementCount = state.elements.size,
                        updatedAt = System.currentTimeMillis(),
                        createdAt = System.currentTimeMillis(),
                        backgroundColorHex = state.backgroundColorHex,
                        gridType = state.gridType,
                        templateType = "BLANK"
                    )
                    list.add(item)
                    initialActiveId = id
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // 3. Если всё еще пусто — создаем одну чистую пустую доску (без навязчивых демо-элементов)
        if (list.isEmpty()) {
            val (blankItem, _) = createBoard("Моя первая доска", BoardTemplate.BLANK)
            list.add(blankItem)
            initialActiveId = blankItem.id
        }

        val sortedList = list.sortedByDescending { it.updatedAt }
        val newIndex = BoardsIndex(boards = sortedList, lastActiveBoardId = initialActiveId ?: sortedList.firstOrNull()?.id)
        saveIndex(newIndex)
        return newIndex
    }

    fun listBoards(): List<BoardItemInfo> {
        val index = getIndex()
        // Фильтруем записи, файлы которых реально существуют на диске
        val validBoards = index.boards.filter {
            File(boardsDir, it.fileName).exists()
        }.sortedByDescending { it.updatedAt }

        if (validBoards.size != index.boards.size) {
            saveIndex(index.copy(boards = validBoards))
        }
        return validBoards
    }

    fun getBoardFile(info: BoardItemInfo): File {
        return File(boardsDir, info.fileName)
    }

    fun getBoardFile(id: String): File? {
        val info = getIndex().boards.find { it.id == id } ?: return null
        return File(boardsDir, info.fileName)
    }

    @Synchronized
    fun createBoard(
        title: String,
        template: BoardTemplate = BoardTemplate.BLANK
    ): Pair<BoardItemInfo, BoardState> {
        val id = UUID.randomUUID().toString()
        val cleanTitle = title.trim().ifBlank { "Новая доска" }
        val fileName = "board_$id.lkboard"
        val file = File(boardsDir, fileName)

        val state = BoardTemplateGenerator.generate(template, cleanTitle)
        BoardSerializer.save(file, state)

        if (state.elements.isNotEmpty()) {
            try {
                com.lkboard.app.export.PngExporter.renderThumbnailToPng(getThumbnailFile(id), state)
            } catch (_: Exception) {}
        }

        val now = System.currentTimeMillis()
        val item = BoardItemInfo(
            id = id,
            title = cleanTitle,
            fileName = fileName,
            elementCount = state.elements.size,
            updatedAt = now,
            createdAt = now,
            backgroundColorHex = state.backgroundColorHex,
            gridType = state.gridType,
            templateType = template.name
        )

        val current = getIndex()
        val updatedList = listOf(item) + current.boards
        saveIndex(current.copy(boards = updatedList, lastActiveBoardId = id))

        return item to state
    }

    @Synchronized
    fun loadBoard(id: String): Pair<BoardItemInfo, BoardState>? {
        val index = getIndex()
        val info = index.boards.find { it.id == id } ?: return null
        val file = File(boardsDir, info.fileName)
        if (!file.exists()) return null

        val state = BoardSerializer.load(file)
        saveIndex(index.copy(lastActiveBoardId = id))
        return info to state
    }

    @Synchronized
    fun saveBoard(id: String, state: BoardState): BoardItemInfo? {
        val index = getIndex()
        val info = index.boards.find { it.id == id } ?: return null
        val file = File(boardsDir, info.fileName)

        BoardSerializer.save(file, state)

        // Асинхронное/фоновое обновление миниатюры доски
        try {
            val thumb = getThumbnailFile(id)
            if (state.elements.isNotEmpty()) {
                com.lkboard.app.export.PngExporter.renderThumbnailToPng(thumb, state)
            } else if (thumb.exists()) {
                thumb.delete()
            }
        } catch (_: Exception) {}

        val now = System.currentTimeMillis()
        val updatedInfo = info.copy(
            title = state.title,
            elementCount = state.elements.size,
            updatedAt = now,
            backgroundColorHex = state.backgroundColorHex,
            gridType = state.gridType
        )

        val updatedList = index.boards.map { if (it.id == id) updatedInfo else it }
        saveIndex(index.copy(boards = updatedList, lastActiveBoardId = id))
        return updatedInfo
    }

    @Synchronized
    fun renameBoard(id: String, newTitle: String): BoardItemInfo? {
        val index = getIndex()
        val info = index.boards.find { it.id == id } ?: return null
        val cleanTitle = newTitle.trim().ifBlank { info.title }
        val file = File(boardsDir, info.fileName)

        if (file.exists()) {
            try {
                val state = BoardSerializer.load(file)
                val updatedState = state.copy(title = cleanTitle)
                BoardSerializer.save(file, updatedState)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val updatedInfo = info.copy(
            title = cleanTitle,
            updatedAt = System.currentTimeMillis()
        )
        val updatedList = index.boards.map { if (it.id == id) updatedInfo else it }
        saveIndex(index.copy(boards = updatedList))
        return updatedInfo
    }

    @Synchronized
    fun duplicateBoard(id: String): BoardItemInfo? {
        val index = getIndex()
        val sourceInfo = index.boards.find { it.id == id } ?: return null
        val sourceFile = File(boardsDir, sourceInfo.fileName)
        if (!sourceFile.exists()) return null

        val newId = UUID.randomUUID().toString()
        val newFileName = "board_$newId.lkboard"
        val newFile = File(boardsDir, newFileName)

        val sourceState = BoardSerializer.load(sourceFile)
        val duplicatedTitle = "${sourceInfo.title} (Копия)"
        val duplicatedState = sourceState.copy(title = duplicatedTitle)
        BoardSerializer.save(newFile, duplicatedState)

        val now = System.currentTimeMillis()
        val newItem = BoardItemInfo(
            id = newId,
            title = duplicatedTitle,
            fileName = newFileName,
            elementCount = duplicatedState.elements.size,
            updatedAt = now,
            createdAt = now,
            backgroundColorHex = duplicatedState.backgroundColorHex,
            gridType = duplicatedState.gridType,
            templateType = sourceInfo.templateType
        )

        try {
            val srcThumb = getThumbnailFile(id)
            if (srcThumb.exists()) {
                srcThumb.copyTo(getThumbnailFile(newId), overwrite = true)
            }
        } catch (_: Exception) {}

        val updatedList = listOf(newItem) + index.boards
        saveIndex(index.copy(boards = updatedList))
        return newItem
    }

    @Synchronized
    fun deleteBoard(id: String): Boolean {
        val index = getIndex()
        val info = index.boards.find { it.id == id } ?: return false
        val file = File(boardsDir, info.fileName)
        if (file.exists()) {
            file.delete()
        }

        try {
            getThumbnailFile(id).delete()
        } catch (_: Exception) {}

        val updatedList = index.boards.filterNot { it.id == id }
        val newActiveId = if (index.lastActiveBoardId == id) {
            updatedList.firstOrNull()?.id
        } else index.lastActiveBoardId

        saveIndex(index.copy(boards = updatedList, lastActiveBoardId = newActiveId))
        return true
    }

    @Synchronized
    fun importBoardFile(sourceFile: File): Pair<BoardItemInfo, BoardState>? {
        if (!sourceFile.exists()) return null
        return try {
            val state = BoardSerializer.load(sourceFile)
            val id = UUID.randomUUID().toString()
            val fileName = "board_$id.lkboard"
            val targetFile = File(boardsDir, fileName)
            BoardSerializer.save(targetFile, state)

            val now = System.currentTimeMillis()
            val item = BoardItemInfo(
                id = id,
                title = state.title.ifBlank { sourceFile.nameWithoutExtension },
                fileName = fileName,
                elementCount = state.elements.size,
                updatedAt = now,
                createdAt = now,
                backgroundColorHex = state.backgroundColorHex,
                gridType = state.gridType,
                templateType = "IMPORTED"
            )

            val index = getIndex()
            val updatedList = listOf(item) + index.boards
            saveIndex(index.copy(boards = updatedList, lastActiveBoardId = id))

            if (state.elements.isNotEmpty()) {
                try {
                    com.lkboard.app.export.PngExporter.renderThumbnailToPng(getThumbnailFile(id), state)
                } catch (_: Exception) {}
            }

            item to state
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Фоновая генерация миниатюр для существующих досок, у которых есть элементы, но еще нет файла превью.
     */
    fun preloadMissingThumbnails() {
        try {
            val current = getIndex()
            for (board in current.boards) {
                if (board.elementCount > 0) {
                    val thumb = getThumbnailFile(board.id)
                    if (!thumb.exists() || thumb.length() == 0L) {
                        val file = File(boardsDir, board.fileName)
                        if (file.exists()) {
                            try {
                                val state = BoardSerializer.load(file)
                                com.lkboard.app.export.PngExporter.renderThumbnailToPng(thumb, state)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
