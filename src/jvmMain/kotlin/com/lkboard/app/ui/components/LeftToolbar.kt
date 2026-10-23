package com.lkboard.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.CropLandscape
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lkboard.app.model.CanvasTool
import com.lkboard.app.model.GridType
import com.lkboard.app.model.PenSettings
import com.lkboard.app.model.ShapeSettings

/**
 * Левый тулбар инструментов Lkboard в эталонном стиле Apple Liquid Glass:
 * - Монолитный стеклянный Box без использования Material 3 Surface и без внутренних белых подложек/полос.
 * - Единый внешний фон: Color(0xFFF5F5F7).copy(alpha = 0.85f), скругление 22.dp, граница 1.dp Color.White.copy(alpha = 0.70f).
 * - Все внутренние Layout-компоненты (Column, Row, Box) строго прозрачные (Color.Transparent).
 * - Поддержка открытия меню свойств для Пера (StylusPropertiesPopup) и Фигур (ShapePropertiesPopup).
 * - Тоггл при повторном клике на активный инструмент (Перо / Фигура).
 */
@Composable
fun LeftToolbar(
    activeTool: CanvasTool,
    penSettings: PenSettings,
    shapeSettings: ShapeSettings = ShapeSettings(),
    hasSelectedShape: Boolean = false,
    selectedShapeSettings: ShapeSettings? = null,
    onToolSelect: (CanvasTool) -> Unit,
    onPenSettingsChange: (PenSettings) -> Unit,
    onShapeSettingsChange: (ShapeSettings) -> Unit = {},
    onRotateShapeBy: ((Float) -> Unit)? = null,
    onResetShapeRotation: (() -> Unit)? = null,
    gridType: GridType = GridType.DOTS,
    onGridTypeChange: ((GridType) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isStylusMenuOpen by remember { mutableStateOf(activeTool == CanvasTool.PEN) }
    var isShapeMenuOpen by remember { mutableStateOf(activeTool == CanvasTool.SHAPE || hasSelectedShape) }

    LaunchedEffect(activeTool, hasSelectedShape) {
        when {
            hasSelectedShape -> {
                isShapeMenuOpen = true
                isStylusMenuOpen = false
            }
            activeTool == CanvasTool.PEN -> {
                isStylusMenuOpen = true
                isShapeMenuOpen = false
            }
            activeTool == CanvasTool.SHAPE -> {
                isShapeMenuOpen = true
                isStylusMenuOpen = false
            }
            else -> {
                isStylusMenuOpen = false
                isShapeMenuOpen = false
            }
        }
    }

    Row(
        modifier = modifier.background(Color.Transparent),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Монолитная стеклянная панель тулбара (чистый прозрачный Column внутри)
        Box(
            modifier = Modifier
                .shadow(
                    elevation = 18.dp,
                    shape = RoundedCornerShape(22.dp),
                    ambientColor = Color.Black.copy(alpha = 0.04f),
                    spotColor = Color.Black.copy(alpha = 0.08f)
                )
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFFF7F7F9))
                .border(1.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(22.dp))
        ) {
            Column(
                modifier = Modifier
                    .background(Color.Transparent)
                    .padding(7.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Выделение
                ToolbarIconButton(
                    icon = Icons.Default.Navigation,
                    label = "Выделение (1 / V)",
                    isSelected = activeTool == CanvasTool.SELECT,
                    onClick = {
                        isStylusMenuOpen = false
                        isShapeMenuOpen = false
                        onToolSelect(CanvasTool.SELECT)
                    }
                )

                // 2. Панорама
                ToolbarIconButton(
                    icon = Icons.Default.PanTool,
                    label = "Панорама (H / Пробел)",
                    isSelected = activeTool == CanvasTool.HAND,
                    onClick = {
                        isStylusMenuOpen = false
                        isShapeMenuOpen = false
                        onToolSelect(CanvasTool.HAND)
                    }
                )

                // 3. Перо (стилус) с индикатором цвета
                ToolbarIconButton(
                    icon = Icons.Default.Create,
                    label = "Перо (Q / P)",
                    isSelected = activeTool == CanvasTool.PEN,
                    colorIndicator = Color(penSettings.colorHex),
                    onClick = {
                        if (activeTool == CanvasTool.PEN) {
                            isStylusMenuOpen = !isStylusMenuOpen
                        } else {
                            onToolSelect(CanvasTool.PEN)
                            isStylusMenuOpen = true
                            isShapeMenuOpen = false
                        }
                    }
                )

                // 4. Ластик
                ToolbarIconButton(
                    icon = Icons.Default.AutoFixNormal,
                    label = "Ластик (E)",
                    isSelected = activeTool == CanvasTool.ERASER,
                    onClick = {
                        isStylusMenuOpen = false
                        isShapeMenuOpen = false
                        onToolSelect(CanvasTool.ERASER)
                    }
                )

                // 5. Стикер
                ToolbarIconButton(
                    icon = Icons.AutoMirrored.Filled.StickyNote2,
                    label = "Стикер (S)",
                    isSelected = activeTool == CanvasTool.STICKY_NOTE,
                    colorIndicator = Color(0xFFFEF08A),
                    onClick = {
                        isStylusMenuOpen = false
                        isShapeMenuOpen = false
                        onToolSelect(CanvasTool.STICKY_NOTE)
                    }
                )

                // 6. Геометрические фигуры
                val effectiveShapeSettings = selectedShapeSettings ?: shapeSettings
                ToolbarIconButton(
                    icon = Icons.Default.CropLandscape,
                    label = "Фигура (R)",
                    isSelected = activeTool == CanvasTool.SHAPE || hasSelectedShape,
                    colorIndicator = Color(effectiveShapeSettings.strokeColorHex),
                    onClick = {
                        if (activeTool == CanvasTool.SHAPE || hasSelectedShape) {
                            isShapeMenuOpen = !isShapeMenuOpen
                        } else {
                            onToolSelect(CanvasTool.SHAPE)
                            isShapeMenuOpen = true
                            isStylusMenuOpen = false
                        }
                    }
                )

                // 7. Умные коннекторы (Smart Connectors)
                ToolbarIconButton(
                    icon = Icons.AutoMirrored.Filled.AltRoute,
                    label = "Коннектор (C)",
                    isSelected = activeTool == CanvasTool.CONNECTOR,
                    onClick = {
                        isStylusMenuOpen = false
                        isShapeMenuOpen = false
                        onToolSelect(CanvasTool.CONNECTOR)
                    }
                )

                // 8. Веха дорожной карты (Milestone Card)
                ToolbarIconButton(
                    icon = Icons.Default.Flag,
                    label = "Веха / Roadmap (M)",
                    isSelected = activeTool == CanvasTool.MILESTONE,
                    onClick = {
                        isStylusMenuOpen = false
                        isShapeMenuOpen = false
                        onToolSelect(CanvasTool.MILESTONE)
                    }
                )

                // 9. Канбан-доска (Kanban Board)
                ToolbarIconButton(
                    icon = Icons.Default.ViewKanban,
                    label = "Канбан-доска (K)",
                    isSelected = activeTool == CanvasTool.KANBAN,
                    onClick = {
                        isStylusMenuOpen = false
                        isShapeMenuOpen = false
                        onToolSelect(CanvasTool.KANBAN)
                    }
                )

                // 9. График функций (Math Function Plotter)
                ToolbarIconButton(
                    icon = Icons.AutoMirrored.Filled.ShowChart,
                    label = "График функций (G)",
                    isSelected = activeTool == CanvasTool.FUNCTION_GRAPH,
                    onClick = {
                        isStylusMenuOpen = false
                        isShapeMenuOpen = false
                        onToolSelect(CanvasTool.FUNCTION_GRAPH)
                    }
                )

                // 10. Текст
                ToolbarIconButton(
                    icon = Icons.Default.FormatColorText,
                    label = "Текст (T)",
                    isSelected = activeTool == CanvasTool.TEXT,
                    onClick = {
                        isStylusMenuOpen = false
                        isShapeMenuOpen = false
                        onToolSelect(CanvasTool.TEXT)
                    }
                )

                // 11. Фрейм / Слайды
                ToolbarIconButton(
                    icon = Icons.Default.Slideshow,
                    label = "Фрейм / Презентация (F)",
                    isSelected = activeTool == CanvasTool.FRAME,
                    onClick = {
                        isStylusMenuOpen = false
                        isShapeMenuOpen = false
                        onToolSelect(CanvasTool.FRAME)
                    }
                )

                // 12. Лазерная указка
                ToolbarIconButton(
                    icon = Icons.Default.Highlight,
                    label = "Лазерная указка (L)",
                    isSelected = activeTool == CanvasTool.LASER_POINTER,
                    colorIndicator = Color(0xFFFF2A4B),
                    onClick = {
                        isStylusMenuOpen = false
                        isShapeMenuOpen = false
                        onToolSelect(CanvasTool.LASER_POINTER)
                    }
                )
            }
        }

        // Всплывающее меню настроек пера
        AnimatedVisibility(
            visible = activeTool == CanvasTool.PEN && isStylusMenuOpen,
            enter = fadeIn() + scaleIn(initialScale = 0.94f),
            exit = fadeOut() + scaleOut(targetScale = 0.94f)
        ) {
            StylusPropertiesPopup(
                settings = penSettings,
                onSettingsChange = onPenSettingsChange,
                gridType = gridType,
                onGridTypeChange = onGridTypeChange
            )
        }

        // Всплывающее меню настроек геометрических фигур
        val effectiveShape = selectedShapeSettings ?: shapeSettings
        AnimatedVisibility(
            visible = (activeTool == CanvasTool.SHAPE || hasSelectedShape) && isShapeMenuOpen,
            enter = fadeIn() + scaleIn(initialScale = 0.94f),
            exit = fadeOut() + scaleOut(targetScale = 0.94f)
        ) {
            ShapePropertiesPopup(
                settings = effectiveShape,
                isEditingSelectedShape = hasSelectedShape,
                onSettingsChange = onShapeSettingsChange,
                onRotateBy = onRotateShapeBy,
                onResetRotation = onResetShapeRotation
            )
        }
    }
}

