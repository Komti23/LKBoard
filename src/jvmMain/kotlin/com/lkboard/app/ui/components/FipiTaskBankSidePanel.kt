package com.lkboard.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lkboard.app.data.FipiTaskBank
import com.lkboard.app.data.FipiTaskItem
import com.lkboard.app.ui.animation.bouncyClickable
import com.lkboard.app.ui.theme.LocalAppTheme
import kotlinx.coroutines.launch

/**
 * Правая боковая панель «Банк заданий ФИПИ (ОГЭ / ЕГЭ)»:
 * - Выбор экзамена (ЕГЭ Профиль / База, ОГЭ) и предмета (Математика, Физика, Информатика, Химия, Обществознание).
 * - Быстрый отбор по номеру задания (№1..№19), темам и строке поиска.
 * - Карточка задания с условием, скрываемым ответом, критериями оценивания и вставкой на доску.
 * - Встроенная оффлайн база + динамическое обновление.
 */
@Composable
fun FipiTaskBankSidePanel(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onInsertTask: (FipiTaskItem) -> Unit
) {
    val isDark = LocalAppTheme.current.isDark
    val scope = rememberCoroutineScope()

    var tasks by remember { mutableStateOf(FipiTaskBank.BUILTIN_TASKS) }
    var selectedExam by remember { mutableStateOf("ЕГЭ (Профиль)") }
    var selectedSubject by remember { mutableStateOf("Математика") }
    var selectedTaskNumber by remember { mutableStateOf("Все") }
    var searchQuery by remember { mutableStateOf("") }
    var previewAnswerIds by remember { mutableStateOf(setOf<String>()) }

    // Асинхронная проверка обновлений заданий онлайн
    LaunchedEffect(Unit) {
        scope.launch {
            val freshTasks = FipiTaskBank.fetchTasksOnline()
            if (freshTasks.isNotEmpty()) {
                tasks = freshTasks
            }
        }
    }

    // Доступные номера заданий для выбранного предмета и экзамена
    val availableTaskNumbers = remember(tasks, selectedExam, selectedSubject) {
        val filtered = tasks.filter {
            (selectedExam == "Все" || it.examType == selectedExam) &&
            (selectedSubject == "Все" || it.subject == selectedSubject)
        }
        val numbers = filtered.map { it.taskNumber }.distinct()
        listOf("Все") + numbers
    }

    // Итоговый отфильтрованный список заданий
    val filteredTasks = remember(tasks, selectedExam, selectedSubject, selectedTaskNumber, searchQuery) {
        tasks.filter { task ->
            val matchesExam = selectedExam == "Все" || task.examType.equals(selectedExam, ignoreCase = true)
            val matchesSubject = selectedSubject == "Все" || task.subject.equals(selectedSubject, ignoreCase = true)
            val matchesNumber = selectedTaskNumber == "Все" || task.taskNumber.equals(selectedTaskNumber, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                    task.condition.contains(searchQuery.trim(), ignoreCase = true) ||
                    task.topic.contains(searchQuery.trim(), ignoreCase = true) ||
                    task.taskNumber.contains(searchQuery.trim(), ignoreCase = true) ||
                    task.answer.contains(searchQuery.trim(), ignoreCase = true)

            matchesExam && matchesSubject && matchesNumber && matchesSearch
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Затемняющий оверлей с плавным закрытием
        AnimatedVisibility(
            visible = isOpen,
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(180))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.40f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }

        // Выдвигающаяся правая боковая панель
        AnimatedVisibility(
            visible = isOpen,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
            ) + fadeIn(),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
            ) + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(540.dp)
                    .shadow(
                        elevation = 28.dp,
                        shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp),
                        ambientColor = Color.Black.copy(alpha = 0.12f),
                        spotColor = Color.Black.copy(alpha = 0.28f)
                    )
                    .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
                    .background(if (isDark) Color(0xFF14171D) else Color.White)
                    .border(
                        1.dp,
                        if (isDark) Color(0xFF2E333D) else Color(0xFFE2E8F0),
                        RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                Column(modifier = Modifier.fillMaxSize()) {

                    // 1. Шапка панели (Header)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isDark) Color(0xFF181B22) else Color(0xFFF8FAFC))
                            .border(1.dp, if (isDark) Color(0xFF232730) else Color(0xFFE2E8F0))
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (isDark) Color(0xFF1E2638) else Color(0xFFDBEAFE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB),
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Банк заданий ФИПИ",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Официальные прототипы ОГЭ и ЕГЭ 2026",
                                    fontSize = 11.sp,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Кнопка случайной задачи
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDark) Color(0xFF242832) else Color(0xFFF1F5F9))
                                    .border(1.dp, if (isDark) Color(0xFF363C4A) else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                    .bouncyClickable(hoverScale = 1.05f, pressedScale = 0.92f) {
                                        if (filteredTasks.isNotEmpty()) {
                                            val randomTask = filteredTasks.random()
                                            onInsertTask(randomTask)
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Casino,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "🎲 Случайное",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
                                    )
                                }
                            }

                            // Кнопка закрыть
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDark) Color(0xFF242832) else Color(0xFFE2E8F0))
                                    .bouncyClickable(hoverScale = 1.08f, pressedScale = 0.90f) { onDismiss() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Закрыть",
                                    tint = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // 2. Блок фильтрации (Exam, Subject, Task Number, Search)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isDark) Color(0xFF171A21) else Color(0xFFFAFAFA))
                            .border(1.dp, if (isDark) Color(0xFF232730) else Color(0xFFF1F5F9))
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Сегмент экзаменов (ЕГЭ Профиль / ЕГЭ База / ОГЭ / Все)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (isDark) Color(0xFF101216) else Color(0xFFE2E8F0))
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            listOf("ЕГЭ (Профиль)", "ЕГЭ (База)", "ОГЭ", "Все").forEach { exam ->
                                val isSelected = selectedExam == exam
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(
                                            if (isSelected) {
                                                if (isDark) Color(0xFF2563EB) else Color.White
                                            } else Color.Transparent
                                        )
                                        .bouncyClickable(hoverScale = 1.04f, pressedScale = 0.95f) {
                                            selectedExam = exam
                                            selectedTaskNumber = "Все"
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = exam,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) {
                                            if (isDark) Color.White else Color(0xFF0F172A)
                                        } else {
                                            if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                        }
                                    )
                                }
                            }
                        }

                        // Чипы выбора предметов
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            (listOf("Все") + FipiTaskBank.AVAILABLE_SUBJECTS).forEach { subj ->
                                val isSelected = selectedSubject == subj
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) {
                                                if (isDark) Color(0xFF1E2638) else Color(0xFFDBEAFE)
                                            } else {
                                                if (isDark) Color(0xFF20242D) else Color(0xFFF1F5F9)
                                            }
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) Color(0xFF2563EB) else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .bouncyClickable(hoverScale = 1.05f, pressedScale = 0.94f) {
                                            selectedSubject = subj
                                            selectedTaskNumber = "Все"
                                        }
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = subj,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) {
                                            if (isDark) Color(0xFF38BDF8) else Color(0xFF1D4ED8)
                                        } else {
                                            if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                                        }
                                    )
                                }
                            }
                        }

                        // Горизонтальный скролл номеров заданий
                        if (availableTaskNumbers.size > 1) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Задание:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    modifier = Modifier.padding(end = 4.dp)
                                )

                                availableTaskNumbers.forEach { num ->
                                    val isSelected = selectedTaskNumber == num
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isSelected) Color(0xFF2563EB)
                                                else (if (isDark) Color(0xFF242832) else Color(0xFFE2E8F0))
                                            )
                                            .bouncyClickable(hoverScale = 1.08f, pressedScale = 0.92f) {
                                                selectedTaskNumber = num
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = num,
                                            fontSize = 10.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155))
                                        )
                                    }
                                }
                            }
                        }

                        // Поле мгновенного поиска по тексту условия
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (isDark) Color(0xFF121419) else Color.White)
                                .border(1.dp, if (isDark) Color(0xFF2A2E39) else Color(0xFFCBD5E1), RoundedCornerShape(9.dp))
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    modifier = Modifier.size(15.dp)
                                )

                                Box(modifier = Modifier.weight(1f)) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Поиск по тексту задачи, номеру или теме...",
                                            fontSize = 12.sp,
                                            color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                                        )
                                    }
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        textStyle = TextStyle(
                                            fontSize = 12.sp,
                                            color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
                                        ),
                                        cursorBrush = SolidColor(Color(0xFF2563EB)),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                if (searchQuery.isNotEmpty()) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Очистить",
                                        tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { searchQuery = "" }
                                    )
                                }
                            }
                        }
                    }

                    // 3. Список найденных задач ФИПИ (Результаты)
                    if (filteredTasks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF475569) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(42.dp)
                                )
                                Text(
                                    text = "Задания не найдены",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                                )
                                Text(
                                    text = "Попробуйте выбрать другой номер или очистить строку поиска",
                                    fontSize = 12.sp,
                                    color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(filteredTasks, key = { it.id }) { task ->
                                val isAnswerOpen = task.id in previewAnswerIds

                                FipiTaskCardView(
                                    task = task,
                                    isAnswerPreviewOpen = isAnswerOpen,
                                    onToggleAnswer = {
                                        previewAnswerIds = if (isAnswerOpen) {
                                            previewAnswerIds - task.id
                                        } else {
                                            previewAnswerIds + task.id
                                        }
                                    },
                                    onInsertToBoard = { onInsertTask(task) },
                                    isDark = isDark
                                )
                            }
                        }
                    }

                    // 4. Нижний статус-бар
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isDark) Color(0xFF181B22) else Color(0xFFF8FAFC))
                            .border(1.dp, if (isDark) Color(0xFF232730) else Color(0xFFE2E8F0))
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Найдено: ${filteredTasks.size} ${getTasksNoun(filteredTasks.size)}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )

                        Text(
                            text = "✓ Оффлайн-база активна",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF16A34A)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Карточка задания внутри бокового списка с кнопкой вставки и спойлером решения.
 */
