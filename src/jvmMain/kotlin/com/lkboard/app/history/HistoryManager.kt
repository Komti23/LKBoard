package com.lkboard.app.history

import com.lkboard.app.model.BoardState
import java.util.ArrayDeque

/**
 * Менеджер истории операций (стек Undo / Redo).
 */
class HistoryManager(private val maxHistorySize: Int = 50) {

    private val undoStack = ArrayDeque<BoardCommand>()
    private val redoStack = ArrayDeque<BoardCommand>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    /**
     * Выполняет команду, применяет её к состоянию и добавляет в стек Undo.
     */
    fun execute(command: BoardCommand, currentState: BoardState): BoardState {
        val newState = command.execute(currentState)
        if (undoStack.size >= maxHistorySize) {
            undoStack.removeLast()
        }
        undoStack.push(command)
        redoStack.clear()
        return newState
    }

    /**
     * Отменяет последнюю операцию.
     */
    fun undo(currentState: BoardState): BoardState {
        if (undoStack.isEmpty()) return currentState
        val command = undoStack.pop()
        val revertedState = command.undo(currentState)
        redoStack.push(command)
        return revertedState
    }

    /**
     * Повторяет отмененную операцию.
     */
    fun redo(currentState: BoardState): BoardState {
        if (redoStack.isEmpty()) return currentState
        val command = redoStack.pop()
        val reappliedState = command.execute(currentState)
        undoStack.push(command)
        return reappliedState
    }

    /**
     * Очищает историю.
     */
    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
