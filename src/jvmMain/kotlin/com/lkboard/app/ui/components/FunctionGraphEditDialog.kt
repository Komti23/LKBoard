package com.lkboard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lkboard.app.model.BoardElement

/**
 * Диалог настройки математического графика функций (Desmos-стиль).
 */
@Composable
fun FunctionGraphEditDialog(
    element: BoardElement.FunctionGraph,
    onSave: (BoardElement.FunctionGraph) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(element.title) }
    var expressionsText by remember { mutableStateOf(element.expressions.joinToString("\n")) }
    var xMinText by remember { mutableStateOf(element.xMin.toString()) }
    var xMaxText by remember { mutableStateOf(element.xMax.toString()) }
    var yMinText by remember { mutableStateOf(element.yMin.toString()) }
    var yMaxText by remember { mutableStateOf(element.yMax.toString()) }

    val presetFormulas = listOf("sin(x)", "cos(x)", "x^2 - 4", "sqrt(abs(x))", "0.5*x^3 - x", "1 / (x^2 + 1)")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            shadowElevation = 16.dp,
            modifier = Modifier.width(480.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "📈 График функций (Desmos-стиль)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Заголовок графика") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = expressionsText,
                    onValueChange = { expressionsText = it },
                    label = { Text("Функции f(x) (по одной на строку)") },
                    placeholder = { Text("sin(x)\n0.5*x^2 - 2") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Быстрые шаблоны функций:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetFormulas.take(4).forEach { formula ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF1F5F9))
                                .clickable {
                                    val current = expressionsText.trim()
                                    expressionsText = if (current.isEmpty()) formula else "$current\n$formula"
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = formula,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF2563EB)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
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

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Отмена", color = Color(0xFF64748B))
                    }

                    Button(
                        onClick = {
                            val exprList = expressionsText.lines()
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                            val newXMin = xMinText.toFloatOrNull() ?: -10f
                            val newXMax = xMaxText.toFloatOrNull() ?: 10f
                            val newYMin = yMinText.toFloatOrNull() ?: -6f
                            val newYMax = yMaxText.toFloatOrNull() ?: 6f
                            val updated = element.copy(
                                title = title.ifBlank { "График функции" },
                                expressions = if (exprList.isNotEmpty()) exprList else listOf("sin(x)"),
                                xMin = kotlin.math.min(newXMin, newXMax - 0.5f),
                                xMax = kotlin.math.max(newXMax, newXMin + 0.5f),
                                yMin = kotlin.math.min(newYMin, newYMax - 0.5f),
                                yMax = kotlin.math.max(newYMax, newYMin + 0.5f)
                            )
                            onSave(updated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Построить", color = Color.White)
                    }
                }
            }
        }
    }
}
