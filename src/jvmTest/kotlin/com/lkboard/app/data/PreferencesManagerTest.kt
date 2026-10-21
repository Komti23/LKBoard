package com.lkboard.app.data

import com.lkboard.app.model.GridType
import com.lkboard.app.model.PenSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PreferencesManagerTest {

    @Test
    fun testSaveAndLoadPenSettings() {
        val customSettings = PenSettings(
            colorHex = 0xFFDC2626,
            strokeWidth = 14.5f,
            isStraightLine = true,
            isHighlighter = false
        )

        PreferencesManager.savePenSettings(customSettings)
        val loaded = PreferencesManager.loadPenSettings()

        assertEquals(customSettings.colorHex, loaded.colorHex)
        assertEquals(customSettings.strokeWidth, loaded.strokeWidth, 0.001f)
        assertEquals(customSettings.isStraightLine, loaded.isStraightLine)
        assertEquals(customSettings.isHighlighter, loaded.isHighlighter)
    }

    @Test
    fun testSaveAndLoadGridType() {
        PreferencesManager.saveGridType(GridType.LINES)
        val loadedLines = PreferencesManager.loadGridType()
        assertEquals(GridType.LINES, loadedLines)

        PreferencesManager.saveGridType(GridType.NONE)
        val loadedNone = PreferencesManager.loadGridType()
        assertEquals(GridType.NONE, loadedNone)

        PreferencesManager.saveGridType(GridType.DOTS)
        val loadedDots = PreferencesManager.loadGridType()
        assertEquals(GridType.DOTS, loadedDots)
    }
}
