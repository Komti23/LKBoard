package com.lkboard.app.history

import com.lkboard.app.model.BoardElement
import com.lkboard.app.model.BoardState

/**
 * Базовый интерфейс команды для стека отмены действий (Undo/Redo Command Pattern).
 */
interface BoardCommand {
    val description: String

    /**
     * Применяет команду к состоянию доски и возвращает новое состояние.
     */
    fun execute(state: BoardState): BoardState

    /**
     * Отменяет действие команды и возвращает предшествующее состояние.
     */
    fun undo(state: BoardState): BoardState
}

/**
 * Команда перемещения группы элементов.
 */
class MoveElementsCommand(
    val elementIds: Set<String>,
    val deltaX: Float,
    val deltaY: Float
) : BoardCommand {
    override val description: String = "Перемещение элементов"

    override fun execute(state: BoardState): BoardState {
        val updatedElements = state.elements.toMutableMap()
        for (id in elementIds) {
            val el = updatedElements[id] ?: continue
            updatedElements[id] = el.translate(deltaX, deltaY)
        }
        return state.copy(elements = updatedElements)
    }

    override fun undo(state: BoardState): BoardState {
        val updatedElements = state.elements.toMutableMap()
        for (id in elementIds) {
            val el = updatedElements[id] ?: continue
            updatedElements[id] = el.translate(-deltaX, -deltaY)
        }
        return state.copy(elements = updatedElements)
    }
}

/**
 * Команда изменения размера элемента.
 */
class ResizeElementCommand(
    val elementId: String,
    val oldX: Float,
    val oldY: Float,
    val oldWidth: Float,
    val oldHeight: Float,
    val newX: Float,
    val newY: Float,
    val newWidth: Float,
    val newHeight: Float
) : BoardCommand {
    override val description: String = "Изменение размера элемента"

    override fun execute(state: BoardState): BoardState {
        val el = state.elements[elementId] ?: return state
        val resized = el.copyWithPosition(newX, newY).copyWithSize(newWidth, newHeight)
        return state.withElement(resized)
    }

    override fun undo(state: BoardState): BoardState {
        val el = state.elements[elementId] ?: return state
        val reverted = el.copyWithPosition(oldX, oldY).copyWithSize(oldWidth, oldHeight)
        return state.withElement(reverted)
    }
}

/**
 * Команда добавления нового элемента на доску.
 */
class AddElementCommand(
    val element: BoardElement,
    val selectAfterAdd: Boolean = true
) : BoardCommand {
    override val description: String = "Добавление элемента"

    override fun execute(state: BoardState): BoardState {
        val next = state.withElement(element)
        return if (selectAfterAdd) next.withSelection(setOf(element.id)) else next.clearSelection()
    }

    override fun undo(state: BoardState): BoardState {
        return state.withoutElement(element.id)
    }
}

/**
 * Команда пакетного добавления нескольких элементов (например, шаблона дорожной карты).
 */
class AddElementsCommand(
    val newElements: List<BoardElement>,
    val selectAfterAdd: Boolean = true
) : BoardCommand {
    override val description: String = "Добавление группы элементов"

    override fun execute(state: BoardState): BoardState {
        var next = state
        for (el in newElements) {
            next = next.withElement(el)
        }
        return if (selectAfterAdd) next.withSelection(newElements.map { it.id }.toSet()) else next.clearSelection()
    }

    override fun undo(state: BoardState): BoardState {
        var next = state
        for (el in newElements) {
            next = next.withoutElement(el.id)
        }
        return next
    }
}

/**
 * Команда удаления группы элементов.
 */
class DeleteElementsCommand(
    val removedElements: List<BoardElement>
) : BoardCommand {
    override val description: String = "Удаление элементов"

    override fun execute(state: BoardState): BoardState {
        var next = state
        for (el in removedElements) {
            next = next.withoutElement(el.id)
        }
        return next
    }

    override fun undo(state: BoardState): BoardState {
        var next = state
        for (el in removedElements) {
            next = next.withElement(el)
        }
        return next.withSelection(removedElements.map { it.id }.toSet())
    }
}

/**
 * Команда обновления текста в элементе (стикер / текст).
 */
class UpdateTextCommand(
    val elementId: String,
    val oldText: String,
    val newText: String
) : BoardCommand {
    override val description: String = "Изменение текста"

    override fun execute(state: BoardState): BoardState {
        val el = state.elements[elementId] ?: return state
        val updated = when (el) {
            is BoardElement.StickyNote -> el.copy(text = newText)
            is BoardElement.Text -> el.copy(text = newText)
            else -> el
        }
        return state.withElement(updated)
    }

    override fun undo(state: BoardState): BoardState {
        val el = state.elements[elementId] ?: return state
        val reverted = when (el) {
            is BoardElement.StickyNote -> el.copy(text = oldText)
            is BoardElement.Text -> el.copy(text = oldText)
            else -> el
        }
        return state.withElement(reverted)
    }
}

/**
 * Команда дублирования выделенных элементов.
 */
class DuplicateElementsCommand(
    val duplicatedElements: List<BoardElement>
) : BoardCommand {
    override val description: String = "Дублирование элементов"

    override fun execute(state: BoardState): BoardState {
        var next = state
        for (el in duplicatedElements) {
            next = next.withElement(el)
        }
        return next.withSelection(duplicatedElements.map { it.id }.toSet())
    }

    override fun undo(state: BoardState): BoardState {
        var next = state
        for (el in duplicatedElements) {
            next = next.withoutElement(el.id)
        }
        return next
    }
}

/**
 * Команда изменения свойств геометрической фигуры (цвет обводки/заливки, толщина, пунктир, форма).
 */
class UpdateShapePropertiesCommand(
    private val shapeId: String,
    private val oldShape: BoardElement.Shape,
    private val newShape: BoardElement.Shape
) : BoardCommand {
    override val description: String = "Изменение свойств фигуры"

    override fun execute(state: BoardState): BoardState {
        return state.withElement(newShape)
    }

    override fun undo(state: BoardState): BoardState {
        return state.withElement(oldShape)
    }
}

/**
 * Команда вращения элементов (с поддержкой Undo/Redo).
 */
class RotateElementsCommand(
    val oldRotations: Map<String, Float>,
    val newRotations: Map<String, Float>
) : BoardCommand {
    override val description: String = "Вращение элементов"

    override fun execute(state: BoardState): BoardState {
        var updated = state
        for ((id, rot) in newRotations) {
            val el = updated.elements[id] ?: continue
            updated = updated.withElement(el.withRotation(rot))
        }
        return updated
    }

    override fun undo(state: BoardState): BoardState {
        var updated = state
        for ((id, rot) in oldRotations) {
            val el = updated.elements[id] ?: continue
            updated = updated.withElement(el.withRotation(rot))
        }
        return updated
    }
}

