package com.lkboard.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lkboard.app.model.GridType
import com.lkboard.app.model.PenSettings
import com.lkboard.app.ui.theme.LocalAppTheme
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Всплывающее меню настроек стилуса в эталонном стиле iOS Glassmorphism (Apple Liquid Glass):
 * - Строго ОДИН фоновый слой на самом внешнем контейнере:
 *   Box -> width(280.dp), shadow(16.dp, 20.dp), clip(20.dp), background(Color(0xFFF9F9FB).copy(alpha = 0.82f)), border(1.dp, 20.dp), padding(16.dp).
 * - Внутри ТОЛЬКО прозрачный Column (Color.Transparent), без промежуточных Surface, Card или фоновых Box.
 * - Outset ring для активного цвета.
 * - Однострочный слайдер: миниатюра -> тонкий слайдер -> значение.
 * - Компактный Switch для прямой линии (Shift).
 * - Сегментный переключатель сетки без непрозрачных подложек.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StylusPropertiesPopup(
    settings: PenSettings,
    onSettingsChange: (PenSettings) -> Unit,
    gridType: GridType = GridType.DOTS,
    onGridTypeChange: ((GridType) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colorTitle = when (settings.colorHex) {
        0xFF1E1E1EL -> "Черный"
        0xFF2563EBL -> "Синий"
        0xFFDC2626L -> "Красный"
        0xFF16A34AL -> "Зеленый"
        0xFFFACC15L -> "Желтый"
        else -> "Перо"
    }
    val titleText = if (settings.isHighlighter) "Маркер" else colorTitle
    val formattedWidth = String.format(Locale.US, "%.1f px", settings.strokeWidth)

    val isDark = LocalAppTheme.current.isDark

    // ЕДИНСТВЕННЫЙ фоновый контейнер окна:
    Box(
        modifier = modifier
            .width(280.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(22.dp))
            .background(if (isDark) Color(0xFF181B20).copy(alpha = 0.96f) else Color(0xFFF7F7F9))
            .border(1.dp, if (isDark) Color(0xFF2E333D) else Color.White.copy(alpha = 0.85f), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        // ВНУТРИ ТОЛЬКО ПРОЗРАЧНЫЙ COLUMN:
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Заголовок: Название цвета / режима слева + полупрозрачная пилюля справа
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.background(Color.Transparent),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(settings.colorHex))
                    )
                    Text(
                        text = titleText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF1C1C1E)
                    )
                }

                // Аккуратная плашка-пилюля с размером
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF007AFF).copy(alpha = 0.10f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formattedWidth,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFF38BDF8) else Color(0xFF007AFF)
                    )
                }
            }

            // 2. Расширенная палитра цветов (2 ряда по 7 оттенков)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val colorRows = PenSettings.PRESET_COLORS.chunked(7)
                colorRows.forEach { rowColors ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Transparent),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowColors.forEach { colorHex ->
                            val isHighlighterColor = colorHex == PenSettings.COLOR_HIGHLIGHTER
                            val isSelected = settings.colorHex == colorHex && settings.isHighlighter == isHighlighterColor
                            val color = Color(colorHex)
                            val scale by animateFloatAsState(if (isSelected) 1.06f else 1.0f)

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color.Transparent)
                                    .scale(scale),
                                contentAlignment = Alignment.Center
                            ) {
                                // Внешнее кольцо обводки при выборе
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .border(
                                                width = 2.dp,
                                                color = if (colorHex == 0xFF1E1E1EL) Color(0xFF1E1E1E) else Color(0xFF007AFF),
                                                shape = CircleShape
                                            )
                                    )
                                }

                                // Кружок цвета
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .then(
                                            if (!isSelected) {
                                                Modifier.border(0.5.dp, Color.Black.copy(alpha = 0.12f), CircleShape)
                                            } else Modifier
                                        )
                                        .clickable {
                                            onSettingsChange(
                                                settings.copy(
                                                    colorHex = colorHex,
                                                    isHighlighter = isHighlighterColor,
                                                    strokeWidth = if (isHighlighterColor) 20f else settings.strokeWidth.coerceAtMost(16f)
                                                )
                                            )
                                        }
                                )
                            }
                        }
                    }
                }
            }

            // 3. Слайдер толщины: в одну строку (миниатюра слева -> тонкий слайдер -> число справа)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Миниатюра штриха слева
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    val dotSize = (settings.strokeWidth * 0.75f).coerceIn(2.5f, 20f).dp
                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .clip(CircleShape)
                            .background(
                                if (settings.isHighlighter) Color(settings.colorHex).copy(alpha = 0.65f)
                                else Color(settings.colorHex)
                            )
                    )
                }

                // Стильный тонкий Slider
                Slider(
                    value = settings.strokeWidth,
                    onValueChange = { newWidth ->
                        onSettingsChange(settings.copy(strokeWidth = (newWidth * 10).roundToInt() / 10f))
                    },
                    valueRange = 1.0f..32.0f,
                    thumb = {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .shadow(2.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.15f))
                                .background(Color(0xFF007AFF), CircleShape)
                                .border(1.5.dp, Color.White, CircleShape)
                        )
                    },
                    track = { sliderState ->
                        SliderDefaults.Track(
                            sliderState = sliderState,
                            modifier = Modifier.height(4.dp),
                            colors = SliderDefaults.colors(
                                activeTrackColor = Color(0xFF007AFF),
                                inactiveTrackColor = Color.Black.copy(alpha = 0.10f)
                            )
                        )
                    },
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color(0xFF007AFF),
                        inactiveTrackColor = Color.Black.copy(alpha = 0.10f)
                    ),
                    modifier = Modifier.weight(1f)
                )

                // Числовое значение справа
                Text(
                    text = String.format(Locale.US, "%.1f", settings.strokeWidth),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color(0xFFE4E4E7) else Color(0xFF3C3C43),
                    modifier = Modifier.width(28.dp),
                    textAlign = TextAlign.End
                )
            }

            // 4. Переключатель «Прямая линия»
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.background(Color.Transparent),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Straighten,
                        contentDescription = "Прямая линия",
                        tint = if (settings.isStraightLine) (if (isDark) Color(0xFF38BDF8) else Color(0xFF007AFF)) else (if (isDark) Color(0xFF71717A) else Color(0xFF8E8E93)),
                        modifier = Modifier.size(17.dp)
                    )
                    Column(modifier = Modifier.background(Color.Transparent)) {
                        Text(
                            text = "Прямая линия",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF1C1C1E)
                        )
                        Text(
                            text = "Shift во время рисования",
                            fontSize = 10.sp,
                            color = if (isDark) Color(0xFF71717A) else Color(0xFF8E8E93)
                        )
                    }
                }

                Switch(
                    checked = settings.isStraightLine,
                    onCheckedChange = { isChecked ->
                        onSettingsChange(settings.copy(isStraightLine = isChecked))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF007AFF),
                        uncheckedThumbColor = if (isDark) Color(0xFF94A3B8) else Color.White,
                        uncheckedTrackColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.12f),
                        uncheckedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier.scale(0.80f)
                )
            }

            // 5. Выбор фона холста (Segmented Control без непрозрачных белых плашек)
            if (onGridTypeChange != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Фон холста",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF6E6E73)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
                            .padding(2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Transparent),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IosSegmentButton(
                                title = "Точки",
                                isSelected = gridType == GridType.DOTS,
                                onClick = { onGridTypeChange(GridType.DOTS) },
                                modifier = Modifier.weight(1f)
                            )
                            IosSegmentButton(
                                title = "Клетка",
                                isSelected = gridType == GridType.LINES,
                                onClick = { onGridTypeChange(GridType.LINES) },
                                modifier = Modifier.weight(1f)
                            )
                            IosSegmentButton(
                                title = "Чистый",
                                isSelected = gridType == GridType.NONE,
                                onClick = { onGridTypeChange(GridType.NONE) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Элемент сегмента внутри цельного Segmented Control:
 * - Активная кнопка: мягкая полупрозрачная синяя подсветка.
 * - Неактивная: прозрачный фон.
 * Без использования непрозрачного белого фона во избежание артефактов.
 */
@Composable
private fun IosSegmentButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalAppTheme.current.isDark
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) (if (isDark) Color(0xFF38BDF8) else Color(0xFF007AFF)).copy(alpha = 0.16f)
                else Color.Transparent
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isSelected) (if (isDark) Color(0xFF38BDF8) else Color(0xFF007AFF)) else (if (isDark) Color(0xFFE4E4E7) else Color(0xFF3C3C43).copy(alpha = 0.70f))
        )
    }
}
