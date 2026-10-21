package com.lkboard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lkboard.app.model.BoardElement
import com.lkboard.app.model.CanvasTransform
import kotlin.math.roundToInt

/**
 * Вспомогательный расчет размеров текстового блока с автопереносом.
 */
object TextLayoutHelper {
    const val MAX_TEXT_WIDTH = 700f
    const val MIN_TEXT_WIDTH = 120f
    const val LINE_HEIGHT_RATIO = 1.35f

    fun calculateDimensions(
        text: String,
        fontSize: Float = 19f,
        maxWidth: Float = MAX_TEXT_WIDTH
    ): Pair<Float, Float> {
        val lines = text.lines()
        val charWidth = fontSize * 0.58f
        var maxLineLen = 0
        var totalLinesCount = 0

        for (line in lines) {
            val len = line.length.coerceAtLeast(1)
            val approxLineWidth = len * charWidth

            if (approxLineWidth > maxWidth) {
                val wrapped = ((approxLineWidth / maxWidth).toInt() + 1)
                totalLinesCount += wrapped
                maxLineLen = (maxWidth / charWidth).toInt()
            } else {
                totalLinesCount += 1
                if (len > maxLineLen) {
                    maxLineLen = len
                }
            }
        }

        val computedWidth = (maxLineLen * charWidth + 24f).coerceIn(MIN_TEXT_WIDTH, maxWidth)
        val lineHeight = fontSize * LINE_HEIGHT_RATIO
        val computedHeight = (totalLinesCount * lineHeight + 18f).coerceAtLeast(42f)

        return Pair(computedWidth, computedHeight)
    }
}

/**
 * Полноценный Composable-компонент inline-редактирования TextElement на бесконечном холсте.
 * Включает всплывающую панель форматирования: размер шрифта, жирный/курсивный начертание, палитра цветов.
 */
@Composable
fun InlineTextEditorOverlay(
    element: BoardElement.Text,
    transform: CanvasTransform,
    onCommit: (String) -> Unit = {},
    onCommitDetailed: (text: String, fontSize: Float, isBold: Boolean, isItalic: Boolean, colorHex: Long) -> Unit = { t, _, _, _, _ -> onCommit(t) },
    onCancel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var textValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = element.text,
                selection = TextRange(element.text.length)
            )
        )
    }

    var fontSize by remember { mutableStateOf(element.fontSize) }
    var isBold by remember { mutableStateOf(element.isBold) }
    var isItalic by remember { mutableStateOf(element.isItalic) }
    var colorHex by remember { mutableStateOf(element.colorHex) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Экранные координаты и масштаб текстового блока
    val screenLeft = element.x * transform.scale + transform.panX
    val screenTop = element.y * transform.scale + transform.panY
    val screenWidth = (element.width * transform.scale).coerceAtLeast(140f)
    val screenHeight = (element.height * transform.scale).coerceAtLeast(42f)
    val scaledFontSize = (fontSize * transform.scale).coerceIn(11f, 72f)

    val palette = listOf(
        0xFF0F172AL, // Dark slate
        0xFF2563EBL, // Blue
        0xFF16A34AL, // Green
        0xFFDC2626L, // Red
        0xFF9333EAL, // Purple
        0xFFD97706L  // Amber
    )

    fun doCommit() {
        onCommitDetailed(textValue.text, fontSize, isBold, isItalic, colorHex)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // Клик по пустому пространству холста вне текста фиксирует изменения
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                doCommit()
            }
    ) {
        Column(
            modifier = Modifier
                .offset {
                    val toolbarH = 46
                    val targetY = (screenTop - toolbarH - 8).coerceAtLeast(10f).roundToInt()
                    IntOffset(screenLeft.roundToInt(), targetY)
                }
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
        ) {
            // Всплывающая плавающая панель инструментов форматирования текста
            Row(
                modifier = Modifier
                    .shadow(12.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Размеры шрифта
                Text("Размер:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))

                listOf(14f, 18f, 24f, 32f, 48f).forEach { sz ->
                    val isSelected = (fontSize.toInt() == sz.toInt())
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(if (isSelected) Color(0xFF2563EB) else Color(0xFFF1F5F9))
                            .clickable { fontSize = sz },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${sz.toInt()}",
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF1E293B)
                        )
                    }
                }

                Box(modifier = Modifier.width(1.dp).height(16.dp).background(Color(0xFFE2E8F0)))

                // Жирный (Bold)
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isBold) Color(0xFFDBEAFE) else Color.Transparent)
                        .clickable { isBold = !isBold },
                    contentAlignment = Alignment.Center
                ) {
                    Text("B", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = if (isBold) Color(0xFF1D4ED8) else Color(0xFF334155))
                }

                // Курсив (Italic)
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isItalic) Color(0xFFDBEAFE) else Color.Transparent)
                        .clickable { isItalic = !isItalic },
                    contentAlignment = Alignment.Center
                ) {
                    Text("I", fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isItalic) Color(0xFF1D4ED8) else Color(0xFF334155))
                }

                Box(modifier = Modifier.width(1.dp).height(16.dp).background(Color(0xFFE2E8F0)))

                // Цветовая палитра
                palette.forEach { cHex ->
                    val isSelected = colorHex == cHex
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color(cHex))
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) Color(0xFF2563EB) else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { colorHex = cHex }
                    )
                }

                Box(modifier = Modifier.width(1.dp).height(16.dp).background(Color(0xFFE2E8F0)))

                // Кнопка подтверждения
                Box(
                    modifier = Modifier
                        .height(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF2563EB))
                        .clickable { doCommit() }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✓ Готово", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Поле ввода текста с живым стилем
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(screenWidth.dp, screenHeight.dp)
                    .background(Color.White.copy(alpha = 0.95f), RoundedCornerShape(8.dp))
                    .border(2.dp, Color(0xFF2563EB), RoundedCornerShape(8.dp))
                    .padding(8.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* Защита от клика сквозь поле ввода */ }
            ) {
                BasicTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    textStyle = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = scaledFontSize.sp,
                        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                        color = Color(colorHex),
                        lineHeight = (scaledFontSize * 1.35f).sp
                    ),
                    cursorBrush = SolidColor(Color(0xFF2563EB)),
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(focusRequester)
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown) {
                                if (keyEvent.key == Key.Escape) {
                                    doCommit()
                                    true
                                } else {
                                    false
                                }
                            } else false
                        }
                )
            }
        }
    }
}
