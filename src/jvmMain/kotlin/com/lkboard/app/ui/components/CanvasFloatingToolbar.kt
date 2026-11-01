package com.lkboard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.lkboard.app.ui.animation.bouncyClickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lkboard.app.model.BoardElement
import com.lkboard.app.model.CanvasTransform
import com.lkboard.app.model.PhysicsStencilDefaults
import com.lkboard.app.model.PhysicsStencilType
import com.lkboard.app.ui.theme.LocalAppTheme
import kotlin.math.roundToInt

/**
 * Контекстная плавающая панель быстрого управления размером, начертанием и стилем
 * для выделенного текстового блока (BoardElement.Text) на холсте.
 * Изменяет размер и стиль "прям на глазах" без открытия модальных окон.
 */
@Composable
fun TextFloatingQuickBar(
    element: BoardElement.Text,
    transform: CanvasTransform,
    onUpdateProperties: (fontSizeDelta: Float?, targetFontSize: Float?, isBold: Boolean?, isItalic: Boolean?, colorHex: Long?) -> Unit,
    onStartEditing: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalAppTheme.current.isDark

    val screenCenterX = (element.x + element.width / 2f) * transform.scale + transform.panX
    val screenTopY = element.y * transform.scale + transform.panY

    val barWidth = 380
    val targetX = (screenCenterX - barWidth / 2f).coerceAtLeast(16f).roundToInt()
    val targetY = if (screenTopY - 50f >= 55f) {
        (screenTopY - 48f).roundToInt()
    } else {
        ((element.y + element.height) * transform.scale + transform.panY + 12f).roundToInt()
    }

    val palette = listOf(
        if (isDark) 0xFFF4F4F5L else 0xFF0F172AL,
        0xFF2563EBL, // Blue
        0xFF16A34AL, // Green
        0xFFDC2626L, // Red
        0xFF9333EAL, // Purple
        0xFFD97706L  // Amber
    )

    var isColorDialogOpen by remember { mutableStateOf(false) }

    if (isColorDialogOpen) {
        HexColorPickerDialog(
            title = "Цвет текста (HEX)",
            initialColorHex = element.colorHex,
            showAlpha = false,
            onDismiss = { isColorDialogOpen = false },
            onColorSelected = { onUpdateProperties(null, null, null, null, it) }
        )
    }

    Box(
        modifier = modifier
            .offset { IntOffset(targetX, targetY) }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
    ) {
        Row(
            modifier = Modifier
                .shadow(
                    elevation = 14.dp,
                    shape = RoundedCornerShape(14.dp),
                    ambientColor = Color.Black.copy(alpha = 0.05f),
                    spotColor = Color.Black.copy(alpha = 0.12f)
                )
                .clip(RoundedCornerShape(14.dp))
                .background(if (isDark) Color(0xFF181B20).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.95f))
                .border(1.dp, if (isDark) Color(0xFF2E333D) else Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // A-
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (isDark) Color(0xFF242830) else Color(0xFFF1F5F9))
                    .bouncyClickable(hoverScale = 1.10f, pressedScale = 0.90f) { onUpdateProperties(-2f, null, null, null, null) },
                contentAlignment = Alignment.Center
            ) {
                Text("A-", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B))
            }

            // A+
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (isDark) Color(0xFF242830) else Color(0xFFF1F5F9))
                    .bouncyClickable(hoverScale = 1.10f, pressedScale = 0.90f) { onUpdateProperties(+2f, null, null, null, null) },
                contentAlignment = Alignment.Center
            ) {
                Text("A+", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B))
            }

            // Чипы быстрых размеров
            listOf(14f, 18f, 24f, 32f, 48f).forEach { sz ->
                val isSelected = element.fontSize.toInt() == sz.toInt()
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (isSelected) Color(0xFF2563EB) else (if (isDark) Color(0xFF242830) else Color(0xFFF1F5F9)))
                        .border(1.dp, if (isSelected) Color(0xFF2563EB) else Color.Transparent, RoundedCornerShape(5.dp))
                        .bouncyClickable(hoverScale = 1.10f, pressedScale = 0.90f) { onUpdateProperties(null, sz, null, null, null) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${sz.toInt()}",
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155))
                    )
                }
            }

            Box(modifier = Modifier.width(1.dp).height(16.dp).background(if (isDark) Color(0xFF2E333D) else Color(0xFFE2E8F0)))

            // B (Bold)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (element.isBold) (if (isDark) Color(0xFF1E2638) else Color(0xFFDBEAFE)) else Color.Transparent)
                    .bouncyClickable(hoverScale = 1.10f, pressedScale = 0.90f) { onUpdateProperties(null, null, !element.isBold, null, null) },
                contentAlignment = Alignment.Center
            ) {
                Text("B", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = if (element.isBold) (if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8)) else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)))
            }

            // I (Italic)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (element.isItalic) (if (isDark) Color(0xFF1E2638) else Color(0xFFDBEAFE)) else Color.Transparent)
                    .bouncyClickable(hoverScale = 1.10f, pressedScale = 0.90f) { onUpdateProperties(null, null, null, !element.isItalic, null) },
                contentAlignment = Alignment.Center
            ) {
                Text("I", fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (element.isItalic) (if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8)) else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)))
            }

            Box(modifier = Modifier.width(1.dp).height(16.dp).background(if (isDark) Color(0xFF2E333D) else Color(0xFFE2E8F0)))

            // Палитра цветов
            palette.forEach { cHex ->
                val isSelected = element.colorHex == cHex
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(cHex))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color(0xFF2563EB) else (if (isDark) Color(0xFF3F4554) else Color(0xFFCBD5E1)),
                            shape = CircleShape
                        )
                        .bouncyClickable(hoverScale = 1.15f, pressedScale = 0.88f) { onUpdateProperties(null, null, null, null, cHex) }
                )
            }

            // Кастомный цвет, если выбран не из палитры
            val isPreset = palette.contains(element.colorHex)
            if (!isPreset) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(element.colorHex))
                        .border(2.dp, Color(0xFF2563EB), CircleShape)
                        .bouncyClickable(hoverScale = 1.15f, pressedScale = 0.88f) { isColorDialogOpen = true }
                )
            }

            // Кнопка палитры HEX
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF242830) else Color(0xFFF1F5F9))
                    .border(1.dp, if (isDark) Color(0xFF3F4554) else Color(0xFFCBD5E1), CircleShape)
                    .bouncyClickable(hoverScale = 1.12f, pressedScale = 0.90f) { isColorDialogOpen = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Выбрать HEX",
                    tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    modifier = Modifier.size(11.dp)
                )
            }

            Box(modifier = Modifier.width(1.dp).height(16.dp).background(if (isDark) Color(0xFF2E333D) else Color(0xFFE2E8F0)))

            // Редактировать текст
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (isDark) Color(0xFF242830) else Color(0xFFF1F5F9))
                    .bouncyClickable(hoverScale = 1.10f, pressedScale = 0.90f) { onStartEditing() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Редактировать текст",
                    tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                    modifier = Modifier.size(14.dp)
                )
            }

            // Удалить
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (isDark) Color(0xFF242830) else Color(0xFFF1F5F9))
                    .bouncyClickable(hoverScale = 1.10f, pressedScale = 0.90f) { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Удалить",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

/**
 * Контекстная плавающая панель быстрого масштабирования и настройки
 * для выделенной математической формулы (BoardElement.LatexFormula) на холсте.
 * Дает возможность мгновенно увеличить или уменьшить формулу кнопками A- / A+ и чипами.
 */
@Composable
fun LatexFloatingQuickBar(
    element: BoardElement.LatexFormula,
    transform: CanvasTransform,
    onUpdateProperties: (fontSizeDelta: Float?, targetFontSize: Float?, colorHex: Long?, toggleBackground: Boolean) -> Unit,
    onEditFormula: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalAppTheme.current.isDark

    val screenCenterX = (element.x + element.width / 2f) * transform.scale + transform.panX
    val screenTopY = element.y * transform.scale + transform.panY

    val barWidth = 420
    val targetX = (screenCenterX - barWidth / 2f).coerceAtLeast(16f).roundToInt()
    val targetY = if (screenTopY - 50f >= 55f) {
        (screenTopY - 48f).roundToInt()
    } else {
        ((element.y + element.height) * transform.scale + transform.panY + 12f).roundToInt()
    }

    val palette = listOf(
        if (isDark) 0xFFF4F4F5L else 0xFF0F172AL,
        0xFF2563EBL, // Royal Blue
        0xFF16A34AL, // Green
        0xFFDC2626L, // Red
        0xFF9333EAL, // Purple
        0xFFEA580CL  // Orange
    )

    var isColorDialogOpen by remember { mutableStateOf(false) }

    if (isColorDialogOpen) {
        HexColorPickerDialog(
            title = "Цвет формулы LaTeX (HEX)",
            initialColorHex = element.colorHex,
            showAlpha = false,
            onDismiss = { isColorDialogOpen = false },
            onColorSelected = { onUpdateProperties(null, null, it, false) }
        )
    }

    Box(
        modifier = modifier
            .offset { IntOffset(targetX, targetY) }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
    ) {
        Row(
            modifier = Modifier
                .shadow(
                    elevation = 14.dp,
                    shape = RoundedCornerShape(14.dp),
                    ambientColor = Color.Black.copy(alpha = 0.05f),
                    spotColor = Color.Black.copy(alpha = 0.12f)
                )
                .clip(RoundedCornerShape(14.dp))
                .background(if (isDark) Color(0xFF181B20).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.95f))
                .border(1.dp, if (isDark) Color(0xFF2E333D) else Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Метка LaTeX
            Text(
                text = "∑",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2563EB),
                modifier = Modifier.padding(horizontal = 2.dp)
            )

            // A-
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (isDark) Color(0xFF242830) else Color(0xFFF1F5F9))
                    .bouncyClickable(hoverScale = 1.10f, pressedScale = 0.90f) { onUpdateProperties(-4f, null, null, false) },
                contentAlignment = Alignment.Center
            ) {
                Text("A-", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B))
            }

            // A+
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (isDark) Color(0xFF242830) else Color(0xFFF1F5F9))
                    .bouncyClickable(hoverScale = 1.10f, pressedScale = 0.90f) { onUpdateProperties(+4f, null, null, false) },
                contentAlignment = Alignment.Center
            ) {
                Text("A+", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B))
            }

            // Чипы масштабирования формулы
            listOf(18f to "S", 24f to "M", 32f to "L", 48f to "XL", 64f to "2XL").forEach { (sz, label) ->
                val isSelected = element.fontSize.toInt() == sz.toInt()
                Box(
                    modifier = Modifier
                        .height(22.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (isSelected) Color(0xFF2563EB) else (if (isDark) Color(0xFF242830) else Color(0xFFF1F5F9)))
                        .border(1.dp, if (isSelected) Color(0xFF2563EB) else Color.Transparent, RoundedCornerShape(5.dp))
                        .bouncyClickable(hoverScale = 1.10f, pressedScale = 0.90f) { onUpdateProperties(null, sz, null, false) }
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${sz.toInt()} $label",
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155))
                    )
                }
            }

            Box(modifier = Modifier.width(1.dp).height(16.dp).background(if (isDark) Color(0xFF2E333D) else Color(0xFFE2E8F0)))

            // Переключатель рамки / фона формулы
            Box(
                modifier = Modifier
                    .height(22.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (element.hasBackgroundCard) (if (isDark) Color(0xFF1E2638) else Color(0xFFDBEAFE)) else Color.Transparent)
                    .border(1.dp, if (element.hasBackgroundCard) (if (isDark) Color(0xFF2563EB) else Color(0xFF93C5FD)) else Color.Transparent, RoundedCornerShape(5.dp))
                    .bouncyClickable(hoverScale = 1.08f, pressedScale = 0.92f) { onUpdateProperties(null, null, null, true) }
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Рамка",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (element.hasBackgroundCard) (if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8)) else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155))
                )
            }

            Box(modifier = Modifier.width(1.dp).height(16.dp).background(if (isDark) Color(0xFF2E333D) else Color(0xFFE2E8F0)))

            // Палитра цветов
            palette.forEach { cHex ->
                val isSelected = element.colorHex == cHex
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(cHex))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color(0xFF2563EB) else (if (isDark) Color(0xFF3F4554) else Color(0xFFCBD5E1)),
                            shape = CircleShape
                        )
                        .bouncyClickable(hoverScale = 1.15f, pressedScale = 0.88f) { onUpdateProperties(null, null, cHex, false) }
                )
            }

            // Кастомный выбранный цвет
            val isPreset = palette.contains(element.colorHex)
            if (!isPreset) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(element.colorHex))
                        .border(2.dp, Color(0xFF2563EB), CircleShape)
                        .bouncyClickable(hoverScale = 1.15f, pressedScale = 0.88f) { isColorDialogOpen = true }
                )
            }

            // Кнопка палитры HEX
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF242830) else Color(0xFFF1F5F9))
                    .border(1.dp, if (isDark) Color(0xFF3F4554) else Color(0xFFCBD5E1), CircleShape)
                    .bouncyClickable(hoverScale = 1.12f, pressedScale = 0.90f) { isColorDialogOpen = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Выбрать HEX",
                    tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    modifier = Modifier.size(11.dp)
                )
            }

            Box(modifier = Modifier.width(1.dp).height(16.dp).background(if (isDark) Color(0xFF2E333D) else Color(0xFFE2E8F0)))

            // Кнопка изменить формулу
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (isDark) Color(0xFF242830) else Color(0xFFF1F5F9))
                    .bouncyClickable(hoverScale = 1.10f, pressedScale = 0.90f) { onEditFormula() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Изменить формулу LaTeX",
                    tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                    modifier = Modifier.size(14.dp)
                )
            }

            // Удалить
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (isDark) Color(0xFF242830) else Color(0xFFF1F5F9))
                    .bouncyClickable(hoverScale = 1.10f, pressedScale = 0.90f) { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Удалить",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

/**
 * Плавающая панель быстрого управления заданием ФИПИ на холсте.
 * Позволяет учителю в 1 клик показать или скрыть ответ/решение.
 */
@Composable
fun FipiTaskFloatingQuickBar(
    element: BoardElement.FipiTask,
    transform: CanvasTransform,
    onToggleAnswer: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = com.lkboard.app.ui.theme.LocalAppTheme.current.isDark
    val screenCenterX = (element.x + element.width / 2f) * transform.scale + transform.panX
    val screenTopY = element.y * transform.scale + transform.panY

    val barWidth = 320
    val targetX = (screenCenterX - barWidth / 2f).coerceAtLeast(16f).roundToInt()
    val targetY = if (screenTopY - 50f >= 55f) {
        (screenTopY - 48f).roundToInt()
    } else {
        ((element.y + element.height) * transform.scale + transform.panY + 12f).roundToInt()
    }

    Box(
        modifier = modifier
            .offset { IntOffset(targetX, targetY) }
            .shadow(12.dp, RoundedCornerShape(12.dp), ambientColor = Color.Black.copy(alpha = 0.08f), spotColor = Color.Black.copy(alpha = 0.20f))
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) Color(0xFF1E222A).copy(alpha = 0.96f) else Color.White.copy(alpha = 0.96f))
            .border(1.dp, if (isDark) Color(0xFF333844) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isOpen = element.isAnswerRevealed
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (isOpen) Color(0xFF16A34A).copy(alpha = 0.16f) else Color(0xFF2563EB).copy(alpha = 0.14f))
                    .border(1.dp, if (isOpen) Color(0xFF16A34A) else Color(0xFF2563EB), RoundedCornerShape(7.dp))
                    .bouncyClickable(hoverScale = 1.05f, pressedScale = 0.92f) { onToggleAnswer() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isOpen) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = if (isOpen) Color(0xFF16A34A) else Color(0xFF2563EB),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = if (isOpen) "Скрыть ответ для ученика" else "Показать ответ и критерии",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOpen) Color(0xFF16A34A) else Color(0xFF2563EB)
                    )
                }
            }

            Box(modifier = Modifier.width(1.dp).height(20.dp).background(if (isDark) Color(0xFF333844) else Color(0xFFE2E8F0)))

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isDark) Color(0xFF262A34) else Color(0xFFF1F5F9))
                    .bouncyClickable(hoverScale = 1.10f, pressedScale = 0.90f) { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Удалить",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Контекстная плавающая HUD-панель живого управления физическим стенсилом прямо на холсте.
 * Располагается рядом с выбранным блоком и позволяет в 60 FPS динамически изменять параметры
 * и наблюдать физическую реакцию и расчеты модели в реальном времени.
 */
@Composable
fun PhysicsStencilFloatingQuickBar(
    element: BoardElement.PhysicsStencil,
    transform: CanvasTransform,
    onUpdateParams: (Map<String, Float>) -> Unit,
    onUpdateStyle: (colorHex: Long?, strokeWidth: Float?) -> Unit,
    onResetParams: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalAppTheme.current.isDark
    val currentParams = element.effectiveParams()

    val screenRightX = (element.x + element.width) * transform.scale + transform.panX + 16f
    val screenLeftX = element.x * transform.scale + transform.panX
    val screenTopY = element.y * transform.scale + transform.panY

    val hudWidth = 320
    val targetX = if (screenRightX + hudWidth > 1360f && screenLeftX - hudWidth - 16f > 16f) {
        (screenLeftX - hudWidth - 16f).roundToInt()
    } else {
        screenRightX.coerceAtLeast(16f).roundToInt()
    }
    val targetY = screenTopY.coerceIn(55f, 920f).roundToInt()

    var isColorDialogOpen by remember { mutableStateOf(false) }

    if (isColorDialogOpen) {
        HexColorPickerDialog(
            title = "Цвет стенсила (HEX)",
            initialColorHex = element.strokeColorHex,
            showAlpha = false,
            onDismiss = { isColorDialogOpen = false },
            onColorSelected = { onUpdateStyle(it, null) }
        )
    }

    Box(
        modifier = modifier
            .offset { IntOffset(targetX, targetY) }
            .width(hudWidth.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.22f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) Color(0xFF161920).copy(alpha = 0.96f) else Color.White.copy(alpha = 0.96f))
            .border(1.dp, if (isDark) Color(0xFF2C323E) else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .padding(14.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Заголовок карточки
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color(0xFF2563EB).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = element.stencilType.displayName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                        )
                        Text(
                            text = element.stencilType.category,
                            fontSize = 10.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Кнопка сброса параметров к дефолтным
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isDark) Color(0xFF242832) else Color(0xFFF1F5F9))
                            .bouncyClickable(hoverScale = 1.10f, pressedScale = 0.90f) { onResetParams() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Сбросить параметры",
                            tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Кнопка удаления
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isDark) Color(0xFF242832) else Color(0xFFF1F5F9))
                            .bouncyClickable(hoverScale = 1.10f, pressedScale = 0.90f) { onDelete() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Удалить",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Быстрый выбор цвета и толщины линии
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        0xFF2563EBL, // Blue
                        0xFF0284C7L, // Cyan
                        0xFF16A34AL, // Green
                        0xFFD97706L, // Amber
                        0xFFDC2626L, // Red
                        0xFF9333EAL  // Purple
                    ).forEach { colorHex ->
                        val isSelected = element.strokeColorHex == colorHex
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color(colorHex))
                                .border(if (isSelected) 2.dp else 0.dp, if (isDark) Color.White else Color(0xFF0F172A), CircleShape)
                                .bouncyClickable(hoverScale = 1.15f, pressedScale = 0.90f) {
                                    onUpdateStyle(colorHex, null)
                                }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF2A2F3D) else Color(0xFFE2E8F0))
                            .bouncyClickable(hoverScale = 1.15f, pressedScale = 0.90f) {
                                isColorDialogOpen = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "HEX",
                            tint = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }

                // Толщина линии: 1.5, 2.5, 4.0
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(1.5f, 2.5f, 4.0f).forEach { th ->
                        val isSelected = kotlin.math.abs(element.strokeWidth - th) < 0.2f
                        Box(
                            modifier = Modifier
                                .height(18.dp)
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) Color(0xFF2563EB) else (if (isDark) Color(0xFF242832) else Color(0xFFF1F5F9)))
                                .bouncyClickable(hoverScale = 1.08f, pressedScale = 0.92f) {
                                    onUpdateStyle(null, th)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${th}px",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(if (isDark) Color(0xFF262A34) else Color(0xFFF1F5F9)))

            // Секция живых слайдеров в зависимости от типа
            when (element.stencilType) {
                PhysicsStencilType.PULLEY_BLOCK -> {
                    val m1 = currentParams["m1"] ?: 3.0f
                    val m2 = currentParams["m2"] ?: 1.0f
                    val a = if (m1 + m2 > 0f) kotlin.math.abs(m1 - m2) * 9.8f / (m1 + m2) else 0f
                    val t = if (m1 + m2 > 0f) (2f * m1 * m2 * 9.8f) / (m1 + m2) else 0f

                    PhysicsQuickSlider("Масса m₁", m1, "кг", 0.5f..10.0f) { onUpdateParams(currentParams + ("m1" to it)) }
                    PhysicsQuickSlider("Масса m₂", m2, "кг", 0.5f..10.0f) { onUpdateParams(currentParams + ("m2" to it)) }

                    PhysicsLiveStatCard(
                        isDark = isDark,
                        stats = listOf(
                            "Ускорение a" to "${"%.2f".format(a)} м/с²",
                            "Натяжение нити T" to "${"%.1f".format(t)} Н"
                        )
                    )
                }
                PhysicsStencilType.INCLINED_PLANE -> {
                    val angle = currentParams["angle"] ?: 30f
                    val m = currentParams["m"] ?: 2.0f
                    val mu = currentParams["mu"] ?: 0.15f
                    val angleRad = (angle * kotlin.math.PI / 180.0).toFloat()
                    val g = 9.8f
                    val fNet = maxOf(0f, m * g * kotlin.math.sin(angleRad) - mu * m * g * kotlin.math.cos(angleRad))
                    val a = fNet / m
                    val n = m * g * kotlin.math.cos(angleRad)

                    PhysicsQuickSlider("Угол наклона α", angle, "°", 10f..75f, formatStr = "%.0f") { onUpdateParams(currentParams + ("angle" to it)) }
                    PhysicsQuickSlider("Масса бруска m", m, "кг", 0.5f..10.0f) { onUpdateParams(currentParams + ("m" to it)) }
                    PhysicsQuickSlider("Коэфф. трения μ", mu, "", 0.00f..0.80f, formatStr = "%.2f") { onUpdateParams(currentParams + ("mu" to it)) }

                    PhysicsLiveStatCard(
                        isDark = isDark,
                        stats = listOf(
                            "Ускорение a" to "${"%.2f".format(a)} м/с²",
                            "Сила реакции N" to "${"%.1f".format(n)} Н"
                        )
                    )
                }
                PhysicsStencilType.SPRING -> {
                    val k = currentParams["k"] ?: 100f
                    val m = currentParams["m"] ?: 2.0f
                    val deltaX = (m * 9.8f / maxOf(10f, k)) * 100f
                    val periodT = 2f * kotlin.math.PI.toFloat() * kotlin.math.sqrt(m / maxOf(1f, k))

                    PhysicsQuickSlider("Жесткость k", k, "Н/м", 20f..500f, formatStr = "%.0f") { onUpdateParams(currentParams + ("k" to it)) }
                    PhysicsQuickSlider("Масса груза m", m, "кг", 0.2f..10.0f) { onUpdateParams(currentParams + ("m" to it)) }

                    PhysicsLiveStatCard(
                        isDark = isDark,
                        stats = listOf(
                            "Растяжение Δx" to "${"%.1f".format(deltaX)} см",
                            "Период T" to "${"%.2f".format(periodT)} с"
                        )
                    )
                }
                PhysicsStencilType.CIRCUIT_RESISTOR -> {
                    val r = currentParams["R"] ?: 6.0f
                    val u = currentParams["U"] ?: 12.0f
                    val i = if (r > 0f) u / r else 0f
                    val p = u * i

                    PhysicsQuickSlider("Сопротивление R", r, "Ом", 0.5f..50.0f) { onUpdateParams(currentParams + ("R" to it)) }
                    PhysicsQuickSlider("Напряжение U", u, "В", 1.0f..48.0f) { onUpdateParams(currentParams + ("U" to it)) }

                    PhysicsLiveStatCard(
                        isDark = isDark,
                        stats = listOf(
                            "Сила тока I" to "${"%.2f".format(i)} А",
                            "Мощность P" to "${"%.1f".format(p)} Вт"
                        )
                    )
                }
                PhysicsStencilType.CIRCUIT_RHEOSTAT -> {
                    val rCur = currentParams["Rcur"] ?: 20.0f
                    val rMax = maxOf(1f, currentParams["Rmax"] ?: 50.0f)
                    val u = currentParams["U"] ?: 12.0f
                    val i = if (rCur > 0f) u / rCur else 0f

                    PhysicsQuickSlider("Ползунок R_тек", rCur, "Ом", 1.0f..rMax) { onUpdateParams(currentParams + ("Rcur" to it)) }
                    PhysicsQuickSlider("Максимум R_макс", rMax, "Ом", 10.0f..100.0f, formatStr = "%.0f") { onUpdateParams(currentParams + ("Rmax" to it)) }
                    PhysicsQuickSlider("Напряжение U", u, "В", 1.0f..48.0f) { onUpdateParams(currentParams + ("U" to it)) }

                    PhysicsLiveStatCard(
                        isDark = isDark,
                        stats = listOf(
                            "Сила тока I" to "${"%.2f".format(i)} А",
                            "Доля сопротивления" to "${(rCur / rMax * 100f).toInt()}%"
                        )
                    )
                }
                PhysicsStencilType.CIRCUIT_AMMETER -> {
                    val i = currentParams["I"] ?: 2.4f
                    val iMax = currentParams["Imax"] ?: 5.0f
                    PhysicsQuickSlider("Показание I", i, "А", 0.0f..iMax, formatStr = "%.2f") { onUpdateParams(currentParams + ("I" to it)) }
                    PhysicsQuickSlider("Предел шкалы", iMax, "А", 1.0f..20.0f, formatStr = "%.1f") { onUpdateParams(currentParams + ("Imax" to it)) }
                }
                PhysicsStencilType.CIRCUIT_VOLTMETER -> {
                    val u = currentParams["U"] ?: 9.6f
                    val uMax = currentParams["Umax"] ?: 15.0f
                    PhysicsQuickSlider("Показание U", u, "В", 0.0f..uMax, formatStr = "%.1f") { onUpdateParams(currentParams + ("U" to it)) }
                    PhysicsQuickSlider("Предел шкалы", uMax, "В", 5.0f..50.0f, formatStr = "%.1f") { onUpdateParams(currentParams + ("Umax" to it)) }
                }
                PhysicsStencilType.CIRCUIT_BATTERY -> {
                    val u = currentParams["U"] ?: 12.0f
                    PhysicsQuickSlider("ЭДС источника U", u, "В", 1.5f..48.0f) { onUpdateParams(currentParams + ("U" to it)) }
                }
                PhysicsStencilType.CIRCUIT_CAPACITOR -> {
                    val c = currentParams["C"] ?: 47.0f
                    val u = currentParams["U"] ?: 12.0f
                    val wEnergy = 0.5f * (c * 1e-6f) * u * u * 1000f

                    PhysicsQuickSlider("Емкость C", c, "мкФ", 1.0f..200.0f, formatStr = "%.0f") { onUpdateParams(currentParams + ("C" to it)) }
                    PhysicsQuickSlider("Напряжение U", u, "В", 1.0f..48.0f) { onUpdateParams(currentParams + ("U" to it)) }

                    PhysicsLiveStatCard(
                        isDark = isDark,
                        stats = listOf(
                            "Энергия W" to "${"%.2f".format(wEnergy)} мДж"
                        )
                    )
                }
                PhysicsStencilType.CIRCUIT_INDUCTOR -> {
                    val l = currentParams["L"] ?: 10.0f
                    PhysicsQuickSlider("Индуктивность L", l, "мГн", 1.0f..100.0f, formatStr = "%.0f") { onUpdateParams(currentParams + ("L" to it)) }
                }
                PhysicsStencilType.CIRCUIT_LAMP -> {
                    val r = currentParams["R"] ?: 6.0f
                    val u = currentParams["U"] ?: 12.0f
                    val i = if (r > 0f) u / r else 0f
                    val p = u * i

                    PhysicsQuickSlider("Сопротивление спирали", r, "Ом", 1.0f..30.0f) { onUpdateParams(currentParams + ("R" to it)) }
                    PhysicsQuickSlider("Напряжение U", u, "В", 1.0f..24.0f) { onUpdateParams(currentParams + ("U" to it)) }

                    PhysicsLiveStatCard(
                        isDark = isDark,
                        stats = listOf(
                            "Мощность накала" to "${"%.1f".format(p)} Вт",
                            "Ток нити" to "${"%.2f".format(i)} А"
                        )
                    )
                }
                PhysicsStencilType.THERMO_CYLINDER -> {
                    val v = currentParams["V"] ?: 5.0f
                    val p = currentParams["p"] ?: 100f
                    val nu = currentParams["nu"] ?: 1.0f
                    val tK = if (nu > 0f) (p * 1000f * (v * 1e-3f)) / (nu * 8.314f) else 0f

                    PhysicsQuickSlider("Объем V (положение поршня)", v, "л", 1.0f..12.0f) { onUpdateParams(currentParams + ("V" to it)) }
                    PhysicsQuickSlider("Давление газа p", p, "кПа", 20f..400f, formatStr = "%.0f") { onUpdateParams(currentParams + ("p" to it)) }
                    PhysicsQuickSlider("Количество моль ν", nu, "моль", 0.5f..5.0f) { onUpdateParams(currentParams + ("nu" to it)) }

                    PhysicsLiveStatCard(
                        isDark = isDark,
                        stats = listOf(
                            "Температура T" to "${tK.toInt()} K (${(tK - 273.15f).toInt()} °C)"
                        )
                    )
                }
                PhysicsStencilType.THERMO_PV_CYCLE -> {
                    val p1 = currentParams["p1"] ?: 100f
                    val p2 = currentParams["p2"] ?: 300f
                    val v1 = currentParams["V1"] ?: 2.0f
                    val v2 = currentParams["V2"] ?: 6.0f
                    val work = kotlin.math.abs(p2 - p1) * 1000f * (kotlin.math.abs(v2 - v1) * 1e-3f)

                    PhysicsQuickSlider("Давление p₁", p1, "кПа", 50f..300f, formatStr = "%.0f") { onUpdateParams(currentParams + ("p1" to it)) }
                    PhysicsQuickSlider("Давление p₂", p2, "кПа", 100f..500f, formatStr = "%.0f") { onUpdateParams(currentParams + ("p2" to it)) }
                    PhysicsQuickSlider("Объем V₁", v1, "л", 1.0f..5.0f) { onUpdateParams(currentParams + ("V1" to it)) }
                    PhysicsQuickSlider("Объем V₂", v2, "л", 3.0f..10.0f) { onUpdateParams(currentParams + ("V2" to it)) }

                    PhysicsLiveStatCard(
                        isDark = isDark,
                        stats = listOf(
                            "Работа цикла A" to "${"%.1f".format(work)} Дж"
                        )
                    )
                }
                PhysicsStencilType.LEVER_BALANCE -> {
                    val d1 = currentParams["d1"] ?: 40f
                    val d2 = currentParams["d2"] ?: 20f
                    val m1 = currentParams["m1"] ?: 2.0f
                    val m2 = if (d2 > 0) m1 * d1 / d2 else 0f

                    PhysicsQuickSlider("Плечо d₁", d1, "см", 10f..70f, formatStr = "%.0f") { onUpdateParams(currentParams + ("d1" to it)) }
                    PhysicsQuickSlider("Плечо d₂", d2, "см", 10f..70f, formatStr = "%.0f") { onUpdateParams(currentParams + ("d2" to it)) }
                    PhysicsQuickSlider("Масса m₁", m1, "кг", 0.5f..10.0f) { onUpdateParams(currentParams + ("m1" to it)) }

                    PhysicsLiveStatCard(
                        isDark = isDark,
                        stats = listOf(
                            "Баланс: масса m₂" to "${"%.1f".format(m2)} кг"
                        )
                    )
                }
                PhysicsStencilType.MATHEMATICAL_PENDULUM -> {
                    val l = currentParams["L"] ?: 1.0f
                    val g = currentParams["g"] ?: 9.8f
                    val t = if (g > 0f && l > 0f) 2f * kotlin.math.PI.toFloat() * kotlin.math.sqrt(l / g) else 0f

                    PhysicsQuickSlider("Длина нити L", l, "м", 0.2f..3.0f, formatStr = "%.2f") { onUpdateParams(currentParams + ("L" to it)) }
                    PhysicsQuickSlider("Ускорение g", g, "м/с²", 1.6f..12.0f, formatStr = "%.1f") { onUpdateParams(currentParams + ("g" to it)) }

                    PhysicsLiveStatCard(
                        isDark = isDark,
                        stats = listOf(
                            "Период T" to "${"%.2f".format(t)} с",
                            "Частота ν" to "${"%.2f".format(if (t > 0f) 1f / t else 0f)} Гц"
                        )
                    )
                }
                PhysicsStencilType.OPTICAL_LENS -> {
                    val f = currentParams["F"] ?: 20.0f
                    val d = currentParams["d"] ?: 30.0f
                    val dP = if (f > 0) 100f / f else 0f

                    PhysicsQuickSlider("Фокусное расст. F", f, "см", 5.0f..40.0f, formatStr = "%.0f") { onUpdateParams(currentParams + ("F" to it)) }
                    PhysicsQuickSlider("Расстояние d", d, "см", 6.0f..80.0f, formatStr = "%.0f") { onUpdateParams(currentParams + ("d" to it)) }

                    PhysicsLiveStatCard(
                        isDark = isDark,
                        stats = listOf(
                            "Оптическая сила D" to "${"%.1f".format(dP)} дптр"
                        )
                    )
                }
                PhysicsStencilType.OPTICAL_PRISM -> {
                    val a = currentParams["A"] ?: 60.0f
                    val n = currentParams["n"] ?: 1.50f

                    PhysicsQuickSlider("Преломляющий угол A", a, "°", 30f..80f, formatStr = "%.0f") { onUpdateParams(currentParams + ("A" to it)) }
                    PhysicsQuickSlider("Показатель n", n, "", 1.10f..2.40f, formatStr = "%.2f") { onUpdateParams(currentParams + ("n" to it)) }
                }
                PhysicsStencilType.MAGNET_FIELD -> {
                    val b = currentParams["B"] ?: 0.5f
                    val i = currentParams["I"] ?: 3.0f

                    PhysicsQuickSlider("Индукция поля B", b, "Тл", 0.1f..2.5f, formatStr = "%.2f") { onUpdateParams(currentParams + ("B" to it)) }
                    PhysicsQuickSlider("Ток проводника I", i, "А", 0.5f..10.0f) { onUpdateParams(currentParams + ("I" to it)) }
                }
                PhysicsStencilType.CIRCUIT_SWITCH -> {
                    Text(
                        text = "Контакт ключа разомкнут / замкнут",
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

@Composable
private fun PhysicsQuickSlider(
    label: String,
    value: Float,
    unit: String,
    valueRange: ClosedFloatingPointRange<Float>,
    formatStr: String = "%.1f",
    onValueChange: (Float) -> Unit
) {
    val isDark = LocalAppTheme.current.isDark
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
            )
            val valText = if (unit.isNotBlank()) "${formatStr.format(value)} $unit" else formatStr.format(value)
            Text(
                text = valText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)
            )
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF2563EB),
                activeTrackColor = Color(0xFF2563EB),
                inactiveTrackColor = if (isDark) Color(0xFF2E333D) else Color(0xFFE2E8F0)
            ),
            modifier = Modifier.fillMaxWidth().height(26.dp)
        )
    }
}

@Composable
private fun PhysicsLiveStatCard(
    isDark: Boolean,
    stats: List<Pair<String, String>>
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isDark) Color(0xFF1E222B) else Color(0xFFF1F5F9))
            .border(1.dp, if (isDark) Color(0xFF2C323E) else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            stats.forEach { (name, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = name,
                        fontSize = 10.5.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                    Text(
                        text = value,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isDark) Color(0xFF38BDF8) else Color(0xFF1D4ED8)
                    )
                }
            }
        }
    }
}
