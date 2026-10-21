package com.lkboard.app.history

import com.lkboard.app.model.BoardElement
import com.lkboard.app.model.BoardState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HistoryManagerTest {

    @Test
    fun testAddAndUndoRedoCommand() {
        val history = HistoryManager()
        var state = BoardState()

        val sticky = BoardElement.StickyNote(
            id = "note-1",
            x = 100f,
            y = 100f,
            text = "Тест истории"
        )

        assertFalse(history.canUndo)
        assertFalse(history.canRedo)

        // Добавляем элемент через команду
        state = history.execute(AddElementCommand(sticky), state)
        assertTrue(history.canUndo)
        assertFalse(history.canRedo)
        assertNotNull(state.elements["note-1"])

        // Отменяем
        state = history.undo(state)
        assertFalse(history.canUndo)
        assertTrue(history.canRedo)
        assertNull(state.elements["note-1"])

        // Повторяем
        state = history.redo(state)
        assertTrue(history.canUndo)
        assertFalse(history.canRedo)
        assertNotNull(state.elements["note-1"])
    }

    @Test
    fun testMoveElementsCommandUndoRedo() {
        val history = HistoryManager()
        val sticky = BoardElement.StickyNote(id = "note-1", x = 100f, y = 100f)
        var state = BoardState(elements = mapOf(sticky.id to sticky))

        val moveCmd = MoveElementsCommand(setOf("note-1"), deltaX = 50f, deltaY = -20f)
        state = history.execute(moveCmd, state)

        assertEquals(150f, state.elements["note-1"]!!.x)
        assertEquals(80f, state.elements["note-1"]!!.y)

        state = history.undo(state)
        assertEquals(100f, state.elements["note-1"]!!.x)
        assertEquals(100f, state.elements["note-1"]!!.y)

        state = history.redo(state)
        assertEquals(150f, state.elements["note-1"]!!.x)
        assertEquals(80f, state.elements["note-1"]!!.y)
    }

    @Test
    fun testMaxHistoryLimit() {
        val history = HistoryManager(maxHistorySize = 3)
        var state = BoardState()

        for (i in 1..5) {
            val el = BoardElement.Text(id = "text-$i", x = i * 10f, y = 0f)
            state = history.execute(AddElementCommand(el), state)
        }

        // В стеке может остаться не более 3 операций
        var undos = 0
        while (history.canUndo) {
            state = history.undo(state)
            undos++
        }
        assertEquals(3, undos)
    }

    @Test
    fun testAddDrawingPathWithoutAutoSelection() {
        val history = HistoryManager()
        val state = BoardState()

        val path = BoardElement.DrawingPath(
            id = "path-ink",
            x = 10f,
            y = 20f
        )

        val nextState = history.execute(AddElementCommand(path, selectAfterAdd = false), state)
        assertTrue(nextState.elements.containsKey("path-ink"))
        assertTrue(nextState.selectedElementIds.isEmpty(), "Drawing path must not be auto-selected")
    }
}
