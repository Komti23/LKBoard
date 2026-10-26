package com.lkboard.app.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Точка на холсте для сохранения путей векторного рисования.
 */
@Serializable
data class CanvasPoint(
    val x: Float,
    val y: Float
) {
    fun toOffset(): Offset = Offset(x, y)

    companion object {
        fun fromOffset(offset: Offset): CanvasPoint = CanvasPoint(offset.x, offset.y)
    }
}

/**
 * Тип геометрической фигуры.
 */
@Serializable
enum class ShapeType {
    // 2D Базовые
    RECTANGLE,
    ROUNDED_RECTANGLE,
    ELLIPSE,
    TRIANGLE,
    DIAMOND,
    PARALLELOGRAM,
    HEXAGON,
    STAR,

    // Стрелки и выноски
    ARROW,
    DOUBLE_ARROW,
    SPEECH_BUBBLE,
    CLOUD,

    // 3D / Объемные фигуры
    CUBE_3D,
    CYLINDER_3D,
    CONE_3D,
    PYRAMID_3D,
    SPHERE_3D,

    FRAME
}

/**
 * Базовый интерфейс всех сущностей, размещаемых на доске Lkboard.
 */
@Serializable
sealed interface BoardElement {
    val id: String
    val x: Float
    val y: Float
    val width: Float
    val height: Float
    val zIndex: Int
    val isLocked: Boolean
    val rotation: Float
        get() = 0f

    /**
     * Прямоугольник границ элемента в мировых координатах доски.
     */
    val bounds: Rect
        get() = Rect(x, y, x + width, y + height)

    /**
     * Создает копию элемента со смещением на (deltaX, deltaY).
     */
    fun translate(deltaX: Float, deltaY: Float): BoardElement

    /**
     * Создает копию элемента с новой позицией (x, y).
     */
    fun copyWithPosition(newX: Float, newY: Float): BoardElement

    /**
     * Создает копию элемента с новыми размерами (width, height).
     */
    fun copyWithSize(newWidth: Float, newHeight: Float): BoardElement

    /**
     * Создает копию элемента с новым углом поворота в градусах (0..360).
     */
    fun withRotation(newRotation: Float): BoardElement

    /**
     * Проверяет попадание точки курсора в элемент с учетом его поворота вокруг центра
     * и небольшого допуска (hit-padding 6px) для удобства выбора мышью.
     */
    fun isPointInside(point: Offset): Boolean {
        val pad = 6f
        val inflated = bounds.inflate(pad)
        if (rotation == 0f) return inflated.contains(point)
        val cx = x + width / 2f
        val cy = y + height / 2f
        val rad = -Math.toRadians(rotation.toDouble())
        val cos = Math.cos(rad)
        val sin = Math.sin(rad)
        val dx = point.x - cx
        val dy = point.y - cy
        val rx = (dx * cos - dy * sin).toFloat() + cx
        val ry = (dx * sin + dy * cos).toFloat() + cy
        return inflated.contains(Offset(rx, ry))
    }

    /**
     * Стикер (Sticky Note) с текстом и настраиваемым цветом.
     */
    @Serializable
    @SerialName("sticky_note")
    data class StickyNote(
        override val id: String,
        override val x: Float,
        override val y: Float,
        override val width: Float = 210f,
        override val height: Float = 210f,
        override val zIndex: Int = 0,
        override val isLocked: Boolean = false,
        override val rotation: Float = 0f,
        val text: String = "Заметка\n- [ ] Первая задача\n- [ ] Вторая задача",
        val colorHex: Long = 0xFFFEF08A, // Пастельный солнечный желтый
        val fontSize: Float = 16f
    ) : BoardElement {
        override fun translate(deltaX: Float, deltaY: Float) = copy(x = x + deltaX, y = y + deltaY)
        override fun copyWithPosition(newX: Float, newY: Float) = copy(x = newX, y = newY)
        override fun copyWithSize(newWidth: Float, newHeight: Float) = copy(width = newWidth, height = newHeight)
        override fun withRotation(newRotation: Float) = copy(rotation = newRotation)

        companion object {
            val PRESET_COLORS = listOf(
                0xFFFEF08AL, // Солнечный желтый
                0xFFBBF7D0L, // Мятный зеленый
                0xFFFED7AAL, // Коралловый / Персик
                0xFFBAE6FDL, // Небесно-голубой
                0xFFFBCFE8L, // Лавандовый / Розовый
                0xFFF8FAFCL  // Матовое стекло
            )
        }
    }

