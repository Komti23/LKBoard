package com.lkboard.app.data

import com.lkboard.app.model.BoardElement
import com.lkboard.app.model.ShapeType
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BoardManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testInitDefaultBoardsCreatesInitialBoard() {
        val root = tempFolder.newFolder("lkboard_appdata")
        val manager = BoardManager(root)

        val boards = manager.listBoards()
        assertTrue(boards.isNotEmpty(), "Должна быть создана хотя бы одна стартовая доска")
        val initial = boards.firstOrNull()
        assertNotNull(initial, "Стартовая доска должна присутствовать в списке")
    }

    @Test
    fun testCreateAndLoadBoardWithRoadmapTemplate() {
        val root = tempFolder.newFolder("lkboard_test2")
        val manager = BoardManager(root)

        val (info, createdState) = manager.createBoard(
            title = "Мой Q3-Q4 План",
            template = BoardTemplate.ROADMAP
        )

        assertEquals("Мой Q3-Q4 План", info.title)
        assertTrue(createdState.elements.isNotEmpty(), "Шаблон дорожной карты должен содержать блоки")

        // Проверяем наличие ключевых фаз и стрелок
        val hasArrows = createdState.elements.values.any {
            it is BoardElement.Shape && it.shapeType == ShapeType.ARROW
        }
        assertTrue(hasArrows, "Дорожная карта обязана содержать соединительные стрелки между этапами")

        // Загрузка доски
        val loadedPair = manager.loadBoard(info.id)
        assertNotNull(loadedPair)
        val (loadedInfo, loadedState) = loadedPair
        assertEquals(info.id, loadedInfo.id)
        assertEquals(createdState.elements.size, loadedState.elements.size)
    }

    @Test
    fun testRenameDuplicateAndDeleteBoard() {
        val root = tempFolder.newFolder("lkboard_test3")
        val manager = BoardManager(root)

        val (created, _) = manager.createBoard("Проект Альфа", BoardTemplate.BLANK)

        // Переименование
        val renamed = manager.renameBoard(created.id, "Проект Бета")
        assertNotNull(renamed)
        assertEquals("Проект Бета", renamed.title)

        // Дублирование
        val duplicated = manager.duplicateBoard(created.id)
        assertNotNull(duplicated)
        assertTrue(duplicated.title.contains("Копия"))
        assertFalse(duplicated.id == created.id)

        // Удаление исходной
        val deleted = manager.deleteBoard(created.id)
        assertTrue(deleted)

        val allRemaining = manager.listBoards()
        assertFalse(allRemaining.any { it.id == created.id }, "Удаленная доска не должна присутствовать в списке")
        assertTrue(allRemaining.any { it.id == duplicated.id }, "Дублированная доска должна остаться")
    }
}
