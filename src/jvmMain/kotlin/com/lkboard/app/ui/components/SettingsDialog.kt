package com.lkboard.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lkboard.app.collaboration.NetworkUtils
import com.lkboard.app.collaboration.TabletActionRecord
import com.lkboard.app.data.AppTheme
import com.lkboard.app.data.StylusPressureCurve
import com.lkboard.app.data.ToolbarPosition
import com.lkboard.app.data.UserRole
import com.lkboard.app.model.CanvasTool
import com.lkboard.app.timer.AudioChimePlayer
import com.lkboard.app.ui.animation.bouncyClickable
import com.lkboard.app.update.AppVersion
import com.lkboard.app.util.QrCodeGenerator
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI

/**
 * Диалог настроек LKBoard (отображается по центру экрана):
 * - Выбор положения панели инструментов: Слева / Снизу / Справа (ToolbarPosition).
 * - Выбор темы приложения (Тёмная, Светлая, Тетрадь в клетку, Система).
 * - Режим графического планшета (телефон со стилусом): длинная индивидуальная ссылка, QR-код, лента 3 действий, Focus Lock, Chime, калибровка нажима.
 * - Выбор быстрого профиля роли (Репетитор, Команда, Скетчи, Все инструменты).
 * - Выборочное включение / отключение инструментов на панели (скроллируемый список с иконками).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsDialog(
    currentTheme: AppTheme,
    currentRole: UserRole,
    enabledTools: Set<CanvasTool>,
    toolbarPosition: ToolbarPosition = ToolbarPosition.LEFT,
    onChangeToolbarPosition: (ToolbarPosition) -> Unit = {},
    isTabletModeActive: Boolean = false,
    tabletWebLink: String = "",
    connectedTabletClientsCount: Int = 0,
    recentTabletActions: List<TabletActionRecord> = emptyList(),
    isFocusLocked: Boolean = false,
    stylusPressureCurve: StylusPressureCurve = StylusPressureCurve.NORMAL,
    isConnectChimeEnabled: Boolean = true,
    onToggleTabletMode: (Boolean) -> Unit = {},
    onToggleFocusLock: (Boolean?) -> Unit = {},
    onChangeStylusPressureCurve: (StylusPressureCurve) -> Unit = {},
    onToggleConnectChime: (Boolean) -> Unit = {},
    onDismiss: () -> Unit,
    onSaveSettings: (newTheme: AppTheme, newRole: UserRole, newEnabledTools: Set<CanvasTool>, newToolbarPosition: ToolbarPosition) -> Unit
) {
    var theme by remember(currentTheme) { mutableStateOf(currentTheme) }
    var role by remember(currentRole) { mutableStateOf(currentRole) }
    var tools by remember(enabledTools) { mutableStateOf(enabledTools) }
    var selectedPosition by remember(toolbarPosition) { mutableStateOf(toolbarPosition) }
    var isTabletActive by remember(isTabletModeActive) { mutableStateOf(isTabletModeActive) }
    var activeFocusLocked by remember(isFocusLocked) { mutableStateOf(isFocusLocked) }
    var activePressureCurve by remember(stylusPressureCurve) { mutableStateOf(stylusPressureCurve) }
    var chimeEnabled by remember(isConnectChimeEnabled) { mutableStateOf(isConnectChimeEnabled) }
    var studentNameInput by remember { mutableStateOf("Преподаватель") }
    var selectedStudentColorHex by remember { mutableStateOf(0xFF2563EBL) }
    var isLinkCopied by remember { mutableStateOf(false) }
    var showQrModal by remember { mutableStateOf(false) }
    val isDark = theme.isDark

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(620.dp)
                .heightIn(max = 760.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF14171D) else Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF2E333D) else Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Верхний заголовок диалога с кнопкой закрытия
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
                            text = "Настройки",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
                        )
                        Text(
                            text = "LKBoard v${AppVersion.VERSION_NAME}",
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

                // 2. Скроллируемое тело настроек
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                        // 1. ПОЛОЖЕНИЕ ПАНЕЛИ ИНСТРУМЕНТОВ (Слева / Снизу / Справа)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "РАСПОЛОЖЕНИЕ ПАНЕЛИ ИНСТРУМЕНТОВ:",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ToolbarPosition.entries.forEach { pos ->
                                    val isSelected = pos == selectedPosition
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
                                            .clickable {
                                                selectedPosition = pos
                                                onChangeToolbarPosition(pos)
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${pos.icon} ${pos.title}",
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) (if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)) else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569))
                                        )
                                    }
                                }
                            }

                            Text(
                                text = selectedPosition.description,
                                fontSize = 10.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }

                        // 2. ТЕМА ОФОРМЛЕНИЯ ХОЛСТА И ИНТЕРФЕЙСА
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "ТЕМА ОФОРМЛЕНИЯ ХОЛСТА И ИНТЕРФЕЙСА:",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AppTheme.entries.forEach { appTheme ->
                                    val isSelected = appTheme == theme
                                    val (themeIcon, themeLabel) = when(appTheme) {
                                        AppTheme.SYSTEM -> "🖥️" to "Система"
                                        AppTheme.DARK -> "🌙" to "Тёмная"
                                        AppTheme.LIGHT -> "☀️" to "Светлая"
                                        AppTheme.NOTEBOOK -> "📖" to "Тетрадь"
                                    }
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
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(text = themeIcon, fontSize = 13.sp)
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = themeLabel,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) (if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)) else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569))
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3. РЕЖИМ ГРАФИЧЕСКОГО ПЛАНШЕТА (ТЕЛЕФОН СО СТИЛУСОМ)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📱 РЕЖИМ ГРАФИЧЕСКОГО ПЛАНШЕТА:",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )

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
                                val cleanId = studentNameInput.trim().lowercase()
                                    .replace(Regex("[^a-zA-Z0-9а-яА-Я_]+"), "_")
                                    .ifEmpty { "tablet" }
                                val fullLongLink = NetworkUtils.buildTabletWebLink(
                                    localIp = ip,
                                    port = NetworkUtils.DEFAULT_PORT,
                                    id = cleanId,
                                    name = studentNameInput.trim().ifEmpty { "Планшет" },
                                    colorHex = selectedStudentColorHex,
                                    pressureCurve = activePressureCurve.id
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isDark) Color(0xFF121824) else Color(0xFFF0F7FF))
                                        .border(1.dp, if (isDark) Color(0xFF1E3A8A) else Color(0xFFBFDBFE), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        // Заголовок статуса подключения
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "🟢 СЕРВЕР ПЛАНШЕТА АКТИВЕН:",
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

                                        // Пресеты участников
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf(
                                                Triple("Преподаватель", 0xFF2563EBL, "👨‍🏫"),
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
                                                            isLinkCopied = false
                                                        }
                                                        .padding(horizontal = 6.dp, vertical = 4.dp)
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

                                        // Поле ввода имени и палитра
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
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
                                                    text = "Устройство: ",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                                )
                                                BasicTextField(
                                                    value = studentNameInput,
                                                    onValueChange = {
                                                        studentNameInput = it
                                                        isLinkCopied = false
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

                                            // Палитра цвета пера
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                listOf(
                                                    0xFF2563EBL,
                                                    0xFF16A34AL,
                                                    0xFFDC2626L,
                                                    0xFF9333EAL,
                                                    0xFFEA580CL
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
                                                                isLinkCopied = false
                                                            }
                                                    )
                                                }
                                            }
                                        }

                                        // Длинная ссылка
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
                                                text = fullLongLink,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color(selectedStudentColorHex),
                                                modifier = Modifier.weight(1f)
                                            )

                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Button(
                                                    onClick = {
                                                        try {
                                                            val sel = StringSelection(fullLongLink)
                                                            Toolkit.getDefaultToolkit().systemClipboard.setContents(sel, sel)
                                                            isLinkCopied = true
                                                        } catch (_: Exception) {}
                                                    },
                                                    shape = RoundedCornerShape(6.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (isLinkCopied) Color(0xFF16A34A) else Color(selectedStudentColorHex),
                                                        contentColor = Color.White
                                                    ),
                                                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text(if (isLinkCopied) "✓ Скопировано" else "Копировать", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }

                                                OutlinedButton(
                                                    onClick = { showQrModal = true },
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text("📷 QR", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        try {
                                                            Desktop.getDesktop().browse(URI.create(fullLongLink))
                                                        } catch (_: Exception) {}
                                                    },
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text("↗", fontSize = 11.sp)
                                                }
                                            }
                                        }

                                        // РЕЖИМ ФИКСАЦИИ ВНИМАНИЯ УЧИТЕЛЯ (TEACHER FOCUS LOCK)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (activeFocusLocked) (if (isDark) Color(0xFF3B1219) else Color(0xFFFEF2F2))
                                                    else (if (isDark) Color(0xFF0F141E) else Color(0xFFF1F5F9))
                                                )
                                                .border(
                                                    1.dp,
                                                    if (activeFocusLocked) (if (isDark) Color(0xFFEF4444) else Color(0xFFFCA5A5))
                                                    else (if (isDark) Color(0xFF222F46) else Color(0xFFCBD5E1)),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(9.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (activeFocusLocked) "🔒 РЕЖИМ ВНИМАНИЯ АКТИВЕН" else "🔒 РЕЖИМ ВНИМАНИЯ («СМОТРЕТЬ СЮДА»):",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = if (activeFocusLocked) Color(0xFFEF4444) else (if (isDark) Color(0xFF38BDF8) else Color(0xFF1D4ED8))
                                                )
                                                Text(
                                                    text = if (activeFocusLocked)
                                                        "Рисование на телефонах учеников заблокировано. Они следят за демонстрацией."
                                                    else
                                                        "Блокирует рисование на телефонах всех учеников во время объяснения материала.",
                                                    fontSize = 10.sp,
                                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                                )
                                            }

                                            Button(
                                                onClick = {
                                                    val next = !activeFocusLocked
                                                    activeFocusLocked = next
                                                    onToggleFocusLock(next)
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (activeFocusLocked) Color(0xFFDC2626) else Color(0xFF2563EB),
                                                    contentColor = Color.White
                                                ),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text(
                                                    text = if (activeFocusLocked) "🔓 Снять блок" else "🔒 Включить",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        // КАЛИБРОВКА КРИВОЙ НАЖИМА СТИЛУСА (STYLUS PRESSURE CURVE)
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isDark) Color(0xFF0F141E) else Color(0xFFF1F5F9))
                                                .border(1.dp, if (isDark) Color(0xFF222F46) else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                                .padding(9.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "🎚️ КАЛИБРОВКА КРИВОЙ НАЖИМА СТИЛУСА:",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF1D4ED8)
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                StylusPressureCurve.entries.forEach { curve ->
                                                    val isSelected = activePressureCurve == curve
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(
                                                                if (isSelected) (if (isDark) Color(0xFF1E3A8A) else Color(0xFFDBEAFE))
                                                                else (if (isDark) Color(0xFF181F2C) else Color.White)
                                                            )
                                                            .border(
                                                                1.2.dp,
                                                                if (isSelected) (if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB))
                                                                else (if (isDark) Color(0xFF2D3748) else Color(0xFFE2E8F0)),
                                                                RoundedCornerShape(6.dp)
                                                            )
                                                            .bouncyClickable {
                                                                activePressureCurve = curve
                                                                onChangeStylusPressureCurve(curve)
                                                            }
                                                            .padding(vertical = 5.dp, horizontal = 4.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "${curve.icon} ${curve.title}",
                                                            fontSize = 10.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                            color = if (isSelected) (if (isDark) Color(0xFF38BDF8) else Color(0xFF1D4ED8)) else (if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155))
                                                        )
                                                    }
                                                }
                                            }

                                            Text(
                                                text = activePressureCurve.subtitle,
                                                fontSize = 9.sp,
                                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                            )
                                        }

                                        // ЗВУКОВОЙ ОТКЛИК ПОДКЛЮЧЕНИЯ УЧЕНИКОВ (CHIME)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isDark) Color(0xFF0F141E) else Color(0xFFF1F5F9))
                                                .border(1.dp, if (isDark) Color(0xFF222F46) else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 9.dp, vertical = 5.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Checkbox(
                                                    checked = chimeEnabled,
                                                    onCheckedChange = {
                                                        chimeEnabled = it
                                                        onToggleConnectChime(it)
                                                    },
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = Color(0xFF10B981),
                                                        checkmarkColor = Color.White
                                                    )
                                                )
                                                Column {
                                                    Text(
                                                        text = "🔊 Звуковой отклик при входе ученика",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
                                                    )
                                                    Text(
                                                        text = "Мягкий сигнал на ПК при сканировании QR или переходе",
                                                        fontSize = 9.sp,
                                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                                    )
                                                }
                                            }

                                            OutlinedButton(
                                                onClick = { AudioChimePlayer.playConnectChime() },
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(26.dp)
                                            ) {
                                                Text("▶ Тест", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        // 3 ПОСЛЕДНИХ ДЕЙСТВИЯ НА ПЛАНШЕТАХ
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isDark) Color(0xFF0F141E) else Color(0xFFF1F5F9))
                                                .border(1.dp, if (isDark) Color(0xFF222F46) else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                                .padding(9.dp),
                                            verticalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "📋 3 ПОСЛЕДНИХ ДЕЙСТВИЯ С ПЛАНШЕТА:",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = if (isDark) Color(0xFF38BDF8) else Color(0xFF1D4ED8)
                                                )
                                                if (recentTabletActions.isNotEmpty()) {
                                                    Text(
                                                        text = "● Живой поток",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF10B981)
                                                    )
                                                }
                                            }

                                            if (recentTabletActions.isEmpty()) {
                                                Text(
                                                    text = "Ожидание действий: проведите пером по экрану телефона, чтобы увидеть запись здесь в реальном времени.",
                                                    fontSize = 10.sp,
                                                    color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                                                )
                                            } else {
                                                recentTabletActions.take(3).forEach { act ->
                                                    val ageSec = maxOf(0L, (System.currentTimeMillis() - act.timestamp) / 1000L)
                                                    val timeStr = if (ageSec < 4) "только что" else "$ageSec сек назад"
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            Text(text = act.icon, fontSize = 12.sp)
                                                            Text(
                                                                text = "${act.peerName}: ${act.actionTitle} ${if (act.detail.isNotBlank()) "(${act.detail})" else ""}",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
                                                            )
                                                        }
                                                        Text(
                                                            text = timeStr,
                                                            fontSize = 9.sp,
                                                            fontFamily = FontFamily.Monospace,
                                                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Text(
                                            text = "💡 Советы:\n" +
                                                    "• Используйте кнопку «📷 QR», чтобы открыть планшет камерой за секунду.\n" +
                                                    "• 1:1 Пропорции монитора: штрихи рисуются без искажения геометрии.\n" +
                                                    "• Двухпальцевое перемещение: проведите 2 пальцами по телефону, чтобы скроллить холст на ПК.",
                                            fontSize = 10.sp,
                                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                            lineHeight = 14.sp
                                        )
                                    }
                                }

                                // Диалог с QR-кодом для мгновенного сканирования камерой телефона
                                if (showQrModal) {
                                    Dialog(onDismissRequest = { showQrModal = false }) {
                                        Card(
                                            modifier = Modifier.width(360.dp),
                                            shape = RoundedCornerShape(20.dp),
                                            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF181B20) else Color.White),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF2E333D) else Color(0xFFCBD5E1))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(22.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(14.dp)
                                            ) {
                                                Text(
                                                    text = "📷 Сканируйте QR-код",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
                                                )
                                                Text(
                                                    text = "Наведите камеру смартфона, чтобы сразу открыть планшет без ввода ссылки:",
                                                    fontSize = 11.sp,
                                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                                    textAlign = TextAlign.Center
                                                )

                                                val qrMatrix = remember(fullLongLink) {
                                                    try {
                                                        QrCodeGenerator.generateQrMatrix(fullLongLink)
                                                    } catch (_: Exception) {
                                                        emptyArray()
                                                    }
                                                }

                                                if (qrMatrix.isNotEmpty()) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(220.dp)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(Color.White)
                                                            .padding(10.dp)
                                                    ) {
                                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                                            val cellW = size.width / qrMatrix[0].size
                                                            val cellH = size.height / qrMatrix.size
                                                            for (y in qrMatrix.indices) {
                                                                for (x in qrMatrix[y].indices) {
                                                                    if (qrMatrix[y][x]) {
                                                                        drawRect(
                                                                            color = Color.Black,
                                                                            topLeft = Offset(x * cellW, y * cellH),
                                                                            size = Size(cellW, cellH)
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                                Text(
                                                    text = fullLongLink,
                                                    fontSize = 9.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = if (isDark) Color(0xFF38BDF8) else Color(0xFF1D4ED8),
                                                    maxLines = 2,
                                                    textAlign = TextAlign.Center
                                                )

                                                Button(
                                                    onClick = { showQrModal = false },
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text("Готово", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 4. БЫСТРЫЙ ПРОФИЛЬ ДЛЯ ВАШИХ ЗАДАЧ
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "БЫСТРЫЙ ПРОФИЛЬ ДЛЯ ВАШИХ ЗАДАЧ:",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
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

                        // 5. ВЫБОРОЧНАЯ НАСТРОЙКА ВИДИМОСТИ ИНСТРУМЕНТОВ (СПИСКОМ С ПРОКРУТКОЙ)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ВИДИМОСТЬ ИНСТРУМЕНТОВ НА ПАНЕЛИ:",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                                Text(
                                    text = "Включено: ${tools.size}/${CanvasTool.entries.size}",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDark) Color(0xFF0F1217) else Color(0xFFF1F5F9))
                                    .border(1.dp, if (isDark) Color(0xFF232833) else Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                                    .padding(6.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    CanvasTool.entries.filter { it != CanvasTool.SELECT && it != CanvasTool.HAND }.forEach { tool ->
                                        val isEnabled = tool in tools
                                        val (toolIcon, toolShortcut) = when(tool) {
                                            CanvasTool.PEN -> "✏️" to "Q / P · Рисование стилусом и пером"
                                            CanvasTool.ERASER -> "🧹" to "E · Стирание штрихов"
                                            CanvasTool.STICKY_NOTE -> "📝" to "S · Заметки и списки задач To-Do"
                                            CanvasTool.SHAPE -> "📐" to "R · Прямоугольники, круги, стрелки"
                                            CanvasTool.LATEX -> "∑" to "X · Математические уравнения и формулы"
                                            CanvasTool.PHYSICS_STENCIL -> "⚡" to "B · Физический конструктор и лаборатория"
                                            CanvasTool.CONNECTOR -> "🔀" to "C · Умные стрелки и связки блоков"
                                            CanvasTool.MILESTONE -> "🚩" to "M · Вехи и этапы дорожной карты"
                                            CanvasTool.KANBAN -> "📋" to "K · Доски задач To Do / Done"
                                            CanvasTool.FUNCTION_GRAPH -> "📈" to "G · Графики математических функций"
                                            CanvasTool.TEXT -> "🔤" to "T · Текстовые блоки"
                                            CanvasTool.FRAME -> "🖼️" to "F · Слайды и рамки презентации"
                                            CanvasTool.LASER_POINTER -> "🔴" to "L · Лазерная указка для объяснений"
                                            else -> "🛠️" to ""
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isEnabled) (if (isDark) Color(0xFF19202E) else Color.White)
                                                    else (if (isDark) Color(0xFF14171E) else Color(0xFFF8FAFC))
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isEnabled) (if (isDark) Color(0xFF2E3D59) else Color(0xFFBFDBFE))
                                                    else (if (isDark) Color(0xFF1E232D) else Color(0xFFE2E8F0)),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    tools = if (isEnabled) tools - tool else tools + tool
                                                }
                                                .padding(horizontal = 10.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(if (isEnabled) (if (isDark) Color(0xFF202A3C) else Color(0xFFDBEAFE)) else (if (isDark) Color(0xFF1A1E26) else Color(0xFFE2E8F0))),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(text = toolIcon, fontSize = 13.sp)
                                                }

                                                Column {
                                                    Text(
                                                        text = tool.displayName.substringBefore(" ("),
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isEnabled) FontWeight.SemiBold else FontWeight.Normal,
                                                        color = if (isEnabled) (if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)) else (if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8))
                                                    )
                                                    if (toolShortcut.isNotBlank()) {
                                                        Text(
                                                            text = toolShortcut,
                                                            fontSize = 9.sp,
                                                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                                        )
                                                    }
                                                }
                                            }

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
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Нижняя панель действий (Отмена и Применить)
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
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF3F3F46) else Color(0xFFCBD5E1))
                        ) {
                            Text("Закрыть", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                onSaveSettings(theme, role, tools, selectedPosition)
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

/**
 * Обратная совместимость с вызовом SettingsSidePanel.
 */
