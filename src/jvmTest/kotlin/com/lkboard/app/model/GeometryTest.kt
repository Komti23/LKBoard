package com.lkboard.app.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GeometryTest {

    @Test
    fun testHitTestElement() {
        val note1 = BoardElement.StickyNote(id = "note-bottom", x = 100f, y = 100f, width = 100f, height = 100f, zIndex = 0)
        val note2 = BoardElement.StickyNote(id = "note-top", x = 150f, y = 150f, width = 100f, height = 100f, zIndex = 1)

        val elements = listOf(note1, note2)

        // Точка в пересечении — должен выбраться верхний элемент (note2)
        val hit = GeometryUtils.hitTestElement(Offset(180f, 180f), elements)
        assertNotNull(hit)
        assertEquals("note-top", hit.id)

        // Точка вне всех элементов
        val miss = GeometryUtils.hitTestElement(Offset(500f, 500f), elements)
        assertNull(miss)
    }

    @Test
    fun testHitTestHandle() {
        val bounds = Rect(100f, 100f, 300f, 300f)
        val handleRadiusWorld = 10f

        // Клик в правый нижний угол
        val hitHandle = GeometryUtils.hitTestHandle(Offset(302f, 298f), bounds, handleRadiusWorld)
        assertEquals(ResizeHandle.BOTTOM_RIGHT, hitHandle)

        // Клик в центр грани слева
        val leftCenterHandle = GeometryUtils.hitTestHandle(Offset(100f, 200f), bounds, handleRadiusWorld)
        assertEquals(ResizeHandle.LEFT_CENTER, leftCenterHandle)

        // Клик мимо всех маркеров
        val missHandle = GeometryUtils.hitTestHandle(Offset(200f, 200f), bounds, handleRadiusWorld)
        assertNull(missHandle)
    }

    @Test
    fun testFindElementsInBox() {
        val el1 = BoardElement.StickyNote(id = "el1", x = 50f, y = 50f, width = 50f, height = 50f)
        val el2 = BoardElement.StickyNote(id = "el2", x = 200f, y = 200f, width = 50f, height = 50f)
        val elements = listOf(el1, el2)

        val box = Rect(30f, 30f, 120f, 120f)
        val selected = GeometryUtils.findElementsInBox(box, elements)

        assertEquals(1, selected.size)
        assertEquals("el1", selected.first().id)
    }

    @Test
    fun testResizeCalculationMinSize() {
        val initial = Rect(100f, 100f, 200f, 200f)
        // Пытаемся уменьшить ширину до отрицательного значения перетаскиванием правого маркера налево на 500px
        val resized = GeometryUtils.calculateResizedBounds(
            initialBounds = initial,
            handle = ResizeHandle.RIGHT_CENTER,
            deltaWorld = Offset(-500f, 0f),
            keepAspectRatio = false
        )

        assertTrue(resized.width >= GeometryUtils.MIN_SIZE)
        assertEquals(100f, resized.left)
        assertEquals(100f + GeometryUtils.MIN_SIZE, resized.right)
    }

    @Test
    fun testHitTestEraser() {
        val note = BoardElement.StickyNote(id = "note-1", x = 100f, y = 100f, width = 100f, height = 100f)
        val stroke = BoardElement.DrawingPath(
            id = "stroke-1",
            points = listOf(CanvasPoint(300f, 300f), CanvasPoint(350f, 350f)),
            strokeWidth = 4f
        )
        val elements = listOf(note, stroke)

        // Попадание в рамку стикера
        val hitNote = GeometryUtils.hitTestEraser(Offset(120f, 120f), elements, eraserRadiusWorld = 10f)
        assertEquals(1, hitNote.size)
        assertEquals("note-1", hitNote.first().id)

        // Попадание в точку линии штриха
        val hitStroke = GeometryUtils.hitTestEraser(Offset(325f, 325f), elements, eraserRadiusWorld = 10f)
        assertEquals(1, hitStroke.size)
        assertEquals("stroke-1", hitStroke.first().id)

        // Промах мимо всех элементов
        val miss = GeometryUtils.hitTestEraser(Offset(0f, 0f), elements, eraserRadiusWorld = 10f)
        assertTrue(miss.isEmpty())
    }
}
