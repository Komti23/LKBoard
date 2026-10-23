package com.lkboard.app.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import java.awt.Cursor
import kotlin.math.max
import kotlin.math.min

/**
 * 8 маркеров изменения размера (Bounding Box Handles).
 */
enum class ResizeHandle(val cursorType: Int) {
    TOP_LEFT(Cursor.NW_RESIZE_CURSOR),
    TOP_CENTER(Cursor.N_RESIZE_CURSOR),
    TOP_RIGHT(Cursor.NE_RESIZE_CURSOR),
    RIGHT_CENTER(Cursor.E_RESIZE_CURSOR),
    BOTTOM_RIGHT(Cursor.SE_RESIZE_CURSOR),
    BOTTOM_CENTER(Cursor.S_RESIZE_CURSOR),
    BOTTOM_LEFT(Cursor.SW_RESIZE_CURSOR),
    LEFT_CENTER(Cursor.W_RESIZE_CURSOR);

    fun getPosition(bounds: Rect): Offset {
        return when (this) {
            TOP_LEFT -> bounds.topLeft
            TOP_CENTER -> Offset(bounds.center.x, bounds.top)
            TOP_RIGHT -> bounds.topRight
            RIGHT_CENTER -> Offset(bounds.right, bounds.center.y)
            BOTTOM_RIGHT -> bounds.bottomRight
            BOTTOM_CENTER -> Offset(bounds.center.x, bounds.bottom)
            BOTTOM_LEFT -> bounds.bottomLeft
            LEFT_CENTER -> Offset(bounds.left, bounds.center.y)
        }
    }
}

object GeometryUtils {

    const val MIN_SIZE = 24f

    /**
     * Вычисляет общий охватывающий прямоугольник (Bounding Box) для списка элементов.
     */
    fun computeBounds(elements: Collection<BoardElement>): Rect? {
        if (elements.isEmpty()) return null
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY

        for (el in elements) {
            val b = el.bounds
            minX = min(minX, b.left)
            minY = min(minY, b.top)
            maxX = max(maxX, b.right)
            maxY = max(maxY, b.bottom)
        }

        if (minX == Float.POSITIVE_INFINITY) return null
        return Rect(minX, minY, maxX, maxY)
    }

    /**
     * Проверяет попадание точки курсора в один из маркеров изменения размера.
     * [handleRadiusWorld] — радиус маркера в мировых координатах.
     */
    fun hitTestHandle(
        worldPoint: Offset,
        bounds: Rect,
        handleRadiusWorld: Float
    ): ResizeHandle? {
        val radiusSquared = handleRadiusWorld * handleRadiusWorld
        for (handle in ResizeHandle.entries) {
            val pos = handle.getPosition(bounds)
            val dx = worldPoint.x - pos.x
            val dy = worldPoint.y - pos.y
            if (dx * dx + dy * dy <= radiusSquared) {
                return handle
            }
        }
        return null
    }

    /**
     * Проверяет попадание точки курсора в элемент доски (сверху вниз по z-index с учетом вращения).
     */
    fun hitTestElement(
        worldPoint: Offset,
        elements: List<BoardElement>
    ): BoardElement? {
        for (element in elements.reversed()) {
            if (element.isPointInside(worldPoint)) {
                return element
            }
        }
        return null
    }

    /**
     * Проверяет попадание точки курсора в верхний маркер вращения (Rotation Handle).
     */
    fun hitTestRotationHandle(
        worldPoint: Offset,
        bounds: Rect,
        rotation: Float,
        scale: Float
    ): Boolean {
        val stemLength = 26f / scale
        val handleRadius = 12f / scale
        val center = bounds.center

        // Обратный поворот точки клика на -rotation относительно центра
        val rad = -Math.toRadians(rotation.toDouble())
        val cos = Math.cos(rad)
        val sin = Math.sin(rad)
        val dx = worldPoint.x - center.x
        val dy = worldPoint.y - center.y
        val unrotatedX = (dx * cos - dy * sin).toFloat() + center.x
        val unrotatedY = (dx * sin + dy * cos).toFloat() + center.y

        val handleX = center.x
        val handleY = bounds.top - stemLength
        val distSq = (unrotatedX - handleX) * (unrotatedX - handleX) + (unrotatedY - handleY) * (unrotatedY - handleY)
        return distSq <= handleRadius * handleRadius
    }

