package com.lkboard.app.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/**
 * Направляющая линия выравнивания (Smart Alignment Guide как в Figma / Sketch).
 */
data class AlignmentGuide(
    val isVertical: Boolean,
    val coordinate: Float,
    val start: Float,
    val end: Float
)

/**
 * Результат магнитного примагничивания с набором активных направляющих.
 */
data class SnapResult(
    val snappedDelta: Offset,
    val guides: List<AlignmentGuide>
)

/**
 * Сервис вычисления умных направляющих и примагничивания при перемещении элементов.
 */
object SmartGuides {

    private const val SNAP_THRESHOLD_WORLD = 6f

    fun calculateSnap(
        dragBounds: Rect,
        otherElements: Collection<BoardElement>,
        delta: Offset
    ): SnapResult {
        var snappedDx = delta.x
        var snappedDy = delta.y
        val guides = mutableListOf<AlignmentGuide>()

        val candidateBounds = Rect(
            left = dragBounds.left + delta.x,
            top = dragBounds.top + delta.y,
            right = dragBounds.right + delta.x,
            bottom = dragBounds.bottom + delta.y
        )

        var minDiffX = SNAP_THRESHOLD_WORLD
        var minDiffY = SNAP_THRESHOLD_WORLD

        for (other in otherElements) {
            val b = other.bounds

            // 1. Проверка вертикального выравнивания (по оси X: левый край, центр, правый край)
            val xPairs = listOf(
                candidateBounds.left to b.left,
                candidateBounds.left to b.right,
                candidateBounds.right to b.left,
                candidateBounds.right to b.right,
                candidateBounds.center.x to b.center.x
            )
            for ((currX, targetX) in xPairs) {
                val diff = kotlin.math.abs(currX - targetX)
                if (diff < minDiffX) {
                    minDiffX = diff
                    snappedDx = delta.x + (targetX - currX)
                    val y1 = kotlin.math.min(candidateBounds.top, b.top) - 15f
                    val y2 = kotlin.math.max(candidateBounds.bottom, b.bottom) + 15f
                    guides.removeAll { it.isVertical }
                    guides.add(AlignmentGuide(isVertical = true, coordinate = targetX, start = y1, end = y2))
                }
            }

            // 2. Проверка горизонтального выравнивания (по оси Y: верх, центр, низ)
            val yPairs = listOf(
                candidateBounds.top to b.top,
                candidateBounds.top to b.bottom,
                candidateBounds.bottom to b.top,
                candidateBounds.bottom to b.bottom,
                candidateBounds.center.y to b.center.y
            )
            for ((currY, targetY) in yPairs) {
                val diff = kotlin.math.abs(currY - targetY)
                if (diff < minDiffY) {
                    minDiffY = diff
                    snappedDy = delta.y + (targetY - currY)
                    val x1 = kotlin.math.min(candidateBounds.left, b.left) - 15f
                    val x2 = kotlin.math.max(candidateBounds.right, b.right) + 15f
                    guides.removeAll { !it.isVertical }
                    guides.add(AlignmentGuide(isVertical = false, coordinate = targetY, start = x1, end = x2))
                }
            }
        }

        return SnapResult(Offset(snappedDx, snappedDy), guides)
    }
}
