package com.lkboard.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lkboard.app.model.BoardElement
import com.lkboard.app.ui.theme.LocalAppTheme
import com.lkboard.app.util.MathEvaluator
import kotlin.math.roundToInt

/**
 * Боковая панель математической студии графиков Desmos (Side Drawer):
 * - Плавно выезжает справа, оставляя холст открытым.
 * - Интерактивный предпросмотр графика f(x) с осями и сеткой.
 * - Настройка диапазона X/Y и виртуальная математическая клавиатура.
 */
@Composable
fun FunctionGraphSidePanel(
    isOpen: Boolean,
    element: BoardElement.FunctionGraph?,
    onSave: (BoardElement.FunctionGraph) -> Unit,
    onDismiss: () -> Unit
) {
    var lastElement by remember { mutableStateOf<BoardElement.FunctionGraph?>(null) }
    if (element != null) {
        lastElement = element
    }
    val activeElement = element ?: lastElement
    if (activeElement == null) return

    var title by remember(activeElement.title, isOpen) { mutableStateOf(activeElement.title) }

    val initialExpressions = if (activeElement.expressions.isEmpty()) listOf("sin(x)") else activeElement.expressions
    val expressions = remember(activeElement.id, isOpen) { mutableStateListOf(*initialExpressions.toTypedArray()) }
    var activeExpressionIndex by remember { mutableStateOf(0) }

    var xMinText by remember(activeElement.xMin, isOpen) { mutableStateOf(activeElement.xMin.toString()) }
    var xMaxText by remember(activeElement.xMax, isOpen) { mutableStateOf(activeElement.xMax.toString()) }
    var yMinText by remember(activeElement.yMin, isOpen) { mutableStateOf(activeElement.yMin.toString()) }
    var yMaxText by remember(activeElement.yMax, isOpen) { mutableStateOf(activeElement.yMax.toString()) }

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

    val isDark = LocalAppTheme.current.isDark

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A),
        unfocusedTextColor = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A),
        focusedContainerColor = if (isDark) Color(0xFF1E2430) else Color.White,
        unfocusedContainerColor = if (isDark) Color(0xFF1E2430) else Color.White,
        cursorColor = Color(0xFF2563EB),
        focusedBorderColor = Color(0xFF2563EB),
        unfocusedBorderColor = if (isDark) Color(0xFF3F4554) else Color(0xFFCBD5E1),
        focusedLabelColor = Color(0xFF3B82F6),
        unfocusedLabelColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Затемняющий оверлей
        AnimatedVisibility(
            visible = isOpen,
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(180))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.40f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }

        // Правая боковая панель с плавной анимацией выезда
        AnimatedVisibility(
            visible = isOpen,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
            ) + fadeIn(),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
            ) + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(520.dp)
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp),
                        ambientColor = Color.Black.copy(alpha = 0.12f),
                        spotColor = Color.Black.copy(alpha = 0.28f)
                    )
                    .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
                    .background(if (isDark) Color(0xFF14171D) else Color.White)
                    .border(
                        1.dp,
                        if (isDark) Color(0xFF2E333D) else Color(0xFFE2E8F0),
                        RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Шапка
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isDark) Color(0xFF181B22) else Color(0xFFF8FAFC))
                            .border(1.dp, if (isDark) Color(0xFF232730) else Color(0xFFE2E8F0))
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "📈 График функций f(x)",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
                            )
                            Text(
                                text = "Интерактивная математическая студия",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDark) Color(0xFF282D37) else Color(0xFFE2E8F0))
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Закрыть",
                                tint = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Скроллируемое тело студии
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Поле названия графика
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Название графика") },
                            singleLine = true,
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Интерактивный Canvas-предпросмотр графика
                        GraphPreviewBox(
                            expressions = expressions,
                            xMin = parsedXMin,
                            xMax = parsedXMax,
                            yMin = parsedYMin,
                            yMax = parsedYMax,
                            paletteColors = paletteColors,
                            isDark = isDark,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )

                        // Список выражений функций f(x)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "МАТЕМАТИЧЕСКИЕ ВЫРАЖЕНИЯ:",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                                if (expressions.size < 5) {
                                    OutlinedButton(
                                        onClick = {
                                            expressions.add("cos(x)")
                                            activeExpressionIndex = expressions.size - 1
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(26.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("+ Функция", fontSize = 10.sp)
                                    }
                                }
                            }

                            expressions.forEachIndexed { idx, expr ->
                                val isSelected = idx == activeExpressionIndex
                                val colorVal = paletteColors[idx % paletteColors.size]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) (if (isDark) Color(0xFF192233) else Color(0xFFEFF6FF)) else (if (isDark) Color(0xFF14171E) else Color(0xFFF8FAFC)))
                                        .border(1.dp, if (isSelected) Color(0xFF2563EB) else (if (isDark) Color(0xFF2A3142) else Color(0xFFE2E8F0)), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(colorVal))
                                    )
                                    Text(
                                        text = "f${idx + 1}(x) =",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(colorVal)
                                    )
                                    OutlinedTextField(
                                        value = expr,
                                        onValueChange = { expressions[idx] = it },
                                        singleLine = true,
                                        colors = fieldColors,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { activeExpressionIndex = idx }
                                    )
                                    if (expressions.size > 1) {
                                        IconButton(
                                            onClick = {
                                                expressions.removeAt(idx)
                                                if (activeExpressionIndex >= expressions.size) {
                                                    activeExpressionIndex = (expressions.size - 1).coerceAtLeast(0)
                                                }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Удалить", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // Диапазон осей X и Y
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "ДИАПАЗОН ОСЕЙ СЕТКИ:",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
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
                                    colors = fieldColors,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = xMaxText,
                                    onValueChange = { xMaxText = it },
                                    label = { Text("X max") },
                                    singleLine = true,
                                    colors = fieldColors,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = yMinText,
                                    onValueChange = { yMinText = it },
                                    label = { Text("Y min") },
                                    singleLine = true,
                                    colors = fieldColors,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = yMaxText,
                                    onValueChange = { yMaxText = it },
                                    label = { Text("Y max") },
                                    singleLine = true,
                                    colors = fieldColors,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Математическая экранная клавиатура
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "МАТЕМАТИЧЕСКАЯ КЛАВИАТУРА:",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )

                            val keyRows = listOf(
                                listOf("sin", "cos", "tan", "sqrt", "^", "(" , ")"),
                                listOf("x", "7", "8", "9", "/", "abs", "pi"),
                                listOf("e", "4", "5", "6", "*", "ln", "log"),
                                listOf("±", "1", "2", "3", "-", "⌫", "C"),
                                listOf("0", ".", "+", "x²", "1/x")
                            )

                            keyRows.forEach { rowKeys ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    rowKeys.forEach { k ->
                                        MathKey(
                                            label = k,
                                            isDigit = k in "0".."9" || k == ".",
                                            isSpecial = k in listOf("⌫", "C"),
                                            isDark = isDark,
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                when (k) {
                                                    "⌫" -> backspaceActive()
                                                    "C" -> clearActive()
                                                    "x²" -> insertIntoActive("^2")
                                                    "1/x" -> insertIntoActive("1/")
                                                    "sqrt" -> insertIntoActive("sqrt(")
                                                    "sin" -> insertIntoActive("sin(")
                                                    "cos" -> insertIntoActive("cos(")
                                                    "tan" -> insertIntoActive("tan(")
                                                    "abs" -> insertIntoActive("abs(")
                                                    "ln" -> insertIntoActive("ln(")
                                                    "log" -> insertIntoActive("log(")
                                                    "pi" -> insertIntoActive("pi")
                                                    "±" -> insertIntoActive("-")
                                                    else -> insertIntoActive(k)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Нижняя панель действий
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isDark) Color(0xFF181B22) else Color(0xFFF8FAFC))
                            .border(1.dp, if (isDark) Color(0xFF232730) else Color(0xFFE2E8F0))
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            ),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF3F4554) else Color(0xFFCBD5E1))
                        ) {
                            Text("Отмена", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val validExprs = expressions.map { it.trim() }.filter { it.isNotEmpty() }
                                val finalExprs = if (validExprs.isEmpty()) listOf("x^2") else validExprs
                                val updated = activeElement.copy(
                                    title = title.ifBlank { "График функций" },
                                    expressions = finalExprs,
                                    xMin = parsedXMin,
                                    xMax = parsedXMax,
                                    yMin = parsedYMin,
                                    yMax = parsedYMax,
                                    strokeColorsHex = paletteColors.take(finalExprs.size)
                                )
                                onSave(updated)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB),
                                contentColor = Color.White
                            )
                        ) {
                            Text("✓ Применить график на доску", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GraphPreviewBox(
    expressions: List<String>,
    xMin: Float,
    xMax: Float,
    yMin: Float,
    yMax: Float,
    paletteColors: List<Long>,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) Color(0xFF0F1115) else Color.White)
            .border(1.dp, if (isDark) Color(0xFF2A3142) else Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            clipRect(0f, 0f, w, h) {
                val spanX = (xMax - xMin).coerceAtLeast(0.001f).toDouble()
                val spanY = (yMax - yMin).coerceAtLeast(0.001f).toDouble()

                fun toScreenX(vx: Double): Float = ((vx - xMin) / spanX * w).toFloat()
                fun toScreenY(vy: Double): Float = (h - (vy - yMin) / spanY * h).toFloat()

                val originX = toScreenX(0.0).coerceIn(0f, w)
                val originY = toScreenY(0.0).coerceIn(0f, h)

                // Сетка и оси
                val numStyle = TextStyle(
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
                )

                // Оси
                drawLine(
                    color = if (isDark) Color(0xFF52525B) else Color(0xFF94A3B8),
                    start = Offset(0f, originY),
                    end = Offset(w, originY),
                    strokeWidth = 1.2f
                )
                drawLine(
                    color = if (isDark) Color(0xFF52525B) else Color(0xFF94A3B8),
                    start = Offset(originX, 0f),
                    end = Offset(originX, h),
                    strokeWidth = 1.2f
                )

                // Кривые функций
                expressions.forEachIndexed { exprIdx, expr ->
                    if (expr.isBlank()) return@forEachIndexed
                    val curveColor = Color(paletteColors[exprIdx % paletteColors.size])
                    val samples = MathEvaluator.sampleFunction(expr, xMin.toDouble(), xMax.toDouble(), steps = 250)

                    val curvePath = Path()
                    var inPath = false

                    for ((sxVal, syVal) in samples) {
                        if (syVal == null || syVal.isNaN() || syVal.isInfinite() || syVal < yMin - 50 || syVal > yMax + 50) {
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
}

@Composable
private fun MathKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDigit: Boolean = false,
    isSpecial: Boolean = false,
    isDark: Boolean = false
) {
    val bg = when {
        isSpecial -> if (isDark) Color(0xFF450A0A) else Color(0xFFFEE2E2)
        isDigit -> if (isDark) Color(0xFF1E222A) else Color.White
        else -> if (isDark) Color(0xFF1E293B) else Color(0xFFE0E7FF)
    }
    val textColor = when {
        isSpecial -> if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
        isDigit -> if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
        else -> if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
    }

    Box(
        modifier = modifier
            .height(30.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, if (isDark) Color(0xFF333D4F) else Color(0xFFCBD5E1), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

/**
 * Обратная совместимость для вызова FunctionGraphEditDialog.
 */
@Composable
fun FunctionGraphEditDialog(
    element: BoardElement.FunctionGraph,
    onSave: (BoardElement.FunctionGraph) -> Unit,
    onDismiss: () -> Unit
) {
    FunctionGraphSidePanel(
        isOpen = true,
        element = element,
        onSave = onSave,
        onDismiss = onDismiss
    )
}
