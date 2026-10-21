package com.lkboard.app.export

import com.lkboard.app.model.BoardElement
import com.lkboard.app.model.BoardState
import com.lkboard.app.model.GeometryUtils
import com.lkboard.app.model.ShapeType
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
            when (element) {
                is BoardElement.StickyNote -> renderStickyNote(canvas, element, defaultTypeface)
                is BoardElement.Shape -> renderShape(canvas, element)
                is BoardElement.Text -> renderText(canvas, element, defaultTypeface)
                is BoardElement.DrawingPath -> renderDrawingPath(canvas, element)
                is BoardElement.ImageElement -> renderImageElement(canvas, element)
                is BoardElement.DocumentFrame -> renderDocumentFrame(canvas, element, defaultTypeface)
                else -> {}
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
}
