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
import com.lkboard.app.data.UserRole
import com.lkboard.app.ui.theme.LocalAppTheme

/**
 * Диалог первого запуска: спрашивает пользователя о сценарии работы
 * и настраивает набор инструментов без визуального мусора.
 */
@Composable
fun FirstRunOnboardingDialog(
    initialRole: UserRole = UserRole.TUTOR_STEM,
    onComplete: (selectedRole: UserRole) -> Unit
) {
    val isDark = LocalAppTheme.current.isDark
    var selectedRole by remember { mutableStateOf(initialRole) }

    Dialog(onDismissRequest = { /* Обязательный выбор роли при первом старте */ }) {
        Card(
            modifier = Modifier.width(580.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF181B20) else Color.White
            ),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF2E333D) else Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier.padding(26.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Заголовок
                Column {
                    Text(
                        text = "Добро пожаловать в LKBoard!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Выберите, для чего вы будете использовать доску. Мы оставим только нужные инструменты, чтобы не перегружать экран лишним мусором:",
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        lineHeight = 17.sp
                    )
                }

                // 4 карточки сценариев
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    UserRole.entries.forEach { role ->
                        val isSelected = role == selectedRole
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) (if (isDark) Color(0xFF1E2638) else Color(0xFFEFF6FF))
                                    else (if (isDark) Color(0xFF242830) else Color(0xFFF8FAFC))
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF2563EB) else (if (isDark) Color(0xFF3F4554) else Color(0xFFE2E8F0)),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedRole = role }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = role.icon,
                                fontSize = 24.sp,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = role.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) (if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)) else (if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A))
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = role.subtitle,
                                    fontSize = 11.sp,
                                    color = if (isSelected) (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                                )
                            }
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF2563EB))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "ВЫБРАНО",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "💡 Вы в любой момент сможете включить любые инструменты или сменить тему в Настройках.",
                    fontSize = 11.sp,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )

                // Кнопка перехода к работе
                Button(
                    onClick = { onComplete(selectedRole) },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Начать работу с доской →",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
