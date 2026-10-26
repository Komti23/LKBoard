package com.lkboard.app.data

/**
 * Варианты расположения основной панели инструментов на холсте:
 * - LEFT (Слева, по умолчанию) — вертикальная панель по левому краю
 * - BOTTOM (Снизу) — горизонтальная плавающая панель по нижнему краю (стиль планшетов и Miro)
 * - RIGHT (Справа) — вертикальная панель по правому краю (удобно для правшей и рисования стилусом)
 */
enum class ToolbarPosition(
    val id: String,
    val title: String,
    val icon: String,
    val description: String
) {
    LEFT(
        id = "left",
        title = "Слева",
        icon = "⬅️",
        description = "Вертикальная панель инструментов у левого края холста"
    ),
    BOTTOM(
        id = "bottom",
        title = "Снизу",
        icon = "⬇️",
        description = "Горизонтальная плавающая панель по центру снизу"
    ),
    RIGHT(
        id = "right",
        title = "Справа",
        icon = "➡️",
        description = "Вертикальная панель инструментов у правого края холста"
    );

    companion object {
        fun fromId(id: String?): ToolbarPosition =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: LEFT
    }
}
