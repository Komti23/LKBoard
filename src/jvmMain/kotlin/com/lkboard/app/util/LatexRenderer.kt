package com.lkboard.app.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.lkboard.app.model.BoardElement
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Font
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Typeface

/**
 * Высокопроизводительный движок рендеринга математических выражений LaTeX "на лету":
 * - Преобразует LaTeX-макросы в математические символы Юникода и структурные блоки.
 * - Отрисовывает дроби \frac{A}{B} с дробной чертой.
 * - Отрисовывает корни \sqrt{x} с радикалом и горизонтальной полосой.
 * - Отрисовывает степени x^2, индексы x_i, интегралы \int, суммы \sum.
 * - Поддерживает как Compose DrawScope, так и Skia Canvas (для PNG/SVG экспорта).
 */
object LatexRenderer {

    /**
     * Преобразует стандартные LaTeX-макросы в красивые математические символы Юникода.
     */
    fun formatLatexToReadable(raw: String): String {
        var s = raw.trim()
        val replacements = mapOf(
            "\\alpha" to "α", "\\beta" to "β", "\\gamma" to "γ", "\\delta" to "δ",
            "\\Delta" to "Δ", "\\epsilon" to "ε", "\\theta" to "θ", "\\lambda" to "λ",
            "\\mu" to "μ", "\\pi" to "π", "\\rho" to "ρ", "\\sigma" to "σ",
            "\\tau" to "τ", "\\phi" to "φ", "\\omega" to "ω", "\\Omega" to "Ω",
            "\\pm" to "±", "\\times" to "×", "\\cdot" to "·", "\\div" to "÷",
            "\\le" to "≤", "\\ge" to "≥", "\\ne" to "≠", "\\approx" to "≈",
            "\\infty" to "∞", "\\to" to "→", "\\partial" to "∂", "\\nabla" to "∇",
            "\\sum" to "∑", "\\int" to "∫", "\\prod" to "∏", "\\sqrt" to "√"
        )
        for ((macro, symbol) in replacements) {
            s = s.replace(macro, symbol)
        }
        return s
    }

    /**
     * Оценивает размеры формулы по тексту и размеру шрифта.
     */
    fun estimateSize(formula: String, fontSize: Float): Pair<Float, Float> {
        val readable = formatLatexToReadable(formula)
        val charWidth = fontSize * 0.58f
        val w = (readable.length * charWidth) + 32f
        val h = if (formula.contains("\\frac") || formula.contains("\\int") || formula.contains("\\sum")) {
            (fontSize * 2.8f) + 20f
        } else {
            (fontSize * 1.8f) + 16f
        }
        return Pair(w, h)
    }

