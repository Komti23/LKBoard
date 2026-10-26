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

        // Пресеты масштаба для аккуратного и понятного округления процентов
        val PRESET_SCALES = floatArrayOf(
            0.10f, 0.25f, 0.33f, 0.50f, 0.67f, 0.75f, 0.90f,
            1.00f,
            1.10f, 1.25f, 1.50f, 1.75f, 2.00f, 2.50f, 3.00f, 4.00f
        )
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
     * Масштабирует холст к точному целевому масштабу [targetScaleRaw] относительно экранной точки [pivotScreen].
     * Включает магнитное притягивание к 100% при приближении на ±3.5%.
     */
    fun zoomTo(pivotScreen: Offset, targetScaleRaw: Float): CanvasTransform {
        val targetScale = when {
            kotlin.math.abs(targetScaleRaw - 1.0f) <= 0.035f -> 1.0f
            else -> targetScaleRaw
        }.coerceIn(MIN_SCALE, MAX_SCALE)

        if (targetScale == scale) return this

        val worldPivotX = (pivotScreen.x - panX) / scale
        val worldPivotY = (pivotScreen.y - panY) / scale

        val newPanX = pivotScreen.x - worldPivotX * targetScale
        val newPanY = pivotScreen.y - worldPivotY * targetScale

        return copy(
            panX = newPanX,
            panY = newPanY,
            scale = targetScale
        )
    }

    /**
     * Пошаговый зум вперед с аккуратным переходом по пресетам масштаба (25%, 50%, 75%, 100%, 125%...).
     */
    fun zoomInStep(pivotScreen: Offset): CanvasTransform {
        val nextScale = PRESET_SCALES.firstOrNull { it > scale + 0.02f } ?: (scale * 1.2f).coerceAtMost(MAX_SCALE)
        return zoomTo(pivotScreen, nextScale)
    }

    /**
     * Пошаговый зум назад с аккуратным переходом по пресетам масштаба.
     */
    fun zoomOutStep(pivotScreen: Offset): CanvasTransform {
        val prevScale = PRESET_SCALES.lastOrNull { it < scale - 0.02f } ?: (scale / 1.2f).coerceAtLeast(MIN_SCALE)
        return zoomTo(pivotScreen, prevScale)
    }

    /**
     * Масштабирует холст относительно экранной позиции курсора [pivotScreen].
     */
    fun zoomAround(pivotScreen: Offset, zoomMultiplier: Float): CanvasTransform {
        val rawScale = scale * zoomMultiplier
        val targetScale = if (kotlin.math.abs(rawScale - 1.0f) <= 0.035f) 1.0f else rawScale
        return zoomTo(pivotScreen, targetScale)
    }

    /**
     * Сброс масштаба строго к 100% (1.0f) с фокусом на центр экрана [viewportCenter].
     */
    fun resetZoom(viewportCenter: Offset): CanvasTransform {
        return zoomTo(viewportCenter, 1.0f)
    }
}
