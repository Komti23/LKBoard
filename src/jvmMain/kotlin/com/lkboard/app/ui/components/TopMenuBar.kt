package com.lkboard.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Grid4x4
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lkboard.app.model.GridType

import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Timeline
import com.lkboard.app.ui.components.AppLogo

/**
 * Верхнее меню Lkboard в стиле плавающей стеклянной капсулы (Glass Capsule Menu Bar):
 * - Заменяет устаревший Swing MenuBar.
 * - Форма: RoundedCornerShape(14.dp)
 * - Фон: Color.White.copy(alpha = 0.75f)
 * - Граница: BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
 * - Тень: мягкая глубинная тень без шума.
 * - Кнопки: «Доски», «Файл», «Правка», «Вид» со стильными стеклянными выпадающими меню.
 */
@Composable
fun TopMenuBar(
    boardTitle: String,
    onNavigateToDashboard: () -> Unit = {},
    onInsertRoadmap: () -> Unit = {},
    onInsertKanban: () -> Unit = {},
    onNewBoard: () -> Unit,
    onOpenBoard: () -> Unit,
    onSaveBoard: () -> Unit,
    onSaveBoardAs: () -> Unit,
    onExportPng: () -> Unit,
    onExportSvg: () -> Unit = {},
    onExportPdf: () -> Unit = {},
    onCopyBoardImage: () -> Unit = {},
    onTogglePresentation: () -> Unit = {},
    onToggleLaser: () -> Unit = {},
    onToggleTimer: () -> Unit = {},
    onOpenShareDialog: () -> Unit = {},
    isCollabActive: Boolean = false,
    collabClientsCount: Int = 0,
    onExit: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    hasSelection: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onSelectAll: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit,
    onZoomToFit: () -> Unit,
    gridType: GridType,
    onGridTypeChange: (GridType) -> Unit,
    backgroundColorHex: Long = 0xFFFFFFFFL,
    onBackgroundColorChange: (Long) -> Unit = {},
    onImportDocument: () -> Unit = {},
    onExportSelectionPng: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isFileMenuOpen by remember { mutableStateOf(false) }
    var isEditMenuOpen by remember { mutableStateOf(false) }
    var isViewMenuOpen by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.75f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.60f)), RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .background(Color.Transparent)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Кнопка быстрого перехода на Главный экран (Менеджер проектов) с логотипом
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onNavigateToDashboard() }
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                AppLogo.LogoIcon(size = 20.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Доски",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2563EB)
                )
            }

            Spacer(
                modifier = Modifier
                    .width(1.dp)
                    .height(14.dp)
                    .background(Color.Black.copy(alpha = 0.10f))
            )

            // Пункт «Файл»
            TopMenuCapsuleButton(
                title = "Файл",
                isOpen = isFileMenuOpen,
                onClick = {
                    isFileMenuOpen = !isFileMenuOpen
                    isEditMenuOpen = false
                    isViewMenuOpen = false
                }
            ) {
                DropdownMenu(
                    expanded = isFileMenuOpen,
                    onDismissRequest = { isFileMenuOpen = false },
                    offset = DpOffset(0.dp, 6.dp),
                    modifier = Modifier
                        .requiredWidth(340.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(12.dp),
                            ambientColor = Color.Black.copy(alpha = 0.04f),
                            spotColor = Color.Black.copy(alpha = 0.08f)
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.96f))
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.70f)), RoundedCornerShape(12.dp))
                        .padding(vertical = 4.dp)
                ) {
                    StyledMenuItem(
                        text = "Все доски (Главный экран)",
                        shortcut = "Ctrl+H",
                        icon = Icons.Default.Dashboard,
                        onClick = {
                            isFileMenuOpen = false
                            onNavigateToDashboard()
                        }
                    )
                    StyledMenuItem(
                        text = "Вставить дорожную карту",
                        shortcut = "Ctrl+Shift+R",
                        icon = Icons.Default.Timeline,
                        onClick = {
                            isFileMenuOpen = false
                            onInsertRoadmap()
                        }
                    )
                    StyledMenuItem(
                        text = "Вставить канбан-доску",
                        shortcut = "Ctrl+Shift+K",
                        icon = Icons.Default.ViewKanban,
                        onClick = {
                            isFileMenuOpen = false
                            onInsertKanban()
                        }
                    )
                    MenuDivider()
                    StyledMenuItem(
                        text = "Новая доска",
                        shortcut = "Ctrl+N",
                        icon = Icons.Default.Add,
                        onClick = {
                            isFileMenuOpen = false
                            onNewBoard()
                        }
                    )
                    StyledMenuItem(
                        text = "Открыть...",
                        shortcut = "Ctrl+O",
                        icon = Icons.Default.FolderOpen,
                        onClick = {
                            isFileMenuOpen = false
                            onOpenBoard()
                        }
                    )

                    StyledMenuItem(
                        text = "Импорт презентации / PDF...",
                        shortcut = "Ctrl+Shift+O",
                        icon = Icons.Default.Slideshow,
                        onClick = {
                            isFileMenuOpen = false
                            onImportDocument()
                        }
                    )
                    StyledMenuItem(
                        text = "Сохранить",
                        shortcut = "Ctrl+S",
                        icon = Icons.Default.Save,
                        onClick = {
                            isFileMenuOpen = false
                            onSaveBoard()
                        }
                    )
                    StyledMenuItem(
                        text = "Сохранить как...",
                        shortcut = "Ctrl+Shift+S",
                        icon = Icons.Default.Save,
                        onClick = {
                            isFileMenuOpen = false
                            onSaveBoardAs()
                        }
                    )
                    MenuDivider()
                    StyledMenuItem(
                        text = "Совместный доступ по ссылке...",
                        icon = Icons.Default.Link,
                        onClick = {
                            isFileMenuOpen = false
                            onOpenShareDialog()
                        }
                    )
                    MenuDivider()
                    StyledMenuItem(
                        text = "Экспорт в PNG...",
                        shortcut = "Ctrl+E",
                        icon = Icons.Default.Image,
                        onClick = {
                            isFileMenuOpen = false
                            onExportPng()
                        }
                    )
                    StyledMenuItem(
                        text = "Экспорт выделения в PNG...",
                        shortcut = "Ctrl+Shift+E",
                        icon = Icons.Default.Image,
                        enabled = hasSelection,
                        onClick = {
                            isFileMenuOpen = false
                            onExportSelectionPng()
                        }
                    )
                    StyledMenuItem(
                        text = "Экспорт в SVG (Вектор)...",
                        icon = Icons.Default.Code,
                        onClick = {
                            isFileMenuOpen = false
                            onExportSvg()
                        }
                    )
                    StyledMenuItem(
                        text = "Экспорт в PDF...",
                        icon = Icons.Default.PictureAsPdf,
                        onClick = {
                            isFileMenuOpen = false
                            onExportPdf()
                        }
                    )
                    MenuDivider()
                    StyledMenuItem(
                        text = "Выход",
                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                        isDestructive = true,
                        onClick = {
                            isFileMenuOpen = false
                            onExit()
                        }
                    )
                }
            }

            // Пункт «Правка»
            TopMenuCapsuleButton(
                title = "Правка",
                isOpen = isEditMenuOpen,
                onClick = {
                    isEditMenuOpen = !isEditMenuOpen
                    isFileMenuOpen = false
                    isViewMenuOpen = false
                }
            ) {
                DropdownMenu(
                    expanded = isEditMenuOpen,
                    onDismissRequest = { isEditMenuOpen = false },
                    offset = DpOffset(0.dp, 6.dp),
                    modifier = Modifier
                        .requiredWidth(320.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(12.dp),
                            ambientColor = Color.Black.copy(alpha = 0.04f),
                            spotColor = Color.Black.copy(alpha = 0.08f)
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.96f))
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.70f)), RoundedCornerShape(12.dp))
                        .padding(vertical = 4.dp)
                ) {
                    StyledMenuItem(
                        text = "Отменить",
                        shortcut = "Ctrl+Z",
                        icon = Icons.AutoMirrored.Filled.Undo,
                        enabled = canUndo,
                        onClick = {
                            isEditMenuOpen = false
                            onUndo()
                        }
                    )
                    StyledMenuItem(
                        text = "Повторить",
                        shortcut = "Ctrl+Y",
                        icon = Icons.AutoMirrored.Filled.Redo,
                        enabled = canRedo,
                        onClick = {
                            isEditMenuOpen = false
                            onRedo()
                        }
                    )
                    MenuDivider()
                    StyledMenuItem(
                        text = "Копировать",
                        shortcut = "Ctrl+C",
                        icon = Icons.Default.ContentCopy,
                        enabled = hasSelection,
                        onClick = {
                            isEditMenuOpen = false
                            onCopy()
                        }
                    )
                    StyledMenuItem(
                        text = "Скопировать как изображение",
                        shortcut = "Ctrl+Shift+C",
                        icon = Icons.Default.ContentCopy,
                        onClick = {
                            isEditMenuOpen = false
                            onCopyBoardImage()
                        }
                    )
                    StyledMenuItem(
                        text = "Вставить",
                        shortcut = "Ctrl+V",
                        icon = Icons.Default.ContentPaste,
                        onClick = {
                            isEditMenuOpen = false
                            onPaste()
                        }
                    )
                    StyledMenuItem(
                        text = "Дублировать",
                        shortcut = "Ctrl+D",
                        enabled = hasSelection,
                        onClick = {
                            isEditMenuOpen = false
                            onDuplicate()
                        }
                    )
                    StyledMenuItem(
                        text = "Удалить",
                        shortcut = "Del",
                        icon = Icons.Default.DeleteOutline,
                        enabled = hasSelection,
                        isDestructive = true,
                        onClick = {
                            isEditMenuOpen = false
                            onDelete()
                        }
                    )
                    MenuDivider()
                    StyledMenuItem(
                        text = "Выделить все",
                        shortcut = "Ctrl+A",
                        icon = Icons.Default.SelectAll,
                        onClick = {
                            isEditMenuOpen = false
                            onSelectAll()
                        }
                    )
                }
            }

            // Пункт «Вид»
            TopMenuCapsuleButton(
                title = "Вид",
                isOpen = isViewMenuOpen,
                onClick = {
                    isViewMenuOpen = !isViewMenuOpen
                    isFileMenuOpen = false
                    isEditMenuOpen = false
                }
            ) {
                DropdownMenu(
                    expanded = isViewMenuOpen,
                    onDismissRequest = { isViewMenuOpen = false },
                    offset = DpOffset(0.dp, 6.dp),
                    modifier = Modifier
                        .requiredWidth(300.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(12.dp),
                            ambientColor = Color.Black.copy(alpha = 0.04f),
                            spotColor = Color.Black.copy(alpha = 0.08f)
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.96f))
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.70f)), RoundedCornerShape(12.dp))
                        .padding(vertical = 4.dp)
                ) {
                    StyledMenuItem(
                        text = "Приблизить",
                        shortcut = "Ctrl+=",
                        icon = Icons.Default.Add,
                        onClick = {
                            isViewMenuOpen = false
                            onZoomIn()
                        }
                    )
                    StyledMenuItem(
                        text = "Отдалить",
                        shortcut = "Ctrl+-",
                        icon = Icons.Default.Remove,
                        onClick = {
                            isViewMenuOpen = false
                            onZoomOut()
                        }
                    )
                    StyledMenuItem(
                        text = "Сброс масштаба (100%)",
                        shortcut = "Ctrl+0",
                        onClick = {
                            isViewMenuOpen = false
                            onResetZoom()
                        }
                    )
                    StyledMenuItem(
                        text = "По размеру контента",
                        shortcut = "Ctrl+1",
                        icon = Icons.Default.FitScreen,
                        onClick = {
                            isViewMenuOpen = false
                            onZoomToFit()
                        }
                    )
                    MenuDivider()
                    StyledMenuItem(
                        text = "Режим презентации",
                        shortcut = "F5",
                        icon = Icons.Default.Slideshow,
                        onClick = {
                            isViewMenuOpen = false
                            onTogglePresentation()
                        }
                    )
                    StyledMenuItem(
                        text = "Лазерная указка",
                        shortcut = "L",
                        icon = Icons.Default.Highlight,
                        onClick = {
                            isViewMenuOpen = false
                            onToggleLaser()
                        }
                    )
                    StyledMenuItem(
                        text = "Таймер воркшопа",
                        icon = Icons.Default.Timer,
                        onClick = {
                            isViewMenuOpen = false
                            onToggleTimer()
                        }
                    )
                    MenuDivider()
                    StyledMenuItem(
                        text = "Сетка: Точки",
                        icon = if (gridType == GridType.DOTS) Icons.Default.Check else Icons.Default.GridOn,
                        onClick = {
                            isViewMenuOpen = false
                            onGridTypeChange(GridType.DOTS)
                        }
                    )
                    StyledMenuItem(
                        text = "Сетка: Клетка (математика)",
                        icon = if (gridType == GridType.LINES) Icons.Default.Check else Icons.Default.Grid4x4,
                        onClick = {
                            isViewMenuOpen = false
                            onGridTypeChange(GridType.LINES)
                        }
                    )
                    StyledMenuItem(
                        text = "Сетка: Чистый лист",
                        icon = if (gridType == GridType.NONE) Icons.Default.Check else Icons.Default.GridOff,
                        onClick = {
                            isViewMenuOpen = false
                            onGridTypeChange(GridType.NONE)
                        }
                    )
                    MenuDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Цвет фона холста",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val presetBgs = listOf(
                            0xFFFFFFFFL, // Белый
                            0xFFFAF8F5L, // Теплая бумага
                            0xFFF1F5F9L, // Светлый сланец
                            0xFF1E293BL, // Графитовый
                            0xFF0F172AL, // Полночь
                            0xFF050505L, // AMOLED Черный
                            0xFF1B3B2BL, // Школьная доска
                            0xFF1A365DL  // Чертеж
                        )
                        presetBgs.forEach { hex ->
                            val isSelected = backgroundColorHex == hex
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(hex))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF007AFF) else Color.Black.copy(alpha = 0.25f),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        onBackgroundColorChange(hex)
                                        isViewMenuOpen = false
                                    }
                            )
                        }
                    }
                }
            }

            // Тонкий вертикальный разделитель
            Spacer(
                modifier = Modifier
                    .width(1.dp)
                    .height(14.dp)
                    .background(Color.Black.copy(alpha = 0.10f))
            )

            // Название доски
            Text(
                text = boardTitle,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E293B),
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Кнопка быстрого шеринга: «Поделиться ссылкой»
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isCollabActive) Color(0xFFECFDF5) else Color(0xFFF1F5F9))
                    .border(
                        BorderStroke(
                            1.dp,
                            if (isCollabActive) Color(0xFFA7F3D0) else Color(0xFFE2E8F0)
                        ),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onOpenShareDialog() }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                if (isCollabActive) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (collabClientsCount > 1) "В эфире ($collabClientsCount)" else "В эфире",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF047857)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = "Поделиться",
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Поделиться",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0284C7)
                    )
                }
            }

            Spacer(
                modifier = Modifier
                    .width(1.dp)
                    .height(14.dp)
                    .background(Color.Black.copy(alpha = 0.10f))
            )

            // Кнопка «Настройки приложения»
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenSettings() }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Настройки",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

