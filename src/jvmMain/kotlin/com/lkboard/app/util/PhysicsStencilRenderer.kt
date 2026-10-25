package com.lkboard.app.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.lkboard.app.model.BoardElement
import com.lkboard.app.model.PhysicsStencilType
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Font
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.Typeface
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Рендерер инженерных и физических стенсилов для онлайн-уроков:
 * - Механика: наклонная плоскость с бруском и силами, блоки с нитью, пружина с грузом.
 * - Оптика: собирающая линза с фокусами F, 2F и ходом лучей.
 * - Электричество: резистор, источник тока, конденсатор, катушка, ключ, лампа.
 */
object PhysicsStencilRenderer {

    fun draw(
        drawScope: DrawScope,
        element: BoardElement.PhysicsStencil,
        textMeasurer: TextMeasurer
    ) {
        val strokeColor = Color(element.strokeColorHex)
        val fillColor = element.fillColorHex?.let { Color(it) } ?: Color.Transparent
        val strokeWidth = element.strokeWidth
        val x = element.x
        val y = element.y
        val w = element.width
        val h = element.height

        when (element.stencilType) {
            PhysicsStencilType.INCLINED_PLANE -> drawInclinedPlane(drawScope, x, y, w, h, strokeColor, fillColor, strokeWidth, textMeasurer, element.label)
            PhysicsStencilType.PULLEY_BLOCK -> drawPulleyBlock(drawScope, x, y, w, h, strokeColor, fillColor, strokeWidth, textMeasurer, element.label)
            PhysicsStencilType.SPRING -> drawSpring(drawScope, x, y, w, h, strokeColor, fillColor, strokeWidth, textMeasurer, element.label)
            PhysicsStencilType.OPTICAL_LENS -> drawOpticalLens(drawScope, x, y, w, h, strokeColor, strokeWidth, textMeasurer, element.label)
            PhysicsStencilType.CIRCUIT_RESISTOR -> drawResistor(drawScope, x, y, w, h, strokeColor, fillColor, strokeWidth, textMeasurer, element.label)
            PhysicsStencilType.CIRCUIT_BATTERY -> drawBattery(drawScope, x, y, w, h, strokeColor, strokeWidth, textMeasurer, element.label)
            PhysicsStencilType.CIRCUIT_CAPACITOR -> drawCapacitor(drawScope, x, y, w, h, strokeColor, strokeWidth, textMeasurer, element.label)
            PhysicsStencilType.CIRCUIT_INDUCTOR -> drawInductor(drawScope, x, y, w, h, strokeColor, strokeWidth, textMeasurer, element.label)
            PhysicsStencilType.CIRCUIT_SWITCH -> drawSwitch(drawScope, x, y, w, h, strokeColor, strokeWidth, textMeasurer, element.label)
            PhysicsStencilType.CIRCUIT_LAMP -> drawLamp(drawScope, x, y, w, h, strokeColor, strokeWidth, textMeasurer, element.label)
        }
    }