    /**
     * Геометрическая фигура (прямоугольник, скругленный прямоугольник, эллипс, стрелка, рамка).
     */
    @Serializable
    @SerialName("shape")
    data class Shape(
        override val id: String,
        override val x: Float,
        override val y: Float,
        override val width: Float = 160f,
        override val height: Float = 100f,
        override val zIndex: Int = 0,
        override val isLocked: Boolean = false,
        override val rotation: Float = 0f,
        val shapeType: ShapeType = ShapeType.ROUNDED_RECTANGLE,
        val strokeColorHex: Long = 0xFF1976D2,
        val fillColorHex: Long? = 0x221976D2,
        val strokeWidth: Float = 2f,
        val cornerRadius: Float = 12f,
        val isDashed: Boolean = false,
        val strokeStyle: StrokeStyle = StrokeStyle.SOLID
    ) : BoardElement {
        override fun translate(deltaX: Float, deltaY: Float) = copy(x = x + deltaX, y = y + deltaY)
        override fun copyWithPosition(newX: Float, newY: Float) = copy(x = newX, y = newY)
        override fun copyWithSize(newWidth: Float, newHeight: Float) = copy(width = newWidth, height = newHeight)
        override fun withRotation(newRotation: Float) = copy(rotation = newRotation)
    }

    /**
     * Текстовый блок со свободным позиционированием и прозрачным фоном.
     */
    @Serializable
    @SerialName("text")
    data class Text(
        override val id: String,
        override val x: Float,
        override val y: Float,
        override val width: Float = 240f,
        override val height: Float = 50f,
        override val zIndex: Int = 0,
        override val isLocked: Boolean = false,
        override val rotation: Float = 0f,
        val text: String = "",
        val colorHex: Long = 0xFF1E1E1E,
        val fontSize: Float = 19f,
        val isBold: Boolean = false,
        val isItalic: Boolean = false
    ) : BoardElement {
        override fun translate(deltaX: Float, deltaY: Float) = copy(x = x + deltaX, y = y + deltaY)
        override fun copyWithPosition(newX: Float, newY: Float) = copy(x = newX, y = newY)
        override fun copyWithSize(newWidth: Float, newHeight: Float): Text {
            val scale = if (height > 0f) newHeight / height else 1f
            val newFontSize = if (kotlin.math.abs(newHeight - height) > 0.5f) {
                (fontSize * scale).coerceIn(10f, 220f)
            } else {
                fontSize
            }
            return copy(width = newWidth, height = newHeight, fontSize = newFontSize)
        }
        override fun withRotation(newRotation: Float) = copy(rotation = newRotation)
    }