    /**
     * Проверяет попадание ластика в элементы доски (через bounding box или точки штриха).
     * Возвращает список элементов, которых коснулся ластик.
     */
    fun hitTestEraser(
        worldPoint: Offset,
        elements: Collection<BoardElement>,
        eraserRadiusWorld: Float = 12f
    ): List<BoardElement> {
        val hit = mutableListOf<BoardElement>()
        for (element in elements) {
            val inflatedBounds = element.bounds.inflate(eraserRadiusWorld)
            if (!inflatedBounds.contains(worldPoint)) {
                continue
            }
            if (element is BoardElement.DrawingPath) {
                val effectiveRadius = eraserRadiusWorld + (element.strokeWidth / 2f)
                val effectiveRadiusSq = effectiveRadius * effectiveRadius
                var touched = false
                for (pt in element.points) {
                    val dx = pt.x - worldPoint.x
                    val dy = pt.y - worldPoint.y
                    if (dx * dx + dy * dy <= effectiveRadiusSq) {
                        touched = true
                        break
                    }
                }
                if (!touched && element.points.size >= 2) {
                    for (i in 0 until element.points.size - 1) {
                        val p1 = element.points[i].toOffset()
                        val p2 = element.points[i + 1].toOffset()
                        if (distanceToSegment(worldPoint, p1, p2) <= effectiveRadius) {
                            touched = true
                            break
                        }
                    }
                }
                if (touched || element.bounds.inflate(4f).contains(worldPoint)) {
                    hit.add(element)
                }
            } else {
                hit.add(element)
            }
        }
        return hit
    }

    private fun distanceToSegment(p: Offset, v: Offset, w: Offset): Float {
        val l2 = (w.x - v.x) * (w.x - v.x) + (w.y - v.y) * (w.y - v.y)
        if (l2 == 0f) return (p - v).getDistance()
        val t = (((p.x - v.x) * (w.x - v.x) + (p.y - v.y) * (w.y - v.y)) / l2).coerceIn(0f, 1f)
        val projection = Offset(v.x + t * (w.x - v.x), v.y + t * (w.y - v.y))
        return (p - projection).getDistance()
    }

    /**
     * Находит все элементы, пересекающиеся с прямоугольником рамки выделения [selectionBox].
     */
    fun findElementsInBox(
        selectionBox: Rect,
        elements: Collection<BoardElement>
    ): List<BoardElement> {
        val normalizedBox = Rect(
            left = min(selectionBox.left, selectionBox.right),
            top = min(selectionBox.top, selectionBox.bottom),
            right = max(selectionBox.left, selectionBox.right),
            bottom = max(selectionBox.top, selectionBox.bottom)
        )
        return elements.filter { element ->
            element.bounds.overlaps(normalizedBox)
        }
    }

    /**
     * Вычисляет новые границы элемента при перетаскивании маркера [handle].
     */
    fun calculateResizedBounds(
        initialBounds: Rect,
        handle: ResizeHandle,
        deltaWorld: Offset,
        keepAspectRatio: Boolean
    ): Rect {
        var left = initialBounds.left
        var top = initialBounds.top
        var right = initialBounds.right
        var bottom = initialBounds.bottom

        when (handle) {
            ResizeHandle.TOP_LEFT -> {
                left += deltaWorld.x
                top += deltaWorld.y
            }
            ResizeHandle.TOP_CENTER -> {
                top += deltaWorld.y
            }
            ResizeHandle.TOP_RIGHT -> {
                right += deltaWorld.x
                top += deltaWorld.y
            }
            ResizeHandle.RIGHT_CENTER -> {
                right += deltaWorld.x
            }
            ResizeHandle.BOTTOM_RIGHT -> {
                right += deltaWorld.x
                bottom += deltaWorld.y
            }
            ResizeHandle.BOTTOM_CENTER -> {
                bottom += deltaWorld.y
            }
            ResizeHandle.BOTTOM_LEFT -> {
                left += deltaWorld.x
                bottom += deltaWorld.y
            }
            ResizeHandle.LEFT_CENTER -> {
                left += deltaWorld.x
            }
        }

        // Ограничение минимального размера и предотвращение выворачивания
        if (right - left < MIN_SIZE) {
            when (handle) {
                ResizeHandle.TOP_LEFT, ResizeHandle.LEFT_CENTER, ResizeHandle.BOTTOM_LEFT ->
                    left = right - MIN_SIZE
                else -> right = left + MIN_SIZE
            }
        }

        if (bottom - top < MIN_SIZE) {
            when (handle) {
                ResizeHandle.TOP_LEFT, ResizeHandle.TOP_CENTER, ResizeHandle.TOP_RIGHT ->
                    top = bottom - MIN_SIZE
                else -> bottom = top + MIN_SIZE
            }
        }

        if (keepAspectRatio) {
            val initialAspectRatio = initialBounds.width / initialBounds.height
            val currentWidth = right - left
            val currentHeight = bottom - top

            if (currentWidth / currentHeight > initialAspectRatio) {
                val correctedWidth = currentHeight * initialAspectRatio
                right = left + correctedWidth
            } else {
                val correctedHeight = currentWidth / initialAspectRatio
                bottom = top + correctedHeight
            }
        }

        return Rect(left, top, right, bottom)
    }
}
