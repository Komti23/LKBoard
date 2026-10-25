package com.lkboard.app.ui.canvas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isShiftPressed
import com.lkboard.app.model.AnchorPoint
import com.lkboard.app.model.ConnectorStyle
import com.lkboard.app.model.getAnchorOffset
import com.lkboard.app.model.BoardElement
import com.lkboard.app.model.BoardState
import com.lkboard.app.model.CanvasPoint
import com.lkboard.app.model.CanvasTool
import com.lkboard.app.model.GeometryUtils
import com.lkboard.app.model.ResizeHandle
import com.lkboard.app.util.PathSmoothing
import java.util.UUID

/**
 * Режимы интерактивного взаимодействия с холстом Lkboard.
 */
sealed interface InteractionMode {
    object Idle : InteractionMode

    data class Panning(var lastScreen: Offset) : InteractionMode

    data class DraggingElements(
        val elementIds: Set<String>,
        val startWorld: Offset,
        var accumulatedDelta: Offset = Offset.Zero
    ) : InteractionMode

    data class Resizing(
        val elementId: String,
        val handle: ResizeHandle,
        val initialBounds: Rect,
        val startWorld: Offset,
        var currentBounds: Rect
    ) : InteractionMode

    data class BoxSelecting(
        val startWorld: Offset,
        var currentWorld: Offset
    ) : InteractionMode

    data class Drawing(
        val pathId: String,
        val startPoint: CanvasPoint,
        val points: MutableList<CanvasPoint>,
        var isStraightLine: Boolean = false,
        var currentEndPoint: CanvasPoint = startPoint
    ) : InteractionMode

    data class Erasing(
        val erasedElementIds: MutableSet<String> = mutableSetOf()
    ) : InteractionMode

    data class CreatingShape(
        val startWorld: Offset,
        var currentWorld: Offset,
        var keepAspectRatio: Boolean = false
    ) : InteractionMode

    data class Connecting(
        val startElementId: String?,
        val startAnchor: AnchorPoint,
        val startPoint: CanvasPoint,
        var currentWorld: Offset,
        var targetElementId: String? = null,
        var targetAnchor: AnchorPoint = AnchorPoint.AUTO
    ) : InteractionMode

    data class Rotating(
        val elementIds: Set<String>,
        val centerWorld: Offset,
        val initialPointerAngle: Float,
        val initialRotations: Map<String, Float>,
        var currentAngleDelta: Float = 0f
    ) : InteractionMode
}

/**
 * Колбэки для передачи действий жестов холста во ViewModel.
 */
data class CanvasGestureCallbacks(
    val onPanBy: (Offset) -> Unit,
    val onSelectElement: (id: String, addToSelection: Boolean) -> Unit,
    val onSelectElements: (ids: Set<String>) -> Unit,
    val onClearSelection: () -> Unit,
    val onMoveSelectedLive: (deltaX: Float, deltaY: Float) -> Unit,
    val onCommitMove: (ids: Set<String>, totalDeltaX: Float, totalDeltaY: Float) -> Unit,
    val onResizeElementLive: (elementId: String, newBounds: Rect) -> Unit,
    val onCommitResize: (elementId: String, initialBounds: Rect, finalBounds: Rect) -> Unit,
    val onRotateElementsLive: (elementIds: Set<String>, initialRotations: Map<String, Float>, deltaAngle: Float) -> Unit = { _, _, _ -> },
    val onCommitRotate: (elementIds: Set<String>, initialRotations: Map<String, Float>, deltaAngle: Float) -> Unit = { _, _, _ -> },
    val onDoubleTapElement: (BoardElement) -> Unit,
    val onCreateStickyNote: (Offset) -> Unit,
    val onCreateShape: (left: Float, top: Float, width: Float, height: Float) -> Unit,
    val onCreateText: (Offset) -> Unit,
    val onCreateDocumentFrame: (Offset) -> Unit,
    val onCreateMilestoneCard: (Offset) -> Unit = {},
    val onCreateFunctionGraph: (Offset) -> Unit = {},
    val onCreateLatexFormula: (Offset) -> Unit = {},
    val onCreatePhysicsStencil: (Offset) -> Unit = {},
    val onCreateKanbanBoard: (Offset) -> Unit = {},
    val onToggleStickyTodo: (noteId: String, lineIndex: Int) -> Unit = { _, _ -> },
    val onCreateConnector: (BoardElement.Connector) -> Unit = {},
    val onCycleMilestoneStatus: (String) -> Unit = {},
    val onLaserPointMoved: (Offset) -> Unit = {},
    val onAddDrawingPath: (BoardElement.DrawingPath) -> Unit,
    val onDeleteElements: (List<BoardElement>) -> Unit,
    val onCursorPositionChange: (Offset) -> Unit
)

