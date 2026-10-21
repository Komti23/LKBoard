package com.lkboard.app.ui.viewmodel

import com.lkboard.app.data.AppPreferences
import com.lkboard.app.history.BoardCommand
import com.lkboard.app.history.UpdateShapePropertiesCommand
import com.lkboard.app.model.BoardElement
import com.lkboard.app.model.BoardState
import com.lkboard.app.model.ShapeSettings
import com.lkboard.app.model.ShapeType
import com.lkboard.app.model.StrokeStyle

/**
 * Контроллер и модель управления выделением элементов на холсте Lkboard:
 * - Отслеживание текущего выделенного элемента / множества элементов.
 * - Определение активной выбранной фигуры (selectedShape).
 * - Обновление контекстных свойств выделенной фигуры в реальном времени
 *   (цвет обводки, цвет заливки, толщина, стиль штриха, тип фигуры, радиус).
 * - Запись изменений параметров в стек отмены Undo/Redo через Command Pattern.
 * - Синхронизация последних выбранных параметров с AppPreferences для новых фигур.
 */
class SelectionManager(
    private val getState: () -> BoardState,
    private val onExecuteCommand: (BoardCommand) -> Unit,
    private val onUpdateState: ((BoardState) -> BoardState) -> Unit
) {
    /**
     * Возвращает выделенную фигуру, если на холсте выделена ровно одна фигура (Shape).
     */
    val selectedShape: BoardElement.Shape?
        get() = getState().selectedShape

    /**
     * Проверяет, выделена ли в данный момент геометрическая фигура.
     */
    val hasSelectedShape: Boolean
        get() = selectedShape != null

    /**
     * Возвращает актуальные настройки для всплывающего меню фигур (ShapePropertiesPopup):
     * - Если выделена конкретная фигура: её текущие параметры.
     * - Если фигура не выделена: глобальные настройки новых фигур (из состояния или AppPreferences).
     */
    val activeShapeSettings: ShapeSettings
        get() {
            val shape = selectedShape
            return if (shape != null) {
                ShapeSettings(
                    shapeType = shape.shapeType,
                    strokeColorHex = shape.strokeColorHex,
                    fillColorHex = shape.fillColorHex,
                    strokeWidth = shape.strokeWidth,
                    strokeStyle = shape.strokeStyle,
                    cornerRadius = shape.cornerRadius
                )
            } else {
                getState().shapeSettings
            }
        }

    /**
     * Применяет новые параметры к выделенной фигуре (с записью в историю Undo/Redo)
     * либо сохраняет их как глобальные параметры для последующего создания фигур.
     * Также обновляет постоянное хранилище настроек AppPreferences.
     */
    fun updateShapeSettings(newSettings: ShapeSettings) {
        val currentShape = selectedShape
        if (currentShape != null) {
            val updatedShape = currentShape.copy(
                shapeType = newSettings.shapeType,
                strokeColorHex = newSettings.strokeColorHex,
                fillColorHex = newSettings.fillColorHex,
                strokeWidth = newSettings.strokeWidth,
                strokeStyle = newSettings.strokeStyle,
                cornerRadius = newSettings.cornerRadius
            )
            // Регистрируем команду в стеке Undo/Redo
            val command = UpdateShapePropertiesCommand(
                shapeId = currentShape.id,
                oldShape = currentShape,
                newShape = updatedShape
            )
            onExecuteCommand(command)
        }

        // Обновляем текущие настройки для будущих фигур и сохраняем в постоянное хранилище
        onUpdateState { it.withShapeSettings(newSettings) }
        AppPreferences.saveShapeSettings(newSettings)
    }

    /**
     * Изменение цвета контура (обводки).
     */
    fun updateStrokeColor(colorHex: Long) {
        val current = activeShapeSettings
        updateShapeSettings(current.copy(strokeColorHex = colorHex))
    }

    /**
     * Изменение цвета заливки (null = прозрачная заливка / без заливки).
     */
    fun updateFillColor(fillHex: Long?) {
        val current = activeShapeSettings
        updateShapeSettings(current.copy(fillColorHex = fillHex))
    }

    /**
     * Изменение толщины линии контура.
     */
    fun updateStrokeWidth(width: Float) {
        val current = activeShapeSettings
        updateShapeSettings(current.copy(strokeWidth = width))
    }

    /**
     * Изменение стиля линии (Solid / Dashed / Dotted).
     */
    fun updateStrokeStyle(style: StrokeStyle) {
        val current = activeShapeSettings
        updateShapeSettings(current.copy(strokeStyle = style))
    }

    /**
     * Изменение типа геометрической формы.
     */
    fun updateShapeType(type: ShapeType) {
        val current = activeShapeSettings
        updateShapeSettings(current.copy(shapeType = type))
    }
}
