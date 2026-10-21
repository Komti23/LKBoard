package com.lkboard.app.model

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanvasTransformTest {

    @Test
    fun testScreenToWorldAndBack() {
        val transform = CanvasTransform(panX = 150f, panY = -80f, scale = 1.5f)
        val originalWorld = Offset(300f, 450f)

        val screen = transform.worldToScreen(originalWorld)
        val roundtripWorld = transform.screenToWorld(screen)

        assertEquals(originalWorld.x, roundtripWorld.x, 0.001f)
        assertEquals(originalWorld.y, roundtripWorld.y, 0.001f)
    }

    @Test
    fun testZoomInvarianceUnderCursor() {
        val initialTransform = CanvasTransform(panX = 200f, panY = 150f, scale = 1.0f)
        val cursorScreenPos = Offset(400f, 300f)

        // Мировая точка под курсором ДО зума
        val worldPointBefore = initialTransform.screenToWorld(cursorScreenPos)

        // Выполняем зум x1.5 относительно курсора
        val zoomedTransform = initialTransform.zoomAround(cursorScreenPos, 1.5f)

        // Мировая точка под курсором ПОСЛЕ зума
        val worldPointAfter = zoomedTransform.screenToWorld(cursorScreenPos)

        // Точка под курсором не должна сместиться ни на пиксель
        assertEquals(worldPointBefore.x, worldPointAfter.x, 0.001f)
        assertEquals(worldPointBefore.y, worldPointAfter.y, 0.001f)
    }

    @Test
    fun testScaleClamping() {
        val transform = CanvasTransform(scale = 1.0f)
        val cursor = Offset(100f, 100f)

        // Попытка зума сверх максимума
        val extremeZoomIn = transform.zoomAround(cursor, 100f)
        assertEquals(CanvasTransform.MAX_SCALE, extremeZoomIn.scale)

        // Попытка зума ниже минимума
        val extremeZoomOut = transform.zoomAround(cursor, 0.001f)
        assertEquals(CanvasTransform.MIN_SCALE, extremeZoomOut.scale)
    }

    @Test
    fun testSmoothPan1to1() {
        val initial = CanvasTransform(panX = 100.5f, panY = 200.25f, scale = 2.5f)
        val dragAmount = Offset(12.75f, -8.5f)

        // Физическое смещение мыши (Float) прибавляется строго 1:1 к panOffset без деления на scale:
        val panned = initial.panBy(dragAmount)
        assertEquals(113.25f, panned.panX, 0.0001f)
        assertEquals(191.75f, panned.panY, 0.0001f)
        assertEquals(2.5f, panned.scale)
    }
}
