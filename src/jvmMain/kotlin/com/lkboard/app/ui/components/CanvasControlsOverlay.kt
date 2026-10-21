package com.lkboard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.CropLandscape
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lkboard.app.model.CanvasTool
import kotlin.math.roundToInt

/**
 * Плавающие элементы интерфейса поверх бесконечного холста:
 * - Панель инструментов (Toolbar в стиле Miro слева по центру)
 * - Панель управления историей (Undo/Redo/Delete/Duplicate вверху)
 * - Виджет управления масштабом (Zoom HUD в правом нижнем углу)
 * - Заголовок доски (в левом верхнем углу)
 */
@Composable
fun CanvasControlsOverlay(
    boardTitle: String,
    scale: Float,
    activeTool: CanvasTool,
    canUndo: Boolean,
    canRedo: Boolean,
    hasSelection: Boolean,
    onToolSelect: (CanvasTool) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onDeleteSelected: () -> Unit,
    onDuplicateSelected: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().padding(16.dp)) {
        // Верхняя панель: заголовок и кнопки истории действий (Undo/Redo)
        Row(
            modifier = Modifier.align(Alignment.TopStart),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.shadow(elevation = 6.dp, shape = RoundedCornerShape(12.dp)),
                color = Color.White,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2563EB))
                    )
                    Text(
                        text = boardTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )
                }
            }

            Surface(
                modifier = Modifier.shadow(elevation = 6.dp, shape = RoundedCornerShape(12.dp)),
                color = Color.White,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onUndo,
                        enabled = canUndo,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Отменить (Ctrl+Z)",
                            tint = if (canUndo) Color(0xFF334155) else Color(0xFFCBD5E1)
                        )
                    }

                    IconButton(
                        onClick = onRedo,
                        enabled = canRedo,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Повторить (Ctrl+Y)",
                            tint = if (canRedo) Color(0xFF334155) else Color(0xFFCBD5E1)
                        )
                    }

                    if (hasSelection) {
                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = onDuplicateSelected,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Дублировать (Ctrl+D)",
                                tint = Color(0xFF334155)
                            )
                        }

                        IconButton(
                            onClick = onDeleteSelected,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Удалить (Del)",
                                tint = Color(0xFFEF4444)
                            )
                        }
                    }
                }
            }
        }

        // Панель инструментов слева по центру
        Surface(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp)),
            color = Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ToolButton(
                    icon = Icons.Default.Navigation,
                    label = "Выделение (V)",
                    isSelected = activeTool == CanvasTool.SELECT,
                    onClick = { onToolSelect(CanvasTool.SELECT) }
                )
                ToolButton(
                    icon = Icons.Default.PanTool,
                    label = "Панорама (H / Пробел)",
                    isSelected = activeTool == CanvasTool.HAND,
                    onClick = { onToolSelect(CanvasTool.HAND) }
                )
                ToolButton(
                    icon = Icons.Default.EditNote,
                    label = "Стикер (S)",
                    isSelected = activeTool == CanvasTool.STICKY_NOTE,
                    onClick = { onToolSelect(CanvasTool.STICKY_NOTE) }
                )
                ToolButton(
                    icon = Icons.Default.CropLandscape,
                    label = "Фигура (R)",
                    isSelected = activeTool == CanvasTool.SHAPE,
                    onClick = { onToolSelect(CanvasTool.SHAPE) }
                )
                ToolButton(
                    icon = Icons.Default.FormatColorText,
                    label = "Текст (T)",
                    isSelected = activeTool == CanvasTool.TEXT,
                    onClick = { onToolSelect(CanvasTool.TEXT) }
                )
                ToolButton(
                    icon = Icons.Default.Create,
                    label = "Перо (P)",
                    isSelected = activeTool == CanvasTool.PEN,
                    onClick = { onToolSelect(CanvasTool.PEN) }
                )
            }
        }

        // Виджет масштабирования справа внизу
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp)),
            color = Color.White,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onZoomOut, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Отдалить",
                        tint = Color(0xFF475569)
                    )
                }

                Text(
                    text = "${(scale * 100).roundToInt()}%",
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onResetZoom() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF334155)
                )

                IconButton(onClick = onZoomIn, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Приблизить",
                        tint = Color(0xFF475569)
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) Color(0xFFEFF6FF) else Color.Transparent
    val tint = if (isSelected) Color(0xFF2563EB) else Color(0xFF64748B)

    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}
