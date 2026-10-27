package com.lkboard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lkboard.app.model.BoardElement
import com.lkboard.app.model.CanvasTransform
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
