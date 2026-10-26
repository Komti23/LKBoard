package com.lkboard.app.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var boardToRename by remember { mutableStateOf<BoardItemInfo?>(null) }
    var boardToDelete by remember { mutableStateOf<BoardItemInfo?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newBoardTemplate by remember { mutableStateOf(BoardTemplate.BLANK) }

    val filteredBoards = remember(boards, searchQuery) {
        if (searchQuery.isBlank()) boards
        else boards.filter { it.title.contains(searchQuery.trim(), ignoreCase = true) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FA))
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
                onOpenSettings = onOpenSettings
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
                }
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
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFE2E8F0))
                            .padding(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${filteredBoards.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
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
                    }
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 270.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(bottom = 32.dp),
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
    onOpenSettings: () -> Unit = {}
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
            .background(Color.White.copy(alpha = 0.90f))
            .border(BorderStroke(1.dp, Color.White), RoundedCornerShape(18.dp))
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
                AppLogo.LogoIcon(size = 42.dp)

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "LKBoard",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A),
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFEFF6FF))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "v${AppVersion.VERSION_NAME}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB)
                            )
                        }
                    }
                    Text(
                        text = "Бесконечные доски, дорожные карты & презентации",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
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
                        containerColor = if (hasUpdate) Color(0xFFFEF3C7) else Color(0xFFF1F5F9),
                        contentColor = if (hasUpdate) Color(0xFFB45309) else Color(0xFF475569)
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
                        Text("🚀", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Доступно v${updateState.updateInfo!!.version}!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text("🔄", fontSize = 12.sp)
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
                        .background(Color(0xFFF1F5F9))
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
                            onValueChange = onSearchQueryChange,
                            placeholder = {
                                Text(
                                    "Поиск по доскам...",
                                    fontSize = 13.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
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
                        containerColor = Color(0xFFF1F5F9),
                        contentColor = Color(0xFF334155)
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Импорт",
                        modifier = Modifier.size(17.dp),
                        tint = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Импорт",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF334155)
                    )
                }

                // Кнопка настроек
                Button(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF1F5F9),
                        contentColor = Color(0xFF334155)
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Настройки",
                        modifier = Modifier.size(17.dp),
                        tint = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Настройки",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF334155)
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
    onSelectTemplate: (BoardTemplate) -> Unit
) {
    Column {
        Text(
            text = "Быстрый старт с шаблонами",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF64748B)
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BoardTemplate.values().forEach { template ->
                val isRoadmap = template == BoardTemplate.ROADMAP
                val cardBorder = if (isRoadmap) {
                    BorderStroke(1.5.dp, Color(0xFF2563EB).copy(alpha = 0.6f))
                } else {
                    BorderStroke(1.dp, Color.White)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .shadow(
                            elevation = if (isRoadmap) 10.dp else 4.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = Color.Black.copy(alpha = 0.02f),
                            spotColor = if (isRoadmap) Color(0x332563EB) else Color.Black.copy(alpha = 0.05f)
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isRoadmap) Color(0xFFF8FAFF) else Color.White
                        )
                        .border(cardBorder, RoundedCornerShape(16.dp))
                        .clickable { onSelectTemplate(template) }
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = template.icon,
                                fontSize = 24.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = template.displayName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = template.description,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
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
 * Карточка сохраненной доски.
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
    var menuOpen by remember { mutableStateOf(false) }

    val formattedDate = remember(board.updatedAt) {
        val sdf = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault())
        sdf.format(Date(board.updatedAt))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isActive) 14.dp else 8.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color.Black.copy(alpha = 0.03f),
                spotColor = if (isActive) Color(0x332563EB) else Color.Black.copy(alpha = 0.07f)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(
                if (isActive) BorderStroke(1.5.dp, Color(0xFF2563EB))
                else BorderStroke(1.dp, Color(0xFFE2E8F0)),
                RoundedCornerShape(18.dp)
            )
            .clickable { onOpen() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Верхнее превью (Preview Banner)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(Color(board.backgroundColorHex))
                    .padding(12.dp)
            ) {
                // Бейдж шаблона или элементов
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    val templateIcon = when (board.templateType) {
                        BoardTemplate.ROADMAP.name -> "🗺️ Дорожная карта"
                        BoardTemplate.KANBAN.name -> "📊 Канбан"
                        BoardTemplate.BRAINSTORM.name -> "💡 Mindmap"
                        BoardTemplate.MATH_STUDIO.name -> "📈 Графики"
                        else -> "📄 Доска"
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.06f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = templateIcon,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF334155)
                        )
                    }

                    if (isActive) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2563EB))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Открыта сейчас",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Индикатор количества элементов
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.85f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (board.elementCount > 0) "${board.elementCount} объектов" else "Пустая",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF475569)
                    )
                }
            }

            // Нижняя часть карточки (Заголовок, дата, действия)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
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
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )

                    // Меню карточки (три точки)
                    Box {
                        IconButton(
                            onClick = { menuOpen = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Меню",
                                tint = Color(0xFF64748B),
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
                                .background(Color.White)
                                .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(12.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Открыть", fontSize = 13.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = Color(0xFF475569), modifier = Modifier.size(16.dp))
                                },
                                onClick = {
                                    menuOpen = false
                                    onOpen()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Переименовать", fontSize = 13.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, null, tint = Color(0xFF475569), modifier = Modifier.size(16.dp))
                                },
                                onClick = {
                                    menuOpen = false
                                    onRename()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Дублировать", fontSize = 13.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(Icons.Default.ContentCopy, null, tint = Color(0xFF475569), modifier = Modifier.size(16.dp))
                                },
                                onClick = {
                                    menuOpen = false
                                    onDuplicate()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Удалить", fontSize = 13.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(Icons.Default.DeleteOutline, null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                },
                                onClick = {
                                    menuOpen = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Изменено: $formattedDate",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )

                    // Кнопка открыть
                    Button(
                        onClick = onOpen,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isActive) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                            contentColor = if (isActive) Color.White else Color(0xFF1E293B)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = if (isActive) "Продолжить" else "Открыть",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Отображение при отсутствии досок или пустом результате поиска.
 */
@Composable
private fun EmptyBoardsState(
    isSearch: Boolean,
    onClearSearch: () -> Unit,
    onCreateNew: () -> Unit
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
            Text(
                text = if (isSearch) "🔍" else "🎨",
                fontSize = 42.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isSearch) "Ничего не найдено" else "У вас пока нет созданных досок",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isSearch) "Попробуйте изменить поисковый запрос" else "Выберите шаблон выше или создайте чистую доску для работы",
                fontSize = 13.sp,
                color = Color(0xFF64748B)
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
    var title by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf(initialTemplate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        titleContentColor = Color(0xFF0F172A),
        textContentColor = Color(0xFF334155),
        title = { Text("Создать новую доску", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название доски") },
                    placeholder = { Text("Например: Дорожная карта Q4") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A),
                        cursorColor = Color(0xFF2563EB),
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedLabelColor = Color(0xFF2563EB),
                        unfocusedLabelColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Выберите шаблон:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))

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
                                .background(if (isSel) Color(0xFF2563EB) else Color(0xFFF1F5F9))
                                .clickable { selectedTemplate = tmpl }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${tmpl.icon} ${tmpl.displayName.substringBefore(" ")}",
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) Color.White else Color(0xFF1E293B)
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
                Text("Отмена")
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
    var newTitle by remember { mutableStateOf(currentTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        titleContentColor = Color(0xFF0F172A),
        textContentColor = Color(0xFF334155),
        title = { Text("Переименовать доску", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
        text = {
            OutlinedTextField(
                value = newTitle,
                onValueChange = { newTitle = it },
                label = { Text("Новое название") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A),
                    cursorColor = Color(0xFF2563EB),
                    focusedBorderColor = Color(0xFF2563EB),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedLabelColor = Color(0xFF2563EB),
                    unfocusedLabelColor = Color(0xFF64748B)
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
                Text("Отмена")
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
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        titleContentColor = Color(0xFF0F172A),
        textContentColor = Color(0xFF334155),
        title = { Text("Удалить доску?", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
        text = {
            Text("Вы уверены, что хотите безвозвратно удалить доску «$boardTitle»? Это действие нельзя отменить.", color = Color(0xFF334155))
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
                Text("Отмена")
            }
        }
    )
}
