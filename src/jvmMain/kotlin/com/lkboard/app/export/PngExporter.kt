package com.lkboard.app.export

import com.lkboard.app.model.BoardElement
import com.lkboard.app.model.BoardState
import com.lkboard.app.model.ConnectorStyle
import com.lkboard.app.model.GeometryUtils
import com.lkboard.app.model.ShapeType
import com.lkboard.app.model.getAnchorOffset
import com.lkboard.app.util.MathEvaluator
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Font
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.Path
import org.jetbrains.skia.RRect
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import org.jetbrains.skia.Typeface
import java.io.File
import kotlin.math.max

/**
 * Высокопроизводительный экспортер сцены доски в растровое изображение PNG с помощью Skia.
 */
object PngExporter {

    private const val PADDING = 40f

    /**
     * Рендерит состояние доски в java.awt.image.BufferedImage с помощью Skia.
     */
    fun renderBoardToBufferedImage(
        state: BoardState,
        onlySelected: Boolean = false
    ): java.awt.image.BufferedImage? {
        val elementsToRender = if (onlySelected && state.selectedElements.isNotEmpty()) {
            state.selectedElements
        } else {
            state.sortedElements
        }

        if (elementsToRender.isEmpty()) return null

        val contentBounds = GeometryUtils.computeBounds(elementsToRender) ?: return null
        val exportWidth = max(100, (contentBounds.width + PADDING * 2).toInt())
        val exportHeight = max(100, (contentBounds.height + PADDING * 2).toInt())

        val surface = Surface.makeRasterN32Premul(exportWidth, exportHeight)
        val canvas = surface.canvas
        canvas.clear(state.backgroundColorHex.toInt())
        canvas.translate(-contentBounds.left + PADDING, -contentBounds.top + PADDING)

        val defaultTypeface = Typeface.makeDefault()

        for (element in elementsToRender) {
            val hasRotation = element.rotation != 0f
            if (hasRotation) {
                canvas.save()
                canvas.rotate(element.rotation, element.x + element.width / 2f, element.y + element.height / 2f)
            }
            when (element) {
                is BoardElement.StickyNote -> renderStickyNote(canvas, element, defaultTypeface)
                is BoardElement.Shape -> renderShape(canvas, element)
                is BoardElement.Text -> renderText(canvas, element, defaultTypeface)
                is BoardElement.DrawingPath -> renderDrawingPath(canvas, element)
                is BoardElement.ImageElement -> renderImageElement(canvas, element)
                is BoardElement.DocumentFrame -> renderDocumentFrame(canvas, element, defaultTypeface)
                is BoardElement.Connector -> renderConnector(canvas, element, state)
                is BoardElement.MilestoneCard -> renderMilestoneCard(canvas, element, defaultTypeface)
                is BoardElement.FunctionGraph -> renderFunctionGraph(canvas, element, defaultTypeface)
                is BoardElement.LatexFormula -> com.lkboard.app.util.LatexRenderer.renderSkia(canvas, element, defaultTypeface)
                is BoardElement.PhysicsStencil -> com.lkboard.app.util.PhysicsStencilRenderer.renderSkia(canvas, element, defaultTypeface)
                is BoardElement.KanbanBoard -> renderKanbanBoard(canvas, element, defaultTypeface, state)
            }
            if (hasRotation) {
                canvas.restore()
            }
        }

        val image = surface.makeImageSnapshot()
        val data = image.encodeToData(EncodedImageFormat.PNG, 100) ?: return null
        return javax.imageio.ImageIO.read(java.io.ByteArrayInputStream(data.bytes))
    }

    /**
     * Экспортирует доску [state] (или только выбранные элементы [onlySelected]) в PNG-файл [targetFile].
     */
    fun exportToPng(
        targetFile: File,
        state: BoardState,
        onlySelected: Boolean = false
    ): Boolean {
        val bufferedImage = renderBoardToBufferedImage(state, onlySelected) ?: return false
        return javax.imageio.ImageIO.write(bufferedImage, "PNG", targetFile)
    }

    private fun renderStickyNote(
        canvas: org.jetbrains.skia.Canvas,
        note: BoardElement.StickyNote,
        typeface: Typeface
    ) {
        val rrect = RRect.makeXYWH(note.x, note.y, note.width, note.height, 8f)

        // Тень стикера
        val shadowPaint = Paint().apply {
            color = 0x22000000.toInt()
            mode = PaintMode.FILL
            isAntiAlias = true
        }
        canvas.drawRRect(RRect.makeXYWH(note.x + 3f, note.y + 4f, note.width, note.height, 8f), shadowPaint)

        // Фон стикера
        val fillPaint = Paint().apply {
            color = note.colorHex.toInt()
            mode = PaintMode.FILL
            isAntiAlias = true
        }
        canvas.drawRRect(rrect, fillPaint)

        // Окантовка
        val strokePaint = Paint().apply {
            color = 0x14000000.toInt()
            mode = PaintMode.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }
        canvas.drawRRect(rrect, strokePaint)

        // Текст стикера
        if (note.text.isNotBlank()) {
            val font = Font(typeface, note.fontSize)
            val textPaint = Paint().apply {
                color = 0xFF263238.toInt()
                isAntiAlias = true
            }

            val lines = note.text.lines()
            var yOffset = note.y + note.fontSize + 14f
            val lineHeight = note.fontSize * 1.35f

            for (line in lines) {
                if (yOffset > note.y + note.height - 10f) break
                canvas.drawString(line, note.x + 14f, yOffset, font, textPaint)
                yOffset += lineHeight
            }
        }
    }

