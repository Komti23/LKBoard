package com.lkboard.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lkboard.app.model.BoardElement
import com.lkboard.app.util.MathEvaluator

/**
 * Полноценная математическая студия графиков в стиле Desmos.
 * Включает виртуальную экранную математическую клавиатуру, интерактивный предпросмотр в реальном времени
 * и удобный перенос графиков на бесконечный холст.
 */
@Composable
fun FunctionGraphEditDialog(
    element: BoardElement.FunctionGraph,
    onSave: (BoardElement.FunctionGraph) -> Unit,
    onDismiss: () -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    var title by remember { mutableStateOf(element.title) }

    val initialExpressions = if (element.expressions.isEmpty()) listOf("sin(x)") else element.expressions
    val expressions = remember { mutableStateListOf(*initialExpressions.toTypedArray()) }
    var activeExpressionIndex by remember { mutableStateOf(0) }

    var xMinText by remember { mutableStateOf(element.xMin.toString()) }
    var xMaxText by remember { mutableStateOf(element.xMax.toString()) }
    var yMinText by remember { mutableStateOf(element.yMin.toString()) }
    var yMaxText by remember { mutableStateOf(element.yMax.toString()) }

    val paletteColors = listOf(
        0xFF2563EBL, // Blue
        0xFFDC2626L, // Red
        0xFF16A34AL, // Green
        0xFF9333EAL, // Purple
        0xFFD97706L  // Amber
    )

    fun insertIntoActive(str: String) {
        if (expressions.isEmpty()) {
            expressions.add(str)
            activeExpressionIndex = 0
            return
        }
        val safeIdx = activeExpressionIndex.coerceIn(0, expressions.size - 1)
        val cur = expressions[safeIdx]
        expressions[safeIdx] = cur + str
    }

    fun backspaceActive() {
        if (expressions.isEmpty()) return
        val safeIdx = activeExpressionIndex.coerceIn(0, expressions.size - 1)
        val cur = expressions[safeIdx]
        if (cur.isNotEmpty()) {
            expressions[safeIdx] = cur.dropLast(1)
        }
    }

    fun clearActive() {
        if (expressions.isEmpty()) return
        val safeIdx = activeExpressionIndex.coerceIn(0, expressions.size - 1)
        expressions[safeIdx] = ""
    }

    val parsedXMin = xMinText.toFloatOrNull() ?: -10f
    val parsedXMax = xMaxText.toFloatOrNull() ?: 10f
    val parsedYMin = yMinText.toFloatOrNull() ?: -5f
    val parsedYMax = yMaxText.toFloatOrNull() ?: 5f

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 24.dp,
            modifier = Modifier
                .width(960.dp)
                .height(680.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF2563EB)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("📈", fontSize = 16.sp)
                            }
                            Text(
                                text = "Desmos Математическая Студия",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                        Text(
                            text = "Интерактивное построение функций с математической клавиатурой",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Отмена", color = Color(0xFF475569))
                        }
                        Button(
                            onClick = {
                                val validExprs = expressions.map { it.trim() }.filter { it.isNotEmpty() }
                                val finalExprs = if (validExprs.isEmpty()) listOf("x^2") else validExprs
                                val updated = element.copy(
                                    title = title.ifBlank { "График функций" },
                                    expressions = finalExprs,
                                    xMin = parsedXMin,
                                    xMax = parsedXMax,
                                    yMin = parsedYMin,
                                    yMax = parsedYMax,
                                    strokeColorsHex = paletteColors.take(finalExprs.size)
                                )
                                onSave(updated)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("✓ Перенести на холст LKBoard", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2-Column Content Layout
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // LEFT COLUMN: Expressions & Math Keypad
                    Column(
                        modifier = Modifier
                            .weight(1.05f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Заголовок графика") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "Функции f(x):",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )

                        // Expressions list
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            expressions.forEachIndexed { idx, expr ->
                                val colorHex = paletteColors[idx % paletteColors.size]
                                val isActive = activeExpressionIndex == idx

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(
                                            width = if (isActive) 1.5.dp else 1.dp,
                                            color = if (isActive) Color(0xFF2563EB) else Color(0xFFE2E8F0),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .background(if (isActive) Color(0xFFF8FAFC) else Color.White)
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(colorHex))
                                    )
                                    Text(
                                        text = "f${idx + 1}(x) =",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(colorHex)
                                    )
                                    OutlinedTextField(
                                        value = expr,
                                        onValueChange = { expressions[idx] = it },
                                        placeholder = { Text("например: sin(x) или x^2 - 4") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { activeExpressionIndex = idx }
                                    )
                                    if (expressions.size > 1) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .clickable {
                                                    expressions.removeAt(idx)
                                                    if (activeExpressionIndex >= expressions.size) {
                                                        activeExpressionIndex = expressions.size - 1
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("✕", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                        }
                                    }
                                }
                            }

                            if (expressions.size < 5) {
                                Button(
                                    onClick = {
                                        val presets = listOf("cos(x)", "0.5*x^2 - 2", "sqrt(x)", "2*sin(x)", "1 / (x^2 + 1)")
                                        val nextPreset = presets.getOrNull(expressions.size) ?: "x + 1"
                                        expressions.add(nextPreset)
                                        activeExpressionIndex = expressions.size - 1
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("+ Добавить функцию", color = Color(0xFF2563EB), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                            }
                        }

                        // DESMOS VIRTUAL MATH KEYPAD
                        Text(
                            text = "Виртуальная математическая клавиатура:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Row 1: Variables & Powers
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                MathKey("x", onClick = { insertIntoActive("x") }, modifier = Modifier.weight(1f))
                                MathKey("y", onClick = { insertIntoActive("y") }, modifier = Modifier.weight(1f))
                                MathKey("x²", onClick = { insertIntoActive("^2") }, modifier = Modifier.weight(1f))
                                MathKey("x^", onClick = { insertIntoActive("^") }, modifier = Modifier.weight(1f))
                                MathKey("√x", onClick = { insertIntoActive("sqrt(") }, modifier = Modifier.weight(1f))
                                MathKey("|x|", onClick = { insertIntoActive("abs(") }, modifier = Modifier.weight(1f))
                                MathKey("π", onClick = { insertIntoActive("pi") }, modifier = Modifier.weight(1f))
                                MathKey("e", onClick = { insertIntoActive("e") }, modifier = Modifier.weight(1f))
                            }

                            // Row 2: Trig & Log
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                MathKey("sin", onClick = { insertIntoActive("sin(") }, modifier = Modifier.weight(1f))
                                MathKey("cos", onClick = { insertIntoActive("cos(") }, modifier = Modifier.weight(1f))
                                MathKey("tan", onClick = { insertIntoActive("tan(") }, modifier = Modifier.weight(1f))
                                MathKey("ln", onClick = { insertIntoActive("log(") }, modifier = Modifier.weight(1f))
                                MathKey("(", onClick = { insertIntoActive("(") }, modifier = Modifier.weight(1f))
                                MathKey(")", onClick = { insertIntoActive(")") }, modifier = Modifier.weight(1f))
                                MathKey("⌫", onClick = { backspaceActive() }, isSpecial = true, modifier = Modifier.weight(1f))
                                MathKey("C", onClick = { clearActive() }, isSpecial = true, modifier = Modifier.weight(1f))
                            }

                            // Row 3: Digits 7,8,9 and Operators
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                MathKey("7", onClick = { insertIntoActive("7") }, isDigit = true, modifier = Modifier.weight(1f))
                                MathKey("8", onClick = { insertIntoActive("8") }, isDigit = true, modifier = Modifier.weight(1f))
                                MathKey("9", onClick = { insertIntoActive("9") }, isDigit = true, modifier = Modifier.weight(1f))
                                MathKey("/", onClick = { insertIntoActive(" / ") }, modifier = Modifier.weight(1f))
                                MathKey("*", onClick = { insertIntoActive("*") }, modifier = Modifier.weight(1f))
                                MathKey("-", onClick = { insertIntoActive("-") }, modifier = Modifier.weight(1f))
                                MathKey("+", onClick = { insertIntoActive("+") }, modifier = Modifier.weight(1f))
                            }

                            // Row 4: Digits 4,5,6, 1,2,3, 0
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                MathKey("4", onClick = { insertIntoActive("4") }, isDigit = true, modifier = Modifier.weight(1f))
                                MathKey("5", onClick = { insertIntoActive("5") }, isDigit = true, modifier = Modifier.weight(1f))
                                MathKey("6", onClick = { insertIntoActive("6") }, isDigit = true, modifier = Modifier.weight(1f))
                                MathKey("1", onClick = { insertIntoActive("1") }, isDigit = true, modifier = Modifier.weight(1f))
                                MathKey("2", onClick = { insertIntoActive("2") }, isDigit = true, modifier = Modifier.weight(1f))
                                MathKey("3", onClick = { insertIntoActive("3") }, isDigit = true, modifier = Modifier.weight(1f))
                                MathKey("0", onClick = { insertIntoActive("0") }, isDigit = true, modifier = Modifier.weight(1f))
                                MathKey(".", onClick = { insertIntoActive(".") }, modifier = Modifier.weight(1f))
                            }
                        }

                        // Ranges (X min, X max, Y min, Y max)
                        Text(
                            text = "Диапазон осей:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = xMinText,
                                onValueChange = { xMinText = it },
                                label = { Text("X min") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = xMaxText,
                                onValueChange = { xMaxText = it },
                                label = { Text("X max") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = yMinText,
                                onValueChange = { yMinText = it },
                                label = { Text("Y min") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = yMaxText,
                                onValueChange = { yMaxText = it },
                                label = { Text("Y max") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // RIGHT COLUMN: Live Interactive Desmos Preview
                    Column(
                        modifier = Modifier
                            .weight(1.15f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Интерактивный предпросмотр (Live Preview):",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "Обрезается строго по рамке",
                                fontSize = 11.sp,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.5.dp, Color(0xFFCBD5E1), RoundedCornerShape(14.dp))
                                .background(Color(0xFFFAFAFA))
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height

                                clipRect(left = 0f, top = 0f, right = w, bottom = h) {
                                    val xMin = parsedXMin.toDouble()
                                    val xMax = parsedXMax.toDouble()
                                    val yMin = parsedYMin.toDouble()
                                    val yMax = parsedYMax.toDouble()

                                    fun toScreenX(xVal: Double): Float {
                                        return (((xVal - xMin) / (xMax - xMin)) * w).toFloat()
                                    }

                                    fun toScreenY(yVal: Double): Float {
                                        return (((yMax - yVal) / (yMax - yMin)) * h).toFloat()
                                    }

                                    val originX = toScreenX(0.0).coerceIn(0f, w)
                                    val originY = toScreenY(0.0).coerceIn(0f, h)

                                    fun calculateNiceStep(span: Double, targetCount: Int = 8): Double {
                                        if (span <= 0.0) return 1.0
                                        val rawStep = span / targetCount.coerceAtLeast(1).toDouble()
                                        val magnitude = java.lang.Math.pow(10.0, kotlin.math.floor(kotlin.math.log10(rawStep)))
                                        val residual = rawStep / magnitude
                                        val niceResidual = when {
                                            residual < 1.5 -> 1.0
                                            residual < 3.5 -> 2.0
                                            residual < 7.5 -> 5.0
                                            else -> 10.0
                                        }
                                        return niceResidual * magnitude
                                    }

                                    fun formatCoord(value: Double): String {
                                        return if (kotlin.math.abs(value - kotlin.math.round(value)) < 1e-5) {
                                            value.roundToInt().toString()
                                        } else {
                                            String.format(java.util.Locale.US, "%.1f", value)
                                        }
                                    }

                                    val targetXCount = (w / 65f).toInt().coerceIn(4, 12)
                                    val targetYCount = (h / 55f).toInt().coerceIn(4, 10)
                                    val xStep = calculateNiceStep(xMax - xMin, targetXCount)
                                    val yStep = calculateNiceStep(yMax - yMin, targetYCount)

                                    val numStyle = TextStyle(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF64748B)
                                    )

                                    // X Grid & Numbers
                                    val startX = kotlin.math.ceil(xMin / xStep) * xStep
                                    var currentX = startX
                                    while (currentX <= xMax + 1e-6) {
                                        val sx = toScreenX(currentX)
                                        if (kotlin.math.abs(currentX) > 1e-5) {
                                            drawLine(
                                                color = Color(0xFFE2E8F0),
                                                start = Offset(sx, 0f),
                                                end = Offset(sx, h),
                                                strokeWidth = 1f
                                            )
                                            drawLine(
                                                color = Color(0xFF94A3B8),
                                                start = Offset(sx, originY - 3.5f),
                                                end = Offset(sx, originY + 3.5f),
                                                strokeWidth = 1.2f
                                            )
                                            val textRes = textMeasurer.measure(formatCoord(currentX), style = numStyle)
                                            val labelY = (originY + 5f).coerceIn(4f, h - textRes.size.height - 4f)
                                            val labelX = (sx - textRes.size.width / 2f).coerceIn(4f, w - textRes.size.width - 4f)
                                            drawText(textRes, topLeft = Offset(labelX, labelY))
                                        }
                                        currentX += xStep
                                    }

                                    // Y Grid & Numbers
                                    val startY = kotlin.math.ceil(yMin / yStep) * yStep
                                    var currentY = startY
                                    while (currentY <= yMax + 1e-6) {
                                        val sy = toScreenY(currentY)
                                        if (kotlin.math.abs(currentY) > 1e-5) {
                                            drawLine(
                                                color = Color(0xFFE2E8F0),
                                                start = Offset(0f, sy),
                                                end = Offset(w, sy),
                                                strokeWidth = 1f
                                            )
                                            drawLine(
                                                color = Color(0xFF94A3B8),
                                                start = Offset(originX - 3.5f, sy),
                                                end = Offset(originX + 3.5f, sy),
                                                strokeWidth = 1.2f
                                            )
                                            val textRes = textMeasurer.measure(formatCoord(currentY), style = numStyle)
                                            val labelX = (originX - textRes.size.width - 5f).coerceIn(4f, w - textRes.size.width - 4f)
                                            val labelY = (sy - textRes.size.height / 2f).coerceIn(4f, h - textRes.size.height - 4f)
                                            drawText(textRes, topLeft = Offset(labelX, labelY))
                                        }
                                        currentY += yStep
                                    }

                                    // Axes
                                    drawLine(
                                        color = Color(0xFF94A3B8),
                                        start = Offset(0f, originY),
                                        end = Offset(w, originY),
                                        strokeWidth = 1.5f
                                    )
                                    drawLine(
                                        color = Color(0xFF94A3B8),
                                        start = Offset(originX, 0f),
                                        end = Offset(originX, h),
                                        strokeWidth = 1.5f
                                    )

                                    // Zero
                                    val zeroRes = textMeasurer.measure("0", style = numStyle)
                                    val zeroX = (originX - zeroRes.size.width - 5f).coerceIn(4f, w - zeroRes.size.width - 4f)
                                    val zeroY = (originY + 5f).coerceIn(4f, h - zeroRes.size.height - 4f)
                                    drawText(zeroRes, topLeft = Offset(zeroX, zeroY))

                                    // Curves
                                    expressions.forEachIndexed { exprIdx, expr ->
                                        if (expr.isBlank()) return@forEachIndexed
                                        val curveColor = Color(paletteColors[exprIdx % paletteColors.size])
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
                                            style = Stroke(width = 2.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MathKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDigit: Boolean = false,
    isSpecial: Boolean = false
) {
    val bg = when {
        isSpecial -> Color(0xFFFEE2E2)
        isDigit -> Color.White
        else -> Color(0xFFE0E7FF)
    }
    val textColor = when {
        isSpecial -> Color(0xFFDC2626)
        isDigit -> Color(0xFF0F172A)
        else -> Color(0xFF1D4ED8)
    }

    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
