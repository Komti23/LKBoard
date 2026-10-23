package com.lkboard.app.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.lkboard.app.model.GeometryUtils
import com.lkboard.app.model.ResizeHandle
import kotlin.math.max
import kotlin.math.min

/**
 * Отрисовщик рамки выделения (Bounding Box), маркеров изменения размера и прямоугольника группового выделения (Marquee Box).
 */
object SelectionRenderer {

    private val ACCENT_COLOR = Color(0xFF2563EB) // Royal Blue
    private val SELECTION_FILL = Color(0x1A2563EB) // 10% прозрачности

    /**
     * Отрисовывает рамку выделения, 8 маркеров ресайза и верхний маркер вращения вокруг выбранной области [bounds].
     * Поддерживает визуальное вращение рамки при [rotation] != 0.
     * [scale] используется для сохранения фиксированного экранного размера маркеров (~9px).
     */
    fun drawBoundingBox(
        drawScope: DrawScope,
        bounds: Rect,
        scale: Float,
        isMultiSelection: Boolean = false,
        rotation: Float = 0f
    ) {
        val drawContent = {
            val strokeWidth = 1.5f / scale
            val handleRadius = 4.5f / scale
            val rotHandleRadius = 5.5f / scale
            val stemLength = 26f / scale

            // Внешняя рамка
            drawScope.drawRect(
                color = ACCENT_COLOR,
                topLeft = bounds.topLeft,
                size = bounds.size,
                style = Stroke(width = strokeWidth)
            )

            // 8 интерактивных маркеров ресайза (Handles)
            for (handle in ResizeHandle.entries) {
                val center = handle.getPosition(bounds)

                // Белая подложка маркера
                drawScope.drawCircle(
                    color = Color.White,
                    radius = handleRadius,
                    center = center,
                    style = Fill
                )

                // Акцентная окантовка маркера
                drawScope.drawCircle(
                    color = ACCENT_COLOR,
                    radius = handleRadius,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )
            }

            // Ножка и маркер вращения (Rotation Handle)
            val topCenter = Offset(bounds.center.x, bounds.top)
            val rotHandleCenter = Offset(bounds.center.x, bounds.top - stemLength)

            // Линия соединителя маркера вращения
            drawScope.drawLine(
                color = ACCENT_COLOR,
                start = topCenter,
                end = rotHandleCenter,
                strokeWidth = strokeWidth
            )

            // Белая подложка маркера вращения
            drawScope.drawCircle(
                color = Color.White,
                radius = rotHandleRadius,
                center = rotHandleCenter,
                style = Fill
            )

            // Акцентная окантовка маркера вращения
            drawScope.drawCircle(
                color = ACCENT_COLOR,
                radius = rotHandleRadius,
                center = rotHandleCenter,
                style = Stroke(width = strokeWidth * 1.3f)
            )

            // Внутреннее функциональное ядро маркера вращения
            drawScope.drawCircle(
                color = ACCENT_COLOR,
                radius = rotHandleRadius * 0.45f,
                center = rotHandleCenter,
                style = Fill
            )
        }

        if (rotation != 0f) {
            drawScope.rotate(degrees = rotation, pivot = bounds.center) {
                drawContent()
            }
        } else {
            drawContent()
        }
    }

    /**
     * Отрисовывает динамическую рамку множественного выделения (Marquee Selection Box).
     */
    fun drawSelectionMarquee(
        drawScope: DrawScope,
        startWorld: Offset,
        currentWorld: Offset,
        scale: Float
    ) {
        val left = min(startWorld.x, currentWorld.x)
        val top = min(startWorld.y, currentWorld.y)
        val width = max(startWorld.x, currentWorld.x) - left
        val height = max(startWorld.y, currentWorld.y) - top

        val strokeWidth = 1.5f / scale
        val rectTopLeft = Offset(left, top)
        val rectSize = Size(width, height)

        // Полупрозрачная заливка области выделения
        drawScope.drawRect(
            color = SELECTION_FILL,
            topLeft = rectTopLeft,
            size = rectSize,
            style = Fill
        )

        // Граница рамки
        drawScope.drawRect(
            color = ACCENT_COLOR,
            topLeft = rectTopLeft,
            size = rectSize,
            style = Stroke(width = strokeWidth)
        )
    }
}
