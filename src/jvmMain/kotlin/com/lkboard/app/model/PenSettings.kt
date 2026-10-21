package com.lkboard.app.model

import kotlinx.serialization.Serializable

/**
 * Настройки активного инструмента рисования (ручка / стилус / маркер для преподавания).
 */
@Serializable
data class PenSettings(
    val colorHex: Long = COLOR_BLACK,
    val strokeWidth: Float = 4f,
    val isHighlighter: Boolean = false,
    val isStraightLine: Boolean = false
) {
    companion object {
        const val COLOR_BLACK = 0xFF1E1E1EL
        const val COLOR_SLATE = 0xFF64748BL
        const val COLOR_BLUE = 0xFF2563EBL
        const val COLOR_CYAN = 0xFF0284C7L
        const val COLOR_TEAL = 0xFF0D9488L
        const val COLOR_GREEN = 0xFF16A34AL
        const val COLOR_EMERALD = 0xFF059669L

        const val COLOR_RED = 0xFFDC2626L
        const val COLOR_ROSE = 0xFFE11D48L
        const val COLOR_PINK = 0xFFDB2777L
        const val COLOR_ORANGE = 0xFFEA580CL
        const val COLOR_YELLOW = 0xFFEAB308L
        const val COLOR_HIGHLIGHTER = 0xFFFACC15L
        const val COLOR_PURPLE = 0xFF9333EAL

        val PRESET_COLORS = listOf(
            COLOR_BLACK,
            COLOR_SLATE,
            COLOR_BLUE,
            COLOR_CYAN,
            COLOR_TEAL,
            COLOR_GREEN,
            COLOR_EMERALD,
            COLOR_RED,
            COLOR_ROSE,
            COLOR_PINK,
            COLOR_ORANGE,
            COLOR_YELLOW,
            COLOR_HIGHLIGHTER,
            COLOR_PURPLE
        )

        // 4 пресета: Тонкая 2px, Средняя 4px, Толстая 8px, Маркер 24px
        val PRESET_WIDTHS = listOf(2f, 4f, 8f, 24f)
    }
}
