package com.lkboard.app.ui.components

import androidx.compose.foundation.BorderStroke
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
import com.lkboard.app.ui.theme.LocalAppTheme

/**
 * Диалог физического и инженерного конструктора:
 * Позволяет в 1 клик выбрать готовый физический примитив для вставки на холст.
 */
@Composable
fun PhysicsStencilDialog(
    onDismiss: () -> Unit,
    onSelectStencil: (type: PhysicsStencilType, label: String) -> Unit
) {
    val isDark = LocalAppTheme.current.isDark
    var selectedType by remember { mutableStateOf(PhysicsStencilType.INCLINED_PLANE) }
    var selectedCategory by remember { mutableStateOf("Механика") }
    var customLabel by remember { mutableStateOf("") }

    val categories = listOf("Механика", "Оптика", "Электричество")
    val stencilsInCategory = PhysicsStencilType.entries.filter { it.category == selectedCategory }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.width(540.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF181B20) else Color.White
            ),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF2E333D) else Color(0xFFE2E8F0))
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
                        color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
                    )
                    Text(
                        text = "Stencils 1-Click",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
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
                                .background(
                                    if (isSelected) (if (isDark) Color(0xFF1E2638) else Color(0xFFEFF6FF))
                                    else (if (isDark) Color(0xFF242830) else Color(0xFFF8FAFC))
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF2563EB) else (if (isDark) Color(0xFF3F4554) else Color(0xFFE2E8F0)),
                                    RoundedCornerShape(8.dp)
                                )
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
                                color = if (isSelected) (if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB)) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
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
                                .background(
                                    if (isCurrent) (if (isDark) Color(0xFF1E2638) else Color(0xFFEFF6FF))
                                    else (if (isDark) Color(0xFF242830) else Color(0xFFF8FAFC))
                                )
                                .border(
                                    1.dp,
                                    if (isCurrent) Color(0xFF2563EB) else (if (isDark) Color(0xFF3F4554) else Color(0xFFE2E8F0)),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedType = stencil }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stencil.displayName,
                                fontSize = 13.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrent) (if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)) else (if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B))
                            )
                            if (isCurrent) {
                                Text(
                                    text = "✓ ВЫБРАН",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
                                )
                            }
                        }
                    }
                }

                // Подпись / номинал
                OutlinedTextField(
                    value = customLabel,
                    onValueChange = { customLabel = it },
                    label = { Text("Параметр / подпись (необязательно, например R = 20 Ом)") },
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
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        ),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF3F4554) else Color(0xFFCBD5E1))
                    ) {
                        Text("Отмена", fontSize = 12.sp)
                    }

                    Spacer(Modifier.width(10.dp))

                    Button(
                        onClick = { onSelectStencil(selectedType, customLabel.trim()) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2563EB),
                            contentColor = Color.White
                        )
                    ) {
                        Text("✓ Добавить примитив", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
