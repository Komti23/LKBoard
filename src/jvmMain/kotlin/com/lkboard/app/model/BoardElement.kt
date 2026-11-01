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
     * Инженерный и физический примитив (стенсил) с привязкой к сетке и живой физической моделью.
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
        val strokeWidth: Float = 2.5f,
        val params: Map<String, Float> = emptyMap()
    ) : BoardElement {
        override fun translate(deltaX: Float, deltaY: Float) = copy(x = x + deltaX, y = y + deltaY)
        override fun copyWithPosition(newX: Float, newY: Float) = copy(x = newX, y = newY)
        override fun copyWithSize(newWidth: Float, newHeight: Float) = copy(width = newWidth, height = newHeight)
        override fun withRotation(newRotation: Float) = copy(rotation = newRotation)

        fun effectiveParams(): Map<String, Float> {
            return if (params.isNotEmpty()) params else PhysicsStencilDefaults.defaultParams(stencilType)
        }
    }

    /**
     * Карточка экзаменационного задания ФИПИ (ОГЭ / ЕГЭ).
     * Включает условие, тему, номер задания, скрываемый спойлер с ответом и критериями проверки.
     */
    @Serializable
    @SerialName("fipi_task")
    data class FipiTask(
        override val id: String,
        override val x: Float,
        override val y: Float,
        override val width: Float = 480f,
        override val height: Float = 320f,
        override val zIndex: Int = 0,
        override val isLocked: Boolean = false,
        override val rotation: Float = 0f,
        val examType: String = "ЕГЭ (Профиль)",
        val subject: String = "Математика",
        val taskNumber: String = "№13",
        val topic: String = "Тригонометрические уравнения",
        val condition: String = "",
        val answer: String = "",
        val solutionCriteria: String = "",
        val isAnswerRevealed: Boolean = false,
        val primaryScore: Int = 0,
        val maxScore: Int = 2,
        val cardColorHex: Long = 0xFFFFFFFFL
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
    SPRING("Пружинный маятник", "Механика"),
    MATHEMATICAL_PENDULUM("Нитяной маятник", "Механика"),
    LEVER_BALANCE("Рычаг и правило моментов", "Механика"),

    // Термодинамика и МКТ
    THERMO_CYLINDER("Цилиндр с поршнем и газом", "Термодинамика"),
    THERMO_PV_CYCLE("Цикл в координатах p-V", "Термодинамика"),

    // Оптика
    OPTICAL_LENS("Собирающая линза", "Оптика"),
    OPTICAL_PRISM("Оптическая призма", "Оптика"),

    // Электрические цепи и Магнетизм
    CIRCUIT_RESISTOR("Резистор", "Электричество"),
    CIRCUIT_BATTERY("Источник тока (Батарея)", "Электричество"),
    CIRCUIT_CAPACITOR("Конденсатор", "Электричество"),
    CIRCUIT_INDUCTOR("Катушка индуктивности", "Электричество"),
    CIRCUIT_SWITCH("Ключ (выключатель)", "Электричество"),
    CIRCUIT_LAMP("Лампочка", "Электричество"),
    CIRCUIT_AMMETER("Амперметр (A)", "Электричество"),
    CIRCUIT_VOLTMETER("Вольтметр (V)", "Электричество"),
    CIRCUIT_RHEOSTAT("Реостат (R рег)", "Электричество"),
    MAGNET_FIELD("Полосовой магнит и поле B", "Магнетизм")
}

/**
 * Дефолтные физические параметры и генератор маркировки для интерактивных стенсилов.
 */
