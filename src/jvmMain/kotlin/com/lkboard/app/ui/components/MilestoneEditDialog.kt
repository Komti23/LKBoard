package com.lkboard.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lkboard.app.model.BoardElement
import com.lkboard.app.model.MilestoneStatus
import com.lkboard.app.ui.theme.LocalAppTheme

/**
 * Боковая панель редактирования параметров вехи дорожной карты (Side Drawer):
 * - Плавно выезжает справа, не перекрывая доску.
 * - Управление названием, описанием, статусом, прогрессом и сроками выполнения.
 */
@Composable
fun MilestoneSidePanel(
    isOpen: Boolean,
    element: BoardElement.MilestoneCard?,
    onSave: (BoardElement.MilestoneCard) -> Unit,
    onDismiss: () -> Unit
) {
    var lastElement by remember { mutableStateOf<BoardElement.MilestoneCard?>(null) }
    if (element != null) {
        lastElement = element
    }
    val activeElement = element ?: lastElement
    if (activeElement == null) return

    val isDark = LocalAppTheme.current.isDark
    var title by remember(activeElement.id, isOpen) { mutableStateOf(activeElement.title) }
    var description by remember(activeElement.id, isOpen) { mutableStateOf(activeElement.description) }
    var status by remember(activeElement.id, isOpen) { mutableStateOf(activeElement.status) }
    var progress by remember(activeElement.id, isOpen) { mutableStateOf(activeElement.progress) }
    var dueDate by remember(activeElement.id, isOpen) { mutableStateOf(activeElement.dueDate) }
    var tagsString by remember(activeElement.id, isOpen) { mutableStateOf(activeElement.tags.joinToString(", ")) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A),
        unfocusedTextColor = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A),
        focusedContainerColor = if (isDark) Color(0xFF1E2430) else Color.White,
        unfocusedContainerColor = if (isDark) Color(0xFF1E2430) else Color.White,
        cursorColor = Color(0xFF2563EB),
        focusedBorderColor = Color(0xFF2563EB),
        unfocusedBorderColor = if (isDark) Color(0xFF3F4554) else Color(0xFFCBD5E1),
        focusedLabelColor = Color(0xFF3B82F6),
        unfocusedLabelColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Затемняющий оверлей
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

        // Правая боковая панель с плавной анимацией выезда
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
                    .width(480.dp)
                    .shadow(
                        elevation = 24.dp,
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
                    // Шапка
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isDark) Color(0xFF181B22) else Color(0xFFF8FAFC))
                            .border(1.dp, if (isDark) Color(0xFF232730) else Color(0xFFE2E8F0))
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "🚩 Веха дорожной карты",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
                            )
                            Text(
                                text = "Параметры этапа проекта",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDark) Color(0xFF282D37) else Color(0xFFE2E8F0))
                                .clickable(onClick = onDismiss),
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

                    // Скроллируемое тело
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Название вехи") },
                            singleLine = true,
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Описание и результаты") },
                            maxLines = 3,
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "СТАТУС ЗАДАЧИ:",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MilestoneStatus.entries.forEach { st ->
                                val isSelected = st == status
                                val stColor = Color(st.colorHex)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) stColor.copy(alpha = 0.20f) else (if (isDark) Color(0xFF242830) else Color(0xFFF1F5F9)))
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) stColor else (if (isDark) Color(0xFF3F4554) else Color(0xFFE2E8F0)),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { status = st }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${st.icon} ${st.label}",
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) stColor else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569))
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "ПРОГРЕСС ВЫПОЛНЕНИЯ:",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(status.colorHex)
                            )
                        }

                        Slider(
                            value = progress,
                            onValueChange = { progress = it },
                            valueRange = 0f..1f,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = dueDate,
                                onValueChange = { dueDate = it },
                                label = { Text("Срок (дедлайн)") },
                                placeholder = { Text("2026-11-01") },
                                singleLine = true,
                                colors = fieldColors,
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = tagsString,
                                onValueChange = { tagsString = it },
                                label = { Text("Теги (через запятую)") },
                                singleLine = true,
                                colors = fieldColors,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Нижняя панель действий
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isDark) Color(0xFF181B22) else Color(0xFFF8FAFC))
                            .border(1.dp, if (isDark) Color(0xFF232730) else Color(0xFFE2E8F0))
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            ),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF3F4554) else Color(0xFFCBD5E1))
                        ) {
                            Text("Отмена", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val tagsList = tagsString.split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }
                                val updated = activeElement.copy(
                                    title = title.ifBlank { "Веха" },
                                    description = description,
                                    status = status,
                                    progress = progress,
                                    dueDate = dueDate.trim(),
                                    tags = if (tagsList.isNotEmpty()) tagsList else listOf("Release")
                                )
                                onSave(updated)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB),
                                contentColor = Color.White
                            )
                        ) {
                            Text("✓ Сохранить веху", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Обратная совместимость для вызова как Dialog.
 */
@Composable
fun MilestoneEditDialog(
    element: BoardElement.MilestoneCard,
    onSave: (BoardElement.MilestoneCard) -> Unit,
    onDismiss: () -> Unit
) {
    MilestoneSidePanel(
        isOpen = true,
        element = element,
        onSave = onSave,
        onDismiss = onDismiss
    )
}