    /**
     * Линия свободного рисования (ручка / маркер / хайлайтер).
     */
    @Serializable
    @SerialName("drawing_path")
    data class DrawingPath(
        override val id: String,
        override val x: Float = 0f,
        override val y: Float = 0f,
        override val width: Float = 0f,
        override val height: Float = 0f,
        override val zIndex: Int = 0,
        override val isLocked: Boolean = false,
        override val rotation: Float = 0f,
        val points: List<CanvasPoint> = emptyList(),
        val strokeColorHex: Long = 0xFF212121,
        val strokeWidth: Float = 3f,
        val isHighlighter: Boolean = false
    ) : BoardElement {
        override val bounds: Rect
            get() {
                if (points.isEmpty()) return Rect(x, y, x + width, y + height)
                val minX = points.minOf { it.x }
                val minY = points.minOf { it.y }
                val maxX = points.maxOf { it.x }
                val maxY = points.maxOf { it.y }
                val pad = (strokeWidth / 2f).coerceAtLeast(2f)
                return Rect(minX - pad, minY - pad, maxX + pad, maxY + pad)
            }

        override fun translate(deltaX: Float, deltaY: Float): DrawingPath {
            val newPoints = points.map { CanvasPoint(it.x + deltaX, it.y + deltaY) }
            return copy(x = x + deltaX, y = y + deltaY, points = newPoints)
        }

        override fun copyWithPosition(newX: Float, newY: Float): DrawingPath {
            val currentBounds = bounds
            val dx = newX - currentBounds.left
            val dy = newY - currentBounds.top
            return translate(dx, dy)
        }

        override fun copyWithSize(newWidth: Float, newHeight: Float): DrawingPath {
            val currentBounds = bounds
            val curW = currentBounds.width.coerceAtLeast(1f)
            val curH = currentBounds.height.coerceAtLeast(1f)
            val scaleX = newWidth / curW
            val scaleY = newHeight / curH
            val originX = currentBounds.left
            val originY = currentBounds.top
            val newPoints = points.map {
                CanvasPoint(originX + (it.x - originX) * scaleX, originY + (it.y - originY) * scaleY)
            }
            return copy(width = newWidth, height = newHeight, points = newPoints)
        }

        override fun withRotation(newRotation: Float) = copy(rotation = newRotation)
    }

    /**
     * Растровое изображение (скриншот, вставка из буфера обмена).
     */
    @Serializable
    @SerialName("image")
    data class ImageElement(
        override val id: String,
        override val x: Float,
        override val y: Float,
        override val width: Float,
        override val height: Float,
        override val zIndex: Int = 0,
        override val isLocked: Boolean = false,
        override val rotation: Float = 0f,
        val imageBase64: String
    ) : BoardElement {
        override fun translate(deltaX: Float, deltaY: Float) = copy(x = x + deltaX, y = y + deltaY)
        override fun copyWithPosition(newX: Float, newY: Float) = copy(x = newX, y = newY)
        override fun copyWithSize(newWidth: Float, newHeight: Float) = copy(width = newWidth, height = newHeight)
        override fun withRotation(newRotation: Float) = copy(rotation = newRotation)
    }

    /**
     * Интерактивный фрейм / контейнер для презентаций (PowerPoint PPTX), PDF и слайдов.
     * Позволяет листать слайды, масштабировать и наносить рукописные пометки поверх контента.
     */
    @Serializable
    @SerialName("document_frame")
    data class DocumentFrame(
        override val id: String,
        override val x: Float,
        override val y: Float,
        override val width: Float = 800f,
        override val height: Float = 500f,
        override val zIndex: Int = 0,
        override val isLocked: Boolean = false,
        override val rotation: Float = 0f,
        val title: String = "Презентация / PDF",
        val pages: List<String> = emptyList(), // Base64 PNG-слайды
        val currentPageIndex: Int = 0,
        val backgroundColorHex: Long = 0xFFFFFFFF
    ) : BoardElement {
        override fun translate(deltaX: Float, deltaY: Float) = copy(x = x + deltaX, y = y + deltaY)
        override fun copyWithPosition(newX: Float, newY: Float) = copy(x = newX, y = newY)
        override fun copyWithSize(newWidth: Float, newHeight: Float) = copy(width = newWidth, height = newHeight)
        override fun withRotation(newRotation: Float) = copy(rotation = newRotation)

        val totalPages: Int get() = pages.size
        val currentPageBase64: String? get() = pages.getOrNull(currentPageIndex)
    }

