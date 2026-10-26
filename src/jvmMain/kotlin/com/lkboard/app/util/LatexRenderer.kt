package com.lkboard.app.util

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
 * - Преобразует LaTeX-макросы в математические символы Юникода, верхние/нижние индексы.
 * - Поддерживает вложенные скобки в дробях \frac{num}{den} и корнях \sqrt{x}.
 * - Полностью защищен от ошибок переполнения координат (все тексты измеряются до отрисовки).
 * - Поддерживает как Compose DrawScope, так и Skia Canvas (для экспорта в PNG/SVG).
 */
object LatexRenderer {

    private val GREEK_AND_SYMBOLS = listOf(
        "\\alpha" to "α", "\\beta" to "β", "\\gamma" to "γ", "\\delta" to "δ",
        "\\Delta" to "Δ", "\\epsilon" to "ε", "\\varepsilon" to "ε", "\\zeta" to "ζ",
        "\\eta" to "η", "\\theta" to "θ", "\\vartheta" to "θ", "\\iota" to "ι",
        "\\kappa" to "κ", "\\lambda" to "λ", "\\Lambda" to "Λ", "\\mu" to "μ",
        "\\nu" to "ν", "\\xi" to "ξ", "\\pi" to "π", "\\Pi" to "Π",
        "\\rho" to "ρ", "\\sigma" to "σ", "\\Sigma" to "Σ", "\\tau" to "τ",
        "\\upsilon" to "υ", "\\phi" to "φ", "\\varphi" to "φ", "\\chi" to "χ",
        "\\psi" to "ψ", "\\Psi" to "Ψ", "\\omega" to "ω", "\\Omega" to "Ω",
        "\\pm" to "±", "\\mp" to "∓", "\\times" to "×", "\\cdot" to "·",
        "\\div" to "÷", "\\le" to "≤", "\\leq" to "≤", "\\ge" to "≥",
        "\\geq" to "≥", "\\ne" to "≠", "\\neq" to "≠", "\\approx" to "≈",
        "\\equiv" to "≡", "\\infty" to "∞", "\\to" to "→", "\\rightarrow" to "→",
        "\\leftarrow" to "←", "\\Rightarrow" to "⇒", "\\Leftarrow" to "⇐",
        "\\partial" to "∂", "\\nabla" to "∇", "\\sum" to "∑", "\\int" to "∫",
        "\\prod" to "∏", "\\sqrt" to "√", "\\forall" to "∀", "\\exists" to "∃",
        "\\in" to "∈", "\\notin" to "∉", "\\subset" to "⊂", "\\subseteq" to "⊆",
        "\\cup" to "∪", "\\cap" to "∩", "\\vec" to ""
    )

    private val SUBSCRIPT_MAP = mapOf(
        '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄',
        '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
        '+' to '₊', '-' to '₋', '=' to '₌', '(' to '₍', ')' to '₎',
        'a' to 'ₐ', 'e' to 'ₑ', 'h' to 'ₕ', 'i' to 'ᵢ', 'j' to 'ⱼ',
        'k' to 'ₖ', 'l' to 'ₗ', 'm' to 'ₘ', 'n' to 'ₙ', 'o' to 'ₒ',
        'p' to 'ₚ', 'r' to 'ᵣ', 's' to 'ₛ', 't' to 'ₜ', 'u' to 'ᵤ',
        'v' to 'ᵥ', 'x' to 'ₓ'
    )

    private val SUPERSCRIPT_MAP = mapOf(
        '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
        '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
        '+' to '⁺', '-' to '⁻', '=' to '⁼', '(' to '⁽', ')' to '⁾',
        'a' to 'ᵃ', 'b' to 'ᵇ', 'c' to 'ᶜ', 'd' to 'ᵈ', 'e' to 'ᵉ',
        'i' to 'ⁱ', 'm' to 'ᵐ', 'n' to 'ⁿ', 't' to 'ᵗ', 'x' to 'ˣ',
        'y' to 'ʸ'
    )