@Composable
private fun FipiTaskCardView(
    task: FipiTaskItem,
    isAnswerPreviewOpen: Boolean,
    onToggleAnswer: () -> Unit,
    onInsertToBoard: () -> Unit,
    isDark: Boolean
) {
    val isOge = task.examType.contains("ОГЭ")
    val examColor = if (isOge) Color(0xFF7C3AED) else Color(0xFF2563EB)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = Color.Black.copy(alpha = 0.03f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDark) Color(0xFF1C2028) else Color.White)
            .border(
                1.dp,
                if (isDark) Color(0xFF2E3442) else Color(0xFFE2E8F0),
                RoundedCornerShape(14.dp)
            )
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // Верхняя строка карточки: Бейджи и Кнопка вставки
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Бейдж экзамена
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(examColor.copy(alpha = 0.12f))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${task.examType} · ${task.subject}",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = examColor
                        )
                    }

                    // Бейдж номера задания
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFD97706).copy(alpha = 0.12f))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = task.taskNumber,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFD97706)
                        )
                    }

                    // Баллы
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isDark) Color(0xFF242832) else Color(0xFFF1F5F9))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${task.maxScore} б.",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                }

                // Кнопка быстрой вставки на холст
                Button(
                    onClick = onInsertToBoard,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .height(28.dp)
                        .bouncyClickable(hoverScale = 1.06f, pressedScale = 0.90f) { onInsertToBoard() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                        Text("На доску", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Тема задания
            if (task.topic.isNotBlank()) {
                Text(
                    text = task.topic,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
                )
            }

            // Текст условия задачи
            Text(
                text = task.condition,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
            )

            // Спойлер ответа и критериев проверки
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDark) Color(0xFF14171E) else Color(0xFFF8FAFC))
                    .border(1.dp, if (isDark) Color(0xFF282D39) else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onToggleAnswer),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isAnswerPreviewOpen) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = if (isAnswerPreviewOpen) Color(0xFF16A34A) else Color(0xFF64748B),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isAnswerPreviewOpen) "Скрыть ответ и решение" else "Показать ответ и решение",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isAnswerPreviewOpen) Color(0xFF16A34A) else Color(0xFF64748B)
                        )
                    }

                    Icon(
                        imageVector = if (isAnswerPreviewOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (isAnswerPreviewOpen) {
                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isDark) Color(0xFF0D1E16) else Color(0xFFF0FDF4))
                            .border(1.dp, Color(0xFF86EFAC), RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Ответ:",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D)
                        )
                        Text(
                            text = task.answer,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF86EFAC) else Color(0xFF166534)
                        )
                        if (task.solutionCriteria.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Критерии ФИПИ:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                            Text(
                                text = task.solutionCriteria,
                                fontSize = 10.5.sp,
                                color = if (isDark) Color(0xFFA7F3D0) else Color(0xFF14532D),
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getTasksNoun(count: Int): String {
    val mod10 = count % 10
    val mod100 = count % 100
    return when {
        mod100 in 11..19 -> "заданий"
        mod10 == 1 -> "задание"
        mod10 in 2..4 -> "задания"
        else -> "заданий"
    }
}
