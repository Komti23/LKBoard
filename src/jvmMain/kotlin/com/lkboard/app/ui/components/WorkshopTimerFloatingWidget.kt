package com.lkboard.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lkboard.app.timer.WorkshopTimerState

/**
 * Плавающий стеклянный таймер воркшопов, уроков и спринтов (Apple Liquid Glass).
 */
@Composable
fun WorkshopTimerFloatingWidget(
    state: WorkshopTimerState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onAddMinutes: (Int) -> Unit,
    onSetPreset: (Int) -> Unit,
    onToggleMode: () -> Unit,
    onToggleMinimize: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state.isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        if (state.isMinimized) {
            // Минималистичная компактная стеклянная капсула
            Box(
                modifier = Modifier
                    .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.12f))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFFBFBFD).copy(alpha = 0.92f))
                    .border(1.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(20.dp))
                    .clickable { onToggleMinimize() }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (state.isFinished) Icons.Default.NotificationsActive else Icons.Default.Timer,
                        contentDescription = null,
                        tint = if (state.isFinished) Color(0xFFEF4444) else Color(0xFF007AFF),
                        modifier = Modifier.size(17.dp)
                    )

                    Text(
                        text = state.formattedTime,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (state.isFinished) Color(0xFFEF4444) else Color(0xFF0F172A)
                    )

                    IconButton(
                        onClick = { if (state.isRunning) onPause() else onStart() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (state.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.isRunning) "Пауза" else "Старт",
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.UnfoldMore,
                        contentDescription = "Развернуть",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        } else {
            // Развернутая стеклянная панель управления
            Box(
                modifier = Modifier
                    .width(300.dp)
                    .shadow(18.dp, RoundedCornerShape(22.dp), spotColor = Color.Black.copy(alpha = 0.14f))
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFFFBFBFD).copy(alpha = 0.94f))
                    .border(1.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(22.dp))
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Шапка карточки таймера
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.clickable { onToggleMode() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = Color(0xFF007AFF),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (state.isStopwatch) "Секундомер" else "Таймер воркшопа",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E293B)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = onToggleMinimize,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Text("—", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                            }

                            IconButton(
                                onClick = onClose,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Закрыть",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Табло с крупными цифрами
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (state.isFinished) Color(0xFFFEE2E2) else Color(0xFFF1F5F9))
                            .border(
                                1.dp,
                                if (state.isFinished) Color(0xFFEF4444) else Color(0xFFE2E8F0),
                                RoundedCornerShape(14.dp)
                            )
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.formattedTime,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (state.isFinished) Color(0xFFDC2626) else Color(0xFF0F172A)
                            )

                            if (state.isFinished) {
                                Text(
                                    text = "🔔 Время истекло!",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDC2626)
                                )
                            }
                        }
                    }

                    // Линейный прогресс-бар оставшегося времени (если не секундомер)
                    if (!state.isStopwatch) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFFE2E8F0))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(state.progress)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (state.progress < 0.15f) Color(0xFFEF4444) else Color(0xFF007AFF))
                            )
                        }
                    }

                    // Быстрые пресеты времени: 5м, 15м, 25м (Pomodoro), 45м
                    if (!state.isStopwatch) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val presets = listOf(5 to "5м", 15 to "15м", 25 to "25м 🍅", 45 to "45м")
                            presets.forEach { (mins, label) ->
                                val isCur = state.totalSeconds == mins * 60
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isCur) Color(0xFF007AFF).copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.05f))
                                        .clickable { onSetPreset(mins) }
                                        .padding(vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isCur) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isCur) Color(0xFF007AFF) else Color(0xFF334155)
                                    )
                                }
                            }
                        }
                    }

                    // Основные кнопки управления: Старт / Пауза, +1м, Сброс
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Сброс
                        IconButton(
                            onClick = onReset,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.05f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Сбросить",
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Главная кнопка: Старт / Пауза
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (state.isRunning) Color(0xFFEF4444) else Color(0xFF007AFF))
                                .clickable { if (state.isRunning) onPause() else onStart() }
                                .padding(horizontal = 24.dp, vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (state.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (state.isRunning) "Пауза" else "Старт",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Кнопка +1 минута
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF007AFF).copy(alpha = 0.10f))
                                .clickable { onAddMinutes(1) }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "+1м",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF007AFF)
                            )
                        }
                    }
                }
            }
        }
    }
}