    /**
     * Умный коннектор (Smart Connector) между фигурами с автоматической магнитной привязкой.
     */
    @Serializable
    @SerialName("connector")
    data class Connector(
        override val id: String,
        override val x: Float = 0f,
        override val y: Float = 0f,
        override val width: Float = 0f,
        override val height: Float = 0f,
        override val zIndex: Int = 0,
        override val isLocked: Boolean = false,
        val startElementId: String? = null,
        val startAnchor: AnchorPoint = AnchorPoint.RIGHT,
        val startPoint: CanvasPoint = CanvasPoint(0f, 0f),
        val endElementId: String? = null,
        val endAnchor: AnchorPoint = AnchorPoint.LEFT,
        val endPoint: CanvasPoint = CanvasPoint(120f, 0f),
        val style: ConnectorStyle = ConnectorStyle.ORTHOGONAL,
        val strokeColorHex: Long = 0xFF2563EB,
        val strokeWidth: Float = 2.5f,
        val hasStartArrow: Boolean = false,
        val hasEndArrow: Boolean = true,
        val label: String = ""
    ) : BoardElement {
        override val bounds: Rect
            get() {
                val minX = kotlin.math.min(startPoint.x, endPoint.x) - 12f
                val minY = kotlin.math.min(startPoint.y, endPoint.y) - 12f
                val maxX = kotlin.math.max(startPoint.x, endPoint.x) + 12f
                val maxY = kotlin.math.max(startPoint.y, endPoint.y) + 12f
                return Rect(minX, minY, maxX, maxY)
            }

        override fun translate(deltaX: Float, deltaY: Float) = copy(
            x = x + deltaX,
            y = y + deltaY,
            startPoint = CanvasPoint(startPoint.x + deltaX, startPoint.y + deltaY),
            endPoint = CanvasPoint(endPoint.x + deltaX, endPoint.y + deltaY)
        )

        override fun copyWithPosition(newX: Float, newY: Float): Connector {
            val dx = newX - bounds.left
            val dy = newY - bounds.top
            return translate(dx, dy)
        }

        override fun copyWithSize(newWidth: Float, newHeight: Float): Connector = this
        override fun withRotation(newRotation: Float): Connector = this
    }

    /**
     * Интерактивная карточка вехи / задачи дорожной карты (Roadmap Milestone Card).
     */
    @Serializable
    @SerialName("milestone_card")
    data class MilestoneCard(
        override val id: String,
        override val x: Float,
        override val y: Float,
        override val width: Float = 280f,
        override val height: Float = 160f,
        override val zIndex: Int = 0,
        override val isLocked: Boolean = false,
        override val rotation: Float = 0f,
        val title: String = "Новая веха",
        val description: String = "Описание этапа и ключевых результатов...",
        val status: MilestoneStatus = MilestoneStatus.IN_PROGRESS,
        val progress: Float = 0.5f,
        val dueDate: String = "",
        val tags: List<String> = listOf("Release"),
        val colorHex: Long = 0xFF2563EB
    ) : BoardElement {
        override fun translate(deltaX: Float, deltaY: Float) = copy(x = x + deltaX, y = y + deltaY)
        override fun copyWithPosition(newX: Float, newY: Float) = copy(x = newX, y = newY)
        override fun copyWithSize(newWidth: Float, newHeight: Float) = copy(width = newWidth, height = newHeight)
        override fun withRotation(newRotation: Float) = copy(rotation = newRotation)
    }

