package com.lkboard.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Grid4x4
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lkboard.app.ui.theme.LocalAppTheme
import androidx.compose.ui.unit.sp
import com.lkboard.app.data.ToolbarPosition
import com.lkboard.app.model.CanvasTool
import com.lkboard.app.model.GridType
import com.lkboard.app.model.PenSettings
import kotlin.math.roundToInt

/**
 * Контейнер в эталонном стиле Apple Liquid Glass:
 * - Монолитный стеклянный Box без Surface (исключает визуальные полосы и артефакты).
 * - Фон: Color.White.copy(alpha = 0.85f).
 * - Граница: BorderStroke(1.dp, Color.White.copy(alpha = 0.65f)).
 * - Тень: объемная мягкая тень (spotColor = Color.Black.copy(alpha = 0.08f)).
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    content: @Composable () -> Unit
) {
    val isDark = LocalAppTheme.current.isDark
    Box(
        modifier = modifier
            .shadow(
                elevation = 14.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .clip(shape)
            .background(if (isDark) Color(0xFF181B20).copy(alpha = 0.92f) else Color(0xFFF7F7F9).copy(alpha = 0.92f))
            .border(BorderStroke(1.dp, if (isDark) Color(0xFF2E333D) else Color.White.copy(alpha = 0.85f)), shape)
    ) {
        content()
    }
}

/**
 * Панели управления Lkboard в стиле Apple Glassmorphism:
 * - Верхнее плавающее меню TopMenuBar + быстрые кнопки истории и буфера обмена
 * - Боковой тулбар с инструментами и всплывающим меню стилуса (LeftToolbar)
 * - Виджет масштабирования (Zoom HUD) и переключения сетки
 */
