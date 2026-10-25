package com.lkboard.app.model

/**
 * Инструменты взаимодействия с бесконечным холстом Lkboard.
 */
enum class CanvasTool(val displayName: String) {
    SELECT("Выделение (V)"),
    HAND("Рука / Панорамирование (H)"),
    STICKY_NOTE("Стикер (S)"),
    SHAPE("Фигура (R)"),
    TEXT("Текст (T)"),
    PEN("Перо (P)"),
    ERASER("Ластик (E)"),
    FRAME("Фрейм / Презентация (F)"),
    CONNECTOR("Умный коннектор (C)"),
    MILESTONE("Веха / Roadmap (M)"),
    FUNCTION_GRAPH("График функций (G)"),
    LATEX("Формула LaTeX (X)"),
    PHYSICS_STENCIL("Конструктор физики (B)"),
    KANBAN("Канбан-доска (K)"),
    LASER_POINTER("Лазерная указка (L)")
}
