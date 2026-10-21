package com.lkboard.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lkboard.app.model.ShapeSettings
import com.lkboard.app.model.ShapeType
import com.lkboard.app.model.StrokeStyle
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Всплывающее меню настроек геометрических фигур (ShapePropertiesPopup) в стиле Apple Liquid Glass:
 * - Монолитный стеклянный Box без использования Material 3 Surface и белых подложек.
 * - Единый внешний фон: Color(0xFFF5F5F7).copy(alpha = 0.85f), скругление 22.dp.
 * - Внутренние Layout-компоненты строго прозрачные (Color.Transparent).
 * - Выбор типа фигуры (прямоугольник, скругленный, эллипс, ромб, треугольник, стрелка, двусторонняя стрелка).
 * - Палитра цвета обводки.
 * - Палитра заливки с возможностью отключения (прозрачный).
 * - Слайдер толщины обводки (1–24 px).
 * - Селектор стиля линии (Сплошная / Пунктир / Точки).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShapePropertiesPopup(
    settings: ShapeSettings,
    isEditingSelectedShape: Boolean = false,
    onSettingsChange: (ShapeSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    val mainTitle = if (isEditingSelectedShape) "Свойства фигуры" else "Новая фигура"
    val shapeTitle = when (settings.shapeType) {
        ShapeType.RECTANGLE -> "Прямоугольник"
        ShapeType.ROUNDED_RECTANGLE -> "Скругленный прямоугольник"
        ShapeType.ELLIPSE -> "Овал / Круг"
        ShapeType.DIAMOND -> "Ромб"
        ShapeType.TRIANGLE -> "Треугольник"
        ShapeType.PARALLELOGRAM -> "Параллелограмм"
        ShapeType.HEXAGON -> "Шестиугольник"
        ShapeType.STAR -> "Звезда"
        ShapeType.ARROW -> "Стрелка"
        ShapeType.DOUBLE_ARROW -> "Двусторонняя стрелка"
        ShapeType.SPEECH_BUBBLE -> "Облако диалога"
        ShapeType.CLOUD -> "Облако мыслей"
        ShapeType.CUBE_3D -> "Куб 3D"
        ShapeType.CYLINDER_3D -> "Цилиндр 3D"
        ShapeType.CONE_3D -> "Конус 3D"
        ShapeType.PYRAMID_3D -> "Пирамида 3D"
        ShapeType.SPHERE_3D -> "Сфера 3D"
        ShapeType.FRAME -> "Рамка"
    }
    val formattedWidth = String.format(Locale.US, "%.1f px", settings.strokeWidth)

    // ЕДИНСТВЕННЫЙ внешний фоновый слой
    Box(
        modifier = modifier
            .width(304.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFFF7F7F9))
            .border(1.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(22.dp))
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            // 1. Заголовок и бейдж размера
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.background(Color.Transparent)) {
                    Text(
                        text = mainTitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1C1C1E)
                    )
                    Text(
                        text = shapeTitle,
                        fontSize = 11.sp,
                        color = Color(0xFF8E8E93)
                    )
                }

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
                        color = Color(0xFF007AFF)
                    )
                }
            }

            // 2. Сетка выбора геометрических фигур
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "Базовые 2D",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF6E6E73)
                )

                // Ряд 1: Прямоугольники, Эллипс, Ромб
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    ShapeTypeButton(
                        shapeType = ShapeType.RECTANGLE,
                        isSelected = settings.shapeType == ShapeType.RECTANGLE,
                        onClick = { onSettingsChange(settings.copy(shapeType = ShapeType.RECTANGLE)) },
                        modifier = Modifier.weight(1f)
                    )
                    ShapeTypeButton(
                        shapeType = ShapeType.ROUNDED_RECTANGLE,
                        isSelected = settings.shapeType == ShapeType.ROUNDED_RECTANGLE,
                        onClick = { onSettingsChange(settings.copy(shapeType = ShapeType.ROUNDED_RECTANGLE)) },
                        modifier = Modifier.weight(1f)
                    )
                    ShapeTypeButton(
                        shapeType = ShapeType.ELLIPSE,
                        isSelected = settings.shapeType == ShapeType.ELLIPSE,
                        onClick = { onSettingsChange(settings.copy(shapeType = ShapeType.ELLIPSE)) },
                        modifier = Modifier.weight(1f)
                    )
                    ShapeTypeButton(
                        shapeType = ShapeType.DIAMOND,
                        isSelected = settings.shapeType == ShapeType.DIAMOND,
                        onClick = { onSettingsChange(settings.copy(shapeType = ShapeType.DIAMOND)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Ряд 2: Треугольник, Параллелограмм, Шестиугольник, Звезда
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    ShapeTypeButton(
                        shapeType = ShapeType.TRIANGLE,
                        isSelected = settings.shapeType == ShapeType.TRIANGLE,
                        onClick = { onSettingsChange(settings.copy(shapeType = ShapeType.TRIANGLE)) },
                        modifier = Modifier.weight(1f)
                    )
                    ShapeTypeButton(
                        shapeType = ShapeType.PARALLELOGRAM,
                        isSelected = settings.shapeType == ShapeType.PARALLELOGRAM,
                        onClick = { onSettingsChange(settings.copy(shapeType = ShapeType.PARALLELOGRAM)) },
                        modifier = Modifier.weight(1f)
                    )
                    ShapeTypeButton(
                        shapeType = ShapeType.HEXAGON,
                        isSelected = settings.shapeType == ShapeType.HEXAGON,
                        onClick = { onSettingsChange(settings.copy(shapeType = ShapeType.HEXAGON)) },
                        modifier = Modifier.weight(1f)
                    )
                    ShapeTypeButton(
                        shapeType = ShapeType.STAR,
                        isSelected = settings.shapeType == ShapeType.STAR,
                        onClick = { onSettingsChange(settings.copy(shapeType = ShapeType.STAR)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "3D / Объемные",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF6E6E73),
                    modifier = Modifier.padding(top = 2.dp)
                )

                // Ряд 3: 3D Фигуры (Куб, Цилиндр, Конус, Пирамида, Сфера)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ShapeTypeButton(
                        shapeType = ShapeType.CUBE_3D,
                        isSelected = settings.shapeType == ShapeType.CUBE_3D,
                        onClick = { onSettingsChange(settings.copy(shapeType = ShapeType.CUBE_3D)) },
                        modifier = Modifier.weight(1f)
                    )
                    ShapeTypeButton(
                        shapeType = ShapeType.CYLINDER_3D,
                        isSelected = settings.shapeType == ShapeType.CYLINDER_3D,
                        onClick = { onSettingsChange(settings.copy(shapeType = ShapeType.CYLINDER_3D)) },
                        modifier = Modifier.weight(1f)
                    )
                    ShapeTypeButton(
                        shapeType = ShapeType.CONE_3D,
                        isSelected = settings.shapeType == ShapeType.CONE_3D,
                        onClick = { onSettingsChange(settings.copy(shapeType = ShapeType.CONE_3D)) },
                        modifier = Modifier.weight(1f)
                    )
                    ShapeTypeButton(
                        shapeType = ShapeType.PYRAMID_3D,
                        isSelected = settings.shapeType == ShapeType.PYRAMID_3D,
                        onClick = { onSettingsChange(settings.copy(shapeType = ShapeType.PYRAMID_3D)) },
                        modifier = Modifier.weight(1f)
                    )
                    ShapeTypeButton(
                        shapeType = ShapeType.SPHERE_3D,
                        isSelected = settings.shapeType == ShapeType.SPHERE_3D,
                        onClick = { onSettingsChange(settings.copy(shapeType = ShapeType.SPHERE_3D)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Стрелки и выноски",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF6E6E73),
                    modifier = Modifier.padding(top = 2.dp)
                )

                // Ряд 4: Стрелка, Двусторонняя стрелка, Облако диалога, Облако мыслей
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    ShapeTypeButton(
                        shapeType = ShapeType.ARROW,
                        isSelected = settings.shapeType == ShapeType.ARROW,
                        onClick = { onSettingsChange(settings.copy(shapeType = ShapeType.ARROW)) },
                        modifier = Modifier.weight(1f)
                    )
                    ShapeTypeButton(
                        shapeType = ShapeType.DOUBLE_ARROW,
                        isSelected = settings.shapeType == ShapeType.DOUBLE_ARROW,
                        onClick = { onSettingsChange(settings.copy(shapeType = ShapeType.DOUBLE_ARROW)) },
                        modifier = Modifier.weight(1f)
                    )
                    ShapeTypeButton(
                        shapeType = ShapeType.SPEECH_BUBBLE,
                        isSelected = settings.shapeType == ShapeType.SPEECH_BUBBLE,
                        onClick = { onSettingsChange(settings.copy(shapeType = ShapeType.SPEECH_BUBBLE)) },
                        modifier = Modifier.weight(1f)
                    )
                    ShapeTypeButton(
                        shapeType = ShapeType.CLOUD,
                        isSelected = settings.shapeType == ShapeType.CLOUD,
                        onClick = { onSettingsChange(settings.copy(shapeType = ShapeType.CLOUD)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 3. Палитра цвета контура (обводки)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Контур",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF6E6E73)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val strokeRows = ShapeSettings.PRESET_STROKE_COLORS.chunked(7)
                    strokeRows.forEach { rowColors ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Transparent),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            rowColors.forEach { colorHex ->
                                val isSelected = settings.strokeColorHex == colorHex
                                val color = Color(colorHex)
                                val scale by animateFloatAsState(if (isSelected) 1.06f else 1.0f)

                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color.Transparent)
                                        .scale(scale),
                                    contentAlignment = Alignment.Center
                                ) {
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
                                                onSettingsChange(settings.copy(strokeColorHex = colorHex))
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Палитра цвета заливки (2 ряда по 7 вариантов)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Заливка",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF6E6E73)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val fillRows = ShapeSettings.PRESET_FILL_COLORS.chunked(7)
                    fillRows.forEach { rowFills ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Transparent),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            rowFills.forEach { fillHex ->
                                val isSelected = settings.fillColorHex == fillHex
                                val scale by animateFloatAsState(if (isSelected) 1.06f else 1.0f)

                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color.Transparent)
                                        .scale(scale),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .border(
                                                    width = 2.dp,
                                                    color = Color(0xFF007AFF),
                                                    shape = CircleShape
                                                )
                                        )
                                    }

                                    if (fillHex == null) {
                                        // Кнопка «Без заливки / Прозрачный»
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.04f))
                                                .border(1.dp, Color.Black.copy(alpha = 0.15f), CircleShape)
                                                .clickable {
                                                    onSettingsChange(settings.copy(fillColorHex = null))
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Block,
                                                contentDescription = "Без заливки",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    } else {
                                        // Полупрозрачная или цветная плашка
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color(fillHex))
                                                .border(0.5.dp, Color.Black.copy(alpha = 0.12f), CircleShape)
                                                .clickable {
                                                    onSettingsChange(settings.copy(fillColorHex = fillHex))
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Слайдер толщины обводки
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Толщина линии",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF6E6E73)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Миниатюра толщины
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        val dotSize = (settings.strokeWidth * 0.7f).coerceIn(2.5f, 18f).dp
                        Box(
                            modifier = Modifier
                                .size(dotSize)
                                .clip(CircleShape)
                                .background(Color(settings.strokeColorHex))
                        )
                    }

                    Slider(
                        value = settings.strokeWidth,
                        onValueChange = { newWidth ->
                            onSettingsChange(settings.copy(strokeWidth = (newWidth * 10).roundToInt() / 10f))
                        },
                        valueRange = 1.0f..24.0f,
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

                    Text(
                        text = String.format(Locale.US, "%.1f", settings.strokeWidth),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF3C3C43),
                        modifier = Modifier.width(28.dp),
                        textAlign = TextAlign.End
                    )
                }
            }

            // 6. Селектор стиля линии (Сплошная / Пунктир / Точки)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Стиль линии",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF6E6E73)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.05f))
                        .padding(2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        StrokeStyleSegment(
                            title = "Сплошная",
                            isSelected = settings.strokeStyle == StrokeStyle.SOLID,
                            onClick = { onSettingsChange(settings.copy(strokeStyle = StrokeStyle.SOLID)) },
                            modifier = Modifier.weight(1f)
                        )
                        StrokeStyleSegment(
                            title = "Пунктир",
                            isSelected = settings.strokeStyle == StrokeStyle.DASHED,
                            onClick = { onSettingsChange(settings.copy(strokeStyle = StrokeStyle.DASHED)) },
                            modifier = Modifier.weight(1f)
                        )
                        StrokeStyleSegment(
                            title = "Точки",
                            isSelected = settings.strokeStyle == StrokeStyle.DOTTED,
                            onClick = { onSettingsChange(settings.copy(strokeStyle = StrokeStyle.DOTTED)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Кнопка выбора типа фигуры с векторной векторной миниатюрой.
 */
@Composable
private fun ShapeTypeButton(
    shapeType: ShapeType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bgColor = when {
        isSelected -> Color(0xFF007AFF).copy(alpha = 0.12f)
        isHovered -> Color.Black.copy(alpha = 0.05f)
        else -> Color.Transparent
    }
    val iconColor = if (isSelected) Color(0xFF007AFF) else Color(0xFF3C3C43)

    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .then(
                if (isSelected) {
                    Modifier.border(1.dp, Color(0xFF007AFF).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                } else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(20.dp)) {
            val stroke = Stroke(width = 1.6f, cap = StrokeCap.Round)
            val w = size.width
            val h = size.height

            when (shapeType) {
                ShapeType.RECTANGLE -> {
                    drawRect(color = iconColor, topLeft = Offset(1f, 3f), size = Size(w - 2f, h - 6f), style = stroke)
                }
                ShapeType.ROUNDED_RECTANGLE -> {
                    drawRoundRect(
                        color = iconColor,
                        topLeft = Offset(1f, 3f),
                        size = Size(w - 2f, h - 6f),
                        cornerRadius = CornerRadius(4f, 4f),
                        style = stroke
                    )
                }
                ShapeType.ELLIPSE -> {
                    drawOval(color = iconColor, topLeft = Offset(1f, 2f), size = Size(w - 2f, h - 4f), style = stroke)
                }
                ShapeType.DIAMOND -> {
                    val p = Path().apply {
                        moveTo(w / 2f, 1f)
                        lineTo(w - 1f, h / 2f)
                        lineTo(w / 2f, h - 1f)
                        lineTo(1f, h / 2f)
                        close()
                    }
                    drawPath(path = p, color = iconColor, style = stroke)
                }
                ShapeType.TRIANGLE -> {
                    val p = Path().apply {
                        moveTo(w / 2f, 2f)
                        lineTo(w - 1f, h - 2f)
                        lineTo(1f, h - 2f)
                        close()
                    }
                    drawPath(path = p, color = iconColor, style = stroke)
                }
                ShapeType.PARALLELOGRAM -> {
                    val skew = w * 0.25f
                    val p = Path().apply {
                        moveTo(skew, 2f)
                        lineTo(w - 1f, 2f)
                        lineTo(w - skew, h - 2f)
                        lineTo(1f, h - 2f)
                        close()
                    }
                    drawPath(path = p, color = iconColor, style = stroke)
                }
                ShapeType.HEXAGON -> {
                    val p = Path().apply {
                        moveTo(w * 0.5f, 1f)
                        lineTo(w - 1f, h * 0.25f)
                        lineTo(w - 1f, h * 0.75f)
                        lineTo(w * 0.5f, h - 1f)
                        lineTo(1f, h * 0.75f)
                        lineTo(1f, h * 0.25f)
                        close()
                    }
                    drawPath(path = p, color = iconColor, style = stroke)
                }
                ShapeType.STAR -> {
                    val p = Path()
                    val cx = w / 2f
                    val cy = h / 2f
                    val outerR = minOf(w, h) / 2f - 1f
                    val innerR = outerR * 0.45f
                    val points = 5
                    var angle = -Math.PI / 2.0
                    val step = Math.PI / points
                    p.moveTo((cx + outerR * Math.cos(angle)).toFloat(), (cy + outerR * Math.sin(angle)).toFloat())
                    for (i in 1 until points * 2) {
                        angle += step
                        val r = if (i % 2 == 1) innerR else outerR
                        p.lineTo((cx + r * Math.cos(angle)).toFloat(), (cy + r * Math.sin(angle)).toFloat())
                    }
                    p.close()
                    drawPath(path = p, color = iconColor, style = stroke)
                }
                ShapeType.ARROW -> {
                    val midY = h / 2f
                    drawLine(color = iconColor, start = Offset(2f, midY), end = Offset(w - 6f, midY), strokeWidth = 1.6f)
                    val head = Path().apply {
                        moveTo(w - 1f, midY)
                        lineTo(w - 6f, midY - 4.5f)
                        lineTo(w - 6f, midY + 4.5f)
                        close()
                    }
                    drawPath(path = head, color = iconColor, style = Fill)
                }
                ShapeType.DOUBLE_ARROW -> {
                    val midY = h / 2f
                    drawLine(color = iconColor, start = Offset(5f, midY), end = Offset(w - 5f, midY), strokeWidth = 1.6f)
                    val leftHead = Path().apply {
                        moveTo(1f, midY)
                        lineTo(6f, midY - 4f)
                        lineTo(6f, midY + 4f)
                        close()
                    }
                    val rightHead = Path().apply {
                        moveTo(w - 1f, midY)
                        lineTo(w - 6f, midY - 4f)
                        lineTo(w - 6f, midY + 4f)
                        close()
                    }
                    drawPath(path = leftHead, color = iconColor, style = Fill)
                    drawPath(path = rightHead, color = iconColor, style = Fill)
                }
                ShapeType.SPEECH_BUBBLE -> {
                    val p = Path().apply {
                        addRoundRect(androidx.compose.ui.geometry.RoundRect(1f, 1f, w - 1f, h - 5f, CornerRadius(3f, 3f)))
                        moveTo(w * 0.3f, h - 5f)
                        lineTo(w * 0.2f, h - 1f)
                        lineTo(w * 0.5f, h - 5f)
                    }
                    drawPath(path = p, color = iconColor, style = stroke)
                }
                ShapeType.CLOUD -> {
                    drawOval(color = iconColor, topLeft = Offset(1f, 5f), size = Size(w * 0.55f, h * 0.55f), style = stroke)
                    drawOval(color = iconColor, topLeft = Offset(w * 0.35f, 2f), size = Size(w * 0.6f, h * 0.65f), style = stroke)
                    drawOval(color = iconColor, topLeft = Offset(w * 0.2f, 7f), size = Size(w * 0.75f, h * 0.5f), style = stroke)
                }
                ShapeType.CUBE_3D -> {
                    val off = w * 0.28f
                    drawRect(color = iconColor, topLeft = Offset(1f, off), size = Size(w - off - 1f, h - off - 1f), style = stroke)
                    drawLine(color = iconColor, start = Offset(1f, off), end = Offset(1f + off, 1f), strokeWidth = 1.4f)
                    drawLine(color = iconColor, start = Offset(w - off, off), end = Offset(w - 1f, 1f), strokeWidth = 1.4f)
                    drawLine(color = iconColor, start = Offset(1f + off, 1f), end = Offset(w - 1f, 1f), strokeWidth = 1.4f)
                    drawLine(color = iconColor, start = Offset(w - off, h - 1f), end = Offset(w - 1f, h - off), strokeWidth = 1.4f)
                    drawLine(color = iconColor, start = Offset(w - 1f, 1f), end = Offset(w - 1f, h - off), strokeWidth = 1.4f)
                }
                ShapeType.CYLINDER_3D -> {
                    val topH = h * 0.32f
                    drawOval(color = iconColor, topLeft = Offset(2f, 1f), size = Size(w - 4f, topH), style = stroke)
                    drawLine(color = iconColor, start = Offset(2f, topH / 2f + 1f), end = Offset(2f, h - topH / 2f - 1f), strokeWidth = 1.4f)
                    drawLine(color = iconColor, start = Offset(w - 2f, topH / 2f + 1f), end = Offset(w - 2f, h - topH / 2f - 1f), strokeWidth = 1.4f)
                    val bottomArc = Path().apply {
                        arcTo(Rect(2f, h - topH - 1f, w - 2f, h - 1f), 0f, 180f, false)
                    }
                    drawPath(path = bottomArc, color = iconColor, style = stroke)
                }
                ShapeType.CONE_3D -> {
                    val baseH = h * 0.30f
                    drawLine(color = iconColor, start = Offset(w / 2f, 1f), end = Offset(2f, h - baseH / 2f), strokeWidth = 1.4f)
                    drawLine(color = iconColor, start = Offset(w / 2f, 1f), end = Offset(w - 2f, h - baseH / 2f), strokeWidth = 1.4f)
                    val baseArc = Path().apply {
                        arcTo(Rect(2f, h - baseH - 1f, w - 2f, h - 1f), 0f, 180f, false)
                    }
                    drawPath(path = baseArc, color = iconColor, style = stroke)
                }
                ShapeType.PYRAMID_3D -> {
                    val apex = Offset(w / 2f, 1f)
                    val bl = Offset(2f, h - 4f)
                    val bm = Offset(w * 0.45f, h - 1f)
                    val br = Offset(w - 2f, h - 5f)
                    drawLine(color = iconColor, start = apex, end = bl, strokeWidth = 1.4f)
                    drawLine(color = iconColor, start = apex, end = bm, strokeWidth = 1.4f)
                    drawLine(color = iconColor, start = apex, end = br, strokeWidth = 1.4f)
                    drawLine(color = iconColor, start = bl, end = bm, strokeWidth = 1.4f)
                    drawLine(color = iconColor, start = bm, end = br, strokeWidth = 1.4f)
                }
                ShapeType.SPHERE_3D -> {
                    drawOval(color = iconColor, topLeft = Offset(1f, 1f), size = Size(w - 2f, h - 2f), style = stroke)
                    val equator = Path().apply {
                        arcTo(Rect(1f, h * 0.35f, w - 1f, h * 0.65f), 0f, 180f, false)
                    }
                    drawPath(path = equator, color = iconColor, style = stroke)
                }
                ShapeType.FRAME -> {
                    drawRoundRect(
                        color = iconColor,
                        topLeft = Offset(1f, 1f),
                        size = Size(w - 2f, h - 2f),
                        cornerRadius = CornerRadius(2f, 2f),
                        style = stroke
                    )
                }
            }
        }
    }
}

/**
 * Элемент выбора стиля линии в Segmented Control.
 */
@Composable
private fun StrokeStyleSegment(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) Color(0xFF007AFF).copy(alpha = 0.15f)
                else Color.Transparent
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isSelected) Color(0xFF007AFF) else Color(0xFF3C3C43).copy(alpha = 0.70f)
        )
    }
}
