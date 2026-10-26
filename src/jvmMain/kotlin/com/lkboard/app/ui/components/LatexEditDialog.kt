package com.lkboard.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lkboard.app.ui.theme.LocalAppTheme
import com.lkboard.app.util.LatexRenderer

/**
 * Диалог создания и быстрого редактирования математических формул LaTeX "на лету".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LatexEditDialog(
    initialFormula: String = "\\int_{0}^{\\infty} e^{-x^2} dx = \\frac{\\sqrt{\\pi}}{2}",
    initialFontSize: Float = 24f,
    onDismiss: () -> Unit,
    onSave: (formula: String, fontSize: Float) -> Unit
) {
    val isDark = LocalAppTheme.current.isDark
    var formula by remember { mutableStateOf(initialFormula) }
    var fontSize by remember { mutableStateOf(initialFontSize) }

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

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.width(520.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF181B20) else Color.White
            ),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF2E333D) else Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Заголовок
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Формула LaTeX «на лету»",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
                    )
                    Text(
                        text = "∑ Math Engine",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
                    )
                }

                // Превью
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0xFF0F1115) else Color(0xFFF8FAFC))
                        .border(1.dp, if (isDark) Color(0xFF2E333D) else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ПРЕДПРОСМОТР:",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                    Spacer(Modifier.height(6.dp))
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
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )

                    // A-
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isDark) Color(0xFF242830) else Color(0xFFF1F5F9))
                            .border(1.dp, if (isDark) Color(0xFF3F4554) else Color(0xFFCBD5E1), RoundedCornerShape(6.dp))
                            .clickable { fontSize = (fontSize - 4f).coerceAtLeast(14f) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("A-", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B))
                    }

                    // A+
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isDark) Color(0xFF242830) else Color(0xFFF1F5F9))
                            .border(1.dp, if (isDark) Color(0xFF3F4554) else Color(0xFFCBD5E1), RoundedCornerShape(6.dp))
                            .clickable { fontSize = (fontSize + 4f).coerceAtMost(96f) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("A+", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B))
                    }

                    listOf(18f to "Мелкий", 24f to "Стандарт", 32f to "Крупный", 48f to "Огромный").forEach { (sz, label) ->
                        val isSelected = fontSize.toInt() == sz.toInt()
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) Color(0xFF2563EB) else (if (isDark) Color(0xFF242830) else Color(0xFFF1F5F9)))
                                .border(1.dp, if (isSelected) Color(0xFF2563EB) else (if (isDark) Color(0xFF3F4554) else Color(0xFFCBD5E1)), RoundedCornerShape(6.dp))
                                .clickable { fontSize = sz }
                                .padding(horizontal = 7.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${sz.toInt()} $label",
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
                        focusedContainerColor = if (isDark) Color(0xFF242830) else Color.White,
                        unfocusedContainerColor = if (isDark) Color(0xFF242830) else Color.White,
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
                Text(
                    text = "БЫСТРЫЕ ВСТАВКИ:",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
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

                // Готовые пресеты
                Text(
                    text = "ГОТОВЫЕ ФОРМУЛЫ:",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
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

                // Кнопки действий
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
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

                    Spacer(Modifier.width(10.dp))

                    Button(
                        onClick = {
                            if (formula.isNotBlank()) {
                                onSave(formula.trim(), fontSize)
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2563EB),
                            contentColor = Color.White
                        )
                    ) {
                        Text("✓ Добавить на доску", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
