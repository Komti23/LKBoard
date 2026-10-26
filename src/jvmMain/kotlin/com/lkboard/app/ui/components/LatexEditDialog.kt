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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lkboard.app.ui.theme.LocalAppTheme
import com.lkboard.app.util.LatexRenderer

/**
 * Боковая панель создания и редактирования математических формул LaTeX (Side Drawer):
 * - Плавно выезжает справа, не перекрывая центральную часть холста.
 * - Интерактивный предпросмотр формулы "на лету".
 * - Быстрые математические символы (интегралы, дроби, корни, степени, греческие буквы).
 * - Готовые пресеты школьной и университетской физики/математики.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LatexSidePanel(
    isOpen: Boolean,
    initialFormula: String = "\\int_{0}^{\\infty} e^{-x^2} dx = \\frac{\\sqrt{\\pi}}{2}",
    initialFontSize: Float = 24f,
    onDismiss: () -> Unit,
    onSave: (formula: String, fontSize: Float) -> Unit
) {
    val isDark = LocalAppTheme.current.isDark
    var formula by remember(initialFormula, isOpen) { mutableStateOf(initialFormula) }
    var fontSize by remember(initialFontSize, isOpen) { mutableStateOf(initialFontSize) }

    val quickSnippets = listOf(
        "Дробь" to "\\frac{a}{b}",
        "Корень" to "\\sqrt{x}",
        "Степень" to "x^{2}",
        "Индекс" to "x_{1}",
        "Интеграл" to "\\int_{a}^{b} f(x) dx",
        "Сумма" to "\\sum_{i=1}^{n} x_i",
        "±" to "\\pm",
        "·" to "\\cdot",
        "π" to "\\pi",
        "α" to "\\alpha",
        "β" to "\\beta",
        "Δ" to "\\Delta",
        "→" to "\\to",
        "∞" to "\\infty"
    )

    val presets = listOf(
        "Квадратное уравнение" to "x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}",
        "Второй закон Ньютона" to "\\vec{F} = m \\cdot \\vec{a}",
        "Закон Ома" to "I = \\frac{U}{R}",
        "Энергия Эйнштейна" to "E = m c^2",
        "Длина волны де Бройля" to "\\lambda = \\frac{h}{p}"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Затемняющий оверлей (Scrim)
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
                    .width(480.dp)
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
                                text = "Формула LaTeX",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
                            )
                            Text(
                                text = "∑ Math Engine",
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

                    // Скроллируемое тело
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Превью
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDark) Color(0xFF0F1115) else Color(0xFFF8FAFC))
                                .border(1.dp, if (isDark) Color(0xFF2E333D) else Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "ПРЕДПРОСМОТР:",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = LatexRenderer.formatLatexToReadable(formula).replace("{", "").replace("}", ""),
                                fontSize = (fontSize * 0.9f).coerceIn(14f, 44f).sp,
                                fontFamily = FontFamily.Serif,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
                            )
                        }

                        // Выбор размера формулы
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "РАЗМЕР:",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )

                            listOf(18f to "Мелкий", 24f to "Стандарт", 32f to "Крупный", 48f to "Огромный").forEach { (sz, label) ->
                                val isSelected = fontSize.toInt() == sz.toInt()
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) Color(0xFF2563EB) else (if (isDark) Color(0xFF242830) else Color(0xFFF1F5F9)))
                                        .border(1.dp, if (isSelected) Color(0xFF2563EB) else (if (isDark) Color(0xFF3F4554) else Color(0xFFCBD5E1)), RoundedCornerShape(6.dp))
                                        .clickable { fontSize = sz }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF1E293B))
                                    )
                                }
                            }
                        }

                        // Ввод формулы
                        OutlinedTextField(
                            value = formula,
                            onValueChange = { formula = it },
                            label = { Text("LaTeX выражение") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A),
                                unfocusedTextColor = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A),
                                focusedBorderColor = Color(0xFF2563EB),
                                unfocusedBorderColor = if (isDark) Color(0xFF3F4554) else Color(0xFFCBD5E1),
                                focusedContainerColor = if (isDark) Color(0xFF181C24) else Color.White,
                                unfocusedContainerColor = if (isDark) Color(0xFF181C24) else Color.White,
                                cursorColor = Color(0xFF2563EB),
                                focusedLabelColor = Color(0xFF3B82F6),
                                unfocusedLabelColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            )
                        )

                        // Быстрые символы и макросы
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "БЫСТРЫЕ ВСТАВКИ:",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                quickSnippets.forEach { (label, code) ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isDark) Color(0xFF242830) else Color(0xFFF1F5F9))
                                            .border(1.dp, if (isDark) Color(0xFF3F4554) else Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                                            .clickable { formula = if (formula.isBlank()) code else "$formula $code" }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
                                        )
                                    }
                                }
                            }
                        }

                        // Готовые пресеты
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "ГОТОВЫЕ ФОРМУЛЫ:",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                presets.forEach { (title, code) ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF))
                                            .border(1.dp, if (isDark) Color(0xFF2563EB).copy(alpha = 0.5f) else Color(0xFFBFDBFE), RoundedCornerShape(6.dp))
                                            .clickable { formula = code }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = title,
                                            fontSize = 11.sp,
                                            color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
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
                                if (formula.isNotBlank()) {
                                    onSave(formula.trim(), fontSize)
                                    onDismiss()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB),
                                contentColor = Color.White
                            )
                        ) {
                            Text("✓ Применить формулу", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Обратная совместимость для вызова как Dialog.
 */
@Composable
fun LatexEditDialog(
    initialFormula: String = "\\int_{0}^{\\infty} e^{-x^2} dx = \\frac{\\sqrt{\\pi}}{2}",
    initialFontSize: Float = 24f,
    onDismiss: () -> Unit,
    onSave: (formula: String, fontSize: Float) -> Unit
) {
    LatexSidePanel(
        isOpen = true,
        initialFormula = initialFormula,
        initialFontSize = initialFontSize,
        onDismiss = onDismiss,
        onSave = onSave
    )
}
