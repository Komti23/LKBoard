package com.lkboard.app.ui.dashboard

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import com.lkboard.app.ui.animation.bouncyClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.runtime.produceState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import com.lkboard.app.data.AppPreferences
import com.lkboard.app.model.GridType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage
import java.io.File
import java.util.Calendar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.CircularProgressIndicator
import com.lkboard.app.update.AppVersion
import com.lkboard.app.update.UpdateCheckState
import com.lkboard.app.ui.theme.LocalAppTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lkboard.app.data.BoardItemInfo
import com.lkboard.app.data.BoardTemplate
import com.lkboard.app.ui.components.AppLogo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Главный экран Lkboard (Dashboard / Менеджер проектов):
 * - Сетка всех сохраненных досок с превью и метаданными
 * - Быстрый поиск досок по названию
 * - Создание новых досок по шаблонам (Чистый холст, Дорожная карта, Канбан, Мозговой штурм)
 * - Дублирование, переименование, удаление и импорт .lkboard файлов
 * - Премиальный стиль Apple Liquid Glass
 */
@Composable
fun DashboardScreen(
    boards: List<BoardItemInfo>,
    activeBoardId: String?,
    onOpenBoard: (String) -> Unit,
    onCreateBoard: (String, BoardTemplate) -> Unit,
    onDuplicateBoard: (String) -> Unit,
    onRenameBoard: (String, String) -> Unit,
    onDeleteBoard: (String) -> Unit,
    onImportBoard: () -> Unit,
    updateState: UpdateCheckState = UpdateCheckState(),
    onCheckUpdates: () -> Unit = {},
    onOpenUpdateDialog: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenVersionDetails: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var boardToRename by remember { mutableStateOf<BoardItemInfo?>(null) }
    var boardToDelete by remember { mutableStateOf<BoardItemInfo?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newBoardTemplate by remember { mutableStateOf(BoardTemplate.BLANK) }

    val isDark = LocalAppTheme.current.isDark

    val filteredBoards = remember(boards, searchQuery) {
        val list = if (searchQuery.isBlank()) boards
        else boards.filter { it.title.contains(searchQuery.trim(), ignoreCase = true) }
        list.distinctBy { it.id }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF0F1115) else Color(0xFFF7F8FA))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 36.dp, vertical = 24.dp)
        ) {
            // 1. Верхний стеклянный хедер (Header)
            DashboardHeader(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onImportClick = onImportBoard,
                onCreateClick = {
                    newBoardTemplate = BoardTemplate.BLANK
                    showCreateDialog = true
                },
                updateState = updateState,
                onCheckUpdates = onCheckUpdates,
                onOpenUpdateDialog = onOpenUpdateDialog,
                onOpenSettings = onOpenSettings,
                onOpenVersionDetails = onOpenVersionDetails,
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 2. Секция шаблонов быстрого старта (Быстрое создание Roadmap, Kanban и т.д.)
            TemplatesSection(
                onSelectTemplate = { template ->
                    val defaultName = when (template) {
                        BoardTemplate.BLANK -> "Новая доска"
                        BoardTemplate.ROADMAP -> "Дорожная карта: Проект 2026"
                        BoardTemplate.KANBAN -> "Канбан-доска"
                        BoardTemplate.BRAINSTORM -> "Мозговой штурм"
                        BoardTemplate.MATH_STUDIO -> "Математика: Графики и Функции"
                    }
                    onCreateBoard(defaultName, template)
                },
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 3. Заголовок секции досок
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Все доски",
                        fontSize = 20.dp.value.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF242830) else Color(0xFFE2E8F0))
                            .padding(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${filteredBoards.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Сетка карточек досок (Boards Grid)
            if (filteredBoards.isEmpty()) {
                EmptyBoardsState(
                    isSearch = searchQuery.isNotBlank(),
                    onClearSearch = { searchQuery = "" },
                    onCreateNew = {
                        newBoardTemplate = BoardTemplate.BLANK
                        showCreateDialog = true
                    },
                    isDark = isDark
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 270.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(top = 14.dp, bottom = 36.dp, start = 8.dp, end = 8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredBoards, key = { it.id }) { board ->
                        BoardCard(
                            board = board,
                            isActive = board.id == activeBoardId,
                            onOpen = { onOpenBoard(board.id) },
                            onRename = { boardToRename = board },
                            onDuplicate = { onDuplicateBoard(board.id) },
                            onDelete = { boardToDelete = board }
                        )
                    }
                }
            }
        }

        // Диалог создания новой доски
        if (showCreateDialog) {
            CreateBoardDialog(
                initialTemplate = newBoardTemplate,
                onDismiss = { showCreateDialog = false },
                onCreate = { title, template ->
                    showCreateDialog = false
                    onCreateBoard(title, template)
                }
            )
        }

        // Диалог переименования доски
        boardToRename?.let { board ->
            RenameBoardDialog(
                currentTitle = board.title,
                onDismiss = { boardToRename = null },
                onConfirm = { newTitle ->
                    boardToRename = null
                    onRenameBoard(board.id, newTitle)
                }
            )
        }

        // Диалог подтверждения удаления доски
        boardToDelete?.let { board ->
            DeleteConfirmDialog(
                boardTitle = board.title,
                onDismiss = { boardToDelete = null },
                onConfirm = {
                    boardToDelete = null
                    onDeleteBoard(board.id)
                }
            )
        }
    }
}

