package com.lkboard.app.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.lkboard.app.model.BoardElement
import com.lkboard.app.model.ShapeType
import com.lkboard.app.model.ConnectorStyle
import com.lkboard.app.model.MilestoneStatus
import com.lkboard.app.model.getAnchorOffset
import com.lkboard.app.util.MathEvaluator
import com.lkboard.app.util.PathSmoothing

/**
 * Отрисовщик графических элементов доски в мировых координатах.
 */
object ElementRenderer {

    private val imageCache = mutableMapOf<String, androidx.compose.ui.graphics.ImageBitmap>()

    fun drawElement(
        drawScope: DrawScope,
        element: BoardElement,
        textMeasurer: TextMeasurer,
        isSelected: Boolean,
        allElements: Map<String, BoardElement> = emptyMap()
    ) {
        when (element) {
            is BoardElement.StickyNote -> drawStickyNote(drawScope, element, textMeasurer)
            is BoardElement.Shape -> drawShape(drawScope, element)
            is BoardElement.Text -> drawTextBlock(drawScope, element, textMeasurer)
            is BoardElement.DrawingPath -> drawDrawingPath(drawScope, element)
            is BoardElement.ImageElement -> drawImageElement(drawScope, element)
            is BoardElement.DocumentFrame -> drawDocumentFrame(drawScope, element, textMeasurer)
            is BoardElement.Connector -> drawConnector(drawScope, element, allElements, textMeasurer)
            is BoardElement.MilestoneCard -> drawMilestoneCard(drawScope, element, textMeasurer)
            is BoardElement.FunctionGraph -> drawFunctionGraph(drawScope, element, textMeasurer)
            is BoardElement.KanbanBoard -> drawKanbanBoard(drawScope, element, allElements, textMeasurer)
        }

        if (isSelected) {
            drawSelectionOutline(drawScope, element)
        }
    }

    private fun drawImageElement(
        drawScope: DrawScope,
        img: BoardElement.ImageElement
    ) {
        try {
            val bitmap = imageCache.getOrPut(img.id) {
                val bytes = java.util.Base64.getDecoder().decode(img.imageBase64)
                androidx.compose.ui.res.loadImageBitmap(java.io.ByteArrayInputStream(bytes))
            }
            drawScope.drawImage(
                image = bitmap,
                dstOffset = androidx.compose.ui.unit.IntOffset(img.x.toInt(), img.y.toInt()),
                dstSize = androidx.compose.ui.unit.IntSize(img.width.toInt(), img.height.toInt())
            )
        } catch (e: Exception) {
            drawScope.drawRect(
                color = Color.LightGray,
                topLeft = Offset(img.x, img.y),
                size = Size(img.width, img.height)
            )
        }
    }

