package com.lkboard.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.lkboard.app.export.PngExporter
import com.lkboard.app.model.BoardElement
import com.lkboard.app.model.CanvasTool
import com.lkboard.app.ui.canvas.InfiniteCanvas
import com.lkboard.app.ui.components.AppLogo
import com.lkboard.app.ui.components.GlassToolbar
import com.lkboard.app.ui.components.MilestoneEditDialog
import com.lkboard.app.ui.components.FunctionGraphEditDialog
import com.lkboard.app.ui.components.ShareBoardDialog
import com.lkboard.app.ui.components.StickyNoteEditDialog
import com.lkboard.app.ui.components.TextEditDialog
import com.lkboard.app.ui.components.UpdateDialog
import com.lkboard.app.ui.components.LatexEditDialog
import com.lkboard.app.ui.components.PhysicsStencilDialog
import com.lkboard.app.ui.components.FirstRunOnboardingDialog
import com.lkboard.app.ui.components.SettingsDialog
import com.lkboard.app.ui.components.VersionDetailsDialog
import com.lkboard.app.ui.dashboard.DashboardScreen
import com.lkboard.app.ui.theme.LkboardTheme
import com.lkboard.app.ui.viewmodel.AppScreen
import com.lkboard.app.ui.viewmodel.BoardViewModel
import com.lkboard.app.util.FileDialogs

