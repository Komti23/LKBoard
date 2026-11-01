package com.lkboard.app.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
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
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Рендерер инженерных и физических интерактивных стенсилов:
 * - Все физические блоки полностью динамические: геометрия и векторы пересчитываются в 60 FPS
 *   на основе live-параметров (params: Map<String, Float>).
 * - Механика: наклонная плоскость с углом α, бруском и силами (mg, N, Fтр, a); блоки Атвуда с живым
 *   смещением грузов и вектором ускорения; пружина с упругим растяжением; маятники; рычаг моментов.
 * - Термодинамика: цилиндр с подвижным поршнем и сжимаемым газом; цикл p-V с работой.
 * - Оптика: собирающая линза с фокусами и ходом преломленных лучей; призма со спектральной дисперсией.
 * - Электричество: резистор с индикацией I, U, P; реостат с двигающимся ползунком; стрелочные приборы A и V;
 *   конденсатор, катушка, ключ, батарея, лампа; магнитное поле B с силой Лоренца.
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
        val params = element.effectiveParams()

        when (element.stencilType) {
            PhysicsStencilType.INCLINED_PLANE -> drawInclinedPlane(drawScope, x, y, w, h, strokeColor, fillColor, strokeWidth, textMeasurer, element.label, params)
            PhysicsStencilType.PULLEY_BLOCK -> drawPulleyBlock(drawScope, x, y, w, h, strokeColor, fillColor, strokeWidth, textMeasurer, element.label, params)
            PhysicsStencilType.SPRING -> drawSpring(drawScope, x, y, w, h, strokeColor, fillColor, strokeWidth, textMeasurer, element.label, params)
            PhysicsStencilType.MATHEMATICAL_PENDULUM -> drawMathematicalPendulum(drawScope, x, y, w, h, strokeColor, fillColor, strokeWidth, textMeasurer, element.label, params)
            PhysicsStencilType.LEVER_BALANCE -> drawLeverBalance(drawScope, x, y, w, h, strokeColor, fillColor, strokeWidth, textMeasurer, element.label, params)
            PhysicsStencilType.THERMO_CYLINDER -> drawThermoCylinder(drawScope, x, y, w, h, strokeColor, fillColor, strokeWidth, textMeasurer, element.label, params)
            PhysicsStencilType.THERMO_PV_CYCLE -> drawThermoPvCycle(drawScope, x, y, w, h, strokeColor, fillColor, strokeWidth, textMeasurer, element.label, params)
            PhysicsStencilType.OPTICAL_LENS -> drawOpticalLens(drawScope, x, y, w, h, strokeColor, strokeWidth, textMeasurer, element.label, params)
            PhysicsStencilType.OPTICAL_PRISM -> drawOpticalPrism(drawScope, x, y, w, h, strokeColor, fillColor, strokeWidth, textMeasurer, element.label, params)
            PhysicsStencilType.CIRCUIT_RESISTOR -> drawResistor(drawScope, x, y, w, h, strokeColor, fillColor, strokeWidth, textMeasurer, element.label, params)
            PhysicsStencilType.CIRCUIT_BATTERY -> drawBattery(drawScope, x, y, w, h, strokeColor, strokeWidth, textMeasurer, element.label, params)
            PhysicsStencilType.CIRCUIT_CAPACITOR -> drawCapacitor(drawScope, x, y, w, h, strokeColor, strokeWidth, textMeasurer, element.label, params)
            PhysicsStencilType.CIRCUIT_INDUCTOR -> drawInductor(drawScope, x, y, w, h, strokeColor, strokeWidth, textMeasurer, element.label, params)
            PhysicsStencilType.CIRCUIT_SWITCH -> drawSwitch(drawScope, x, y, w, h, strokeColor, strokeWidth, textMeasurer, element.label)
            PhysicsStencilType.CIRCUIT_LAMP -> drawLamp(drawScope, x, y, w, h, strokeColor, strokeWidth, textMeasurer, element.label, params)
            PhysicsStencilType.CIRCUIT_AMMETER -> drawMeter(drawScope, x, y, w, h, strokeColor, fillColor, strokeWidth, textMeasurer, "A", element.label, params)
            PhysicsStencilType.CIRCUIT_VOLTMETER -> drawMeter(drawScope, x, y, w, h, strokeColor, fillColor, strokeWidth, textMeasurer, "V", element.label, params)
            PhysicsStencilType.CIRCUIT_RHEOSTAT -> drawRheostat(drawScope, x, y, w, h, strokeColor, fillColor, strokeWidth, textMeasurer, element.label, params)
            PhysicsStencilType.MAGNET_FIELD -> drawMagnetField(drawScope, x, y, w, h, strokeColor, strokeWidth, textMeasurer, element.label, params)
        }
    }

    // 1. Динамическая наклонная плоскость: угол α физически наклоняет клин, брусок ориентирован по склону, вектор a
    private fun drawInclinedPlane(
        drawScope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        strokeColor: Color, fillColor: Color, strokeWidth: Float,
        textMeasurer: TextMeasurer, label: String,
        params: Map<String, Float>
    ) {
        val angle = (params["angle"] ?: 30f).coerceIn(10f, 75f)
        val m = (params["m"] ?: 2.0f).coerceIn(0.2f, 20f)
        val mu = (params["mu"] ?: 0.15f).coerceIn(0.0f, 0.85f)

        val angleRad = (angle * PI / 180.0).toFloat()
        val g = 9.8f
        val fParallel = m * g * sin(angleRad)
        val fFrictionMax = mu * m * g * cos(angleRad)
        val fNet = maxOf(0f, fParallel - fFrictionMax)
        val a = fNet / m

        val baseLeft = Offset(x + 10f, y + h - 12f)
        val baseRight = Offset(x + w - 10f, y + h - 12f)
        val baseLen = baseRight.x - baseLeft.x

        // Высота клина зависит от угла наклона
        val wedgeHeight = (baseLen * tan(angleRad)).coerceIn(30f, h - 24f)
        val topVertex = Offset(baseLeft.x, baseLeft.y - wedgeHeight)

        // Рисуем клин
        val wedgePath = Path().apply {
            moveTo(baseLeft.x, baseLeft.y)
            lineTo(baseRight.x, baseRight.y)
            lineTo(topVertex.x, topVertex.y)
            close()
        }
        drawScope.drawPath(wedgePath, if (fillColor != Color.Transparent) fillColor else strokeColor.copy(alpha = 0.08f))
        drawScope.drawPath(wedgePath, strokeColor, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Дуга угла α у основания
        val arcR = 26f
        drawScope.drawArc(
            color = strokeColor.copy(alpha = 0.7f),
            startAngle = 180f,
            sweepAngle = angle,
            useCenter = false,
            topLeft = Offset(baseRight.x - arcR, baseRight.y - arcR),
            size = Size(arcR * 2, arcR * 2),
            style = Stroke(1.5f)
        )
        drawLabel(
            drawScope, textMeasurer, "${angle.toInt()}°",
            Offset(baseRight.x - arcR - 22f, baseRight.y - 18f),
            TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = strokeColor)
        )

        // Брусок на наклонной плоскости (примерно на 55% длины склона)
        val t = 0.52f
        val blockCenterOnRamp = Offset(
            topVertex.x + (baseRight.x - topVertex.x) * t,
            topVertex.y + (baseRight.y - topVertex.y) * t
        )

        val blockW = (28f + (m / 10f) * 12f).coerceIn(24f, 40f)
        val blockH = (18f + (m / 10f) * 8f).coerceIn(16f, 26f)

        // Нормаль к поверхности (направлена вверх-вправо от склона)
        val rampAngle = Math.atan2((baseRight.y - topVertex.y).toDouble(), (baseRight.x - topVertex.x).toDouble()).toFloat()
        val normalAngle = rampAngle - (PI / 2.0).toFloat()

        val blockCenter = Offset(
            blockCenterOnRamp.x + (blockH / 2f) * cos(normalAngle),
            blockCenterOnRamp.y + (blockH / 2f) * sin(normalAngle)
        )

        // Рисуем повернутый прямоугольник бруска
        val cosA = cos(rampAngle)
        val sinA = sin(rampAngle)
        val cosN = cos(normalAngle)
        val sinN = sin(normalAngle)

        val halfW = blockW / 2f
        val halfH = blockH / 2f

        val p1 = Offset(blockCenter.x - halfW * cosA - halfH * cosN, blockCenter.y - halfW * sinA - halfH * sinN)
        val p2 = Offset(blockCenter.x + halfW * cosA - halfH * cosN, blockCenter.y + halfW * sinA - halfH * sinN)
        val p3 = Offset(blockCenter.x + halfW * cosA + halfH * cosN, blockCenter.y + halfW * sinA + halfH * sinN)
        val p4 = Offset(blockCenter.x - halfW * cosA + halfH * cosN, blockCenter.y - halfW * sinA + halfH * sinN)

        val blockPath = Path().apply {
            moveTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            lineTo(p3.x, p3.y)
            lineTo(p4.x, p4.y)
            close()
        }
        drawScope.drawPath(blockPath, strokeColor.copy(alpha = 0.18f))
        drawScope.drawPath(blockPath, strokeColor, style = Stroke(strokeWidth, join = StrokeJoin.Round))

        // Сила тяжести mg (строго вертикально вниз)
        val mgLen = (24f + (m / 10f) * 16f).coerceIn(22f, 42f)
        val mgEnd = Offset(blockCenter.x, blockCenter.y + mgLen)
        drawArrow(drawScope, blockCenter, mgEnd, Color(0xFFDC2626), strokeWidth)

        // Сила реакции опоры N (строго по нормали к склону)
        val nLen = mgLen * cos(angleRad)
        val nEnd = Offset(blockCenter.x + nLen * cos(normalAngle), blockCenter.y + nLen * sin(normalAngle))
        drawArrow(drawScope, blockCenter, nEnd, Color(0xFF16A34A), strokeWidth)

        // Сила трения Fтр (вдоль склона вверх)
        if (mu > 0.01f) {
            val fTrLen = (nLen * mu).coerceIn(8f, 32f)
            val fTrEnd = Offset(blockCenter.x - fTrLen * cosA, blockCenter.y - fTrLen * sinA)
            drawArrow(drawScope, blockCenter, fTrEnd, Color(0xFFD97706), strokeWidth)
        }

        // Вектор ускорения a (вдоль склона вниз, если a > 0)
        if (a > 0.1f) {
            val aStart = Offset(blockCenter.x + 12f * cosN, blockCenter.y + 12f * sinN)
            val aLen = (18f + a * 2.5f).coerceIn(16f, 36f)
            val aEnd = Offset(aStart.x + aLen * cosA, aStart.y + aLen * sinA)
            drawArrow(drawScope, aStart, aEnd, Color(0xFF2563EB), strokeWidth * 1.2f)
            drawLabel(drawScope, textMeasurer, "a", Offset(aEnd.x + 2f, aEnd.y - 10f), TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB)))
        }

        // Плашка с живыми расчетами
        val displayTxt = if (label.isNotBlank()) label else "m=${"%.1f".format(m)}кг, α=${angle.toInt()}°, a=${"%.2f".format(a)}м/с²"
        drawLabel(
            drawScope, textMeasurer, displayTxt,
            Offset(x + 12f, y + 6f),
            TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = strokeColor)
        )
    }

    // 2. Динамический блок Атвуда с нитью и двумя грузами: высоты и стрелки a и T адаптируются к массам m1 и m2
    private fun drawPulleyBlock(
        drawScope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        strokeColor: Color, fillColor: Color, strokeWidth: Float,
        textMeasurer: TextMeasurer, label: String,
        params: Map<String, Float>
    ) {
        val m1 = params["m1"] ?: 3.0f
        val m2 = params["m2"] ?: 1.0f

        val totalMass = maxOf(0.1f, m1 + m2)
        val a = abs(m1 - m2) * 9.8f / totalMass
        val tensionT = (2f * m1 * m2 * 9.8f) / totalMass

        val cx = x + w / 2f
        val r = (w * 0.18f).coerceIn(16f, 28f)
        val pulleyY = y + r + 20f

        // Потолок со штриховкой
        drawScope.drawLine(strokeColor, Offset(cx - r * 1.6f, y + 6f), Offset(cx + r * 1.6f, y + 6f), strokeWidth = 3f)
        for (i in -3..3) {
            val hx = cx + i * 10f
            drawScope.drawLine(strokeColor.copy(alpha = 0.45f), Offset(hx, y + 6f), Offset(hx + 6f, y), strokeWidth = 1.5f)
        }
        // Подвес шкива
        drawScope.drawLine(strokeColor, Offset(cx, y + 6f), Offset(cx, pulleyY), strokeWidth = 2.5f)

        // Шкив блока
        drawScope.drawCircle(if (fillColor != Color.Transparent) fillColor else Color.White, r, Offset(cx, pulleyY))
        drawScope.drawCircle(strokeColor, r, Offset(cx, pulleyY), style = Stroke(strokeWidth))
        drawScope.drawCircle(strokeColor, 3.5f, Offset(cx, pulleyY))

        // Динамический расчет высот грузов на основе разницы масс m1 и m2:
        // Тяжелый груз опускается ниже, легкий поднимается выше!
        val massDiffRatio = ((m1 - m2) / totalMass).coerceIn(-0.35f, 0.35f)
        val leftLoadY = (pulleyY + h * (0.42f + massDiffRatio)).coerceIn(pulleyY + r + 15f, y + h - 45f)
        val rightLoadY = (pulleyY + h * (0.42f - massDiffRatio)).coerceIn(pulleyY + r + 15f, y + h - 45f)

        val leftX = cx - r
        val rightX = cx + r

        // Нити от шкива к грузам
        drawScope.drawLine(strokeColor, Offset(leftX, pulleyY), Offset(leftX, leftLoadY), strokeWidth = 2f)
        drawScope.drawLine(strokeColor, Offset(rightX, pulleyY), Offset(rightX, rightLoadY), strokeWidth = 2f)

        // Размеры грузов масштабируются пропорционально их массе
        val minBox = 22f
        val maxBox = 38f
        val load1W = (minBox + (m1 / 10f) * (maxBox - minBox)).coerceIn(minBox, maxBox)
        val load1H = load1W * 0.95f
        val load2W = (minBox + (m2 / 10f) * (maxBox - minBox)).coerceIn(minBox, maxBox)
        val load2H = load2W * 0.95f

        // Груз m1 слева
        val r1 = androidx.compose.ui.geometry.Rect(leftX - load1W / 2f, leftLoadY, leftX + load1W / 2f, leftLoadY + load1H)
        drawScope.drawRoundRect(strokeColor.copy(alpha = 0.15f), r1.topLeft, r1.size, androidx.compose.ui.geometry.CornerRadius(4f, 4f))
        drawScope.drawRoundRect(strokeColor, r1.topLeft, r1.size, androidx.compose.ui.geometry.CornerRadius(4f, 4f), style = Stroke(strokeWidth))
        drawLabel(drawScope, textMeasurer, "${"%.1f".format(m1)}кг", Offset(leftX - 14f, leftLoadY + load1H * 0.28f), TextStyle(fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = strokeColor))

        // Груз m2 справа
        val r2 = androidx.compose.ui.geometry.Rect(rightX - load2W / 2f, rightLoadY, rightX + load2W / 2f, rightLoadY + load2H)
        drawScope.drawRoundRect(strokeColor.copy(alpha = 0.15f), r2.topLeft, r2.size, androidx.compose.ui.geometry.CornerRadius(4f, 4f))
        drawScope.drawRoundRect(strokeColor, r2.topLeft, r2.size, androidx.compose.ui.geometry.CornerRadius(4f, 4f), style = Stroke(strokeWidth))
        drawLabel(drawScope, textMeasurer, "${"%.1f".format(m2)}кг", Offset(rightX - 14f, rightLoadY + load2H * 0.28f), TextStyle(fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = strokeColor))

        // Стрелки натяжения нити T вверх от грузов
        drawArrow(drawScope, Offset(leftX, leftLoadY), Offset(leftX, leftLoadY - 18f), Color(0xFF16A34A), strokeWidth)
        drawArrow(drawScope, Offset(rightX, rightLoadY), Offset(rightX, rightLoadY - 18f), Color(0xFF16A34A), strokeWidth)

        // Стрелка ускорения a рядом с движущимся грузом
        if (a > 0.15f) {
            if (m1 > m2) {
                // m1 падает вниз, m2 поднимается вверх
                drawArrow(drawScope, Offset(leftX - load1W / 2f - 8f, leftLoadY + 4f), Offset(leftX - load1W / 2f - 8f, leftLoadY + 28f), Color(0xFF2563EB), strokeWidth * 1.1f)
                drawLabel(drawScope, textMeasurer, "a", Offset(leftX - load1W / 2f - 20f, leftLoadY + 10f), TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB)))
            } else {
                drawArrow(drawScope, Offset(rightX + load2W / 2f + 8f, rightLoadY + 4f), Offset(rightX + load2W / 2f + 8f, rightLoadY + 28f), Color(0xFF2563EB), strokeWidth * 1.1f)
                drawLabel(drawScope, textMeasurer, "a", Offset(rightX + load2W / 2f + 14f, rightLoadY + 10f), TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB)))
            }
        }

        // Информационная плашка
        val summaryTxt = if (label.isNotBlank()) label else "a=${"%.2f".format(a)}м/с², T=${"%.1f".format(tensionT)}Н"
        drawLabel(drawScope, textMeasurer, summaryTxt, Offset(x + 10f, y + h - 16f), TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = strokeColor))
    }

    // 3. Динамическая пружина с грузом: растяжение Δx = mg/k адаптирует шаг витков и позицию груза
    private fun drawSpring(
        drawScope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        strokeColor: Color, fillColor: Color, strokeWidth: Float,
        textMeasurer: TextMeasurer, label: String,
        params: Map<String, Float>
    ) {
        val m = params["m"] ?: 2.0f
        val k = params["k"] ?: 100f
        val deltaXCm = (m * 9.8f / maxOf(10f, k)) * 100f // см
        val periodT = 2f * PI.toFloat() * sqrt(m / maxOf(1f, k))

        val cx = x + w / 2f
        // Потолок
        drawScope.drawLine(strokeColor, Offset(cx - 36f, y + 6f), Offset(cx + 36f, y + 6f), strokeWidth = 3f)
        for (i in -3..3) {
            drawScope.drawLine(strokeColor.copy(alpha = 0.4f), Offset(cx + i * 9f, y + 6f), Offset(cx + i * 9f + 6f, y + 1f), strokeWidth = 1.5f)
        }

        val springTop = y + 14f
        // Растяжение пружины на холсте в пикселях
        val stretchPix = ((deltaXCm / 100f) * 60f).coerceIn(0f, h * 0.35f)
        val springBottom = (y + h * 0.45f + stretchPix).coerceIn(springTop + 35f, y + h - 50f)

        val coils = 8
        val dy = (springBottom - springTop) / coils
        val coilW = w * 0.16f

        val path = Path().apply {
            moveTo(cx, springTop)
            for (i in 0 until coils) {
                val cyStep = springTop + i * dy
                val side = if (i % 2 == 0) -coilW else coilW
                quadraticBezierTo(cx + side * 1.2f, cyStep + dy * 0.5f, cx, cyStep + dy)
            }
        }
        drawScope.drawPath(path, strokeColor, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Груз m
        val loadW = (28f + (m / 10f) * 16f).coerceIn(24f, 48f)
        val loadH = (22f + (m / 10f) * 12f).coerceIn(18f, 36f)

        drawScope.drawRoundRect(
            color = strokeColor.copy(alpha = 0.20f),
            topLeft = Offset(cx - loadW / 2f, springBottom),
            size = Size(loadW, loadH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f, 5f)
        )
        drawScope.drawRoundRect(
            color = strokeColor,
            topLeft = Offset(cx - loadW / 2f, springBottom),
            size = Size(loadW, loadH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f, 5f),
            style = Stroke(strokeWidth)
        )
        drawLabel(drawScope, textMeasurer, "${"%.1f".format(m)}кг", Offset(cx - 12f, springBottom + loadH * 0.25f), TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = strokeColor))

        // Векторы сил: Fупр вверх, mg вниз
        drawArrow(drawScope, Offset(cx, springBottom), Offset(cx, springBottom - 20f), Color(0xFF16A34A), strokeWidth)
        drawArrow(drawScope, Offset(cx, springBottom + loadH), Offset(cx, springBottom + loadH + 20f), Color(0xFFDC2626), strokeWidth)

        val txt = if (label.isNotBlank()) label else "Δx=${"%.1f".format(deltaXCm)}см, T=${"%.2f".format(periodT)}с"
        drawLabel(drawScope, textMeasurer, txt, Offset(x + 10f, y + h - 16f), TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = strokeColor))
    }

    // 4. Оптическая линза: фокусное расстояние F и предмет d адаптируют фокусы и ход лучей
    private fun drawOpticalLens(
        drawScope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        strokeColor: Color, strokeWidth: Float,
        textMeasurer: TextMeasurer, label: String,
        params: Map<String, Float>
    ) {
        val fCm = (params["F"] ?: 20.0f).coerceIn(5f, 50f)
        val dCm = (params["d"] ?: 30.0f).coerceIn(6f, 100f)
        val dP = 100f / fCm

        val cy = y + h / 2f
        val cx = x + w / 2f

        // Главная оптическая ось
        drawScope.drawLine(strokeColor.copy(alpha = 0.5f), Offset(x, cy), Offset(x + w, cy), strokeWidth = 1.2f)

        // Линза
        val lensTop = y + 10f
        val lensBottom = y + h - 10f
        drawScope.drawLine(strokeColor, Offset(cx, lensTop), Offset(cx, lensBottom), strokeWidth = strokeWidth * 1.3f)

        // Стрелки наружу (собирающая линза)
        val arrSz = 8f
        drawScope.drawLine(strokeColor, Offset(cx - arrSz, lensTop + arrSz), Offset(cx, lensTop), strokeWidth = strokeWidth)
        drawScope.drawLine(strokeColor, Offset(cx + arrSz, lensTop + arrSz), Offset(cx, lensTop), strokeWidth = strokeWidth)
        drawScope.drawLine(strokeColor, Offset(cx - arrSz, lensBottom - arrSz), Offset(cx, lensBottom), strokeWidth = strokeWidth)
        drawScope.drawLine(strokeColor, Offset(cx + arrSz, lensBottom - arrSz), Offset(cx, lensBottom), strokeWidth = strokeWidth)

        // Фокусное расстояние на экране пропорционально F
        val focalDist = (w * 0.18f * (fCm / 20f)).coerceIn(25f, w * 0.38f)
        val f1X = cx - focalDist
        val f2X = cx + focalDist

        // Засечки фокусов F и F'
        drawScope.drawLine(strokeColor, Offset(f1X, cy - 6f), Offset(f1X, cy + 6f), strokeWidth = 2f)
        drawScope.drawLine(strokeColor, Offset(f2X, cy - 6f), Offset(f2X, cy + 6f), strokeWidth = 2f)
        drawLabel(drawScope, textMeasurer, "F", Offset(f1X - 5f, cy + 8f), TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = strokeColor))
        drawLabel(drawScope, textMeasurer, "F'", Offset(f2X - 5f, cy + 8f), TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = strokeColor))

        // Ход лучей: луч 1 параллелен оси -> преломляется через задний фокус F'
        val rayY = cy - h * 0.28f
        drawScope.drawLine(Color(0xFF38BDF8), Offset(x + 12f, rayY), Offset(cx, rayY), strokeWidth = 1.8f)
        val ray1Slope = (cy - rayY) / (f2X - cx)
        val ray1EndX = (x + w - 10f)
        val ray1EndY = cy + ray1Slope * (ray1EndX - f2X)
        drawScope.drawLine(Color(0xFF38BDF8), Offset(cx, rayY), Offset(ray1EndX, ray1EndY), strokeWidth = 1.8f)

        // Луч 2 через оптический центр (идет без преломления)
        drawScope.drawLine(Color(0xFF818CF8), Offset(x + 12f, rayY), Offset(cx, cy), strokeWidth = 1.5f)
        drawScope.drawLine(Color(0xFF818CF8), Offset(cx, cy), Offset(ray1EndX, cy + (cy - rayY) * ((ray1EndX - cx) / (cx - (x + 12f)))), strokeWidth = 1.5f)

        val txt = if (label.isNotBlank()) label else "F=${fCm.toInt()}см, D=${"%.1f".format(dP)}дптр, d=${dCm.toInt()}см"
        drawLabel(drawScope, textMeasurer, txt, Offset(x + 10f, y + h - 16f), TextStyle(fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = strokeColor))
    }

    // 5. Динамический резистор с расчетом тока и мощности
    private fun drawResistor(
        drawScope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        strokeColor: Color, fillColor: Color, strokeWidth: Float,
        textMeasurer: TextMeasurer, label: String,
        params: Map<String, Float>
    ) {
        val r = params["R"] ?: 6.0f
        val u = params["U"] ?: 12.0f
        val i = if (r > 0f) u / r else 0f
        val p = u * i

        val cy = y + h / 2f
        val bodyW = w * 0.55f
        val bodyH = h * 0.42f
        val bodyX = x + (w - bodyW) / 2f
        val bodyY = cy - bodyH / 2f

        // Выводы слева и справа
        drawScope.drawLine(strokeColor, Offset(x, cy), Offset(bodyX, cy), strokeWidth = strokeWidth)
        drawScope.drawLine(strokeColor, Offset(bodyX + bodyW, cy), Offset(x + w, cy), strokeWidth = strokeWidth)

        // Стрелки направления тока I на проводах
        drawArrow(drawScope, Offset(x + (bodyX - x) * 0.3f, cy), Offset(x + (bodyX - x) * 0.8f, cy), Color(0xFF2563EB), strokeWidth)

        // Прямоугольник резистора
        drawScope.drawRect(if (fillColor != Color.Transparent) fillColor else strokeColor.copy(alpha = 0.08f), Offset(bodyX, bodyY), Size(bodyW, bodyH))
        drawScope.drawRect(strokeColor, Offset(bodyX, bodyY), Size(bodyW, bodyH), style = Stroke(strokeWidth))

        // Живая надпись параметров по ГОСТ
        val displayTxt = if (label.isNotBlank()) label else "R=${"%.1f".format(r)}Ом, I=${"%.2f".format(i)}А, P=${"%.1f".format(p)}Вт"
        drawLabel(drawScope, textMeasurer, displayTxt, Offset(bodyX - 10f, bodyY - 18f), TextStyle(fontSize = 11.5.sp, color = strokeColor, fontWeight = FontWeight.Bold))
    }

    // 6. Источник тока (Батарейка)
    private fun drawBattery(
        drawScope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        strokeColor: Color, strokeWidth: Float,
        textMeasurer: TextMeasurer, label: String,
        params: Map<String, Float>
    ) {
        val u = (params["U"] ?: 12.0f).toInt()
        val cy = y + h / 2f
        val cx = x + w / 2f
        val gap = 14f
        val longPlateH = h * 0.72f
        val shortPlateH = h * 0.42f

        drawScope.drawLine(strokeColor, Offset(x, cy), Offset(cx - gap / 2f, cy), strokeWidth = strokeWidth)
        drawScope.drawLine(strokeColor, Offset(cx + gap / 2f, cy), Offset(x + w, cy), strokeWidth = strokeWidth)

        // Длинная тонкая (+) пластина
        drawScope.drawLine(strokeColor, Offset(cx - gap / 2f, cy - longPlateH / 2f), Offset(cx - gap / 2f, cy + longPlateH / 2f), strokeWidth = 2.2f)
        // Короткая толстая (-) пластина
        drawScope.drawLine(strokeColor, Offset(cx + gap / 2f, cy - shortPlateH / 2f), Offset(cx + gap / 2f, cy + shortPlateH / 2f), strokeWidth = 5f)

        drawLabel(drawScope, textMeasurer, "+", Offset(cx - gap - 10f, cy - longPlateH / 2f - 4f), TextStyle(fontSize = 13.sp, color = strokeColor, fontWeight = FontWeight.Bold))
        drawLabel(drawScope, textMeasurer, "-", Offset(cx + gap + 4f, cy - shortPlateH / 2f - 6f), TextStyle(fontSize = 15.sp, color = strokeColor, fontWeight = FontWeight.Bold))
        val txt = if (label.isNotBlank()) label else "U=${u}В"
        drawLabel(drawScope, textMeasurer, txt, Offset(cx - 14f, cy + longPlateH / 2f + 4f), TextStyle(fontSize = 11.sp, color = strokeColor, fontWeight = FontWeight.Bold))
    }

    // 7. Конденсатор с расчетом энергии
    private fun drawCapacitor(
        drawScope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        strokeColor: Color, strokeWidth: Float,
        textMeasurer: TextMeasurer, label: String,
        params: Map<String, Float>
    ) {
        val c = (params["C"] ?: 47.0f).toInt()
        val u = params["U"] ?: 12.0f
        val wEnergy = 0.5f * (c * 1e-6f) * u * u * 1000f

        val cy = y + h / 2f
        val cx = x + w / 2f
        val gap = 12f
        val plateH = h * 0.65f

        drawScope.drawLine(strokeColor, Offset(x, cy), Offset(cx - gap / 2f, cy), strokeWidth = strokeWidth)
        drawScope.drawLine(strokeColor, Offset(cx + gap / 2f, cy), Offset(x + w, cy), strokeWidth = strokeWidth)
        drawScope.drawLine(strokeColor, Offset(cx - gap / 2f, cy - plateH / 2f), Offset(cx - gap / 2f, cy + plateH / 2f), strokeWidth = 3.5f)
        drawScope.drawLine(strokeColor, Offset(cx + gap / 2f, cy - plateH / 2f), Offset(cx + gap / 2f, cy + plateH / 2f), strokeWidth = 3.5f)

        val txt = if (label.isNotBlank()) label else "C=${c}мкФ, W=${"%.2f".format(wEnergy)}мДж"
        drawLabel(drawScope, textMeasurer, txt, Offset(cx - 24f, cy - plateH / 2f - 18f), TextStyle(fontSize = 11.sp, color = strokeColor, fontWeight = FontWeight.Bold))
    }

    // 8. Катушка индуктивности
    private fun drawInductor(
        drawScope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        strokeColor: Color, strokeWidth: Float,
        textMeasurer: TextMeasurer, label: String,
        params: Map<String, Float>
    ) {
        val l = (params["L"] ?: 10.0f).toInt()
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

        val txt = if (label.isNotBlank()) label else "L=${l}мГн"
        drawLabel(drawScope, textMeasurer, txt, Offset(bodyX + bodyW / 2f - 14f, cy - r - 16f), TextStyle(fontSize = 11.5.sp, color = strokeColor, fontWeight = FontWeight.Bold))
    }

    // 9. Ключ цепи
    private fun drawSwitch(
        drawScope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        strokeColor: Color, strokeWidth: Float,
        textMeasurer: TextMeasurer, label: String
    ) {
        val cy = y + h / 2f
        val cx = x + w / 2f
        val gap = 28f

        drawScope.drawLine(strokeColor, Offset(x, cy), Offset(cx - gap / 2f, cy), strokeWidth = strokeWidth)
        drawScope.drawLine(strokeColor, Offset(cx + gap / 2f, cy), Offset(x + w, cy), strokeWidth = strokeWidth)
        drawScope.drawCircle(strokeColor, 4f, Offset(cx - gap / 2f, cy), style = Stroke(strokeWidth))
        drawScope.drawCircle(strokeColor, 4f, Offset(cx + gap / 2f, cy), style = Stroke(strokeWidth))

        drawScope.drawLine(strokeColor, Offset(cx - gap / 2f, cy), Offset(cx + gap / 2f - 4f, cy - 18f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
        val txt = if (label.isNotBlank()) label else "Ключ К1"
        drawLabel(drawScope, textMeasurer, txt, Offset(cx - 10f, cy - 32f), TextStyle(fontSize = 11.sp, color = strokeColor, fontWeight = FontWeight.Bold))
    }

    // 10. Лампочка с накалом
    private fun drawLamp(
        drawScope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        strokeColor: Color, strokeWidth: Float,
        textMeasurer: TextMeasurer, label: String,
        params: Map<String, Float>
    ) {
        val cy = y + h / 2f
        val cx = x + w / 2f
        val r = h * 0.32f

        drawScope.drawLine(strokeColor, Offset(x, cy), Offset(cx - r, cy), strokeWidth = strokeWidth)
        drawScope.drawLine(strokeColor, Offset(cx + r, cy), Offset(x + w, cy), strokeWidth = strokeWidth)
        drawScope.drawCircle(Color(0xFFFEF3C7).copy(alpha = 0.35f), r, Offset(cx, cy))
        drawScope.drawCircle(strokeColor, r, Offset(cx, cy), style = Stroke(strokeWidth))

        val crossR = r * 0.707f
        drawScope.drawLine(strokeColor, Offset(cx - crossR, cy - crossR), Offset(cx + crossR, cy + crossR), strokeWidth = strokeWidth * 0.85f)
        drawScope.drawLine(strokeColor, Offset(cx - crossR, cy + crossR), Offset(cx + crossR, cy - crossR), strokeWidth = strokeWidth * 0.85f)

        if (label.isNotBlank()) {
            drawLabel(drawScope, textMeasurer, label, Offset(x + 10f, y + 6f), TextStyle(fontSize = 10.5.sp, color = strokeColor, fontWeight = FontWeight.Bold))
        }
    }

    // 11. Нитяной маятник: длина нити и период адаптируются
    private fun drawMathematicalPendulum(
        drawScope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        strokeColor: Color, fillColor: Color, strokeWidth: Float,
        textMeasurer: TextMeasurer, label: String,
        params: Map<String, Float>
    ) {
        val l = params["L"] ?: 1.0f
        val g = params["g"] ?: 9.8f
        val tPeriod = if (g > 0f && l > 0f) 2f * PI.toFloat() * sqrt(l / g) else 0f
        val nu = if (tPeriod > 0f) 1f / tPeriod else 0f

        val ceilingY = y + 14f
        val pivotX = x + w * 0.45f

        drawScope.drawLine(strokeColor, Offset(x + w * 0.15f, ceilingY), Offset(x + w * 0.75f, ceilingY), strokeWidth = strokeWidth * 1.2f)
        for (i in 0..6) {
            val hx = x + w * 0.18f + i * (w * 0.08f)
            drawScope.drawLine(strokeColor.copy(alpha = 0.5f), Offset(hx, ceilingY), Offset(hx + 8f, ceilingY - 8f), strokeWidth = strokeWidth * 0.8f)
        }

        drawScope.drawLine(strokeColor.copy(alpha = 0.3f), Offset(pivotX, ceilingY), Offset(pivotX, y + h * 0.85f), strokeWidth = 1f)

        val angleRad = (22.0 * PI / 180.0).toFloat()
        // Длина нити на экране масштабируется пропорционально длине L
        val stringLen = (h * 0.50f + (l / 2f) * (h * 0.25f)).coerceIn(h * 0.40f, h * 0.75f)
        val bobX = pivotX + (stringLen * sin(angleRad))
        val bobY = ceilingY + (stringLen * cos(angleRad))
        val bobR = 13f

        drawScope.drawLine(strokeColor, Offset(pivotX, ceilingY), Offset(bobX, bobY), strokeWidth = strokeWidth)
        drawScope.drawCircle(if (fillColor != Color.Transparent) fillColor else Color(0xFF2563EB).copy(alpha = 0.20f), bobR, Offset(bobX, bobY))
        drawScope.drawCircle(strokeColor, bobR, Offset(bobX, bobY), style = Stroke(strokeWidth))

        // Сила тяжести mg
        drawArrow(drawScope, Offset(bobX, bobY), Offset(bobX, bobY + 28f), Color(0xFFDC2626), strokeWidth)

        // Сила натяжения нити T
        val tensionEndX = bobX - 24f * sin(angleRad)
        val tensionEndY = bobY - 24f * cos(angleRad)
        drawArrow(drawScope, Offset(bobX, bobY), Offset(tensionEndX, tensionEndY), Color(0xFF16A34A), strokeWidth)

        val txt = if (label.isNotBlank()) label else "L=${"%.2f".format(l)}м, T=${"%.2f".format(tPeriod)}с, ν=${"%.2f".format(nu)}Гц"
        drawLabel(drawScope, textMeasurer, txt, Offset(x + 10f, y + h - 18f), TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = strokeColor))
    }

    // 12. Рычаг и правило моментов: опора смещается по отношению d1 : d2
    private fun drawLeverBalance(
        drawScope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        strokeColor: Color, fillColor: Color, strokeWidth: Float,
        textMeasurer: TextMeasurer, label: String,
        params: Map<String, Float>
    ) {
        val d1 = (params["d1"] ?: 40f).toInt()
        val d2 = (params["d2"] ?: 20f).toInt()
        val m1 = params["m1"] ?: 2.0f
        val m2 = if (d2 > 0) m1 * d1 / d2 else 0f

        val ratio = (d1.toFloat() / maxOf(1f, (d1 + d2).toFloat())).coerceIn(0.2f, 0.8f)
        val fulcrumX = x + 16f + (w - 32f) * ratio
        val fulcrumY = y + h * 0.60f
        val beamY = fulcrumY

        // Треугольная опора
        val triPath = Path().apply {
            moveTo(fulcrumX, fulcrumY)
            lineTo(fulcrumX - 16f, fulcrumY + 28f)
            lineTo(fulcrumX + 16f, fulcrumY + 28f)
            close()
        }
        drawScope.drawPath(triPath, strokeColor.copy(alpha = 0.2f))
        drawScope.drawPath(triPath, strokeColor, style = Stroke(strokeWidth))
        drawScope.drawLine(strokeColor, Offset(fulcrumX - 28f, fulcrumY + 28f), Offset(fulcrumX + 28f, fulcrumY + 28f), strokeWidth = strokeWidth)

        // Балка рычага
        drawScope.drawLine(strokeColor, Offset(x + 12f, beamY), Offset(x + w - 12f, beamY), strokeWidth = strokeWidth * 1.6f)

        // Левый груз (m1) на плече d1
        val load1X = x + 24f
        val box1H = (16f + (m1 / 10f) * 16f).coerceIn(16f, 32f)
        drawScope.drawRect(strokeColor.copy(alpha = 0.15f), Offset(load1X - 12f, beamY - box1H), Size(24f, box1H))
        drawScope.drawRect(strokeColor, Offset(load1X - 12f, beamY - box1H), Size(24f, box1H), style = Stroke(strokeWidth))
        drawArrow(drawScope, Offset(load1X, beamY), Offset(load1X, beamY + 26f), Color(0xFFDC2626), strokeWidth)

        // Правый груз (m2) на плече d2
        val load2X = x + w - 24f
        val box2H = (16f + (m2 / 10f) * 16f).coerceIn(16f, 32f)
        drawScope.drawRect(strokeColor.copy(alpha = 0.15f), Offset(load2X - 12f, beamY - box2H), Size(24f, box2H))
        drawScope.drawRect(strokeColor, Offset(load2X - 12f, beamY - box2H), Size(24f, box2H), style = Stroke(strokeWidth))
        drawArrow(drawScope, Offset(load2X, beamY), Offset(load2X, beamY + 26f), Color(0xFFDC2626), strokeWidth)

        // Стрелки плеч d1 и d2
        drawLabel(drawScope, textMeasurer, "d₁=${d1}см", Offset((load1X + fulcrumX) / 2f - 18f, beamY - 18f), TextStyle(fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = strokeColor))
        drawLabel(drawScope, textMeasurer, "d₂=${d2}см", Offset((fulcrumX + load2X) / 2f - 18f, beamY - 18f), TextStyle(fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = strokeColor))

        val txt = if (label.isNotBlank()) label else "m₁·d₁ = m₂·d₂ -> m₂=${"%.1f".format(m2)}кг"
        drawLabel(drawScope, textMeasurer, txt, Offset(x + 10f, y + 6f), TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = strokeColor))
    }

    // 13. Термодинамический цилиндр: поршень физически смещается с объемом V, газ сжимается
    private fun drawThermoCylinder(
        drawScope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        strokeColor: Color, fillColor: Color, strokeWidth: Float,
        textMeasurer: TextMeasurer, label: String,
        params: Map<String, Float>
    ) {
        val p = (params["p"] ?: 100f).toInt()
        val v = params["V"] ?: 5.0f
        val nu = params["nu"] ?: 1.0f
        val tK = if (nu > 0f) (p * 1000f * (v * 1e-3f)) / (nu * 8.314f) else 0f

        val cx = x + w * 0.15f
        val cw = w * 0.70f
        val ch = h * 0.75f
        val cy = y + h * 0.12f

        // Корпус цилиндра
        val cylPath = Path().apply {
            moveTo(cx, cy)
            lineTo(cx, cy + ch)
            lineTo(cx + cw, cy + ch)
            lineTo(cx + cw, cy)
        }
        drawScope.drawPath(cylPath, strokeColor, style = Stroke(strokeWidth * 1.3f))

        // Положение поршня зависит от объема V (больше V -> поршень выше)
        val vRatio = (v / 12f).coerceIn(0.18f, 0.85f)
        val pistonY = cy + ch * (1f - vRatio)
        val pistonH = 14f

        // Газовая область под поршнем (теплый оттенок при высоком давлении)
        val gasAlpha = (p / 400f).coerceIn(0.08f, 0.35f)
        val gasColor = if (p > 150) Color(0xFFEA580C) else Color(0xFF38BDF8)
        drawScope.drawRect(gasColor.copy(alpha = gasAlpha), Offset(cx + 2f, pistonY + pistonH), Size(cw - 4f, (cy + ch) - (pistonY + pistonH)))

        // Поршень со штоком
        drawScope.drawRoundRect(strokeColor.copy(alpha = 0.30f), Offset(cx + 2f, pistonY), Size(cw - 4f, pistonH), androidx.compose.ui.geometry.CornerRadius(3f, 3f))
        drawScope.drawRoundRect(strokeColor, Offset(cx + 2f, pistonY), Size(cw - 4f, pistonH), androidx.compose.ui.geometry.CornerRadius(3f, 3f), style = Stroke(strokeWidth))

        val rodX = cx + cw / 2f
        drawScope.drawLine(strokeColor, Offset(rodX, pistonY), Offset(rodX, y + 4f), strokeWidth = strokeWidth * 1.5f)

        // Стрелки давления p на поршень снизу вверх
        drawArrow(drawScope, Offset(rodX - cw * 0.22f, pistonY + pistonH + 16f), Offset(rodX - cw * 0.22f, pistonY + pistonH + 2f), Color(0xFFD97706), strokeWidth)
        drawArrow(drawScope, Offset(rodX + cw * 0.22f, pistonY + pistonH + 16f), Offset(rodX + cw * 0.22f, pistonY + pistonH + 2f), Color(0xFFD97706), strokeWidth)

        val txt = if (label.isNotBlank()) label else "p=${p}кПа, V=${"%.1f".format(v)}л, T=${tK.toInt()}K"
        drawLabel(drawScope, textMeasurer, txt, Offset(x + 10f, y + h - 16f), TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = strokeColor))
    }

    // 14. График замкнутого цикла p-V
    private fun drawThermoPvCycle(
        drawScope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        strokeColor: Color, fillColor: Color, strokeWidth: Float,
        textMeasurer: TextMeasurer, label: String,
        params: Map<String, Float>
    ) {
        val p1 = (params["p1"] ?: 100f).toInt()
        val p2 = (params["p2"] ?: 300f).toInt()
        val v1 = params["V1"] ?: 2.0f
        val v2 = params["V2"] ?: 6.0f
        val workCycle = kotlin.math.abs(p2 - p1) * 1000f * (kotlin.math.abs(v2 - v1) * 1e-3f)

        val ox = x + 26f
        val oy = y + h - 24f

        drawArrow(drawScope, Offset(ox, oy), Offset(ox, y + 6f), strokeColor, strokeWidth)
        drawArrow(drawScope, Offset(ox, oy), Offset(x + w - 6f, oy), strokeColor, strokeWidth)
        drawLabel(drawScope, textMeasurer, "p", Offset(ox - 14f, y + 2f), TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = strokeColor))
        drawLabel(drawScope, textMeasurer, "V", Offset(x + w - 16f, oy + 4f), TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = strokeColor))

        val pt1 = Offset(ox + w * 0.20f, oy - h * 0.24f)
        val pt2 = Offset(ox + w * 0.20f, oy - h * 0.68f)
        val pt3 = Offset(ox + w * 0.68f, oy - h * 0.68f)
        val pt4 = Offset(ox + w * 0.68f, oy - h * 0.24f)

        val cyclePath = Path().apply {
            moveTo(pt1.x, pt1.y)
            lineTo(pt2.x, pt2.y)
            lineTo(pt3.x, pt3.y)
            lineTo(pt4.x, pt4.y)
            close()
        }
        drawScope.drawPath(cyclePath, Color(0xFF2563EB).copy(alpha = 0.12f))
        drawScope.drawPath(cyclePath, strokeColor, style = Stroke(strokeWidth))

        drawArrow(drawScope, Offset(pt1.x, pt1.y), Offset(pt1.x, (pt1.y + pt2.y) / 2f), strokeColor, strokeWidth)
        drawArrow(drawScope, Offset(pt2.x, pt2.y), Offset((pt2.x + pt3.x) / 2f, pt2.y), strokeColor, strokeWidth)
        drawArrow(drawScope, Offset(pt3.x, pt3.y), Offset(pt3.x, (pt3.y + pt4.y) / 2f), strokeColor, strokeWidth)
        drawArrow(drawScope, Offset(pt4.x, pt4.y), Offset((pt4.x + pt1.x) / 2f, pt4.y), strokeColor, strokeWidth)

        val txt = if (label.isNotBlank()) label else "A(цикл)=${"%.1f".format(workCycle)}Дж"
        drawLabel(drawScope, textMeasurer, txt, Offset(ox + w * 0.24f, oy - h * 0.44f), TextStyle(fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = strokeColor))
    }

    // 15. Оптическая призма с дисперсией света
    private fun drawOpticalPrism(
        drawScope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        strokeColor: Color, fillColor: Color, strokeWidth: Float,
        textMeasurer: TextMeasurer, label: String,
        params: Map<String, Float>
    ) {
        val aDeg = (params["A"] ?: 60.0f).toInt()
        val n = params["n"] ?: 1.50f

        val apex = Offset(x + w * 0.5f, y + 14f)
        val left = Offset(x + w * 0.18f, y + h - 18f)
        val right = Offset(x + w * 0.82f, y + h - 18f)

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

        // Разложение на спектр (красный луч отклоняется слабее, фиолетовый сильнее)
        val outRayRed = Offset(x + w - 4f, exitPoint.y + 10f)
        val outRayViolet = Offset(x + w - 4f, exitPoint.y + 24f)
        drawArrow(drawScope, exitPoint, outRayRed, Color(0xFFDC2626), strokeWidth)
        drawArrow(drawScope, exitPoint, outRayViolet, Color(0xFF7C3AED), strokeWidth)

        val txt = if (label.isNotBlank()) label else "A=${aDeg}°, n=${"%.2f".format(n)}"
        drawLabel(drawScope, textMeasurer, txt, Offset(x + 10f, y + h - 16f), TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = strokeColor))
    }

    // 16. Стрелочный измерительный прибор (Амперметр / Вольтметр) с живой стрелкой
    private fun drawMeter(
        drawScope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        strokeColor: Color, fillColor: Color, strokeWidth: Float,
        textMeasurer: TextMeasurer, symbol: String, label: String,
        params: Map<String, Float>
    ) {
        val valCur = if (symbol == "A") (params["I"] ?: 2.4f) else (params["U"] ?: 9.6f)
        val valMax = if (symbol == "A") (params["Imax"] ?: 5.0f) else (params["Umax"] ?: 15.0f)

        val cy = y + h / 2f
        val cx = x + w / 2f
        val r = h * 0.35f

        drawScope.drawLine(strokeColor, Offset(x, cy), Offset(cx - r, cy), strokeWidth = strokeWidth)
        drawScope.drawLine(strokeColor, Offset(cx + r, cy), Offset(x + w, cy), strokeWidth = strokeWidth)
        drawScope.drawCircle(if (fillColor != Color.Transparent) fillColor else Color.White, r, Offset(cx, cy))
        drawScope.drawCircle(strokeColor, r, Offset(cx, cy), style = Stroke(strokeWidth))

        // Символ шкалы A или V
        drawLabel(drawScope, textMeasurer, symbol, Offset(cx - 7f, cy + 3f), TextStyle(fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = strokeColor))

        // Стрелка индикатора, отклоняющаяся на угол в зависимости от значения
        val fraction = (valCur / maxOf(0.1f, valMax)).coerceIn(0f, 1f)
        val needleAngleRad = (-135.0 + fraction * 90.0) * PI / 180.0
        val needleLen = r * 0.72f
        val needleEnd = Offset(cx + (needleLen * cos(needleAngleRad)).toFloat(), cy + (needleLen * sin(needleAngleRad)).toFloat())

        drawScope.drawLine(Color(0xFFDC2626), Offset(cx, cy), needleEnd, strokeWidth = 2f, cap = StrokeCap.Round)
        drawScope.drawCircle(strokeColor, 3f, Offset(cx, cy))

        val txt = if (label.isNotBlank()) label else (if (symbol == "A") "I=${"%.2f".format(valCur)}А" else "U=${"%.1f".format(valCur)}В")
        drawLabel(drawScope, textMeasurer, txt, Offset(x + 10f, y + 4f), TextStyle(fontSize = 11.sp, color = strokeColor, fontWeight = FontWeight.Bold))
    }

    // 17. Реостат с двигающимся ползунком
    private fun drawRheostat(
        drawScope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        strokeColor: Color, fillColor: Color, strokeWidth: Float,
        textMeasurer: TextMeasurer, label: String,
        params: Map<String, Float>
    ) {
        val rCur = (params["Rcur"] ?: 20.0f).toInt()
        val rMax = maxOf(1, (params["Rmax"] ?: 50.0f).toInt())
        val u = params["U"] ?: 12.0f
        val i = if (rCur > 0) u / rCur else 0f

        val cy = y + h * 0.60f
        val bodyW = w * 0.55f
        val bodyH = h * 0.32f
        val bodyX = x + (w - bodyW) / 2f
        val bodyY = cy - bodyH / 2f

        drawScope.drawLine(strokeColor, Offset(x, cy), Offset(bodyX, cy), strokeWidth = strokeWidth)
        drawScope.drawLine(strokeColor, Offset(bodyX + bodyW, cy), Offset(x + w, cy), strokeWidth = strokeWidth)
        drawScope.drawRect(if (fillColor != Color.Transparent) fillColor else strokeColor.copy(alpha = 0.08f), Offset(bodyX, bodyY), Size(bodyW, bodyH))
        drawScope.drawRect(strokeColor, Offset(bodyX, bodyY), Size(bodyW, bodyH), style = Stroke(strokeWidth))

        // Ползунок двигается пропорционально Rcur / Rmax
        val fraction = (rCur.toFloat() / rMax.toFloat()).coerceIn(0.06f, 0.94f)
        val sliderX = bodyX + bodyW * fraction

        drawArrow(drawScope, Offset(sliderX, y + 10f), Offset(sliderX, bodyY), Color(0xFF2563EB), strokeWidth * 1.2f)
        drawScope.drawLine(strokeColor, Offset(x + 10f, y + 10f), Offset(sliderX, y + 10f), strokeWidth = strokeWidth)

        val txt = if (label.isNotBlank()) label else "R=${rCur}/${rMax}Ом, I=${"%.2f".format(i)}А"
        drawLabel(drawScope, textMeasurer, txt, Offset(bodyX + 4f, cy + bodyH / 2f + 4f), TextStyle(fontSize = 11.sp, color = strokeColor, fontWeight = FontWeight.Bold))
    }

    // 18. Полосовой магнит и линии поля
    private fun drawMagnetField(
        drawScope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        strokeColor: Color, strokeWidth: Float,
        textMeasurer: TextMeasurer, label: String,
        params: Map<String, Float>
    ) {
        val b = params["B"] ?: 0.5f
        val i = params["I"] ?: 3.0f

        val my = y + h * 0.40f
        val mw = w * 0.55f
        val mh = h * 0.28f
        val mx = x + (w - mw) / 2f

        drawScope.drawRect(Color(0xFFEF4444), Offset(mx, my), Size(mw / 2f, mh))
        drawLabel(drawScope, textMeasurer, "N", Offset(mx + mw * 0.2f - 6f, my + 4f), TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White))

        drawScope.drawRect(Color(0xFF3B82F6), Offset(mx + mw / 2f, my), Size(mw / 2f, mh))
        drawLabel(drawScope, textMeasurer, "S", Offset(mx + mw * 0.7f - 6f, my + 4f), TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White))

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

        val txt = if (label.isNotBlank()) label else "B=${"%.2f".format(b)}Тл, I=${"%.1f".format(i)}А"
        drawLabel(drawScope, textMeasurer, txt, Offset(x + 10f, y + h - 16f), TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = strokeColor))
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
        val params = element.effectiveParams()

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
            PhysicsStencilType.PULLEY_BLOCK -> {
                val m1 = params["m1"] ?: 3.0f
                val m2 = params["m2"] ?: 1.0f
                val totalMass = maxOf(0.1f, m1 + m2)
                val diffRatio = ((m1 - m2) / totalMass).coerceIn(-0.35f, 0.35f)

                val cx = x + w / 2f
                val r = w * 0.18f
                val pulleyY = y + r + 18f
                canvas.drawLine(cx - r * 1.5f, y + 4f, cx + r * 1.5f, y + 4f, paint)
                canvas.drawLine(cx, y + 4f, cx, pulleyY, paint)
                canvas.drawCircle(cx, pulleyY, r, paint)

                val leftLoadY = pulleyY + h * (0.40f + diffRatio)
                val rightLoadY = pulleyY + h * (0.40f - diffRatio)
                canvas.drawLine(cx - r, pulleyY, cx - r, leftLoadY, paint)
                canvas.drawLine(cx + r, pulleyY, cx + r, rightLoadY, paint)
                canvas.drawRect(org.jetbrains.skia.Rect.makeXYWH(cx - r - 12f, leftLoadY, 24f, 24f), paint)
                canvas.drawRect(org.jetbrains.skia.Rect.makeXYWH(cx + r - 12f, rightLoadY, 24f, 24f), paint)
            }
            PhysicsStencilType.CIRCUIT_RHEOSTAT -> {
                val rCur = (params["Rcur"] ?: 20.0f).toInt()
                val rMax = maxOf(1, (params["Rmax"] ?: 50.0f).toInt())
                val cy = y + h / 2f
                val bodyW = w * 0.55f
                val bodyH = h * 0.45f
                val bodyX = x + (w - bodyW) / 2f
                val bodyY = cy - bodyH / 2f
                canvas.drawLine(x, cy, bodyX, cy, paint)
                canvas.drawLine(bodyX + bodyW, cy, x + w, cy, paint)
                canvas.drawRect(org.jetbrains.skia.Rect.makeXYWH(bodyX, bodyY, bodyW, bodyH), paint)
                val fraction = (rCur.toFloat() / rMax.toFloat()).coerceIn(0.1f, 0.9f)
                val sliderX = bodyX + bodyW * fraction
                canvas.drawLine(sliderX, bodyY - 10f, sliderX, bodyY, paint)
            }
            PhysicsStencilType.INCLINED_PLANE -> {
                val angle = (params["angle"] ?: 30f).coerceIn(10f, 75f)
                val angleRad = (angle * PI / 180.0).toFloat()
                val baseLen = w - 20f
                val wedgeH = (baseLen * tan(angleRad)).coerceIn(25f, h - 25f)
                val p = org.jetbrains.skia.Path().apply {
                    moveTo(x + 10f, y + h - 14f)
                    lineTo(x + 10f + baseLen, y + h - 14f)
                    lineTo(x + 10f, y + h - 14f - wedgeH)
                    closePath()
                }
                canvas.drawPath(p, paint)
            }
            PhysicsStencilType.SPRING -> {
                val cx = x + w / 2f
                canvas.drawLine(cx - 30f, y + 6f, cx + 30f, y + 6f, paint)
                val springBot = y + h * 0.60f
                canvas.drawLine(cx, y + 10f, cx, springBot, paint)
                canvas.drawRect(org.jetbrains.skia.Rect.makeXYWH(cx - 16f, springBot, 32f, 24f), paint)
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
            else -> {
                canvas.drawRect(org.jetbrains.skia.Rect.makeXYWH(x, y, w, h), paint)
            }
        }

        val font = Font(typeface, 11.5f)
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