@Composable
fun SettingsSidePanel(
    isOpen: Boolean,
    currentTheme: AppTheme,
    currentRole: UserRole,
    enabledTools: Set<CanvasTool>,
    toolbarPosition: ToolbarPosition = ToolbarPosition.LEFT,
    onChangeToolbarPosition: (ToolbarPosition) -> Unit = {},
    isTabletModeActive: Boolean = false,
    tabletWebLink: String = "",
    connectedTabletClientsCount: Int = 0,
    recentTabletActions: List<TabletActionRecord> = emptyList(),
    isFocusLocked: Boolean = false,
    stylusPressureCurve: StylusPressureCurve = StylusPressureCurve.NORMAL,
    isConnectChimeEnabled: Boolean = true,
    onToggleTabletMode: (Boolean) -> Unit = {},
    onToggleFocusLock: (Boolean?) -> Unit = {},
    onChangeStylusPressureCurve: (StylusPressureCurve) -> Unit = {},
    onToggleConnectChime: (Boolean) -> Unit = {},
    onDismiss: () -> Unit,
    onSaveSettings: (newTheme: AppTheme, newRole: UserRole, newEnabledTools: Set<CanvasTool>, newToolbarPosition: ToolbarPosition) -> Unit
) {
    if (isOpen) {
        SettingsDialog(
            currentTheme = currentTheme,
            currentRole = currentRole,
            enabledTools = enabledTools,
            toolbarPosition = toolbarPosition,
            onChangeToolbarPosition = onChangeToolbarPosition,
            isTabletModeActive = isTabletModeActive,
            tabletWebLink = tabletWebLink,
            connectedTabletClientsCount = connectedTabletClientsCount,
            recentTabletActions = recentTabletActions,
            isFocusLocked = isFocusLocked,
            stylusPressureCurve = stylusPressureCurve,
            isConnectChimeEnabled = isConnectChimeEnabled,
            onToggleTabletMode = onToggleTabletMode,
            onToggleFocusLock = onToggleFocusLock,
            onChangeStylusPressureCurve = onChangeStylusPressureCurve,
            onToggleConnectChime = onToggleConnectChime,
            onDismiss = onDismiss,
            onSaveSettings = onSaveSettings
        )
    }
}

