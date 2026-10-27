package com.lkboard.app.data

import com.lkboard.app.model.CanvasTool
import com.lkboard.app.model.GridType
import com.lkboard.app.model.PenSettings
import com.lkboard.app.model.ShapeSettings
import com.lkboard.app.model.ShapeType
import com.lkboard.app.model.StrokeStyle
import java.util.prefs.Preferences

/**
 * Детектор системной темы Windows (Dark / Light Mode).
 */
object SystemThemeDetector {
    private var cachedIsDark: Boolean? = null
    private var lastCheckTime = 0L

    fun isSystemDark(): Boolean {
        val now = System.currentTimeMillis()
        if (cachedIsDark != null && now - lastCheckTime < 4000L) {
            return cachedIsDark!!
        }
        val isDark = try {
            val process = ProcessBuilder("reg", "query", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize", "/v", "AppsUseLightTheme")
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            if (output.contains("0x0")) true else if (output.contains("0x1")) false else true
        } catch (_: Exception) {
            true
        }
        cachedIsDark = isDark
        lastCheckTime = now
        return isDark
    }
}

/**
 * Тема оформления приложения Lkboard.
 */
enum class AppTheme(
    val displayName: String,
    private val rawIsDark: Boolean,
    private val rawBgColor: Long,
    val defaultGridType: GridType
) {
    SYSTEM("Как на компьютере (Авто)", true, 0xFF0D0F12, GridType.DOTS),
    DARK("Тёмная (Industrial Dark)", true, 0xFF0D0F12, GridType.DOTS),
    LIGHT("Светлая (Чистая бумага)", false, 0xFFFFFFFF, GridType.DOTS),
    NOTEBOOK("Тетрадь в клетку", false, 0xFFF8FAFC, GridType.LINES);

    val isDark: Boolean
        get() = if (this == SYSTEM) SystemThemeDetector.isSystemDark() else rawIsDark

    val defaultBgColor: Long
        get() = if (this == SYSTEM) (if (SystemThemeDetector.isSystemDark()) 0xFF0D0F12 else 0xFFFFFFFF) else rawBgColor
}

/**
 * Профиль использования Lkboard при онбординге и в настройках.
 */
enum class UserRole(
    val title: String,
    val subtitle: String,
    val icon: String,
    val tools: Set<CanvasTool>
) {
    TUTOR_STEM(
        title = "Репетитор (Математика и Физика)",
        subtitle = "LaTeX-формулы, физический конструктор, графики Desmos, стикеры с домашкой",
        icon = "👨‍🏫",
        tools = setOf(
            CanvasTool.SELECT, CanvasTool.PEN, CanvasTool.ERASER, CanvasTool.TEXT,
            CanvasTool.LATEX, CanvasTool.PHYSICS_STENCIL, CanvasTool.FUNCTION_GRAPH,
            CanvasTool.STICKY_NOTE, CanvasTool.SHAPE
        )
    ),
    TEAM_COLLAB(
        title = "Команда и созвоны",
        subtitle = "Канбан-доски, вехи дорожной карты, стикеры, умные стрелки-коннекторы",
        icon = "💼",
        tools = setOf(
            CanvasTool.SELECT, CanvasTool.STICKY_NOTE, CanvasTool.KANBAN,
            CanvasTool.MILESTONE, CanvasTool.CONNECTOR, CanvasTool.SHAPE,
            CanvasTool.TEXT, CanvasTool.PEN, CanvasTool.ERASER
        )
    ),
    SKETCH_NOTE(
        title = "Скетчи и быстрое рисование",
        subtitle = "Перо, ластик, базовые фигуры, текст — чистота и фокус",
        icon = "🎨",
        tools = setOf(
            CanvasTool.SELECT, CanvasTool.PEN, CanvasTool.ERASER,
            CanvasTool.SHAPE, CanvasTool.TEXT
        )
    ),
    ALL_TOOLS(
        title = "Полный набор (Все инструменты)",
        subtitle = "Максимальный функционал без ограничений для опытных пользователей",
        icon = "⚡",
        tools = CanvasTool.entries.toSet()
    )
}

/**
 * Синглтон настроек приложения Lkboard на базе java.util.prefs.Preferences.
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

    private const val KEY_APP_THEME = "app_theme_mode"
    private const val KEY_USER_ROLE = "user_role_profile"
    private const val KEY_FIRST_RUN_DONE = "first_run_onboarding_completed"
    private const val KEY_ENABLED_TOOLS = "enabled_tools_csv"
    private const val KEY_TOOLBAR_POSITION = "toolbar_dock_position"
    private const val KEY_RECENT_COLORS = "recent_custom_colors_hex"

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

    // --- Тема оформления ---

    fun loadAppTheme(): AppTheme {
        return try {
            val name = prefs.get(KEY_APP_THEME, AppTheme.LIGHT.name)
            AppTheme.valueOf(name)
        } catch (_: Exception) {
            AppTheme.LIGHT
        }
    }

    fun saveAppTheme(theme: AppTheme) {
        try {
            prefs.put(KEY_APP_THEME, theme.name)
            prefs.flush()
        } catch (_: Exception) {}
    }

    // --- Онбординг и профиль пользователя ---

    fun isFirstRun(): Boolean {
        return try {
            !prefs.getBoolean(KEY_FIRST_RUN_DONE, false)
        } catch (_: Exception) {
            false
        }
    }

    fun setFirstRunCompleted() {
        try {
            prefs.putBoolean(KEY_FIRST_RUN_DONE, true)
            prefs.flush()
        } catch (_: Exception) {}
    }

    fun loadUserRole(): UserRole {
        return try {
            val name = prefs.get(KEY_USER_ROLE, UserRole.ALL_TOOLS.name)
            UserRole.valueOf(name)
        } catch (_: Exception) {
            UserRole.ALL_TOOLS
        }
    }

    fun saveUserRole(role: UserRole) {
        try {
            prefs.put(KEY_USER_ROLE, role.name)
            saveEnabledTools(role.tools)
            prefs.flush()
        } catch (_: Exception) {}
    }

    // --- Включенные инструменты тулбара ---

    fun loadEnabledTools(): Set<CanvasTool> {
        return try {
            val csv = prefs.get(KEY_ENABLED_TOOLS, null) ?: return CanvasTool.entries.toSet()
            if (csv.isBlank()) return CanvasTool.entries.toSet()
            val tools = csv.split(",").mapNotNull { name ->
                try { CanvasTool.valueOf(name.trim()) } catch (_: Exception) { null }
            }.toSet()
            if (tools.isEmpty()) CanvasTool.entries.toSet() else tools
        } catch (_: Exception) {
            CanvasTool.entries.toSet()
        }
    }

    fun saveEnabledTools(tools: Set<CanvasTool>) {
        try {
            val csv = tools.joinToString(",") { it.name }
            prefs.put(KEY_ENABLED_TOOLS, csv)
            prefs.flush()
        } catch (_: Exception) {}
    }

    // --- Положение панели инструментов (Слева / Снизу / Справа) ---

    fun loadToolbarPosition(): ToolbarPosition {
        return try {
            val id = prefs.get(KEY_TOOLBAR_POSITION, ToolbarPosition.LEFT.id)
            ToolbarPosition.fromId(id)
        } catch (_: Exception) {
            ToolbarPosition.LEFT
        }
    }

    fun saveToolbarPosition(position: ToolbarPosition) {
        try {
            prefs.put(KEY_TOOLBAR_POSITION, position.id)
            prefs.flush()
        } catch (_: Exception) {}
    }

    // --- Недавние пользовательские цвета палитры (HEX) ---

    val DEFAULT_RECENT_COLORS = listOf(
        0xFF2563EBL, // Royal Blue
        0xFF0D9488L, // Teal
        0xFF16A34AL, // Green
        0xFFF59E0BL, // Amber
        0xFFDC2626L, // Red
        0xFF9333EAL, // Purple
        0xFFEC4899L, // Pink
        0xFF0284C7L  // Cyan
    )

    fun loadRecentColors(): List<Long> {
        return try {
            val csv = prefs.get(KEY_RECENT_COLORS, null) ?: return DEFAULT_RECENT_COLORS
            if (csv.isBlank()) return DEFAULT_RECENT_COLORS
            val list = csv.split(",").mapNotNull { hexStr ->
                hexStr.trim().toLongOrNull(16)
            }
            if (list.isEmpty()) DEFAULT_RECENT_COLORS else list.take(8)
        } catch (_: Exception) {
            DEFAULT_RECENT_COLORS
        }
    }

    fun saveRecentColor(colorHex: Long) {
        try {
            val current = loadRecentColors().toMutableList()
            current.remove(colorHex)
            current.add(0, colorHex)
            val updated = current.take(8)
            val csv = updated.joinToString(",") { (it and 0xFFFFFFFFL).toString(16) }
            prefs.put(KEY_RECENT_COLORS, csv)
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
