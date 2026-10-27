package com.lkboard.app.ui

import com.lkboard.app.data.AppPreferences
import com.lkboard.app.ui.components.HsvColor
import com.lkboard.app.ui.components.formatHex
import com.lkboard.app.ui.components.hsvToLongColor
import com.lkboard.app.ui.components.hsvToRgb
import com.lkboard.app.ui.components.longColorToHsv
import com.lkboard.app.ui.components.parseHexInput
import com.lkboard.app.ui.components.rgbToHsv
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HexColorPickerTest {

    @Test
    fun testParseHexInputStandard() {
        val parsed = parseHexInput("#FF5733")
        assertNotNull(parsed)
        assertEquals(0xFFFF5733L, parsed)
    }

    @Test
    fun testParseHexInputWithoutHash() {
        val parsed = parseHexInput("2563EB")
        assertNotNull(parsed)
        assertEquals(0xFF2563EBL, parsed)
    }

    @Test
    fun testParseHexInputShorthandThreeChars() {
        val parsed = parseHexInput("#F53")
        assertNotNull(parsed)
        assertEquals(0xFFFF5533L, parsed)
    }

    @Test
    fun testParseHexInputWithAlphaEightChars() {
        val parsed = parseHexInput("#80FF5733")
        assertNotNull(parsed)
        assertEquals(0x80FF5733L, parsed)
    }

    @Test
    fun testParseHexInputInvalid() {
        assertNull(parseHexInput("ZZZZZZ"))
        assertNull(parseHexInput("#12"))
        assertNull(parseHexInput("#12345"))
        assertNull(parseHexInput(""))
    }

    @Test
    fun testFormatHex() {
        assertEquals("#FF5733", formatHex(0xFFFF5733L, includeAlpha = false))
        assertEquals("#80FF5733", formatHex(0x80FF5733L, includeAlpha = true))
    }

    @Test
    fun testHsvRgbRoundtripPureRed() {
        val (h, s, v) = rgbToHsv(1f, 0f, 0f)
        assertEquals(0f, h, 0.01f)
        assertEquals(1f, s, 0.01f)
        assertEquals(1f, v, 0.01f)

        val (r, g, b) = hsvToRgb(h, s, v)
        assertEquals(1f, r, 0.01f)
        assertEquals(0f, g, 0.01f)
        assertEquals(0f, b, 0.01f)
    }

    @Test
    fun testHsvRgbRoundtripPureGreen() {
        val (h, s, v) = rgbToHsv(0f, 1f, 0f)
        assertEquals(120f, h, 0.01f)
        assertEquals(1f, s, 0.01f)
        assertEquals(1f, v, 0.01f)

        val (r, g, b) = hsvToRgb(h, s, v)
        assertEquals(0f, r, 0.01f)
        assertEquals(1f, g, 0.01f)
        assertEquals(0f, b, 0.01f)
    }

    @Test
    fun testHsvRgbRoundtripPureBlue() {
        val (h, s, v) = rgbToHsv(0f, 0f, 1f)
        assertEquals(240f, h, 0.01f)
        assertEquals(1f, s, 0.01f)
        assertEquals(1f, v, 0.01f)

        val (r, g, b) = hsvToRgb(h, s, v)
        assertEquals(0f, r, 0.01f)
        assertEquals(0f, g, 0.01f)
        assertEquals(1f, b, 0.01f)
    }

    @Test
    fun testLongColorHsvConversion() {
        val originalColor = 0xFF2563EBL
        val hsv = longColorToHsv(originalColor)
        val reconstructed = hsvToLongColor(hsv)

        // Допускаем минимальную погрешность округления (1 бит на канал)
        val origR = (originalColor shr 16) and 0xFF
        val origG = (originalColor shr 8) and 0xFF
        val origB = originalColor and 0xFF

        val recR = (reconstructed shr 16) and 0xFF
        val recG = (reconstructed shr 8) and 0xFF
        val recB = reconstructed and 0xFF

        assertTrue(abs(origR - recR) <= 1)
        assertTrue(abs(origG - recG) <= 1)
        assertTrue(abs(origB - recB) <= 1)
    }

    @Test
    fun testRecentColorsPersistence() {
        val testColor = 0xFFABCDEFL
        AppPreferences.saveRecentColor(testColor)
        val recents = AppPreferences.loadRecentColors()

        assertTrue(recents.isNotEmpty())
        assertEquals(testColor, recents.first())
        assertTrue(recents.size <= 8)
    }
}