/**
 * Контроллер обработки указателя (Pointer / Mouse / Stylus) для бесконечного холста:
 * - Разделяет логику пера (рисование без выделения) и геометрических фигур (автовыделение + рамка трансформации).
 * - Обеспечивает интерактивное вытягивание фигуры с фиксацией пропорций 1:1 при зажатом Shift.
 * - Управляет панорамированием 1:1, перемещением элементов, ресайзом по маркерам и групповым выделением рамкой.
 */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
class CanvasGestureHandler(
    private val callbacks: CanvasGestureCallbacks
) {
    var interactionMode: InteractionMode by mutableStateOf(InteractionMode.Idle)
    var pendingCommittedPath: BoardElement.DrawingPath? by mutableStateOf(null)
    private var lastClickTime: Long = 0L
    private var lastClickPos: Offset = Offset.Zero
    val currentDrawingPoints: SnapshotStateList<CanvasPoint> = mutableStateListOf()

    fun findNearestAnchor(
        worldPos: Offset,
        elements: Collection<BoardElement>,
        excludeId: String? = null,
        snapRadius: Float = 24f
    ): Pair<BoardElement, AnchorPoint>? {
        var closest: Pair<BoardElement, AnchorPoint>? = null
        var minDistanceSq = snapRadius * snapRadius

        for (el in elements) {
            if (el.id == excludeId || el is BoardElement.Connector || el is BoardElement.DrawingPath) continue
            if (!el.bounds.inflate(snapRadius).contains(worldPos)) continue

            for (anchor in listOf(AnchorPoint.TOP, AnchorPoint.RIGHT, AnchorPoint.BOTTOM, AnchorPoint.LEFT)) {
                val pt = el.getAnchorOffset(anchor)
                val distSq = (worldPos.x - pt.x) * (worldPos.x - pt.x) + (worldPos.y - pt.y) * (worldPos.y - pt.y)
                if (distSq < minDistanceSq) {
                    minDistanceSq = distSq
                    closest = Pair(el, anchor)
                }
            }
        }
        return closest
    }

    fun handlePointerEvent(
        event: PointerEvent,
        boardState: BoardState,
        isPanMode: Boolean
    ) {
        val change = event.changes.firstOrNull() ?: return
        if (change.isConsumed) return
        val screenPos = change.position
        callbacks.onCursorPositionChange(screenPos)

        val worldPos = boardState.transform.screenToWorld(screenPos)
        val isShift = event.keyboardModifiers.isShiftPressed
        val isStraightMode = isShift || boardState.penSettings.isStraightLine

        when (event.type) {
            PointerEventType.Press -> {
                val isMiddle = event.button == PointerButton.Tertiary
                val isPrimary = event.button == PointerButton.Primary

                val currentTime = System.currentTimeMillis()
                val isDoubleTap = (currentTime - lastClickTime < 350L) &&
                        ((screenPos - lastClickPos).getDistance() < 10f)
                lastClickTime = currentTime
                lastClickPos = screenPos

                if (isMiddle || isPanMode) {
                    // Панорамирование холста (1:1 пиксели экрана)
                    interactionMode = InteractionMode.Panning(screenPos)
                    change.consume()
                } else if (isPrimary) {
                    when (boardState.activeTool) {
                        CanvasTool.SELECT -> {
                            val hitElement = GeometryUtils.hitTestElement(worldPos, boardState.sortedElements)
                            if (hitElement is BoardElement.MilestoneCard) {
                                val inBadgeX = worldPos.x >= (hitElement.x + 16f) && worldPos.x <= (hitElement.x + 130f)
                                val inBadgeY = worldPos.y >= (hitElement.y + 10f) && worldPos.y <= (hitElement.y + 38f)
                                if (inBadgeX && inBadgeY) {
                                    callbacks.onCycleMilestoneStatus(hitElement.id)
                                    callbacks.onSelectElement(hitElement.id, false)
                                    interactionMode = InteractionMode.Idle
                                    change.consume()
                                    return
                                }
                            }

                            if (hitElement is BoardElement.StickyNote) {
                                val textLen = hitElement.text.length
                                val autoFontSize = when {
                                    textLen < 35 -> (hitElement.width / 11.5f).coerceIn(15f, 20f)
                                    textLen < 85 -> (hitElement.width / 13.5f).coerceIn(13.5f, 16.5f)
                                    textLen < 160 -> (hitElement.width / 15.5f).coerceIn(12f, 14f)
                                    else -> (hitElement.width / 18f).coerceIn(10.5f, 12f)
                                }
                                val pad = 16f
                                val lineH = autoFontSize * 1.5f
                                val lines = hitElement.text.split("\n")
                                var curY = hitElement.y + pad
                                for ((lineIdx, rawLine) in lines.withIndex()) {
                                    if (curY + lineH > hitElement.y + hitElement.height - pad) break
                                    val trimmed = rawLine.trimStart()
                                    val isTodo = trimmed.startsWith("- [x] ", ignoreCase = true) || trimmed.startsWith("[x] ", ignoreCase = true) ||
                                                 trimmed.startsWith("- [ ] ") || trimmed.startsWith("[ ] ")
                                    if (isTodo) {
                                        val cbSize = (autoFontSize * 1.05f).coerceIn(13f, 18f)
                                        val cbY = curY + (lineH - cbSize) / 2f
                                        val cbX = hitElement.x + pad
                                        val inCbX = worldPos.x >= (cbX - 4f) && worldPos.x <= (cbX + cbSize + 6f)
                                        val inCbY = worldPos.y >= (cbY - 4f) && worldPos.y <= (cbY + cbSize + 6f)
                                        if (inCbX && inCbY) {
                                            callbacks.onToggleStickyTodo(hitElement.id, lineIdx)
                                            callbacks.onSelectElement(hitElement.id, false)
                                            interactionMode = InteractionMode.Idle
                                            change.consume()
                                            return
                                        }
                                    }
                                    curY += lineH
                                }
                            }

                            if (isDoubleTap && hitElement != null) {
                                callbacks.onDoubleTapElement(hitElement)
                                interactionMode = InteractionMode.Idle
                            } else {
                                val bounds = GeometryUtils.computeBounds(boardState.selectedElements)
                                val singleElement = if (boardState.selectedElements.size == 1) boardState.selectedElements.first() else null
                                val currentRotation = singleElement?.rotation ?: 0f

                                // 1. Проверка клика по маркеру вращения (Rotation Handle)
                                val isRotationHandleHit = bounds != null && boardState.selectedElements.isNotEmpty() &&
                                        GeometryUtils.hitTestRotationHandle(worldPos, bounds, currentRotation, boardState.transform.scale)

                                if (isRotationHandleHit) {
                                    val b = bounds!!
                                    val centerWorld = b.center
                                    val dx = worldPos.x - centerWorld.x
                                    val dy = worldPos.y - centerWorld.y
                                    val initialAngle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                    val initialRotations = boardState.selectedElements.associate { it.id to it.rotation }
                                    interactionMode = InteractionMode.Rotating(
                                        elementIds = boardState.selectedElementIds,
                                        centerWorld = centerWorld,
                                        initialPointerAngle = initialAngle,
                                        initialRotations = initialRotations
                                    )
                                    change.consume()
                                    return
                                }

                                // 2. Проверка клика по маркеру ресайза (только если выделен 1 элемент)
                                val handle = if (bounds != null && boardState.selectedElements.size == 1) {
                                    val handleRadius = 8f / boardState.transform.scale
                                    GeometryUtils.hitTestHandle(worldPos, bounds, handleRadius)
                                } else null

                                if (handle != null && boardState.selectedElements.size == 1) {
                                    val el = boardState.selectedElements.first()
                                    interactionMode = InteractionMode.Resizing(
                                        elementId = el.id,
                                        handle = handle,
                                        initialBounds = el.bounds,
                                        startWorld = worldPos,
                                        currentBounds = el.bounds
                                    )
                                } else if (hitElement != null) {
                                    // Клик по элементу (фигура, текст, заметка, штрих, стенсил)
                                    val targetIds = if (hitElement.id in boardState.selectedElementIds) {
                                        boardState.selectedElementIds
                                    } else {
                                        callbacks.onSelectElement(hitElement.id, isShift)
                                        if (isShift) boardState.selectedElementIds + hitElement.id else setOf(hitElement.id)
                                    }
                                    interactionMode = InteractionMode.DraggingElements(
                                        elementIds = targetIds,
                                        startWorld = worldPos,
                                        accumulatedDelta = Offset.Zero
                                    )
                                } else {
                                    // Клик по пустому холсту — сброс выделения или рамка выбора
                                    if (!isShift) {
                                        callbacks.onClearSelection()
                                    }
                                    interactionMode = InteractionMode.BoxSelecting(
                                        startWorld = worldPos,
                                        currentWorld = worldPos
                                    )
                                }
                            }
                        }

                        CanvasTool.STICKY_NOTE -> {
                            callbacks.onCreateStickyNote(worldPos)
                            interactionMode = InteractionMode.Idle
                            change.consume()
                        }

                        CanvasTool.SHAPE -> {
                            // Начало интерактивного вытягивания геометрической фигуры
                            callbacks.onClearSelection()
                            interactionMode = InteractionMode.CreatingShape(
                                startWorld = worldPos,
                                currentWorld = worldPos,
                                keepAspectRatio = isShift
                            )
                            change.consume()
                        }

                        CanvasTool.CONNECTOR -> {
                            callbacks.onClearSelection()
                            val snapRadius = 24f / boardState.transform.scale
                            val nearest = findNearestAnchor(worldPos, boardState.elements.values, snapRadius = snapRadius)
                            val startEl = nearest?.first ?: GeometryUtils.hitTestElement(worldPos, boardState.sortedElements)
                            val startAnchor = nearest?.second ?: if (startEl != null) {
                                val b = startEl.bounds
                                val dTop = kotlin.math.abs(worldPos.y - b.top)
                                val dBottom = kotlin.math.abs(worldPos.y - b.bottom)
                                val dLeft = kotlin.math.abs(worldPos.x - b.left)
                                val dRight = kotlin.math.abs(worldPos.x - b.right)
                                val minD = minOf(dTop, dBottom, dLeft, dRight)
                                when (minD) {
                                    dTop -> AnchorPoint.TOP
                                    dBottom -> AnchorPoint.BOTTOM
                                    dLeft -> AnchorPoint.LEFT
                                    else -> AnchorPoint.RIGHT
                                }
                            } else AnchorPoint.AUTO

                            val startPt = if (startEl != null && startAnchor != AnchorPoint.AUTO) {
                                CanvasPoint.fromOffset(startEl.getAnchorOffset(startAnchor))
                            } else {
                                CanvasPoint.fromOffset(worldPos)
                            }

                            interactionMode = InteractionMode.Connecting(
                                startElementId = startEl?.id,
                                startAnchor = startAnchor,
                                startPoint = startPt,
                                currentWorld = worldPos
                            )
                            change.consume()
                        }

                        CanvasTool.MILESTONE -> {
                            callbacks.onCreateMilestoneCard(worldPos)
                            interactionMode = InteractionMode.Idle
                            change.consume()
                        }

                        CanvasTool.FUNCTION_GRAPH -> {
                            callbacks.onCreateFunctionGraph(worldPos)
                            interactionMode = InteractionMode.Idle
                            change.consume()
                        }

                        CanvasTool.LATEX -> {
                            callbacks.onCreateLatexFormula(worldPos)
                            interactionMode = InteractionMode.Idle
                            change.consume()
                        }

                        CanvasTool.PHYSICS_STENCIL -> {
                            callbacks.onCreatePhysicsStencil(worldPos)
                            interactionMode = InteractionMode.Idle
                            change.consume()
                        }

                        CanvasTool.LASER_POINTER -> {
                            callbacks.onLaserPointMoved(worldPos)
                            change.consume()
                        }

                        CanvasTool.KANBAN -> {
                            callbacks.onCreateKanbanBoard(worldPos)
                            interactionMode = InteractionMode.Idle
                            change.consume()
                        }

                        CanvasTool.TEXT -> {
                            callbacks.onCreateText(worldPos)
                            interactionMode = InteractionMode.Idle
                        }

                        CanvasTool.FRAME -> {
                            callbacks.onCreateDocumentFrame(worldPos)
                            interactionMode = InteractionMode.Idle
                        }

                        CanvasTool.PEN -> {
                            // Рисование пером от руки: выделение сбрасывается для непрерывного письма
                            callbacks.onClearSelection()
                            val pathId = "path-${UUID.randomUUID()}"
                            val startPt = CanvasPoint.fromOffset(worldPos)
                            currentDrawingPoints.clear()
                            currentDrawingPoints.add(startPt)
                            interactionMode = InteractionMode.Drawing(
                                pathId = pathId,
                                startPoint = startPt,
                                points = currentDrawingPoints,
                                isStraightLine = isStraightMode,
                                currentEndPoint = startPt
                            )
                        }

                        CanvasTool.HAND -> {
                            interactionMode = InteractionMode.Panning(screenPos)
                            change.consume()
                        }

                        CanvasTool.ERASER -> {
                            val eraserRadiusWorld = 12f / boardState.transform.scale
                            val hitElements = GeometryUtils.hitTestEraser(
                                worldPos,
                                boardState.elements.values,
                                eraserRadiusWorld
                            )
                            val erasedIds = mutableSetOf<String>()
                            if (hitElements.isNotEmpty()) {
                                callbacks.onDeleteElements(hitElements)
                                hitElements.forEach { erasedIds.add(it.id) }
                            }
                            interactionMode = InteractionMode.Erasing(erasedIds)
                            change.consume()
                        }
                    }
                }
            }

            PointerEventType.Move -> {
                when (val mode = interactionMode) {
                    is InteractionMode.Panning -> {
                        val deltaScreen = screenPos - mode.lastScreen
                        mode.lastScreen = screenPos
                        if (deltaScreen.x != 0f || deltaScreen.y != 0f) {
                            callbacks.onPanBy(deltaScreen)
                        }
                        change.consume()
                    }

                    is InteractionMode.Erasing -> {
                        val eraserRadiusWorld = 12f / boardState.transform.scale
                        val hitElements = GeometryUtils.hitTestEraser(
                            worldPos,
                            boardState.elements.values,
                            eraserRadiusWorld
                        )
                        val newHits = hitElements.filter { it.id !in mode.erasedElementIds }
                        if (newHits.isNotEmpty()) {
                            callbacks.onDeleteElements(newHits)
                            newHits.forEach { mode.erasedElementIds.add(it.id) }
                        }
                        change.consume()
                    }

                    is InteractionMode.DraggingElements -> {
                        val deltaWorld = worldPos - (mode.startWorld + mode.accumulatedDelta)
                        mode.accumulatedDelta += deltaWorld
                        callbacks.onMoveSelectedLive(deltaWorld.x, deltaWorld.y)
                        change.consume()
                    }

                    is InteractionMode.Resizing -> {
                        val delta = worldPos - mode.startWorld
                        val newBounds = GeometryUtils.calculateResizedBounds(
                            initialBounds = mode.initialBounds,
                            handle = mode.handle,
                            deltaWorld = delta,
                            keepAspectRatio = isShift
                        )
                        mode.currentBounds = newBounds
                        callbacks.onResizeElementLive(mode.elementId, newBounds)
                        change.consume()
                    }

                    is InteractionMode.Rotating -> {
                        val dx = worldPos.x - mode.centerWorld.x
                        val dy = worldPos.y - mode.centerWorld.y
                        val currentPointerAngle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                        var deltaAngle = currentPointerAngle - mode.initialPointerAngle
                        if (isShift) {
                            // При удержании Shift шаг привязки 15 градусов
                            deltaAngle = kotlin.math.round(deltaAngle / 15f) * 15f
                        }
                        mode.currentAngleDelta = deltaAngle
                        callbacks.onRotateElementsLive(mode.elementIds, mode.initialRotations, deltaAngle)
                        change.consume()
                    }

                    is InteractionMode.BoxSelecting -> {
                        mode.currentWorld = worldPos
                        change.consume()
                    }

                    is InteractionMode.Drawing -> {
                        val currentPt = CanvasPoint.fromOffset(worldPos)
                        mode.currentEndPoint = currentPt
                        if (isStraightMode) {
                            mode.isStraightLine = true
                        } else {
                            mode.isStraightLine = false
                            val last = mode.points.lastOrNull()
                            if (last == null || (worldPos - last.toOffset()).getDistance() >= 2.0f) {
                                mode.points.add(currentPt)
                            }
                        }
                        change.consume()
                    }

                    is InteractionMode.CreatingShape -> {
                        mode.currentWorld = worldPos
                        mode.keepAspectRatio = isShift
                        change.consume()
                    }

                    is InteractionMode.Connecting -> {
                        val snapRadius = 24f / boardState.transform.scale
                        val nearest = findNearestAnchor(worldPos, boardState.elements.values, excludeId = mode.startElementId, snapRadius = snapRadius)
                        if (nearest != null) {
                            mode.targetElementId = nearest.first.id
                            mode.targetAnchor = nearest.second
                            mode.currentWorld = nearest.first.getAnchorOffset(nearest.second)
                        } else {
                            mode.targetElementId = null
                            mode.targetAnchor = AnchorPoint.AUTO
                            mode.currentWorld = worldPos
                        }
                        change.consume()
                    }

                    InteractionMode.Idle -> {
                        if (boardState.activeTool == CanvasTool.LASER_POINTER) {
                            callbacks.onLaserPointMoved(worldPos)
                        }
                    }
                }
            }

            PointerEventType.Release -> {
                when (val mode = interactionMode) {
                    is InteractionMode.Panning -> {
                        change.consume()
                    }

                    is InteractionMode.Erasing -> {
                        change.consume()
                    }

                    is InteractionMode.DraggingElements -> {
                        callbacks.onCommitMove(
                            mode.elementIds,
                            mode.accumulatedDelta.x,
                            mode.accumulatedDelta.y
                        )
                    }

                    is InteractionMode.Resizing -> {
                        callbacks.onCommitResize(
                            mode.elementId,
                            mode.initialBounds,
                            mode.currentBounds
                        )
                    }

                    is InteractionMode.Rotating -> {
                        callbacks.onCommitRotate(
                            mode.elementIds,
                            mode.initialRotations,
                            mode.currentAngleDelta
                        )
                    }

                    is InteractionMode.BoxSelecting -> {
                        val left = minOf(mode.startWorld.x, mode.currentWorld.x)
                        val top = minOf(mode.startWorld.y, mode.currentWorld.y)
                        val right = maxOf(mode.startWorld.x, mode.currentWorld.x)
                        val bottom = maxOf(mode.startWorld.y, mode.currentWorld.y)
                        val box = Rect(left, top, right, bottom)
                        if (box.width > 3f || box.height > 3f) {
                            val insideElements = GeometryUtils.findElementsInBox(
                                box,
                                boardState.elements.values
                            )
                            val ids = insideElements.map { it.id }.toSet()
                            if (ids.isNotEmpty()) {
                                callbacks.onSelectElements(if (isShift) boardState.selectedElementIds + ids else ids)
                            }
                        }
                    }

                    is InteractionMode.Drawing -> {
                        // Завершение рисования пером: точное сохранение штриха без деформации и мерцания
                        val finalPoints = if (mode.isStraightLine) {
                            PathSmoothing.makeStraightLine(mode.startPoint, mode.currentEndPoint)
                        } else {
                            val pts = mode.points.toMutableList()
                            if (pts.isEmpty()) {
                                pts.add(mode.startPoint)
                            } else {
                                val last = pts.last()
                                val endPt = CanvasPoint.fromOffset(worldPos)
                                val dist = kotlin.math.hypot(endPt.x - last.x, endPt.y - last.y)
                                if (dist >= 1.0f) {
                                    pts.add(endPt)
                                }
                            }
                            pts
                        }
                        if (finalPoints.isNotEmpty()) {
                            val first = finalPoints.first()
                            val path = BoardElement.DrawingPath(
                                id = mode.pathId,
                                x = first.x,
                                y = first.y,
                                points = finalPoints,
                                strokeColorHex = boardState.penSettings.colorHex,
                                strokeWidth = boardState.penSettings.strokeWidth,
                                isHighlighter = boardState.penSettings.isHighlighter
                            )
                            // Сохраняем в pending, чтобы штрих не исчезал ни на один кадр при смене стейта Compose
                            pendingCommittedPath = path
                            callbacks.onAddDrawingPath(path)
                        }
                        currentDrawingPoints.clear()
                        callbacks.onClearSelection()
                    }

                    is InteractionMode.CreatingShape -> {
                        // Завершение создания фигуры: вычисление геометрии и АВТОВЫДЕЛЕНИЕ
                        val dx = mode.currentWorld.x - mode.startWorld.x
                        val dy = mode.currentWorld.y - mode.startWorld.y
                        val isRatioLocked = isShift || mode.keepAspectRatio

                        val endX: Float
                        val endY: Float
                        if (isRatioLocked) {
                            val size = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy))
                            val targetW = if (dx >= 0) size else -size
                            val targetH = if (dy >= 0) size else -size
                            endX = mode.startWorld.x + targetW
                            endY = mode.startWorld.y + targetH
                        } else {
                            endX = mode.currentWorld.x
                            endY = mode.currentWorld.y
                        }

                        val left = minOf(mode.startWorld.x, endX)
                        val top = minOf(mode.startWorld.y, endY)
                        val width = kotlin.math.abs(endX - mode.startWorld.x)
                        val height = kotlin.math.abs(endY - mode.startWorld.y)

                        if (width < 6f && height < 6f) {
                            // Клик без перетаскивания создает фигуру стандартного размера
                            callbacks.onCreateShape(
                                mode.startWorld.x - 80f,
                                mode.startWorld.y - 50f,
                                160f,
                                100f
                            )
                        } else {
                            // Вытянутая пользователем фигура
                            callbacks.onCreateShape(
                                left,
                                top,
                                width.coerceAtLeast(16f),
                                height.coerceAtLeast(16f)
                            )
                        }
                        // Фигура автовыделена во ViewModel, onClearSelection НЕ вызываем!
                        change.consume()
                    }

                    is InteractionMode.Connecting -> {
                        val dist = (mode.currentWorld - mode.startPoint.toOffset()).getDistance()
                        if (dist >= 10f) {
                            val connector = BoardElement.Connector(
                                id = "connector-${UUID.randomUUID()}",
                                startElementId = mode.startElementId,
                                startAnchor = mode.startAnchor,
                                startPoint = mode.startPoint,
                                endElementId = mode.targetElementId,
                                endAnchor = mode.targetAnchor,
                                endPoint = CanvasPoint.fromOffset(mode.currentWorld),
                                style = ConnectorStyle.ORTHOGONAL,
                                hasEndArrow = true
                            )
                            callbacks.onCreateConnector(connector)
                        }
                        change.consume()
                    }

                    InteractionMode.Idle -> {}
                }
                interactionMode = InteractionMode.Idle
            }
        }
    }
}