    private fun renderShape(canvas: org.jetbrains.skia.Canvas, shape: BoardElement.Shape) {
        val strokePaint = Paint().apply {
            color = shape.strokeColorHex.toInt()
            mode = PaintMode.STROKE
            strokeWidth = shape.strokeWidth
            isAntiAlias = true
        }

        val fillPaint = shape.fillColorHex?.let { fill ->
            Paint().apply {
                color = fill.toInt()
                mode = PaintMode.FILL
                isAntiAlias = true
            }
        }

        when (shape.shapeType) {
            ShapeType.RECTANGLE -> {
                val rect = Rect.makeXYWH(shape.x, shape.y, shape.width, shape.height)
                fillPaint?.let { canvas.drawRect(rect, it) }
                canvas.drawRect(rect, strokePaint)
            }
            ShapeType.ROUNDED_RECTANGLE -> {
                val rrect = RRect.makeXYWH(shape.x, shape.y, shape.width, shape.height, shape.cornerRadius)
                fillPaint?.let { canvas.drawRRect(rrect, it) }
                canvas.drawRRect(rrect, strokePaint)
            }
            ShapeType.ELLIPSE -> {
                val oval = Rect.makeXYWH(shape.x, shape.y, shape.width, shape.height)
                fillPaint?.let { canvas.drawOval(oval, it) }
                canvas.drawOval(oval, strokePaint)
            }
            ShapeType.DIAMOND -> {
                val path = Path().apply {
                    moveTo(shape.x + shape.width / 2f, shape.y)
                    lineTo(shape.x + shape.width, shape.y + shape.height / 2f)
                    lineTo(shape.x + shape.width / 2f, shape.y + shape.height)
                    lineTo(shape.x, shape.y + shape.height / 2f)
                    closePath()
                }
                fillPaint?.let { canvas.drawPath(path, it) }
                canvas.drawPath(path, strokePaint)
            }
            ShapeType.ARROW -> {
                val startY = shape.y + shape.height / 2f
                canvas.drawLine(shape.x, startY, shape.x + shape.width, startY, strokePaint)

                val arrowHeadSize = 14f
                val headPath = Path().apply {
                    moveTo(shape.x + shape.width, startY)
                    lineTo(shape.x + shape.width - arrowHeadSize, startY - arrowHeadSize * 0.6f)
                    lineTo(shape.x + shape.width - arrowHeadSize, startY + arrowHeadSize * 0.6f)
                    closePath()
                }
                val arrowHeadPaint = Paint().apply {
                    color = shape.strokeColorHex.toInt()
                    mode = PaintMode.FILL
                    isAntiAlias = true
                }
                canvas.drawPath(headPath, arrowHeadPaint)
            }
            ShapeType.FRAME -> {
                val rrect = RRect.makeXYWH(shape.x, shape.y, shape.width, shape.height, 4f)
                val whitePaint = Paint().apply {
                    color = 0xFFFFFFFF.toInt()
                    mode = PaintMode.FILL
                }
                canvas.drawRRect(rrect, whitePaint)
                canvas.drawRRect(rrect, strokePaint)
            }
            ShapeType.TRIANGLE -> {
                val path = Path().apply {
                    moveTo(shape.x + shape.width / 2f, shape.y)
                    lineTo(shape.x + shape.width, shape.y + shape.height)
                    lineTo(shape.x, shape.y + shape.height)
                    closePath()
                }
                fillPaint?.let { canvas.drawPath(path, it) }
                canvas.drawPath(path, strokePaint)
            }
            ShapeType.DOUBLE_ARROW -> {
                val startY = shape.y + shape.height / 2f
                canvas.drawLine(shape.x, startY, shape.x + shape.width, startY, strokePaint)

                val arrowHeadSize = 14f
                val arrowHeadPaint = Paint().apply {
                    color = shape.strokeColorHex.toInt()
                    mode = PaintMode.FILL
                    isAntiAlias = true
                }
                val rightHead = Path().apply {
                    moveTo(shape.x + shape.width, startY)
                    lineTo(shape.x + shape.width - arrowHeadSize, startY - arrowHeadSize * 0.6f)
                    lineTo(shape.x + shape.width - arrowHeadSize, startY + arrowHeadSize * 0.6f)
                    closePath()
                }
                canvas.drawPath(rightHead, arrowHeadPaint)

                val leftHead = Path().apply {
                    moveTo(shape.x, startY)
                    lineTo(shape.x + arrowHeadSize, startY - arrowHeadSize * 0.6f)
                    lineTo(shape.x + arrowHeadSize, startY + arrowHeadSize * 0.6f)
                    closePath()
                }
                canvas.drawPath(leftHead, arrowHeadPaint)
            }
            ShapeType.PARALLELOGRAM -> {
                val slant = (shape.width * 0.22f).coerceAtMost(shape.height * 0.8f)
                val path = Path().apply {
                    moveTo(shape.x + slant, shape.y)
                    lineTo(shape.x + shape.width, shape.y)
                    lineTo(shape.x + shape.width - slant, shape.y + shape.height)
                    lineTo(shape.x, shape.y + shape.height)
                    closePath()
                }
                fillPaint?.let { canvas.drawPath(path, it) }
                canvas.drawPath(path, strokePaint)
            }
            ShapeType.HEXAGON -> {
                val w4 = shape.width * 0.25f
                val path = Path().apply {
                    moveTo(shape.x + w4, shape.y)
                    lineTo(shape.x + shape.width - w4, shape.y)
                    lineTo(shape.x + shape.width, shape.y + shape.height / 2f)
                    lineTo(shape.x + shape.width - w4, shape.y + shape.height)
                    lineTo(shape.x + w4, shape.y + shape.height)
                    lineTo(shape.x, shape.y + shape.height / 2f)
                    closePath()
                }
                fillPaint?.let { canvas.drawPath(path, it) }
                canvas.drawPath(path, strokePaint)
            }
            ShapeType.STAR -> {
                val cx = shape.x + shape.width / 2f
                val cy = shape.y + shape.height / 2f
                val outerR = kotlin.math.min(shape.width, shape.height) / 2f
                val innerR = outerR * 0.40f
                val path = Path()
                for (i in 0 until 10) {
                    val angle = (i * 36 - 90) * (kotlin.math.PI / 180.0)
                    val r = if (i % 2 == 0) outerR else innerR
                    val px = (cx + r * kotlin.math.cos(angle)).toFloat()
                    val py = (cy + r * kotlin.math.sin(angle)).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.closePath()
                fillPaint?.let { canvas.drawPath(path, it) }
                canvas.drawPath(path, strokePaint)
            }
            ShapeType.SPEECH_BUBBLE -> {
                val tailH = (shape.height * 0.22f).coerceIn(12f, 32f)
                val bodyH = shape.height - tailH
                val rrect = RRect.makeXYWH(shape.x, shape.y, shape.width, bodyH, 12f)
                fillPaint?.let { canvas.drawRRect(rrect, it) }
                canvas.drawRRect(rrect, strokePaint)

                val tailPath = Path().apply {
                    moveTo(shape.x + shape.width * 0.22f, shape.y + bodyH - 1f)
                    lineTo(shape.x + shape.width * 0.12f, shape.y + shape.height)
                    lineTo(shape.x + shape.width * 0.38f, shape.y + bodyH - 1f)
                    closePath()
                }
                fillPaint?.let { canvas.drawPath(tailPath, it) }
                canvas.drawPath(tailPath, strokePaint)
            }
            ShapeType.CLOUD -> {
                val path = Path().apply {
                    moveTo(shape.x + shape.width * 0.20f, shape.y + shape.height * 0.70f)
                    cubicTo(shape.x, shape.y + shape.height * 0.70f, shape.x, shape.y + shape.height * 0.40f, shape.x + shape.width * 0.20f, shape.y + shape.height * 0.35f)
                    cubicTo(shape.x + shape.width * 0.15f, shape.y + shape.height * 0.12f, shape.x + shape.width * 0.45f, shape.y + shape.height * 0.05f, shape.x + shape.width * 0.50f, shape.y + shape.height * 0.25f)
                    cubicTo(shape.x + shape.width * 0.60f, shape.y + shape.height * 0.08f, shape.x + shape.width * 0.85f, shape.y + shape.height * 0.15f, shape.x + shape.width * 0.85f, shape.y + shape.height * 0.35f)
                    cubicTo(shape.x + shape.width, shape.y + shape.height * 0.40f, shape.x + shape.width, shape.y + shape.height * 0.70f, shape.x + shape.width * 0.80f, shape.y + shape.height * 0.70f)
                    cubicTo(shape.x + shape.width * 0.85f, shape.y + shape.height * 0.95f, shape.x + shape.width * 0.65f, shape.y + shape.height, shape.x + shape.width * 0.50f, shape.y + shape.height * 0.85f)
                    cubicTo(shape.x + shape.width * 0.35f, shape.y + shape.height, shape.x + shape.width * 0.15f, shape.y + shape.height * 0.95f, shape.x + shape.width * 0.20f, shape.y + shape.height * 0.70f)
                    closePath()
                }
                fillPaint?.let { canvas.drawPath(path, it) }
                canvas.drawPath(path, strokePaint)
            }
            ShapeType.CUBE_3D -> {
                val depth = kotlin.math.min(shape.width, shape.height) * 0.26f
                val frontW = shape.width - depth
                val frontH = shape.height - depth

                val topPath = Path().apply {
                    moveTo(shape.x, shape.y + depth)
                    lineTo(shape.x + depth, shape.y)
                    lineTo(shape.x + shape.width, shape.y)
                    lineTo(shape.x + frontW, shape.y + depth)
                    closePath()
                }
                fillPaint?.let { canvas.drawPath(topPath, it) }
                canvas.drawPath(topPath, strokePaint)

                val rightPath = Path().apply {
                    moveTo(shape.x + frontW, shape.y + depth)
                    lineTo(shape.x + shape.width, shape.y)
                    lineTo(shape.x + shape.width, shape.y + frontH)
                    lineTo(shape.x + frontW, shape.y + shape.height)
                    closePath()
                }
                fillPaint?.let { canvas.drawPath(rightPath, it) }
                canvas.drawPath(rightPath, strokePaint)

                val frontRect = Rect.makeXYWH(shape.x, shape.y + depth, frontW, frontH)
                fillPaint?.let { canvas.drawRect(frontRect, it) }
                canvas.drawRect(frontRect, strokePaint)
            }
            ShapeType.CYLINDER_3D -> {
                val capH = (shape.height * 0.22f).coerceIn(12f, 40f)
                val body = Path().apply {
                    moveTo(shape.x, shape.y + capH / 2f)
                    lineTo(shape.x + shape.width, shape.y + capH / 2f)
                    lineTo(shape.x + shape.width, shape.y + shape.height - capH / 2f)
                    cubicTo(shape.x + shape.width, shape.y + shape.height + capH * 0.45f, shape.x, shape.y + shape.height + capH * 0.45f, shape.x, shape.y + shape.height - capH / 2f)
                    closePath()
                }
                fillPaint?.let { canvas.drawPath(body, it) }

                val outline = Path().apply {
                    moveTo(shape.x, shape.y + capH / 2f)
                    lineTo(shape.x, shape.y + shape.height - capH / 2f)
                    cubicTo(shape.x, shape.y + shape.height + capH * 0.45f, shape.x + shape.width, shape.y + shape.height + capH * 0.45f, shape.x + shape.width, shape.y + shape.height - capH / 2f)
                    lineTo(shape.x + shape.width, shape.y + capH / 2f)
                }
                canvas.drawPath(outline, strokePaint)

                val topOval = Rect.makeXYWH(shape.x, shape.y, shape.width, capH)
                fillPaint?.let { canvas.drawOval(topOval, it) }
                canvas.drawOval(topOval, strokePaint)
            }
            ShapeType.CONE_3D -> {
                val capH = (shape.height * 0.22f).coerceIn(12f, 36f)
                val apexX = shape.x + shape.width / 2f
                val apexY = shape.y
                val cone = Path().apply {
                    moveTo(apexX, apexY)
                    lineTo(shape.x + shape.width, shape.y + shape.height - capH / 2f)
                    cubicTo(shape.x + shape.width, shape.y + shape.height + capH * 0.45f, shape.x, shape.y + shape.height + capH * 0.45f, shape.x, shape.y + shape.height - capH / 2f)
                    closePath()
                }
                fillPaint?.let { canvas.drawPath(cone, it) }
                val outline = Path().apply {
                    moveTo(apexX, apexY)
                    lineTo(shape.x, shape.y + shape.height - capH / 2f)
                    cubicTo(shape.x, shape.y + shape.height + capH * 0.45f, shape.x + shape.width, shape.y + shape.height + capH * 0.45f, shape.x + shape.width, shape.y + shape.height - capH / 2f)
                    lineTo(apexX, apexY)
                }
                canvas.drawPath(outline, strokePaint)
            }
            ShapeType.PYRAMID_3D -> {
                val apexX = shape.x + shape.width * 0.48f
                val apexY = shape.y
                val bottomMidX = shape.x + shape.width * 0.60f
                val bottomMidY = shape.y + shape.height

                val leftFace = Path().apply {
                    moveTo(apexX, apexY)
                    lineTo(shape.x, shape.y + shape.height - 10f)
                    lineTo(bottomMidX, bottomMidY)
                    closePath()
                }
                fillPaint?.let { canvas.drawPath(leftFace, it) }
                canvas.drawPath(leftFace, strokePaint)

                val rightFace = Path().apply {
                    moveTo(apexX, apexY)
                    lineTo(bottomMidX, bottomMidY)
                    lineTo(shape.x + shape.width, shape.y + shape.height - 16f)
                    closePath()
                }
                fillPaint?.let { canvas.drawPath(rightFace, it) }
                canvas.drawPath(rightFace, strokePaint)
            }
            ShapeType.SPHERE_3D -> {
                val sizeVal = kotlin.math.min(shape.width, shape.height)
                val left = shape.x + (shape.width - sizeVal) / 2f
                val top = shape.y + (shape.height - sizeVal) / 2f
                val oval = Rect.makeXYWH(left, top, sizeVal, sizeVal)
                fillPaint?.let { canvas.drawOval(oval, it) }
                canvas.drawOval(oval, strokePaint)
            }
        }
    }

    private fun renderDocumentFrame(
        canvas: org.jetbrains.skia.Canvas,
        frame: BoardElement.DocumentFrame,
        typeface: Typeface
    ) {
        val rrect = RRect.makeXYWH(frame.x, frame.y, frame.width, frame.height, 8f)
        val shadow = Paint().apply {
            color = 0x22000000.toInt()
            mode = PaintMode.FILL
            isAntiAlias = true
        }
        canvas.drawRRect(RRect.makeXYWH(frame.x + 3f, frame.y + 4f, frame.width, frame.height, 8f), shadow)

        val fillPaint = Paint().apply {
            color = frame.backgroundColorHex.toInt()
            mode = PaintMode.FILL
            isAntiAlias = true
        }
        canvas.drawRRect(rrect, fillPaint)

        // Шапка фрейма
        val headerH = 34f
        val headerPaint = Paint().apply {
            color = 0xFFF1F5F9.toInt()
            mode = PaintMode.FILL
            isAntiAlias = true
        }
        canvas.drawRRect(RRect.makeXYWH(frame.x, frame.y, frame.width, headerH, 8f), headerPaint)

        // Текст заголовка
        val font = Font(typeface, 13f)
        val textPaint = Paint().apply {
            color = 0xFF1E293B.toInt()
            isAntiAlias = true
        }
        canvas.drawString(frame.title, frame.x + 12f, frame.y + 22f, font, textPaint)

        // Слайд
        val base64 = frame.currentPageBase64
        if (!base64.isNullOrBlank()) {
            try {
                val bytes = java.util.Base64.getDecoder().decode(base64)
                val skiaImg = org.jetbrains.skia.Image.makeFromEncoded(bytes)
                val dst = Rect.makeXYWH(frame.x, frame.y + headerH, frame.width, frame.height - headerH)
                canvas.drawImageRect(skiaImg, dst)
            } catch (_: Exception) {}
        }

        // Окантовка
        val strokePaint = Paint().apply {
            color = 0x33000000.toInt()
            mode = PaintMode.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }
        canvas.drawRRect(rrect, strokePaint)
    }

    private fun renderText(
        canvas: org.jetbrains.skia.Canvas,
        textElem: BoardElement.Text,
        typeface: Typeface
    ) {
        val font = Font(typeface, textElem.fontSize)
        val textPaint = Paint().apply {
            color = textElem.colorHex.toInt()
            isAntiAlias = true
        }

        val lines = textElem.text.lines()
        var yOffset = textElem.y + textElem.fontSize
        val lineHeight = textElem.fontSize * 1.3f

        for (line in lines) {
            canvas.drawString(line, textElem.x, yOffset, font, textPaint)
            yOffset += lineHeight
        }
    }

    private fun renderDrawingPath(
        canvas: org.jetbrains.skia.Canvas,
        drawingPath: BoardElement.DrawingPath
    ) {
        if (drawingPath.points.isEmpty()) return

        val color = if (drawingPath.isHighlighter) {
            (drawingPath.strokeColorHex and 0x00FFFFFF) or 0x55000000
        } else {
            drawingPath.strokeColorHex
        }

        if (drawingPath.points.size == 1) {
            val circlePaint = Paint().apply {
                this.color = color.toInt()
                mode = PaintMode.FILL
                isAntiAlias = true
            }
            val radius = (drawingPath.strokeWidth / 2f).coerceAtLeast(1.5f)
            canvas.drawCircle(drawingPath.points[0].x, drawingPath.points[0].y, radius, circlePaint)
            return
        }

        val path = Path().apply {
            val first = drawingPath.points.first()
            moveTo(first.x, first.y)
            if (drawingPath.points.size == 2) {
                lineTo(drawingPath.points[1].x, drawingPath.points[1].y)
            } else {
                val mid0X = (drawingPath.points[0].x + drawingPath.points[1].x) / 2f
                val mid0Y = (drawingPath.points[0].y + drawingPath.points[1].y) / 2f
                lineTo(mid0X, mid0Y)
                for (i in 1 until drawingPath.points.size - 1) {
                    val curr = drawingPath.points[i]
                    val next = drawingPath.points[i + 1]
                    val nextMidX = (curr.x + next.x) / 2f
                    val nextMidY = (curr.y + next.y) / 2f
                    quadTo(curr.x, curr.y, nextMidX, nextMidY)
                }
                lineTo(drawingPath.points.last().x, drawingPath.points.last().y)
            }
        }

        val strokePaint = Paint().apply {
            this.color = color.toInt()
            mode = PaintMode.STROKE
            strokeWidth = drawingPath.strokeWidth
            strokeCap = org.jetbrains.skia.PaintStrokeCap.ROUND
            strokeJoin = org.jetbrains.skia.PaintStrokeJoin.ROUND
            isAntiAlias = true
        }

        canvas.drawPath(path, strokePaint)
    }

    private fun renderImageElement(canvas: org.jetbrains.skia.Canvas, img: BoardElement.ImageElement) {
        try {
            val bytes = java.util.Base64.getDecoder().decode(img.imageBase64)
            val skiaImage = org.jetbrains.skia.Image.makeFromEncoded(bytes)
            val dst = Rect.makeXYWH(img.x, img.y, img.width, img.height)
            canvas.drawImageRect(skiaImage, dst)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun renderConnector(
        canvas: org.jetbrains.skia.Canvas,
        conn: BoardElement.Connector,
        state: BoardState
    ) {
        val startOffset = conn.startElementId?.let { id ->
            state.elements[id]?.getAnchorOffset(conn.startAnchor)
        } ?: conn.startPoint.toOffset()
        val endOffset = conn.endElementId?.let { id ->
            state.elements[id]?.getAnchorOffset(conn.endAnchor)
        } ?: conn.endPoint.toOffset()

        val strokePaint = Paint().apply {
            color = conn.strokeColorHex.toInt()
            mode = PaintMode.STROKE
            strokeWidth = conn.strokeWidth
            strokeCap = org.jetbrains.skia.PaintStrokeCap.ROUND
            strokeJoin = org.jetbrains.skia.PaintStrokeJoin.ROUND
            isAntiAlias = true
        }

        val path = Path().apply {
            moveTo(startOffset.x, startOffset.y)
            when (conn.style) {
                ConnectorStyle.STRAIGHT -> lineTo(endOffset.x, endOffset.y)
                ConnectorStyle.ORTHOGONAL -> {
                    val midX = (startOffset.x + endOffset.x) / 2f
                    lineTo(midX, startOffset.y)
                    lineTo(midX, endOffset.y)
                    lineTo(endOffset.x, endOffset.y)
                }
                ConnectorStyle.BEZIER -> {
                    val dx = (endOffset.x - startOffset.x) * 0.5f
                    cubicTo(startOffset.x + dx, startOffset.y, endOffset.x - dx, endOffset.y, endOffset.x, endOffset.y)
                }
            }
        }
        canvas.drawPath(path, strokePaint)

        // Стрелка на конце
        if (conn.hasEndArrow) {
            val headLen = (conn.strokeWidth * 3.5f + 6f).coerceIn(10f, 22f)
            val angle = kotlin.math.atan2((endOffset.y - startOffset.y).toDouble(), (endOffset.x - startOffset.x).toDouble())
            val wingAngle = 0.45
            val p1x = (endOffset.x - headLen * kotlin.math.cos(angle - wingAngle)).toFloat()
            val p1y = (endOffset.y - headLen * kotlin.math.sin(angle - wingAngle)).toFloat()
            val p2x = (endOffset.x - headLen * kotlin.math.cos(angle + wingAngle)).toFloat()
            val p2y = (endOffset.y - headLen * kotlin.math.sin(angle + wingAngle)).toFloat()

            val arrowPath = Path().apply {
                moveTo(endOffset.x, endOffset.y)
                lineTo(p1x, p1y)
                lineTo(p2x, p2y)
                closePath()
            }
            val fillPaint = Paint().apply {
                color = conn.strokeColorHex.toInt()
                mode = PaintMode.FILL
                isAntiAlias = true
            }
            canvas.drawPath(arrowPath, fillPaint)
        }
    }

    private fun renderMilestoneCard(
        canvas: org.jetbrains.skia.Canvas,
        card: BoardElement.MilestoneCard,
        typeface: Typeface
    ) {
        val rrect = RRect.makeXYWH(card.x, card.y, card.width, card.height, 14f)

        // Тень
        val shadow = Paint().apply {
            color = 0x18000000.toInt()
            mode = PaintMode.FILL
            isAntiAlias = true
        }
        canvas.drawRRect(RRect.makeXYWH(card.x + 2f, card.y + 4f, card.width, card.height, 14f), shadow)

        // Фон
        val bgPaint = Paint().apply {
            color = 0xFFFFFFFF.toInt()
            mode = PaintMode.FILL
            isAntiAlias = true
        }
        canvas.drawRRect(rrect, bgPaint)

        // Бордер
        val borderPaint = Paint().apply {
            color = 0xFFE2E8F0.toInt()
            mode = PaintMode.STROKE
            strokeWidth = 1.2f
            isAntiAlias = true
        }
        canvas.drawRRect(rrect, borderPaint)

        // Цветная полоска статуса
        val stripeColor = card.status.colorHex.toInt()
        val stripePaint = Paint().apply {
            color = stripeColor
            mode = PaintMode.FILL
            isAntiAlias = true
        }
        canvas.drawRRect(RRect.makeXYWH(card.x, card.y, 6f, card.height, 3f), stripePaint)

        // Бейдж статуса
        val badgeBg = Paint().apply {
            color = (stripeColor and 0x00FFFFFF) or 0x22000000
            mode = PaintMode.FILL
            isAntiAlias = true
        }
        canvas.drawRRect(RRect.makeXYWH(card.x + 16f, card.y + 12f, 95f, 22f, 6f), badgeBg)

        val statusFont = Font(typeface, 11f)
        val statusTextPaint = Paint().apply {
            color = stripeColor
            isAntiAlias = true
        }
        canvas.drawString("${card.status.icon} ${card.status.label}", card.x + 22f, card.y + 27f, statusFont, statusTextPaint)

        // Заголовок
        val titleFont = Font(typeface, 14f)
        val titlePaint = Paint().apply {
            color = 0xFF0F172A.toInt()
            isAntiAlias = true
        }
        canvas.drawString(card.title, card.x + 16f, card.y + 56f, titleFont, titlePaint)

        // Описание
        if (card.description.isNotBlank()) {
            val descFont = Font(typeface, 11f)
            val descPaint = Paint().apply {
                color = 0xFF64748B.toInt()
                isAntiAlias = true
            }
            val shortDesc = if (card.description.length > 40) card.description.take(37) + "..." else card.description
            canvas.drawString(shortDesc, card.x + 16f, card.y + 78f, descFont, descPaint)
        }

        // Прогресс бар
        val barY = card.y + card.height - 30f
        val barW = card.width - 65f
        val barBg = Paint().apply {
            color = 0xFFE2E8F0.toInt()
            mode = PaintMode.FILL
            isAntiAlias = true
        }
        canvas.drawRRect(RRect.makeXYWH(card.x + 16f, barY, barW, 5f, 2.5f), barBg)

        val fillW = barW * card.progress.coerceIn(0f, 1f)
        if (fillW > 0f) {
            canvas.drawRRect(RRect.makeXYWH(card.x + 16f, barY, fillW, 5f, 2.5f), stripePaint)
        }

        val pctFont = Font(typeface, 10f)
        val pctPaint = Paint().apply {
            color = stripeColor
            isAntiAlias = true
        }
        canvas.drawString("${(card.progress * 100).toInt()}%", card.x + 16f + barW + 8f, barY + 5f, pctFont, pctPaint)
    }

    private fun renderFunctionGraph(
        canvas: org.jetbrains.skia.Canvas,
        graph: BoardElement.FunctionGraph,
        typeface: Typeface
    ) {
        val gScale = (graph.width / 440f).coerceIn(0.6f, 3.5f)
        val rrect = RRect.makeXYWH(graph.x, graph.y, graph.width, graph.height, 12f * gScale)

        // Фон
        val bgPaint = Paint().apply {
            color = 0xFFFFFFFF.toInt()
            mode = PaintMode.FILL
            isAntiAlias = true
        }
        canvas.drawRRect(rrect, bgPaint)

        // Окантовка
        val borderPaint = Paint().apply {
            color = 0xFFCBD5E1.toInt()
            mode = PaintMode.STROKE
            strokeWidth = 1.2f * gScale
            isAntiAlias = true
        }
        canvas.drawRRect(rrect, borderPaint)

        // Шапка
        val headerH = (34f * gScale).coerceAtLeast(26f)
        val headerPaint = Paint().apply {
            color = 0xFFF8FAFC.toInt()
            mode = PaintMode.FILL
            isAntiAlias = true
        }
        canvas.drawRRect(RRect.makeXYWH(graph.x, graph.y, graph.width, headerH, 12f * gScale), headerPaint)

        val headerText = graph.expressions.joinToString(", ") { "f(x)=$it" }.ifBlank { graph.title }
        val headerFont = Font(typeface, 11.5f * gScale)
        val headerTextPaint = Paint().apply {
            color = 0xFF1E293B.toInt()
            isAntiAlias = true
        }
        canvas.drawString(headerText, graph.x + 12f * gScale, graph.y + headerH * 0.65f, headerFont, headerTextPaint)

        // Область построения
        val pad = 12f * gScale
        val plotL = graph.x + pad
        val plotT = graph.y + headerH + pad
        val plotW = graph.width - pad * 2
        val plotH = graph.height - headerH - pad * 2
        if (plotW <= 10f || plotH <= 10f) return

        canvas.save()
        canvas.clipRect(Rect.makeXYWH(plotL, plotT, plotW, plotH))

        val xMin = graph.xMin.toDouble()
        val xMax = graph.xMax.toDouble()
        val yMin = graph.yMin.toDouble()
        val yMax = graph.yMax.toDouble()

        fun toX(v: Double): Float = (plotL + ((v - xMin) / (xMax - xMin)) * plotW).toFloat()
        fun toY(v: Double): Float = (plotT + ((yMax - v) / (yMax - yMin)) * plotH).toFloat()

        val originX = toX(0.0).coerceIn(plotL, plotL + plotW)
        val originY = toY(0.0).coerceIn(plotT, plotT + plotH)

        // Оси
        val axisPaint = Paint().apply {
            color = 0xFF94A3B8.toInt()
            mode = PaintMode.STROKE
            strokeWidth = 1.5f * gScale
            isAntiAlias = true
        }
        canvas.drawLine(plotL, originY, plotL + plotW, originY, axisPaint)
        canvas.drawLine(originX, plotT, originX, plotT + plotH, axisPaint)

        // Число 0
        val numFont = Font(typeface, 9f * gScale)
        val numPaint = Paint().apply {
            color = 0xFF64748B.toInt()
            isAntiAlias = true
        }
        canvas.drawString("0", originX + 4f, originY + 12f * gScale, numFont, numPaint)

        // Кривые
        val colors = graph.strokeColorsHex.ifEmpty { listOf(0xFF2563EBL) }
        graph.expressions.forEachIndexed { idx, expr ->
            if (expr.isBlank()) return@forEachIndexed
            val curveColor = colors[idx % colors.size].toInt()
            val curvePaint = Paint().apply {
                color = curveColor
                mode = PaintMode.STROKE
                strokeWidth = 2.4f * gScale
                strokeCap = org.jetbrains.skia.PaintStrokeCap.ROUND
                strokeJoin = org.jetbrains.skia.PaintStrokeJoin.ROUND
                isAntiAlias = true
            }

            val samples = MathEvaluator.sampleFunction(expr, xMin, xMax, 250)
            val curvePath = Path()
            var inPath = false
            for ((sx, sy) in samples) {
                if (sy == null || sy.isNaN() || sy.isInfinite() || sy < yMin - 100 || sy > yMax + 100) {
                    inPath = false
                    continue
                }
                val px = toX(sx)
                val py = toY(sy)
                if (!inPath) {
                    curvePath.moveTo(px, py)
                    inPath = true
                } else {
                    curvePath.lineTo(px, py)
                }
            }
            canvas.drawPath(curvePath, curvePaint)
        }

        canvas.restore()
    }

    private fun renderKanbanBoard(
        canvas: org.jetbrains.skia.Canvas,
        kanban: BoardElement.KanbanBoard,
        typeface: Typeface,
        state: BoardState
    ) {
        val kScale = (kanban.width / 1000f).coerceIn(0.6f, 3.5f)
        val rrect = RRect.makeXYWH(kanban.x, kanban.y, kanban.width, kanban.height, 16f * kScale)

        // Фон
        val bgPaint = Paint().apply {
            color = 0xFFF8FAFC.toInt()
            mode = PaintMode.FILL
            isAntiAlias = true
        }
        canvas.drawRRect(rrect, bgPaint)

        val borderPaint = Paint().apply {
            color = 0xFFE2E8F0.toInt()
            mode = PaintMode.STROKE
            strokeWidth = 1.5f * kScale
            isAntiAlias = true
        }
        canvas.drawRRect(rrect, borderPaint)

        // Шапка
        val headerH = (46f * kScale).coerceAtLeast(36f)
        val headerPaint = Paint().apply {
            color = 0xFFF1F5F9.toInt()
            mode = PaintMode.FILL
            isAntiAlias = true
        }
        canvas.drawRRect(RRect.makeXYWH(kanban.x, kanban.y, kanban.width, headerH, 16f * kScale), headerPaint)

        val titleFont = Font(typeface, 15f * kScale)
        val titlePaint = Paint().apply {
            color = 0xFF0F172A.toInt()
            isAntiAlias = true
        }
        canvas.drawString("📋 ${kanban.title}", kanban.x + 18f * kScale, kanban.y + headerH * 0.65f, titleFont, titlePaint)

        // 4 Колонки
        val cols = listOf("🔴 Бэклог", "🟡 В работе", "🟣 На ревью", "🟢 Готово")
        val colW = kanban.width / 4f
        val colH = kanban.height - headerH
        val colHeadH = (38f * kScale).coerceAtLeast(28f)

        val colLinePaint = Paint().apply {
            color = 0xFFE2E8F0.toInt()
            mode = PaintMode.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }
        val colFont = Font(typeface, 12.5f * kScale)
        val colTextPaint = Paint().apply {
            color = 0xFF334155.toInt()
            isAntiAlias = true
        }

        for (i in 0 until 4) {
            val cx = kanban.x + i * colW
            val cy = kanban.y + headerH
            if (i > 0) {
                canvas.drawLine(cx, cy, cx, cy + colH, colLinePaint)
            }
            canvas.drawString(cols[i], cx + 14f * kScale, cy + colHeadH * 0.65f, colFont, colTextPaint)
        }
    }
}
