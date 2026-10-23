package com.lkboard.app.data

import com.lkboard.app.model.BoardElement
import com.lkboard.app.model.BoardState
import com.lkboard.app.model.CanvasPoint
import com.lkboard.app.model.GridType
import com.lkboard.app.model.PenSettings
import com.lkboard.app.model.ShapeSettings
import com.lkboard.app.model.ShapeType
import com.lkboard.app.model.StrokeStyle

/**
 * Перечень доступных шаблонов создания досок.
 */
enum class BoardTemplate(val displayName: String, val icon: String, val description: String) {
    BLANK(
        displayName = "Чистый холст",
        icon = "📄",
        description = "Пустая бесконечная доска для свободных записей и рисования"
    ),
    ROADMAP(
        displayName = "Дорожная карта (Roadmap)",
        icon = "🗺️",
        description = "Готовая структура фаз проекта (Q1–Q4), вехи, задачи и стрелки зависимостей"
    ),
    KANBAN(
        displayName = "Канбан / Спринт",
        icon = "📊",
        description = "Колонки To Do, In Progress, Done для управления задачами"
    ),
    BRAINSTORM(
        displayName = "Мозговой штурм (Mind Map)",
        icon = "💡",
        description = "Центральная концепция и разноцветные ветви идей со стикерами"
    ),
    MATH_STUDIO(
        displayName = "Математика (Desmos)",
        icon = "📈",
        description = "Интерактивные графики функций, координатные плоскости и формулы"
    )
}

/**
 * Генератор предзаполненных структур досок и дорожных карт.
 */
object BoardTemplateGenerator {

    fun generate(template: BoardTemplate, title: String): BoardState {
        val defaultPen = AppPreferences.loadPenSettings()
        val defaultShape = AppPreferences.loadShapeSettings()

        return when (template) {
            BoardTemplate.BLANK -> {
                BoardState(
                    title = title,
                    elements = emptyMap(),
                    penSettings = defaultPen,
                    shapeSettings = defaultShape,
                    gridType = AppPreferences.loadGridType(),
                    backgroundColorHex = AppPreferences.loadBackgroundColor()
                )
            }

            BoardTemplate.ROADMAP -> generateRoadmap(title, defaultPen, defaultShape)
            BoardTemplate.KANBAN -> generateKanban(title, defaultPen, defaultShape)
            BoardTemplate.BRAINSTORM -> generateBrainstorm(title, defaultPen, defaultShape)
            BoardTemplate.MATH_STUDIO -> generateMathStudio(title, defaultPen, defaultShape)
        }
    }

