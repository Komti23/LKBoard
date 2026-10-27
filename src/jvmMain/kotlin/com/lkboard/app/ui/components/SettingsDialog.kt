package com.lkboard.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TabletAndroid
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
 * Премиальный диалог настроек LKBoard:
 * - Выбор положения панели инструментов: Слева / Снизу / Справа.
 * - Выбор темы (Система, Тёмная, Светлая, Тетрадь).
 * - Режим графического планшета (стилус, QR, ссылка, калибровка, Focus Lock).
 * - Выбор профиля роли (Репетитор, Команда, Скетчи, Все инструменты).
 * - Список видимости инструментов на панели.
 * - Чистый Fluent/Apple дизайн: без смайликов, с векторными иконками и плавными анимациями.
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

    // Цветовая палитра: нейтральные премиальные поверхности без синего оттенка
    val dialogBg = if (isDark) Color(0xFF13151A) else Color(0xFFFFFFFF)
    val cardHeaderBg = if (isDark) Color(0xFF181B22) else Color(0xFFF8FAFC)
    val cardBorder = if (isDark) Color(0xFF262A34) else Color(0xFFE2E8F0)
    val sectionHeaderColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    val coroutineScope = rememberCoroutineScope()
    val animProgress = remember { Animatable(0f) }
    var isClosing by remember { mutableStateOf(false) }

    fun collapseAndDismiss(onAfterClose: (() -> Unit)? = null) {
        if (isClosing) return
        isClosing = true
        coroutineScope.launch {
            animProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 150, easing = FastOutLinearInEasing)
            )
            onAfterClose?.invoke()
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Escape) {
                    collapseAndDismiss()
                    true
                } else false
            },
        contentAlignment = Alignment.Center
    ) {
        val progress = animProgress.value.coerceIn(0f, 1f)

        // Затемняющий фон (scrim), плавно закрывающийся при клике
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.50f * progress))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    collapseAndDismiss()
                }
        )

        Card(
            modifier = Modifier
                .width(640.dp)
                .heightIn(max = 760.dp)
                .graphicsLayer {
                    scaleX = 0.90f + 0.10f * progress
                    scaleY = 0.90f + 0.10f * progress
                    alpha = progress
                    translationY = (1f - progress) * -28.dp.toPx()
                }
                .shadow(elevation = 28.dp, shape = RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = dialogBg,
                contentColor = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // 1. Верхний заголовок модального окна
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cardHeaderBg)
                        .border(1.dp, cardBorder)
                        .padding(horizontal = 22.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Настройки",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isDark) Color(0xFF1E2638) else Color(0xFFEEF2F6))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "v${AppVersion.VERSION_NAME}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) Color(0xFF242832) else Color(0xFFE2E8F0))
                            .bouncyClickable(hoverScale = 1.08f, pressedScale = 0.90f) { collapseAndDismiss() },
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
                        .padding(horizontal = 22.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {

                    // СЕКЦИЯ 1: РАСПОЛОЖЕНИЕ ПАНЕЛИ ИНСТРУМЕНТОВ
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "РАСПОЛОЖЕНИЕ ПАНЕЛИ ИНСТРУМЕНТОВ",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = sectionHeaderColor
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ToolbarPosition.entries.forEach { pos ->
                                val isSelected = pos == selectedPosition
                                val iconVector: ImageVector = when (pos) {
                                    ToolbarPosition.LEFT -> Icons.AutoMirrored.Filled.ArrowBack
                                    ToolbarPosition.BOTTOM -> Icons.Default.ArrowDownward
                                    ToolbarPosition.RIGHT -> Icons.AutoMirrored.Filled.ArrowForward
                                }
                                SegmentOptionButton(
                                    selected = isSelected,
                                    title = pos.title,
                                    icon = iconVector,
                                    isDark = isDark,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        selectedPosition = pos
                                        onChangeToolbarPosition(pos)
                                    }
                                )
                            }
                        }

                        Text(
                            text = selectedPosition.description,
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }

                    // СЕКЦИЯ 2: ТЕМА ОФОРМЛЕНИЯ ХОЛСТА И ИНТЕРФЕЙСА
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "ТЕМА ОФОРМЛЕНИЯ",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = sectionHeaderColor
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppTheme.entries.forEach { appTheme ->
                                val isSelected = appTheme == theme
                                val (iconVector, themeLabel) = when (appTheme) {
                                    AppTheme.SYSTEM -> Icons.Default.Computer to "Система"
                                    AppTheme.DARK -> Icons.Default.DarkMode to "Тёмная"
                                    AppTheme.LIGHT -> Icons.Default.LightMode to "Светлая"
                                    AppTheme.NOTEBOOK -> Icons.Default.GridOn to "Тетрадь"
                                }
                                SegmentOptionButton(
                                    selected = isSelected,
                                    title = themeLabel,
                                    icon = iconVector,
                                    isDark = isDark,
                                    modifier = Modifier.weight(1f),
                                    onClick = { theme = appTheme }
                                )
                            }
                        }
                    }

                    // СЕКЦИЯ 3: РЕЖИМ ГРАФИЧЕСКОГО ПЛАНШЕТА (ТЕЛЕФОН СО СТИЛУСОМ)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TabletAndroid,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "РЕЖИМ ГРАФИЧЕСКОГО ПЛАНШЕТА",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = sectionHeaderColor
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isTabletActive) (if (isDark) Color(0xFF1E3A8A) else Color(0xFFDBEAFE))
                                        else (if (isDark) Color(0xFF242832) else Color(0xFFF1F5F9))
                                    )
                                    .border(
                                        1.dp,
                                        if (isTabletActive) (if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB))
                                        else (if (isDark) Color(0xFF333844) else Color(0xFFCBD5E1)),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .bouncyClickable {
                                        val nextState = !isTabletActive
                                        isTabletActive = nextState
                                        onToggleTabletMode(nextState)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    if (isTabletActive) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF1D4ED8),
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                    Text(
                                        text = if (isTabletActive) "ВКЛЮЧЕНО" else "ВЫКЛЮЧЕНО",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isTabletActive) (if (isDark) Color(0xFF38BDF8) else Color(0xFF1D4ED8)) else (if (isDark) Color(0xFF71717A) else Color(0xFF64748B))
                                    )
                                }
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
                                    .background(if (isDark) Color(0xFF171A22) else Color(0xFFF8FAFC))
                                    .border(1.dp, if (isDark) Color(0xFF262C38) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                    .padding(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                                    // Заголовок статуса подключения
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF16A34A),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "СЕРВЕР ПЛАНШЕТА АКТИВЕН",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF1D4ED8)
                                            )
                                        }

                                        if (connectedTabletClientsCount > 0) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PhoneAndroid,
                                                    contentDescription = null,
                                                    tint = Color(0xFF16A34A),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = "В сети: $connectedTabletClientsCount",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF16A34A)
                                                )
                                            }
                                        }
                                    }

                                    // Пресеты участников (без эмодзи)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(
                                            Pair("Преподаватель", 0xFF2563EBL),
                                            Pair("Ученик 1", 0xFF16A34AL),
                                            Pair("Ученик 2", 0xFFDC2626L),
                                            Pair("Ученик 3", 0xFF9333EAL),
                                            Pair("Планшет 2", 0xFFEA580CL)
                                        ).forEach { (presetName, presetColor) ->
                                            val isSelected = studentNameInput == presetName
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        if (isSelected) (if (isDark) Color(0xFF222B3D) else Color(0xFFEFF6FF))
                                                        else (if (isDark) Color(0xFF11141B) else Color.White)
                                                    )
                                                    .border(
                                                        1.dp,
                                                        if (isSelected) (if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB))
                                                        else (if (isDark) Color(0xFF262A34) else Color(0xFFE2E8F0)),
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .bouncyClickable {
                                                        studentNameInput = presetName
                                                        selectedStudentColorHex = presetColor
                                                        isLinkCopied = false
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(presetColor))
                                                    )
                                                    Text(
                                                        text = presetName,
                                                        fontSize = 10.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) (if (isDark) Color(0xFF38BDF8) else Color(0xFF1D4ED8)) else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155))
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Поле ввода имени устройства и палитра
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isDark) Color(0xFF111319) else Color.White)
                                                .border(1.dp, if (isDark) Color(0xFF282E3C) else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
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
                                                        .clip(CircleShape)
                                                        .background(Color(colorVal))
                                                        .border(
                                                            width = if (isColorSelected) 2.5.dp else 1.dp,
                                                            color = if (isColorSelected) Color.White else Color.Transparent,
                                                            shape = CircleShape
                                                        )
                                                        .clickable {
                                                            selectedStudentColorHex = colorVal
                                                            isLinkCopied = false
                                                        }
                                                )
                                            }
                                        }
                                    }

                                    // Длинная ссылка с кнопками копирования и QR
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isDark) Color(0xFF111319) else Color.White)
                                            .border(1.dp, if (isDark) Color(0xFF282E3C) else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
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
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (isLinkCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Text(if (isLinkCopied) "Скопировано" else "Копировать", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            OutlinedButton(
                                                onClick = { showQrModal = true },
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.QrCode,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Text("QR-код", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
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
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                    contentDescription = "Открыть в браузере",
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Режим фиксации внимания учителя (Teacher Focus Lock)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (activeFocusLocked) (if (isDark) Color(0xFF3B1219) else Color(0xFFFEF2F2))
                                                else (if (isDark) Color(0xFF11141B) else Color.White)
                                            )
                                            .border(
                                                1.dp,
                                                if (activeFocusLocked) (if (isDark) Color(0xFFEF4444) else Color(0xFFFCA5A5))
                                                else (if (isDark) Color(0xFF262C38) else Color(0xFFE2E8F0)),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(9.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (activeFocusLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                                    contentDescription = null,
                                                    tint = if (activeFocusLocked) Color(0xFFEF4444) else (if (isDark) Color(0xFF38BDF8) else Color(0xFF1D4ED8)),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = if (activeFocusLocked) "РЕЖИМ ВНИМАНИЯ АКТИВЕН" else "РЕЖИМ ВНИМАНИЯ («СМОТРЕТЬ СЮДА»)",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = if (activeFocusLocked) Color(0xFFEF4444) else (if (isDark) Color(0xFF38BDF8) else Color(0xFF1D4ED8))
                                                )
                                            }
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
                                                text = if (activeFocusLocked) "Снять блок" else "Включить",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // Калибровка нажима стилуса
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isDark) Color(0xFF11141B) else Color.White)
                                            .border(1.dp, if (isDark) Color(0xFF262C38) else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                            .padding(9.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Tune,
                                                contentDescription = null,
                                                tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF1D4ED8),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "КАЛИБРОВКА КРИВОЙ НАЖИМА СТИЛУСА",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF1D4ED8)
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            StylusPressureCurve.entries.forEach { curve ->
                                                val isSelected = activePressureCurve == curve
                                                val curveIcon: ImageVector = when (curve) {
                                                    StylusPressureCurve.SOFT -> Icons.Default.Gesture
                                                    StylusPressureCurve.NORMAL -> Icons.Default.Tune
                                                    StylusPressureCurve.HARD -> Icons.Default.Edit
                                                    StylusPressureCurve.CAPACITIVE -> Icons.Default.Speed
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(
                                                            if (isSelected) (if (isDark) Color(0xFF1E3A8A) else Color(0xFFEFF6FF))
                                                            else (if (isDark) Color(0xFF181F2C) else Color(0xFFF8FAFC))
                                                        )
                                                        .border(
                                                            1.2.dp,
                                                            if (isSelected) (if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB))
                                                            else (if (isDark) Color(0xFF262C38) else Color(0xFFE2E8F0)),
                                                            RoundedCornerShape(6.dp)
                                                        )
                                                        .bouncyClickable {
                                                            activePressureCurve = curve
                                                            onChangeStylusPressureCurve(curve)
                                                        }
                                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = curveIcon,
                                                            contentDescription = null,
                                                            tint = if (isSelected) (if (isDark) Color(0xFF38BDF8) else Color(0xFF1D4ED8)) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Text(
                                                            text = curve.title,
                                                            fontSize = 10.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                            color = if (isSelected) (if (isDark) Color(0xFF38BDF8) else Color(0xFF1D4ED8)) else (if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155))
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Text(
                                            text = activePressureCurve.subtitle,
                                            fontSize = 9.sp,
                                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                        )
                                    }

                                    // Звуковой отклик
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isDark) Color(0xFF11141B) else Color.White)
                                            .border(1.dp, if (isDark) Color(0xFF262C38) else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
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
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.VolumeUp,
                                                        contentDescription = null,
                                                        tint = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Text(
                                                        text = "Звуковой отклик при входе ученика",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
                                                    )
                                                }
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
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Text("Тест", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    // 3 последних действия
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isDark) Color(0xFF11141B) else Color.White)
                                            .border(1.dp, if (isDark) Color(0xFF262C38) else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                            .padding(9.dp),
                                        verticalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.History,
                                                    contentDescription = null,
                                                    tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF1D4ED8),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = "3 ПОСЛЕДНИХ ДЕЙСТВИЯ С ПЛАНШЕТА",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = if (isDark) Color(0xFF38BDF8) else Color(0xFF1D4ED8)
                                                )
                                            }
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
                                                text = "Ожидание действий: проведите пером по экрану телефона для отображения здесь.",
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
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Edit,
                                                            contentDescription = null,
                                                            tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB),
                                                            modifier = Modifier.size(11.dp)
                                                        )
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
                                }
                            }

                            // Модальный диалог с QR-кодом
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
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.QrCode,
                                                    contentDescription = null,
                                                    tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = "Сканируйте QR-код",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
                                                )
                                            }
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

                    // СЕКЦИЯ 4: БЫСТРЫЙ ПРОФИЛЬ ДЛЯ ВАШИХ ЗАДАЧ
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "БЫСТРЫЙ ПРОФИЛЬ ДЛЯ ВАШИХ ЗАДАЧ",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = sectionHeaderColor
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            UserRole.entries.forEach { r ->
                                val isSelected = r == role
                                val (roleIcon, roleTitle) = when (r) {
                                    UserRole.TUTOR_STEM -> Icons.Default.School to "Репетитор"
                                    UserRole.TEAM_COLLAB -> Icons.Default.Groups to "Команда"
                                    UserRole.SKETCH_NOTE -> Icons.Default.Brush to "Скетчи"
                                    UserRole.ALL_TOOLS -> Icons.Default.Apps to "Все"
                                }
                                SegmentOptionButton(
                                    selected = isSelected,
                                    title = roleTitle,
                                    icon = roleIcon,
                                    isDark = isDark,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        role = r
                                        tools = r.tools
                                    }
                                )
                            }
                        }
                    }

                    // СЕКЦИЯ 5: ВИДИМОСТЬ ИНСТРУМЕНТОВ НА ПАНЕЛИ
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ВИДИМОСТЬ ИНСТРУМЕНТОВ НА ПАНЕЛИ",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = sectionHeaderColor
                            )
                            Text(
                                text = "Включено: ${tools.size}/${CanvasTool.entries.size}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0xFF0F1116) else Color(0xFFF8FAFC))
                                .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
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
                                    val (toolIcon, toolShortcut) = when (tool) {
                                        CanvasTool.PEN -> Icons.Default.Edit to "Q / P · Рисование стилусом и пером"
                                        CanvasTool.ERASER -> Icons.Default.AutoFixNormal to "E · Стирание штрихов"
                                        CanvasTool.STICKY_NOTE -> Icons.Default.Description to "S · Заметки и списки задач To-Do"
                                        CanvasTool.SHAPE -> Icons.Default.Category to "R · Прямоугольники, круги, стрелки"
                                        CanvasTool.LATEX -> Icons.Default.Functions to "X · Математические уравнения и формулы"
                                        CanvasTool.PHYSICS_STENCIL -> Icons.Default.Science to "B · Физический конструктор и лаборатория"
                                        CanvasTool.CONNECTOR -> Icons.Default.Timeline to "C · Умные стрелки и связки блоков"
                                        CanvasTool.MILESTONE -> Icons.Default.Flag to "M · Вехи и этапы дорожной карты"
                                        CanvasTool.KANBAN -> Icons.Default.ViewColumn to "K · Доски задач To Do / Done"
                                        CanvasTool.FUNCTION_GRAPH -> Icons.Default.ShowChart to "G · Графики математических функций"
                                        CanvasTool.TEXT -> Icons.Default.TextFields to "T · Текстовые блоки"
                                        CanvasTool.FRAME -> Icons.Default.Crop to "F · Слайды и рамки презентации"
                                        CanvasTool.LASER_POINTER -> Icons.Default.GpsFixed to "L · Лазерная указка для объяснений"
                                        else -> Icons.Default.Category to ""
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isEnabled) (if (isDark) Color(0xFF19202E) else Color.White)
                                                else (if (isDark) Color(0xFF13151B) else Color(0xFFF1F5F9))
                                            )
                                            .border(
                                                1.dp,
                                                if (isEnabled) (if (isDark) Color(0xFF2E3D59) else Color(0xFFBFDBFE))
                                                else (if (isDark) Color(0xFF1E222A) else Color(0xFFE2E8F0)),
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
                                                    .background(
                                                        if (isEnabled) (if (isDark) Color(0xFF202A3C) else Color(0xFFDBEAFE))
                                                        else (if (isDark) Color(0xFF1A1E26) else Color(0xFFE2E8F0))
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = toolIcon,
                                                    contentDescription = null,
                                                    tint = if (isEnabled) (if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)) else (if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)),
                                                    modifier = Modifier.size(15.dp)
                                                )
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
                        .background(cardHeaderBg)
                        .border(1.dp, cardBorder)
                        .padding(horizontal = 22.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { collapseAndDismiss() },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF3F3F46) else Color(0xFFCBD5E1)),
                        modifier = Modifier.bouncyClickable(hoverScale = 1.05f, pressedScale = 0.94f) { collapseAndDismiss() }
                    ) {
                        Text("Закрыть", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            collapseAndDismiss {
                                onSaveSettings(theme, role, tools, selectedPosition)
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2563EB),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.bouncyClickable(hoverScale = 1.05f, pressedScale = 0.94f) {
                            collapseAndDismiss {
                                onSaveSettings(theme, role, tools, selectedPosition)
                            }
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Text("Применить настройки", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Интерактивная кнопка выбора с плавной анимацией фона, границы и иконки.
 */
@Composable
private fun SegmentOptionButton(
    selected: Boolean,
    title: String,
    icon: ImageVector,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "segmentScale"
    )

    val targetBg = when {
        selected -> if (isDark) Color(0xFF202636) else Color(0xFFEFF6FF)
        isHovered -> if (isDark) Color(0xFF1E222A) else Color(0xFFF1F5F9)
        else -> if (isDark) Color(0xFF16181F) else Color(0xFFF8FAFC)
    }

    val targetBorder = when {
        selected -> if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)
        isHovered -> if (isDark) Color(0xFF333844) else Color(0xFFCBD5E1)
        else -> if (isDark) Color(0xFF242730) else Color(0xFFE2E8F0)
    }

    val targetContentColor = when {
        selected -> if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)
        else -> if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
    }

    val animatedBg by animateColorAsState(targetBg, tween(150), label = "segmentBg")
    val animatedBorder by animateColorAsState(targetBorder, tween(150), label = "segmentBorder")
    val animatedColor by animateColorAsState(targetContentColor, tween(150), label = "segmentColor")

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(8.dp))
            .background(animatedBg)
            .border(1.5.dp, animatedBorder, RoundedCornerShape(8.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = animatedColor,
                modifier = Modifier.size(17.dp)
            )
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = animatedColor,
                maxLines = 1
            )
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
