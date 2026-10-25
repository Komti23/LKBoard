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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lkboard.app.model.PhysicsStencilType

/**
 * Диалог физического и инженерного конструктора:
 * Позволяет в 1 клик выбрать готовый физический примитив для вставки на холст.
 */
@Composable
fun PhysicsStencilDialog(
    onDismiss: () -> Unit,
    onSelectStencil: (type: PhysicsStencilType, label: String) -> Unit
) {
    var selectedType by remember { mutableStateOf(PhysicsStencilType.INCLINED_PLANE) }
    var selectedCategory by remember { mutableStateOf("Механика") }
    var customLabel by remember { mutableStateOf("") }

    val categories = listOf("Механика", "Оптика", "Электричество")
    val stencilsInCategory = PhysicsStencilType.entries.filter { it.category == selectedCategory }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.width(540.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF13161A)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF242830))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Заголовок
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Физический конструктор",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF4F4F5)
                    )
                    Text(
                        text = "Stencils 1-Click",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF38BDF8)
                    )
                }

                // Вкладки категорий
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = cat == selectedCategory
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF1F2329) else Color(0xFF181B20))
                                .border(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF242830), RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedCategory = cat
                                    selectedType = PhysicsStencilType.entries.first { it.category == cat }
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF38BDF8) else Color(0xFFA1A1AA)
                            )
                        }
                    }
                }

                // Сетка стенсилов в выбранной категории
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    stencilsInCategory.forEach { stencil ->
                        val isCurrent = stencil == selectedType
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCurrent) Color(0x2238BDF8) else Color(0xFF181B20))
                                .border(1.dp, if (isCurrent) Color(0xFF38BDF8) else Color(0xFF242830), RoundedCornerShape(8.dp))
                                .clickable { selectedType = stencil }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stencil.displayName,
                                fontSize = 13.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrent) Color(0xFF38BDF8) else Color(0xFFF4F4F5)
                            )
                            if (isCurrent) {
                                Text(
                                    text = "✓ ВЫБРАН",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }
                    }
                }

                // Подпись / номинал
                OutlinedTextField(
                    value = customLabel,
                    onValueChange = { customLabel = it },
                    label = { Text("Параметр / подпись (необязательно, например R = 20 Ом)", color = Color(0xFFA1A1AA)) },
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
                    singleLine = true
                )

                // Кнопки
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA1A1AA)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF242830))
                    ) {
                        Text("Отмена", fontSize = 12.sp)
                    }

                    Spacer(Modifier.width(10.dp))

                    Button(
                        onClick = { onSelectStencil(selectedType, customLabel.trim()) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF38BDF8),
                            contentColor = Color(0xFF0D0F12)
                        )
                    ) {
                        Text("Добавить примитив", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
