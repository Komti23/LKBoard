package com.lkboard.app.data

import com.lkboard.app.model.GridType
import com.lkboard.app.model.PenSettings
import com.lkboard.app.model.ShapeSettings
import com.lkboard.app.model.ShapeType
import com.lkboard.app.model.StrokeStyle
import java.util.prefs.Preferences

/**
 * Синглтон настроек приложения Lkboard на базе java.util.prefs.Preferences.
 *
 * Сохраняет:
 * - Параметры пера: цвет, толщина, режим прямой линии, режим хайлайтера.
 * - Параметры сетки: DOTS, LINES, NONE.
 * - Параметры геометрических фигур: тип фигуры, цвет обводки, цвет заливки, толщина линии, стиль штриха (SOLID, DASHED, DOTTED).
 */
object AppPreferences {
    private const val KEY_PEN_COLOR = "pen_color_argb"
    private const val KEY_PEN_WIDTH = "pen_stroke_width"
    private const val KEY_PEN_STRAIGHT = "pen_is_straight_line"
    private const val KEY_PEN_HIGHLIGHTER = "pen_is_highlighter"
    private const val KEY_GRID_TYPE = "canvas_grid_type"
    private const val KEY_BACKGROUND_COLOR = "canvas_background_color"

    private const val KEY_SHAPE_TYPE = "shape_type"
    private const val KEY_SHAPE_STROKE_COLOR = "shape_stroke_color"
    private const val KEY_SHAPE_FILL_COLOR = "shape_fill_color"
    private const val KEY_SHAPE_STROKE_WIDTH = "shape_stroke_width"
    private const val KEY_SHAPE_STROKE_STYLE = "shape_stroke_style"
    private const val KEY_SHAPE_CORNER_RADIUS = "shape_corner_radius"

    private val prefs: Preferences by lazy {
        Preferences.userNodeForPackage(AppPreferences::class.java)
    }

    // --- Параметры пера ---

    fun loadPenSettings(): PenSettings {
        return try {
            val defaultSettings = PenSettings()
            val colorHex = prefs.getLong(KEY_PEN_COLOR, defaultSettings.colorHex)
            val strokeWidth = prefs.getFloat(KEY_PEN_WIDTH, defaultSettings.strokeWidth).coerceIn(1.0f, 32.0f)
            val isStraight = prefs.getBoolean(KEY_PEN_STRAIGHT, defaultSettings.isStraightLine)
            val isHighlighter = prefs.getBoolean(KEY_PEN_HIGHLIGHTER, defaultSettings.isHighlighter)
            PenSettings(
                colorHex = colorHex,
                strokeWidth = strokeWidth,
                isStraightLine = isStraight,
                isHighlighter = isHighlighter
            )
        } catch (_: Exception) {
            PenSettings()
        }
    }

    fun savePenSettings(settings: PenSettings) {
        try {
            prefs.putLong(KEY_PEN_COLOR, settings.colorHex)
            prefs.putFloat(KEY_PEN_WIDTH, settings.strokeWidth)
            prefs.putBoolean(KEY_PEN_STRAIGHT, settings.isStraightLine)
            prefs.putBoolean(KEY_PEN_HIGHLIGHTER, settings.isHighlighter)
            prefs.flush()
        } catch (_: Exception) {}
    }

    // --- Параметры сетки холста ---

    fun loadGridType(): GridType {
        return try {
            val name = prefs.get(KEY_GRID_TYPE, GridType.DOTS.name)
            GridType.valueOf(name)
        } catch (_: Exception) {
            GridType.DOTS
        }
    }

    fun saveGridType(gridType: GridType) {
        try {
            prefs.put(KEY_GRID_TYPE, gridType.name)
            prefs.flush()
        } catch (_: Exception) {}
    }

    // --- Цвет фона холста ---

    fun loadBackgroundColor(): Long {
        return try {
            prefs.getLong(KEY_BACKGROUND_COLOR, 0xFFFFFFFF)
        } catch (_: Exception) {
            0xFFFFFFFF
        }
    }

    fun saveBackgroundColor(colorHex: Long) {
        try {
            prefs.putLong(KEY_BACKGROUND_COLOR, colorHex)
            prefs.flush()
        } catch (_: Exception) {}
    }

    // --- Параметры геометрических фигур ---

    fun loadShapeSettings(): ShapeSettings {
        return try {
            val defaultSettings = ShapeSettings()
            val shapeTypeName = prefs.get(KEY_SHAPE_TYPE, defaultSettings.shapeType.name)
            val shapeType = try {
                ShapeType.valueOf(shapeTypeName)
            } catch (_: Exception) {
                defaultSettings.shapeType
            }

            val strokeColor = prefs.getLong(KEY_SHAPE_STROKE_COLOR, defaultSettings.strokeColorHex)
            val fillColorVal = prefs.getLong(KEY_SHAPE_FILL_COLOR, -1L)
            val fillColor = if (fillColorVal == -1L) null else fillColorVal

            val strokeWidth = prefs.getFloat(KEY_SHAPE_STROKE_WIDTH, defaultSettings.strokeWidth).coerceIn(1.0f, 24.0f)

            val strokeStyleName = prefs.get(KEY_SHAPE_STROKE_STYLE, defaultSettings.strokeStyle.name)
            val strokeStyle = try {
                StrokeStyle.valueOf(strokeStyleName)
            } catch (_: Exception) {
                defaultSettings.strokeStyle
            }

            val cornerRadius = prefs.getFloat(KEY_SHAPE_CORNER_RADIUS, defaultSettings.cornerRadius)

            ShapeSettings(
                shapeType = shapeType,
                strokeColorHex = strokeColor,
                fillColorHex = fillColor,
                strokeWidth = strokeWidth,
                strokeStyle = strokeStyle,
                cornerRadius = cornerRadius
            )
        } catch (_: Exception) {
            ShapeSettings()
        }
    }

    fun saveShapeSettings(settings: ShapeSettings) {
        try {
            prefs.put(KEY_SHAPE_TYPE, settings.shapeType.name)
            prefs.putLong(KEY_SHAPE_STROKE_COLOR, settings.strokeColorHex)
            prefs.putLong(KEY_SHAPE_FILL_COLOR, settings.fillColorHex ?: -1L)
            prefs.putFloat(KEY_SHAPE_STROKE_WIDTH, settings.strokeWidth)
            prefs.put(KEY_SHAPE_STROKE_STYLE, settings.strokeStyle.name)
            prefs.putFloat(KEY_SHAPE_CORNER_RADIUS, settings.cornerRadius)
            prefs.flush()
        } catch (_: Exception) {}
    }

    // --- Автосохранение всей доски и настроек ---

    fun getAppDataDir(): java.io.File {
        val os = System.getProperty("os.name", "").lowercase()
        val dir = when {
            os.contains("win") -> {
                val appData = System.getenv("APPDATA")
                if (!appData.isNullOrBlank()) java.io.File(appData, "LKBoard")
                else java.io.File(System.getProperty("user.home"), ".lkboard")
            }
            os.contains("mac") -> {
                java.io.File(System.getProperty("user.home"), "Library/Application Support/LKBoard")
            }
            else -> {
                java.io.File(System.getProperty("user.home"), ".config/lkboard")
            }
        }
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getAutoSaveFile(): java.io.File {
        return java.io.File(getAppDataDir(), "autosave_board.lkboard")
    }
}
