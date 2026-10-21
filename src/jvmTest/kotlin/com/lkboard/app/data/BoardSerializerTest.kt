package com.lkboard.app.data

import com.lkboard.app.export.PngExporter
import com.lkboard.app.model.BoardElement
import com.lkboard.app.model.BoardState
import com.lkboard.app.model.CanvasPoint
import com.lkboard.app.model.ShapeType
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BoardSerializerTest {

    @Test
    fun testSerializationRoundtrip() {
        val note = BoardElement.StickyNote(
            id = "note-1",
            x = 120f,
            y = 140f,
            text = "Тестовая заметка для сохранения",
            colorHex = 0xFFFFD54F
        )

        val shape = BoardElement.Shape(
            id = "shape-1",
            x = 300f,
            y = 200f,
            shapeType = ShapeType.ROUNDED_RECTANGLE,
            strokeColorHex = 0xFF2563EB
        )

        val text = BoardElement.Text(
            id = "text-1",
            x = 50f,
            y = 80f,
            text = "Заголовок доски",
            fontSize = 24f,
            isBold = true
        )

        val path = BoardElement.DrawingPath(
            id = "path-1",
            points = listOf(CanvasPoint(10f, 10f), CanvasPoint(20f, 30f), CanvasPoint(40f, 50f)),
            strokeWidth = 3f
        )

        val originalState = BoardState(
            title = "Тестовый проект",
            gridType = com.lkboard.app.model.GridType.LINES,
            elements = mapOf(
                note.id to note,
                shape.id to shape,
                text.id to text,
                path.id to path
            )
        )

        val doc = BoardSerializer.stateToDocument(originalState)
        val json = BoardSerializer.serialize(doc)

        // Проверяем, что в JSON присутствуют полиморфные метки и сетка
        assertTrue(json.contains("sticky_note"))
        assertTrue(json.contains("shape"))
        assertTrue(json.contains("text"))
        assertTrue(json.contains("drawing_path"))
        assertTrue(json.contains("LINES"))

        // Десериализуем обратно
        val loadedDoc = BoardSerializer.deserialize(json)
        val loadedState = BoardSerializer.documentToState(loadedDoc)

        assertEquals(originalState.title, loadedState.title)
        assertEquals(com.lkboard.app.model.GridType.LINES, loadedState.gridType)
        assertEquals(4, loadedState.elements.size)
        assertEquals(note.text, (loadedState.elements["note-1"] as BoardElement.StickyNote).text)
        assertEquals(shape.shapeType, (loadedState.elements["shape-1"] as BoardElement.Shape).shapeType)
    }

    @Test
    fun testExportToPngFile() {
        val tempFile = File.createTempFile("lkboard-test", ".png")
        tempFile.deleteOnExit()

        val sampleNote = BoardElement.StickyNote(
            id = "test-note",
            x = 50f,
            y = 50f,
            width = 160f,
            height = 140f,
            text = "Skia Export Test"
        )
        val state = BoardState(elements = mapOf(sampleNote.id to sampleNote))

        val success = PngExporter.exportToPng(tempFile, state)
        assertTrue(success)
        assertTrue(tempFile.exists())
        assertTrue(tempFile.length() > 500) // Валидный PNG весит более нескольких сотен байт
    }

    @Test
    fun testAutoSaveFileRoundtripAndSettingsPersistence() {
        val tempFile = File.createTempFile("autosave-test", ".lkboard")
        tempFile.deleteOnExit()

        val pen = com.lkboard.app.model.PenSettings(
            colorHex = com.lkboard.app.model.PenSettings.COLOR_CYAN,
            strokeWidth = 12f,
            isHighlighter = true
        )
        val shapeSettings = com.lkboard.app.model.ShapeSettings(
            shapeType = ShapeType.DIAMOND,
            strokeColorHex = 0xFF059669L,
            fillColorHex = 0x26059669L,
            strokeWidth = 5f
        )
        val stroke = BoardElement.DrawingPath(
            id = "stroke-test",
            points = listOf(CanvasPoint(0f, 0f), CanvasPoint(100f, 100f)),
            strokeColorHex = pen.colorHex,
            strokeWidth = pen.strokeWidth
        )

        val state = BoardState(
            title = "Autosaved Board",
            elements = mapOf(stroke.id to stroke),
            penSettings = pen,
            shapeSettings = shapeSettings,
            gridType = com.lkboard.app.model.GridType.LINES
        )

        BoardSerializer.save(tempFile, state)
        val loaded = BoardSerializer.load(tempFile)

        assertEquals("Autosaved Board", loaded.title)
        assertEquals(1, loaded.elements.size)
        assertEquals(pen.colorHex, loaded.penSettings.colorHex)
        assertEquals(pen.strokeWidth, loaded.penSettings.strokeWidth)
        assertEquals(true, loaded.penSettings.isHighlighter)
        assertEquals(ShapeType.DIAMOND, loaded.shapeSettings.shapeType)
        assertEquals(0xFF059669L, loaded.shapeSettings.strokeColorHex)
        assertEquals(0x26059669L, loaded.shapeSettings.fillColorHex)
        assertEquals(com.lkboard.app.model.GridType.LINES, loaded.gridType)
    }
}
