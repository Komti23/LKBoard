package com.lkboard.app.ui.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.lkboard.app.model.CanvasTransform
import com.lkboard.app.model.GridType
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/**
 * Отрисовщик адаптивной координатной сетки и фона холста:
 * - DOTS: точечная сетка Miro с адаптивным шагом
 * - LINES: тетрадь в клетку для математики и записей от руки
 * - NONE: чистый белый лист без сетки
 * 
 * Оптимизация Skia:
 * 1. Вычисляет только видимый диапазон координат (Viewport Culling).
 * 2. Динамически адаптирует шаг сетки (от 20px до 80px на экране), предотвращая перегрузку GPU.
 * 3. Отмечает начало координат (World Origin 0,0) акцентным крестиком.
 */
object GridRenderer {

    private const val BASE_GRID_STEP = 40f
    private val DOT_COLOR = Color(0xFF94A3B8).copy(alpha = 0.45f)
    private val LINE_COLOR = Color(0xFFCBD5E1).copy(alpha = 0.65f)
    private val ORIGIN_COLOR = Color(0xFF6366F1).copy(alpha = 0.8f)

    /**
     * Вычисляет оптимальный шаг сетки в мировых координатах в зависимости от масштаба.
     */
    fun calculateAdaptiveStep(scale: Float, baseStep: Float = BASE_GRID_STEP): Float {
        var step = baseStep
        while (step * scale < 20f) {
            step *= 2f
        }
        while (step * scale > 80f) {
            step /= 2f
        }
        return step
    }

    /**
     * Отрисовывает адаптивную сетку [gridType] в пределах видимой области [size] с учетом цвета фона.
     */
    fun drawGrid(
        drawScope: DrawScope,
        transform: CanvasTransform,
        size: Size,
        gridType: GridType = GridType.DOTS,
        backgroundColor: Color = Color.White
    ) {
        if (gridType == GridType.NONE) return

        val isDark = (backgroundColor.red * 0.299f + backgroundColor.green * 0.587f + backgroundColor.blue * 0.114f) < 0.5f
        val dotColor = if (isDark) Color.White.copy(alpha = 0.22f) else DOT_COLOR
        val lineColor = if (isDark) Color.White.copy(alpha = 0.16f) else LINE_COLOR

        val scale = transform.scale
        val panX = transform.panX
        val panY = transform.panY

        val step = calculateAdaptiveStep(scale)

        // Границы видимой области в мировых координатах (Culling)
        val worldLeft = (0f - panX) / scale
        val worldRight = (size.width - panX) / scale
        val worldTop = (0f - panY) / scale
        val worldBottom = (size.height - panY) / scale

        val startX = (floor(worldLeft / step) * step)
        val endX = (ceil(worldRight / step) * step)
        val startY = (floor(worldTop / step) * step)
        val endY = (ceil(worldBottom / step) * step)

        when (gridType) {
            GridType.DOTS -> {
                val dotRadius = max(1.2f, 1.6f * (scale.coerceIn(0.5f, 2.0f)))
                var wx = startX
                while (wx <= endX) {
                    val sx = wx * scale + panX
                    var wy = startY
                    while (wy <= endY) {
                        val sy = wy * scale + panY
                        drawScope.drawCircle(
                            color = dotColor,
                            radius = dotRadius,
                            center = Offset(sx, sy)
                        )
                        wy += step
                    }
                    wx += step
                }
            }
            GridType.LINES -> {
                // Вертикальные линии клетки
                var wx = startX
                while (wx <= endX) {
                    val sx = wx * scale + panX
                    drawScope.drawLine(
                        color = lineColor,
                        start = Offset(sx, 0f),
                        end = Offset(sx, size.height),
                        strokeWidth = 1f
                    )
                    wx += step
                }

                // Горизонтальные линии клетки
                var wy = startY
                while (wy <= endY) {
                    val sy = wy * scale + panY
                    drawScope.drawLine(
                        color = lineColor,
                        start = Offset(0f, sy),
                        end = Offset(size.width, sy),
                        strokeWidth = 1f
                    )
                    wy += step
                }
            }
            GridType.NONE -> {}
        }

        // Отрисовка начала мировых координат (0, 0), если оно попадает в экран
        val originScreenX = 0f * scale + panX
        val originScreenY = 0f * scale + panY

        if (originScreenX in -20f..(size.width + 20f) && originScreenY in -20f..(size.height + 20f)) {
            val crossSize = 8f * scale.coerceIn(0.8f, 1.5f)
            drawScope.drawLine(
                color = ORIGIN_COLOR,
                start = Offset(originScreenX - crossSize, originScreenY),
                end = Offset(originScreenX + crossSize, originScreenY),
                strokeWidth = 2f
            )
            drawScope.drawLine(
                color = ORIGIN_COLOR,
                start = Offset(originScreenX, originScreenY - crossSize),
                end = Offset(originScreenX, originScreenY + crossSize),
                strokeWidth = 2f
            )
        }
    }
}
