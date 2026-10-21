package com.lkboard.app.util

import com.lkboard.app.model.CanvasPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PathSmoothingTest {

    @Test
    fun testMakeStraightLine() {
        val start = CanvasPoint(10f, 10f)
        val end = CanvasPoint(200f, 300f)

        val line = PathSmoothing.makeStraightLine(start, end)
        assertEquals(2, line.size)
        assertEquals(start, line.first())
        assertEquals(end, line.last())
    }

    @Test
    fun testSmoothPointsPreservesEndpoints() {
        val raw = listOf(
            CanvasPoint(0f, 0f),
            CanvasPoint(20f, 40f),
            CanvasPoint(50f, 60f),
            CanvasPoint(80f, 90f),
            CanvasPoint(120f, 150f)
        )

        val smoothed = PathSmoothing.smoothPoints(raw)
        assertTrue(smoothed.size >= raw.size)
        assertEquals(raw.first().x, smoothed.first().x, 0.001f)
        assertEquals(raw.first().y, smoothed.first().y, 0.001f)
        assertEquals(raw.last().x, smoothed.last().x, 0.001f)
        assertEquals(raw.last().y, smoothed.last().y, 0.001f)
    }

    @Test
    fun testSmallPointsListNotModified() {
        val raw = listOf(CanvasPoint(0f, 0f), CanvasPoint(10f, 10f))
        val result = PathSmoothing.smoothPoints(raw)
        assertEquals(2, result.size)

        val singlePoint = listOf(CanvasPoint(5f, 5f))
        val singleResult = PathSmoothing.smoothPoints(singlePoint)
        assertEquals(1, singleResult.size)
    }

    @Test
    fun testBuildSmoothPathSingleAndMultiplePoints() {
        val emptyPath = PathSmoothing.buildSmoothPath(emptyList())
        assertTrue(emptyPath.isEmpty)

        val singlePoint = listOf(CanvasPoint(10f, 10f))
        val singlePath = PathSmoothing.buildSmoothPath(singlePoint)
        // Single point path has moveTo but no drawing operations (drawn via drawCircle in renderer)
        assertTrue(!singlePath.isEmpty)

        val multiPoints = listOf(
            CanvasPoint(0f, 0f),
            CanvasPoint(10f, 20f),
            CanvasPoint(20f, 10f),
            CanvasPoint(30f, 40f)
        )
        val smoothPath = PathSmoothing.buildSmoothPath(multiPoints)
        assertTrue(!smoothPath.isEmpty)
    }
}
