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
            PhysicsStencilType.MATHEMATICAL_PENDULUM -> drawMathematicalPendulum(drawScope, x, y, w, h, strokeColor, fillColor, strokeWidth, textMeasurer, element.label)
            PhysicsStencilType.LEVER_BALANCE -> drawLeverBalance(drawScope, x, y, w, h, strokeColor, fillColor, strokeWidth, textMeasurer, element.label)
            PhysicsStencilType.THERMO_CYLINDER -> drawThermoCylinder(drawScope, x, y, w, h, strokeColor, fillColor, strokeWidth, textMeasurer, element.label)
            PhysicsStencilType.THERMO_PV_CYCLE -> drawThermoPvCycle(drawScope, x, y, w, h, strokeColor, fillColor, strokeWidth, textMeasurer, element.label)
            PhysicsStencilType.OPTICAL_LENS -> drawOpticalLens(drawScope, x, y, w, h, strokeColor, strokeWidth, textMeasurer, element.label)
            PhysicsStencilType.OPTICAL_PRISM -> drawOpticalPrism(drawScope, x, y, w, h, strokeColor, fillColor, strokeWidth, textMeasurer, element.label)
            PhysicsStencilType.CIRCUIT_RESISTOR -> drawResistor(drawScope, x, y, w, h, strokeColor, fillColor, strokeWidth, textMeasurer, element.label)
            PhysicsStencilType.CIRCUIT_BATTERY -> drawBattery(drawScope, x, y, w, h, strokeColor, strokeWidth, textMeasurer, element.label)
            PhysicsStencilType.CIRCUIT_CAPACITOR -> drawCapacitor(drawScope, x, y, w, h, strokeColor, strokeWidth, textMeasurer, element.label)
            PhysicsStencilType.CIRCUIT_INDUCTOR -> drawInductor(drawScope, x, y, w, h, strokeColor, strokeWidth, textMeasurer, element.label)
            PhysicsStencilType.CIRCUIT_SWITCH -> drawSwitch(drawScope, x, y, w, h, strokeColor, strokeWidth, textMeasurer, element.label)
            PhysicsStencilType.CIRCUIT_LAMP -> drawLamp(drawScope, x, y, w, h, strokeColor, strokeWidth, textMeasurer, element.label)
            PhysicsStencilType.CIRCUIT_AMMETER -> drawMeter(drawScope, x, y, w, h, strokeColor, fillColor, strokeWidth, textMeasurer, "A", element.label)
            PhysicsStencilType.CIRCUIT_VOLTMETER -> drawMeter(drawScope, x, y, w, h, strokeColor, fillColor, strokeWidth, textMeasurer, "V", element.label)
            PhysicsStencilType.CIRCUIT_RHEOSTAT -> drawRheostat(drawScope, x, y, w, h, strokeColor, fillColor, strokeWidth, textMeasurer, element.label)
            PhysicsStencilType.MAGNET_FIELD -> drawMagnetField(drawScope, x, y, w, h, strokeColor, strokeWidth, textMeasurer, element.label)
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
        drawLabel(
            drawScope = drawScope,
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

        drawLabel(drawScope, textMeasurer, "m₁", Offset(leftX - 8f, leftLoadY + 4f), TextStyle(fontSize = 10.sp, color = strokeColor, fontWeight = FontWeight.Bold))
        drawLabel(drawScope, textMeasurer, "m₂", Offset(rightX - 8f, rightLoadY + 4f), TextStyle(fontSize = 10.sp, color = strokeColor, fontWeight = FontWeight.Bold))
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
        drawLabel(drawScope, textMeasurer, txt, Offset(cx + loadW / 2f + 8f, springBottom + 4f), TextStyle(fontSize = 11.sp, color = strokeColor, fontWeight = FontWeight.Bold))
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
        drawLabel(drawScope, textMeasurer, "F", Offset(f1X - 4f, cy + 8f), TextStyle(fontSize = 11.sp, color = strokeColor, fontWeight = FontWeight.Bold))
        drawLabel(drawScope, textMeasurer, "F'", Offset(f2X - 4f, cy + 8f), TextStyle(fontSize = 11.sp, color = strokeColor, fontWeight = FontWeight.Bold))

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
        drawLabel(drawScope, textMeasurer, txt, Offset(bodyX + bodyW / 2f - 6f, bodyY - 18f), TextStyle(fontSize = 12.sp, color = strokeColor, fontWeight = FontWeight.Bold))
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

        drawLabel(drawScope, textMeasurer, "+", Offset(cx - gap - 8f, cy - longPlateH / 2f - 4f), TextStyle(fontSize = 12.sp, color = strokeColor, fontWeight = FontWeight.Bold))
        drawLabel(drawScope, textMeasurer, "-", Offset(cx + gap + 2f, cy - shortPlateH / 2f - 6f), TextStyle(fontSize = 14.sp, color = strokeColor, fontWeight = FontWeight.Bold))
        val txt = if (label.isNotBlank()) label else "E, r"
        drawLabel(drawScope, textMeasurer, txt, Offset(cx - 10f, cy + longPlateH / 2f + 4f), TextStyle(fontSize = 11.sp, color = strokeColor, fontWeight = FontWeight.Bold))
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
        drawLabel(drawScope, textMeasurer, txt, Offset(cx - 5f, cy - plateH / 2f - 18f), TextStyle(fontSize = 12.sp, color = strokeColor, fontWeight = FontWeight.Bold))
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
        drawLabel(drawScope, textMeasurer, txt, Offset(bodyX + bodyW / 2f - 5f, cy - r - 16f), TextStyle(fontSize = 12.sp, color = strokeColor, fontWeight = FontWeight.Bold))
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
        drawLabel(drawScope, textMeasurer, txt, Offset(cx - 5f, cy - 32f), TextStyle(fontSize = 12.sp, color = strokeColor, fontWeight = FontWeight.Bold))
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

    private fun drawMathematicalPendulum(
        drawScope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        strokeColor: Color, fillColor: Color, strokeWidth: Float,
        textMeasurer: TextMeasurer, label: String
    ) {
        val ceilingY = y + 14f
        val pivotX = x + w * 0.45f

        drawScope.drawLine(strokeColor, Offset(x + w * 0.15f, ceilingY), Offset(x + w * 0.75f, ceilingY), strokeWidth = strokeWidth * 1.2f)
        for (i in 0..6) {
            val hx = x + w * 0.18f + i * (w * 0.08f)
            drawScope.drawLine(strokeColor.copy(alpha = 0.5f), Offset(hx, ceilingY), Offset(hx + 8f, ceilingY - 8f), strokeWidth = strokeWidth * 0.8f)
        }

        drawScope.drawLine(strokeColor.copy(alpha = 0.3f), Offset(pivotX, ceilingY), Offset(pivotX, y + h * 0.85f), strokeWidth = 1f)

        val angleRad = (22.0 * PI / 180.0).toFloat()
        val stringLen = h * 0.68f
        val bobX = pivotX + (stringLen * sin(angleRad))
        val bobY = ceilingY + (stringLen * cos(angleRad))
        val bobR = 12f

        drawScope.drawLine(strokeColor, Offset(pivotX, ceilingY), Offset(bobX, bobY), strokeWidth = strokeWidth)
        drawScope.drawCircle(if (fillColor != Color.Transparent) fillColor else Color(0xFF2563EB).copy(alpha = 0.15f), bobR, Offset(bobX, bobY))
        drawScope.drawCircle(strokeColor, bobR, Offset(bobX, bobY), style = Stroke(strokeWidth))

        drawArrow(drawScope, Offset(bobX, bobY), Offset(bobX, bobY + 28f), strokeColor, strokeWidth)

        val tensionEndX = bobX - 22f * sin(angleRad)
        val tensionEndY = bobY - 22f * cos(angleRad)
        drawArrow(drawScope, Offset(bobX, bobY), Offset(tensionEndX, tensionEndY), Color(0xFF16A34A), strokeWidth)

        val txt = if (label.isNotBlank()) label else "L, m, T=2π√(L/g)"
        drawLabel(drawScope, textMeasurer, txt, Offset(x + 10f, y + h - 20f), TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = strokeColor))
    }

    private fun drawLeverBalance(
        drawScope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        strokeColor: Color, fillColor: Color, strokeWidth: Float,
        textMeasurer: TextMeasurer, label: String
    ) {
        val fulcrumX = x + w * 0.45f
        val fulcrumY = y + h * 0.60f
        val beamY = fulcrumY

        val triPath = Path().apply {
            moveTo(fulcrumX, fulcrumY)
            lineTo(fulcrumX - 16f, fulcrumY + 28f)
            lineTo(fulcrumX + 16f, fulcrumY + 28f)
            close()
        }
        drawScope.drawPath(triPath, strokeColor.copy(alpha = 0.2f))
        drawScope.drawPath(triPath, strokeColor, style = Stroke(strokeWidth))
        drawScope.drawLine(strokeColor, Offset(fulcrumX - 28f, fulcrumY + 28f), Offset(fulcrumX + 28f, fulcrumY + 28f), strokeWidth = strokeWidth)
        drawScope.drawLine(strokeColor, Offset(x + 10f, beamY), Offset(x + w - 10f, beamY), strokeWidth = strokeWidth * 1.5f)

        val load1X = x + 24f
        drawScope.drawRect(strokeColor, Offset(load1X - 10f, beamY - 22f), Size(20f, 22f), style = Stroke(strokeWidth))
        drawArrow(drawScope, Offset(load1X, beamY), Offset(load1X, beamY + 26f), strokeColor, strokeWidth)

        val load2X = x + w - 24f
        drawScope.drawRect(strokeColor, Offset(load2X - 8f, beamY - 18f), Size(16f, 18f), style = Stroke(strokeWidth))
        drawArrow(drawScope, Offset(load2X, beamY), Offset(load2X, beamY + 22f), strokeColor, strokeWidth)

        val txt = if (label.isNotBlank()) label else "F₁·d₁ = F₂·d₂"
        drawLabel(drawScope, textMeasurer, txt, Offset(x + 10f, y + 8f), TextStyle(fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = strokeColor))
    }

    private fun drawThermoCylinder(
        drawScope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        strokeColor: Color, fillColor: Color, strokeWidth: Float,
        textMeasurer: TextMeasurer, label: String
    ) {
        val cx = x + w * 0.15f
        val cw = w * 0.70f
        val ch = h * 0.75f
        val cy = y + h * 0.15f

        val cylPath = Path().apply {
            moveTo(cx, cy)
            lineTo(cx, cy + ch)
            lineTo(cx + cw, cy + ch)
            lineTo(cx + cw, cy)
        }
        drawScope.drawPath(cylPath, strokeColor, style = Stroke(strokeWidth * 1.3f))

        val pistonY = cy + ch * 0.40f
        val pistonH = 14f
        drawScope.drawRoundRect(strokeColor.copy(alpha = 0.25f), Offset(cx + 2f, pistonY), Size(cw - 4f, pistonH), androidx.compose.ui.geometry.CornerRadius(2f, 2f))
        drawScope.drawRoundRect(strokeColor, Offset(cx + 2f, pistonY), Size(cw - 4f, pistonH), androidx.compose.ui.geometry.CornerRadius(2f, 2f), style = Stroke(strokeWidth))

        val rodX = cx + cw / 2f
        drawScope.drawLine(strokeColor, Offset(rodX, pistonY), Offset(rodX, y + 6f), strokeWidth = strokeWidth * 1.5f)

        val gasDots = listOf(
            Offset(cx + cw * 0.25f, cy + ch * 0.65f),
            Offset(cx + cw * 0.50f, cy + ch * 0.80f),
            Offset(cx + cw * 0.75f, cy + ch * 0.60f),
            Offset(cx + cw * 0.35f, cy + ch * 0.88f),
            Offset(cx + cw * 0.70f, cy + ch * 0.85f)
        )
        gasDots.forEach { dot ->
            drawScope.drawCircle(strokeColor, 3f, dot)
            drawScope.drawLine(strokeColor.copy(alpha = 0.4f), dot, Offset(dot.x + 6f, dot.y - 4f), strokeWidth = 1f)
        }

        drawArrow(drawScope, Offset(rodX, cy + ch + 18f), Offset(rodX, cy + ch + 2f), Color(0xFFD97706), strokeWidth)

        val txt = if (label.isNotBlank()) label else "p, V, T (p·V = νRT)"
        drawLabel(drawScope, textMeasurer, txt, Offset(x + 10f, y + h - 18f), TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = strokeColor))
    }

    private fun drawThermoPvCycle(
        drawScope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        strokeColor: Color, fillColor: Color, strokeWidth: Float,
        textMeasurer: TextMeasurer, label: String
    ) {
        val ox = x + 24f
        val oy = y + h - 24f

        drawArrow(drawScope, Offset(ox, oy), Offset(ox, y + 8f), strokeColor, strokeWidth)
        drawArrow(drawScope, Offset(ox, oy), Offset(x + w - 8f, oy), strokeColor, strokeWidth)
        drawLabel(drawScope, textMeasurer, "p", Offset(ox - 14f, y + 4f), TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = strokeColor))
        drawLabel(drawScope, textMeasurer, "V", Offset(x + w - 16f, oy + 4f), TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = strokeColor))

        val p1 = Offset(ox + w * 0.22f, oy - h * 0.22f)
        val p2 = Offset(ox + w * 0.22f, oy - h * 0.65f)
        val p3 = Offset(ox + w * 0.65f, oy - h * 0.65f)

        val cyclePath = Path().apply {
            moveTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            lineTo(p3.x, p3.y)
            close()
        }
        drawScope.drawPath(cyclePath, Color(0xFF2563EB).copy(alpha = 0.12f))
        drawScope.drawPath(cyclePath, strokeColor, style = Stroke(strokeWidth))

        drawArrow(drawScope, Offset(p1.x, p1.y), Offset(p1.x, (p1.y + p2.y) / 2f - 2f), strokeColor, strokeWidth)
        drawArrow(drawScope, Offset(p2.x, p2.y), Offset((p2.x + p3.x) / 2f + 2f, p2.y), strokeColor, strokeWidth)
        drawArrow(drawScope, Offset(p3.x, p3.y), Offset((p3.x + p1.x) / 2f, (p3.y + p1.y) / 2f), strokeColor, strokeWidth)

        val txt = if (label.isNotBlank()) label else "A = S_цикла, η = A/Q₁"
        drawLabel(drawScope, textMeasurer, txt, Offset(ox + w * 0.26f, oy - h * 0.40f), TextStyle(fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = strokeColor))
    }

    private fun drawOpticalPrism(
        drawScope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        strokeColor: Color, fillColor: Color, strokeWidth: Float,
        textMeasurer: TextMeasurer, label: String
    ) {
        val apex = Offset(x + w * 0.5f, y + 14f)
        val left = Offset(x + w * 0.2f, y + h - 18f)
        val right = Offset(x + w * 0.8f, y + h - 18f)

        val prismPath = Path().apply {
            moveTo(apex.x, apex.y)
            lineTo(right.x, right.y)
            lineTo(left.x, left.y)
            close()
        }
        drawScope.drawPath(prismPath, Color(0xFF38BDF8).copy(alpha = 0.12f))
        drawScope.drawPath(prismPath, strokeColor, style = Stroke(strokeWidth))

        val hitPoint = Offset(x + w * 0.32f, y + h * 0.58f)
        val inRayStart = Offset(x + 4f, hitPoint.y + 16f)
        drawArrow(drawScope, inRayStart, hitPoint, Color(0xFFD97706), strokeWidth)

        val exitPoint = Offset(x + w * 0.68f, y + h * 0.64f)
        drawScope.drawLine(Color(0xFFD97706), hitPoint, exitPoint, strokeWidth = strokeWidth)

        val outRayRed = Offset(x + w - 4f, exitPoint.y + 12f)
        val outRayViolet = Offset(x + w - 4f, exitPoint.y + 24f)
        drawArrow(drawScope, exitPoint, outRayRed, Color(0xFFDC2626), strokeWidth)
        drawArrow(drawScope, exitPoint, outRayViolet, Color(0xFF7C3AED), strokeWidth)

        val txt = if (label.isNotBlank()) label else "n, sin α / sin β = n"
        drawLabel(drawScope, textMeasurer, txt, Offset(x + 10f, y + h - 16f), TextStyle(fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = strokeColor))
    }

    private fun drawMeter(
        drawScope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        strokeColor: Color, fillColor: Color, strokeWidth: Float,
        textMeasurer: TextMeasurer, symbol: String, label: String
    ) {
        val cy = y + h / 2f
        val cx = x + w / 2f
        val r = h * 0.34f

        drawScope.drawLine(strokeColor, Offset(x, cy), Offset(cx - r, cy), strokeWidth = strokeWidth)
        drawScope.drawLine(strokeColor, Offset(cx + r, cy), Offset(x + w, cy), strokeWidth = strokeWidth)
        drawScope.drawCircle(if (fillColor != Color.Transparent) fillColor else Color.White, r, Offset(cx, cy))
        drawScope.drawCircle(strokeColor, r, Offset(cx, cy), style = Stroke(strokeWidth))

        drawLabel(drawScope, textMeasurer, symbol, Offset(cx - 7f, cy - 11f), TextStyle(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = strokeColor))

        if (label.isNotBlank()) {
            drawLabel(drawScope, textMeasurer, label, Offset(x + 8f, y + 6f), TextStyle(fontSize = 10.5.sp, color = strokeColor, fontWeight = FontWeight.Bold))
        }
    }

    private fun drawRheostat(
        drawScope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        strokeColor: Color, fillColor: Color, strokeWidth: Float,
        textMeasurer: TextMeasurer, label: String
    ) {
        val cy = y + h * 0.60f
        val bodyW = w * 0.55f
        val bodyH = h * 0.32f
        val bodyX = x + (w - bodyW) / 2f
        val bodyY = cy - bodyH / 2f

        drawScope.drawLine(strokeColor, Offset(x, cy), Offset(bodyX, cy), strokeWidth = strokeWidth)
        drawScope.drawLine(strokeColor, Offset(bodyX + bodyW, cy), Offset(x + w, cy), strokeWidth = strokeWidth)
        drawScope.drawRect(fillColor, Offset(bodyX, bodyY), Size(bodyW, bodyH))
        drawScope.drawRect(strokeColor, Offset(bodyX, bodyY), Size(bodyW, bodyH), style = Stroke(strokeWidth))

        val sliderX = bodyX + bodyW * 0.55f
        drawArrow(drawScope, Offset(sliderX, y + 10f), Offset(sliderX, bodyY), strokeColor, strokeWidth)
        drawScope.drawLine(strokeColor, Offset(x + 10f, y + 10f), Offset(sliderX, y + 10f), strokeWidth = strokeWidth)

        val txt = if (label.isNotBlank()) label else "R_рег"
        drawLabel(drawScope, textMeasurer, txt, Offset(bodyX + 8f, cy - 8f), TextStyle(fontSize = 11.sp, color = strokeColor, fontWeight = FontWeight.Bold))
    }

    private fun drawMagnetField(
        drawScope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        strokeColor: Color, strokeWidth: Float,
        textMeasurer: TextMeasurer, label: String
    ) {
        val my = y + h * 0.40f
        val mw = w * 0.55f
        val mh = h * 0.28f
        val mx = x + (w - mw) / 2f

        drawScope.drawRect(Color(0xFFEF4444), Offset(mx, my), Size(mw / 2f, mh))
        drawLabel(drawScope, textMeasurer, "N", Offset(mx + mw * 0.2f - 5f, my + 4f), TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White))

        drawScope.drawRect(Color(0xFF3B82F6), Offset(mx + mw / 2f, my), Size(mw / 2f, mh))
        drawLabel(drawScope, textMeasurer, "S", Offset(mx + mw * 0.7f - 5f, my + 4f), TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White))

        drawScope.drawRect(strokeColor, Offset(mx, my), Size(mw, mh), style = Stroke(strokeWidth))

        val arcTop = Path().apply {
            moveTo(mx + mw * 0.15f, my)
            quadraticBezierTo(x + w * 0.5f, y + 4f, mx + mw * 0.85f, my)
        }
        drawScope.drawPath(arcTop, strokeColor.copy(alpha = 0.5f), style = Stroke(strokeWidth * 0.8f))

        val arcBottom = Path().apply {
            moveTo(mx + mw * 0.15f, my + mh)
            quadraticBezierTo(x + w * 0.5f, y + h - 8f, mx + mw * 0.85f, my + mh)
        }
        drawScope.drawPath(arcBottom, strokeColor.copy(alpha = 0.5f), style = Stroke(strokeWidth * 0.8f))

        val txt = if (label.isNotBlank()) label else "B (N -> S)"
        drawLabel(drawScope, textMeasurer, txt, Offset(x + 10f, y + h - 16f), TextStyle(fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = strokeColor))
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
                val cr = r * 0.707f
                canvas.drawLine(cx - cr, cy - cr, cx + cr, cy + cr, paint)
                canvas.drawLine(cx - cr, cy + cr, cx + cr, cy - cr, paint)
            }
            PhysicsStencilType.CIRCUIT_BATTERY -> {
                val cy = y + h / 2f
                val cx = x + w / 2f
                canvas.drawLine(x, cy, cx - 12f, cy, paint)
                canvas.drawLine(cx + 12f, cy, x + w, cy, paint)
                // Длинная тонкая обкладка (+)
                canvas.drawLine(cx - 12f, cy - 24f, cx - 12f, cy + 24f, paint)
                // Короткая толстая обкладка (-)
                val thickPaint = Paint().apply {
                    color = element.strokeColorHex.toInt()
                    strokeWidth = element.strokeWidth * 2.2f
                    mode = PaintMode.STROKE
                    isAntiAlias = true
                }
                canvas.drawLine(cx + 12f, cy - 14f, cx + 12f, cy + 14f, thickPaint)
            }
            PhysicsStencilType.CIRCUIT_CAPACITOR -> {
                val cy = y + h / 2f
                val cx = x + w / 2f
                canvas.drawLine(x, cy, cx - 10f, cy, paint)
                canvas.drawLine(cx + 10f, cy, x + w, cy, paint)
                canvas.drawLine(cx - 10f, cy - 22f, cx - 10f, cy + 22f, paint)
                canvas.drawLine(cx + 10f, cy - 22f, cx + 10f, cy + 22f, paint)
            }
            PhysicsStencilType.CIRCUIT_AMMETER, PhysicsStencilType.CIRCUIT_VOLTMETER -> {
                val cy = y + h / 2f
                val cx = x + w / 2f
                val r = h * 0.32f
                canvas.drawLine(x, cy, cx - r, cy, paint)
                canvas.drawLine(cx + r, cy, x + w, cy, paint)
                canvas.drawCircle(cx, cy, r, paint)
                val mFont = Font(typeface, 16f)
                val mPaint = Paint().apply { color = element.strokeColorHex.toInt(); isAntiAlias = true }
                val letter = if (element.stencilType == PhysicsStencilType.CIRCUIT_AMMETER) "A" else "V"
                canvas.drawString(letter, cx - 5.5f, cy + 5.5f, mFont, mPaint)
            }
            PhysicsStencilType.CIRCUIT_RHEOSTAT -> {
                val cy = y + h / 2f
                val bodyW = w * 0.55f
                val bodyH = h * 0.45f
                val bodyX = x + (w - bodyW) / 2f
                val bodyY = cy - bodyH / 2f
                canvas.drawLine(x, cy, bodyX, cy, paint)
                canvas.drawLine(bodyX + bodyW, cy, x + w, cy, paint)
                canvas.drawRect(org.jetbrains.skia.Rect.makeXYWH(bodyX, bodyY, bodyW, bodyH), paint)
                canvas.drawLine(bodyX - 4f, bodyY + bodyH + 6f, bodyX + bodyW + 8f, bodyY - 6f, paint)
            }
            PhysicsStencilType.CIRCUIT_SWITCH -> {
                val cy = y + h / 2f
                val cx = x + w / 2f
                canvas.drawLine(x, cy, cx - 20f, cy, paint)
                canvas.drawLine(cx + 20f, cy, x + w, cy, paint)
                canvas.drawCircle(cx - 20f, cy, 3.5f, paint)
                canvas.drawCircle(cx + 20f, cy, 3.5f, paint)
                canvas.drawLine(cx - 18f, cy, cx + 12f, cy - 16f, paint)
            }
            PhysicsStencilType.INCLINED_PLANE -> {
                val p = org.jetbrains.skia.Path().apply {
                    moveTo(x + 10f, y + h - 14f)
                    lineTo(x + w - 10f, y + h - 14f)
                    lineTo(x + 10f, y + 14f)
                    closePath()
                }
                canvas.drawPath(p, paint)
                // Брусок на склоне
                val bx = x + w * 0.40f
                val by = y + h * 0.45f
                canvas.drawRect(org.jetbrains.skia.Rect.makeXYWH(bx, by, 32f, 22f), paint)
            }
            PhysicsStencilType.MATHEMATICAL_PENDULUM -> {
                val cx = x + w / 2f
                val topY = y + 12f
                canvas.drawLine(cx - 30f, topY, cx + 30f, topY, paint)
                val bobX = cx + 24f
                val bobY = y + h - 28f
                canvas.drawLine(cx, topY, bobX, bobY, paint)
                canvas.drawCircle(bobX, bobY, 12f, paint)
            }
            PhysicsStencilType.LEVER_BALANCE -> {
                val cx = x + w / 2f
                val cy = y + h * 0.58f
                val p = org.jetbrains.skia.Path().apply {
                    moveTo(cx, cy)
                    lineTo(cx - 14f, cy + 24f)
                    lineTo(cx + 14f, cy + 24f)
                    closePath()
                }
                canvas.drawPath(p, paint)
                canvas.drawLine(x + 12f, cy, x + w - 12f, cy, paint)
                canvas.drawRect(org.jetbrains.skia.Rect.makeXYWH(x + 20f, cy - 20f, 18f, 20f), paint)
                canvas.drawRect(org.jetbrains.skia.Rect.makeXYWH(x + w - 38f, cy - 20f, 18f, 20f), paint)
            }
            PhysicsStencilType.THERMO_CYLINDER -> {
                val left = x + 12f
                val right = x + w - 12f
                val top = y + 20f
                val bot = y + h - 24f
                canvas.drawLine(left, top, right, top, paint)
                canvas.drawLine(left, bot, right, bot, paint)
                canvas.drawLine(left, top, left, bot, paint)
                // Поршень
                val px = left + (right - left) * 0.55f
                val thickP = Paint().apply {
                    color = element.strokeColorHex.toInt()
                    strokeWidth = 5f
                    mode = PaintMode.STROKE
                    isAntiAlias = true
                }
                canvas.drawLine(px, top + 2f, px, bot - 2f, thickP)
                canvas.drawLine(px, (top + bot) / 2f, right + 10f, (top + bot) / 2f, paint)
            }
            PhysicsStencilType.THERMO_PV_CYCLE -> {
                val ox = x + 24f
                val oy = y + h - 24f
                canvas.drawLine(ox, oy, ox, y + 10f, paint)
                canvas.drawLine(ox, oy, x + w - 10f, oy, paint)
                // Прямоугольный цикл
                val cx1 = ox + 25f
                val cx2 = ox + w * 0.55f
                val cy1 = oy - 20f
                val cy2 = oy - h * 0.50f
                val cyclePath = org.jetbrains.skia.Path().apply {
                    moveTo(cx1, cy1)
                    lineTo(cx2, cy1)
                    lineTo(cx2, cy2)
                    lineTo(cx1, cy2)
                    closePath()
                }
                canvas.drawPath(cyclePath, paint)
            }
            PhysicsStencilType.OPTICAL_LENS -> {
                val cx = x + w / 2f
                canvas.drawLine(x + 8f, y + h / 2f, x + w - 8f, y + h / 2f, paint)
                canvas.drawLine(cx, y + 12f, cx, y + h - 12f, paint)
                // Стрелочки на концах собирающей линзы
                canvas.drawLine(cx, y + 12f, cx - 8f, y + 20f, paint)
                canvas.drawLine(cx, y + 12f, cx + 8f, y + 20f, paint)
                canvas.drawLine(cx, y + h - 12f, cx - 8f, y + h - 20f, paint)
                canvas.drawLine(cx, y + h - 12f, cx + 8f, y + h - 20f, paint)
            }
            PhysicsStencilType.OPTICAL_PRISM -> {
                val p = org.jetbrains.skia.Path().apply {
                    moveTo(x + w / 2f, y + 14f)
                    lineTo(x + w - 16f, y + h - 16f)
                    lineTo(x + 16f, y + h - 16f)
                    closePath()
                }
                canvas.drawPath(p, paint)
            }
            PhysicsStencilType.MAGNET_FIELD -> {
                val my = y + 20f
                val mw = w * 0.70f
                val mh = h * 0.40f
                val mx = x + (w - mw) / 2f
                canvas.drawRect(org.jetbrains.skia.Rect.makeXYWH(mx, my, mw / 2f, mh), paint)
                canvas.drawRect(org.jetbrains.skia.Rect.makeXYWH(mx + mw / 2f, my, mw / 2f, mh), paint)
                val mFont = Font(typeface, 14f)
                val mPaint = Paint().apply { color = element.strokeColorHex.toInt(); isAntiAlias = true }
                canvas.drawString("N", mx + mw * 0.20f, my + mh * 0.65f, mFont, mPaint)
                canvas.drawString("S", mx + mw * 0.70f, my + mh * 0.65f, mFont, mPaint)
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

    private fun drawLabel(
        drawScope: DrawScope,
        textMeasurer: TextMeasurer,
        text: String,
        topLeft: Offset,
        style: TextStyle
    ) {
        val layout = textMeasurer.measure(text, style)
        drawScope.drawText(textLayoutResult = layout, topLeft = topLeft)
    }
}