/**
 * Оверлоад для совместимости с 3-аргументным onSaveSettings
 */
@Composable
fun SettingsDialog(
    currentTheme: AppTheme,
    currentRole: UserRole,
    enabledTools: Set<CanvasTool>,
    isTabletModeActive: Boolean = false,
    tabletWebLink: String = "",
    connectedTabletClientsCount: Int = 0,
    recentTabletActions: List<TabletActionRecord> = emptyList(),
    isFocusLocked: Boolean = false,
    stylusPressureCurve: StylusPressureCurve = StylusPressureCurve.NORMAL,
    isConnectChimeEnabled: Boolean = true,
    onToggleTabletMode: (Boolean) -> Unit = {},
    onToggleFocusLock: (Boolean?) -> Unit = {},
    onChangeStylusPressureCurve: (StylusPressureCurve) -> Unit = {},
    onToggleConnectChime: (Boolean) -> Unit = {},
    onDismiss: () -> Unit,
    onSaveSettings: (newTheme: AppTheme, newRole: UserRole, newEnabledTools: Set<CanvasTool>) -> Unit
) {
    SettingsDialog(
        currentTheme = currentTheme,
        currentRole = currentRole,
        enabledTools = enabledTools,
        toolbarPosition = ToolbarPosition.LEFT,
        onChangeToolbarPosition = {},
        isTabletModeActive = isTabletModeActive,
        tabletWebLink = tabletWebLink,
        connectedTabletClientsCount = connectedTabletClientsCount,
        recentTabletActions = recentTabletActions,
        isFocusLocked = isFocusLocked,
        stylusPressureCurve = stylusPressureCurve,
        isConnectChimeEnabled = isConnectChimeEnabled,
        onToggleTabletMode = onToggleTabletMode,
        onToggleFocusLock = onToggleFocusLock,
        onChangeStylusPressureCurve = onChangeStylusPressureCurve,
        onToggleConnectChime = onToggleConnectChime,
        onDismiss = onDismiss,
        onSaveSettings = { t, r, tools, _ -> onSaveSettings(t, r, tools) }
    )
}