    private fun generateRoadmap(
        title: String,
        penSettings: PenSettings,
        shapeSettings: ShapeSettings
    ): BoardState {
        val elements = mutableMapOf<String, BoardElement>()

        // 1. Заголовок и описание
        val titleText = BoardElement.Text(
            id = "roadmap-header",
            x = 100f,
            y = 50f,
            width = 800f,
            height = 45f,
            text = "🗺️ $title",
            fontSize = 26f,
            isBold = true,
            colorHex = 0xFF0F172A
        )
        elements[titleText.id] = titleText

        val subtitleText = BoardElement.Text(
            id = "roadmap-sub",
            x = 100f,
            y = 100f,
            width = 800f,
            height = 30f,
            text = "Стратегический план развития продукта: фазы, вехи и ключевые зависимости",
            fontSize = 15f,
            colorHex = 0xFF64748B
        )
        elements[subtitleText.id] = subtitleText

        // 2. Фазы / Колонки дорожной карты (Q1, Q2, Q3, Q4)
        data class PhaseDef(
            val key: String,
            val name: String,
            val strokeColor: Long,
            val fillColor: Long,
            val cards: List<Pair<String, String>>
        )

        val phases = listOf(
            PhaseDef(
                key = "q1",
                name = "Q1 • Базовая архитектура",
                strokeColor = 0xFF2563EB,
                fillColor = 0x0A2563EB,
                cards = listOf(
                    "✅ Infinite Canvas Engine" to "Масштабирование от 10% до 500%, быстрый Skia-рендеринг",
                    "✅ Apple Liquid Glass UI" to "Эстетичные стеклянные панели без швов и артефактов",
                    "✅ Плавное перо (Catmull-Rom)" to "Сглаживание рукописного ввода, маркер и точки"
                )
            ),
            PhaseDef(
                key = "q2",
                name = "Q2 • Презентации & Проекты",
                strokeColor = 0xFF059669,
                fillColor = 0x0A059669,
                cards = listOf(
                    "✅ PDF & PPTX Фреймы" to "Интерактивный показ слайдов с рукописными пометками",
                    "🚀 Главный экран & Доски" to "Менеджер досок, превью, быстрый поиск и переключение",
                    "✅ Геометрия & 3D Фигуры" to "Кубы, цилиндры, конусы, сферы, выноски и полигоны"
                )
            ),
            PhaseDef(
                key = "q3",
                name = "Q3 • Дорожные карты & Смарт",
                strokeColor = 0xFFEA580C,
                fillColor = 0x0AEA580C,
                cards = listOf(
                    "📅 Смарт-коннекторы" to "Динамические стрелки с автоматической привязкой к граням",
                    "📅 Таймлайны & Спринты" to "Гант-диаграммы, дорожки этапов и бейджи дедлайнов",
                    "📅 Магнитная сетка (Snap)" to "Умные направляющие и автоматическое выравнивание"
                )
            ),
            PhaseDef(
                key = "q4",
                name = "Q4 • Экосистема & Экспорт",
                strokeColor = 0xFF9333EA,
                fillColor = 0x0A9333EA,
                cards = listOf(
                    "📅 Векторный SVG & PDF" to "Высококачественный экспорт проектов без потери четкости",
                    "📅 Real-Time Коллаборация" to "Сетевая совместная работа над доской через WebSocket",
                    "📅 Облачная синхронизация" to "Автоматический бэкап и доступ к проектам с любого ПК"
                )
            )
        )

        val startX = 100f
        val colWidth = 310f
        val colGap = 40f
        val startY = 160f
        val colHeight = 560f

        phases.forEachIndexed { colIdx, phase ->
            val colX = startX + colIdx * (colWidth + colGap)

            // Контейнер колонки (Swimlane / Phase Card)
            val columnShape = BoardElement.Shape(
                id = "lane-${phase.key}",
                x = colX,
                y = startY,
                width = colWidth,
                height = colHeight,
                shapeType = ShapeType.ROUNDED_RECTANGLE,
                strokeColorHex = phase.strokeColor,
                fillColorHex = phase.fillColor,
                strokeWidth = 2f,
                cornerRadius = 16f
            )
            elements[columnShape.id] = columnShape

            // Заголовок фазы
            val colTitle = BoardElement.Text(
                id = "title-${phase.key}",
                x = colX + 16f,
                y = startY + 16f,
                width = colWidth - 32f,
                height = 32f,
                text = phase.name,
                fontSize = 17f,
                isBold = true,
                colorHex = phase.strokeColor
            )
            elements[colTitle.id] = colTitle

            // Карточки внутри фазы
            var cardY = startY + 60f
            phase.cards.forEachIndexed { cardIdx, (cardTitle, cardDesc) ->
                val cardShape = BoardElement.Shape(
                    id = "card-${phase.key}-$cardIdx",
                    x = colX + 16f,
                    y = cardY,
                    width = colWidth - 32f,
                    height = 95f,
                    shapeType = ShapeType.ROUNDED_RECTANGLE,
                    strokeColorHex = 0xFFE2E8F0,
                    fillColorHex = 0xFFFFFFFF,
                    strokeWidth = 1.2f,
                    cornerRadius = 12f
                )
                elements[cardShape.id] = cardShape

                val cardText = BoardElement.Text(
                    id = "text-${phase.key}-$cardIdx",
                    x = colX + 26f,
                    y = cardY + 12f,
                    width = colWidth - 52f,
                    height = 70f,
                    text = "$cardTitle\n$cardDesc",
                    fontSize = 13f,
                    isBold = false,
                    colorHex = 0xFF1E293B
                )
                elements[cardText.id] = cardText

                cardY += 105f
            }

            // Направляющая стрелка к следующей колонке
            if (colIdx < phases.size - 1) {
                val arrowX = colX + colWidth + 6f
                val arrowY = startY + 105f + (colIdx % 2) * 105f
                val arrow = BoardElement.Shape(
                    id = "arrow-${phase.key}-next",
                    x = arrowX,
                    y = arrowY,
                    width = colGap - 12f,
                    height = 20f,
                    shapeType = ShapeType.ARROW,
                    strokeColorHex = phase.strokeColor,
                    strokeWidth = 2.5f
                )
                elements[arrow.id] = arrow
            }
        }

        return BoardState(
            title = title,
            elements = elements,
            penSettings = penSettings,
            shapeSettings = shapeSettings,
            gridType = GridType.DOTS,
            backgroundColorHex = 0xFFF8FAFC
        )
    }