/**
 * Стеклянный хедер Главного экрана.
 */
@Composable
private fun DashboardHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onImportClick: () -> Unit,
    onCreateClick: () -> Unit,
    updateState: UpdateCheckState,
    onCheckUpdates: () -> Unit,
    onOpenUpdateDialog: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenVersionDetails: () -> Unit = {},
    isDark: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color.Black.copy(alpha = 0.03f),
                spotColor = Color.Black.copy(alpha = 0.06f)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(if (isDark) Color(0xFF181B20).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.90f))
            .border(BorderStroke(1.dp, if (isDark) Color(0xFF27272A) else Color.White), RoundedCornerShape(18.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Бренд: логотип + название
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AppLogo.LogoIcon(size = 52.dp)

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "LKBoard",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A),
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF))
                                .border(1.dp, if (isDark) Color(0xFF38BDF8).copy(alpha = 0.4f) else Color(0xFFBFDBFE), RoundedCornerShape(8.dp))
                                .bouncyClickable { onOpenVersionDetails() }
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "v${AppVersion.VERSION_NAME}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)
                                )
                                Text(
                                    text = "✨ Что нового",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                            }
                        }
                    }
                    Text(
                        text = "Бесконечные доски, дорожные карты & презентации",
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }
            }

            // Поисковая строка и кнопки действий
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Кнопка проверки обновлений / индикатор новой версии
                val hasUpdate = updateState.updateAvailable && updateState.updateInfo != null
                Button(
                    onClick = if (hasUpdate) onOpenUpdateDialog else onCheckUpdates,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasUpdate) Color(0xFFFEF3C7) else if (isDark) Color(0xFF242830) else Color(0xFFF1F5F9),
                        contentColor = if (hasUpdate) Color(0xFFB45309) else if (isDark) Color(0xFFE4E4E7) else Color(0xFF475569)
                    ),
                    border = if (hasUpdate) BorderStroke(1.5.dp, Color(0xFFF59E0B)) else null,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    if (updateState.isChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF2563EB)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Проверка...", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    } else if (hasUpdate) {
                        Icon(
                            imageVector = Icons.Default.RocketLaunch,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Доступно v${updateState.updateInfo!!.version}!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Обновления", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }

                // Поле поиска
                Box(
                    modifier = Modifier
                        .width(280.dp)
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF242830) else Color(0xFFF1F5F9))
                        .border(1.dp, if (isDark) Color(0xFF333842) else Color.Transparent, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Поиск",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onSearchQueryChange,
                            placeholder = {
                                Text(
                                    "Поиск по доскам...",
                                    fontSize = 13.sp,
                                    color = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
                                )
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A),
                                unfocusedTextColor = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A),
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { onSearchQueryChange("") },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Очистить",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Кнопка импорта
                Button(
                    onClick = onImportClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF242830) else Color(0xFFF1F5F9),
                        contentColor = if (isDark) Color(0xFFE4E4E7) else Color(0xFF334155)
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Импорт",
                        modifier = Modifier.size(17.dp),
                        tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Импорт",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0xFFE4E4E7) else Color(0xFF334155)
                    )
                }

                // Кнопка настроек
                Button(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF242830) else Color(0xFFF1F5F9),
                        contentColor = if (isDark) Color(0xFFE4E4E7) else Color(0xFF334155)
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Настройки",
                        modifier = Modifier.size(17.dp),
                        tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Настройки",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0xFFE4E4E7) else Color(0xFF334155)
                    )
                }

                // Кнопка новой доски
                Button(
                    onClick = onCreateClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Создать",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Новая доска",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Секция быстрых шаблонов создания доски (включая Roadmap).
 */
@Composable
private fun TemplatesSection(
    onSelectTemplate: (BoardTemplate) -> Unit,
    isDark: Boolean = false
) {
    Column {
        Text(
            text = "Быстрый старт с шаблонами",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BoardTemplate.values().forEach { template ->
                val isRoadmap = template == BoardTemplate.ROADMAP
                val cardBorder = if (isRoadmap) {
                    BorderStroke(1.5.dp, if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB).copy(alpha = 0.6f))
                } else {
                    BorderStroke(1.dp, if (isDark) Color(0xFF27272A) else Color.White)
                }

                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val isHovered by interactionSource.collectIsHoveredAsState()
                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.98f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                    label = "templateCardScale"
                )
                val translateY by animateFloatAsState(
                    targetValue = when {
                        isPressed -> 2f
                        isHovered -> -4f
                        else -> 0f
                    },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                    label = "templateCardTranslateY"
                )
                val elevation by animateDpAsState(
                    targetValue = when {
                        isPressed -> 2.dp
                        isHovered -> 12.dp
                        isRoadmap -> 8.dp
                        else -> 3.dp
                    },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                    label = "templateCardElevation"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp, horizontal = 2.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationY = translateY.dp.toPx()
                        }
                        .shadow(
                            elevation = elevation,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = Color.Black.copy(alpha = 0.02f),
                            spotColor = if (isRoadmap) Color(0x332563EB) else Color.Black.copy(alpha = 0.05f)
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isRoadmap) {
                                if (isDark) Color(0xFF1E2638) else Color(0xFFF8FAFF)
                            } else {
                                if (isDark) Color(0xFF181B20) else Color.White
                            }
                        )
                        .border(cardBorder, RoundedCornerShape(16.dp))
                        .clickable(interactionSource = interactionSource, indication = null) { onSelectTemplate(template) }
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val templateVector = when (template) {
                                BoardTemplate.BLANK -> Icons.Default.Article
                                BoardTemplate.ROADMAP -> Icons.Default.Timeline
                                BoardTemplate.KANBAN -> Icons.Default.ViewColumn
                                BoardTemplate.BRAINSTORM -> Icons.Default.Psychology
                                BoardTemplate.MATH_STUDIO -> Icons.Default.ShowChart
                            }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isRoadmap) (if (isDark) Color(0xFF202A3C) else Color(0xFFDBEAFE))
                                        else (if (isDark) Color(0xFF222630) else Color(0xFFF1F5F9))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = templateVector,
                                    contentDescription = null,
                                    tint = if (isRoadmap) (if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = template.displayName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = template.description,
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            lineHeight = 15.sp,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

/**
 * Премиальная карточка сохраненной доски в стиле Apple Liquid Glass:
 * - Отображает живую миниатюру сцены (thumbnail) или стилизованную сетку холста (dots, lines, grid)
 * - Стеклянные бейджи шаблона и количества элементов
 * - Плавные hover-эффекты с подъемом карточки, подсветкой границ и быстрой кнопкой перехода
 * - Человекопонятное форматирование даты изменения
 */
@Composable
private fun BoardCard(
    board: BoardItemInfo,
    isActive: Boolean,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = LocalAppTheme.current.isDark
    var menuOpen by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.99f else if (isHovered) 1.006f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "boardCardScale"
    )
    val translateY by animateFloatAsState(
        targetValue = when {
            isPressed -> 1.5f
            isHovered -> -3f
            else -> 0f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "boardCardTranslateY"
    )
    val elevation by animateDpAsState(
        targetValue = when {
            isPressed -> 2.dp
            isHovered -> 10.dp
            isActive -> 8.dp
            else -> 3.dp
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "boardCardElevation"
    )

    // Асинхронная подгрузка миниатюры доски с генерацией на лету при необходимости
    val thumbnailBitmap by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = board.id,
        key2 = board.updatedAt
    ) {
        value = withContext(Dispatchers.IO) {
            val thumbFile = File(AppPreferences.getAppDataDir(), "boards/thumbnails/thumb_${board.id}.png")
            if (!thumbFile.exists() || thumbFile.length() == 0L) {
                if (board.elementCount > 0) {
                    try {
                        val boardFile = File(AppPreferences.getAppDataDir(), "boards/${board.fileName}")
                        if (boardFile.exists()) {
                            val state = com.lkboard.app.data.BoardSerializer.load(boardFile)
                            com.lkboard.app.export.PngExporter.renderThumbnailToPng(thumbFile, state)
                        }
                    } catch (_: Exception) {}
                }
            }
            if (thumbFile.exists() && thumbFile.length() > 0) {
                try {
                    val bytes = thumbFile.readBytes()
                    SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
                } catch (_: Exception) {
                    null
                }
            } else {
                null
            }
        }
    }

    val formattedDate = remember(board.updatedAt) {
        formatRelativeBoardDate(board.updatedAt)
    }

    val borderColor = when {
        isActive -> if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)
        isHovered -> if (isDark) Color(0xFF475569) else Color(0xFF93C5FD)
        else -> if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 4.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = translateY.dp.toPx()
            }
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color.Black.copy(alpha = 0.03f),
                spotColor = if (isActive) Color(0x332563EB) else if (isHovered) Color(0x22000000) else Color.Black.copy(alpha = 0.05f)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(if (isDark) Color(0xFF181B20) else Color.White)
            .border(
                BorderStroke(if (isActive) 1.5.dp else 1.dp, borderColor),
                RoundedCornerShape(18.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null) { onOpen() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Верхнее превью (Preview Banner с миниатюрой или сеткой)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(134.dp)
                    .background(Color(board.backgroundColorHex))
            ) {
                // Фоновая сетка холста (DOTS, LINES, GRID)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val isLightBg = Color(board.backgroundColorHex).luminance() > 0.5f
                    val gridColor = if (isLightBg) Color(0xFF475569).copy(alpha = 0.12f) else Color.White.copy(alpha = 0.12f)
                    val step = 16.dp.toPx()

                    when (board.gridType) {
                        GridType.DOTS -> {
                            var x = step / 2f
                            while (x < size.width) {
                                var y = step / 2f
                                while (y < size.height) {
                                    drawCircle(gridColor, radius = 1.3.dp.toPx(), center = Offset(x, y))
                                    y += step
                                }
                                x += step
                            }
                        }
                        GridType.LINES -> {
                            var y = step
                            while (y < size.height) {
                                drawLine(gridColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1.dp.toPx())
                                y += step
                            }
                        }
                        else -> {}
                    }
                }

                // Отображение реальной миниатюры содержимого
                if (thumbnailBitmap != null) {
                    Image(
                        bitmap = thumbnailBitmap!!,
                        contentDescription = board.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                } else if (board.elementCount == 0) {
                    // Индикатор чистого холста
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isDark) Color(0xFF1E222A).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.80f))
                            .border(1.dp, if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Чистый холст",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                    }
                }

                // Мягкий нижний градиент для перехода к карточке
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    if (isDark) Color(0xFF181B20).copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.05f)
                                )
                            )
                        )
                )

                // Верхняя плашка с бейджами (Шаблон + Статус / Hover кнопка)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    val (templateIconVector, templateTitle) = when (board.templateType) {
                        BoardTemplate.ROADMAP.name -> Icons.Default.Timeline to "Roadmap"
                        BoardTemplate.KANBAN.name -> Icons.Default.ViewColumn to "Канбан"
                        BoardTemplate.BRAINSTORM.name -> Icons.Default.Psychology to "Mindmap"
                        BoardTemplate.MATH_STUDIO.name -> Icons.Default.ShowChart to "Графика"
                        else -> Icons.Default.Article to "Доска"
                    }

                    // Стеклянный бейдж типа шаблона
                    Box(
                        modifier = Modifier
                            .shadow(2.dp, RoundedCornerShape(8.dp), ambientColor = Color.Black.copy(alpha = 0.04f))
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) Color(0xFF181B20).copy(alpha = 0.88f) else Color.White.copy(alpha = 0.90f))
                            .border(1.dp, if (isDark) Color(0xFF2E333D) else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = templateIconVector,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = templateTitle,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF1E293B)
                            )
                        }
                    }

                    // Индикатор "Открыта" или кнопка быстрого открытия при Hover
                    if (isActive) {
                        Box(
                            modifier = Modifier
                                .shadow(2.dp, RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDark) Color(0xFF0F291E) else Color(0xFFDCFCE7))
                                .border(1.dp, if (isDark) Color(0xFF15803D) else Color(0xFF86EFAC), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF16A34A))
                                )
                                Text(
                                    text = "Открыта",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D)
                                )
                            }
                        }
                    } else {
                        AnimatedVisibility(
                            visible = isHovered,
                            enter = fadeIn(tween(140)) + scaleIn(tween(140)),
                            exit = fadeOut(tween(90)) + scaleOut(tween(90))
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF222630).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.95f))
                                    .border(1.dp, if (isDark) Color(0xFF38BDF8).copy(alpha = 0.4f) else Color(0xFF93C5FD), CircleShape)
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Открыть",
                                    tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Нижний бейдж количества элементов
                if (board.elementCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                            .shadow(2.dp, RoundedCornerShape(6.dp))
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isDark) Color(0xFF181B20).copy(alpha = 0.90f) else Color.White.copy(alpha = 0.92f))
                            .border(1.dp, if (isDark) Color(0xFF2E333D) else Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = pluralObjectsCount(board.elementCount),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) Color(0xFFE4E4E7) else Color(0xFF334155)
                            )
                        }
                    }
                }
            }

            // Тонкий разделитель превью и панели метаданных
            HorizontalDivider(
                thickness = 1.dp,
                color = if (isDark) Color(0xFF232730) else Color(0xFFF1F5F9)
            )

            // 2. Нижняя панель (Заголовок, дата и действия)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = board.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Меню карточки (три точки)
                    Box {
                        IconButton(
                            onClick = { menuOpen = true },
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (menuOpen) (if (isDark) Color(0xFF262A34) else Color(0xFFF1F5F9)) else Color.Transparent)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Меню",
                                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                            offset = DpOffset(0.dp, 4.dp),
                            modifier = Modifier
                                .shadow(14.dp, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0xFF1E222A) else Color.White)
                                .border(BorderStroke(1.dp, if (isDark) Color(0xFF2E333D) else Color(0xFFE2E8F0)), RoundedCornerShape(12.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Открыть", fontSize = 13.sp, color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF1E293B), fontWeight = FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569), modifier = Modifier.size(16.dp))
                                },
                                onClick = {
                                    menuOpen = false
                                    onOpen()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Переименовать", fontSize = 13.sp, color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF1E293B), fontWeight = FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, null, tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569), modifier = Modifier.size(16.dp))
                                },
                                onClick = {
                                    menuOpen = false
                                    onRename()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Дублировать", fontSize = 13.sp, color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF1E293B), fontWeight = FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(Icons.Default.ContentCopy, null, tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569), modifier = Modifier.size(16.dp))
                                },
                                onClick = {
                                    menuOpen = false
                                    onDuplicate()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Удалить", fontSize = 13.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(Icons.Default.DeleteOutline, null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                },
                                onClick = {
                                    menuOpen = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = formattedDate,
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
                        )
                    }

                    // Кнопка продолжить / открыть
                    Button(
                        onClick = onOpen,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isActive) (if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)) else (if (isDark) Color(0xFF242830) else Color(0xFFF1F5F9)),
                            contentColor = if (isActive) (if (isDark) Color(0xFF0F1115) else Color.White) else (if (isDark) Color(0xFFE4E4E7) else Color(0xFF1E293B))
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (isActive) "Продолжить" else "Открыть",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = if (isActive) Icons.Default.PlayArrow else Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatRelativeBoardDate(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { time = date }
    val timeFormat = SimpleDateFormat("HH:mm", Locale("ru"))
    val timeStr = timeFormat.format(date)

    val isToday = now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    if (isToday) return "Сегодня, $timeStr"

    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val isYesterday = yesterday.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
            yesterday.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    if (isYesterday) return "Вчера, $timeStr"

    val sameYear = now.get(Calendar.YEAR) == then.get(Calendar.YEAR)
    return if (sameYear) {
        SimpleDateFormat("d MMM, HH:mm", Locale("ru")).format(date)
    } else {
        SimpleDateFormat("d MMM yyyy, HH:mm", Locale("ru")).format(date)
    }
}