object PhysicsStencilDefaults {
    fun defaultParams(type: PhysicsStencilType): Map<String, Float> = when (type) {
        PhysicsStencilType.INCLINED_PLANE -> mapOf("m" to 2.0f, "angle" to 30f, "mu" to 0.15f)
        PhysicsStencilType.PULLEY_BLOCK -> mapOf("m1" to 3.0f, "m2" to 1.0f)
        PhysicsStencilType.SPRING -> mapOf("m" to 2.0f, "k" to 100f)
        PhysicsStencilType.MATHEMATICAL_PENDULUM -> mapOf("L" to 1.0f, "g" to 9.8f)
        PhysicsStencilType.LEVER_BALANCE -> mapOf("d1" to 40f, "d2" to 20f, "m1" to 2.0f)
        PhysicsStencilType.THERMO_CYLINDER -> mapOf("p" to 100f, "V" to 5.0f, "nu" to 1.0f)
        PhysicsStencilType.THERMO_PV_CYCLE -> mapOf("p1" to 100f, "p2" to 300f, "V1" to 2.0f, "V2" to 6.0f)
        PhysicsStencilType.CIRCUIT_RESISTOR -> mapOf("R" to 6.0f, "U" to 12.0f)
        PhysicsStencilType.CIRCUIT_BATTERY -> mapOf("U" to 12.0f)
        PhysicsStencilType.CIRCUIT_CAPACITOR -> mapOf("C" to 47.0f, "U" to 12.0f)
        PhysicsStencilType.CIRCUIT_INDUCTOR -> mapOf("L" to 10.0f)
        PhysicsStencilType.CIRCUIT_SWITCH -> emptyMap()
        PhysicsStencilType.CIRCUIT_LAMP -> mapOf("R" to 6.0f, "U" to 12.0f)
        PhysicsStencilType.CIRCUIT_AMMETER -> mapOf("I" to 2.4f, "Imax" to 5.0f)
        PhysicsStencilType.CIRCUIT_VOLTMETER -> mapOf("U" to 9.6f, "Umax" to 15.0f)
        PhysicsStencilType.CIRCUIT_RHEOSTAT -> mapOf("Rcur" to 20.0f, "Rmax" to 50.0f, "U" to 12.0f)
        PhysicsStencilType.MAGNET_FIELD -> mapOf("B" to 0.5f, "q" to 10.0f, "v" to 1000.0f, "I" to 3.0f)
        PhysicsStencilType.OPTICAL_LENS -> mapOf("F" to 20.0f, "d" to 30.0f)
        PhysicsStencilType.OPTICAL_PRISM -> mapOf("A" to 60.0f, "n" to 1.50f)
    }