@Composable
fun GlassToolbar(
    boardTitle: String,
    onNavigateToDashboard: () -> Unit = {},
    onInsertRoadmap: () -> Unit = {},
    onInsertKanban: () -> Unit = {},
    onToggleTimer: () -> Unit = {},
    onOpenShareDialog: () -> Unit = {},
    isCollabActive: Boolean = false,
    collabClientsCount: Int = 0,
    scale: Float,
    activeTool: CanvasTool,
    penSettings: PenSettings,
    shapeSettings: com.lkboard.app.model.ShapeSettings = com.lkboard.app.model.ShapeSettings(),
    hasSelectedShape: Boolean = false,
    selectedShapeSettings: com.lkboard.app.model.ShapeSettings? = null,
    canUndo: Boolean,
    canRedo: Boolean,
    hasSelection: Boolean,
    onToolSelect: (CanvasTool) -> Unit,
    onPenSettingsChange: (PenSettings) -> Unit,
    onShapeSettingsChange: (com.lkboard.app.model.ShapeSettings) -> Unit = {},
    onRotateSelectedBy: ((Float) -> Unit)? = null,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onDeleteSelected: () -> Unit,
    onDuplicateSelected: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit,
    onZoomToFit: () -> Unit,
    gridType: GridType = GridType.DOTS,
    onCycleGridType: () -> Unit = {},
    onGridTypeChange: ((GridType) -> Unit)? = null,
    backgroundColorHex: Long = 0xFFFFFFFFL,
    onBackgroundColorChange: (Long) -> Unit = {},
    onImportDocument: () -> Unit = {},
    onNewBoard: () -> Unit = {},
    onOpenBoard: () -> Unit = {},
    onSaveBoard: () -> Unit = {},
    onSaveBoardAs: () -> Unit = {},
    onExportPng: () -> Unit = {},
    onExportSvg: () -> Unit = {},
    onExportPdf: () -> Unit = {},
    onExportSelectionPng: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenVersionDetails: () -> Unit = {},
    enabledTools: Set<CanvasTool> = CanvasTool.entries.toSet(),
    toolbarPosition: ToolbarPosition = ToolbarPosition.LEFT,
    onChangeToolbarPosition: ((ToolbarPosition) -> Unit)? = null,
    onCopyBoardImage: () -> Unit = {},
    onTogglePresentation: () -> Unit = {},
    onToggleLaser: () -> Unit = {},
    isPresentationMode: Boolean = false,
    onExit: () -> Unit = {},
    onSelectAll: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (isPresentationMode) return
    val isDark = LocalAppTheme.current.isDark

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(18.dp)
            .background(Color.Transparent)
    ) {

        // 1. Верхняя панель: Стеклянная капсула меню (TopMenuBar) + кнопки быстрого действия
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .background(Color.Transparent),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Капсула меню: Файл, Правка, Вид и название доски
            TopMenuBar(
                boardTitle = boardTitle,
                onNavigateToDashboard = onNavigateToDashboard,
                onInsertRoadmap = onInsertRoadmap,
                onInsertKanban = onInsertKanban,
                onNewBoard = onNewBoard,
                onOpenBoard = onOpenBoard,
                onSaveBoard = onSaveBoard,
                onSaveBoardAs = onSaveBoardAs,
                onExportPng = onExportPng,
                onExportSvg = onExportSvg,
                onExportPdf = onExportPdf,
                onCopyBoardImage = onCopyBoardImage,
                onTogglePresentation = onTogglePresentation,
                onToggleLaser = onToggleLaser,
                onToggleTimer = onToggleTimer,
                onOpenShareDialog = onOpenShareDialog,
                isCollabActive = isCollabActive,
                collabClientsCount = collabClientsCount,
                onExit = onExit,
                canUndo = canUndo,
                canRedo = canRedo,
                hasSelection = hasSelection,
                onUndo = onUndo,
                onRedo = onRedo,
                onCopy = onCopy,
                onPaste = onPaste,
                onDuplicate = onDuplicateSelected,
                onDelete = onDeleteSelected,
                onSelectAll = onSelectAll,
                onZoomIn = onZoomIn,
                onZoomOut = onZoomOut,
                onResetZoom = onResetZoom,
                onZoomToFit = onZoomToFit,
                gridType = gridType,
                onGridTypeChange = { onGridTypeChange?.invoke(it) },
                backgroundColorHex = backgroundColorHex,
                onBackgroundColorChange = onBackgroundColorChange,
                onImportDocument = onImportDocument,
                onExportSelectionPng = onExportSelectionPng,
                onOpenSettings = onOpenSettings,
                onOpenVersionDetails = onOpenVersionDetails
            )

            // Быстрые кнопки истории и буфера обмена
            GlassPanel(shape = RoundedCornerShape(14.dp)) {
                Row(
                    modifier = Modifier
                        .background(Color.Transparent)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
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
                            tint = if (canUndo) (if (isDark) Color(0xFFF4F4F5) else Color(0xFF1E293B)) else (if (isDark) Color(0xFF52525B) else Color(0xFF94A3B8)),
                            modifier = Modifier.size(17.dp)
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
                            tint = if (canRedo) (if (isDark) Color(0xFFF4F4F5) else Color(0xFF1E293B)) else (if (isDark) Color(0xFF52525B) else Color(0xFF94A3B8)),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Spacer(
                        modifier = Modifier
                            .width(1.dp)
                            .height(16.dp)
                            .background(if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.10f))
                    )

                    IconButton(
                        onClick = onPaste,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Вставить (Ctrl+V)",
                            tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    if (hasSelection) {
                        IconButton(
                            onClick = onCopy,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Копировать (Ctrl+C)",
                                tint = if (isDark) Color(0xFFF4F4F5) else Color(0xFF1E293B),
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        IconButton(
                            onClick = onDeleteSelected,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Удалить (Del)",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Тулбар инструментов с выплывающими меню (LeftToolbar / DockedToolbar)
        val toolbarAlignment = when (toolbarPosition) {
            ToolbarPosition.LEFT -> Alignment.CenterStart
            ToolbarPosition.BOTTOM -> Alignment.BottomCenter
            ToolbarPosition.RIGHT -> Alignment.CenterEnd
        }

        LeftToolbar(
            activeTool = activeTool,
            penSettings = penSettings,
            shapeSettings = shapeSettings,
            hasSelectedShape = hasSelectedShape,
            selectedShapeSettings = selectedShapeSettings,
            onToolSelect = onToolSelect,
            onPenSettingsChange = onPenSettingsChange,
            onShapeSettingsChange = onShapeSettingsChange,
            onRotateShapeBy = onRotateSelectedBy,
            onResetShapeRotation = { onRotateSelectedBy?.invoke(0f) },
            gridType = gridType,
            onGridTypeChange = onGridTypeChange,
            enabledTools = enabledTools,
            position = toolbarPosition,
            onChangePosition = onChangeToolbarPosition,
            modifier = Modifier.align(toolbarAlignment)
        )

        // 3. Виджет масштабирования (Zoom HUD) и сетки в правом нижнем углу
        GlassPanel(
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            Row(
                modifier = Modifier
                    .background(Color.Transparent)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                IconButton(onClick = onZoomOut, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Отдалить",
                        tint = if (isDark) Color(0xFFE4E4E7) else Color(0xFF334155),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = "${(scale * 100).roundToInt()}%",
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onResetZoom() }
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
                )

                IconButton(onClick = onZoomIn, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Приблизить",
                        tint = if (isDark) Color(0xFFE4E4E7) else Color(0xFF334155),
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(onClick = onZoomToFit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.FitScreen,
                        contentDescription = "По размеру контента",
                        tint = if (isDark) Color(0xFFE4E4E7) else Color(0xFF334155),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(
                    modifier = Modifier
                        .width(1.dp)
                        .height(16.dp)
                        .background(if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.10f))
                )

                // Кнопка переключения сетки (Точки -> Клетка -> Чистый лист)
                val (gridIcon, gridDesc) = when (gridType) {
                    GridType.DOTS -> Pair(Icons.Default.GridOn, "Сетка: Точки (нажмите для смены)")
                    GridType.LINES -> Pair(Icons.Default.Grid4x4, "Сетка: Клетка (нажмите для смены)")
                    GridType.NONE -> Pair(Icons.Default.GridOff, "Сетка: Отключена (нажмите для смены)")
                }

                IconButton(onClick = onCycleGridType, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = gridIcon,
                        contentDescription = gridDesc,
                        tint = if (gridType == GridType.NONE) (if (isDark) Color(0xFF52525B) else Color(0xFF94A3B8)) else (if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