    private fun drawDocumentFrame(
        drawScope: DrawScope,
        frame: BoardElement.DocumentFrame,
        textMeasurer: TextMeasurer
    ) {
        val headerH = 38f
        val cr = CornerRadius(10f, 10f)

        // 1. Мягкая внешняя тень фрейма (Apple Card Shadow)
        drawScope.drawRoundRect(
            color = Color.Black.copy(alpha = 0.08f),
            topLeft = Offset(frame.x + 3f, frame.y + 6f),
            size = Size(frame.width, frame.height),
            cornerRadius = cr
        )

        // 2. Фоновое тело фрейма (слайда)
        drawScope.drawRoundRect(
            color = Color(frame.backgroundColorHex),
            topLeft = Offset(frame.x, frame.y),
            size = Size(frame.width, frame.height),
            cornerRadius = cr
        )

        // 3. Шапка фрейма с заголовком и счетчиком слайдов
        drawScope.drawRoundRect(
            color = Color(0xFFF1F5F9),
            topLeft = Offset(frame.x, frame.y),
            size = Size(frame.width, headerH),
            cornerRadius = CornerRadius(10f, 10f)
        )
        // Скрываем скругления в нижней части шапки прямоугольником
        drawScope.drawRect(
            color = Color(0xFFF1F5F9),
            topLeft = Offset(frame.x, frame.y + headerH - 8f),
            size = Size(frame.width, 8f)
        )

        // Разделительная линия под шапкой
        drawScope.drawLine(
            color = Color(0xFFE2E8F0),
            start = Offset(frame.x, frame.y + headerH),
            end = Offset(frame.x + frame.width, frame.y + headerH),
            strokeWidth = 1f
        )

        // Заголовок фрейма
        val titleResult = textMeasurer.measure(
            text = frame.title,
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E293B)
            ),
            maxLines = 1
        )
        drawScope.drawText(
            textLayoutResult = titleResult,
            topLeft = Offset(frame.x + 14f, frame.y + 10f)
        )

        // Бейдж номера слайда / страницы (например: "1 / 15" или "Пустой фрейм")
        val pageText = if (frame.totalPages > 0) {
            "${frame.currentPageIndex + 1} / ${frame.totalPages}"
        } else {
            "Нет файла"
        }
        val badgeResult = textMeasurer.measure(
            text = pageText,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (frame.totalPages > 0) Color(0xFF0284C7) else Color(0xFF64748B)
            )
        )
        val badgeW = badgeResult.size.width + 16f
        val badgeX = frame.x + frame.width - badgeW - 12f
        drawScope.drawRoundRect(
            color = if (frame.totalPages > 0) Color(0xFFE0F2FE) else Color(0xFFE2E8F0),
            topLeft = Offset(badgeX, frame.y + 7f),
            size = Size(badgeW, 24f),
            cornerRadius = CornerRadius(12f, 12f)
        )
        drawScope.drawText(
            textLayoutResult = badgeResult,
            topLeft = Offset(badgeX + 8f, frame.y + 11f)
        )

        // 4. Область отображения содержимого слайда
        val contentY = frame.y + headerH
        val contentH = frame.height - headerH
        val currentBase64 = frame.currentPageBase64

        if (!currentBase64.isNullOrBlank()) {
            try {
                val cacheKey = "${frame.id}_${frame.currentPageIndex}"
                val bitmap = imageCache.getOrPut(cacheKey) {
                    val bytes = java.util.Base64.getDecoder().decode(currentBase64)
                    androidx.compose.ui.res.loadImageBitmap(java.io.ByteArrayInputStream(bytes))
                }
                drawScope.drawImage(
                    image = bitmap,
                    dstOffset = androidx.compose.ui.unit.IntOffset(frame.x.toInt(), contentY.toInt()),
                    dstSize = androidx.compose.ui.unit.IntSize(frame.width.toInt(), contentH.toInt())
                )
            } catch (_: Exception) {
                // Если картинка не отрисовалась, рисуем заглушку
                drawEmptyFramePlaceholder(drawScope, frame, contentY, contentH, textMeasurer)
            }
        } else {
            drawEmptyFramePlaceholder(drawScope, frame, contentY, contentH, textMeasurer)
        }

        // 5. Тонкая внешняя окантовка всего фрейма
        drawScope.drawRoundRect(
            color = Color(0xFFCBD5E1).copy(alpha = 0.8f),
            topLeft = Offset(frame.x, frame.y),
            size = Size(frame.width, frame.height),
            cornerRadius = cr,
            style = Stroke(width = 1.2f)
        )
    }

    private fun drawEmptyFramePlaceholder(
        drawScope: DrawScope,
        frame: BoardElement.DocumentFrame,
        contentY: Float,
        contentH: Float,
        textMeasurer: TextMeasurer
    ) {
        // Фоновая подсветка пустой области
        drawScope.drawRect(
            color = Color(0xFFF8FAFC),
            topLeft = Offset(frame.x, contentY),
            size = Size(frame.width, contentH)
        )

        val mainMsg = textMeasurer.measure(
            text = "Область презентации / PDF",
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF475569)
            )
        )
        val subMsg = textMeasurer.measure(
            text = "Нажмите на фрейм и выберите файл (PDF, PPTX или изображения)",
            style = TextStyle(
                fontSize = 12.sp,
                color = Color(0xFF94A3B8)
            )
        )

        val centerX = frame.x + frame.width / 2f
        val centerY = contentY + contentH / 2f

        drawScope.drawText(
            textLayoutResult = mainMsg,
            topLeft = Offset(centerX - mainMsg.size.width / 2f, centerY - 18f)
        )
        drawScope.drawText(
            textLayoutResult = subMsg,
            topLeft = Offset(centerX - subMsg.size.width / 2f, centerY + 8f)
        )
    }

    private fun drawStickyNote(
        drawScope: DrawScope,
        note: BoardElement.StickyNote,
        textMeasurer: TextMeasurer
    ) {
        val baseColor = Color(note.colorHex)
        val cr = CornerRadius(12f, 12f)
        val pad = 16f

        // 1. Мягкая внешняя тень стикера
        drawScope.drawRoundRect(
            color = Color.Black.copy(alpha = 0.08f),
            topLeft = Offset(note.x + 3f, note.y + 5f),
            size = Size(note.width, note.height),
            cornerRadius = cr
        )

        // 2. Тело стикера
        drawScope.drawRoundRect(
            color = baseColor,
            topLeft = Offset(note.x, note.y),
            size = Size(note.width, note.height),
            cornerRadius = cr
        )

        // 3. Эффект приподнятого/загнутого нижнего правого уголка бумаги (Paper peel)
        val foldSize = (note.width * 0.11f).coerceIn(16f, 24f)
        val foldPath = Path().apply {
            moveTo(note.x + note.width - foldSize, note.y + note.height)
            lineTo(note.x + note.width, note.y + note.height - foldSize)
            lineTo(note.x + note.width - foldSize, note.y + note.height - foldSize)
            close()
        }
        drawScope.drawPath(
            path = foldPath,
            color = Color.Black.copy(alpha = 0.06f)
        )

        // Тонкая окантовка стикера
        drawScope.drawRoundRect(
            color = Color.Black.copy(alpha = 0.08f),
            topLeft = Offset(note.x, note.y),
            size = Size(note.width, note.height),
            cornerRadius = cr,
            style = Stroke(width = 1f)
        )

        // 4. Умный авто-масштаб шрифта по объему текста и габаритам карточки
        val text = note.text
        if (text.isNotBlank()) {
            val textLen = text.length
            val autoFontSize = when {
                textLen < 35 -> (note.width / 11.5f).coerceIn(15f, 20f)
                textLen < 85 -> (note.width / 13.5f).coerceIn(13.5f, 16.5f)
                textLen < 160 -> (note.width / 15.5f).coerceIn(12f, 14f)
                else -> (note.width / 18f).coerceIn(10.5f, 12f)
            }
            val lineH = autoFontSize * 1.5f

            val lines = text.split("\n")
            var currentY = note.y + pad

            for ((lineIdx, rawLine) in lines.withIndex()) {
                if (currentY + lineH > note.y + note.height - pad) break
                val trimmed = rawLine.trimStart()
                val isChecked = trimmed.startsWith("- [x] ", ignoreCase = true) || trimmed.startsWith("[x] ", ignoreCase = true)
                val isUnchecked = trimmed.startsWith("- [ ] ") || trimmed.startsWith("[ ] ")

                if (isChecked || isUnchecked) {
                    val todoText = if (trimmed.startsWith("- ")) trimmed.substring(6) else trimmed.substring(4)
                    val cbSize = (autoFontSize * 1.05f).coerceIn(13f, 18f)
                    val cbY = currentY + (lineH - cbSize) / 2f
                    val cbX = note.x + pad

                    if (isChecked) {
                        // Отмеченный To-Do: зеленая заливка с белой галочкой ✓
                        drawScope.drawRoundRect(
                            color = Color(0xFF16A34A),
                            topLeft = Offset(cbX, cbY),
                            size = Size(cbSize, cbSize),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                        val checkPath = Path().apply {
                            moveTo(cbX + cbSize * 0.22f, cbY + cbSize * 0.52f)
                            lineTo(cbX + cbSize * 0.44f, cbY + cbSize * 0.74f)
                            lineTo(cbX + cbSize * 0.78f, cbY + cbSize * 0.26f)
                        }
                        drawScope.drawPath(
                            path = checkPath,
                            color = Color.White,
                            style = Stroke(width = (cbSize * 0.15f).coerceAtLeast(1.5f), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        // Зачеркнутый текст с приглушенным цветом
                        val res = textMeasurer.measure(
                            text = todoText,
                            style = TextStyle(
                                fontSize = autoFontSize.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF64748B),
                                textDecoration = TextDecoration.LineThrough
                            )
                        )
                        drawScope.drawText(res, topLeft = Offset(cbX + cbSize + 8f, currentY))
                    } else {
                        // Неотмеченный To-Do: белый квадрат с тонким контуром
                        drawScope.drawRoundRect(
                            color = Color.White.copy(alpha = 0.92f),
                            topLeft = Offset(cbX, cbY),
                            size = Size(cbSize, cbSize),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                        drawScope.drawRoundRect(
                            color = Color(0xFF94A3B8),
                            topLeft = Offset(cbX, cbY),
                            size = Size(cbSize, cbSize),
                            cornerRadius = CornerRadius(4f, 4f),
                            style = Stroke(width = 1.3f)
                        )
                        val res = textMeasurer.measure(
                            text = todoText,
                            style = TextStyle(
                                fontSize = autoFontSize.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1E293B)
                            )
                        )
                        drawScope.drawText(res, topLeft = Offset(cbX + cbSize + 8f, currentY))
                    }
                } else {
                    // Обычная строка текста
                    val isFirstHeader = lineIdx == 0 && lines.size > 1 && !rawLine.startsWith("- ")
                    val res = textMeasurer.measure(
                        text = rawLine,
                        style = TextStyle(
                            fontSize = autoFontSize.sp,
                            fontWeight = if (isFirstHeader) FontWeight.Bold else FontWeight.Medium,
                            color = Color(0xFF1E293B)
                        )
                    )
                    drawScope.drawText(res, topLeft = Offset(note.x + pad, currentY))
                }
                currentY += lineH
            }
        }
    }

    private fun drawKanbanBoard(
        drawScope: DrawScope,
        kanban: BoardElement.KanbanBoard,
        allElements: Map<String, BoardElement>,
        textMeasurer: TextMeasurer
    ) {
        val kScale = (kanban.width / 1000f).coerceIn(0.6f, 3.5f)
        val cr = CornerRadius(16f * kScale, 16f * kScale)
        val headerH = (46f * kScale).coerceAtLeast(36f)
        val colHeaderH = (38f * kScale).coerceAtLeast(28f)

        // 1. Мягкая внешняя тень канбан-доски
        drawScope.drawRoundRect(
            color = Color.Black.copy(alpha = 0.08f),
            topLeft = Offset(kanban.x + 3f * kScale, kanban.y + 6f * kScale),
            size = Size(kanban.width, kanban.height),
            cornerRadius = cr
        )

        // 2. Тело доски
        drawScope.drawRoundRect(
            color = Color(0xFFF8FAFC),
            topLeft = Offset(kanban.x, kanban.y),
            size = Size(kanban.width, kanban.height),
            cornerRadius = cr
        )
        drawScope.drawRoundRect(
            color = Color(0xFFE2E8F0),
            topLeft = Offset(kanban.x, kanban.y),
            size = Size(kanban.width, kanban.height),
            cornerRadius = cr,
            style = Stroke(width = 1.5f * kScale.coerceAtLeast(1f))
        )

        // 3. Шапка канбан-доски
        drawScope.drawRoundRect(
            color = Color(0xFFF1F5F9),
            topLeft = Offset(kanban.x, kanban.y),
            size = Size(kanban.width, headerH),
            cornerRadius = cr
        )
        drawScope.drawRect(
            color = Color(0xFFF1F5F9),
            topLeft = Offset(kanban.x, kanban.y + headerH - 12f * kScale),
            size = Size(kanban.width, 12f * kScale)
        )
        drawScope.drawLine(
            color = Color(0xFFE2E8F0),
            start = Offset(kanban.x, kanban.y + headerH),
            end = Offset(kanban.x + kanban.width, kanban.y + headerH),
            strokeWidth = 1f * kScale.coerceAtLeast(1f)
        )

        // Подсчет карточек вех внутри доски
        val milestoneCards = allElements.values.filterIsInstance<BoardElement.MilestoneCard>().filter {
            val center = Offset(it.x + it.width / 2f, it.y + it.height / 2f)
            kanban.bounds.contains(center)
        }

        // Заголовок доски (масштабируется с размером)
        val titleRes = textMeasurer.measure(
            text = "📋 ${kanban.title}",
            style = TextStyle(
                fontSize = (15f * kScale).sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            ),
            maxLines = 1
        )
        drawScope.drawText(titleRes, topLeft = Offset(kanban.x + 18f * kScale, kanban.y + (headerH - titleRes.size.height) / 2f))

        // Бейдж общего числа задач
        val totalBadgeText = "${milestoneCards.size} задач"
        val totalBadgeRes = textMeasurer.measure(
            text = totalBadgeText,
            style = TextStyle(
                fontSize = (11f * kScale).sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2563EB)
            )
        )
        val totalBadgeW = totalBadgeRes.size.width + 16f * kScale
        val totalBadgeH = 24f * kScale
        val totalBadgeX = kanban.x + kanban.width - totalBadgeW - 16f * kScale
        drawScope.drawRoundRect(
            color = Color(0xFFEFF6FF),
            topLeft = Offset(totalBadgeX, kanban.y + (headerH - totalBadgeH) / 2f),
            size = Size(totalBadgeW, totalBadgeH),
            cornerRadius = CornerRadius(6f * kScale, 6f * kScale)
        )
        drawScope.drawText(
            totalBadgeRes,
            topLeft = Offset(totalBadgeX + (totalBadgeW - totalBadgeRes.size.width) / 2f, kanban.y + (headerH - totalBadgeRes.size.height) / 2f)
        )

        // 4. Отрисовка 4 колонок
        val colW = kanban.width / 4f
        val colH = kanban.height - headerH

        val colDefs = listOf(
            Triple("🔴 Бэклог", Color(0xFF64748B), Color(0xFFF1F5F9)),
            Triple("🟡 В работе", Color(0xFF2563EB), Color(0xFFEFF6FF)),
            Triple("🟣 На ревью", Color(0xFF9333EA), Color(0xFFFAF5FF)),
            Triple("🟢 Готово", Color(0xFF16A34A), Color(0xFFF0FDF4))
        )

        for (i in 0 until 4) {
            val colX = kanban.x + i * colW
            val colY = kanban.y + headerH
            val (colTitle, colColor, colTint) = colDefs[i]

            // Вертикальный разделитель колонок
            if (i > 0) {
                drawScope.drawLine(
                    color = Color(0xFFE2E8F0),
                    start = Offset(colX, colY),
                    end = Offset(colX, colY + colH),
                    strokeWidth = 1f
                )
            }

            // Фоновая подложка колонки
            drawScope.drawRect(
                color = colTint.copy(alpha = 0.35f),
                topLeft = Offset(colX, colY),
                size = Size(colW, colH)
            )

            // Подсчет карточек именно в этой колонке
            val colCardsCount = milestoneCards.count {
                kanban.getColumnIndex(it.x + it.width / 2f) == i
            }

            // Заголовок колонки
            val colTitleRes = textMeasurer.measure(
                text = colTitle,
                style = TextStyle(
                    fontSize = (12.5f * kScale).sp,
                    fontWeight = FontWeight.Bold,
                    color = colColor
                )
            )
            drawScope.drawText(colTitleRes, topLeft = Offset(colX + 14f * kScale, colY + (colHeaderH - colTitleRes.size.height) / 2f))

            // Бейдж счетчика задач в колонке
            val countRes = textMeasurer.measure(
                text = "$colCardsCount",
                style = TextStyle(
                    fontSize = (11f * kScale).sp,
                    fontWeight = FontWeight.Bold,
                    color = colColor
                )
            )
            val countBadgeW = (countRes.size.width + 12f * kScale).coerceAtLeast(20f * kScale)
            val countBadgeH = 20f * kScale
            val countBadgeX = colX + colW - countBadgeW - 14f * kScale
            drawScope.drawRoundRect(
                color = colColor.copy(alpha = 0.12f),
                topLeft = Offset(countBadgeX, colY + (colHeaderH - countBadgeH) / 2f),
                size = Size(countBadgeW, countBadgeH),
                cornerRadius = CornerRadius(10f * kScale, 10f * kScale)
            )
            drawScope.drawText(
                countRes,
                topLeft = Offset(countBadgeX + (countBadgeW - countRes.size.width) / 2f, colY + (colHeaderH - countRes.size.height) / 2f)
            )

            // Линия под шапкой колонки
            drawScope.drawLine(
                color = Color(0xFFE2E8F0),
                start = Offset(colX + 8f * kScale, colY + colHeaderH),
                end = Offset(colX + colW - 8f * kScale, colY + colHeaderH),
                strokeWidth = 1f
            )

            // Подсказка, если колонка пуста
            if (colCardsCount == 0) {
                val dropNoticeRes = textMeasurer.measure(
                    text = "Перетащите веху сюда",
                    style = TextStyle(
                        fontSize = (11f * kScale).sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF94A3B8)
                    )
                )
                val dropNoticeX = colX + (colW - dropNoticeRes.size.width) / 2f
                drawScope.drawText(dropNoticeRes, topLeft = Offset(dropNoticeX, colY + 70f * kScale))
            }
        }
    }

    private fun drawShape(drawScope: DrawScope, shape: BoardElement.Shape) {
        val strokeColor = Color(shape.strokeColorHex)
        val fillColor = shape.fillColorHex?.let { Color(it) }

        com.lkboard.app.model.ShapeRenderer.drawShape(
            drawScope = drawScope,
            x = shape.x,
            y = shape.y,
            width = shape.width,
            height = shape.height,
            shapeType = shape.shapeType,
            strokeColor = strokeColor,
            fillColor = fillColor,
            strokeWidth = shape.strokeWidth,
            strokeStyle = shape.strokeStyle,
            cornerRadius = shape.cornerRadius
        )
    }

    private fun drawTextBlock(
        drawScope: DrawScope,
        textElem: BoardElement.Text,
        textMeasurer: TextMeasurer
    ) {
        val result = textMeasurer.measure(
            text = textElem.text,
            style = TextStyle(
                fontSize = textElem.fontSize.sp,
                fontWeight = if (textElem.isBold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (textElem.isItalic) FontStyle.Italic else FontStyle.Normal,
                color = Color(textElem.colorHex),
                lineHeight = (textElem.fontSize * 1.35f).sp
            ),
            constraints = androidx.compose.ui.unit.Constraints(
                maxWidth = textElem.width.toInt().coerceAtLeast(60)
            )
        )
        drawScope.drawText(
            textLayoutResult = result,
            topLeft = Offset(textElem.x, textElem.y)
        )
    }

    fun drawDrawingPath(
        drawScope: DrawScope,
        drawingPath: BoardElement.DrawingPath
    ) {
        if (drawingPath.points.isEmpty()) return

        val color = Color(drawingPath.strokeColorHex).let {
            if (drawingPath.isHighlighter) it.copy(alpha = 0.35f) else it
        }

        // Если ровно 1 точка — отрисовываем четкую круглую точку (аккуратный тап стилусом / мышью)
        if (drawingPath.points.size == 1) {
            val pt = drawingPath.points.first()
            val radius = (drawingPath.strokeWidth / 2f).coerceAtLeast(1.5f)
            drawScope.drawCircle(
                color = color,
                radius = radius,
                center = Offset(pt.x, pt.y)
            )
            return
        }

        // Плавная квадратичная кривая Безье без острых углов и изломов
        val path = PathSmoothing.buildSmoothPath(drawingPath.points)

        drawScope.drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = drawingPath.strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }

    private fun drawSelectionOutline(drawScope: DrawScope, element: BoardElement) {
        val bounds = element.bounds
        val padding = 4f
        drawScope.drawRect(
            color = Color(0xFF2979FF),
            topLeft = Offset(bounds.left - padding, bounds.top - padding),
            size = Size(bounds.width + padding * 2, bounds.height + padding * 2),
            style = Stroke(width = 2f)
        )
    }

    private fun drawConnector(
        drawScope: DrawScope,
        connector: BoardElement.Connector,
        allElements: Map<String, BoardElement>,
        textMeasurer: TextMeasurer
    ) {
        val startOffset = connector.startElementId?.let { id ->
            allElements[id]?.getAnchorOffset(connector.startAnchor)
        } ?: connector.startPoint.toOffset()

        val endOffset = connector.endElementId?.let { id ->
            allElements[id]?.getAnchorOffset(connector.endAnchor)
        } ?: connector.endPoint.toOffset()

        val color = Color(connector.strokeColorHex)
        val stroke = Stroke(
            width = connector.strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        val path = Path()
        when (connector.style) {
            ConnectorStyle.STRAIGHT -> {
                path.moveTo(startOffset.x, startOffset.y)
                path.lineTo(endOffset.x, endOffset.y)
            }
            ConnectorStyle.ORTHOGONAL -> {
                path.moveTo(startOffset.x, startOffset.y)
                val dx = endOffset.x - startOffset.x
                val dy = endOffset.y - startOffset.y
                if (kotlin.math.abs(dx) >= kotlin.math.abs(dy)) {
                    val midX = startOffset.x + dx / 2f
                    path.lineTo(midX, startOffset.y)
                    path.lineTo(midX, endOffset.y)
                    path.lineTo(endOffset.x, endOffset.y)
                } else {
                    val midY = startOffset.y + dy / 2f
                    path.lineTo(startOffset.x, midY)
                    path.lineTo(endOffset.x, midY)
                    path.lineTo(endOffset.x, endOffset.y)
                }
            }
            ConnectorStyle.BEZIER -> {
                path.moveTo(startOffset.x, startOffset.y)
                val dx = (endOffset.x - startOffset.x) * 0.5f
                val ctrl1 = Offset(startOffset.x + dx, startOffset.y)
                val ctrl2 = Offset(endOffset.x - dx, endOffset.y)
                path.cubicTo(ctrl1.x, ctrl1.y, ctrl2.x, ctrl2.y, endOffset.x, endOffset.y)
            }
        }

        drawScope.drawPath(path = path, color = color, style = stroke)

        if (connector.hasEndArrow) {
            drawArrowHead(drawScope, startOffset, endOffset, color, connector.strokeWidth, connector.style)
        }

        if (connector.label.isNotBlank()) {
            val midX = (startOffset.x + endOffset.x) / 2f
            val midY = (startOffset.y + endOffset.y) / 2f
            val layout = textMeasurer.measure(
                text = connector.label,
                style = TextStyle(fontSize = 11.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.SemiBold)
            )
            val padX = 6f
            val padY = 3f
            drawScope.drawRoundRect(
                color = Color.White,
                topLeft = Offset(midX - layout.size.width / 2f - padX, midY - layout.size.height / 2f - padY),
                size = Size(layout.size.width + padX * 2, layout.size.height + padY * 2),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawScope.drawRoundRect(
                color = color.copy(alpha = 0.5f),
                topLeft = Offset(midX - layout.size.width / 2f - padX, midY - layout.size.height / 2f - padY),
                size = Size(layout.size.width + padX * 2, layout.size.height + padY * 2),
                cornerRadius = CornerRadius(6f, 6f),
                style = Stroke(width = 1f)
            )
            drawScope.drawText(
                textLayoutResult = layout,
                topLeft = Offset(midX - layout.size.width / 2f, midY - layout.size.height / 2f)
            )
        }
    }

    private fun drawArrowHead(
        drawScope: DrawScope,
        start: Offset,
        end: Offset,
        color: Color,
        strokeWidth: Float,
        style: ConnectorStyle
    ) {
        val headLen = (strokeWidth * 3.5f + 6f).coerceIn(10f, 22f)
        val angle = when (style) {
            ConnectorStyle.ORTHOGONAL -> {
                if (kotlin.math.abs(end.x - start.x) >= kotlin.math.abs(end.y - start.y)) {
                    if (end.x >= start.x) 0.0 else kotlin.math.PI
                } else {
                    if (end.y >= start.y) kotlin.math.PI / 2.0 else -kotlin.math.PI / 2.0
                }
            }
            else -> kotlin.math.atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
        }

        val wingAngle = 0.45
        val p1 = Offset(
            (end.x - headLen * kotlin.math.cos(angle - wingAngle)).toFloat(),
            (end.y - headLen * kotlin.math.sin(angle - wingAngle)).toFloat()
        )
        val p2 = Offset(
            (end.x - headLen * kotlin.math.cos(angle + wingAngle)).toFloat(),
            (end.y - headLen * kotlin.math.sin(angle + wingAngle)).toFloat()
        )

        val headPath = Path().apply {
            moveTo(end.x, end.y)
            lineTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            close()
        }
        drawScope.drawPath(path = headPath, color = color, style = Fill)
    }

    private fun drawMilestoneCard(
        drawScope: DrawScope,
        card: BoardElement.MilestoneCard,
        textMeasurer: TextMeasurer
    ) {
        val cr = CornerRadius(14f, 14f)
        val statusColor = Color(card.status.colorHex)

        // Мягкая внешняя тень
        drawScope.drawRoundRect(
            color = Color.Black.copy(alpha = 0.06f),
            topLeft = Offset(card.x + 2f, card.y + 4f),
            size = Size(card.width, card.height),
            cornerRadius = cr
        )

        // Фон карточки
        drawScope.drawRoundRect(
            color = Color.White,
            topLeft = Offset(card.x, card.y),
            size = Size(card.width, card.height),
            cornerRadius = cr
        )

        // Бордер
        drawScope.drawRoundRect(
            color = Color(0xFFE2E8F0),
            topLeft = Offset(card.x, card.y),
            size = Size(card.width, card.height),
            cornerRadius = cr,
            style = Stroke(width = 1.2f)
        )

        // Левая цветная акцентная полоска статуса
        val stripeW = 6f
        drawScope.drawRoundRect(
            color = statusColor,
            topLeft = Offset(card.x, card.y),
            size = Size(stripeW, card.height),
            cornerRadius = CornerRadius(14f, 14f)
        )

        val padLeft = card.x + 16f
        val padRight = card.x + card.width - 16f
        val contentW = card.width - 32f

        // Бейдж статуса
        val statusText = "${card.status.icon} ${card.status.label}"
        val statusLayout = textMeasurer.measure(
            text = statusText,
            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor)
        )
        val badgeH = statusLayout.size.height + 6f
        val badgeW = statusLayout.size.width + 14f
        drawScope.drawRoundRect(
            color = statusColor.copy(alpha = 0.12f),
            topLeft = Offset(padLeft, card.y + 12f),
            size = Size(badgeW, badgeH),
            cornerRadius = CornerRadius(6f, 6f)
        )
        drawScope.drawText(
            textLayoutResult = statusLayout,
            topLeft = Offset(padLeft + 7f, card.y + 15f)
        )

        // Дедлайн
        if (card.dueDate.isNotBlank()) {
            val dueLayout = textMeasurer.measure(
                text = "📅 ${card.dueDate}",
                style = TextStyle(fontSize = 10.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
            )
            drawScope.drawText(
                textLayoutResult = dueLayout,
                topLeft = Offset(padRight - dueLayout.size.width, card.y + 16f)
            )
        }

        // Заголовок вехи
        val titleLayout = textMeasurer.measure(
            text = card.title,
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)),
            constraints = androidx.compose.ui.unit.Constraints(maxWidth = contentW.toInt())
        )
        drawScope.drawText(
            textLayoutResult = titleLayout,
            topLeft = Offset(padLeft, card.y + 44f)
        )

        // Описание вехи
        if (card.description.isNotBlank()) {
            val descLayout = textMeasurer.measure(
                text = card.description,
                style = TextStyle(fontSize = 11.sp, color = Color(0xFF64748B), lineHeight = 15.sp),
                constraints = androidx.compose.ui.unit.Constraints(maxWidth = contentW.toInt())
            )
            drawScope.drawText(
                textLayoutResult = descLayout,
                topLeft = Offset(padLeft, card.y + 68f)
            )
        }

        // Прогресс-бар выполнения
        val barY = card.y + card.height - 38f
        val barW = contentW - 40f
        val barH = 5f
        drawScope.drawRoundRect(
            color = Color(0xFFE2E8F0),
            topLeft = Offset(padLeft, barY),
            size = Size(barW, barH),
            cornerRadius = CornerRadius(3f, 3f)
        )
        val fillW = (barW * card.progress.coerceIn(0f, 1f))
        if (fillW > 0f) {
            drawScope.drawRoundRect(
                color = statusColor,
                topLeft = Offset(padLeft, barY),
                size = Size(fillW, barH),
                cornerRadius = CornerRadius(3f, 3f)
            )
        }
        val percentLayout = textMeasurer.measure(
            text = "${(card.progress.coerceIn(0f, 1f) * 100).toInt()}%",
            style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = statusColor)
        )
        drawScope.drawText(
            textLayoutResult = percentLayout,
            topLeft = Offset(padLeft + barW + 8f, barY - 4f)
        )

        // Теги
        if (card.tags.isNotEmpty()) {
            var tagX = padLeft
            val tagY = card.y + card.height - 24f
            for (tag in card.tags.take(3)) {
                val tagLayout = textMeasurer.measure(
                    text = tag,
                    style = TextStyle(fontSize = 9.sp, color = Color(0xFF475569), fontWeight = FontWeight.Medium)
                )
                val tW = tagLayout.size.width + 10f
                val tH = tagLayout.size.height + 4f
                if (tagX + tW > padRight) break
                drawScope.drawRoundRect(
                    color = Color(0xFFF1F5F9),
                    topLeft = Offset(tagX, tagY),
                    size = Size(tW, tH),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                drawScope.drawText(
                    textLayoutResult = tagLayout,
                    topLeft = Offset(tagX + 5f, tagY + 2f)
                )
                tagX += tW + 6f
            }
        }
    }

    private fun drawFunctionGraph(
        drawScope: DrawScope,
        graph: BoardElement.FunctionGraph,
        textMeasurer: TextMeasurer
    ) {
        val cr = CornerRadius(12f, 12f)

        // Тень
        drawScope.drawRoundRect(
            color = Color.Black.copy(alpha = 0.05f),
            topLeft = Offset(graph.x + 2f, graph.y + 4f),
            size = Size(graph.width, graph.height),
            cornerRadius = cr
        )

        // Фон карточки
        drawScope.drawRoundRect(
            color = Color.White,
            topLeft = Offset(graph.x, graph.y),
            size = Size(graph.width, graph.height),
            cornerRadius = cr
        )
        drawScope.drawRoundRect(
            color = Color(0xFFCBD5E1),
            topLeft = Offset(graph.x, graph.y),
            size = Size(graph.width, graph.height),
            cornerRadius = cr,
            style = Stroke(width = 1.2f)
        )

        // Шапка с формулами
        val headerH = 34f
        drawScope.drawRoundRect(
            color = Color(0xFFF8FAFC),
            topLeft = Offset(graph.x, graph.y),
            size = Size(graph.width, headerH),
            cornerRadius = CornerRadius(12f, 12f)
        )
        drawScope.drawLine(
            color = Color(0xFFE2E8F0),
            start = Offset(graph.x, graph.y + headerH),
            end = Offset(graph.x + graph.width, graph.y + headerH),
            strokeWidth = 1f
        )

        val headerText = graph.expressions.joinToString(", ") { "f(x)=$it" }.ifBlank { graph.title }
        val headerLayout = textMeasurer.measure(
            text = headerText,
            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
        )
        drawScope.drawText(
            textLayoutResult = headerLayout,
            topLeft = Offset(graph.x + 12f, graph.y + 9f)
        )

        // Область построения графика
        val pad = 12f
        val plotLeft = graph.x + pad
        val plotTop = graph.y + headerH + pad
        val plotW = graph.width - pad * 2
        val plotH = graph.height - headerH - pad * 2

        if (plotW <= 10f || plotH <= 10f) return

        val xMin = graph.xMin.toDouble()
        val xMax = graph.xMax.toDouble()
        val yMin = graph.yMin.toDouble()
        val yMax = graph.yMax.toDouble()

        fun toScreenX(xVal: Double): Float {
            return (plotLeft + ((xVal - xMin) / (xMax - xMin)) * plotW).toFloat()
        }

        fun toScreenY(yVal: Double): Float {
            return (plotTop + ((yMax - yVal) / (yMax - yMin)) * plotH).toFloat()
        }

        val originX = toScreenX(0.0).coerceIn(plotLeft, plotLeft + plotW)
        val originY = toScreenY(0.0).coerceIn(plotTop, plotTop + plotH)

        // Сетка, оси и кривые строго обрезаются внутри области графика (clipRect), предотвращая вылезание кривых наружу
        drawScope.clipRect(
            left = plotLeft,
            top = plotTop,
            right = plotLeft + plotW,
            bottom = plotTop + plotH
        ) {
            // Фоновая подложка координатной сетки
            drawRect(
                color = Color(0xFFFBFBFB),
                topLeft = Offset(plotLeft, plotTop),
                size = Size(plotW, plotH)
            )

            // Сетка
            for (gx in kotlin.math.ceil(xMin).toInt()..kotlin.math.floor(xMax).toInt()) {
                if (gx == 0) continue
                val sx = toScreenX(gx.toDouble())
                drawLine(
                    color = Color(0xFFF1F5F9),
                    start = Offset(sx, plotTop),
                    end = Offset(sx, plotTop + plotH),
                    strokeWidth = 1f
                )
            }
            for (gy in kotlin.math.ceil(yMin).toInt()..kotlin.math.floor(yMax).toInt()) {
                if (gy == 0) continue
                val sy = toScreenY(gy.toDouble())
                drawLine(
                    color = Color(0xFFF1F5F9),
                    start = Offset(plotLeft, sy),
                    end = Offset(plotLeft + plotW, sy),
                    strokeWidth = 1f
                )
            }

            // Оси X и Y
            drawLine(
                color = Color(0xFF94A3B8),
                start = Offset(plotLeft, originY),
                end = Offset(plotLeft + plotW, originY),
                strokeWidth = 1.5f
            )
            drawLine(
                color = Color(0xFF94A3B8),
                start = Offset(originX, plotTop),
                end = Offset(originX, plotTop + plotH),
                strokeWidth = 1.5f
            )

            // Отрисовка кривых функций f(x)
            val colors = graph.strokeColorsHex.ifEmpty { listOf(0xFF2563EB) }
            graph.expressions.forEachIndexed { exprIdx, expr ->
                if (expr.isBlank()) return@forEachIndexed
                val curveColor = Color(colors[exprIdx % colors.size])
                val samples = MathEvaluator.sampleFunction(expr, xMin, xMax, steps = 300)

                val curvePath = Path()
                var inPath = false

                for ((sxVal, syVal) in samples) {
                    if (syVal == null || syVal.isNaN() || syVal.isInfinite() || syVal < yMin - 100 || syVal > yMax + 100) {
                        inPath = false
                        continue
                    }
                    val px = toScreenX(sxVal)
                    val py = toScreenY(syVal)

                    if (!inPath) {
                        curvePath.moveTo(px, py)
                        inPath = true
                    } else {
                        curvePath.lineTo(px, py)
                    }
                }

                drawPath(
                    path = curvePath,
                    color = curveColor,
                    style = Stroke(width = 2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}

