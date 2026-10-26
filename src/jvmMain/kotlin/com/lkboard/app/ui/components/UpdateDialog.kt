package com.lkboard.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lkboard.app.ui.theme.LocalAppTheme
import com.lkboard.app.update.AppVersion
import com.lkboard.app.update.UpdateCheckState
import java.awt.Desktop
import java.net.URI

/**
 * Диалог уведомления о выходе новой версии приложения или подтверждения актуальности.
 */
@Composable
fun UpdateDialog(
    state: UpdateCheckState,
    onDismiss: () -> Unit
) {
    val isDark = LocalAppTheme.current.isDark
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            if (state.updateAvailable && state.updateInfo != null) {
                Button(
                    onClick = {
                        try {
                            val url = state.updateInfo.downloadUrl.ifBlank { AppVersion.DOWNLOAD_PAGE_URL }
                            Desktop.getDesktop().browse(URI.create(url))
                        } catch (_: Exception) {}
                        onDismiss()
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Скачать обновление", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            } else {
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("Понятно", fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            if (state.updateAvailable) {
                TextButton(onClick = onDismiss) {
                    Text("Напомнить позже", color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                }
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.isChecking) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp, color = Color(0xFF2563EB))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Проверка обновлений...", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A))
                } else if (state.updateAvailable && state.updateInfo != null) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF1E2638) else Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RocketLaunch,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Доступно обновление!", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A))
                        Text("Версия ${state.updateInfo.version}", fontSize = 12.sp, color = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB), fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("У вас последняя версия", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A))
                        Text("LKBoard v${AppVersion.VERSION_NAME}", fontSize = 12.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Medium)
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                if (state.isChecking) {
                    Text(
                        "Связываемся с сервером обновлений LKBoard...",
                        fontSize = 13.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                } else if (state.updateAvailable && state.updateInfo != null) {
                    val info = state.updateInfo
                    Text(
                        text = if (info.title.isNotBlank()) info.title else "Вышла новая версия приложения LKBoard с улучшениями и исправлениями.",
                        fontSize = 13.sp,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                        lineHeight = 18.sp
                    )

                    if (info.changelog.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Список изменений:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDark) Color(0xFF242830) else Color(0xFFF8FAFC))
                                .border(BorderStroke(1.dp, if (isDark) Color(0xFF3F4554) else Color(0xFFE2E8F0)), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                info.changelog.forEach { item ->
                                    Row(verticalAlignment = Alignment.Top) {
                                        Text("• ", color = Color(0xFF2563EB), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(item, fontSize = 12.sp, color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155), lineHeight = 16.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Текущая версия: v${AppVersion.VERSION_NAME} → Новая: v${info.version}",
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
                    )
                } else {
                    Text(
                        "Приложение работает на актуальной версии v${AppVersion.VERSION_NAME}. Все последние функции, шаблоны дорожных карт и Desmos-студия уже доступны.",
                        fontSize = 13.sp,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                        lineHeight = 18.sp
                    )
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = if (isDark) Color(0xFF181B20) else Color.White,
        titleContentColor = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A),
        textContentColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
    )
}