    fun computeLabel(type: PhysicsStencilType, params: Map<String, Float>): String {
        return when (type) {
            PhysicsStencilType.INCLINED_PLANE -> {
                val m = params["m"] ?: 2.0f
                val angle = (params["angle"] ?: 30f).toInt()
                val mu = params["mu"] ?: 0.15f
                val rad = (angle * kotlin.math.PI / 180.0).toFloat()
                val g = 9.8f
                val fNet = maxOf(0f, m * g * kotlin.math.sin(rad) - mu * m * g * kotlin.math.cos(rad))
                val a = fNet / m
                "m=${"%.1f".format(m)}кг, α=${angle}°, a=${"%.1f".format(a)}м/с²"
            }
            PhysicsStencilType.PULLEY_BLOCK -> {
                val m1 = params["m1"] ?: 3.0f
                val m2 = params["m2"] ?: 1.0f
                val a = if (m1 + m2 > 0f) kotlin.math.abs(m1 - m2) * 9.8f / (m1 + m2) else 0f
                "m1=${"%.1f".format(m1)}кг, m2=${"%.1f".format(m2)}кг, a=${"%.1f".format(a)}м/с²"
            }
            PhysicsStencilType.SPRING -> {
                val m = params["m"] ?: 2.0f
                val k = params["k"] ?: 100f
                val t = if (k > 0f) 2f * kotlin.math.PI.toFloat() * kotlin.math.sqrt(m / k) else 0f
                "k=${k.toInt()}Н/м, m=${"%.1f".format(m)}кг, T=${"%.2f".format(t)}с"
            }
            PhysicsStencilType.MATHEMATICAL_PENDULUM -> {
                val l = params["L"] ?: 1.0f
                val g = params["g"] ?: 9.8f
                val t = if (g > 0f && l > 0f) 2f * kotlin.math.PI.toFloat() * kotlin.math.sqrt(l / g) else 0f
                val nu = if (t > 0f) 1f / t else 0f
                "L=${"%.2f".format(l)}м, T=${"%.2f".format(t)}с, ν=${"%.2f".format(nu)}Гц"
            }
            PhysicsStencilType.LEVER_BALANCE -> {
                val d1 = (params["d1"] ?: 40f).toInt()
                val d2 = (params["d2"] ?: 20f).toInt()
                val m1 = params["m1"] ?: 2.0f
                val m2 = if (d2 > 0) m1 * d1 / d2 else 0f
                "d1=${d1}см, d2=${d2}см, m2=${"%.1f".format(m2)}кг"
            }
            PhysicsStencilType.THERMO_CYLINDER -> {
                val p = (params["p"] ?: 100f).toInt()
                val v = params["V"] ?: 5.0f
                val nu = params["nu"] ?: 1.0f
                val tK = if (nu > 0f) (p * 1000f * (v * 1e-3f)) / (nu * 8.314f) else 0f
                "p=${p}кПа, V=${"%.1f".format(v)}л, T=${tK.toInt()}K"
            }
            PhysicsStencilType.THERMO_PV_CYCLE -> {
                val p1 = (params["p1"] ?: 100f).toInt()
                val p2 = (params["p2"] ?: 300f).toInt()
                val v1 = params["V1"] ?: 2.0f
                val v2 = params["V2"] ?: 6.0f
                val work = kotlin.math.abs(p2 - p1) * 1000f * (kotlin.math.abs(v2 - v1) * 1e-3f)
                "A(цикл)=${"%.1f".format(work)}Дж, p=${p1}..${p2}кПа"
            }
            PhysicsStencilType.CIRCUIT_RESISTOR -> {
                val r = params["R"] ?: 6.0f
                val u = params["U"] ?: 12.0f
                val i = if (r > 0f) u / r else 0f
                val p = u * i
                "R=${"%.1f".format(r)}Ом, I=${"%.2f".format(i)}А, P=${"%.1f".format(p)}Вт"
            }
            PhysicsStencilType.CIRCUIT_BATTERY -> {
                val u = (params["U"] ?: 12.0f).toInt()
                "U=${u}В"
            }
            PhysicsStencilType.CIRCUIT_CAPACITOR -> {
                val c = (params["C"] ?: 47.0f).toInt()
                val u = params["U"] ?: 12.0f
                val w = 0.5f * (c * 1e-6f) * u * u * 1000f
                "C=${c}мкФ, W=${"%.2f".format(w)}мДж"
            }
            PhysicsStencilType.CIRCUIT_INDUCTOR -> {
                val l = (params["L"] ?: 10.0f).toInt()
                "L=${l}мГн"
            }
            PhysicsStencilType.CIRCUIT_SWITCH -> "Ключ К1"
            PhysicsStencilType.CIRCUIT_LAMP -> {
                val r = params["R"] ?: 6.0f
                val u = params["U"] ?: 12.0f
                val i = if (r > 0f) u / r else 0f
                val p = u * i
                "Лампа ${"%.1f".format(p)}Вт (${u.toInt()}В)"
            }
            PhysicsStencilType.CIRCUIT_AMMETER -> {
                val i = params["I"] ?: 2.4f
                "Амперметр: I=${"%.2f".format(i)}А"
            }
            PhysicsStencilType.CIRCUIT_VOLTMETER -> {
                val u = params["U"] ?: 9.6f
                "Вольтметр: U=${"%.1f".format(u)}В"
            }
            PhysicsStencilType.CIRCUIT_RHEOSTAT -> {
                val rCur = (params["Rcur"] ?: 20.0f).toInt()
                val rMax = (params["Rmax"] ?: 50.0f).toInt()
                val u = params["U"] ?: 12.0f
                val i = if (rCur > 0) u / rCur else 0f
                "Реостат: R=${rCur}/${rMax}Ом, I=${"%.2f".format(i)}А"
            }
            PhysicsStencilType.MAGNET_FIELD -> {
                val b = params["B"] ?: 0.5f
                val i = params["I"] ?: 3.0f
                "B=${"%.2f".format(b)}Тл, I=${"%.1f".format(i)}А"
            }
            PhysicsStencilType.OPTICAL_LENS -> {
                val f = (params["F"] ?: 20.0f).toInt()
                val d = (params["d"] ?: 30.0f).toInt()
                val dP = if (f > 0) 100f / f else 0f
                val mag = if (d - f != 0 && d > 0) kotlin.math.abs((d.toFloat() * f / (d - f)) / d) else 1f
                "F=${f}см, D=${"%.1f".format(dP)}дптр, Г=${"%.1f".format(mag)}x"
            }
            PhysicsStencilType.OPTICAL_PRISM -> {
                val a = (params["A"] ?: 60.0f).toInt()
                val n = params["n"] ?: 1.50f
                "A=${a}°, n=${"%.2f".format(n)}"
            }
        }
    }
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

