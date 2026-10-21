package com.lkboard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
    const val MAX_TEXT_WIDTH = 600f
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
 * Накладывается ровно поверх редактируемого текстового блока в мировых координатах.
 */
@Composable
fun InlineTextEditorOverlay(
    element: BoardElement.Text,
    transform: CanvasTransform,
    onCommit: (String) -> Unit,
    onCancel: () -> Unit,
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

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Экранные координаты и масштаб текстового блока
    val screenLeft = element.x * transform.scale + transform.panX
    val screenTop = element.y * transform.scale + transform.panY
    val screenWidth = (element.width * transform.scale).coerceAtLeast(120f)
    val screenHeight = (element.height * transform.scale).coerceAtLeast(36f)
    val scaledFontSize = (element.fontSize * transform.scale).coerceIn(11f, 72f)

    Box(
        modifier = modifier
            .fillMaxSize()
            // Клик по пустому пространству холста вне текста фиксирует изменения
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onCommit(textValue.text)
            }
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(screenLeft.roundToInt(), screenTop.roundToInt()) }
                .size(screenWidth.dp, screenHeight.dp)
                .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(6.dp))
                .border(1.5.dp, Color(0xFF2563EB), RoundedCornerShape(6.dp))
                .padding(6.dp)
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
                    fontWeight = if (element.isBold) FontWeight.Bold else FontWeight.Normal,
                    color = Color(element.colorHex),
                    lineHeight = (scaledFontSize * 1.35f).sp
                ),
                cursorBrush = SolidColor(Color(0xFF2563EB)),
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester)
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            if (keyEvent.key == Key.Escape) {
                                onCommit(textValue.text)
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
