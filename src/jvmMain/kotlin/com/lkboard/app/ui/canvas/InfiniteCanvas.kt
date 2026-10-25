package com.lkboard.app.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.lkboard.app.timer.WorkshopTimerState
import com.lkboard.app.ui.components.WorkshopTimerFloatingWidget
import com.lkboard.app.model.AlignmentGuide
import com.lkboard.app.model.AnchorPoint
import com.lkboard.app.model.BoardElement
import com.lkboard.app.model.BoardState
import com.lkboard.app.model.CanvasPoint
import com.lkboard.app.model.CanvasTool
import com.lkboard.app.model.CanvasTransform
import com.lkboard.app.model.ConnectorStyle
import com.lkboard.app.model.GeometryUtils
import com.lkboard.app.model.GridType
import com.lkboard.app.model.LaserPoint
import com.lkboard.app.model.LaserPointerManager
import com.lkboard.app.model.ResizeHandle
import com.lkboard.app.model.getAnchorOffset
import com.lkboard.app.ui.components.ElementRenderer
import com.lkboard.app.ui.components.InlineTextEditorOverlay
import com.lkboard.app.ui.components.SelectionRenderer
import com.lkboard.app.util.FileDialogs
import java.awt.Cursor
import java.util.UUID

/**
 * Полнофункциональный бесконечный масштабируемый холст Lkboard (Infinite Canvas).
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun InfiniteCanvas(
    state: BoardState,
    onTransformChange: (CanvasTransform) -> Unit,
    onPanBy: (Offset) -> Unit = {},
    onSelectElement: (id: String, addToSelection: Boolean) -> Unit,
    onSelectElements: (ids: Set<String>) -> Unit,
    onClearSelection: () -> Unit,
    onMoveSelectedLive: (deltaX: Float, deltaY: Float) -> Unit,
    onCommitMove: (ids: Set<String>, totalDeltaX: Float, totalDeltaY: Float) -> Unit,
    onResizeElementLive: (elementId: String, newBounds: Rect) -> Unit,
    onCommitResize: (elementId: String, initialBounds: Rect, finalBounds: Rect) -> Unit,
    onRotateElementsLive: (elementIds: Set<String>, initialRotations: Map<String, Float>, deltaAngle: Float) -> Unit = { _, _, _ -> },
    onCommitRotate: (elementIds: Set<String>, initialRotations: Map<String, Float>, deltaAngle: Float) -> Unit = { _, _, _ -> },
    onDoubleTapElement: (BoardElement) -> Unit,
    onCreateStickyNote: (Offset) -> Unit,
    onCreateShape: (x: Float, y: Float, width: Float, height: Float) -> Unit,
    onCreateText: (Offset) -> Unit,
    onCreateDocumentFrame: (Offset) -> Unit = {},
    onCreateMilestoneCard: (Offset) -> Unit = {},
    onCreateFunctionGraph: (Offset) -> Unit = {},
    onCreateLatexFormula: (Offset) -> Unit = {},
    onCreatePhysicsStencil: (Offset) -> Unit = {},
    onCreateKanbanBoard: (Offset) -> Unit = {},
    onToggleStickyTodo: (String, Int) -> Unit = { _, _ -> },
    onCreateConnector: (BoardElement.Connector) -> Unit = {},
    onCycleMilestoneStatus: (String) -> Unit = {},
    onLaserPointMoved: (Offset) -> Unit = {},
    workshopTimerState: WorkshopTimerState = WorkshopTimerState(),
    onTimerStart: () -> Unit = {},
    onTimerPause: () -> Unit = {},
    onTimerReset: () -> Unit = {},
    onTimerAddMinutes: (Int) -> Unit = {},
    onTimerSetPreset: (Int) -> Unit = {},
    onTimerToggleMode: () -> Unit = {},
    onTimerToggleMinimize: () -> Unit = {},
    onTimerClose: () -> Unit = {},
    activeGuides: List<AlignmentGuide> = emptyList(),
    laserTrail: List<LaserPoint> = emptyList(),
    onPruneLaserTrail: () -> Unit = {},
    isPresentationMode: Boolean = false,
    onNextPresentationSlide: () -> Unit = {},
    onPrevPresentationSlide: () -> Unit = {},
    onToggleLaserPointer: () -> Unit = {},
    onExitPresentationMode: () -> Unit = {},
    onFrameNextPage: (String) -> Unit = {},
    onFramePrevPage: (String) -> Unit = {},
    onLoadDocumentIntoFrame: (String, java.io.File) -> Unit = { _, _ -> },
    onAddDrawingPath: (BoardElement.DrawingPath) -> Unit,
    onDeleteElements: (List<BoardElement>) -> Unit = {},
    onSpaceStateChange: (Boolean) -> Unit,
    editingElement: BoardElement? = null,
    onCommitTextEdit: (String) -> Unit = {},
    onCommitTextElementEdit: (text: String, fontSize: Float, isBold: Boolean, isItalic: Boolean, colorHex: Long) -> Unit = { t, _, _, _, _ -> onCommitTextEdit(t) },
    onCancelTextEdit: () -> Unit = {},
    onCursorPositionChange: (Offset) -> Unit = {},
    remotePointers: Map<String, com.lkboard.app.collaboration.RemotePointer> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val focusRequester = remember { FocusRequester() }

    var isSpaceDown by remember { mutableStateOf(false) }
    var cursorScreenPos by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(laserTrail.isNotEmpty()) {
        while (laserTrail.isNotEmpty()) {
            kotlinx.coroutines.delay(16)
            onPruneLaserTrail()
        }
    }

    val isHandTool = state.activeTool == CanvasTool.HAND
    val isPanMode = isSpaceDown || isHandTool

    val currentState by rememberUpdatedState(state)
    val currentIsPanMode by rememberUpdatedState(isPanMode)
    val currentOnTransformChange by rememberUpdatedState(onTransformChange)
    val currentOnPanBy by rememberUpdatedState(onPanBy)
    val currentOnSelectElement by rememberUpdatedState(onSelectElement)
    val currentOnSelectElements by rememberUpdatedState(onSelectElements)
    val currentOnClearSelection by rememberUpdatedState(onClearSelection)
    val currentOnMoveSelectedLive by rememberUpdatedState(onMoveSelectedLive)
    val currentOnCommitMove by rememberUpdatedState(onCommitMove)
    val currentOnResizeElementLive by rememberUpdatedState(onResizeElementLive)
    val currentOnCommitResize by rememberUpdatedState(onCommitResize)
    val currentOnRotateElementsLive by rememberUpdatedState(onRotateElementsLive)
    val currentOnCommitRotate by rememberUpdatedState(onCommitRotate)
    val currentOnDoubleTapElement by rememberUpdatedState(onDoubleTapElement)
    val currentOnCreateStickyNote by rememberUpdatedState(onCreateStickyNote)
    val currentOnCreateShape by rememberUpdatedState(onCreateShape)
    val currentOnCreateText by rememberUpdatedState(onCreateText)
    val currentOnCreateDocumentFrame by rememberUpdatedState(onCreateDocumentFrame)
    val currentOnCreateMilestoneCard by rememberUpdatedState(onCreateMilestoneCard)
    val currentOnCreateFunctionGraph by rememberUpdatedState(onCreateFunctionGraph)
    val currentOnCreateLatexFormula by rememberUpdatedState(onCreateLatexFormula)
    val currentOnCreatePhysicsStencil by rememberUpdatedState(onCreatePhysicsStencil)
    val currentOnCreateKanbanBoard by rememberUpdatedState(onCreateKanbanBoard)
    val currentOnToggleStickyTodo by rememberUpdatedState(onToggleStickyTodo)
    val currentOnCreateConnector by rememberUpdatedState(onCreateConnector)
    val currentOnCycleMilestoneStatus by rememberUpdatedState(onCycleMilestoneStatus)
    val currentOnLaserPointMoved by rememberUpdatedState(onLaserPointMoved)
    val currentOnAddDrawingPath by rememberUpdatedState(onAddDrawingPath)
    val currentOnDeleteElements by rememberUpdatedState(onDeleteElements)
    val currentOnCursorPositionChange by rememberUpdatedState(onCursorPositionChange)

    val gestureCallbacks = remember(
        currentOnPanBy,
        currentOnSelectElement,
        currentOnSelectElements,
        currentOnClearSelection,
        currentOnMoveSelectedLive,
        currentOnCommitMove,
        currentOnResizeElementLive,
        currentOnCommitResize,
        currentOnRotateElementsLive,
        currentOnCommitRotate,
        currentOnDoubleTapElement,
        currentOnCreateStickyNote,
        currentOnCreateShape,
        currentOnCreateText,
        currentOnCreateDocumentFrame,
        currentOnCreateMilestoneCard,
        currentOnCreateFunctionGraph,
        currentOnCreateLatexFormula,
        currentOnCreatePhysicsStencil,
        currentOnCreateKanbanBoard,
        currentOnToggleStickyTodo,
        currentOnCreateConnector,
        currentOnCycleMilestoneStatus,
        currentOnLaserPointMoved,
        currentOnAddDrawingPath,
        currentOnDeleteElements,
        currentOnCursorPositionChange
    ) {
        CanvasGestureCallbacks(
            onPanBy = currentOnPanBy,
            onSelectElement = currentOnSelectElement,
            onSelectElements = currentOnSelectElements,
            onClearSelection = currentOnClearSelection,
            onMoveSelectedLive = currentOnMoveSelectedLive,
            onCommitMove = currentOnCommitMove,
            onResizeElementLive = currentOnResizeElementLive,
            onCommitResize = currentOnCommitResize,
            onRotateElementsLive = currentOnRotateElementsLive,
            onCommitRotate = currentOnCommitRotate,
            onDoubleTapElement = currentOnDoubleTapElement,
            onCreateStickyNote = currentOnCreateStickyNote,
            onCreateShape = currentOnCreateShape,
            onCreateText = currentOnCreateText,
            onCreateDocumentFrame = currentOnCreateDocumentFrame,
            onCreateMilestoneCard = currentOnCreateMilestoneCard,
            onCreateFunctionGraph = currentOnCreateFunctionGraph,
            onCreateLatexFormula = currentOnCreateLatexFormula,
            onCreatePhysicsStencil = currentOnCreatePhysicsStencil,
            onCreateKanbanBoard = currentOnCreateKanbanBoard,
            onToggleStickyTodo = currentOnToggleStickyTodo,
            onCreateConnector = currentOnCreateConnector,
            onCycleMilestoneStatus = currentOnCycleMilestoneStatus,
            onLaserPointMoved = currentOnLaserPointMoved,
            onAddDrawingPath = currentOnAddDrawingPath,
            onDeleteElements = currentOnDeleteElements,
            onCursorPositionChange = { pos ->
                cursorScreenPos = pos
                currentOnCursorPositionChange(pos)
            }
        )
    }
    val gestureHandler = remember { CanvasGestureHandler(gestureCallbacks) }

    // Вычисляем охватывающий прямоугольник выделения
    val selectedElements = state.selectedElements
    val selectedBounds = remember(selectedElements) {
        GeometryUtils.computeBounds(selectedElements)
    }

    // Определение маркера под курсором (для инструмента SELECT или при активном выделении)
    val hoveredHandle = remember(cursorScreenPos, selectedBounds, state.transform, state.activeTool, state.selectedElementIds) {
        if (selectedBounds != null && (state.activeTool == CanvasTool.SELECT || state.selectedElementIds.isNotEmpty()) && selectedElements.size == 1) {
            val worldPos = state.transform.screenToWorld(cursorScreenPos)
            val handleRadiusWorld = 8f / state.transform.scale
            GeometryUtils.hitTestHandle(worldPos, selectedBounds, handleRadiusWorld)
        } else {
            null
        }
    }

    // Динамический системный курсор в зависимости от инструмента и положения мыши
    val currentCursor = remember(gestureHandler.interactionMode, isPanMode, hoveredHandle, state.activeTool, state.selectedElementIds) {
        val mode = gestureHandler.interactionMode
        when {
            mode is InteractionMode.Panning -> PointerIcon(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR))
            isPanMode -> PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
            state.activeTool == CanvasTool.PEN -> PointerIcon(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR))
            state.activeTool == CanvasTool.ERASER || mode is InteractionMode.Erasing ->
                PointerIcon(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR))
            hoveredHandle != null && (state.activeTool == CanvasTool.SELECT || state.selectedElementIds.isNotEmpty()) ->
                PointerIcon(Cursor.getPredefinedCursor(hoveredHandle.cursorType))
            mode is InteractionMode.Resizing -> PointerIcon(Cursor.getPredefinedCursor(mode.handle.cursorType))
            mode is InteractionMode.DraggingElements -> PointerIcon(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR))
            state.activeTool in listOf(CanvasTool.STICKY_NOTE, CanvasTool.SHAPE, CanvasTool.TEXT, CanvasTool.LATEX, CanvasTool.PHYSICS_STENCIL) ->
                PointerIcon(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR))
            else -> PointerIcon.Default
        }
    }

    val canvasBackground = Color(state.backgroundColorHex)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(canvasBackground)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.key == Key.Spacebar) {
                    when (keyEvent.type) {
                        KeyEventType.KeyDown -> {
                            if (!isSpaceDown) {
                                isSpaceDown = true
                                onSpaceStateChange(true)
                            }
                            true
                        }
                        KeyEventType.KeyUp -> {
                            isSpaceDown = false
                            onSpaceStateChange(false)
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        // 1. Интерактивная поверхность бесконечного холста
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerHoverIcon(currentCursor)
                .onPointerEvent(PointerEventType.Scroll) { event ->
                    val change = event.changes.firstOrNull() ?: return@onPointerEvent
                    val scrollDelta = change.scrollDelta.y
                    val cursorPosition = change.position
                    cursorScreenPos = cursorPosition
                    currentOnCursorPositionChange(cursorPosition)

                    if (scrollDelta != 0f) {
                        val zoomFactor = if (scrollDelta < 0f) 1.10f else (1f / 1.10f)
                        val newTransform = currentState.transform.zoomAround(cursorPosition, zoomFactor)
                        currentOnTransformChange(newTransform)
                        change.consume()
                    }
                }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            gestureHandler.handlePointerEvent(event, currentState, currentIsPanMode)
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // 1. Адаптивная координатная сетка (DOTS, LINES или NONE)
                GridRenderer.drawGrid(
                    drawScope = this,
                    transform = state.transform,
                    size = size,
                    gridType = state.gridType,
                    backgroundColor = canvasBackground
                )

            // 2. Отрисовка всех объектов сцены в мировых координатах
            withTransform({
                translate(state.transform.panX, state.transform.panY)
                scale(state.transform.scale, state.transform.scale, pivot = Offset.Zero)
            }) {
                // Отрисовка элементов доски с передачей полного контекста для динамических коннекторов
                for (element in state.sortedElements) {
                    ElementRenderer.drawElement(
                        drawScope = this,
                        element = element,
                        textMeasurer = textMeasurer,
                        isSelected = false,
                        allElements = state.elements
                    )
                }

                // Отрисовка рисуемого штриха карандаша в реальном времени (включая одиночную точку)
                val activeMode = gestureHandler.interactionMode
                if (activeMode is InteractionMode.Drawing) {
                    val ptsToDraw = if (activeMode.isStraightLine) {
                        listOf(activeMode.startPoint, activeMode.currentEndPoint)
                    } else {
                        activeMode.points
                    }
                    if (ptsToDraw.isNotEmpty()) {
                        val previewPath = BoardElement.DrawingPath(
                            id = "temp-draw",
                            points = ptsToDraw.toList(),
                            strokeColorHex = state.penSettings.colorHex,
                            strokeWidth = state.penSettings.strokeWidth,
                            isHighlighter = state.penSettings.isHighlighter
                        )
                        ElementRenderer.drawDrawingPath(
                            drawScope = this,
                            drawingPath = previewPath
                        )
                    }
                }

                // Отрисовка завершенного штриха до его появления в state.elements (устраняет мерцание в 1 кадр)
                val pending = gestureHandler.pendingCommittedPath
                if (pending != null) {
                    if (state.elements.containsKey(pending.id)) {
                        gestureHandler.pendingCommittedPath = null
                    } else {
                        ElementRenderer.drawDrawingPath(
                            drawScope = this,
                            drawingPath = pending
                        )
                    }
                }

                // Отрисовка создаваемой фигуры в реальном времени (Live Shape Preview)
                if (activeMode is InteractionMode.CreatingShape) {
                    val dx = activeMode.currentWorld.x - activeMode.startWorld.x
                    val dy = activeMode.currentWorld.y - activeMode.startWorld.y
                    val isRatioLocked = activeMode.keepAspectRatio

                    val endX: Float
                    val endY: Float
                    if (isRatioLocked) {
                        val size = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy))
                        val targetW = if (dx >= 0) size else -size
                        val targetH = if (dy >= 0) size else -size
                        endX = activeMode.startWorld.x + targetW
                        endY = activeMode.startWorld.y + targetH
                    } else {
                        endX = activeMode.currentWorld.x
                        endY = activeMode.currentWorld.y
                    }

                    val left = minOf(activeMode.startWorld.x, endX)
                    val top = minOf(activeMode.startWorld.y, endY)
                    val width = kotlin.math.abs(endX - activeMode.startWorld.x).coerceAtLeast(4f)
                    val height = kotlin.math.abs(endY - activeMode.startWorld.y).coerceAtLeast(4f)

                    val shapeSettings = state.shapeSettings
                    com.lkboard.app.model.ShapeRenderer.drawShape(
                        drawScope = this,
                        x = left,
                        y = top,
                        width = width,
                        height = height,
                        shapeType = shapeSettings.shapeType,
                        strokeColor = Color(shapeSettings.strokeColorHex),
                        fillColor = shapeSettings.fillColorHex?.let { Color(it) },
                        strokeWidth = shapeSettings.strokeWidth,
                        strokeStyle = shapeSettings.strokeStyle,
                        cornerRadius = shapeSettings.cornerRadius
                    )
                }

                // Интерактивная отрисовка протягиваемого коннектора в реальном времени (Live Connector Preview)
                if (activeMode is InteractionMode.Connecting) {
                    val previewConnector = BoardElement.Connector(
                        id = "temp-connector",
                        startElementId = activeMode.startElementId,
                        startAnchor = activeMode.startAnchor,
                        startPoint = activeMode.startPoint,
                        endElementId = activeMode.targetElementId,
                        endAnchor = activeMode.targetAnchor,
                        endPoint = CanvasPoint.fromOffset(activeMode.currentWorld),
                        style = ConnectorStyle.ORTHOGONAL,
                        strokeColorHex = 0xFF2563EB,
                        strokeWidth = 2.5f,
                        hasEndArrow = true
                    )
                    ElementRenderer.drawElement(
                        drawScope = this,
                        element = previewConnector,
                        textMeasurer = textMeasurer,
                        isSelected = false,
                        allElements = state.elements
                    )
                }

                // Отрисовка магнитных точек привязки коннекторов (Magnetic Anchor Dots)
                if (state.activeTool == CanvasTool.CONNECTOR || activeMode is InteractionMode.Connecting) {
                    val radius = 5.5f / state.transform.scale
                    for (el in state.elements.values) {
                        if (el is BoardElement.Connector || el is BoardElement.DrawingPath) continue
                        for (anchor in listOf(AnchorPoint.TOP, AnchorPoint.RIGHT, AnchorPoint.BOTTOM, AnchorPoint.LEFT)) {
                            val pt = el.getAnchorOffset(anchor)
                            val isTarget = (activeMode is InteractionMode.Connecting) &&
                                    (activeMode.targetElementId == el.id && activeMode.targetAnchor == anchor)
                            val dotR = if (isTarget) radius * 1.6f else radius
                            val dotColor = if (isTarget) Color(0xFF10B981) else Color(0xFF007AFF)

                            drawCircle(
                                color = dotColor,
                                radius = dotR,
                                center = pt
                            )
                            drawCircle(
                                color = Color.White,
                                radius = dotR,
                                center = pt,
                                style = Stroke(width = 1.5f / state.transform.scale)
                            )
                        }
                    }
                }

                // Умные направляющие выравнивания (Smart Alignment Guides как в Figma)
                for (guide in activeGuides) {
                    val guideColor = Color(0xFF007AFF).copy(alpha = 0.85f)
                    val strokeWidth = 1.5f / state.transform.scale
                    val dashPathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(6f / state.transform.scale, 4f / state.transform.scale),
                        0f
                    )
                    if (guide.isVertical) {
                        drawLine(
                            color = guideColor,
                            start = Offset(guide.coordinate, guide.start),
                            end = Offset(guide.coordinate, guide.end),
                            strokeWidth = strokeWidth,
                            pathEffect = dashPathEffect
                        )
                    } else {
                        drawLine(
                            color = guideColor,
                            start = Offset(guide.start, guide.coordinate),
                            end = Offset(guide.end, guide.coordinate),
                            strokeWidth = strokeWidth,
                            pathEffect = dashPathEffect
                        )
                    }
                }

                // Отрисовка затухающего неонового следа лазерной указки (Laser Pointer Trail)
                val now = System.currentTimeMillis()
                val validPoints = laserTrail.filter { now - it.timestamp <= LaserPointerManager.TRAIL_DURATION_MS }
                if (validPoints.size >= 2) {
                    for (i in 0 until validPoints.size - 1) {
                        val p1 = validPoints[i]
                        val p2 = validPoints[i + 1]
                        val age = (now - p2.timestamp).coerceAtLeast(0L)
                        val life = (1f - age.toFloat() / LaserPointerManager.TRAIL_DURATION_MS).coerceIn(0f, 1f)
                        val alpha = life * life

                        // Внешнее неоновое сияние
                        drawLine(
                            color = Color(0xFFFF2A4B).copy(alpha = alpha * 0.45f),
                            start = p1.pos,
                            end = p2.pos,
                            strokeWidth = 14f / state.transform.scale,
                            cap = StrokeCap.Round
                        )
                        // Яркое ядро лазера
                        drawLine(
                            color = Color(0xFFFF4D6D).copy(alpha = alpha * 0.95f),
                            start = p1.pos,
                            end = p2.pos,
                            strokeWidth = 5f / state.transform.scale,
                            cap = StrokeCap.Round
                        )
                        // Белая сердцевина
                        drawLine(
                            color = Color.White.copy(alpha = alpha * 0.90f),
                            start = p1.pos,
                            end = p2.pos,
                            strokeWidth = 2f / state.transform.scale,
                            cap = StrokeCap.Round
                        )
                    }
                    val head = validPoints.last()
                    drawCircle(
                        color = Color(0xFFFF1744),
                        radius = 6f / state.transform.scale,
                        center = head.pos
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.5f / state.transform.scale,
                        center = head.pos
                    )
                }

                // 2.5 Отрисовка удаленных курсоров участников совместной сессии
                for ((_, pointer) in remotePointers) {
                    val pColor = Color(pointer.colorHex)
                    val pCenter = Offset(pointer.x, pointer.y)
                    val pRadius = if (pointer.isLaser) 7f / state.transform.scale else 4.5f / state.transform.scale

                    drawCircle(
                        color = pColor.copy(alpha = 0.35f),
                        radius = pRadius * 2.2f,
                        center = pCenter
                    )
                    drawCircle(
                        color = pColor,
                        radius = pRadius,
                        center = pCenter
                    )
                    drawCircle(
                        color = Color.White,
                        radius = pRadius * 0.45f,
                        center = pCenter
                    )
                }

                // 3. Отрисовка Bounding Box и маркеров для выделенных элементов (для инструмента SELECT или при активном выделении)
                if (selectedBounds != null && (state.activeTool == CanvasTool.SELECT || state.selectedElementIds.isNotEmpty())) {
                    val singleElement = if (selectedElements.size == 1) selectedElements.first() else null
                    val currentRotation = singleElement?.rotation ?: 0f
                    SelectionRenderer.drawBoundingBox(
                        drawScope = this,
                        bounds = selectedBounds,
                        scale = state.transform.scale,
                        isMultiSelection = selectedElements.size > 1,
                        rotation = currentRotation
                    )
                }

                // 4. Отрисовка рамки множественного выделения (Marquee Box)
                if (activeMode is InteractionMode.BoxSelecting) {
                    SelectionRenderer.drawSelectionMarquee(
                        drawScope = this,
                        startWorld = activeMode.startWorld,
                        currentWorld = activeMode.currentWorld,
                        scale = state.transform.scale
                    )
                }
            }
        }
    }

        // 5. Встроенный инлайн-редактор текста с панелью форматирования шрифта (Inline Text Editor)
        if (editingElement is BoardElement.Text) {
            InlineTextEditorOverlay(
                element = editingElement,
                transform = state.transform,
                onCommit = onCommitTextEdit,
                onCommitDetailed = onCommitTextElementEdit,
                onCancel = onCancelTextEdit
            )
        }

        // 6. Интерактивная плавающая панель управления слайдами презентации / PDF
        val selectedFrame = state.selectedFrame
        if (selectedFrame != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* Предотвращает сквозные клики на холст */ }
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = Color.Black.copy(alpha = 0.05f),
                        spotColor = Color.Black.copy(alpha = 0.12f)
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF7F7F9).copy(alpha = 0.94f))
                    .border(1.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Slideshow,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(20.dp)
                    )

                    Text(
                        text = selectedFrame.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )

                    if (selectedFrame.totalPages > 0) {
                        IconButton(
                            onClick = { onFramePrevPage(selectedFrame.id) },
                            enabled = selectedFrame.currentPageIndex > 0,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                                contentDescription = "Предыдущий слайд",
                                tint = if (selectedFrame.currentPageIndex > 0) Color(0xFF1E293B) else Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF007AFF).copy(alpha = 0.10f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${selectedFrame.currentPageIndex + 1} / ${selectedFrame.totalPages}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF007AFF)
                            )
                        }

                        IconButton(
                            onClick = { onFrameNextPage(selectedFrame.id) },
                            enabled = selectedFrame.currentPageIndex < selectedFrame.totalPages - 1,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                                contentDescription = "Следующий слайд",
                                tint = if (selectedFrame.currentPageIndex < selectedFrame.totalPages - 1) Color(0xFF1E293B) else Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Кнопка загрузки файла в данный фрейм
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2563EB))
                            .clickable {
                                val file = FileDialogs.openDocumentFileDialog()
                                if (file != null) {
                                    onLoadDocumentIntoFrame(selectedFrame.id, file)
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (selectedFrame.totalPages > 0) "Заменить файл" else "Загрузить PDF / PPTX...",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // 7. Интерактивная плавающая панель режима презентации (Presentation Mode HUD)
        if (isPresentationMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* Предотвращает клики на холст */ }
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = Color.Black.copy(alpha = 0.08f),
                        spotColor = Color.Black.copy(alpha = 0.16f)
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E293B).copy(alpha = 0.92f))
                    .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onPrevPresentationSlide,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                            contentDescription = "Предыдущий слайд (Left)",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Презентация",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    IconButton(
                        onClick = onNextPresentationSlide,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                            contentDescription = "Следующий слайд (Right)",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (workshopTimerState.isVisible) {
                        Spacer(
                            modifier = Modifier
                                .width(1.dp)
                                .height(18.dp)
                                .background(Color.White.copy(alpha = 0.20f))
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (workshopTimerState.isFinished) Color(0xFFEF4444).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f))
                                .clickable { if (workshopTimerState.isRunning) onTimerPause() else onTimerStart() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = if (workshopTimerState.isFinished) Color(0xFFEF4444) else Color(0xFF38BDF8),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = workshopTimerState.formattedTime,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier
                            .width(1.dp)
                            .height(18.dp)
                            .background(Color.White.copy(alpha = 0.20f))
                    )

                    // Тоггл лазерной указки
                    IconButton(
                        onClick = onToggleLaserPointer,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Highlight,
                            contentDescription = "Лазерная указка (L)",
                            tint = if (state.activeTool == CanvasTool.LASER_POINTER) Color(0xFFFF2A4B) else Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(
                        modifier = Modifier
                            .width(1.dp)
                            .height(18.dp)
                            .background(Color.White.copy(alpha = 0.20f))
                    )

                    // Выход из полноэкранного режима
                    IconButton(
                        onClick = onExitPresentationMode,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Выйти из режима презентации (Esc)",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Плавающий таймер воркшопов (Workshop Timer & Pomodoro)
        WorkshopTimerFloatingWidget(
            state = workshopTimerState,
            onStart = onTimerStart,
            onPause = onTimerPause,
            onReset = onTimerReset,
            onAddMinutes = onTimerAddMinutes,
            onSetPreset = onTimerSetPreset,
            onToggleMode = onTimerToggleMode,
            onToggleMinimize = onTimerToggleMinimize,
            onClose = onTimerClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 24.dp)
        )
    }
}