fun main() = application {
    val windowState = rememberWindowState(width = 1366.dp, height = 850.dp)
    val viewModel = remember { BoardViewModel() }

    val currentScreen by viewModel.currentScreen.collectAsState()
    val boardsList by viewModel.boardsList.collectAsState()
    val activeBoardId by viewModel.activeBoardId.collectAsState()

    val boardState by viewModel.state.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val editingElement by viewModel.editingElement.collectAsState()
    val editingMilestone by viewModel.editingMilestone.collectAsState()
    val editingFunctionGraph by viewModel.editingFunctionGraph.collectAsState()
    val editingStickyNote by viewModel.editingStickyNote.collectAsState()
    val workshopTimerState by viewModel.workshopTimerState.collectAsState()
    val collabState by viewModel.collabState.collectAsState()
    val isShareDialogOpen by viewModel.isShareDialogOpen.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val showUpdateDialog by viewModel.showUpdateDialog.collectAsState()
    val isPresentationMode by viewModel.isPresentationMode.collectAsState()
    val isLaserPointerActive by viewModel.isLaserPointerActive.collectAsState()
    val laserTrail by viewModel.laserTrail.collectAsState()
    val activeGuides by viewModel.activeGuides.collectAsState()
    val currentFile by viewModel.currentFile.collectAsState()

    val currentTheme by viewModel.currentTheme.collectAsState()
    val currentUserRole by viewModel.currentUserRole.collectAsState()
    val isOnboardingOpen by viewModel.isOnboardingOpen.collectAsState()
    val isSettingsDialogOpen by viewModel.isSettingsDialogOpen.collectAsState()
    val isLatexDialogOpen by viewModel.isLatexDialogOpen.collectAsState()
    val editingLatexFormula by viewModel.editingLatexFormula.collectAsState()
    val isPhysicsStencilDialogOpen by viewModel.isPhysicsStencilDialogOpen.collectAsState()
    val isTabletModeActive by viewModel.isTabletModeEnabled.collectAsState()

    val logoPainter = AppLogo.rememberLogoPainter()

    var showVersionDetailsDialog by remember { mutableStateOf(false) }
    var currentCursorScreenPos by remember { mutableStateOf<Offset?>(null) }

    fun getPasteTargetWorldPos(): Offset {
        val cursor = currentCursorScreenPos
        return if (cursor != null && cursor.x > 0f && cursor.y > 0f) {
            boardState.transform.screenToWorld(cursor)
        } else {
            boardState.transform.screenToWorld(
                Offset(windowState.size.width.value / 2f, windowState.size.height.value / 2f)
            )
        }
    }

    remember {
        Runtime.getRuntime().addShutdownHook(Thread {
            try {
                viewModel.autoSaveSync()
            } catch (_: Exception) {}
        })
    }

    val handleExit: () -> Unit = {
        viewModel.autoSaveSync()
        exitApplication()
    }

    Window(
        onCloseRequest = handleExit,
        title = if (currentScreen == AppScreen.DASHBOARD) "Lkboard — Студия досок и дорожных карт" else "${boardState.title} — Lkboard (Online Tutoring & Inking)",
        state = windowState,
        icon = logoPainter,
        onKeyEvent = { keyEvent ->
            if (showVersionDetailsDialog && keyEvent.type == KeyEventType.KeyDown &&
                (keyEvent.key == Key.Escape || (keyEvent.nativeKeyEvent as? java.awt.event.KeyEvent)?.keyCode == java.awt.event.KeyEvent.VK_ESCAPE)
            ) {
                showVersionDetailsDialog = false
                true
            } else if (currentScreen == AppScreen.CANVAS && keyEvent.type == KeyEventType.KeyDown &&
                editingElement == null && editingStickyNote == null && editingMilestone == null && editingFunctionGraph == null
            ) {
                val isCtrl = keyEvent.isCtrlPressed
                val isShift = keyEvent.isShiftPressed
                val rawKeyCode = (keyEvent.nativeKeyEvent as? java.awt.event.KeyEvent)?.keyCode ?: 0

                when {
                    // Презентационный режим (F5)
                    keyEvent.key == Key.F5 || rawKeyCode == java.awt.event.KeyEvent.VK_F5 -> {
                        viewModel.togglePresentationMode()
                        true
                    }

                    // Навигация внутри режима презентации
                    isPresentationMode && (keyEvent.key == Key.Escape || rawKeyCode == java.awt.event.KeyEvent.VK_ESCAPE) -> {
                        viewModel.setPresentationMode(false)
                        true
                    }
                    isPresentationMode && (keyEvent.key == Key.DirectionRight || keyEvent.key == Key.Spacebar || keyEvent.key == Key.Enter) -> {
                        viewModel.nextPresentationSlide()
                        true
                    }
                    isPresentationMode && keyEvent.key == Key.DirectionLeft -> {
                        viewModel.prevPresentationSlide()
                        true
                    }
                    isPresentationMode && !isCtrl && (keyEvent.key == Key.L || rawKeyCode == java.awt.event.KeyEvent.VK_L) -> {
                        viewModel.toggleLaserPointer()
                        true
                    }

                    // Возврат на Главный экран (Ctrl+H или Esc вне презентации)
                    (isCtrl && (keyEvent.key == Key.H || rawKeyCode == java.awt.event.KeyEvent.VK_H)) ||
                    (!isPresentationMode && (keyEvent.key == Key.Escape || rawKeyCode == java.awt.event.KeyEvent.VK_ESCAPE)) -> {
                        viewModel.navigateToDashboard()
                        true
                    }

                    // Отмена / Повтор
                    isCtrl && (keyEvent.key == Key.Z || rawKeyCode == java.awt.event.KeyEvent.VK_Z) && !isShift -> {
                        viewModel.undo()
                        true
                    }
                    (isCtrl && (keyEvent.key == Key.Y || rawKeyCode == java.awt.event.KeyEvent.VK_Y)) ||
                    (isCtrl && isShift && (keyEvent.key == Key.Z || rawKeyCode == java.awt.event.KeyEvent.VK_Z)) -> {
                        viewModel.redo()
                        true
                    }
                    // Копировать доску как изображение в буфер обмена (Ctrl+Shift+C)
                    isCtrl && isShift && (keyEvent.key == Key.C || rawKeyCode == java.awt.event.KeyEvent.VK_C) -> {
                        viewModel.copyBoardImageToClipboard()
                        true
                    }
                    // Буфер обмена (Копирование / Вставка элементов)
                    isCtrl && (keyEvent.key == Key.C || rawKeyCode == java.awt.event.KeyEvent.VK_C) -> {
                        viewModel.copySelected()
                        true
                    }
                    isCtrl && (keyEvent.key == Key.V || rawKeyCode == java.awt.event.KeyEvent.VK_V) -> {
                        viewModel.pasteFromClipboard(getPasteTargetWorldPos())
                        true
                    }
                    // Дублирование
                    isCtrl && (keyEvent.key == Key.D || rawKeyCode == java.awt.event.KeyEvent.VK_D) -> {
                        viewModel.duplicateSelectedElements()
                        true
                    }
                    // Вращение выбранных элементов (R / Ctrl+R на 90°, [ / ] на 15°)
                    (isCtrl && (keyEvent.key == Key.R || rawKeyCode == java.awt.event.KeyEvent.VK_R)) ||
                    (!isCtrl && boardState.selectedElementIds.isNotEmpty() && (keyEvent.key == Key.R || rawKeyCode == java.awt.event.KeyEvent.VK_R)) -> {
                        viewModel.rotateSelectedBy(if (isShift) -90f else 90f)
                        true
                    }
                    boardState.selectedElementIds.isNotEmpty() && (keyEvent.key == Key.LeftBracket || rawKeyCode == java.awt.event.KeyEvent.VK_OPEN_BRACKET) -> {
                        viewModel.rotateSelectedBy(-15f)
                        true
                    }
                    boardState.selectedElementIds.isNotEmpty() && (keyEvent.key == Key.RightBracket || rawKeyCode == java.awt.event.KeyEvent.VK_CLOSE_BRACKET) -> {
                        viewModel.rotateSelectedBy(15f)
                        true
                    }
                    // Выделить всё (Ctrl+A / Ctrl+Ф)
                    isCtrl && (keyEvent.key == Key.A || rawKeyCode == java.awt.event.KeyEvent.VK_A) -> {
                        viewModel.selectAll()
                        true
                    }
                    // Подогнать масштаб под всё содержимое (Ctrl+1)
                    isCtrl && (keyEvent.key == Key.One || rawKeyCode == java.awt.event.KeyEvent.VK_1) -> {
                        viewModel.zoomToFit(windowState.size.width.value, windowState.size.height.value)
                        true
                    }
                    // Удаление
                    keyEvent.key == Key.Delete || keyEvent.key == Key.Backspace ||
                    rawKeyCode == java.awt.event.KeyEvent.VK_DELETE || rawKeyCode == java.awt.event.KeyEvent.VK_BACK_SPACE -> {
                        viewModel.deleteSelectedElements()
                        true
                    }
                    // Быстрый выбор инструментов:
                    !isCtrl && (keyEvent.key == Key.One || rawKeyCode == java.awt.event.KeyEvent.VK_1 || keyEvent.key == Key.V || rawKeyCode == java.awt.event.KeyEvent.VK_V) -> {
                        viewModel.selectTool(CanvasTool.SELECT)
                        true
                    }
                    !isCtrl && (keyEvent.key == Key.Q || rawKeyCode == java.awt.event.KeyEvent.VK_Q || keyEvent.key == Key.P || rawKeyCode == java.awt.event.KeyEvent.VK_P) -> {
                        viewModel.selectTool(CanvasTool.PEN)
                        true
                    }
                    !isCtrl && (keyEvent.key == Key.E || rawKeyCode == java.awt.event.KeyEvent.VK_E) -> {
                        viewModel.selectTool(CanvasTool.ERASER)
                        true
                    }
                    !isCtrl && (keyEvent.key == Key.H || rawKeyCode == java.awt.event.KeyEvent.VK_H) -> {
                        viewModel.selectTool(CanvasTool.HAND)
                        true
                    }
                    !isCtrl && (keyEvent.key == Key.S || rawKeyCode == java.awt.event.KeyEvent.VK_S) -> {
                        viewModel.selectTool(CanvasTool.STICKY_NOTE)
                        true
                    }
                    !isCtrl && (keyEvent.key == Key.K || rawKeyCode == java.awt.event.KeyEvent.VK_K) -> {
                        viewModel.selectTool(CanvasTool.KANBAN)
                        true
                    }
                    !isCtrl && (keyEvent.key == Key.R || rawKeyCode == java.awt.event.KeyEvent.VK_R) -> {
                        viewModel.selectTool(CanvasTool.SHAPE)
                        true
                    }
                    !isCtrl && (keyEvent.key == Key.C || rawKeyCode == java.awt.event.KeyEvent.VK_C) -> {
                        viewModel.selectTool(CanvasTool.CONNECTOR)
                        true
                    }
                    !isCtrl && (keyEvent.key == Key.M || rawKeyCode == java.awt.event.KeyEvent.VK_M) -> {
                        viewModel.selectTool(CanvasTool.MILESTONE)
                        true
                    }
                    !isCtrl && (keyEvent.key == Key.G || rawKeyCode == java.awt.event.KeyEvent.VK_G) -> {
                        viewModel.selectTool(CanvasTool.FUNCTION_GRAPH)
                        true
                    }
                    !isCtrl && (keyEvent.key == Key.L || rawKeyCode == java.awt.event.KeyEvent.VK_L) -> {
                        viewModel.selectTool(CanvasTool.LASER_POINTER)
                        true
                    }
                    !isCtrl && (keyEvent.key == Key.T || rawKeyCode == java.awt.event.KeyEvent.VK_T) -> {
                        viewModel.selectTool(CanvasTool.TEXT)
                        true
                    }
                    !isCtrl && (keyEvent.key == Key.F || rawKeyCode == java.awt.event.KeyEvent.VK_F) -> {
                        viewModel.selectTool(CanvasTool.FRAME)
                        true
                    }
                    !isCtrl && (keyEvent.key == Key.X || rawKeyCode == java.awt.event.KeyEvent.VK_X) -> {
                        viewModel.selectTool(CanvasTool.LATEX)
                        true
                    }
                    !isCtrl && (keyEvent.key == Key.B || rawKeyCode == java.awt.event.KeyEvent.VK_B) -> {
                        viewModel.selectTool(CanvasTool.PHYSICS_STENCIL)
                        true
                    }
                    isCtrl && isShift && (keyEvent.key == Key.E || rawKeyCode == java.awt.event.KeyEvent.VK_E) -> {
                        if (boardState.selectedElementIds.isNotEmpty()) {
                            val target = FileDialogs.exportPngFileDialog(defaultName = "${boardState.title}_selection.png")
                            if (target != null) {
                                viewModel.exportSelectionToPng(target)
                            }
                        }
                        true
                    }
                    isCtrl && isShift && (keyEvent.key == Key.K || rawKeyCode == java.awt.event.KeyEvent.VK_K) -> {
                        viewModel.createKanbanBoard(getPasteTargetWorldPos())
                        true
                    }
                    isCtrl && isShift && (keyEvent.key == Key.O || rawKeyCode == java.awt.event.KeyEvent.VK_O) -> {
                        val file = FileDialogs.openDocumentFileDialog()
                        if (file != null) {
                            val center = getPasteTargetWorldPos()
                            viewModel.importDocumentAt(file, center)
                        }
                        true
                    }
                    else -> false
                }
            } else false
        }
    ) {
        LkboardTheme(appTheme = currentTheme) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (currentScreen) {
                AppScreen.DASHBOARD -> {
                    DashboardScreen(
                        boards = boardsList,
                        activeBoardId = activeBoardId,
                        onOpenBoard = viewModel::openBoard,
                        onCreateBoard = viewModel::createNewBoard,
                        onDuplicateBoard = viewModel::duplicateBoard,
                        onRenameBoard = viewModel::renameBoard,
                        onDeleteBoard = viewModel::deleteBoard,
                        onImportBoard = {
                            val file = FileDialogs.openBoardFileDialog(window)
                            if (file != null) {
                                viewModel.importExternalBoard(file)
                            }
                        },
                        updateState = updateState,
                        onCheckUpdates = { viewModel.checkForUpdates(isManual = true) },
                        onOpenUpdateDialog = viewModel::openUpdateDialog,
                        onOpenSettings = viewModel::openSettingsDialog,
                        onOpenVersionDetails = { showVersionDetailsDialog = true }
                    )
                }

                AppScreen.CANVAS -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { size ->
                                viewModel.updateCanvasDimensions(size.width.toFloat(), size.height.toFloat())
                            }
                    ) {
                        // 1. Бесконечный холст с поддержкой рисования стилусом и inline-редактирования
                        InfiniteCanvas(
                            state = boardState,
                            onTransformChange = viewModel::updateTransform,
                            onPanBy = viewModel::panBy,
                            onSelectElement = viewModel::selectElement,
                            onSelectElements = viewModel::selectElements,
                            onClearSelection = viewModel::clearSelection,
                            onMoveSelectedLive = viewModel::moveSelectedElementsLive,
                            onCommitMove = viewModel::commitMove,
                            onResizeElementLive = viewModel::resizeElementLive,
                            onCommitResize = viewModel::commitResize,
                            onRotateElementsLive = viewModel::rotateElementsLive,
                            onCommitRotate = viewModel::commitRotate,
                            onDoubleTapElement = { element ->
                                when (element) {
                                    is BoardElement.DocumentFrame -> {
                                        val file = FileDialogs.openDocumentFileDialog(window)
                                        if (file != null) {
                                            viewModel.loadDocumentIntoFrame(element.id, file)
                                        }
                                    }
                                    is BoardElement.MilestoneCard -> {
                                        viewModel.startEditingMilestone(element)
                                    }
                                    is BoardElement.FunctionGraph -> {
                                        viewModel.startEditingFunctionGraph(element)
                                    }
                                    is BoardElement.StickyNote -> {
                                        viewModel.openStickyNoteEditor(element)
                                    }
                                    is BoardElement.LatexFormula -> {
                                        viewModel.openLatexDialog(element)
                                    }
                                    else -> {
                                        viewModel.startEditingElement(element)
                                    }
                                }
                            },
                            onCreateStickyNote = viewModel::createStickyNote,
                            onCreateShape = viewModel::createShape,
                            onCreateText = viewModel::createText,
                            onCreateDocumentFrame = viewModel::createDocumentFrame,
                            onCreateMilestoneCard = viewModel::createMilestoneCard,
                            onCreateFunctionGraph = viewModel::createFunctionGraph,
                            onCreateLatexFormula = { pos -> viewModel.openLatexDialog(position = pos) },
                            onCreatePhysicsStencil = { pos -> viewModel.openPhysicsStencilDialog(position = pos) },
                            onCreateKanbanBoard = viewModel::createKanbanBoard,
                            onToggleStickyTodo = viewModel::toggleStickyTodo,
                            onCreateConnector = viewModel::createConnector,
                            onCycleMilestoneStatus = viewModel::cycleMilestoneStatus,
                            onLaserPointMoved = viewModel::addLaserPoint,
                            workshopTimerState = workshopTimerState,
                            onTimerStart = viewModel::startTimer,
                            onTimerPause = viewModel::pauseTimer,
                            onTimerReset = viewModel::resetTimer,
                            onTimerAddMinutes = viewModel::addTimerMinutes,
                            onTimerSetPreset = viewModel::setTimerPreset,
                            onTimerToggleMode = viewModel::toggleTimerMode,
                            onTimerToggleMinimize = viewModel::toggleTimerMinimized,
                            onTimerClose = viewModel::toggleTimerVisibility,
                            activeGuides = activeGuides,
                            laserTrail = laserTrail,
                            onPruneLaserTrail = viewModel::pruneLaserTrail,
                            isPresentationMode = isPresentationMode,
                            onNextPresentationSlide = viewModel::nextPresentationSlide,
                            onPrevPresentationSlide = viewModel::prevPresentationSlide,
                            onToggleLaserPointer = viewModel::toggleLaserPointer,
                            onExitPresentationMode = { viewModel.setPresentationMode(false) },
                            onFrameNextPage = viewModel::frameNextPage,
                            onFramePrevPage = viewModel::framePrevPage,
                            onLoadDocumentIntoFrame = viewModel::loadDocumentIntoFrame,
                            onAddDrawingPath = viewModel::addDrawingPath,
                            onDeleteElements = viewModel::deleteElements,
                            onSpaceStateChange = viewModel::setSpacePressed,
                            editingElement = editingElement,
                            onCommitTextEdit = viewModel::commitTextEdit,
                            onCommitTextElementEdit = viewModel::commitTextElementEdit,
                            onCancelTextEdit = viewModel::cancelTextEdit,
                            onUpdateSelectedTextProperties = viewModel::updateSelectedTextProperties,
                            onUpdateSelectedLatexProperties = viewModel::updateSelectedLatexProperties,
                            onEditSelectedLatex = { (boardState.selectedElements.singleOrNull() as? BoardElement.LatexFormula)?.let { viewModel.openLatexDialog(it) } },
                            onStartEditingSelectedText = { (boardState.selectedElements.singleOrNull() as? BoardElement.Text)?.let { viewModel.startEditingElement(it) } },
                            onCursorPositionChange = { currentCursorScreenPos = it },
                            remotePointers = collabState.remotePointers,
                            modifier = Modifier.fillMaxSize()
                        )

                        val selectedShape = boardState.selectedShape
                        val selectedShapeSettings = selectedShape?.let {
                            com.lkboard.app.model.ShapeSettings(
                                shapeType = it.shapeType,
                                strokeColorHex = it.strokeColorHex,
                                fillColorHex = it.fillColorHex,
                                strokeWidth = it.strokeWidth,
                                strokeStyle = it.strokeStyle,
                                cornerRadius = it.cornerRadius
                            )
                        }

                        // 2. Стеклянный интерфейс Apple Glassmorphism (TopMenuBar, LeftToolbar, Quick Actions, Zoom HUD)
                        GlassToolbar(
                            boardTitle = boardState.title,
                            onNavigateToDashboard = viewModel::navigateToDashboard,
                            onInsertRoadmap = {
                                val center = getPasteTargetWorldPos()
                                viewModel.insertRoadmapTemplate(center)
                            },
                            onInsertKanban = {
                                val center = getPasteTargetWorldPos()
                                viewModel.createKanbanBoard(center)
                            },
                            onToggleTimer = viewModel::toggleTimerVisibility,
                            onOpenShareDialog = viewModel::openShareDialog,
                            isCollabActive = collabState.isActive,
                            collabClientsCount = collabState.connectedPeers.size,
                            scale = boardState.transform.scale,
                            activeTool = boardState.activeTool,
                            penSettings = boardState.penSettings,
                            shapeSettings = boardState.shapeSettings,
                            hasSelectedShape = selectedShape != null,
                            selectedShapeSettings = selectedShapeSettings,
                            canUndo = canUndo,
                            canRedo = canRedo,
                            hasSelection = boardState.selectedElementIds.isNotEmpty(),
                            onToolSelect = viewModel::selectTool,
                            onPenSettingsChange = viewModel::updatePenSettings,
                            onShapeSettingsChange = viewModel::updateShapeSettings,
                            onRotateSelectedBy = viewModel::rotateSelectedBy,
                            onUndo = viewModel::undo,
                            onRedo = viewModel::redo,
                            onCopy = viewModel::copySelected,
                            onPaste = { viewModel.pasteFromClipboard(getPasteTargetWorldPos()) },
                            onDeleteSelected = viewModel::deleteSelectedElements,
                            onDuplicateSelected = viewModel::duplicateSelectedElements,
                            onZoomIn = {
                                val center = Offset(
                                    windowState.size.width.value / 2f,
                                    windowState.size.height.value / 2f
                                )
                                viewModel.zoomIn(center)
                            },
                            onZoomOut = {
                                val center = Offset(
                                    windowState.size.width.value / 2f,
                                    windowState.size.height.value / 2f
                                )
                                viewModel.zoomOut(center)
                            },
                            onResetZoom = {
                                val center = Offset(
                                    windowState.size.width.value / 2f,
                                    windowState.size.height.value / 2f
                                )
                                viewModel.resetZoom(center)
                            },
                            onZoomToFit = {
                                viewModel.zoomToFit(windowState.size.width.value, windowState.size.height.value)
                            },
                            gridType = boardState.gridType,
                            onCycleGridType = { viewModel.cycleGridType() },
                            onGridTypeChange = { viewModel.setGridType(it) },
                            backgroundColorHex = boardState.backgroundColorHex,
                            onBackgroundColorChange = viewModel::setBackgroundColor,
                            onImportDocument = {
                                val file = FileDialogs.openDocumentFileDialog(window)
                                if (file != null) {
                                    val center = getPasteTargetWorldPos()
                                    viewModel.importDocumentAt(file, center)
                                }
                            },
                            onNewBoard = { viewModel.newBoard() },
                            onOpenBoard = {
                                val file = FileDialogs.openBoardFileDialog(window)
                                if (file != null) {
                                    viewModel.loadBoard(file)
                                }
                            },
                            onSaveBoard = {
                                val target = currentFile ?: FileDialogs.saveBoardFileDialog(window, "${boardState.title}.lkboard")
                                if (target != null) {
                                    viewModel.saveBoard(target)
                                }
                            },
                            onSaveBoardAs = {
                                val target = FileDialogs.saveBoardFileDialog(window, "${boardState.title}.lkboard")
                                if (target != null) {
                                    viewModel.saveBoard(target)
                                }
                            },
                            onExportPng = {
                                val target = FileDialogs.exportPngFileDialog(window, "${boardState.title}.png")
                                if (target != null) {
                                    PngExporter.exportToPng(target, boardState)
                                }
                            },
                            onExportSvg = {
                                val target = FileDialogs.exportSvgFileDialog(window, "${boardState.title}.svg")
                                if (target != null) {
                                    viewModel.exportToSvg(target)
                                }
                            },
                            onExportPdf = {
                                val target = FileDialogs.exportPdfFileDialog(window, "${boardState.title}.pdf")
                                if (target != null) {
                                    viewModel.exportToPdf(target)
                                }
                            },
                            onCopyBoardImage = viewModel::copyBoardImageToClipboard,
                            onTogglePresentation = viewModel::togglePresentationMode,
                            onToggleLaser = viewModel::toggleLaserPointer,
                            isPresentationMode = isPresentationMode,
                            onExit = handleExit,
                            onSelectAll = { viewModel.selectAll() },
                            enabledTools = boardState.enabledTools,
                            onExportSelectionPng = {
                                if (boardState.selectedElementIds.isNotEmpty()) {
                                    val target = FileDialogs.exportPngFileDialog(window, "${boardState.title}_selection.png")
                                    if (target != null) {
                                        viewModel.exportSelectionToPng(target)
                                    }
                                }
                            },
                            onOpenSettings = viewModel::openSettingsDialog,
                            onOpenVersionDetails = { showVersionDetailsDialog = true }
                        )

                        // 3. Модальный редактор для стикеров (Apple Liquid Glass с палитрой и чекбоксами To-Do)
                        if (editingStickyNote != null) {
                            StickyNoteEditDialog(
                                note = editingStickyNote!!,
                                onDismiss = viewModel::closeStickyNoteEditor,
                                onSave = viewModel::saveStickyNote,
                                onDelete = { id ->
                                    val note = editingStickyNote
                                    viewModel.closeStickyNoteEditor()
                                    if (note != null) {
                                        viewModel.deleteElements(listOf(note))
                                    }
                                }
                            )
                        } else if (editingElement is BoardElement.StickyNote) {
                            TextEditDialog(
                                element = editingElement!!,
                                onSave = viewModel::commitTextEdit,
                                onSaveDetailed = viewModel::commitTextElementEdit,
                                onDismiss = viewModel::cancelTextEdit
                            )
                        }

                        // 4. Модальный редактор для вех дорожной карты
                        if (editingMilestone != null) {
                            MilestoneEditDialog(
                                element = editingMilestone!!,
                                onSave = viewModel::updateMilestoneCard,
                                onDismiss = viewModel::closeMilestoneDialog
                            )
                        }

                        // 5. Модальный редактор для математических графиков
                        if (editingFunctionGraph != null) {
                            FunctionGraphEditDialog(
                                element = editingFunctionGraph!!,
                                onSave = viewModel::updateFunctionGraph,
                                onDismiss = viewModel::closeFunctionGraphDialog
                            )
                        }

                        // 6. Диалог совместной работы и шеринга ссылки на доску
                        if (isShareDialogOpen) {
                            ShareBoardDialog(
                                collabState = collabState,
                                boardTitle = boardState.title,
                                onStartHosting = viewModel::startHosting,
                                onStopHosting = viewModel::stopHosting,
                                onConnectAsClient = viewModel::connectAsClient,
                                onDisconnectClient = viewModel::disconnectClient,
                                onDismiss = viewModel::closeShareDialog
                            )
                        }

                        // 7. Диалог создания и редактирования формул LaTeX
                        if (isLatexDialogOpen) {
                            LatexEditDialog(
                                initialFormula = editingLatexFormula?.formula ?: "\\int_{0}^{\\infty} e^{-x^2} dx = \\frac{\\sqrt{\\pi}}{2}",
                                initialFontSize = editingLatexFormula?.fontSize ?: 24f,
                                onDismiss = viewModel::closeLatexDialog,
                                onSave = { formula, fontSize -> viewModel.saveLatexFormula(formula, fontSize) }
                            )
                        }

                        // 8. Физический и инженерный конструктор (Stencils)
                        if (isPhysicsStencilDialogOpen) {
                            PhysicsStencilDialog(
                                onDismiss = viewModel::closePhysicsStencilDialog,
                                onSelectStencil = viewModel::addPhysicsStencil
                            )
                        }
                    }
                }
            }

            // Диалог настроек темы и набора инструментов (доступен и с холста, и с дашборда)
            if (isSettingsDialogOpen) {
                SettingsDialog(
                    currentTheme = currentTheme,
                    currentRole = currentUserRole,
                    enabledTools = boardState.enabledTools,
                    isTabletModeActive = isTabletModeActive,
                    tabletWebLink = collabState.tabletWebLink,
                    connectedTabletClientsCount = collabState.connectedPeers.count { it.clientType == "Tablet" || it.id.contains("tablet") },
                    recentTabletActions = collabState.recentTabletActions,
                    isFocusLocked = collabState.isFocusLocked,
                    stylusPressureCurve = collabState.stylusPressureCurve,
                    isConnectChimeEnabled = collabState.isConnectChimeEnabled,
                    onToggleTabletMode = viewModel::toggleTabletMode,
                    onToggleFocusLock = viewModel::toggleFocusLock,
                    onChangeStylusPressureCurve = viewModel::setStylusPressureCurve,
                    onToggleConnectChime = viewModel::setConnectChimeEnabled,
                    onDismiss = viewModel::closeSettingsDialog,
                    onSaveSettings = viewModel::applySettings
                )
            }

            // Онбординг при первом запуске (выбор роли и фильтрация инструментов)
            if (isOnboardingOpen) {
                FirstRunOnboardingDialog(
                    initialRole = currentUserRole,
                    onComplete = viewModel::completeOnboarding
                )
            }

            if (showUpdateDialog) {
                UpdateDialog(
                    state = updateState,
                    onDismiss = viewModel::dismissUpdateDialog
                )
            }

                if (showVersionDetailsDialog) {
                    VersionDetailsDialog(
                        onDismiss = { showVersionDetailsDialog = false },
                        onCheckForUpdates = { viewModel.checkForUpdates(isManual = true) }
                    )
                }
            }
        }

    }
}
