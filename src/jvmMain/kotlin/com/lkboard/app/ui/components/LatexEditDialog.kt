package com.lkboard.app.ui.components

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
import com.lkboard.app.util.LatexRenderer

/**
 * Диалог создания и быстрого редактирования математических формул LaTeX "на лету".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LatexEditDialog(
    initialFormula: String = "\\int_{0}^{\\infty} e^{-x^2} dx = \\frac{\\sqrt{\\pi}}{2}",
    onDismiss: () -> Unit,
    onSave: (formula: String) -> Unit
) {
    var formula by remember { mutableStateOf(initialFormula) }

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
                containerColor = Color(0xFF13161A)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF242830))
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
                        color = Color(0xFFF4F4F5)
                    )
                    Text(
                        text = "∑ Math Engine",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF38BDF8)
                    )
                }

                // Превью
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0D0F12))
                        .border(1.dp, Color(0xFF242830), RoundedCornerShape(8.dp))
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ПРЕДПРОСМОТР:",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF71717A)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = LatexRenderer.formatLatexToReadable(formula).replace("{", "").replace("}", ""),
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF38BDF8)
                    )
                }

                // Ввод формулы
                OutlinedTextField(
                    value = formula,
                    onValueChange = { formula = it },
                    label = { Text("LaTeX выражение", color = Color(0xFFA1A1AA)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFF4F4F5),
                        unfocusedTextColor = Color(0xFFF4F4F5),
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF242830),
                        focusedContainerColor = Color(0xFF181B20),
                        unfocusedContainerColor = Color(0xFF181B20)
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
                    color = Color(0xFFA1A1AA)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickSnippets.forEach { (label, code) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF181B20))
                                .border(1.dp, Color(0xFF242830), RoundedCornerShape(6.dp))
                                .clickable { formula = if (formula.isBlank()) code else "$formula $code" }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFF4F4F5)
                            )
                        }
                    }
                }

                // Готовые пресеты
                Text(
                    text = "ГОТОВЫЕ ФОРМУЛЫ:",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFA1A1AA)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { (title, code) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1F2329))
                                .border(1.dp, Color(0xFF363C48), RoundedCornerShape(6.dp))
                                .clickable { formula = code }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                color = Color(0xFF38BDF8)
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
                            contentColor = Color(0xFFA1A1AA)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF242830))
                    ) {
                        Text("Отмена", fontSize = 12.sp)
                    }

                    Spacer(Modifier.width(10.dp))

                    Button(
                        onClick = {
                            if (formula.isNotBlank()) {
                                onSave(formula.trim())
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF38BDF8),
                            contentColor = Color(0xFF0D0F12)
                        )
                    ) {
                        Text("Добавить на доску", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