private fun pluralObjectsCount(count: Int): String {
    val mod100 = count % 100
    val mod10 = count % 10
    return when {
        mod100 in 11..19 -> "$count объектов"
        mod10 == 1 -> "$count объект"
        mod10 in 2..4 -> "$count объекта"
        else -> "$count объектов"
    }
}

/**
 * Отображение при отсутствии досок или пустом результате поиска.
 */
@Composable
private fun EmptyBoardsState(
    isSearch: Boolean,
    onClearSearch: () -> Unit,
    onCreateNew: () -> Unit,
    isDark: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isSearch) Icons.Default.Search else Icons.Default.Palette,
                contentDescription = null,
                tint = if (isDark) Color(0xFF52525B) else Color(0xFFCBD5E1),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isSearch) "Ничего не найдено" else "У вас пока нет созданных досок",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isSearch) "Попробуйте изменить поисковый запрос" else "Выберите шаблон выше или создайте чистую доску для работы",
                fontSize = 13.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = if (isSearch) onClearSearch else onCreateNew,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text(if (isSearch) "Сбросить поиск" else "+ Создать первую доску")
            }
        }
    }
}

/**
 * Диалог создания новой доски.
 */
@Composable
private fun CreateBoardDialog(
    initialTemplate: BoardTemplate,
    onDismiss: () -> Unit,
    onCreate: (String, BoardTemplate) -> Unit
) {
    val isDark = LocalAppTheme.current.isDark
    var title by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf(initialTemplate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF181B20) else Color.White,
        titleContentColor = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A),
        textContentColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
        title = { Text("Создать новую доску", fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название доски") },
                    placeholder = { Text("Например: Дорожная карта Q4") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A),
                        unfocusedTextColor = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A),
                        focusedContainerColor = if (isDark) Color(0xFF242830) else Color.White,
                        unfocusedContainerColor = if (isDark) Color(0xFF242830) else Color.White,
                        cursorColor = Color(0xFF2563EB),
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = if (isDark) Color(0xFF3F3F46) else Color(0xFFCBD5E1),
                        focusedLabelColor = Color(0xFF38BDF8),
                        unfocusedLabelColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Выберите шаблон:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BoardTemplate.values().forEach { tmpl ->
                        val isSel = tmpl == selectedTemplate
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) Color(0xFF2563EB) else if (isDark) Color(0xFF242830) else Color(0xFFF1F5F9))
                                .border(1.dp, if (isDark) Color(0xFF3F3F46) else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { selectedTemplate = tmpl }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${tmpl.icon} ${tmpl.displayName.substringBefore(" ")}",
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) Color.White else if (isDark) Color(0xFFE4E4E7) else Color(0xFF1E293B)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTitle = title.trim().ifBlank { selectedTemplate.displayName }
                    onCreate(finalTitle, selectedTemplate)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text("Создать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569))
            }
        }
    )
}