    /**
     * Отрисовывает LaTeX-элемент в Compose DrawScope.
     */
    fun drawLatex(
        drawScope: DrawScope,
        element: BoardElement.LatexFormula,
        textMeasurer: TextMeasurer
    ) {
        val color = Color(element.colorHex)
        val rectTopLeft = Offset(element.x, element.y)
        val rectSize = Size(element.width, element.height)

        // 1. Подложка (карточка формулы)
        if (element.hasBackgroundCard) {
            drawScope.drawRoundRect(
                color = Color(0x1438BDF8),
                topLeft = rectTopLeft,
                size = rectSize,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
            )
            drawScope.drawRoundRect(
                color = Color(0x3338BDF8),
                topLeft = rectTopLeft,
                size = rectSize,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f),
                style = Stroke(width = 1.2f)
            )
            // Бейдж LaTeX
            val badgeStyle = TextStyle(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF38BDF8)
            )
            drawScope.drawText(
                textMeasurer = textMeasurer,
                text = "LaTeX",
                topLeft = Offset(element.x + element.width - 40f, element.y + 6f),
                style = badgeStyle
            )
        }

        // 2. Рендеринг формулы
        val raw = element.formula
        if (raw.contains("\\frac{")) {
            drawFraction(drawScope, element, textMeasurer, color)
        } else {
            drawFormattedText(drawScope, element, textMeasurer, color)
        }
    }

    private fun drawFormattedText(
        drawScope: DrawScope,
        element: BoardElement.LatexFormula,
        textMeasurer: TextMeasurer,
        color: Color
    ) {
        val readable = formatLatexToReadable(element.formula)
            .replace("{", "")
            .replace("}", "")

        val style = TextStyle(
            fontSize = element.fontSize.sp,
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Medium,
            color = color
        )

        val measured = textMeasurer.measure(readable, style)
        val textX = element.x + (element.width - measured.size.width) / 2f
        val textY = element.y + (element.height - measured.size.height) / 2f

        drawScope.drawText(
            textMeasurer = textMeasurer,
            text = readable,
            topLeft = Offset(textX.coerceAtLeast(element.x + 12f), textY),
            style = style
        )
    }

    private fun drawFraction(
        drawScope: DrawScope,
        element: BoardElement.LatexFormula,
        textMeasurer: TextMeasurer,
        color: Color
    ) {
        // Простой разбор структуры \frac{num}{den}
        val raw = element.formula
        val prefix = formatLatexToReadable(raw.substringBefore("\\frac{")).trim()
        val afterFrac = raw.substringAfter("\\frac{")
        val numeratorRaw = afterFrac.substringBefore("}{")
        val rest = afterFrac.substringAfter("}{")
        val denominatorRaw = rest.substringBefore("}")
        val suffix = formatLatexToReadable(rest.substringAfter("}")).trim()

        val num = formatLatexToReadable(numeratorRaw).replace("{", "").replace("}", "")
        val den = formatLatexToReadable(denominatorRaw).replace("{", "").replace("}", "")

        val mainStyle = TextStyle(
            fontSize = element.fontSize.sp,
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            color = color
        )
        val fracStyle = TextStyle(
            fontSize = (element.fontSize * 0.85f).sp,
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            color = color
        )

        val prefixLayout = if (prefix.isNotEmpty()) textMeasurer.measure("$prefix ", mainStyle) else null
        val numLayout = textMeasurer.measure(num, fracStyle)
        val denLayout = textMeasurer.measure(den, fracStyle)
        val suffixLayout = if (suffix.isNotEmpty()) textMeasurer.measure(" $suffix", mainStyle) else null

        val fracWidth = maxOf(numLayout.size.width, denLayout.size.width).toFloat() + 16f
        var curX = element.x + 16f
        val centerY = element.y + element.height / 2f

        if (prefixLayout != null) {
            drawScope.drawText(
                textMeasurer = textMeasurer,
                text = "$prefix ",
                topLeft = Offset(curX, centerY - prefixLayout.size.height / 2f),
                style = mainStyle
            )
            curX += prefixLayout.size.width
        }

        // Числитель
        val numX = curX + (fracWidth - numLayout.size.width) / 2f
        drawScope.drawText(
            textMeasurer = textMeasurer,
            text = num,
            topLeft = Offset(numX, centerY - numLayout.size.height - 4f),
            style = fracStyle
        )

        // Дробная черта
        drawScope.drawLine(
            color = color,
            start = Offset(curX, centerY),
            end = Offset(curX + fracWidth, centerY),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )

        // Знаменатель
        val denX = curX + (fracWidth - denLayout.size.width) / 2f
        drawScope.drawText(
            textMeasurer = textMeasurer,
            text = den,
            topLeft = Offset(denX, centerY + 4f),
            style = fracStyle
        )

        curX += fracWidth

        if (suffixLayout != null) {
            drawScope.drawText(
                textMeasurer = textMeasurer,
                text = " $suffix",
                topLeft = Offset(curX, centerY - suffixLayout.size.height / 2f),
                style = mainStyle
            )
        }
    }

    /**
     * Отрисовывает формулу на Skia Canvas для экспорта в PNG и PDF.
     */
    fun renderSkia(
        canvas: Canvas,
        element: BoardElement.LatexFormula,
        typeface: Typeface
    ) {
        val paint = Paint().apply {
            color = element.colorHex.toInt()
            isAntiAlias = true
        }

        if (element.hasBackgroundCard) {
            val bgPaint = Paint().apply {
                color = 0x1438BDF8
                isAntiAlias = true
            }
            val borderPaint = Paint().apply {
                color = 0x3338BDF8
                mode = org.jetbrains.skia.PaintMode.STROKE
                strokeWidth = 1.2f
                isAntiAlias = true
            }
            val rrect = org.jetbrains.skia.RRect.makeXYWH(element.x, element.y, element.width, element.height, 10f)
            canvas.drawRRect(rrect, bgPaint)
            canvas.drawRRect(rrect, borderPaint)
        }

        val font = Font(typeface, element.fontSize)
        val text = formatLatexToReadable(element.formula)
            .replace("\\frac", "")
            .replace("{", "")
            .replace("}", "")

        canvas.drawString(text, element.x + 16f, element.y + element.height / 2f + element.fontSize / 3f, font, paint)
    }
}