    private fun generateKanban(
        title: String,
        penSettings: PenSettings,
        shapeSettings: ShapeSettings
    ): BoardState {
        val elements = mutableMapOf<String, BoardElement>()

        val titleText = BoardElement.Text(
            id = "kanban-header",
            x = 100f,
            y = 60f,
            width = 600f,
            height = 40f,
            text = "📊 $title",
            fontSize = 24f,
            isBold = true,
            colorHex = 0xFF0F172A
        )
        elements[titleText.id] = titleText

        data class ColumnDef(val name: String, val colorHex: Long, val notes: List<Pair<String, Long>>)

        val cols = listOf(
            ColumnDef("📌 К выполнению (To Do)", 0xFF64748B, listOf(
                "Добавить умные направляющие" to 0xFFFFF176,
                "Оптимизировать экспорт в PNG" to 0xFFFFD54F
            )),
            ColumnDef("⚡ В работе (In Progress)", 0xFF2563EB, listOf(
                "Спроектировать модуль дорожных карт" to 0xFF81D4FA,
                "Улучшить переключение между досками" to 0xFFA5D6A7
            )),
            ColumnDef("🎉 Готово (Done)", 0xFF16A34A, listOf(
                "Apple Glassmorphism редизайн тулбара" to 0xFFC8E6C9,
                "Импорт презентаций PowerPoint и PDF" to 0xFFC8E6C9,
                "Поддержка фирменного логотипа LKBoard" to 0xFFC8E6C9
            ))
        )

        var xOffset = 100f
        cols.forEachIndexed { i, col ->
            val colBox = BoardElement.Shape(
                id = "kb-col-$i",
                x = xOffset,
                y = 120f,
                width = 320f,
                height = 540f,
                shapeType = ShapeType.ROUNDED_RECTANGLE,
                strokeColorHex = col.colorHex,
                fillColorHex = 0x0A000000,
                strokeWidth = 2f,
                cornerRadius = 14f
            )
            elements[colBox.id] = colBox

            val colTitle = BoardElement.Text(
                id = "kb-title-$i",
                x = xOffset + 16f,
                y = 136f,
                width = 280f,
                height = 30f,
                text = col.name,
                fontSize = 17f,
                isBold = true,
                colorHex = col.colorHex
            )
            elements[colTitle.id] = colTitle

            var noteY = 180f
            col.notes.forEachIndexed { noteIdx, (noteText, noteColor) ->
                val note = BoardElement.StickyNote(
                    id = "kb-note-$i-$noteIdx",
                    x = xOffset + 20f,
                    y = noteY,
                    width = 280f,
                    height = 90f,
                    text = noteText,
                    colorHex = noteColor
                )
                elements[note.id] = note
                noteY += 105f
            }

            xOffset += 350f
        }

        return BoardState(
            title = title,
            elements = elements,
            penSettings = penSettings,
            shapeSettings = shapeSettings,
            gridType = GridType.LINES,
            backgroundColorHex = 0xFFFFFFFF
        )
    }

    private fun generateBrainstorm(
        title: String,
        penSettings: PenSettings,
        shapeSettings: ShapeSettings
    ): BoardState {
        val elements = mutableMapOf<String, BoardElement>()

        // Центральный узел концепции
        val centerCard = BoardElement.Shape(
            id = "brain-center",
            x = 550f,
            y = 300f,
            width = 260f,
            height = 140f,
            shapeType = ShapeType.ROUNDED_RECTANGLE,
            strokeColorHex = 0xFF2563EB,
            fillColorHex = 0x1A2563EB,
            strokeWidth = 3f,
            cornerRadius = 20f
        )
        elements[centerCard.id] = centerCard

        val centerText = BoardElement.Text(
            id = "brain-center-txt",
            x = 565f,
            y = 345f,
            width = 230f,
            height = 50f,
            text = "💡 Главная цель:\nСупер-продукт Lkboard",
            fontSize = 18f,
            isBold = true,
            colorHex = 0xFF1E3A8A
        )
        elements[centerText.id] = centerText

        // Стикеры идей вокруг
        val ideas = listOf(
            Triple(160f, 150f, "🎨 Непревзойденный\nдизайн Apple Glass"),
            Triple(940f, 150f, "🗺️ Быстрые дорожные\nкарты и таймлайны"),
            Triple(160f, 460f, "✍️ Идеально плавный\nстилус без дерганий"),
            Triple(940f, 460f, "🚀 Менеджер проектов\nи переключение досок")
        )

        ideas.forEachIndexed { i, (ix, iy, text) ->
            val note = BoardElement.StickyNote(
                id = "brain-idea-$i",
                x = ix,
                y = iy,
                width = 230f,
                height = 120f,
                text = text,
                colorHex = when (i % 4) {
                    0 -> 0xFFFFF176
                    1 -> 0xFF81D4FA
                    2 -> 0xFFA5D6A7
                    else -> 0xFFFFCC80
                }
            )
            elements[note.id] = note
        }

        return BoardState(
            title = title,
            elements = elements,
            penSettings = penSettings,
            shapeSettings = shapeSettings,
            gridType = GridType.DOTS,
            backgroundColorHex = 0xFFFFFFFF
        )
    }