    /**
     * Интерактивный математический график функций (Desmos-стиль).
     */
    @Serializable
    @SerialName("function_graph")
    data class FunctionGraph(
        override val id: String,
        override val x: Float,
        override val y: Float,
        override val width: Float = 420f,
        override val height: Float = 320f,
        override val zIndex: Int = 0,
        override val isLocked: Boolean = false,
        override val rotation: Float = 0f,
        val title: String = "График функции",
        val expressions: List<String> = listOf("sin(x)"),
        val xMin: Float = -10f,
        val xMax: Float = 10f,
        val yMin: Float = -6f,
        val yMax: Float = 6f,
        val strokeColorsHex: List<Long> = listOf(0xFF2563EB, 0xFFDC2626, 0xFF16A34A, 0xFFEA580C)
    ) : BoardElement {
        override fun translate(deltaX: Float, deltaY: Float) = copy(x = x + deltaX, y = y + deltaY)
        override fun copyWithPosition(newX: Float, newY: Float) = copy(x = newX, y = newY)
        override fun copyWithSize(newWidth: Float, newHeight: Float) = copy(width = newWidth, height = newHeight)
        override fun withRotation(newRotation: Float) = copy(rotation = newRotation)
    }

    /**
     * Интерактивный канбан-фрейм (Kanban Board) со столбцами этапов.
     * Автоматически обновляет статус карточек вех (MilestoneCard) при перетаскивании между колонками.
     */
    @Serializable
    @SerialName("kanban_board")
    data class KanbanBoard(
        override val id: String,
        override val x: Float,
        override val y: Float,
        override val width: Float = 1000f,
        override val height: Float = 620f,
        override val zIndex: Int = 0,
        override val isLocked: Boolean = false,
        override val rotation: Float = 0f,
        val title: String = "Канбан-доска проекта",
        val columnTitles: List<String> = listOf("Бэклог", "В работе", "На ревью", "Готово")
    ) : BoardElement {
        override fun translate(deltaX: Float, deltaY: Float) = copy(x = x + deltaX, y = y + deltaY)
        override fun copyWithPosition(newX: Float, newY: Float) = copy(x = newX, y = newY)
        override fun copyWithSize(newWidth: Float, newHeight: Float) = copy(width = newWidth, height = newHeight)
        override fun withRotation(newRotation: Float) = copy(rotation = newRotation)

        fun getColumnIndex(worldX: Float): Int {
            val colW = width / 4f
            val relX = worldX - x
            return (relX / colW).toInt().coerceIn(0, 3)
        }

        fun getStatusForColumn(columnIndex: Int): MilestoneStatus {
            return when (columnIndex) {
                0 -> MilestoneStatus.BACKLOG
                1 -> MilestoneStatus.IN_PROGRESS
                2 -> MilestoneStatus.REVIEW
                else -> MilestoneStatus.DONE
            }
        }

        fun getColumnBounds(columnIndex: Int): Rect {
            val colW = width / 4f
            val left = x + columnIndex * colW
            return Rect(left, y, left + colW, y + height)
        }
    }

    /**
     * Математическая формула LaTeX с быстрым рендерингом на холсте.
     */
    @Serializable
    @SerialName("latex_formula")
    data class LatexFormula(
        override val id: String,
        override val x: Float,
        override val y: Float,
        override val width: Float = 280f,
        override val height: Float = 90f,
        override val zIndex: Int = 0,
        override val isLocked: Boolean = false,
        override val rotation: Float = 0f,
        val formula: String = "\\int_{0}^{\\infty} e^{-x^2} dx = \\frac{\\sqrt{\\pi}}{2}",
        val fontSize: Float = 22f,
        val colorHex: Long = 0xFF1E293B,
        val hasBackgroundCard: Boolean = true
    ) : BoardElement {
        override fun translate(deltaX: Float, deltaY: Float) = copy(x = x + deltaX, y = y + deltaY)
        override fun copyWithPosition(newX: Float, newY: Float) = copy(x = newX, y = newY)
        override fun copyWithSize(newWidth: Float, newHeight: Float): LatexFormula {
            val scale = if (height > 0f) newHeight / height else (if (width > 0f) newWidth / width else 1f)
            val newFontSize = (fontSize * scale).coerceIn(12f, 220f)
            return copy(width = newWidth, height = newHeight, fontSize = newFontSize)
        }
        override fun withRotation(newRotation: Float) = copy(rotation = newRotation)
    }

