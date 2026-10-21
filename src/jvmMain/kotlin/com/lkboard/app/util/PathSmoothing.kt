package com.lkboard.app.util

import androidx.compose.ui.graphics.Path
import com.lkboard.app.model.CanvasPoint
import kotlin.math.sqrt

/**
 * Утилита сглаживания штрихов рисования (Inking / Stylus smoothing).
 * Использует сплайн Midpoint Quadratic Bézier для устранения угловатости и острых углов.
 */
object PathSmoothing {

    /**
     * Строит абсолютно плавную кривую Безье (Midpoint Quadratic Bézier Spline)
     * с C1-гладкостью (непрерывностью касательных) для устранения любых острых углов и изломов.
     */
    fun buildSmoothPath(points: List<CanvasPoint>): Path {
        val path = Path()
        if (points.isEmpty()) return path

        val first = points.first()
        path.moveTo(first.x, first.y)

        if (points.size == 1) {
            return path
        }

        if (points.size == 2) {
            val second = points[1]
            path.lineTo(second.x, second.y)
            return path
        }

        // Соединяем P0 с первой промежуточной точкой
        val mid0X = (points[0].x + points[1].x) / 2f
        val mid0Y = (points[0].y + points[1].y) / 2f
        path.lineTo(mid0X, mid0Y)

        // Сегменты квадратичных кривых Безье через средние точки соседних отрезков
        for (i in 1 until points.size - 1) {
            val current = points[i]
            val next = points[i + 1]
            val nextMidX = (current.x + next.x) / 2f
            val nextMidY = (current.y + next.y) / 2f
            path.quadraticBezierTo(current.x, current.y, nextMidX, nextMidY)
        }

        // Финальный сегмент к последней точке
        val last = points.last()
        path.lineTo(last.x, last.y)

        return path
    }

    /**
     * Сглаживает последовательность точек, устраняя микро-дрожание стилуса/мыши.
     */
    fun smoothPoints(rawPoints: List<CanvasPoint>, minDistance: Float = 4f): List<CanvasPoint> {
        if (rawPoints.size <= 2) return rawPoints

        // 1. Фильтрация слишком близких точек
        val filtered = mutableListOf<CanvasPoint>()
        filtered.add(rawPoints.first())

        for (i in 1 until rawPoints.size - 1) {
            val prev = filtered.last()
            val curr = rawPoints[i]
            val dx = curr.x - prev.x
            val dy = curr.y - prev.y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist >= minDistance) {
                filtered.add(curr)
            }
        }
        filtered.add(rawPoints.last())

        if (filtered.size <= 2) return filtered

        // 2. Генерация плавных промежуточных точек через Midpoint Quadratic Bézier
        val smoothed = mutableListOf<CanvasPoint>()
        smoothed.add(filtered.first())

        for (i in 0 until filtered.size - 1) {
            val p0 = filtered[i]
            val p1 = filtered[i + 1]

            // Средняя точка между соседними узлами
            val midX = (p0.x + p1.x) / 2f
            val midY = (p0.y + p1.y) / 2f

            // Интерполяция 2 шагов между контрольными точками
            val t = 0.5f
            val qx = (1 - t) * (1 - t) * p0.x + 2 * (1 - t) * t * midX + t * t * p1.x
            val qy = (1 - t) * (1 - t) * p0.y + 2 * (1 - t) * t * midY + t * t * p1.y

            smoothed.add(CanvasPoint(qx, qy))
            smoothed.add(p1)
        }

        return smoothed
    }

    /**
     * Создает строго прямую линию от начальной точки до текущего положения курсора.
     */
    fun makeStraightLine(start: CanvasPoint, end: CanvasPoint): List<CanvasPoint> {
        return listOf(start, end)
    }
}
