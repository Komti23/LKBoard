package com.lkboard.app.ui.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.lkboard.app.history.AddElementCommand
import com.lkboard.app.history.BoardCommand
import com.lkboard.app.history.DeleteElementsCommand
import com.lkboard.app.history.DuplicateElementsCommand
import com.lkboard.app.history.HistoryManager
import com.lkboard.app.history.MoveElementsCommand
import com.lkboard.app.history.ResizeElementCommand
import com.lkboard.app.history.RotateElementsCommand
import com.lkboard.app.history.UpdateTextCommand
import com.lkboard.app.model.BoardElement
import com.lkboard.app.model.BoardState
import com.lkboard.app.model.CanvasPoint
import com.lkboard.app.model.CanvasTool
import com.lkboard.app.model.CanvasTransform
import com.lkboard.app.model.GridType
import com.lkboard.app.model.PenSettings
import com.lkboard.app.model.ShapeType
import com.lkboard.app.model.ShapeSettings
import com.lkboard.app.model.PhysicsStencilType
import com.lkboard.app.data.AppPreferences
import com.lkboard.app.data.AppTheme
import com.lkboard.app.data.UserRole
import com.lkboard.app.data.PreferencesManager
import com.lkboard.app.util.DocumentImporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import com.lkboard.app.data.BoardManager
import com.lkboard.app.data.BoardItemInfo
import com.lkboard.app.data.BoardTemplate
import com.lkboard.app.data.BoardTemplateGenerator
import com.lkboard.app.history.AddElementsCommand
import com.lkboard.app.model.AlignmentGuide
import com.lkboard.app.model.SmartGuides
import com.lkboard.app.model.LaserPoint
import com.lkboard.app.model.LaserPointerManager
import com.lkboard.app.model.MilestoneStatus
import com.lkboard.app.model.ConnectorStyle
import com.lkboard.app.model.AnchorPoint
import com.lkboard.app.export.SvgExporter
import com.lkboard.app.export.PdfExporter
import com.lkboard.app.export.PngExporter
import com.lkboard.app.timer.AudioChimePlayer
import com.lkboard.app.timer.WorkshopTimerState
import com.lkboard.app.collaboration.CollabManager
import com.lkboard.app.collaboration.CollabSessionState
import com.lkboard.app.collaboration.NetworkUtils
import com.lkboard.app.data.BoardSerializer
import com.lkboard.app.update.UpdateCheckState
import com.lkboard.app.update.UpdateChecker
import com.lkboard.app.update.UpdateInfo
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Режим экрана приложения Lkboard.
 */
enum class AppScreen {
    DASHBOARD, // Главный экран: список всех досок, поиск, выбор шаблонов
    CANVAS     // Рабочий экран: бесконечный холст выбранной доски
}

/**
 * ViewModel управления состоянием бесконечной доски (MVI/MVVM архитектура)
 * с поддержкой множественных досок, главного экрана (Dashboard),
 * стека отмены действий (Undo/Redo), интерактивных манипуляторов
 * и автоматического сохранения состояния холста и настроек при закрытии/открытии.
 */
class BoardViewModel {

    val boardManager = BoardManager()

    private val historyManager = HistoryManager()
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var autoSaveJob: Job? = null

    private val _currentScreen = MutableStateFlow(AppScreen.DASHBOARD)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _boardsList = MutableStateFlow<List<BoardItemInfo>>(emptyList())
    val boardsList: StateFlow<List<BoardItemInfo>> = _boardsList.asStateFlow()

    private val _activeBoardId = MutableStateFlow<String?>(null)
    val activeBoardId: StateFlow<String?> = _activeBoardId.asStateFlow()

    private val _state = MutableStateFlow(loadOrCreateInitialState())
    val state: StateFlow<BoardState> = _state.asStateFlow()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _editingElement = MutableStateFlow<BoardElement?>(null)
    val editingElement: StateFlow<BoardElement?> = _editingElement.asStateFlow()

    private val _currentFile = MutableStateFlow<java.io.File?>(null)
    val currentFile: StateFlow<java.io.File?> = _currentFile.asStateFlow()

    private val _isPresentationMode = MutableStateFlow(false)
    val isPresentationMode: StateFlow<Boolean> = _isPresentationMode.asStateFlow()

    private val _isLaserPointerActive = MutableStateFlow(false)
    val isLaserPointerActive: StateFlow<Boolean> = _isLaserPointerActive.asStateFlow()

    private val _laserTrail = MutableStateFlow<List<LaserPoint>>(emptyList())
    val laserTrail: StateFlow<List<LaserPoint>> = _laserTrail.asStateFlow()

    private val _activeGuides = MutableStateFlow<List<AlignmentGuide>>(emptyList())
    val activeGuides: StateFlow<List<AlignmentGuide>> = _activeGuides.asStateFlow()

    private val _editingMilestone = MutableStateFlow<BoardElement.MilestoneCard?>(null)
    val editingMilestone: StateFlow<BoardElement.MilestoneCard?> = _editingMilestone.asStateFlow()

    private val _editingFunctionGraph = MutableStateFlow<BoardElement.FunctionGraph?>(null)
    val editingFunctionGraph: StateFlow<BoardElement.FunctionGraph?> = _editingFunctionGraph.asStateFlow()

    private val _editingStickyNote = MutableStateFlow<BoardElement.StickyNote?>(null)
    val editingStickyNote: StateFlow<BoardElement.StickyNote?> = _editingStickyNote.asStateFlow()

    private val _workshopTimerState = MutableStateFlow(WorkshopTimerState())
    val workshopTimerState: StateFlow<WorkshopTimerState> = _workshopTimerState.asStateFlow()

    private val _isShareDialogOpen = MutableStateFlow(false)
    val isShareDialogOpen: StateFlow<Boolean> = _isShareDialogOpen.asStateFlow()

    private val _updateState = MutableStateFlow(UpdateCheckState())
    val updateState: StateFlow<UpdateCheckState> = _updateState.asStateFlow()

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    private val _currentTheme = MutableStateFlow(AppPreferences.loadAppTheme())
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    private val _currentUserRole = MutableStateFlow(AppPreferences.loadUserRole())
    val currentUserRole: StateFlow<UserRole> = _currentUserRole.asStateFlow()

    private val _isOnboardingOpen = MutableStateFlow(AppPreferences.isFirstRun())
    val isOnboardingOpen: StateFlow<Boolean> = _isOnboardingOpen.asStateFlow()

    private val _isSettingsDialogOpen = MutableStateFlow(false)
    val isSettingsDialogOpen: StateFlow<Boolean> = _isSettingsDialogOpen.asStateFlow()

    private val _isLatexDialogOpen = MutableStateFlow(false)
    val isLatexDialogOpen: StateFlow<Boolean> = _isLatexDialogOpen.asStateFlow()

    private val _editingLatexFormula = MutableStateFlow<BoardElement.LatexFormula?>(null)
    val editingLatexFormula: StateFlow<BoardElement.LatexFormula?> = _editingLatexFormula.asStateFlow()

    private var pendingLatexPosition: Offset = Offset(200f, 200f)

    private val _isPhysicsStencilDialogOpen = MutableStateFlow(false)
    val isPhysicsStencilDialogOpen: StateFlow<Boolean> = _isPhysicsStencilDialogOpen.asStateFlow()

    private var pendingPhysicsStencilPosition: Offset = Offset(200f, 200f)

    val collabManager: CollabManager = CollabManager(
        snapshotProvider = { BoardSerializer.stateToDocument(_state.value) },
        onRemoteElementUpsert = { el ->
            _state.update { it.withElement(el) }
        },
        onRemoteElementDelete = { ids ->
            _state.update { it.withoutElements(ids.toSet()) }
        },
        onRemoteSnapshotReceived = { doc ->
            _state.update { BoardSerializer.documentToState(doc) }
        },
        onRemoteToggleStickyTodo = { noteId, lineIndex ->
            toggleStickyTodo(noteId, lineIndex, broadcast = false)
        }
    )
    val collabState: StateFlow<CollabSessionState> = collabManager.state

