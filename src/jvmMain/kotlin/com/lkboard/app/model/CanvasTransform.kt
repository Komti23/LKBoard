package com.lkboard.app.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlinx.serialization.Serializable

/**
 * Модель геометрической трансформации бесконечного холста.
 * 
 * Связь экранных координат (Screen, S) и мировых координат доски (World, W):
 * S = W * scale + pan
 * W = (S - pan) / scale
 *
 * @param panX горизонтальное смещение холста в экранных пикселях
 * @param panY вертикальное смещение холста в экранных пикселях
 * @param scale масштаб холста (1.0 = 100%)
 */
@Serializable
data class CanvasTransform(
    val panX: Float = 0f,
    val panY: Float = 0f,
    val scale: Float = 1.0f
) {
    companion object {
        const val MIN_SCALE = 0.10f // 10%
        const val MAX_SCALE = 4.00f // 400%
        const val ZOOM_IN_STEP = 1.15f
        const val ZOOM_OUT_STEP = 1f / ZOOM_IN_STEP
    }

    val pan: Offset get() = Offset(panX, panY)

    /**
     * Переводит точку из координат экрана (viewport) в мировые координаты доски.
     */
    fun screenToWorld(screenOffset: Offset): Offset {
        return Offset(
            x = (screenOffset.x - panX) / scale,
            y = (screenOffset.y - panY) / scale
        )
    }

    /**
     * Переводит точку из мировых координат доски в экранные координаты.
     */
    fun worldToScreen(worldOffset: Offset): Offset {
        return Offset(
            x = worldOffset.x * scale + panX,
            y = worldOffset.y * scale + panY
        )
    }

    /**
     * Переводит экранный прямоугольник в мировые координаты доски.
     */
    fun screenToWorld(rect: Rect): Rect {
        val topLeft = screenToWorld(rect.topLeft)
        val bottomRight = screenToWorld(rect.bottomRight)
        return Rect(topLeft, bottomRight)
    }

    /**
     * Переводит мировой прямоугольник доски в экранные координаты.
     */
    fun worldToScreen(rect: Rect): Rect {
        val topLeft = worldToScreen(rect.topLeft)
        val bottomRight = worldToScreen(rect.bottomRight)
        return Rect(topLeft, bottomRight)
    }

    /**
     * Смещает холст на дельту [delta] в экранных пикселях (панорамирование).
     */
    fun panBy(delta: Offset): CanvasTransform {
        return copy(
            panX = panX + delta.x,
            panY = panY + delta.y
        )
    }

    /**
     * Масштабирует холст относительно экранной позиции курсора [pivotScreen].
     * Мировая точка, находившаяся под курсором до изменения масштаба,
     * гарантированно остаётся ровно под курсором после зума.
     */
    fun zoomAround(pivotScreen: Offset, zoomMultiplier: Float): CanvasTransform {
        val targetScale = (scale * zoomMultiplier).coerceIn(MIN_SCALE, MAX_SCALE)
        if (targetScale == scale) return this

        // Вычисляем мировую точку под курсором до масштабирования:
        val worldPivotX = (pivotScreen.x - panX) / scale
        val worldPivotY = (pivotScreen.y - panY) / scale

        // Вычисляем новое смещение панорамирования, чтобы точка осталась на том же экранном пикселе:
        val newPanX = pivotScreen.x - worldPivotX * targetScale
        val newPanY = pivotScreen.y - worldPivotY * targetScale

        return copy(
            panX = newPanX,
            panY = newPanY,
            scale = targetScale
        )
    }

    /**
     * Сброс масштаба к 100% с фокусом на центр экрана [viewportCenter].
     */
    fun resetZoom(viewportCenter: Offset): CanvasTransform {
        return zoomAround(viewportCenter, 1.0f / scale)
    }
}
