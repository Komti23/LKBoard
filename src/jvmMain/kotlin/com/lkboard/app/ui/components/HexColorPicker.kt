package com.lkboard.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lkboard.app.data.AppPreferences
import com.lkboard.app.ui.theme.LocalAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

// =========================================================================
// УТИЛИТЫ КОНВЕРТАЦИИ HSV <-> RGB <-> HEX <-> LONG
// =========================================================================

data class HsvColor(
    val hue: Float,        // 0f .. 360f
    val saturation: Float, // 0f .. 1f
    val value: Float,      // 0f .. 1f
    val alpha: Float = 1f  // 0f .. 1f
)

fun hsvToRgb(hue: Float, saturation: Float, value: Float): Triple<Float, Float, Float> {
    val h = ((hue % 360f) + 360f) % 360f
    val s = saturation.coerceIn(0f, 1f)
    val v = value.coerceIn(0f, 1f)

    val c = v * s
    val x = c * (1f - abs((h / 60f) % 2f - 1f))
    val m = v - c

    val (r1, g1, b1) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    return Triple(
        (r1 + m).coerceIn(0f, 1f),
        (g1 + m).coerceIn(0f, 1f),
        (b1 + m).coerceIn(0f, 1f)
    )
}

fun rgbToHsv(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
    val red = r.coerceIn(0f, 1f)
    val green = g.coerceIn(0f, 1f)
    val blue = b.coerceIn(0f, 1f)

    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val delta = max - min

    val hue = when {
        delta < 0.0001f -> 0f
        max == red -> (((green - blue) / delta) % 6f) * 60f
        max == green -> (((blue - red) / delta) + 2f) * 60f
        else -> (((red - green) / delta) + 4f) * 60f
    }.let { if (it < 0f) it + 360f else it }

    val sat = if (max < 0.0001f) 0f else delta / max
    val valVal = max

    return Triple(hue, sat, valVal)
}

fun longColorToHsv(colorHex: Long): HsvColor {
    val a = (((colorHex shr 24) and 0xFF).toInt()) / 255f
    val r = (((colorHex shr 16) and 0xFF).toInt()) / 255f
    val g = (((colorHex shr 8) and 0xFF).toInt()) / 255f
    val b = ((colorHex and 0xFF).toInt()) / 255f
    val (h, s, v) = rgbToHsv(r, g, b)
    return HsvColor(h, s, v, a)
}

fun hsvToLongColor(hsv: HsvColor): Long {
    val (r, g, b) = hsvToRgb(hsv.hue, hsv.saturation, hsv.value)
    val aInt = (hsv.alpha * 255f).roundToInt().coerceIn(0, 255)
    val rInt = (r * 255f).roundToInt().coerceIn(0, 255)
    val gInt = (g * 255f).roundToInt().coerceIn(0, 255)
    val bInt = (b * 255f).roundToInt().coerceIn(0, 255)

    return ((aInt.toLong() and 0xFF) shl 24) or
            ((rInt.toLong() and 0xFF) shl 16) or
            ((gInt.toLong() and 0xFF) shl 8) or
            (bInt.toLong() and 0xFF)
}

fun hsvToComposeColor(hsv: HsvColor): Color {
    val (r, g, b) = hsvToRgb(hsv.hue, hsv.saturation, hsv.value)
    return Color(red = r, green = g, blue = b, alpha = hsv.alpha)
}

fun parseHexInput(input: String): Long? {
    val clean = input.trim().removePrefix("#").trim()
    return when (clean.length) {
        3 -> { // #RGB -> #RRGGBB
            val r = clean[0]
            val g = clean[1]
            val b = clean[2]
            val expanded = "$r$r$g$g$b$b"
            expanded.toLongOrNull(16)?.let { 0xFF000000L or (it and 0xFFFFFFL) }
        }
        4 -> { // #ARGB -> #AARRGGBB
            val a = clean[0]
            val r = clean[1]
            val g = clean[2]
            val b = clean[3]
            val expanded = "$a$a$r$r$g$g$b$b"
            expanded.toLongOrNull(16)
        }
        6 -> { // #RRGGBB
            clean.toLongOrNull(16)?.let { 0xFF000000L or (it and 0xFFFFFFL) }
        }
        8 -> { // #AARRGGBB
            clean.toLongOrNull(16)
        }
        else -> null
    }
}

fun formatHex(colorHex: Long, includeAlpha: Boolean = false): String {
    return if (includeAlpha) {
        String.format(Locale.US, "#%08X", colorHex)
    } else {
        String.format(Locale.US, "#%06X", colorHex and 0xFFFFFFL)
    }
}

