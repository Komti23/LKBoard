package com.lkboard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lkboard.app.collaboration.NetworkUtils
import com.lkboard.app.data.AppTheme
import com.lkboard.app.data.UserRole
import com.lkboard.app.model.CanvasTool
import com.lkboard.app.ui.animation.bouncyClickable
import com.lkboard.app.update.AppVersion
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI

/**
 * Диалог настроек LKBoard:
 * - Выбор темы приложения (Тёмная, Светлая, Тетрадь в клетку).
 * - Режим графического планшета (смартфон / планшет со стилусом как графический планшет).
 * - Выбор быстрого профиля роли (Репетитор, Команда, Скетчи, Все инструменты).
 * - Выборочное включение / отключение инструментов на панели.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsDialog(
    currentTheme: AppTheme,
    currentRole: UserRole,
    enabledTools: Set<CanvasTool>,
    isTabletModeActive: Boolean = false,
    tabletWebLink: String = "",
    connectedTabletClientsCount: Int = 0,
    onToggleTabletMode: (Boolean) -> Unit = {},
    onDismiss: () -> Unit,
    onSaveSettings: (newTheme: AppTheme, newRole: UserRole, newEnabledTools: Set<CanvasTool>) -> Unit
) {
    var theme by remember { mutableStateOf(currentTheme) }
    var role by remember { mutableStateOf(currentRole) }
    var tools by remember { mutableStateOf(enabledTools) }
    var isTabletActive by remember(isTabletModeActive) { mutableStateOf(isTabletModeActive) }
    var isCopied by remember { mutableStateOf(false) }
    var studentNameInput by remember { mutableStateOf("Ученик 1") }
    var selectedStudentColorHex by remember { mutableStateOf(0xFF16A34AL) }
    var isStudentCopied by remember { mutableStateOf(false) }
    val isDark = theme.isDark

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(620.dp)
                .heightIn(max = 740.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF181B20) else Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF2E333D) else Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
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
                        text = "LKBoard v${AppVersion.VERSION_NAME}",
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

                // 2. Режим графического планшета (смартфон со стилусом)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📱 РЕЖИМ ГРАФИЧЕСКОГО ПЛАНШЕТА (ТЕЛЕФОН СО СТИЛУСОМ):",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )

                        // Переключатель режима планшета
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isTabletActive) (if (isDark) Color(0xFF1E3A8A) else Color(0xFFDBEAFE))
                                    else (if (isDark) Color(0xFF27272A) else Color(0xFFF1F5F9))
                                )
                                .border(
                                    1.dp,
                                    if (isTabletActive) (if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB))
                                    else (if (isDark) Color(0xFF3F3F46) else Color(0xFFCBD5E1)),
                                    RoundedCornerShape(8.dp)
                                )
                                .bouncyClickable {
                                    val nextState = !isTabletActive
                                    isTabletActive = nextState
                                    onToggleTabletMode(nextState)
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isTabletActive) "ВКЛЮЧЕНО ✓" else "ВЫКЛЮЧЕНО",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isTabletActive) (if (isDark) Color(0xFF38BDF8) else Color(0xFF1D4ED8)) else (if (isDark) Color(0xFF71717A) else Color(0xFF64748B))
                            )
                        }
                    }

                    if (isTabletActive) {
                        val ip = NetworkUtils.getPrimaryLocalIp()
                        val activeUrl = tabletWebLink.ifEmpty {
                            NetworkUtils.buildTabletWebLink(ip, NetworkUtils.DEFAULT_PORT)
                        }

                        val studentId = studentNameInput.trim().lowercase()
                            .replace(Regex("[^a-zA-Z0-9а-яА-Я_]+"), "_")
                            .ifEmpty { "student_1" }
                        val studentIndividualLink = NetworkUtils.buildTabletWebLink(
                            localIp = ip,
                            port = NetworkUtils.DEFAULT_PORT,
                            id = studentId,
                            name = studentNameInput.trim().ifEmpty { "Ученик" },
                            colorHex = selectedStudentColorHex
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0xFF121824) else Color(0xFFF0F7FF))
                                .border(1.dp, if (isDark) Color(0xFF1E3A8A) else Color(0xFFBFDBFE), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // 1. Статус и Основная ссылка хоста/учителя
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🟢 ОСНОВНАЯ ССЫЛКА (ВАШ ПЛАНШЕТ / СТИЛУС):",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (isDark) Color(0xFF38BDF8) else Color(0xFF1D4ED8)
                                        )
                                        if (connectedTabletClientsCount > 0) {
                                            Text(
                                                text = "📱 В сети: $connectedTabletClientsCount",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF16A34A)
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isDark) Color(0xFF0D1117) else Color.White)
                                            .border(1.dp, if (isDark) Color(0xFF283244) else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = activeUrl,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A),
                                            modifier = Modifier.weight(1f)
                                        )

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Button(
                                                onClick = {
                                                    try {
                                                        val sel = StringSelection(activeUrl)
                                                        Toolkit.getDefaultToolkit().systemClipboard.setContents(sel, sel)
                                                        isCopied = true
                                                    } catch (_: Exception) {}
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isCopied) Color(0xFF16A34A) else Color(0xFF2563EB),
                                                    contentColor = Color.White
                                                ),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text(if (isCopied) "✓ Скопировано" else "Копировать", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    try {
                                                        Desktop.getDesktop().browse(URI.create(activeUrl))
                                                    } catch (_: Exception) {}
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text("Открыть на ПК", fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }

                                // 2. Генератор индивидуальных ссылок для каждого ученика / планшета
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isDark) Color(0xFF181E2C) else Color(0xFFE2E8F0).copy(alpha = 0.45f))
                                        .border(1.dp, if (isDark) Color(0xFF2A374F) else Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🎓 ИНДИВИДУАЛЬНАЯ ССЫЛКА ДЛЯ УЧЕНИКА / ПЛАНШЕТА:",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                                        )
                                    }

                                    // Быстрые пресеты учеников
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(
                                            Triple("Ученик 1", 0xFF16A34AL, "🟢"),
                                            Triple("Ученик 2", 0xFFDC2626L, "🔴"),
                                            Triple("Ученик 3", 0xFF9333EAL, "🟣"),
                                            Triple("Планшет 2", 0xFFEA580CL, "🟠")
                                        ).forEach { (presetName, presetColor, presetEmoji) ->
                                            val isSelected = studentNameInput == presetName
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        if (isSelected) (if (isDark) Color(0xFF223554) else Color(0xFFDBEAFE))
                                                        else (if (isDark) Color(0xFF0F141E) else Color.White)
                                                    )
                                                    .border(
                                                        1.dp,
                                                        if (isSelected) (if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB))
                                                        else (if (isDark) Color(0xFF2D3748) else Color(0xFFE2E8F0)),
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .bouncyClickable {
                                                        studentNameInput = presetName
                                                        selectedStudentColorHex = presetColor
                                                        isStudentCopied = false
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "$presetEmoji $presetName",
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) (if (isDark) Color(0xFF38BDF8) else Color(0xFF1D4ED8)) else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155))
                                                )
                                            }
                                        }
                                    }

                                    // Поле имени и палитра цветов
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Поле ввода имени
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isDark) Color(0xFF0D1117) else Color.White)
                                                .border(1.dp, if (isDark) Color(0xFF283244) else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Имя: ",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                            )
                                            BasicTextField(
                                                value = studentNameInput,
                                                onValueChange = {
                                                    studentNameInput = it
                                                    isStudentCopied = false
                                                },
                                                singleLine = true,
                                                textStyle = TextStyle(
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
                                                ),
                                                cursorBrush = SolidColor(if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        // Выбор персонального цвета пера
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            listOf(
                                                0xFF2563EBL, // Синий
                                                0xFF16A34AL, // Зеленый
                                                0xFFDC2626L, // Красный
                                                0xFF9333EAL, // Фиолетовый
                                                0xFFEA580CL  // Оранжевый
                                            ).forEach { colorVal ->
                                                val isColorSelected = selectedStudentColorHex == colorVal
                                                Box(
                                                    modifier = Modifier
                                                        .size(22.dp)
                                                        .clip(RoundedCornerShape(50))
                                                        .background(Color(colorVal))
                                                        .border(
                                                            width = if (isColorSelected) 2.5.dp else 1.dp,
                                                            color = if (isColorSelected) Color.White else Color.Transparent,
                                                            shape = RoundedCornerShape(50)
                                                        )
                                                        .clickable {
                                                            selectedStudentColorHex = colorVal
                                                            isStudentCopied = false
                                                        }
                                                )
                                            }
                                        }
                                    }

                                    // Поле ссылки для ученика и кнопка копирования
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isDark) Color(0xFF0D1117) else Color.White)
                                            .border(1.dp, if (isDark) Color(0xFF283244) else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = studentIndividualLink,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(selectedStudentColorHex),
                                            modifier = Modifier.weight(1f)
                                        )

                                        Button(
                                            onClick = {
                                                try {
                                                    val sel = StringSelection(studentIndividualLink)
                                                    Toolkit.getDefaultToolkit().systemClipboard.setContents(sel, sel)
                                                    isStudentCopied = true
                                                } catch (_: Exception) {}
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isStudentCopied) Color(0xFF16A34A) else Color(selectedStudentColorHex),
                                                contentColor = Color.White
                                            ),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text(
                                                if (isStudentCopied) "✓ Ссылка скопирована!" else "Скопировать для ${studentNameInput.take(12)}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "💡 Удобство использования (Эргономика планшета):\n" +
                                            "• 1:1 Пропорции монитора: штрихи и окружности рисуются без искажения формы.\n" +
                                            "• Двухпальцевое перемещение: проведите 2 пальцами по экрану телефона, чтобы скроллить холст на ПК.\n" +
                                            "• Сворачиваемый тулбар: нажмите на кнопку панели, чтобы она свернулась в угол и не задевалась рукой.",
                                    fontSize = 10.sp,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }

                // 3. Быстрый профиль инструментов
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

                // 4. Выборочная настройка видимости инструментов
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