    private fun generateMathStudio(
        title: String,
        penSettings: PenSettings,
        shapeSettings: ShapeSettings
    ): BoardState {
        val elements = mutableMapOf<String, BoardElement>()

        // 1. Заголовок
        val header = BoardElement.Text(
            id = "math-header",
            x = 100f,
            y = 50f,
            width = 800f,
            height = 45f,
            text = "📈 $title",
            fontSize = 26f,
            isBold = true,
            colorHex = 0xFF0F172A
        )
        elements[header.id] = header

        val subheader = BoardElement.Text(
            id = "math-sub",
            x = 100f,
            y = 100f,
            width = 800f,
            height = 30f,
            text = "Интерактивная математическая студия: графики функций, формулы и исследование кривых",
            fontSize = 15f,
            colorHex = 0xFF64748B
        )
        elements[subheader.id] = subheader

        // 2. График тригонометрии
        val trigGraph = BoardElement.FunctionGraph(
            id = "math-graph-trig",
            x = 100f,
            y = 150f,
            width = 540f,
            height = 380f,
            title = "Тригонометрия и колебания",
            expressions = listOf("sin(x)", "cos(x)", "sin(2 * x) * 0.5"),
            xMin = -10f,
            xMax = 10f,
            yMin = -4f,
            yMax = 4f,
            strokeColorsHex = listOf(0xFF2563EB, 0xFFDC2626, 0xFF16A34A)
        )
        elements[trigGraph.id] = trigGraph

        // 3. График полиномов и корней
        val polyGraph = BoardElement.FunctionGraph(
            id = "math-graph-poly",
            x = 680f,
            y = 150f,
            width = 540f,
            height = 380f,
            title = "Степенные функции и полиномы",
            expressions = listOf("x^2", "x^3 - 3 * x", "sqrt(abs(x))"),
            xMin = -6f,
            xMax = 6f,
            yMin = -6f,
            yMax = 6f,
            strokeColorsHex = listOf(0xFF7C3AED, 0xFFEA580C, 0xFF0D9488)
        )
        elements[polyGraph.id] = polyGraph

        // 4. Стикеры с полезными подсказками и формулами
        val tipsNote = BoardElement.StickyNote(
            id = "math-tips-note",
            x = 100f,
            y = 560f,
            width = 380f,
            height = 140f,
            text = "💡 Инструкция:\n- Двойной клик на график открывает редактор\n- Доступна виртуальная клавиатура\n- Функции: sin, cos, tan, sqrt, abs, ln, exp, pi, e",
            colorHex = 0xFFBAE6FD
        )
        elements[tipsNote.id] = tipsNote

        val formulasNote = BoardElement.StickyNote(
            id = "math-formulas-note",
            x = 520f,
            y = 560f,
            width = 380f,
            height = 140f,
            text = "📐 Интересные кривые для ввода:\n- f(x) = exp(-0.1 * x^2) * cos(3 * x)\n- f(x) = tan(x)\n- f(x) = abs(x) - 2",
            colorHex = 0xFFBBF7D0
        )
        elements[formulasNote.id] = formulasNote

        return BoardState(
            title = title,
            elements = elements,
            penSettings = penSettings,
            shapeSettings = shapeSettings,
            gridType = GridType.LINES,
            backgroundColorHex = 0xFFFFFFFF
        )
    }
}