/**
 * Диалог переименования доски.
 */
@Composable
private fun RenameBoardDialog(
    currentTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val isDark = LocalAppTheme.current.isDark
    var newTitle by remember { mutableStateOf(currentTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF181B20) else Color.White,
        titleContentColor = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A),
        textContentColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
        title = { Text("Переименовать доску", fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)) },
        text = {
            OutlinedTextField(
                value = newTitle,
                onValueChange = { newTitle = it },
                label = { Text("Новое название") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A),
                    unfocusedTextColor = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A),
                    focusedContainerColor = if (isDark) Color(0xFF242830) else Color.White,
                    unfocusedContainerColor = if (isDark) Color(0xFF242830) else Color.White,
                    cursorColor = Color(0xFF2563EB),
                    focusedBorderColor = Color(0xFF2563EB),
                    unfocusedBorderColor = if (isDark) Color(0xFF3F3F46) else Color(0xFFCBD5E1),
                    focusedLabelColor = Color(0xFF38BDF8),
                    unfocusedLabelColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newTitle.isNotBlank()) {
                        onConfirm(newTitle.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569))
            }
        }
    )
}

/**
 * Диалог подтверждения удаления доски.
 */
@Composable
private fun DeleteConfirmDialog(
    boardTitle: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val isDark = LocalAppTheme.current.isDark

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF181B20) else Color.White,
        titleContentColor = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A),
        textContentColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
        title = { Text("Удалить доску?", fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)) },
        text = {
            Text("Вы уверены, что хотите безвозвратно удалить доску «$boardTitle»? Это действие нельзя отменить.", color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155))
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
            ) {
                Text("Удалить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569))
            }
        }
    )
}
