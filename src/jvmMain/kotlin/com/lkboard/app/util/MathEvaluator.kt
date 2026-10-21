package com.lkboard.app.util

import kotlin.math.*

/**
 * Быстрый математический парсер и вычислитель функций одной переменной f(x).
 * Поддерживает:
 * - Операторы: +, -, *, /, ^ (степень), %
 * - Неявное умножение: 2x, 3sin(x), (x+1)(x-2)
 * - Унарные знаки: -x, -(x+1), +x
 * - Функции: sin, cos, tan, asin, acos, atan, sinh, cosh, tanh, sqrt, cbrt, abs, exp, ln, log, floor, ceil
 * - Константы: pi, e
 */
object MathEvaluator {

    fun evaluate(expression: String, x: Double): Double? {
        val sanitized = preprocess(expression)
        return try {
            val tokens = tokenize(sanitized)
            val parser = Parser(tokens, x)
            val result = parser.parseExpression()
            if (result.isNaN() || result.isInfinite()) null else result
        } catch (_: Exception) {
            null
        }
    }

    fun sampleFunction(
        expression: String,
        xMin: Double,
        xMax: Double,
        steps: Int = 300
    ): List<Pair<Double, Double?>> {
        if (steps <= 1 || xMax <= xMin) return emptyList()
        val dx = (xMax - xMin) / steps
        val points = ArrayList<Pair<Double, Double?>>(steps + 1)
        var curX = xMin
        for (i in 0..steps) {
            val y = evaluate(expression, curX)
            points.add(curX to y)
            curX += dx
        }
        return points
    }

    private fun preprocess(expr: String): String {
        var s = expr.trim().lowercase()
        if (s.startsWith("y=") || s.startsWith("f(x)=") || s.startsWith("y =") || s.startsWith("f(x) =")) {
            s = s.substringAfter("=")
        }
        return s.trim()
    }

    private sealed interface Token {
        data class Number(val value: Double) : Token
        data class Variable(val name: String) : Token
        data class Func(val name: String) : Token
        data class Op(val char: Char) : Token
        data object OpenParen : Token
        data object CloseParen : Token
    }

    private val FUNCTION_NAMES = setOf(
        "sin", "cos", "tan", "asin", "acos", "atan",
        "sinh", "cosh", "tanh", "sqrt", "cbrt", "abs",
        "exp", "ln", "log", "floor", "ceil"
    )

    private fun tokenize(input: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < input.length) {
            val c = input[i]
            when {
                c.isWhitespace() -> i++

                c.isDigit() || (c == '.' && i + 1 < input.length && input[i + 1].isDigit()) -> {
                    val start = i
                    while (i < input.length && (input[i].isDigit() || input[i] == '.')) {
                        i++
                    }
                    val numStr = input.substring(start, i)
                    tokens.add(Token.Number(numStr.toDoubleOrNull() ?: 0.0))
                }

                c.isLetter() -> {
                    val start = i
                    while (i < input.length && (input[i].isLetter() || input[i].isDigit())) {
                        i++
                    }
                    val word = input.substring(start, i)
                    when (word) {
                        "pi" -> tokens.add(Token.Number(PI))
                        "e" -> tokens.add(Token.Number(E))
                        "x" -> tokens.add(Token.Variable("x"))
                        in FUNCTION_NAMES -> tokens.add(Token.Func(word))
                        else -> tokens.add(Token.Variable(word))
                    }
                }

                c in "+-*/^%" -> {
                    tokens.add(Token.Op(c))
                    i++
                }

                c == '(' -> {
                    tokens.add(Token.OpenParen)
                    i++
                }

                c == ')' -> {
                    tokens.add(Token.CloseParen)
                    i++
                }

                else -> i++
            }
        }

        // Вставка неявного умножения: e.g. 2x -> 2 * x, (x+1)(x-1) -> (x+1) * (x-1)
        val expanded = mutableListOf<Token>()
        for (idx in tokens.indices) {
            expanded.add(tokens[idx])
            if (idx + 1 < tokens.size) {
                val cur = tokens[idx]
                val next = tokens[idx + 1]
                val needsMult = (cur is Token.Number || cur is Token.Variable || cur is Token.CloseParen) &&
                        (next is Token.Variable || next is Token.Func || next is Token.OpenParen)
                if (needsMult) {
                    expanded.add(Token.Op('*'))
                }
            }
        }

        return expanded
    }

    private class Parser(
        private val tokens: List<Token>,
        private val xVal: Double
    ) {
        private var pos = 0

        private fun peek(): Token? = if (pos < tokens.size) tokens[pos] else null
        private fun consume(): Token = tokens[pos++]

        fun parseExpression(): Double {
            return parseAddSub()
        }

        private fun parseAddSub(): Double {
            var result = parseMulDiv()
            while (true) {
                val token = peek() as? Token.Op ?: break
                if (token.char != '+' && token.char != '-') break
                consume()
                val right = parseMulDiv()
                result = if (token.char == '+') result + right else result - right
            }
            return result
        }

        private fun parseMulDiv(): Double {
            var result = parsePower()
            while (true) {
                val token = peek() as? Token.Op ?: break
                if (token.char != '*' && token.char != '/' && token.char != '%') break
                consume()
                val right = parsePower()
                result = when (token.char) {
                    '*' -> result * right
                    '/' -> if (right == 0.0) Double.NaN else result / right
                    '%' -> result % right
                    else -> result
                }
            }
            return result
        }

        private fun parsePower(): Double {
            val left = parseUnary()
            val token = peek() as? Token.Op
            if (token != null && token.char == '^') {
                consume()
                val right = parsePower() // правоассоциативный
                return left.pow(right)
            }
            return left
        }

        private fun parseUnary(): Double {
            val token = peek()
            if (token is Token.Op) {
                if (token.char == '-') {
                    consume()
                    return -parseUnary()
                }
                if (token.char == '+') {
                    consume()
                    return parseUnary()
                }
            }
            return parsePrimary()
        }

        private fun parsePrimary(): Double {
            return when (val token = consume()) {
                is Token.Number -> token.value
                is Token.Variable -> if (token.name == "x") xVal else Double.NaN
                is Token.Func -> {
                    val arg = if (peek() is Token.OpenParen) {
                        consume() // '('
                        val value = parseExpression()
                        if (peek() is Token.CloseParen) consume() // ')'
                        value
                    } else {
                        parseUnary()
                    }
                    applyFunction(token.name, arg)
                }

                is Token.OpenParen -> {
                    val value = parseExpression()
                    if (peek() is Token.CloseParen) consume()
                    value
                }

                else -> Double.NaN
            }
        }

        private fun applyFunction(name: String, arg: Double): Double {
            return when (name) {
                "sin" -> sin(arg)
                "cos" -> cos(arg)
                "tan" -> tan(arg)
                "asin" -> asin(arg)
                "acos" -> acos(arg)
                "atan" -> atan(arg)
                "sinh" -> sinh(arg)
                "cosh" -> cosh(arg)
                "tanh" -> tanh(arg)
                "sqrt" -> if (arg >= 0) sqrt(arg) else Double.NaN
                "cbrt" -> cbrt(arg)
                "abs" -> abs(arg)
                "exp" -> exp(arg)
                "ln" -> if (arg > 0) ln(arg) else Double.NaN
                "log" -> if (arg > 0) log10(arg) else Double.NaN
                "floor" -> floor(arg)
                "ceil" -> ceil(arg)
                else -> Double.NaN
            }
        }
    }
}