    /**
     * Извлекает содержимое сбалансированных фигурных скобок `{ ... }`.
     * Возвращает пару (содержимое, индекс_после_закрывающей_скобки) или null при ошибке.
     */
    fun extractBalancedBraces(str: String, startIndex: Int): Pair<String, Int>? {
        val openIdx = str.indexOf('{', startIndex)
        if (openIdx < 0) return null
        var depth = 0
        for (i in openIdx until str.length) {
            when (str[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        val content = str.substring(openIdx + 1, i)
                        return Pair(content, i + 1)
                    }
                }
            }
        }
        return null
    }

    /**
     * Преобразует строку в верхний индекс (Юникод).
     */
    fun toSuperscript(s: String): String {
        val trimmed = s.trim()
        if (trimmed == "∞" || trimmed == "\\infty") return "^∞"
        val sb = StringBuilder()
        for (ch in trimmed) {
            sb.append(SUPERSCRIPT_MAP[ch] ?: ch)
        }
        return sb.toString()
    }

    /**
     * Преобразует строку в нижний индекс (Юникод).
     */
    fun toSubscript(s: String): String {
        val trimmed = s.trim()
        val sb = StringBuilder()
        for (ch in trimmed) {
            sb.append(SUBSCRIPT_MAP[ch] ?: ch)
        }
        return sb.toString()
    }

    /**
     * Преобразует стандартные LaTeX-макросы в красивые математические символы Юникода.
     * Полностью обрабатывает дроби \frac{A}{B}, корни \sqrt{x}, степени ^ и индексы _.
     */
    fun formatLatexToReadable(raw: String): String {
        var s = raw.trim()

        // 1. Обработка дробей \frac{num}{den} -> (num) / (den) или num / den
        while (s.contains("\\frac")) {
            val fracIdx = s.indexOf("\\frac")
            val numPair = extractBalancedBraces(s, fracIdx + 5)
            if (numPair != null) {
                val denPair = extractBalancedBraces(s, numPair.second)
                if (denPair != null) {
                    val numFormatted = formatLatexToReadable(numPair.first)
                    val denFormatted = formatLatexToReadable(denPair.first)
                    val before = s.substring(0, fracIdx).trimEnd()
                    val after = s.substring(denPair.second).trimStart()
                    val fracStr = if (denFormatted.length <= 3 && !numFormatted.contains(" ")) {
                        "$numFormatted / $denFormatted"
                    } else {
                        "($numFormatted) / ($denFormatted)"
                    }
                    s = if (before.isEmpty()) "$fracStr $after".trim() else "$before $fracStr $after".trim()
                    continue
                }
            }
            break
        }

        // 2. Обработка корней \sqrt{inner} -> √(inner)
        while (s.contains("\\sqrt")) {
            val sqrtIdx = s.indexOf("\\sqrt")
            val innerPair = extractBalancedBraces(s, sqrtIdx + 5)
            if (innerPair != null) {
                val innerFormatted = formatLatexToReadable(innerPair.first)
                val before = s.substring(0, sqrtIdx).trimEnd()
                val after = s.substring(innerPair.second).trimStart()
                val sqrtStr = "√($innerFormatted)"
                s = if (before.isEmpty()) "$sqrtStr $after".trim() else "$before $sqrtStr $after".trim()
                continue
            }
            break
        }

        // 3. Замена греческих символов и операторов
        for ((macro, symbol) in GREEK_AND_SYMBOLS) {
            s = s.replace(macro, symbol)
        }

        // 4. Замена индексов _{xyz} -> ₓᵧ𝓏
        val subRegex = Regex("""_\{([^}]+)\}""")
        s = subRegex.replace(s) { match ->
            toSubscript(match.groupValues[1])
        }
        val singleSubRegex = Regex("""_([0-9a-zA-Z])""")
        s = singleSubRegex.replace(s) { match ->
            toSubscript(match.groupValues[1])
        }

        // 5. Замена степеней ^{xyz} -> ˣʸᶻ
        val supRegex = Regex("""\^\{([^}]+)\}""")
        s = supRegex.replace(s) { match ->
            toSuperscript(match.groupValues[1])
        }
        val singleSupRegex = Regex("""\^([0-9a-zA-Z])""")
        s = singleSupRegex.replace(s) { match ->
            toSuperscript(match.groupValues[1])
        }

        // Очистка оставшихся скобок разметки
        return s.replace("{", "").replace("}", "").trim()
    }

    /**
     * Оценивает размеры формулы по тексту и размеру шрифта.
     */
    fun estimateSize(formula: String, fontSize: Float): Pair<Float, Float> {
        val readable = formatLatexToReadable(formula)
        val charWidth = fontSize * 0.60f
        val w = (readable.length * charWidth) + 40f
        val h = if (formula.contains("\\frac") || formula.contains("\\int") || formula.contains("\\sum")) {
            (fontSize * 2.8f) + 24f
        } else {
            (fontSize * 1.8f) + 18f
        }
        return Pair(maxOf(160f, w), maxOf(60f, h))
    }

    /**
     * Отрисовывает LaTeX-элемент в Compose DrawScope.
     * Абсолютно безопасен: все тексты измеряются до отрисовки и никогда не падают из-за отрицательного maxWidth.
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
                color = Color(0x142563EB),
                topLeft = rectTopLeft,
                size = rectSize,
                cornerRadius = CornerRadius(12f, 12f)
            )
            drawScope.drawRoundRect(
                color = Color(0x332563EB),
                topLeft = rectTopLeft,
                size = rectSize,
                cornerRadius = CornerRadius(12f, 12f),
                style = Stroke(width = 1.2f)
            )
            // Бейдж LaTeX
            val badgeStyle = TextStyle(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF2563EB)
            )
            val badgeLayout = textMeasurer.measure(text = "LaTeX", style = badgeStyle)
            drawScope.drawText(
                textLayoutResult = badgeLayout,
                topLeft = Offset(element.x + element.width - badgeLayout.size.width - 12f, element.y + 6f)
            )
        }

        // 2. Рендеринг формулы
        val raw = element.formula
        if (raw.contains("\\frac")) {
            drawFractionFormula(drawScope, element, textMeasurer, color)
        } else {
            drawStandardFormula(drawScope, element, textMeasurer, color)
        }
    }

    private fun drawStandardFormula(
        drawScope: DrawScope,
        element: BoardElement.LatexFormula,
        textMeasurer: TextMeasurer,
        color: Color
    ) {
        val readable = formatLatexToReadable(element.formula)

        val style = TextStyle(
            fontSize = element.fontSize.sp,
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Medium,
            color = color
        )

        val layout = textMeasurer.measure(readable, style)
        val textX = element.x + (element.width - layout.size.width) / 2f
        val textY = element.y + (element.height - layout.size.height) / 2f

        drawScope.drawText(
            textLayoutResult = layout,
            topLeft = Offset(maxOf(element.x + 12f, textX), textY)
        )
    }

    private fun drawFractionFormula(
        drawScope: DrawScope,
        element: BoardElement.LatexFormula,
        textMeasurer: TextMeasurer,
        color: Color
    ) {
        val raw = element.formula
        val fracIdx = raw.indexOf("\\frac")
        val numPair = if (fracIdx >= 0) extractBalancedBraces(raw, fracIdx + 5) else null
        val denPair = if (numPair != null) extractBalancedBraces(raw, numPair.second) else null

        if (numPair == null || denPair == null) {
            // Если структура дроби не распарсилась сбалансированно, рисуем читаемый текст
            drawStandardFormula(drawScope, element, textMeasurer, color)
            return
        }

        val prefixRaw = raw.substring(0, fracIdx).trim()
        val suffixRaw = raw.substring(denPair.second).trim()

        val prefixText = if (prefixRaw.isNotEmpty()) "${formatLatexToReadable(prefixRaw)} " else ""
        val suffixText = if (suffixRaw.isNotEmpty()) " ${formatLatexToReadable(suffixRaw)}" else ""
        val numText = formatLatexToReadable(numPair.first)
        val denText = formatLatexToReadable(denPair.first)

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

        val prefixLayout = if (prefixText.isNotEmpty()) textMeasurer.measure(prefixText, mainStyle) else null
        val suffixLayout = if (suffixText.isNotEmpty()) textMeasurer.measure(suffixText, mainStyle) else null
        val numLayout = textMeasurer.measure(numText, fracStyle)
        val denLayout = textMeasurer.measure(denText, fracStyle)

        val prefixW = prefixLayout?.size?.width?.toFloat() ?: 0f
        val suffixW = suffixLayout?.size?.width?.toFloat() ?: 0f
        val fracW = maxOf(numLayout.size.width, denLayout.size.width).toFloat() + 16f
        val totalW = prefixW + fracW + suffixW

        val startX = element.x + (element.width - totalW) / 2f
        var curX = maxOf(element.x + 12f, startX)
        val centerY = element.y + element.height / 2f

        // 1. Префикс (например: \int_{0}^{\infty} e^{-x^2} dx =)
        if (prefixLayout != null) {
            drawScope.drawText(
                textLayoutResult = prefixLayout,
                topLeft = Offset(curX, centerY - prefixLayout.size.height / 2f)
            )
            curX += prefixW
        }

        // 2. Дробь (числитель, черта, знаменатель)
        val numX = curX + (fracW - numLayout.size.width) / 2f
        drawScope.drawText(
            textLayoutResult = numLayout,
            topLeft = Offset(numX, centerY - numLayout.size.height - 3f)
        )

        drawScope.drawLine(
            color = color,
            start = Offset(curX, centerY),
            end = Offset(curX + fracW, centerY),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )

        val denX = curX + (fracW - denLayout.size.width) / 2f
        drawScope.drawText(
            textLayoutResult = denLayout,
            topLeft = Offset(denX, centerY + 3f)
        )

        curX += fracW

        // 3. Суффикс (если есть)
        if (suffixLayout != null) {
            drawScope.drawText(
                textLayoutResult = suffixLayout,
                topLeft = Offset(curX, centerY - suffixLayout.size.height / 2f)
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
                color = 0x142563EB
                isAntiAlias = true
            }
            val borderPaint = Paint().apply {
                color = 0x332563EB
                mode = org.jetbrains.skia.PaintMode.STROKE
                strokeWidth = 1.2f
                isAntiAlias = true
            }
            val rrect = org.jetbrains.skia.RRect.makeXYWH(element.x, element.y, element.width, element.height, 12f)
            canvas.drawRRect(rrect, bgPaint)
            canvas.drawRRect(rrect, borderPaint)
        }

        val font = Font(typeface, element.fontSize)
        val readable = formatLatexToReadable(element.formula)

        canvas.drawString(readable, element.x + 16f, element.y + element.height / 2f + element.fontSize / 3f, font, paint)
    }
}