// =========================================================================
// ИНТЕРАКТИВНЫЙ КОМПОНЕНТ ПАЛИТРЫ И ВВОДА HEX
// =========================================================================

/**
 * Интерактивный селектор цвета с 2D полем насыщенности и яркости,
 * спектральным слайдером оттенка (Hue), слайдером прозрачности (Alpha),
 * полем ввода любого произвольного HEX кода и историей недавних цветов.
 */
@Composable
fun HexColorPicker(
    currentColorHex: Long,
    onColorChanged: (Long) -> Unit,
    showAlpha: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isDark = LocalAppTheme.current.isDark
    val scope = rememberCoroutineScope()

    // Внутреннее HSV состояние
    val initialHsv = remember(currentColorHex) { longColorToHsv(currentColorHex) }
    var hue by remember { mutableFloatStateOf(initialHsv.hue) }
    var saturation by remember { mutableFloatStateOf(initialHsv.saturation) }
    var value by remember { mutableFloatStateOf(initialHsv.value) }
    var alpha by remember { mutableFloatStateOf(if (showAlpha) initialHsv.alpha else 1f) }

    // Текстовое поле ввода HEX
    var hexInputText by remember {
        mutableStateOf(formatHex(currentColorHex, showAlpha).removePrefix("#"))
    }
    var isHexValid by remember { mutableStateOf(true) }
    var isCopied by remember { mutableStateOf(false) }

    // Синхронизация при внешнем изменении currentColorHex
    LaunchedEffect(currentColorHex) {
        val extHsv = longColorToHsv(currentColorHex)
        hue = extHsv.hue
        saturation = extHsv.saturation
        value = extHsv.value
        if (showAlpha) alpha = extHsv.alpha
        hexInputText = formatHex(currentColorHex, showAlpha).removePrefix("#")
        isHexValid = true
    }

    // Вспомогательная функция отправки изменения цвета
    fun notifyColorChange(newHue: Float, newSat: Float, newVal: Float, newAlpha: Float) {
        val hsv = HsvColor(newHue, newSat, newVal, newAlpha)
        val newLong = hsvToLongColor(hsv)
        hexInputText = formatHex(newLong, showAlpha).removePrefix("#")
        isHexValid = true
        onColorChanged(newLong)
    }

    val currentComposeColor = hsvToComposeColor(HsvColor(hue, saturation, value, alpha))
    val recentColors = remember { AppPreferences.loadRecentColors() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Интерактивное 2D поле Saturation / Value
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = 1.dp,
                    color = if (isDark) Color(0xFF2E333D) else Color.Black.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                )
                .pointerInput(hue) {
                    detectTapGestures { offset ->
                        val newSat = (offset.x / size.width).coerceIn(0f, 1f)
                        val newVal = (1f - (offset.y / size.height)).coerceIn(0f, 1f)
                        saturation = newSat
                        value = newVal
                        notifyColorChange(hue, newSat, newVal, alpha)
                    }
                }
                .pointerInput(hue) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val newSat = (change.position.x / size.width).coerceIn(0f, 1f)
                        val newVal = (1f - (change.position.y / size.height)).coerceIn(0f, 1f)
                        saturation = newSat
                        value = newVal
                        notifyColorChange(hue, newSat, newVal, alpha)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val pureHueTriple = hsvToRgb(hue, 1f, 1f)
                val pureHueColor = Color(pureHueTriple.first, pureHueTriple.second, pureHueTriple.third)

                // Горизонтальный градиент: Белый -> Чистый оттенок
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.White, pureHueColor)
                    )
                )
                // Вертикальный градиент: Прозрачный -> Черный
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black)
                    )
                )

                // Курсор-индикатор текущего выбранного цвета
                val thumbX = saturation * size.width
                val thumbY = (1f - value) * size.height
                val cursorColor = Color(
                    red = hsvToRgb(hue, saturation, value).first,
                    green = hsvToRgb(hue, saturation, value).second,
                    blue = hsvToRgb(hue, saturation, value).third
                )

                // Тень и белый контур курсора
                drawCircle(
                    color = Color.Black.copy(alpha = 0.40f),
                    radius = 9.dp.toPx(),
                    center = Offset(thumbX, thumbY),
                    style = Stroke(width = 2.dp.toPx())
                )
                drawCircle(
                    color = Color.White,
                    radius = 8.dp.toPx(),
                    center = Offset(thumbX, thumbY),
                    style = Stroke(width = 2.5.dp.toPx())
                )
                drawCircle(
                    color = cursorColor,
                    radius = 5.5.dp.toPx(),
                    center = Offset(thumbX, thumbY)
                )
            }
        }

        // 2. Радужный слайдер оттенка (Hue 0°..360°)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .border(
                        0.5.dp,
                        if (isDark) Color(0xFF3F4554) else Color.Black.copy(alpha = 0.15f),
                        RoundedCornerShape(7.dp)
                    )
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val newHue = ((offset.x / size.width).coerceIn(0f, 1f)) * 360f
                            hue = newHue
                            notifyColorChange(newHue, saturation, value, alpha)
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val newHue = ((change.position.x / size.width).coerceIn(0f, 1f)) * 360f
                            hue = newHue
                            notifyColorChange(newHue, saturation, value, alpha)
                        }
                    }
            ) {
                val rainbowBrush = remember {
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Red,
                            Color.Yellow,
                            Color.Green,
                            Color.Cyan,
                            Color.Blue,
                            Color.Magenta,
                            Color.Red
                        )
                    )
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(brush = rainbowBrush)

                    // Индикатор ползунка Hue
                    val thumbX = (hue / 360f) * size.width
                    val thumbHueTriple = hsvToRgb(hue, 1f, 1f)
                    val thumbColor = Color(thumbHueTriple.first, thumbHueTriple.second, thumbHueTriple.third)

                    drawCircle(
                        color = Color.Black.copy(alpha = 0.35f),
                        radius = 7.dp.toPx(),
                        center = Offset(thumbX, size.height / 2f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 6.5.dp.toPx(),
                        center = Offset(thumbX, size.height / 2f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(
                        color = thumbColor,
                        radius = 4.5.dp.toPx(),
                        center = Offset(thumbX, size.height / 2f)
                    )
                }
            }
        }

        // 3. Слайдер непрозрачности (Alpha), если включен
        if (showAlpha) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Непрозрачность",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                    Text(
                        text = "${(alpha * 100).roundToInt()}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .border(
                            0.5.dp,
                            if (isDark) Color(0xFF3F4554) else Color.Black.copy(alpha = 0.15f),
                            RoundedCornerShape(7.dp)
                        )
                        .pointerInput(hue, saturation, value) {
                            detectTapGestures { offset ->
                                val newAlpha = (offset.x / size.width).coerceIn(0f, 1f)
                                alpha = newAlpha
                                notifyColorChange(hue, saturation, value, newAlpha)
                            }
                        }
                        .pointerInput(hue, saturation, value) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                val newAlpha = (change.position.x / size.width).coerceIn(0f, 1f)
                                alpha = newAlpha
                                notifyColorChange(hue, saturation, value, newAlpha)
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Шахматный фон
                        val checkSize = 5.dp.toPx()
                        var x = 0f
                        var row = 0
                        while (row * checkSize < size.height) {
                            x = 0f
                            var col = 0
                            while (x < size.width) {
                                val isEven = (row + col) % 2 == 0
                                drawRect(
                                    color = if (isEven) Color(0xFFE2E8F0) else Color.White,
                                    topLeft = Offset(x, row * checkSize),
                                    size = Size(checkSize, checkSize)
                                )
                                x += checkSize
                                col++
                            }
                            row++
                        }

                        // Градиент от 0% до 100% непрозрачности текущего цвета
                        val baseOpaque = Color(
                            red = hsvToRgb(hue, saturation, value).first,
                            green = hsvToRgb(hue, saturation, value).second,
                            blue = hsvToRgb(hue, saturation, value).third,
                            alpha = 1f
                        )
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(baseOpaque.copy(alpha = 0f), baseOpaque)
                            )
                        )

                        // Ползунок Alpha
                        val thumbX = alpha * size.width
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.35f),
                            radius = 7.dp.toPx(),
                            center = Offset(thumbX, size.height / 2f),
                            style = Stroke(width = 2.dp.toPx())
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 6.5.dp.toPx(),
                            center = Offset(thumbX, size.height / 2f),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }
        }

        // 4. Поле ввода HEX кода и предпросмотр
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Крупный свотч текущего цвета с обводкой
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(currentComposeColor)
                    .border(
                        width = 1.dp,
                        color = if (isDark) Color(0xFF3F4554) else Color.Black.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    )
            )

            // Поле ручного ввода произвольного HEX
            OutlinedTextField(
                value = hexInputText,
                onValueChange = { input ->
                    val clean = input.removePrefix("#").take(if (showAlpha) 8 else 6).uppercase()
                    hexInputText = clean
                    val parsed = parseHexInput(clean)
                    if (parsed != null) {
                        isHexValid = true
                        val newHsv = longColorToHsv(parsed)
                        hue = newHsv.hue
                        saturation = newHsv.saturation
                        value = newHsv.value
                        if (showAlpha) alpha = newHsv.alpha
                        onColorChanged(parsed)
                    } else {
                        isHexValid = clean.isBlank() || clean.matches(Regex("[0-9A-Fa-f]*"))
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                prefix = {
                    Text(
                        text = "#",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                    )
                },
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
                ),
                isError = !isHexValid,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isHexValid) Color(0xFF007AFF) else Color(0xFFEF4444),
                    unfocusedBorderColor = if (isHexValid) {
                        if (isDark) Color(0xFF3F4554) else Color(0xFFCBD5E1)
                    } else Color(0xFFEF4444),
                    focusedContainerColor = if (isDark) Color(0xFF20242C) else Color.White,
                    unfocusedContainerColor = if (isDark) Color(0xFF20242C) else Color.White
                )
            )

            // Кнопка копирования HEX в буфер обмена
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isCopied) Color(0xFF16A34A).copy(alpha = 0.15f) else (if (isDark) Color(0xFF262A34) else Color(0xFFF1F5F9)))
                    .border(
                        1.dp,
                        if (isCopied) Color(0xFF16A34A) else (if (isDark) Color(0xFF3F4554) else Color(0xFFE2E8F0)),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        try {
                            val hexToCopy = formatHex(hsvToLongColor(HsvColor(hue, saturation, value, alpha)), showAlpha)
                            Toolkit.getDefaultToolkit().systemClipboard.setContents(
                                StringSelection(hexToCopy),
                                null
                            )
                            isCopied = true
                            scope.launch {
                                delay(1500L)
                                isCopied = false
                            }
                        } catch (_: Exception) {}
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = "Копировать HEX",
                    tint = if (isCopied) Color(0xFF16A34A) else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // 5. Недавние цвета
        if (recentColors.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Недавние цвета",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    recentColors.take(8).forEach { rHex ->
                        val isSelected = (currentColorHex and 0xFFFFFFL) == (rHex and 0xFFFFFFL)
                        val color = Color(rHex)
                        val scale by animateFloatAsState(if (isSelected) 1.15f else 1.0f)

                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .scale(scale),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .border(2.dp, Color(0xFF007AFF), CircleShape)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        0.5.dp,
                                        if (isDark) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.15f),
                                        CircleShape
                                    )
                                    .clickable {
                                        val newHsv = longColorToHsv(rHex)
                                        hue = newHsv.hue
                                        saturation = newHsv.saturation
                                        value = newHsv.value
                                        notifyColorChange(newHsv.hue, newHsv.saturation, newHsv.value, alpha)
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// ДИАЛОГ ВЫБОРА ЦВЕТА (HEXCOLORPICKERDIALOG)
// =========================================================================

/**
 * Модальный диалог Apple Liquid Glass для точной настройки цвета через интерактивную палитру и HEX.
 */
@Composable
fun HexColorPickerDialog(
    title: String = "Выбор цвета (HEX и палитра)",
    initialColorHex: Long,
    showAlpha: Boolean = false,
    onDismiss: () -> Unit,
    onColorSelected: (Long) -> Unit
) {
    val isDark = LocalAppTheme.current.isDark
    var selectedColorHex by remember { mutableStateOf(initialColorHex) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.40f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(320.dp)
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(22.dp),
                    spotColor = Color.Black.copy(alpha = 0.15f)
                )
                .clip(RoundedCornerShape(22.dp))
                .background(if (isDark) Color(0xFF181B20).copy(alpha = 0.96f) else Color(0xFFFBFBFD).copy(alpha = 0.96f))
                .border(
                    1.dp,
                    if (isDark) Color(0xFF2E333D) else Color.White.copy(alpha = 0.85f),
                    RoundedCornerShape(22.dp)
                )
                .clickable(enabled = false) {}
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Шапка
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF007AFF).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = Color(0xFF007AFF),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Сам Color Picker
                HexColorPicker(
                    currentColorHex = selectedColorHex,
                    onColorChanged = { selectedColorHex = it },
                    showAlpha = showAlpha
                )

                // Кнопки Применить / Отмена
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                        )
                    ) {
                        Text("Отмена", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            AppPreferences.saveRecentColor(selectedColorHex)
                            onColorSelected(selectedColorHex)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF007AFF),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Применить", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
