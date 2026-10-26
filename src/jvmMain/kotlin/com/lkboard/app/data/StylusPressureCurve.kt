package com.lkboard.app.data

/**
 * Пресеты калибровки кривой чувствительности нажима для стилуса (Stylus Pressure Curve).
 * Позволяет адаптировать отклик пера под индивидуальные особенности руки и тип стилуса:
 * - S-Pen / Wacom: идеальная градация полутонов
 * - Apple Pencil: мягкий нажим без усталости кисти
 * - Емкостные стилусы с резиновым/дисковым наконечником: динамическая толщина от скорости штриха
 */
enum class StylusPressureCurve(
    val id: String,
    val title: String,
    val icon: String,
    val subtitle: String,
    val gamma: Float
) {
    SOFT(
        id = "soft",
        title = "Мягкая",
        icon = "🪶",
        subtitle = "Легкое касание дает насыщенный штрих (Apple Pencil / быстрые эскизы)",
        gamma = 0.55f
    ),
    NORMAL(
        id = "normal",
        title = "Сбалансированная",
        icon = "⚖️",
        subtitle = "Естественный отклик 1:1 без искажений (стандарт для S-Pen)",
        gamma = 1.0f
    ),
    HARD(
        id = "hard",
        title = "Жесткая",
        icon = "🖋️",
        subtitle = "Требует ощутимого нажима (точность формул, каллиграфия, контроль)",
        gamma = 1.8f
    ),
    CAPACITIVE(
        id = "capacitive",
        title = "Емкостный",
        icon = "⚡",
        subtitle = "Для недорогих стилусов без датчика нажима: толщина зависит от скорости ведения пера",
        gamma = 0.0f
    );

    companion object {
        fun fromId(id: String?): StylusPressureCurve =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: NORMAL
    }
}