    init {
        refreshBoardsList()
        val index = boardManager.getIndex()
        val targetId = index.lastActiveBoardId ?: index.boards.firstOrNull()?.id
        if (targetId != null) {
            val pair = boardManager.loadBoard(targetId)
            if (pair != null) {
                val (info, loadedState) = pair
                _activeBoardId.value = info.id
                _currentFile.value = boardManager.getBoardFile(info)
                _state.value = loadedState
            }
        }

        // Фоновый тикер для плавающего таймера воркшопов
        ioScope.launch {
            while (isActive) {
                delay(1000L)
                val current = _workshopTimerState.value
                if (current.isRunning) {
                    if (current.isStopwatch) {
                        _workshopTimerState.update { it.copy(remainingSeconds = it.remainingSeconds + 1) }
                    } else {
                        val next = current.remainingSeconds - 1
                        if (next <= 0) {
                            _workshopTimerState.update { it.copy(remainingSeconds = 0, isRunning = false, isFinished = true) }
                            AudioChimePlayer.playChime()
                        } else {
                            _workshopTimerState.update { it.copy(remainingSeconds = next) }
                        }
                    }
                }
            }
        }

        // Автоматическая проверка обновлений в фоновом потоке при старте
        ioScope.launch {
            delay(1500L)
            checkForUpdates(isManual = false)
        }
    }

    fun checkForUpdates(isManual: Boolean = false) {
        ioScope.launch {
            _updateState.update { it.copy(isChecking = true, isManualCheck = isManual, errorMessage = null) }
            val newUpdate = UpdateChecker.checkForUpdates()
            if (newUpdate != null) {
                _updateState.update {
                    it.copy(
                        isChecking = false,
                        updateAvailable = true,
                        updateInfo = newUpdate,
                        isManualCheck = isManual
                    )
                }
                _showUpdateDialog.value = true
            } else {
                _updateState.update {
                    it.copy(
                        isChecking = false,
                        updateAvailable = false,
                        updateInfo = null,
                        isManualCheck = isManual,
                        isUpToDateMessageShown = isManual
                    )
                }
                if (isManual) {
                    _showUpdateDialog.value = true
                }
            }
        }
    }

    fun dismissUpdateDialog() {
        _showUpdateDialog.value = false
    }

    fun openUpdateDialog() {
        _showUpdateDialog.value = true
    }

    val selectionManager: SelectionManager = SelectionManager(
        getState = { _state.value },
        onExecuteCommand = ::executeCommand,
        onUpdateState = { updater ->
            _state.update(updater)
            triggerAutoSave()
        }
    )

    fun refreshBoardsList() {
        _boardsList.value = boardManager.listBoards()
    }

    fun openBoard(id: String) {
        autoSaveSync()
        val pair = boardManager.loadBoard(id)
        if (pair != null) {
            val (info, loadedState) = pair
            _activeBoardId.value = info.id
            _currentFile.value = boardManager.getBoardFile(info)
            historyManager.clear()
            _state.value = loadedState
            updateHistoryFlags()
            _currentScreen.value = AppScreen.CANVAS
        }
    }

    fun navigateToDashboard() {
        autoSaveSync()
        refreshBoardsList()
        _currentScreen.value = AppScreen.DASHBOARD
    }

    fun createNewBoard(title: String, template: BoardTemplate = BoardTemplate.BLANK) {
        autoSaveSync()
        val (info, newState) = boardManager.createBoard(title, template)
        _activeBoardId.value = info.id
        _currentFile.value = boardManager.getBoardFile(info)
        historyManager.clear()
        _state.value = newState
        updateHistoryFlags()
        refreshBoardsList()
        _currentScreen.value = AppScreen.CANVAS
    }

    fun duplicateBoard(id: String) {
        boardManager.duplicateBoard(id)
        refreshBoardsList()
    }

    fun renameBoard(id: String, newTitle: String) {
        boardManager.renameBoard(id, newTitle)
        if (_activeBoardId.value == id) {
            _state.update { it.copy(title = newTitle) }
        }
        refreshBoardsList()
    }

    fun deleteBoard(id: String) {
        boardManager.deleteBoard(id)
        if (_activeBoardId.value == id) {
            _activeBoardId.value = null
        }
        refreshBoardsList()
    }

    fun importExternalBoard(file: java.io.File) {
        val pair = boardManager.importBoardFile(file)
        if (pair != null) {
            val (info, loadedState) = pair
            _activeBoardId.value = info.id
            _currentFile.value = boardManager.getBoardFile(info)
            historyManager.clear()
            _state.value = loadedState
            updateHistoryFlags()
            refreshBoardsList()
            _currentScreen.value = AppScreen.CANVAS
        }
    }

    fun insertRoadmapTemplate(centerWorldPos: Offset = Offset(200f, 200f)) {
        val roadmapState = BoardTemplateGenerator.generate(BoardTemplate.ROADMAP, "Дорожная карта")
        if (roadmapState.elements.isEmpty()) return
        val currentMinX = roadmapState.elements.values.minOf { it.x }
        val currentMinY = roadmapState.elements.values.minOf { it.y }
        val deltaX = centerWorldPos.x - currentMinX
        val deltaY = centerWorldPos.y - currentMinY

        val shiftedElements = roadmapState.elements.values.map { el ->
            val newId = UUID.randomUUID().toString()
            when (el) {
                is BoardElement.Shape -> el.copy(id = newId, x = el.x + deltaX, y = el.y + deltaY)
                is BoardElement.Text -> el.copy(id = newId, x = el.x + deltaX, y = el.y + deltaY)
                is BoardElement.StickyNote -> el.copy(id = newId, x = el.x + deltaX, y = el.y + deltaY)
                else -> el.translate(deltaX, deltaY)
            }
        }
        executeCommand(AddElementsCommand(shiftedElements, selectAfterAdd = true))
    }

