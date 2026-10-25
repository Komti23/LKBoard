package com.lkboard.app.model

import kotlinx.serialization.Serializable

/**
 * Тип фона/координатной сетки холста Lkboard.
 */
@Serializable
enum class GridType {
    DOTS,      // Точки (по умолчанию)
    LINES,     // Клетка (как в тетради по математике)
    NONE       // Чистый белый лист
}

/**
 * Иммутабельное состояние сцены доски Lkboard.
 */
@Serializable
data class BoardState(
    val title: String = "Без названия",
    val elements: Map<String, BoardElement> = emptyMap(),
    val transform: CanvasTransform = CanvasTransform(),
    val selectedElementIds: Set<String> = emptySet(),
    val activeTool: CanvasTool = CanvasTool.SELECT,
    val penSettings: PenSettings = PenSettings(),
    val shapeSettings: ShapeSettings = ShapeSettings(),
    val gridType: GridType = GridType.DOTS,
    val backgroundColorHex: Long = 0xFFFFFFFF,
    val isSpacePressed: Boolean = false,
    val isPanning: Boolean = false,
    val enabledTools: Set<CanvasTool> = CanvasTool.entries.toSet()
) {
    fun withEnabledTools(tools: Set<CanvasTool>): BoardState =
        copy(enabledTools = tools)
    fun withGridType(type: GridType): BoardState =
        copy(gridType = type)

    fun withBackgroundColor(colorHex: Long): BoardState =
        copy(backgroundColorHex = colorHex)

    fun withPenSettings(settings: PenSettings): BoardState =
        copy(penSettings = settings)

    fun withShapeSettings(settings: ShapeSettings): BoardState =
        copy(shapeSettings = settings)

    /**
     * Элементы, отсортированные по zIndex для корректного послойного рендеринга.
     */
    val sortedElements: List<BoardElement>
        get() = elements.values.sortedBy { it.zIndex }

    /**
     * Множество выделенных элементов.
     */
    val selectedElements: List<BoardElement>
        get() = elements.values.filter { it.id in selectedElementIds }

    /**
     * Единственная выделенная фигура (если выделен ровно один элемент и это Shape).
     */
    val selectedShape: BoardElement.Shape?
        get() = if (selectedElementIds.size == 1) {
            elements[selectedElementIds.first()] as? BoardElement.Shape
        } else null

    /**
     * Единственный выделенный документ-фрейм (если выделен ровно 1 фрейм).
     */
    val selectedFrame: BoardElement.DocumentFrame?
        get() = if (selectedElementIds.size == 1) {
            elements[selectedElementIds.first()] as? BoardElement.DocumentFrame
        } else null

    fun withTransform(newTransform: CanvasTransform): BoardState =
        copy(transform = newTransform)

    fun withActiveTool(tool: CanvasTool): BoardState =
        copy(activeTool = tool)

    fun withSpacePressed(pressed: Boolean): BoardState =
        copy(isSpacePressed = pressed)

    fun withPanning(panning: Boolean): BoardState =
        copy(isPanning = panning)

    fun withElement(element: BoardElement): BoardState =
        copy(elements = elements + (element.id to element))

    fun withoutElement(id: String): BoardState =
        copy(
            elements = elements - id,
            selectedElementIds = selectedElementIds - id
        )

    fun withoutElements(ids: Set<String>): BoardState =
        copy(
            elements = elements - ids,
            selectedElementIds = selectedElementIds - ids
        )

    fun withSelection(ids: Set<String>): BoardState =
        copy(selectedElementIds = ids)

    fun clearSelection(): BoardState =
        copy(selectedElementIds = emptySet())
}