/**
 * Кнопка элемента меню в капсуле.
 */
@Composable
private fun TopMenuCapsuleButton(
    title: String,
    isOpen: Boolean,
    onClick: () -> Unit,
    menuContent: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor = when {
        isOpen -> Color(0xFF007AFF).copy(alpha = 0.12f)
        isHovered -> Color.Black.copy(alpha = 0.05f)
        else -> Color.Transparent
    }
    val textColor = when {
        isOpen -> Color(0xFF007AFF)
        else -> Color(0xFF1E293B)
    }

    Box {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(backgroundColor)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .padding(horizontal = 9.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isOpen) FontWeight.SemiBold else FontWeight.Medium,
                color = textColor
            )
        }
        menuContent()
    }
}

/**
 * Элемент выпадающего меню в стиле Apple macOS:
 * - maxLines = 1 и TextOverflow.Ellipsis гарантируют, что текст никогда не переносится и не вылезает.
 * - Деликатный hover-эффект с закруглением 6.dp без лишних отступов.
 * - Единое вертикальное выравнивание иконок и горячих клавиш.
 */
@Composable
private fun StyledMenuItem(
    text: String,
    shortcut: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val itemBg = when {
        !enabled -> Color.Transparent
        isHovered && isDestructive -> Color(0xFFEF4444).copy(alpha = 0.12f)
        isHovered -> Color.Black.copy(alpha = 0.06f)
        else -> Color.Transparent
    }

    val contentColor = when {
        !enabled -> Color(0xFF94A3B8)
        isDestructive -> Color(0xFFEF4444)
        else -> Color(0xFF1E293B)
    }

    val iconColor = when {
        !enabled -> Color(0xFF94A3B8)
        isDestructive -> Color(0xFFEF4444)
        else -> Color(0xFF475569)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(itemBg)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Spacer(modifier = Modifier.size(16.dp))
            }

            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (shortcut != null) {
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = shortcut,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = if (enabled) Color(0xFF94A3B8) else Color(0xFFCBD5E1),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * Разделитель в выпадающем меню.
 */
@Composable
private fun MenuDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        thickness = 1.dp,
        color = Color.Black.copy(alpha = 0.07f)
    )
}