    /**
     * Немедленная синхронная запись состояния холста и всех настроек в файл автосохранения.
     */
    fun autoSaveSync() {
        try {
            val curId = _activeBoardId.value
            if (curId != null) {
                boardManager.saveBoard(curId, _state.value)
            }
            val file = AppPreferences.getAutoSaveFile()
            com.lkboard.app.data.BoardSerializer.save(file, _state.value)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Триггер автосохранения с дебаунсом для частых действий (панорамирование, перемещение)
     * или немедленным сохранением для дискретных операций.
     */
    fun triggerAutoSave(immediate: Boolean = false) {
        if (immediate) {
            autoSaveJob?.cancel()
            autoSaveSync()
            return
        }
        autoSaveJob?.cancel()
        autoSaveJob = ioScope.launch {
            delay(350)
            autoSaveSync()
        }
    }

    fun newBoard() {
        createNewBoard("Новая доска", BoardTemplate.BLANK)
    }

    fun updateShapeSettings(settings: ShapeSettings) {
        selectionManager.updateShapeSettings(settings)
        triggerAutoSave(immediate = true)
    }

    fun loadBoard(file: java.io.File) {
        importExternalBoard(file)
    }

    fun saveBoard(file: java.io.File) {
        com.lkboard.app.data.BoardSerializer.save(file, _state.value)
        _currentFile.value = file
        _state.update { it.copy(title = file.nameWithoutExtension) }
        autoSaveSync()
    }

    fun selectAll() {
        _state.update {
            it.copy(
                activeTool = CanvasTool.SELECT,
                selectedElementIds = it.elements.keys
            )
        }
    }

    fun zoomToFit(viewportWidth: Float, viewportHeight: Float) {
        val elements = _state.value.elements.values
        if (elements.isEmpty()) return
        val bounds = com.lkboard.app.model.GeometryUtils.computeBounds(elements) ?: return
        val padding = 80f
        val availableW = (viewportWidth - padding * 2).coerceAtLeast(100f)
        val availableH = (viewportHeight - padding * 2).coerceAtLeast(100f)
        val scaleX = availableW / bounds.width
        val scaleY = availableH / bounds.height
        val targetScale = kotlin.math.min(scaleX, scaleY).coerceIn(CanvasTransform.MIN_SCALE, CanvasTransform.MAX_SCALE)

        val newPanX = (viewportWidth / 2f) - bounds.center.x * targetScale
        val newPanY = (viewportHeight / 2f) - bounds.center.y * targetScale

        _state.update { it.withTransform(CanvasTransform(newPanX, newPanY, targetScale)) }
        triggerAutoSave(immediate = false)
    }

    private fun updateHistoryFlags() {
        _canUndo.value = historyManager.canUndo
        _canRedo.value = historyManager.canRedo
    }

    private fun executeCommand(command: BoardCommand) {
        _state.update { current ->
            historyManager.execute(command, current)
        }
        updateHistoryFlags()
        triggerAutoSave(immediate = false)

        when (command) {
            is AddElementCommand -> collabManager.broadcastElementUpsert(command.element)
            is AddElementsCommand -> command.newElements.forEach { collabManager.broadcastElementUpsert(it) }
            is DeleteElementsCommand -> collabManager.broadcastElementDelete(command.removedElements.map { it.id })
            is DuplicateElementsCommand -> command.duplicatedElements.forEach { collabManager.broadcastElementUpsert(it) }
            is MoveElementsCommand -> command.elementIds.forEach { id ->
                _state.value.elements[id]?.let { collabManager.broadcastElementUpsert(it) }
            }
            is ResizeElementCommand -> {
                _state.value.elements[command.elementId]?.let { collabManager.broadcastElementUpsert(it) }
            }
            else -> {}
        }
    }

    fun undo() {
        _state.update { current ->
            historyManager.undo(current)
        }
        updateHistoryFlags()
        triggerAutoSave(immediate = false)
    }

    fun redo() {
        _state.update { current ->
            historyManager.redo(current)
        }
        updateHistoryFlags()
        triggerAutoSave(immediate = false)
    }

    fun updateTransform(transform: CanvasTransform) {
        _state.update { it.withTransform(transform) }
        triggerAutoSave(immediate = false)
    }

    /**
     * Смещает камеру на физическую дельту курсора 1:1 в экранных пикселях (Float).
     */
    fun panBy(delta: Offset) {
        if (delta.x == 0f && delta.y == 0f) return
        _state.update { current ->
            val t = current.transform
            current.withTransform(
                t.copy(
                    panX = t.panX + delta.x,
                    panY = t.panY + delta.y
                )
            )
        }
        triggerAutoSave(immediate = false)
    }

    fun updatePenSettings(settings: PenSettings) {
        _state.update { it.withPenSettings(settings) }
        AppPreferences.savePenSettings(settings)
        PreferencesManager.savePenSettings(settings)
        triggerAutoSave(immediate = true)
    }

    fun copySelected() {
        val selected = _state.value.selectedElements
        if (selected.isNotEmpty()) {
            com.lkboard.app.clipboard.ClipboardManager.copyElements(selected)
        }
    }

    fun pasteFromClipboard(targetWorldPos: Offset) {
        val content = com.lkboard.app.clipboard.ClipboardManager.readFromClipboard() ?: return
        when (content) {
            is com.lkboard.app.clipboard.ClipboardContent.Image -> {
                val imageElement = BoardElement.ImageElement(
                    id = "image-${UUID.randomUUID()}",
                    x = targetWorldPos.x - content.width / 2f,
                    y = targetWorldPos.y - content.height / 2f,
                    width = content.width,
                    height = content.height,
                    zIndex = nextZIndex(),
                    imageBase64 = content.base64
                )
                executeCommand(AddElementCommand(imageElement))
            }
            is com.lkboard.app.clipboard.ClipboardContent.Elements -> {
                val offsetDelta = 20f
                val duplicates = content.elements.map { original ->
                    val newId = "${original.id}-paste-${UUID.randomUUID().toString().take(6)}"
                    original.copyWithPosition(original.x + offsetDelta, original.y + offsetDelta)
                        .let {
                            when (it) {
                                is BoardElement.StickyNote -> it.copy(id = newId, zIndex = nextZIndex())
                                is BoardElement.Shape -> it.copy(id = newId, zIndex = nextZIndex())
                                is BoardElement.Text -> it.copy(id = newId, zIndex = nextZIndex())
                                is BoardElement.DrawingPath -> it.copy(id = newId, zIndex = nextZIndex())
                                is BoardElement.ImageElement -> it.copy(id = newId, zIndex = nextZIndex())
                                is BoardElement.DocumentFrame -> it.copy(id = newId, zIndex = nextZIndex())
                                is BoardElement.Connector -> it.copy(id = newId, zIndex = nextZIndex())
                                is BoardElement.MilestoneCard -> it.copy(id = newId, zIndex = nextZIndex())
                                is BoardElement.FunctionGraph -> it.copy(id = newId, zIndex = nextZIndex())
                                is BoardElement.KanbanBoard -> it.copy(id = newId, zIndex = nextZIndex())
                                is BoardElement.LatexFormula -> it.copy(id = newId, zIndex = nextZIndex())
                                is BoardElement.PhysicsStencil -> it.copy(id = newId, zIndex = nextZIndex())
                            }
                        }
                }
                executeCommand(DuplicateElementsCommand(duplicates))
            }
            is com.lkboard.app.clipboard.ClipboardContent.Text -> {
                val fontSize = 19f
                val (computedWidth, computedHeight) = com.lkboard.app.ui.components.TextLayoutHelper.calculateDimensions(
                    text = content.text,
                    fontSize = fontSize,
                    maxWidth = 600f
                )
                val textElement = BoardElement.Text(
                    id = "text-${UUID.randomUUID()}",
                    x = targetWorldPos.x - computedWidth / 2f,
                    y = targetWorldPos.y - computedHeight / 2f,
                    width = computedWidth,
                    height = computedHeight,
                    text = content.text,
                    colorHex = 0xFF1E1E1E,
                    fontSize = fontSize,
                    zIndex = nextZIndex()
                )
                executeCommand(AddElementCommand(textElement))
            }
        }
    }

    fun selectTool(tool: CanvasTool) {
        _state.update {
            val next = it.withActiveTool(tool)
            if (tool == CanvasTool.PEN) next.clearSelection() else next
        }
    }

    fun setSpacePressed(pressed: Boolean) {
        _state.update { it.withSpacePressed(pressed) }
    }

    fun zoomIn(center: Offset) {
        _state.update {
            it.withTransform(it.transform.zoomAround(center, CanvasTransform.ZOOM_IN_STEP))
        }
        triggerAutoSave(immediate = false)
    }

    fun zoomOut(center: Offset) {
        _state.update {
            it.withTransform(it.transform.zoomAround(center, CanvasTransform.ZOOM_OUT_STEP))
        }
        triggerAutoSave(immediate = false)
    }

    fun resetZoom(center: Offset) {
        _state.update {
            it.withTransform(it.transform.resetZoom(center))
        }
        triggerAutoSave(immediate = false)
    }

    // --- Выделение элементов ---

    fun selectElement(id: String, addToSelection: Boolean) {
        _state.update { current ->
            val newSelection = if (addToSelection) {
                if (id in current.selectedElementIds) {
                    current.selectedElementIds - id
                } else {
                    current.selectedElementIds + id
                }
            } else {
                setOf(id)
            }
            current.withSelection(newSelection)
        }
    }

    fun selectElements(ids: Set<String>) {
        _state.update { it.withSelection(ids) }
    }

    fun clearSelection() {
        _state.update { it.clearSelection() }
    }

    // --- Перемещение (Drag-and-Drop) с умными направляющими (Smart Alignment Guides) ---

    fun moveSelectedElementsLive(deltaX: Float, deltaY: Float) {
        val current = _state.value
        val selected = current.selectedElements
        val others = current.elements.values.filter { it.id !in current.selectedElementIds }
        val selBounds = com.lkboard.app.model.GeometryUtils.computeBounds(selected)

        if (selBounds != null && others.isNotEmpty()) {
            val snapResult = SmartGuides.calculateSnap(selBounds, others, Offset(deltaX, deltaY))
            _activeGuides.value = snapResult.guides
        } else {
            _activeGuides.value = emptyList()
        }

        _state.update { curr ->
            val updated = curr.elements.toMutableMap()
            for (id in curr.selectedElementIds) {
                val el = updated[id] ?: continue
                updated[id] = el.translate(deltaX, deltaY)
            }
            curr.copy(elements = updated)
        }
    }

    fun commitMove(elementIds: Set<String>, totalDeltaX: Float, totalDeltaY: Float) {
        _activeGuides.value = emptyList()
        if (totalDeltaX == 0f && totalDeltaY == 0f) return
        if (elementIds.isEmpty()) return
        val cmd = MoveElementsCommand(elementIds, totalDeltaX, totalDeltaY)
        // Команда уже фактически визуализирована в realtime, поэтому пушим её в стек истории:
        _state.update { current ->
            // Чтобы команда записалась корректно с состоянием ДО движения, отменяем и перевыполняем через historyManager
            val stateBeforeMove = current.copy(
                elements = current.elements.mapValues { (id, el) ->
                    if (id in elementIds) el.translate(-totalDeltaX, -totalDeltaY) else el
                }
            )
            historyManager.execute(cmd, stateBeforeMove)
        }

        // Автоматическое переключение статуса карточек вех при перемещении в колонки канбан-доски:
        _state.update { current ->
            val kanbans = current.elements.values.filterIsInstance<BoardElement.KanbanBoard>()
            if (kanbans.isEmpty()) return@update current
            var updatedMap = current.elements
            var changed = false
            for (id in elementIds) {
                val el = updatedMap[id]
                if (el is BoardElement.MilestoneCard) {
                    val cardCenter = Offset(el.x + el.width / 2f, el.y + el.height / 2f)
                    for (kanban in kanbans) {
                        if (kanban.bounds.contains(cardCenter)) {
                            val colIdx = kanban.getColumnIndex(cardCenter.x)
                            val targetStatus = kanban.getStatusForColumn(colIdx)
                            if (el.status != targetStatus) {
                                updatedMap = updatedMap + (id to el.copy(status = targetStatus))
                                changed = true
                            }
                            break
                        }
                    }
                }
            }
            if (changed) current.copy(elements = updatedMap) else current
        }

        updateHistoryFlags()
        triggerAutoSave(immediate = false)
        elementIds.forEach { id ->
            _state.value.elements[id]?.let { collabManager.broadcastElementUpsert(it) }
        }
    }

    // --- Изменение размера (Resize) ---

    fun resizeElementLive(elementId: String, newBounds: Rect) {
        _state.update { current ->
            val el = current.elements[elementId] ?: return@update current
            val updated = el.copyWithPosition(newBounds.left, newBounds.top)
                .copyWithSize(newBounds.width, newBounds.height)
            current.withElement(updated)
        }
    }

    fun commitResize(elementId: String, initialBounds: Rect, finalBounds: Rect) {
        if (initialBounds == finalBounds) return
        val cmd = ResizeElementCommand(
            elementId = elementId,
            oldX = initialBounds.left,
            oldY = initialBounds.top,
            oldWidth = initialBounds.width,
            oldHeight = initialBounds.height,
            newX = finalBounds.left,
            newY = finalBounds.top,
            newWidth = finalBounds.width,
            newHeight = finalBounds.height
        )
        _state.update { current ->
            val el = current.elements[elementId]
            val stateBefore = if (el != null) {
                val reverted = el.copyWithPosition(initialBounds.left, initialBounds.top)
                    .copyWithSize(initialBounds.width, initialBounds.height)
                current.withElement(reverted)
            } else current
            historyManager.execute(cmd, stateBefore)
        }
        updateHistoryFlags()
        triggerAutoSave(immediate = false)
        _state.value.elements[elementId]?.let { collabManager.broadcastElementUpsert(it) }
    }

    // --- Вращение элементов (Rotate) ---

    fun rotateElementsLive(elementIds: Set<String>, initialRotations: Map<String, Float>, deltaAngle: Float) {
        _state.update { current ->
            var updated = current
            for (id in elementIds) {
                val el = updated.elements[id] ?: continue
                val base = initialRotations[id] ?: el.rotation
                var newRot = (base + deltaAngle) % 360f
                if (newRot < 0f) newRot += 360f
                updated = updated.withElement(el.withRotation(newRot))
            }
            updated
        }
    }

    fun commitRotate(elementIds: Set<String>, initialRotations: Map<String, Float>, deltaAngle: Float) {
        if (deltaAngle == 0f) return
        val finalRotations = elementIds.associateWith { id ->
            val base = initialRotations[id] ?: 0f
            var newRot = (base + deltaAngle) % 360f
            if (newRot < 0f) newRot += 360f
            newRot
        }
        val cmd = RotateElementsCommand(
            oldRotations = initialRotations,
            newRotations = finalRotations
        )
        _state.update { current ->
            var revertedState = current
            for ((id, rot) in initialRotations) {
                val el = revertedState.elements[id] ?: continue
                revertedState = revertedState.withElement(el.withRotation(rot))
            }
            historyManager.execute(cmd, revertedState)
        }
        updateHistoryFlags()
        triggerAutoSave(immediate = false)
        elementIds.forEach { id ->
            _state.value.elements[id]?.let { collabManager.broadcastElementUpsert(it) }
        }
    }

    /**
     * Быстрый поворот выделенных элементов на угол [deltaDegrees] (например, +90° или -90°).
     */
    fun rotateSelectedBy(deltaDegrees: Float) {
        val selectedIds = _state.value.selectedElementIds
        if (selectedIds.isEmpty()) return
        val currentElements = _state.value.elements
        val initialRotations = selectedIds.associateWith { currentElements[it]?.rotation ?: 0f }
        val finalRotations = initialRotations.mapValues { (_, rot) ->
            var newRot = (rot + deltaDegrees) % 360f
            if (newRot < 0f) newRot += 360f
            newRot
        }
        val cmd = RotateElementsCommand(
            oldRotations = initialRotations,
            newRotations = finalRotations
        )
        executeCommand(cmd)
        selectedIds.forEach { id ->
            _state.value.elements[id]?.let { collabManager.broadcastElementUpsert(it) }
        }
    }

    // --- Создание элементов ---

    fun createStickyNote(atWorld: Offset) {
        val note = BoardElement.StickyNote(
            id = "note-${UUID.randomUUID()}",
            x = atWorld.x - 105f,
            y = atWorld.y - 105f,
            width = 210f,
            height = 210f,
            text = "Заметка\n- [ ] Первая задача\n- [ ] Вторая задача",
            colorHex = 0xFFFEF08AL,
            zIndex = nextZIndex()
        )
        executeCommand(AddElementCommand(note, selectAfterAdd = true))
        _state.update { it.withSelection(setOf(note.id)).withActiveTool(CanvasTool.SELECT) }
    }

    fun createKanbanBoard(atWorld: Offset) {
        val kanban = BoardElement.KanbanBoard(
            id = "kanban-${UUID.randomUUID()}",
            x = atWorld.x - 500f,
            y = atWorld.y - 310f,
            width = 1000f,
            height = 620f,
            title = "Канбан-доска проекта",
            zIndex = nextZIndex()
        )
        executeCommand(AddElementCommand(kanban, selectAfterAdd = true))
        _state.update { it.withSelection(setOf(kanban.id)).withActiveTool(CanvasTool.SELECT) }
    }

    fun toggleStickyTodo(noteId: String, lineIndex: Int, broadcast: Boolean = true) {
        _state.update { current ->
            val note = current.elements[noteId] as? BoardElement.StickyNote ?: return@update current
            val lines = note.text.split("\n").toMutableList()
            if (lineIndex !in lines.indices) return@update current
            val line = lines[lineIndex]
            val trimmed = line.trimStart()
            val leadingSpaces = line.substring(0, line.length - trimmed.length)

            val newLine = when {
                trimmed.startsWith("- [x] ", ignoreCase = true) -> leadingSpaces + "- [ ] " + trimmed.substring(6)
                trimmed.startsWith("[x] ", ignoreCase = true) -> leadingSpaces + "[ ] " + trimmed.substring(4)
                trimmed.startsWith("- [ ] ") -> leadingSpaces + "- [x] " + trimmed.substring(6)
                trimmed.startsWith("[ ] ") -> leadingSpaces + "[x] " + trimmed.substring(4)
                else -> line
            }
            lines[lineIndex] = newLine
            val updatedNote = note.copy(text = lines.joinToString("\n"))
            current.withElement(updatedNote)
        }
        if (broadcast) {
            collabManager.broadcastToggleStickyTodo(noteId, lineIndex)
        }
        triggerAutoSave(immediate = false)
    }

    fun openStickyNoteEditor(note: BoardElement.StickyNote) {
        _editingStickyNote.value = note
    }

    fun closeStickyNoteEditor() {
        _editingStickyNote.value = null
    }

    fun saveStickyNote(noteId: String, newText: String, newColorHex: Long) {
        _state.update { current ->
            val note = current.elements[noteId] as? BoardElement.StickyNote ?: return@update current
            val updated = note.copy(text = newText, colorHex = newColorHex)
            collabManager.broadcastElementUpsert(updated)
            current.withElement(updated)
        }
        _editingStickyNote.value = null
        triggerAutoSave(immediate = false)
    }

    // --- Управление плавающим таймером (Workshop Timer) ---

    fun toggleTimerVisibility() {
        _workshopTimerState.update { it.copy(isVisible = !it.isVisible) }
    }

    fun showTimer() {
        _workshopTimerState.update { it.copy(isVisible = true) }
    }

    fun hideTimer() {
        _workshopTimerState.update { it.copy(isVisible = false) }
    }

    fun startTimer() {
        _workshopTimerState.update { it.copy(isRunning = true, isFinished = false) }
    }

    fun pauseTimer() {
        _workshopTimerState.update { it.copy(isRunning = false) }
    }

    fun resetTimer() {
        _workshopTimerState.update {
            it.copy(
                isRunning = false,
                remainingSeconds = if (it.isStopwatch) 0 else it.totalSeconds,
                isFinished = false
            )
        }
    }

    fun addTimerMinutes(minutes: Int) {
        _workshopTimerState.update {
            val added = minutes * 60
            it.copy(
                totalSeconds = it.totalSeconds + added,
                remainingSeconds = it.remainingSeconds + added,
                isFinished = false
            )
        }
    }

    fun setTimerPreset(minutes: Int) {
        _workshopTimerState.update {
            val total = minutes * 60
            it.copy(
                totalSeconds = total,
                remainingSeconds = total,
                isRunning = false,
                isStopwatch = false,
                isFinished = false
            )
        }
    }

    fun toggleTimerMode() {
        _workshopTimerState.update {
            val nextMode = !it.isStopwatch
            it.copy(
                isStopwatch = nextMode,
                isRunning = false,
                remainingSeconds = if (nextMode) 0 else it.totalSeconds,
                isFinished = false
            )
        }
    }

    fun toggleTimerMinimized() {
        _workshopTimerState.update { it.copy(isMinimized = !it.isMinimized) }
    }

    fun createShape(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        settings: ShapeSettings = _state.value.shapeSettings
    ) {
        val shape = BoardElement.Shape(
            id = "shape-${UUID.randomUUID()}",
            x = x,
            y = y,
            width = width,
            height = height,
            shapeType = settings.shapeType,
            strokeColorHex = settings.strokeColorHex,
            fillColorHex = settings.fillColorHex,
            strokeWidth = settings.strokeWidth,
            strokeStyle = settings.strokeStyle,
            cornerRadius = settings.cornerRadius,
            zIndex = nextZIndex()
        )
        executeCommand(AddElementCommand(shape, selectAfterAdd = true))
        _state.update { it.withSelection(setOf(shape.id)).withActiveTool(CanvasTool.SELECT) }
    }

    fun createShape(atWorld: Offset, shapeType: ShapeType? = null) {
        val settings = _state.value.shapeSettings.let {
            if (shapeType != null) it.copy(shapeType = shapeType) else it
        }
        createShape(
            x = atWorld.x - 80f,
            y = atWorld.y - 50f,
            width = 160f,
            height = 100f,
            settings = settings
        )
    }

    fun createText(atWorld: Offset) {
        val textElem = BoardElement.Text(
            id = "text-${UUID.randomUUID()}",
            x = atWorld.x,
            y = atWorld.y,
            width = 200f,
            height = 50f,
            text = "Текст",
            zIndex = nextZIndex()
        )
        executeCommand(AddElementCommand(textElem))
        _state.update { it.withActiveTool(CanvasTool.SELECT) }
        startEditingElement(textElem)
    }

    fun addDrawingPath(path: BoardElement.DrawingPath) {
        val pathWithZ = path.copy(zIndex = nextZIndex())
        executeCommand(AddElementCommand(pathWithZ, selectAfterAdd = false))
        _state.update { it.clearSelection() }
    }

    // --- Настройки фона и сетки ---

    fun setGridType(gridType: GridType) {
        _state.update { it.withGridType(gridType) }
        AppPreferences.saveGridType(gridType)
        PreferencesManager.saveGridType(gridType)
        triggerAutoSave(immediate = true)
    }

    fun cycleGridType() {
        _state.update { current ->
            val next = when (current.gridType) {
                GridType.DOTS -> GridType.LINES
                GridType.LINES -> GridType.NONE
                GridType.NONE -> GridType.DOTS
            }
            AppPreferences.saveGridType(next)
            PreferencesManager.saveGridType(next)
            current.withGridType(next)
        }
        triggerAutoSave(immediate = true)
    }

    fun setBackgroundColor(colorHex: Long) {
        _state.update { it.withBackgroundColor(colorHex) }
        AppPreferences.saveBackgroundColor(colorHex)
        PreferencesManager.saveBackgroundColor(colorHex)
        triggerAutoSave(immediate = true)
    }

    // --- Фреймы презентаций и PDF ---

    fun createDocumentFrame(atWorld: Offset, title: String = "Презентация / PDF") {
        val frame = BoardElement.DocumentFrame(
            id = "frame-${UUID.randomUUID()}",
            x = atWorld.x - 400f,
            y = atWorld.y - 250f,
            width = 800f,
            height = 500f,
            title = title,
            zIndex = nextZIndex()
        )
        executeCommand(AddElementCommand(frame, selectAfterAdd = true))
        _state.update { it.withSelection(setOf(frame.id)).withActiveTool(CanvasTool.SELECT) }
    }

    fun importDocumentAt(file: java.io.File, atWorld: Offset) {
        val frameId = "frame-${UUID.randomUUID()}"
        val frame = BoardElement.DocumentFrame(
            id = frameId,
            x = atWorld.x - 400f,
            y = atWorld.y - 250f,
            width = 800f,
            height = 500f,
            title = file.name,
            zIndex = nextZIndex()
        )
        executeCommand(AddElementCommand(frame, selectAfterAdd = true))
        _state.update { it.withSelection(setOf(frame.id)).withActiveTool(CanvasTool.SELECT) }
        loadDocumentIntoFrame(frameId, file)
    }

    fun loadDocumentIntoFrame(frameId: String, file: java.io.File) {
        ioScope.launch {
            try {
                val result = DocumentImporter.importFile(file)
                _state.update { current ->
                    val frame = current.elements[frameId] as? BoardElement.DocumentFrame ?: return@update current
                    val updated = frame.copy(
                        title = result.title,
                        pages = result.pages,
                        currentPageIndex = 0,
                        width = result.preferredWidth,
                        height = result.preferredHeight
                    )
                    current.withElement(updated).withSelection(setOf(frameId))
                }
                triggerAutoSave(immediate = false)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun frameNextPage(frameId: String) {
        _state.update { current ->
            val frame = current.elements[frameId] as? BoardElement.DocumentFrame ?: return@update current
            if (frame.totalPages <= 0) return@update current
            val nextIndex = (frame.currentPageIndex + 1).coerceAtMost(frame.totalPages - 1)
            current.withElement(frame.copy(currentPageIndex = nextIndex))
        }
        triggerAutoSave(immediate = false)
    }

    fun framePrevPage(frameId: String) {
        _state.update { current ->
            val frame = current.elements[frameId] as? BoardElement.DocumentFrame ?: return@update current
            if (frame.totalPages <= 0) return@update current
            val prevIndex = (frame.currentPageIndex - 1).coerceAtLeast(0)
            current.withElement(frame.copy(currentPageIndex = prevIndex))
        }
        triggerAutoSave(immediate = false)
    }

    fun setFramePage(frameId: String, pageIndex: Int) {
        _state.update { current ->
            val frame = current.elements[frameId] as? BoardElement.DocumentFrame ?: return@update current
            if (frame.totalPages <= 0) return@update current
            val validIndex = pageIndex.coerceIn(0, frame.totalPages - 1)
            current.withElement(frame.copy(currentPageIndex = validIndex))
        }
        triggerAutoSave(immediate = false)
    }

    // --- Удаление и дублирование ---

    fun deleteSelectedElements() {
        val selected = _state.value.selectedElements
        if (selected.isEmpty()) return
        executeCommand(DeleteElementsCommand(selected))
    }

    fun deleteElements(elements: List<BoardElement>) {
        if (elements.isEmpty()) return
        executeCommand(DeleteElementsCommand(elements))
    }

    fun duplicateSelectedElements() {
        val selected = _state.value.selectedElements
        if (selected.isEmpty()) return

        val offsetDelta = 30f
        val duplicates = selected.map { original ->
            val newId = "${original.id}-copy-${UUID.randomUUID().toString().take(6)}"
            original.copyWithPosition(original.x + offsetDelta, original.y + offsetDelta)
                .let {
                    when (it) {
                        is BoardElement.StickyNote -> it.copy(id = newId, zIndex = nextZIndex())
                        is BoardElement.Shape -> it.copy(id = newId, zIndex = nextZIndex())
                        is BoardElement.Text -> it.copy(id = newId, zIndex = nextZIndex())
                        is BoardElement.DrawingPath -> it.copy(id = newId, zIndex = nextZIndex())
                        is BoardElement.ImageElement -> it.copy(id = newId, zIndex = nextZIndex())
                        is BoardElement.DocumentFrame -> it.copy(id = newId, zIndex = nextZIndex())
                        is BoardElement.Connector -> it.copy(id = newId, zIndex = nextZIndex())
                        is BoardElement.MilestoneCard -> it.copy(id = newId, zIndex = nextZIndex())
                        is BoardElement.FunctionGraph -> it.copy(id = newId, zIndex = nextZIndex())
                        is BoardElement.KanbanBoard -> it.copy(id = newId, zIndex = nextZIndex())
                        is BoardElement.LatexFormula -> it.copy(id = newId, zIndex = nextZIndex())
                        is BoardElement.PhysicsStencil -> it.copy(id = newId, zIndex = nextZIndex())
                    }
                }
        }
        executeCommand(DuplicateElementsCommand(duplicates))
    }

    // --- Редактирование текста ---

    fun startEditingElement(element: BoardElement) {
        if (element is BoardElement.StickyNote || element is BoardElement.Text) {
            _editingElement.value = element
        }
    }

    fun commitTextEdit(newText: String) {
        val element = _editingElement.value ?: return
        val currentText = when (element) {
            is BoardElement.StickyNote -> element.text
            is BoardElement.Text -> element.text
            else -> ""
        }
        if (newText != currentText) {
            if (element is BoardElement.Text) {
                val (w, h) = com.lkboard.app.ui.components.TextLayoutHelper.calculateDimensions(
                    text = newText,
                    fontSize = element.fontSize,
                    maxWidth = 700f
                )
                val resized = element.copy(text = newText, width = w, height = h)
                executeCommand(object : BoardCommand {
                    override val description = "Изменение текста"
                    override fun execute(state: BoardState) = state.withElement(resized)
                    override fun undo(state: BoardState) = state.withElement(element)
                })
            } else {
                executeCommand(UpdateTextCommand(element.id, currentText, newText))
            }
        }
        _editingElement.value = null
    }

    fun commitTextElementEdit(
        newText: String,
        fontSize: Float = 19f,
        isBold: Boolean = false,
        isItalic: Boolean = false,
        colorHex: Long = 0xFF1E1E1EL
    ) {
        val element = _editingElement.value ?: return
        if (element is BoardElement.Text) {
            val (w, h) = com.lkboard.app.ui.components.TextLayoutHelper.calculateDimensions(
                text = newText,
                fontSize = fontSize,
                maxWidth = 700f
            )
            val updated = element.copy(
                text = newText,
                fontSize = fontSize,
                isBold = isBold,
                isItalic = isItalic,
                colorHex = colorHex,
                width = w,
                height = h
            )
            executeCommand(object : BoardCommand {
                override val description = "Изменение стиля текста"
                override fun execute(state: BoardState) = state.withElement(updated)
                override fun undo(state: BoardState) = state.withElement(element)
            })
            collabManager.broadcastElementUpsert(updated)
        } else if (element is BoardElement.StickyNote) {
            commitTextEdit(newText)
        }
        _editingElement.value = null
    }

    fun cancelTextEdit() {
        _editingElement.value = null
    }

    // --- Умные коннекторы (Smart Connectors) ---

    fun createConnector(connector: BoardElement.Connector) {
        val connectorWithZ = connector.copy(zIndex = nextZIndex())
        executeCommand(AddElementCommand(connectorWithZ, selectAfterAdd = true))
        _state.update { it.withSelection(setOf(connectorWithZ.id)).withActiveTool(CanvasTool.SELECT) }
    }

    // --- Вехи дорожной карты (Milestone Cards) ---

    fun createMilestoneCard(atWorld: Offset) {
        val milestone = BoardElement.MilestoneCard(
            id = "milestone-${UUID.randomUUID()}",
            x = atWorld.x - 140f,
            y = atWorld.y - 80f,
            width = 280f,
            height = 160f,
            title = "Новая веха",
            description = "Описание этапа проекта и ключевых результатов...",
            status = MilestoneStatus.IN_PROGRESS,
            progress = 0.5f,
            dueDate = "",
            tags = listOf("Release"),
            zIndex = nextZIndex()
        )
        executeCommand(AddElementCommand(milestone, selectAfterAdd = true))
        _state.update { it.withSelection(setOf(milestone.id)).withActiveTool(CanvasTool.SELECT) }
    }

    fun cycleMilestoneStatus(cardId: String) {
        val current = _state.value
        val card = current.elements[cardId] as? BoardElement.MilestoneCard ?: return
        val nextStatus = when (card.status) {
            MilestoneStatus.BACKLOG -> MilestoneStatus.IN_PROGRESS
            MilestoneStatus.IN_PROGRESS -> MilestoneStatus.REVIEW
            MilestoneStatus.REVIEW -> MilestoneStatus.DONE
            MilestoneStatus.DONE -> MilestoneStatus.BACKLOG
        }
        val updated = card.copy(status = nextStatus)
        executeCommand(object : BoardCommand {
            override val description = "Смена статуса вехи"
            override fun execute(state: BoardState) = state.withElement(updated)
            override fun undo(state: BoardState) = state.withElement(card)
        })
    }

    fun updateMilestoneCard(card: BoardElement.MilestoneCard) {
        val oldCard = _state.value.elements[card.id] ?: return
        executeCommand(object : BoardCommand {
            override val description = "Редактирование вехи"
            override fun execute(state: BoardState) = state.withElement(card)
            override fun undo(state: BoardState) = state.withElement(oldCard)
        })
        _editingMilestone.value = null
    }

    fun startEditingMilestone(card: BoardElement.MilestoneCard) {
        _editingMilestone.value = card
    }

    fun closeMilestoneDialog() {
        _editingMilestone.value = null
    }

    // --- Графики математических функций (Math Function Plotter) ---

    fun createFunctionGraph(atWorld: Offset) {
        val graph = BoardElement.FunctionGraph(
            id = "graph-${UUID.randomUUID()}",
            x = atWorld.x - 210f,
            y = atWorld.y - 160f,
            width = 420f,
            height = 320f,
            title = "График функции",
            expressions = listOf("sin(x)", "0.5*x^2 - 2"),
            xMin = -10f,
            xMax = 10f,
            yMin = -6f,
            yMax = 6f,
            zIndex = nextZIndex()
        )
        executeCommand(AddElementCommand(graph, selectAfterAdd = true))
        _state.update { it.withSelection(setOf(graph.id)).withActiveTool(CanvasTool.SELECT) }
    }

    fun updateFunctionGraph(graph: BoardElement.FunctionGraph) {
        val oldGraph = _state.value.elements[graph.id] ?: return
        executeCommand(object : BoardCommand {
            override val description = "Редактирование графика"
            override fun execute(state: BoardState) = state.withElement(graph)
            override fun undo(state: BoardState) = state.withElement(oldGraph)
        })
        _editingFunctionGraph.value = null
    }

    fun startEditingFunctionGraph(graph: BoardElement.FunctionGraph) {
        _editingFunctionGraph.value = graph
    }

    fun closeFunctionGraphDialog() {
        _editingFunctionGraph.value = null
    }

    // --- Режим презентации и лазерная указка ---

    fun togglePresentationMode() {
        _isPresentationMode.value = !_isPresentationMode.value
    }

    fun setPresentationMode(enabled: Boolean) {
        _isPresentationMode.value = enabled
    }

    fun nextPresentationSlide() {
        val frames = _state.value.elements.values.filterIsInstance<BoardElement.DocumentFrame>()
        if (frames.isNotEmpty()) {
            val active = frames.firstOrNull { it.id in _state.value.selectedElementIds } ?: frames.first()
            if (active.currentPageIndex < active.totalPages - 1) {
                frameNextPage(active.id)
            } else {
                val nextFrameIndex = (frames.indexOf(active) + 1) % frames.size
                val nextFrame = frames[nextFrameIndex]
                _state.update { it.withSelection(setOf(nextFrame.id)) }
            }
        }
    }

    fun prevPresentationSlide() {
        val frames = _state.value.elements.values.filterIsInstance<BoardElement.DocumentFrame>()
        if (frames.isNotEmpty()) {
            val active = frames.firstOrNull { it.id in _state.value.selectedElementIds } ?: frames.first()
            if (active.currentPageIndex > 0) {
                framePrevPage(active.id)
            } else {
                val prevFrameIndex = if (frames.indexOf(active) - 1 < 0) frames.size - 1 else frames.indexOf(active) - 1
                val prevFrame = frames[prevFrameIndex]
                _state.update { it.withSelection(setOf(prevFrame.id)) }
            }
        }
    }

    fun toggleLaserPointer() {
        _isLaserPointerActive.value = !_isLaserPointerActive.value
        if (!_isLaserPointerActive.value) {
            _laserTrail.value = emptyList()
        }
    }

    fun addLaserPoint(worldPos: Offset) {
        val now = System.currentTimeMillis()
        val pruned = LaserPointerManager.pruneTrail(_laserTrail.value, now)
        _laserTrail.value = pruned + LaserPoint(worldPos, now)
        collabManager.broadcastPointer(worldPos.x, worldPos.y, isLaser = true)
    }

    // --- Управление совместной работой (Collaboration) ---

    fun openShareDialog() {
        _isShareDialogOpen.value = true
    }

    fun closeShareDialog() {
        _isShareDialogOpen.value = false
    }

    fun startHosting(port: Int = NetworkUtils.DEFAULT_PORT, userName: String = "Хост") {
        collabManager.startHosting(port = port, boardTitle = _state.value.title, userName = userName)
    }

    fun stopHosting() {
        collabManager.stopHosting()
    }

    fun connectAsClient(hostUrl: String, userName: String = "Участник") {
        collabManager.connectAsClient(hostUrl = hostUrl, userName = userName)
    }

    fun disconnectClient() {
        collabManager.disconnectClient()
    }

    fun pruneLaserTrail() {
        if (_laserTrail.value.isNotEmpty()) {
            _laserTrail.value = LaserPointerManager.pruneTrail(_laserTrail.value)
        }
    }

    // --- Экспорт в SVG / PDF / Буфер обмена ---

    fun copyBoardImageToClipboard() {
        val image = PngExporter.renderBoardToBufferedImage(_state.value) ?: return
        com.lkboard.app.clipboard.ClipboardManager.copyImageToClipboard(image)
    }

    fun exportToSvg(file: java.io.File) {
        SvgExporter.exportToSvg(file, _state.value)
    }

    fun exportToPdf(file: java.io.File): Boolean {
        return PdfExporter.exportToPdf(file, _state.value)
    }

    fun exportSelectionToPng(file: java.io.File): Boolean {
        return PngExporter.exportToPng(file, _state.value, onlySelected = true)
    }

    // --- LaTeX-формулы на лету ---

    fun openLatexDialog(element: BoardElement.LatexFormula? = null, position: Offset? = null) {
        _editingLatexFormula.value = element
        if (position != null) {
            pendingLatexPosition = position
        }
        _isLatexDialogOpen.value = true
    }

    fun closeLatexDialog() {
        _isLatexDialogOpen.value = false
        _editingLatexFormula.value = null
    }

    fun saveLatexFormula(formulaText: String, fontSize: Float = 24f) {
        val editing = _editingLatexFormula.value
        if (editing != null) {
            val estimated = com.lkboard.app.util.LatexRenderer.estimateSize(formulaText, fontSize)
            val w = estimated.first
            val h = estimated.second
            val updated = editing.copy(
                formula = formulaText,
                fontSize = fontSize,
                width = maxOf(140f, w + 32f),
                height = maxOf(60f, h + 24f)
            )
            _state.update { it.withElement(updated) }
            collabManager.broadcastElementUpsert(updated)
            triggerAutoSave(immediate = false)
        } else {
            val estimated = com.lkboard.app.util.LatexRenderer.estimateSize(formulaText, fontSize)
            val w = estimated.first
            val h = estimated.second
            val newElement = BoardElement.LatexFormula(
                id = UUID.randomUUID().toString(),
                x = pendingLatexPosition.x,
                y = pendingLatexPosition.y,
                width = maxOf(160f, w + 32f),
                height = maxOf(70f, h + 24f),
                formula = formulaText,
                fontSize = fontSize,
                zIndex = nextZIndex()
            )
            executeCommand(AddElementCommand(newElement))
            _state.update { it.copy(selectedElementIds = setOf(newElement.id)) }
        }
        closeLatexDialog()
    }

    fun updateSelectedTextProperties(
        fontSizeDelta: Float? = null,
        targetFontSize: Float? = null,
        isBold: Boolean? = null,
        isItalic: Boolean? = null,
        colorHex: Long? = null
    ) {
        val selectedId = _state.value.selectedElementIds.singleOrNull() ?: return
        val current = _state.value.elements[selectedId] as? BoardElement.Text ?: return

        val newFontSize = when {
            targetFontSize != null -> targetFontSize.coerceIn(10f, 220f)
            fontSizeDelta != null -> (current.fontSize + fontSizeDelta).coerceIn(10f, 220f)
            else -> current.fontSize
        }
        val newBold = isBold ?: current.isBold
        val newItalic = isItalic ?: current.isItalic
        val newColor = colorHex ?: current.colorHex

        val (w, h) = com.lkboard.app.ui.components.TextLayoutHelper.calculateDimensions(
            text = current.text,
            fontSize = newFontSize,
            maxWidth = maxOf(current.width, 700f)
        )
        val updated = current.copy(
            fontSize = newFontSize,
            isBold = newBold,
            isItalic = newItalic,
            colorHex = newColor,
            width = maxOf(current.width, w),
            height = maxOf(current.height, h)
        )
        executeCommand(object : BoardCommand {
            override val description = "Изменение стиля текста"
            override fun execute(state: BoardState) = state.withElement(updated)
            override fun undo(state: BoardState) = state.withElement(current)
        })
        collabManager.broadcastElementUpsert(updated)
        triggerAutoSave(immediate = false)
    }

    fun updateSelectedLatexProperties(
        fontSizeDelta: Float? = null,
        targetFontSize: Float? = null,
        colorHex: Long? = null,
        toggleBackground: Boolean = false
    ) {
        val selectedId = _state.value.selectedElementIds.singleOrNull() ?: return
        val current = _state.value.elements[selectedId] as? BoardElement.LatexFormula ?: return

        val newFontSize = when {
            targetFontSize != null -> targetFontSize.coerceIn(12f, 220f)
            fontSizeDelta != null -> (current.fontSize + fontSizeDelta).coerceIn(12f, 220f)
            else -> current.fontSize
        }
        val newColor = colorHex ?: current.colorHex
        val newHasBg = if (toggleBackground) !current.hasBackgroundCard else current.hasBackgroundCard

        val estimated = com.lkboard.app.util.LatexRenderer.estimateSize(current.formula, newFontSize)
        val w = estimated.first
        val h = estimated.second

        val updated = current.copy(
            fontSize = newFontSize,
            colorHex = newColor,
            hasBackgroundCard = newHasBg,
            width = maxOf(140f, w + 32f),
            height = maxOf(60f, h + 24f)
        )
        executeCommand(object : BoardCommand {
            override val description = "Изменение параметров формулы"
            override fun execute(state: BoardState) = state.withElement(updated)
            override fun undo(state: BoardState) = state.withElement(current)
        })
        collabManager.broadcastElementUpsert(updated)
        triggerAutoSave(immediate = false)
    }

    // --- Физический и инженерный конструктор (Stencils) ---

    fun openPhysicsStencilDialog(position: Offset? = null) {
        if (position != null) {
            pendingPhysicsStencilPosition = position
        }
        _isPhysicsStencilDialogOpen.value = true
    }

    fun closePhysicsStencilDialog() {
        _isPhysicsStencilDialogOpen.value = false
    }

    fun addPhysicsStencil(type: PhysicsStencilType, label: String = "") {
        val (defW, defH) = when (type) {
            PhysicsStencilType.INCLINED_PLANE -> 220f to 150f
            PhysicsStencilType.PULLEY_BLOCK -> 140f to 180f
            PhysicsStencilType.SPRING -> 120f to 170f
            PhysicsStencilType.OPTICAL_LENS -> 260f to 160f
            PhysicsStencilType.CIRCUIT_RESISTOR -> 140f to 70f
            PhysicsStencilType.CIRCUIT_BATTERY -> 110f to 70f
            PhysicsStencilType.CIRCUIT_CAPACITOR -> 100f to 70f
            PhysicsStencilType.CIRCUIT_INDUCTOR -> 140f to 70f
            PhysicsStencilType.CIRCUIT_SWITCH -> 120f to 70f
            PhysicsStencilType.CIRCUIT_LAMP -> 90f to 90f
        }
        val newElement = BoardElement.PhysicsStencil(
            id = UUID.randomUUID().toString(),
            x = pendingPhysicsStencilPosition.x,
            y = pendingPhysicsStencilPosition.y,
            width = defW,
            height = defH,
            stencilType = type,
            label = if (label.isNotBlank()) label else type.displayName,
            zIndex = nextZIndex()
        )
        executeCommand(AddElementCommand(newElement))
        _state.update { it.copy(selectedElementIds = setOf(newElement.id)) }
        closePhysicsStencilDialog()
    }

    // --- Настройки, темы и профили онбординга ---

    fun openSettingsDialog() {
        _isSettingsDialogOpen.value = true
    }

    fun closeSettingsDialog() {
        _isSettingsDialogOpen.value = false
    }

    fun applySettings(newTheme: AppTheme, newRole: UserRole, newTools: Set<CanvasTool>) {
        updateTheme(newTheme)
        _currentUserRole.value = newRole
        AppPreferences.saveUserRole(newRole)
        AppPreferences.saveEnabledTools(newTools)
        _state.update { it.copy(enabledTools = newTools) }
        closeSettingsDialog()
    }

    fun updateTheme(newTheme: AppTheme) {
        _currentTheme.value = newTheme
        AppPreferences.saveAppTheme(newTheme)
        setBackgroundColor(newTheme.defaultBgColor)
        setGridType(newTheme.defaultGridType)
    }

    fun completeOnboarding(selectedRole: UserRole) {
        _currentUserRole.value = selectedRole
        AppPreferences.saveUserRole(selectedRole)
        AppPreferences.setFirstRunCompleted()
        val tools = selectedRole.tools
        _state.update { it.copy(enabledTools = tools) }
        _isOnboardingOpen.value = false
    }

    private fun nextZIndex(): Int {
        val maxZ = _state.value.elements.values.maxOfOrNull { it.zIndex } ?: 0
        return maxZ + 1
    }

    private fun loadOrCreateInitialState(): BoardState {
        try {
            val autoSaveFile = AppPreferences.getAutoSaveFile()
            if (autoSaveFile.exists() && autoSaveFile.length() > 0) {
                val loaded = com.lkboard.app.data.BoardSerializer.load(autoSaveFile)
                return loaded.copy(
                    selectedElementIds = emptySet(),
                    enabledTools = AppPreferences.loadEnabledTools()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return createInitialState()
    }

    private fun createInitialState(): BoardState {
        val sampleSticky = BoardElement.StickyNote(
            id = "note-welcome",
            x = 80f,
            y = 100f,
            width = 240f,
            height = 200f,
            text = "Добро пожаловать в Lkboard! 🚀\n\n• Двойной клик — редактировать текст.\n• Перетаскивайте и меняйте размер за маркеры.\n• Ctrl+Z/Ctrl+Y — Undo/Redo.\n• Ctrl+D — дублировать.",
            colorHex = 0xFFFFF59D
        )

        val sampleShape = BoardElement.Shape(
            id = "shape-arch",
            x = 380f,
            y = 100f,
            width = 220f,
            height = 140f,
            shapeType = ShapeType.ROUNDED_RECTANGLE,
            strokeColorHex = 0xFF2563EB,
            fillColorHex = 0x223B82F6,
            cornerRadius = 16f,
            strokeWidth = 2.5f
        )

        val sampleText = BoardElement.Text(
            id = "text-arch",
            x = 400f,
            y = 150f,
            width = 180f,
            height = 40f,
            text = "Skia Infinite Canvas",
            colorHex = 0xFF1E3A8A,
            fontSize = 17f,
            isBold = true
        )

        val sampleDiamond = BoardElement.Shape(
            id = "shape-decision",
            x = 660f,
            y = 100f,
            width = 150f,
            height = 140f,
            shapeType = ShapeType.DIAMOND,
            strokeColorHex = 0xFF10B981,
            fillColorHex = 0x2210B981,
            strokeWidth = 2.5f
        )

        val samplePath = BoardElement.DrawingPath(
            id = "path-signature",
            x = 100f,
            y = 360f,
            points = listOf(
                CanvasPoint(100f, 380f),
                CanvasPoint(140f, 370f),
                CanvasPoint(180f, 410f),
                CanvasPoint(220f, 360f),
                CanvasPoint(270f, 390f),
                CanvasPoint(320f, 350f)
            ),
            strokeColorHex = 0xFFEF4444,
            strokeWidth = 4f
        )

        return BoardState(
            title = "Архитектурная доска Lkboard",
            elements = mapOf(
                sampleSticky.id to sampleSticky,
                sampleShape.id to sampleShape,
                sampleText.id to sampleText,
                sampleDiamond.id to sampleDiamond,
                samplePath.id to samplePath
            ),
            penSettings = AppPreferences.loadPenSettings(),
            shapeSettings = AppPreferences.loadShapeSettings(),
            gridType = AppPreferences.loadGridType(),
            backgroundColorHex = AppPreferences.loadBackgroundColor(),
            enabledTools = AppPreferences.loadEnabledTools()
        )
    }
}