    /**
     * Инженерный и физический примитив (стенсил) с привязкой к сетке.
     */
    @Serializable
    @SerialName("physics_stencil")
    data class PhysicsStencil(
        override val id: String,
        override val x: Float,
        override val y: Float,
        override val width: Float = 180f,
        override val height: Float = 120f,
        override val zIndex: Int = 0,
        override val isLocked: Boolean = false,
        override val rotation: Float = 0f,
        val stencilType: PhysicsStencilType = PhysicsStencilType.INCLINED_PLANE,
        val label: String = "",
        val strokeColorHex: Long = 0xFF2563EB,
        val fillColorHex: Long? = 0x1A2563EB,
        val strokeWidth: Float = 2.5f
    ) : BoardElement {
        override fun translate(deltaX: Float, deltaY: Float) = copy(x = x + deltaX, y = y + deltaY)
        override fun copyWithPosition(newX: Float, newY: Float) = copy(x = newX, y = newY)
        override fun copyWithSize(newWidth: Float, newHeight: Float) = copy(width = newWidth, height = newHeight)
        override fun withRotation(newRotation: Float) = copy(rotation = newRotation)
    }
}

/**
 * Типы инженерных и физических примитивов (Stencils).
 */
@Serializable
enum class PhysicsStencilType(val displayName: String, val category: String) {
    // Механика
    INCLINED_PLANE("Наклонная плоскость", "Механика"),
    PULLEY_BLOCK("Блок с нитью и грузами", "Механика"),
    SPRING("Пружина с грузом", "Механика"),

    // Оптика
    OPTICAL_LENS("Собирающая линза", "Оптика"),

    // Электрические цепи
    CIRCUIT_RESISTOR("Резистор", "Электричество"),
    CIRCUIT_BATTERY("Источник тока (Батарея)", "Электричество"),
    CIRCUIT_CAPACITOR("Конденсатор", "Электричество"),
    CIRCUIT_INDUCTOR("Катушка индуктивности", "Электричество"),
    CIRCUIT_SWITCH("Ключ (выключатель)", "Электричество"),
    CIRCUIT_LAMP("Лампочка", "Электричество")
}

/**
 * Точка магнитной привязки коннектора к фигуре.
 */
@Serializable
enum class AnchorPoint {
    TOP,
    RIGHT,
    BOTTOM,
    LEFT,
    AUTO
}

/**
 * Стиль трассировки соединительной линии.
 */
@Serializable
enum class ConnectorStyle(val displayName: String) {
    STRAIGHT("Прямая линия"),
    ORTHOGONAL("Ступенчатая (под 90°)"),
    BEZIER("Плавная кривая")
}

/**
 * Статус задачи / вехи дорожной карты.
 */
@Serializable
enum class MilestoneStatus(val label: String, val icon: String, val colorHex: Long) {
    BACKLOG("Бэклог", "🔴", 0xFF64748B),
    IN_PROGRESS("В работе", "🟡", 0xFF2563EB),
    REVIEW("На ревью", "🟣", 0xFF9333EA),
    DONE("Готово", "🟢", 0xFF16A34A)
}

/**
 * Вычисляет мировые координаты магнитной точки на границе элемента.
 */
fun BoardElement.getAnchorOffset(anchor: AnchorPoint): Offset {
    val b = bounds
    return when (anchor) {
        AnchorPoint.TOP -> Offset(b.left + b.width / 2f, b.top)
        AnchorPoint.RIGHT -> Offset(b.right, b.top + b.height / 2f)
        AnchorPoint.BOTTOM -> Offset(b.left + b.width / 2f, b.bottom)
        AnchorPoint.LEFT -> Offset(b.left, b.top + b.height / 2f)
        AnchorPoint.AUTO -> Offset(b.left + b.width / 2f, b.top + b.height / 2f)
    }
}

typealias TextElement = BoardElement.Text
typealias ShapeElement = BoardElement.Shape

