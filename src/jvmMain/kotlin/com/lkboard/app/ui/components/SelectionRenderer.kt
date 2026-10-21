package com.lkboard.app.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
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
     * Отрисовывает рамку выделения и 8 маркеров ресайза вокруг выбранной области [bounds].
     * [scale] используется для сохранения фиксированного экранного размера маркеров (~9px).
     */
    fun drawBoundingBox(
        drawScope: DrawScope,
        bounds: Rect,
        scale: Float,
        isMultiSelection: Boolean = false
    ) {
        val strokeWidth = 1.5f / scale
        val handleRadius = 4.5f / scale

        // Внешняя рамка
        drawScope.drawRect(
            color = ACCENT_COLOR,
            topLeft = bounds.topLeft,
            size = bounds.size,
            style = Stroke(width = strokeWidth)
        )

        // 8 интерактивных маркеров (Handles)
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