/**
 * Кнопка инструмента в тулбаре (42x42.dp) без промежуточных фоновых рамок:
 * - Активная: мягкая полупрозрачная синяя подсветка Color(0xFF007AFF).copy(alpha = 0.12f).
 * - Наведение: легкое затемнение Color.Black.copy(alpha = 0.05f).
 * - Обычное: полностью прозрачный фон (Color.Transparent).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToolbarIconButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    colorIndicator: Color? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor = when {
        isSelected -> Color(0xFF007AFF).copy(alpha = 0.12f)
        isHovered -> Color.Black.copy(alpha = 0.05f)
        else -> Color.Transparent
    }
    val tintColor = if (isSelected) Color(0xFF007AFF) else Color(0xFF3C3C43)

    TooltipArea(
        tooltip = {
            Box(
                modifier = Modifier
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E1E1E).copy(alpha = 0.92f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = Alignment.CenterEnd,
            alignment = Alignment.CenterEnd,
            offset = DpOffset(12.dp, 0.dp)
        ),
        delayMillis = 350
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(backgroundColor)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tintColor,
                modifier = Modifier.size(20.dp)
            )

            // Индикатор активного цвета в правом нижнем углу кнопки
            if (colorIndicator != null && !isSelected) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .align(Alignment.BottomEnd)
                        .padding(end = 4.dp, bottom = 4.dp)
                        .clip(CircleShape)
                        .background(colorIndicator)
                )
            }
        }
    }
}