    // 1. Наклонная плоскость
    private fun drawInclinedPlane(
        drawScope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        strokeColor: Color, fillColor: Color, strokeWidth: Float,
        textMeasurer: TextMeasurer, label: String
    ) {
        // Клин
        val wedgePath = Path().apply {
            moveTo(x, y + h)
            lineTo(x + w, y + h)
            lineTo(x, y)
            close()
        }
        drawScope.drawPath(wedgePath, fillColor)
        drawScope.drawPath(wedgePath, strokeColor, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Брусок на наклонной плоскости
        val blockW = w * 0.22f
        val blockH = h * 0.18f
        val bx = x + w * 0.35f
        val by = y + h * 0.35f

        drawScope.drawRoundRect(
            color = strokeColor,
            topLeft = Offset(bx, by),
            size = Size(blockW, blockH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
            style = Stroke(strokeWidth)
        )

        // Вектор силы тяжести mg вниз
        val center = Offset(bx + blockW / 2f, by + blockH / 2f)
        val mgEnd = Offset(center.x, center.y + h * 0.35f)
        drawArrow(drawScope, center, mgEnd, strokeColor, strokeWidth)

        // Вектор реакции опоры N
        val nEnd = Offset(center.x + blockW * 0.4f, center.y - blockH * 0.9f)
        drawArrow(drawScope, center, nEnd, strokeColor.copy(alpha = 0.8f), strokeWidth * 0.85f)

        // Подпись массы и угла
        val txt = if (label.isNotBlank()) label else "m, α"
        drawScope.drawText(
            textMeasurer = textMeasurer,
            text = txt,
            topLeft = Offset(x + w * 0.82f, y + h - 22f),
            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = strokeColor)
        )
    }

    // 2. Блок с нитью и двумя грузами
    private fun drawPulleyBlock(
        drawScope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        strokeColor: Color, fillColor: Color, strokeWidth: Float,
        textMeasurer: TextMeasurer, label: String
    ) {
        val cx = x + w / 2f
        val r = w * 0.18f
        val pulleyY = y + r + 16f

        // Потолок с штриховкой
        drawScope.drawLine(strokeColor, Offset(cx - r * 1.5f, y + 4f), Offset(cx + r * 1.5f, y + 4f), strokeWidth = 3f)
        drawScope.drawLine(strokeColor, Offset(cx, y + 4f), Offset(cx, pulleyY), strokeWidth = 2f)

        // Шкив
        drawScope.drawCircle(fillColor, r, Offset(cx, pulleyY))
        drawScope.drawCircle(strokeColor, r, Offset(cx, pulleyY), style = Stroke(strokeWidth))
        drawScope.drawCircle(strokeColor, 3.5f, Offset(cx, pulleyY))

        // Нити
        val leftX = cx - r
        val rightX = cx + r
        val leftLoadY = pulleyY + h * 0.45f
        val rightLoadY = pulleyY + h * 0.25f

        drawScope.drawLine(strokeColor, Offset(leftX, pulleyY), Offset(leftX, leftLoadY), strokeWidth = 1.8f)
        drawScope.drawLine(strokeColor, Offset(rightX, pulleyY), Offset(rightX, rightLoadY), strokeWidth = 1.8f)

        // Груз m1
        val loadSize = r * 1.1f
        drawScope.drawRect(strokeColor, Offset(leftX - loadSize / 2f, leftLoadY), Size(loadSize, loadSize), style = Stroke(strokeWidth))
        // Груз m2
        drawScope.drawRect(strokeColor, Offset(rightX - loadSize / 2f, rightLoadY), Size(loadSize, loadSize), style = Stroke(strokeWidth))

        drawScope.drawText(textMeasurer, "m₁", Offset(leftX - 8f, leftLoadY + 4f), TextStyle(fontSize = 10.sp, color = strokeColor, fontWeight = FontWeight.Bold))
        drawScope.drawText(textMeasurer, "m₂", Offset(rightX - 8f, rightLoadY + 4f), TextStyle(fontSize = 10.sp, color = strokeColor, fontWeight = FontWeight.Bold))
    }

    // 3. Пружина с грузом
    private fun drawSpring(
        drawScope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        strokeColor: Color, fillColor: Color, strokeWidth: Float,
        textMeasurer: TextMeasurer, label: String
    ) {
        val cx = x + w / 2f
        // Верхнее крепление
        drawScope.drawLine(strokeColor, Offset(cx - 30f, y + 4f), Offset(cx + 30f, y + 4f), strokeWidth = 3f)

        // Зигзаг пружины
        val springTop = y + 12f
        val springBottom = y + h * 0.65f
        val coils = 7
        val dy = (springBottom - springTop) / coils
        val coilW = w * 0.15f

        val path = Path().apply {
            moveTo(cx, springTop)
            for (i in 0 until coils) {
                val cyStep = springTop + i * dy
                val side = if (i % 2 == 0) -coilW else coilW
                lineTo(cx + side, cyStep + dy / 2f)
                lineTo(cx, cyStep + dy)
            }
        }
        drawScope.drawPath(path, strokeColor, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Груз m
        val loadW = w * 0.35f
        val loadH = h * 0.22f
        drawScope.drawRoundRect(
            color = strokeColor,
            topLeft = Offset(cx - loadW / 2f, springBottom),
            size = Size(loadW, loadH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
            style = Stroke(strokeWidth)
        )
        val txt = if (label.isNotBlank()) label else "k, m"
        drawScope.drawText(textMeasurer, txt, Offset(cx + loadW / 2f + 8f, springBottom + 4f), TextStyle(fontSize = 11.sp, color = strokeColor, fontWeight = FontWeight.Bold))
    }

    // 4. Оптическая линза
    private fun drawOpticalLens(
        drawScope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        strokeColor: Color, strokeWidth: Float,
        textMeasurer: TextMeasurer, label: String
    ) {
        val cy = y + h / 2f
        val cx = x + w / 2f

        // Главная оптическая ось (тонкая пунктирная)
        drawScope.drawLine(strokeColor.copy(alpha = 0.5f), Offset(x, cy), Offset(x + w, cy), strokeWidth = 1.2f)

        // Собирающая линза со стрелками наружу на концах
        val lensTop = y + 10f
        val lensBottom = y + h - 10f
        drawScope.drawLine(strokeColor, Offset(cx, lensTop), Offset(cx, lensBottom), strokeWidth = strokeWidth * 1.2f)

        // Стрелки на концах линзы
        drawScope.drawLine(strokeColor, Offset(cx - 8f, lensTop + 8f), Offset(cx, lensTop), strokeWidth = strokeWidth)
        drawScope.drawLine(strokeColor, Offset(cx + 8f, lensTop + 8f), Offset(cx, lensTop), strokeWidth = strokeWidth)
        drawScope.drawLine(strokeColor, Offset(cx - 8f, lensBottom - 8f), Offset(cx, lensBottom), strokeWidth = strokeWidth)
        drawScope.drawLine(strokeColor, Offset(cx + 8f, lensBottom - 8f), Offset(cx, lensBottom), strokeWidth = strokeWidth)

        // Фокусы F1, F2
        val focalDist = w * 0.25f
        val f1X = cx - focalDist
        val f2X = cx + focalDist

        drawScope.drawLine(strokeColor, Offset(f1X, cy - 6f), Offset(f1X, cy + 6f), strokeWidth = 2f)
        drawScope.drawLine(strokeColor, Offset(f2X, cy - 6f), Offset(f2X, cy + 6f), strokeWidth = 2f)
        drawScope.drawText(textMeasurer, "F", Offset(f1X - 4f, cy + 8f), TextStyle(fontSize = 11.sp, color = strokeColor, fontWeight = FontWeight.Bold))
        drawScope.drawText(textMeasurer, "F'", Offset(f2X - 4f, cy + 8f), TextStyle(fontSize = 11.sp, color = strokeColor, fontWeight = FontWeight.Bold))

        // Ход луча (параллельный луч преломляется в фокус)
        val rayY = cy - h * 0.25f
        drawScope.drawLine(Color(0xFF38BDF8), Offset(x + 10f, rayY), Offset(cx, rayY), strokeWidth = 1.5f)
        drawScope.drawLine(Color(0xFF38BDF8), Offset(cx, rayY), Offset(f2X + (x + w - f2X) * 0.8f, cy + (cy - rayY) * 0.8f), strokeWidth = 1.5f)
    }

    // 5. Резистор
    private fun drawResistor(
        drawScope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        strokeColor: Color, fillColor: Color, strokeWidth: Float,
        textMeasurer: TextMeasurer, label: String
    ) {
        val cy = y + h / 2f
        val bodyW = w * 0.55f
        val bodyH = h * 0.45f
        val bodyX = x + (w - bodyW) / 2f
        val bodyY = cy - bodyH / 2f

        // Выводы слева и справа
        drawScope.drawLine(strokeColor, Offset(x, cy), Offset(bodyX, cy), strokeWidth = strokeWidth)
        drawScope.drawLine(strokeColor, Offset(bodyX + bodyW, cy), Offset(x + w, cy), strokeWidth = strokeWidth)

        // Прямоугольник резистора по ГОСТ
        drawScope.drawRect(fillColor, Offset(bodyX, bodyY), Size(bodyW, bodyH))
        drawScope.drawRect(strokeColor, Offset(bodyX, bodyY), Size(bodyW, bodyH), style = Stroke(strokeWidth))

        val txt = if (label.isNotBlank()) label else "R"
        drawScope.drawText(textMeasurer, txt, Offset(bodyX + bodyW / 2f - 6f, bodyY - 18f), TextStyle(fontSize = 12.sp, color = strokeColor, fontWeight = FontWeight.Bold))
    }

    // 6. Источник тока (Батарейка)
    private fun drawBattery(
        drawScope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        strokeColor: Color, strokeWidth: Float,
        textMeasurer: TextMeasurer, label: String
    ) {
        val cy = y + h / 2f
        val cx = x + w / 2f
        val gap = 12f
        val longPlateH = h * 0.75f
        val shortPlateH = h * 0.42f

        // Выводы
        drawScope.drawLine(strokeColor, Offset(x, cy), Offset(cx - gap / 2f, cy), strokeWidth = strokeWidth)
        drawScope.drawLine(strokeColor, Offset(cx + gap / 2f, cy), Offset(x + w, cy), strokeWidth = strokeWidth)

        // Длинная тонкая (+) пластина
        drawScope.drawLine(strokeColor, Offset(cx - gap / 2f, cy - longPlateH / 2f), Offset(cx - gap / 2f, cy + longPlateH / 2f), strokeWidth = 2f)
        // Короткая толстая (-) пластина
        drawScope.drawLine(strokeColor, Offset(cx + gap / 2f, cy - shortPlateH / 2f), Offset(cx + gap / 2f, cy + shortPlateH / 2f), strokeWidth = 4.5f)

        drawScope.drawText(textMeasurer, "+", Offset(cx - gap - 8f, cy - longPlateH / 2f - 4f), TextStyle(fontSize = 12.sp, color = strokeColor, fontWeight = FontWeight.Bold))
        drawScope.drawText(textMeasurer, "-", Offset(cx + gap + 2f, cy - shortPlateH / 2f - 6f), TextStyle(fontSize = 14.sp, color = strokeColor, fontWeight = FontWeight.Bold))
        val txt = if (label.isNotBlank()) label else "E, r"
        drawScope.drawText(textMeasurer, txt, Offset(cx - 10f, cy + longPlateH / 2f + 4f), TextStyle(fontSize = 11.sp, color = strokeColor, fontWeight = FontWeight.Bold))
    }

    // 7. Конденсатор
    private fun drawCapacitor(
        drawScope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        strokeColor: Color, strokeWidth: Float,
        textMeasurer: TextMeasurer, label: String
    ) {
        val cy = y + h / 2f
        val cx = x + w / 2f
        val gap = 10f
        val plateH = h * 0.65f

        drawScope.drawLine(strokeColor, Offset(x, cy), Offset(cx - gap / 2f, cy), strokeWidth = strokeWidth)
        drawScope.drawLine(strokeColor, Offset(cx + gap / 2f, cy), Offset(x + w, cy), strokeWidth = strokeWidth)
        drawScope.drawLine(strokeColor, Offset(cx - gap / 2f, cy - plateH / 2f), Offset(cx - gap / 2f, cy + plateH / 2f), strokeWidth = 3f)
        drawScope.drawLine(strokeColor, Offset(cx + gap / 2f, cy - plateH / 2f), Offset(cx + gap / 2f, cy + plateH / 2f), strokeWidth = 3f)

        val txt = if (label.isNotBlank()) label else "C"
        drawScope.drawText(textMeasurer, txt, Offset(cx - 5f, cy - plateH / 2f - 18f), TextStyle(fontSize = 12.sp, color = strokeColor, fontWeight = FontWeight.Bold))
    }

    // 8. Катушка индуктивности
    private fun drawInductor(
        drawScope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        strokeColor: Color, strokeWidth: Float,
        textMeasurer: TextMeasurer, label: String
    ) {
        val cy = y + h / 2f
        val bodyW = w * 0.65f
        val bodyX = x + (w - bodyW) / 2f
        val coils = 4
        val r = bodyW / (coils * 2f)

        drawScope.drawLine(strokeColor, Offset(x, cy), Offset(bodyX, cy), strokeWidth = strokeWidth)
        drawScope.drawLine(strokeColor, Offset(bodyX + bodyW, cy), Offset(x + w, cy), strokeWidth = strokeWidth)

        for (i in 0 until coils) {
            val coilCenter = Offset(bodyX + r + i * 2f * r, cy)
            val path = Path().apply {
                arcTo(androidx.compose.ui.geometry.Rect(coilCenter.x - r, coilCenter.y - r, coilCenter.x + r, coilCenter.y + r), 180f, 180f, false)
            }
            drawScope.drawPath(path, strokeColor, style = Stroke(strokeWidth, cap = StrokeCap.Round))
        }

        val txt = if (label.isNotBlank()) label else "L"
        drawScope.drawText(textMeasurer, txt, Offset(bodyX + bodyW / 2f - 5f, cy - r - 16f), TextStyle(fontSize = 12.sp, color = strokeColor, fontWeight = FontWeight.Bold))
    }

    // 9. Ключ
    private fun drawSwitch(
        drawScope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        strokeColor: Color, strokeWidth: Float,
        textMeasurer: TextMeasurer, label: String
    ) {
        val cy = y + h / 2f
        val cx = x + w / 2f
        val gap = 30f

        drawScope.drawLine(strokeColor, Offset(x, cy), Offset(cx - gap / 2f, cy), strokeWidth = strokeWidth)
        drawScope.drawLine(strokeColor, Offset(cx + gap / 2f, cy), Offset(x + w, cy), strokeWidth = strokeWidth)
        drawScope.drawCircle(strokeColor, 4f, Offset(cx - gap / 2f, cy), style = Stroke(strokeWidth))
        drawScope.drawCircle(strokeColor, 4f, Offset(cx + gap / 2f, cy), style = Stroke(strokeWidth))

        // Откинутый контакт ключа
        drawScope.drawLine(strokeColor, Offset(cx - gap / 2f, cy), Offset(cx + gap / 2f - 4f, cy - 18f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
        val txt = if (label.isNotBlank()) label else "K"
        drawScope.drawText(textMeasurer, txt, Offset(cx - 5f, cy - 32f), TextStyle(fontSize = 12.sp, color = strokeColor, fontWeight = FontWeight.Bold))
    }

    // 10. Лампа
    private fun drawLamp(
        drawScope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        strokeColor: Color, strokeWidth: Float,
        textMeasurer: TextMeasurer, label: String
    ) {
        val cy = y + h / 2f
        val cx = x + w / 2f
        val r = h * 0.32f

        drawScope.drawLine(strokeColor, Offset(x, cy), Offset(cx - r, cy), strokeWidth = strokeWidth)
        drawScope.drawLine(strokeColor, Offset(cx + r, cy), Offset(x + w, cy), strokeWidth = strokeWidth)
        drawScope.drawCircle(strokeColor, r, Offset(cx, cy), style = Stroke(strokeWidth))

        val crossR = r * 0.707f
        drawScope.drawLine(strokeColor, Offset(cx - crossR, cy - crossR), Offset(cx + crossR, cy + crossR), strokeWidth = strokeWidth * 0.85f)
        drawScope.drawLine(strokeColor, Offset(cx - crossR, cy + crossR), Offset(cx + crossR, cy - crossR), strokeWidth = strokeWidth * 0.85f)
    }

    private fun drawArrow(drawScope: DrawScope, from: Offset, to: Offset, color: Color, strokeWidth: Float) {
        drawScope.drawLine(color, from, to, strokeWidth = strokeWidth, cap = StrokeCap.Round)
        val dx = to.x - from.x
        val dy = to.y - from.y
        val angle = Math.atan2(dy.toDouble(), dx.toDouble())
        val arrowHeadLength = 10f
        val arrowHeadAngle = PI / 6

        val p1 = Offset(
            (to.x - arrowHeadLength * cos(angle - arrowHeadAngle)).toFloat(),
            (to.y - arrowHeadLength * sin(angle - arrowHeadAngle)).toFloat()
        )
        val p2 = Offset(
            (to.x - arrowHeadLength * cos(angle + arrowHeadAngle)).toFloat(),
            (to.y - arrowHeadLength * sin(angle + arrowHeadAngle)).toFloat()
        )
        drawScope.drawLine(color, to, p1, strokeWidth = strokeWidth, cap = StrokeCap.Round)
        drawScope.drawLine(color, to, p2, strokeWidth = strokeWidth, cap = StrokeCap.Round)
    }

    /**
     * Skia рендерер стенсила для экспорта в PNG и PDF.
     */
    fun renderSkia(
        canvas: Canvas,
        element: BoardElement.PhysicsStencil,
        typeface: Typeface
    ) {
        val paint = Paint().apply {
            color = element.strokeColorHex.toInt()
            strokeWidth = element.strokeWidth
            mode = PaintMode.STROKE
            isAntiAlias = true
        }
        val x = element.x
        val y = element.y
        val w = element.width
        val h = element.height

        when (element.stencilType) {
            PhysicsStencilType.CIRCUIT_RESISTOR -> {
                val cy = y + h / 2f
                val bodyW = w * 0.55f
                val bodyH = h * 0.45f
                val bodyX = x + (w - bodyW) / 2f
                val bodyY = cy - bodyH / 2f
                canvas.drawLine(x, cy, bodyX, cy, paint)
                canvas.drawLine(bodyX + bodyW, cy, x + w, cy, paint)
                canvas.drawRect(org.jetbrains.skia.Rect.makeXYWH(bodyX, bodyY, bodyW, bodyH), paint)
            }
            PhysicsStencilType.CIRCUIT_LAMP -> {
                val cy = y + h / 2f
                val cx = x + w / 2f
                val r = h * 0.32f
                canvas.drawLine(x, cy, cx - r, cy, paint)
                canvas.drawLine(cx + r, cy, x + w, cy, paint)
                canvas.drawCircle(cx, cy, r, paint)
            }
            else -> {
                // Базовый прямоугольник с обозначением стенсила
                canvas.drawRect(org.jetbrains.skia.Rect.makeXYWH(x, y, w, h), paint)
            }
        }

        val font = Font(typeface, 12f)
        val fillPaint = Paint().apply {
            color = element.strokeColorHex.toInt()
            isAntiAlias = true
        }
        val label = if (element.label.isNotBlank()) element.label else element.stencilType.displayName
        canvas.drawString(label, x + 8f, y + 16f, font, fillPaint)
    }
}
