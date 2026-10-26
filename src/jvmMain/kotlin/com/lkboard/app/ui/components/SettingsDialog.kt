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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedButton
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
import com.lkboard.app.data.AppTheme
import com.lkboard.app.data.UserRole
import com.lkboard.app.model.CanvasTool

/**
 * Диалог настроек LKBoard:
 * - Выбор темы приложения (Тёмная, Светлая, Тетрадь в клетку).
 * - Выбор быстрого профиля роли (Репетитор, Команда, Скетчи, Все инструменты).
 * - Выборочное включение / отключение инструментов на панели.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsDialog(
    currentTheme: AppTheme,
    currentRole: UserRole,
    enabledTools: Set<CanvasTool>,
    onDismiss: () -> Unit,
    onSaveSettings: (newTheme: AppTheme, newRole: UserRole, newEnabledTools: Set<CanvasTool>) -> Unit
) {
    var theme by remember { mutableStateOf(currentTheme) }
    var role by remember { mutableStateOf(currentRole) }
    var tools by remember { mutableStateOf(enabledTools) }
    val isDark = theme.isDark

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.width(560.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF181B20) else Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF2E333D) else Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Заголовок
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Настройки приложения",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
                    )
                    Text(
                        text = "LKBoard v1.0",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)
                    )
                }

                // 1. Тема приложения
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "ТЕМА ОФОРМЛЕНИЯ ХОЛСТА И ИНТЕРФЕЙСА:",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppTheme.entries.forEach { appTheme ->
                            val isSelected = appTheme == theme
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) (if (isDark) Color(0xFF1E2638) else Color(0xFFEFF6FF))
                                        else (if (isDark) Color(0xFF242830) else Color(0xFFF8FAFC))
                                    )
                                    .border(
                                        1.5.dp,
                                        if (isSelected) (if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB))
                                        else (if (isDark) Color(0xFF333842) else Color(0xFFE2E8F0)),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { theme = appTheme }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = appTheme.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) (if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)) else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569))
                                )
                            }
                        }
                    }
                }

                // 2. Быстрый профиль инструментов
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "БЫСТРЫЙ ПРОФИЛЬ ДЛЯ ВАШИХ ЗАДАЧ:",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        UserRole.entries.forEach { r ->
                            val isSelected = r == role
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
                                        if (isSelected) (if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB))
                                        else (if (isDark) Color(0xFF333842) else Color(0xFFE2E8F0)),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        role = r
                                        tools = r.tools
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = r.icon, fontSize = 16.sp)
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = when(r) {
                                            UserRole.TUTOR_STEM -> "Репетитор"
                                            UserRole.TEAM_COLLAB -> "Команда"
                                            UserRole.SKETCH_NOTE -> "Скетчи"
                                            UserRole.ALL_TOOLS -> "Все"
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) (if (isDark) Color(0xFF38BDF8) else Color(0xFF1D4ED8)) else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569))
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Выборочная настройка видимости инструментов
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "ВИДИМОСТЬ ИНСТРУМЕНТОВ НА ПАНЕЛИ:",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CanvasTool.entries.filter { it != CanvasTool.SELECT && it != CanvasTool.HAND }.forEach { tool ->
                            val isEnabled = tool in tools
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isDark) Color(0xFF242830) else Color(0xFFF8FAFC))
                                    .border(1.dp, if (isDark) Color(0xFF333842) else Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                                    .clickable {
                                        tools = if (isEnabled) tools - tool else tools + tool
                                    }
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isEnabled,
                                    onCheckedChange = { checked ->
                                        tools = if (checked) tools + tool else tools - tool
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB),
                                        checkmarkColor = if (isDark) Color(0xFF0F1115) else Color.White,
                                        uncheckedColor = if (isDark) Color(0xFF52525B) else Color(0xFF94A3B8)
                                    )
                                )
                                Text(
                                    text = tool.displayName.substringBefore(" ("),
                                    fontSize = 11.sp,
                                    color = if (isEnabled) (if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)) else (if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8))
                                )
                            }
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
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF3F3F46) else Color(0xFFCBD5E1))
                    ) {
                        Text("Отмена", fontSize = 12.sp)
                    }

                    Spacer(Modifier.width(10.dp))

                    Button(
                        onClick = {
                            onSaveSettings(theme, role, tools)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2563EB),
                            contentColor = Color.White
                        )
                    ) {
                        Text("✓ Применить настройки", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
