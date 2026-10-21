package com.lkboard.app.model

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.serialization.Serializable

/**
 * Стиль обводки геометрической фигуры.
 */
@Serializable
enum class StrokeStyle {
    SOLID,   // Сплошная линия
    DASHED,  // Пунктирная линия
    DOTTED   // Точечная линия
}

/**
 * Пользовательские параметры создания геометрических фигур.
 */
@Serializable
data class ShapeSettings(
    val shapeType: ShapeType = ShapeType.ROUNDED_RECTANGLE,
    val strokeColorHex: Long = 0xFF1E1E1EL,
    val fillColorHex: Long? = null, // null = прозрачная заливка
    val strokeWidth: Float = 2.5f,
    val strokeStyle: StrokeStyle = StrokeStyle.SOLID,
    val cornerRadius: Float = 12f
) {
    companion object {
        // Расширенная палитра контура (обводки): 14 гармоничных цветов
        val PRESET_STROKE_COLORS = listOf(
            0xFF1E1E1EL, // Черный
            0xFF64748BL, // Графитовый
            0xFF2563EBL, // Синий
            0xFF0284C7L, // Голубой
            0xFF0D9488L, // Бирюзовый
            0xFF16A34AL, // Зеленый
            0xFF059669L, // Изумрудный
            0xFFDC2626L, // Красный
            0xFFE11D48L, // Малиновый
            0xFFDB2777L, // Розовый
            0xFFEA580CL, // Оранжевый
            0xFFEAB308L, // Янтарный
            0xFFFACC15L, // Желтый
            0xFF9333EAL  // Фиолетовый
        )

        // Расширенная палитра заливки (первый элемент null — прозрачный / без заливки)
        val PRESET_FILL_COLORS = listOf(
            null,         // Без заливки (прозрачный)
            0x20000000L,  // Полупрозрачный нейтральный серый
            0x262563EBL,  // Полупрозрачный синий
            0x260284C7L,  // Полупрозрачный голубой
            0x260D9488L,  // Полупрозрачный бирюзовый
            0x2616A34AL,  // Полупрозрачный зеленый
            0x26059669L,  // Полупрозрачный изумрудный
            0x26DC2626L,  // Полупрозрачный красный
            0x26E11D48L,  // Полупрозрачный малиновый
            0x26DB2777L,  // Полупрозрачный розовый
            0x26EA580CL,  // Полупрозрачный оранжевый
            0x33EAB308L,  // Полупрозрачный янтарный
            0x33FACC15L,  // Полупрозрачный желтый
            0x269333EAL   // Полупрозрачный фиолетовый
        )
    }
}

/**
 * Движок векторного рендеринга геометрических фигур на Skia/Compose Canvas.
 */
object ShapeRenderer {

    fun createStrokeStyle(width: Float, style: StrokeStyle): Stroke {
        val pathEffect = when (style) {
            StrokeStyle.SOLID -> null
            StrokeStyle.DASHED -> PathEffect.dashPathEffect(
                floatArrayOf(width * 3.5f, width * 2.2f),
                0f
            )
            StrokeStyle.DOTTED -> PathEffect.dashPathEffect(
                floatArrayOf(width * 0.8f, width * 2.4f),
                0f
            )
        }
        val cap = if (style == StrokeStyle.DOTTED) StrokeCap.Round else StrokeCap.Square
        return Stroke(
            width = width,
            pathEffect = pathEffect,
            cap = cap,
            join = StrokeJoin.Round
        )
    }

    fun drawShape(
        drawScope: DrawScope,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        shapeType: ShapeType,
        strokeColor: Color,
        fillColor: Color?,
        strokeWidth: Float,
        strokeStyle: StrokeStyle,
        cornerRadius: Float = 12f
    ) {
        val stroke = createStrokeStyle(strokeWidth, strokeStyle)

        when (shapeType) {
            ShapeType.RECTANGLE -> {
                if (fillColor != null && fillColor.alpha > 0f) {
                    drawScope.drawRect(
                        color = fillColor,
                        topLeft = Offset(x, y),
                        size = Size(width, height)
                    )
                }
                drawScope.drawRect(
                    color = strokeColor,
                    topLeft = Offset(x, y),
                    size = Size(width, height),
                    style = stroke
                )
            }

            ShapeType.ROUNDED_RECTANGLE -> {
                val cr = CornerRadius(cornerRadius, cornerRadius)
                if (fillColor != null && fillColor.alpha > 0f) {
                    drawScope.drawRoundRect(
                        color = fillColor,
                        topLeft = Offset(x, y),
                        size = Size(width, height),
                        cornerRadius = cr
                    )
                }
                drawScope.drawRoundRect(
                    color = strokeColor,
                    topLeft = Offset(x, y),
                    size = Size(width, height),
                    cornerRadius = cr,
                    style = stroke
                )
            }

            ShapeType.ELLIPSE -> {
                if (fillColor != null && fillColor.alpha > 0f) {
                    drawScope.drawOval(
                        color = fillColor,
                        topLeft = Offset(x, y),
                        size = Size(width, height)
                    )
                }
                drawScope.drawOval(
                    color = strokeColor,
                    topLeft = Offset(x, y),
                    size = Size(width, height),
                    style = stroke
                )
            }

            ShapeType.DIAMOND -> {
                val path = Path().apply {
                    moveTo(x + width / 2f, y)
                    lineTo(x + width, y + height / 2f)
                    lineTo(x + width / 2f, y + height)
                    lineTo(x, y + height / 2f)
                    close()
                }
                if (fillColor != null && fillColor.alpha > 0f) {
                    drawScope.drawPath(path = path, color = fillColor, style = Fill)
                }
                drawScope.drawPath(path = path, color = strokeColor, style = stroke)
            }

            ShapeType.TRIANGLE -> {
                val path = Path().apply {
                    moveTo(x + width / 2f, y)
                    lineTo(x + width, y + height)
                    lineTo(x, y + height)
                    close()
                }
                if (fillColor != null && fillColor.alpha > 0f) {
                    drawScope.drawPath(path = path, color = fillColor, style = Fill)
                }
                drawScope.drawPath(path = path, color = strokeColor, style = stroke)
            }

            ShapeType.ARROW -> {
                val startY = y + height / 2f
                val endY = y + height / 2f
                val headSize = (strokeWidth * 3.5f + 10f).coerceIn(12f, (width * 0.45f).coerceAtLeast(12f))
                val headHalfH = headSize * 0.6f

                // Линия стрелки
                drawScope.drawLine(
                    color = strokeColor,
                    start = Offset(x, startY),
                    end = Offset(x + width - headSize * 0.7f, endY),
                    strokeWidth = strokeWidth,
                    pathEffect = stroke.pathEffect,
                    cap = stroke.cap
                )

                // Заполненный наконечник
                val headPath = Path().apply {
                    moveTo(x + width, endY)
                    lineTo(x + width - headSize, endY - headHalfH)
                    lineTo(x + width - headSize, endY + headHalfH)
                    close()
                }
                drawScope.drawPath(path = headPath, color = strokeColor, style = Fill)
            }

            ShapeType.DOUBLE_ARROW -> {
                val midY = y + height / 2f
                val headSize = (strokeWidth * 3.5f + 10f).coerceIn(12f, (width * 0.28f).coerceAtLeast(12f))
                val headHalfH = headSize * 0.6f

                // Линия между двумя наконечниками
                drawScope.drawLine(
                    color = strokeColor,
                    start = Offset(x + headSize * 0.7f, midY),
                    end = Offset(x + width - headSize * 0.7f, midY),
                    strokeWidth = strokeWidth,
                    pathEffect = stroke.pathEffect,
                    cap = stroke.cap
                )

                // Левый наконечник
                val leftHead = Path().apply {
                    moveTo(x, midY)
                    lineTo(x + headSize, midY - headHalfH)
                    lineTo(x + headSize, midY + headHalfH)
                    close()
                }
                drawScope.drawPath(path = leftHead, color = strokeColor, style = Fill)

                // Правый наконечник
                val rightHead = Path().apply {
                    moveTo(x + width, midY)
                    lineTo(x + width - headSize, midY - headHalfH)
                    lineTo(x + width - headSize, midY + headHalfH)
                    close()
                }
                drawScope.drawPath(path = rightHead, color = strokeColor, style = Fill)
            }

            ShapeType.PARALLELOGRAM -> {
                val slant = (width * 0.22f).coerceAtMost(height * 0.8f)
                val path = Path().apply {
                    moveTo(x + slant, y)
                    lineTo(x + width, y)
                    lineTo(x + width - slant, y + height)
                    lineTo(x, y + height)
                    close()
                }
                if (fillColor != null && fillColor.alpha > 0f) {
                    drawScope.drawPath(path = path, color = fillColor, style = Fill)
                }
                drawScope.drawPath(path = path, color = strokeColor, style = stroke)
            }

            ShapeType.HEXAGON -> {
                val w4 = width * 0.25f
                val path = Path().apply {
                    moveTo(x + w4, y)
                    lineTo(x + width - w4, y)
                    lineTo(x + width, y + height / 2f)
                    lineTo(x + width - w4, y + height)
                    lineTo(x + w4, y + height)
                    lineTo(x, y + height / 2f)
                    close()
                }
                if (fillColor != null && fillColor.alpha > 0f) {
                    drawScope.drawPath(path = path, color = fillColor, style = Fill)
                }
                drawScope.drawPath(path = path, color = strokeColor, style = stroke)
            }

            ShapeType.STAR -> {
                val cx = x + width / 2f
                val cy = y + height / 2f
                val outerR = kotlin.math.min(width, height) / 2f
                val innerR = outerR * 0.40f
                val path = Path()
                for (i in 0 until 10) {
                    val angle = (i * 36 - 90) * (kotlin.math.PI / 180.0)
                    val r = if (i % 2 == 0) outerR else innerR
                    val px = (cx + r * kotlin.math.cos(angle)).toFloat()
                    val py = (cy + r * kotlin.math.sin(angle)).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                if (fillColor != null && fillColor.alpha > 0f) {
                    drawScope.drawPath(path = path, color = fillColor, style = Fill)
                }
                drawScope.drawPath(path = path, color = strokeColor, style = stroke)
            }

            ShapeType.SPEECH_BUBBLE -> {
                val tailH = (height * 0.22f).coerceIn(12f, 32f)
                val bodyH = height - tailH
                val cr = CornerRadius(cornerRadius.coerceAtMost(bodyH / 3f), cornerRadius.coerceAtMost(bodyH / 3f))
                val path = Path().apply {
                    addRoundRect(androidx.compose.ui.geometry.RoundRect(
                        left = x, top = y, right = x + width, bottom = y + bodyH,
                        topLeftCornerRadius = cr, topRightCornerRadius = cr,
                        bottomLeftCornerRadius = cr, bottomRightCornerRadius = cr
                    ))
                    moveTo(x + width * 0.22f, y + bodyH)
                    lineTo(x + width * 0.12f, y + height)
                    lineTo(x + width * 0.38f, y + bodyH)
                    close()
                }
                if (fillColor != null && fillColor.alpha > 0f) {
                    drawScope.drawPath(path = path, color = fillColor, style = Fill)
                }
                drawScope.drawPath(path = path, color = strokeColor, style = stroke)
            }

            ShapeType.CLOUD -> {
                val path = Path().apply {
                    moveTo(x + width * 0.20f, y + height * 0.70f)
                    cubicTo(x, y + height * 0.70f, x, y + height * 0.40f, x + width * 0.20f, y + height * 0.35f)
                    cubicTo(x + width * 0.15f, y + height * 0.12f, x + width * 0.45f, y + height * 0.05f, x + width * 0.50f, y + height * 0.25f)
                    cubicTo(x + width * 0.60f, y + height * 0.08f, x + width * 0.85f, y + height * 0.15f, x + width * 0.85f, y + height * 0.35f)
                    cubicTo(x + width, y + height * 0.40f, x + width, y + height * 0.70f, x + width * 0.80f, y + height * 0.70f)
                    cubicTo(x + width * 0.85f, y + height * 0.95f, x + width * 0.65f, y + height, x + width * 0.50f, y + height * 0.85f)
                    cubicTo(x + width * 0.35f, y + height, x + width * 0.15f, y + height * 0.95f, x + width * 0.20f, y + height * 0.70f)
                    close()
                }
                if (fillColor != null && fillColor.alpha > 0f) {
                    drawScope.drawPath(path = path, color = fillColor, style = Fill)
                }
                drawScope.drawPath(path = path, color = strokeColor, style = stroke)
            }

            ShapeType.CUBE_3D -> {
                val depth = kotlin.math.min(width, height) * 0.26f
                val frontW = width - depth
                val frontH = height - depth

                val baseColor = fillColor ?: Color(0x221E1E1E)
                val topColor = baseColor.copy(alpha = (baseColor.alpha * 1.35f).coerceAtMost(0.9f))
                val sideColor = Color(
                    red = (baseColor.red * 0.80f).coerceIn(0f, 1f),
                    green = (baseColor.green * 0.80f).coerceIn(0f, 1f),
                    blue = (baseColor.blue * 0.80f).coerceIn(0f, 1f),
                    alpha = (baseColor.alpha * 1.15f).coerceAtMost(0.9f)
                )

                // 1. Верхняя грань (Top Face)
                val topFace = Path().apply {
                    moveTo(x, y + depth)
                    lineTo(x + depth, y)
                    lineTo(x + width, y)
                    lineTo(x + frontW, y + depth)
                    close()
                }
                drawScope.drawPath(path = topFace, color = topColor, style = Fill)
                drawScope.drawPath(path = topFace, color = strokeColor, style = stroke)

                // 2. Боковая правая грань (Right Face)
                val rightFace = Path().apply {
                    moveTo(x + frontW, y + depth)
                    lineTo(x + width, y)
                    lineTo(x + width, y + frontH)
                    lineTo(x + frontW, y + height)
                    close()
                }
                drawScope.drawPath(path = rightFace, color = sideColor, style = Fill)
                drawScope.drawPath(path = rightFace, color = strokeColor, style = stroke)

                // 3. Передняя грань (Front Face)
                val frontRect = androidx.compose.ui.geometry.Rect(x, y + depth, x + frontW, y + height)
                drawScope.drawRect(color = baseColor, topLeft = Offset(frontRect.left, frontRect.top), size = Size(frontRect.width, frontRect.height), style = Fill)
                drawScope.drawRect(color = strokeColor, topLeft = Offset(frontRect.left, frontRect.top), size = Size(frontRect.width, frontRect.height), style = stroke)
            }

            ShapeType.CYLINDER_3D -> {
                val capH = (height * 0.22f).coerceIn(12f, 40f)
                val baseColor = fillColor ?: Color(0x221E1E1E)
                val topColor = baseColor.copy(alpha = (baseColor.alpha * 1.35f).coerceAtMost(0.9f))

                // Тело цилиндра
                val bodyPath = Path().apply {
                    moveTo(x, y + capH / 2f)
                    lineTo(x + width, y + capH / 2f)
                    lineTo(x + width, y + height - capH / 2f)
                    cubicTo(x + width, y + height + capH * 0.45f, x, y + height + capH * 0.45f, x, y + height - capH / 2f)
                    close()
                }
                drawScope.drawPath(path = bodyPath, color = baseColor, style = Fill)

                // Боковые образующие линии и нижняя дуга
                val outlineBody = Path().apply {
                    moveTo(x, y + capH / 2f)
                    lineTo(x, y + height - capH / 2f)
                    cubicTo(x, y + height + capH * 0.45f, x + width, y + height + capH * 0.45f, x + width, y + height - capH / 2f)
                    lineTo(x + width, y + capH / 2f)
                }
                drawScope.drawPath(path = outlineBody, color = strokeColor, style = stroke)

                // Верхний овал
                drawScope.drawOval(color = topColor, topLeft = Offset(x, y), size = Size(width, capH), style = Fill)
                drawScope.drawOval(color = strokeColor, topLeft = Offset(x, y), size = Size(width, capH), style = stroke)
            }

            ShapeType.CONE_3D -> {
                val capH = (height * 0.22f).coerceIn(12f, 36f)
                val apexX = x + width / 2f
                val apexY = y
                val baseColor = fillColor ?: Color(0x221E1E1E)

                // Тело конуса
                val conePath = Path().apply {
                    moveTo(apexX, apexY)
                    lineTo(x + width, y + height - capH / 2f)
                    cubicTo(x + width, y + height + capH * 0.45f, x, y + height + capH * 0.45f, x, y + height - capH / 2f)
                    close()
                }
                drawScope.drawPath(path = conePath, color = baseColor, style = Fill)

                // Контур
                val outline = Path().apply {
                    moveTo(apexX, apexY)
                    lineTo(x, y + height - capH / 2f)
                    cubicTo(x, y + height + capH * 0.45f, x + width, y + height + capH * 0.45f, x + width, y + height - capH / 2f)
                    lineTo(apexX, apexY)
                }
                drawScope.drawPath(path = outline, color = strokeColor, style = stroke)
            }

            ShapeType.PYRAMID_3D -> {
                val apexX = x + width * 0.48f
                val apexY = y
                val baseColor = fillColor ?: Color(0x221E1E1E)
                val leftColor = baseColor.copy(alpha = (baseColor.alpha * 1.25f).coerceAtMost(0.9f))
                val rightColor = Color(
                    red = (baseColor.red * 0.78f).coerceIn(0f, 1f),
                    green = (baseColor.green * 0.78f).coerceIn(0f, 1f),
                    blue = (baseColor.blue * 0.78f).coerceIn(0f, 1f),
                    alpha = (baseColor.alpha * 1.2f).coerceAtMost(0.9f)
                )

                val bottomMidX = x + width * 0.60f
                val bottomMidY = y + height

                // Левая освещенная грань
                val leftFace = Path().apply {
                    moveTo(apexX, apexY)
                    lineTo(x, y + height - 10f)
                    lineTo(bottomMidX, bottomMidY)
                    close()
                }
                drawScope.drawPath(path = leftFace, color = leftColor, style = Fill)
                drawScope.drawPath(path = leftFace, color = strokeColor, style = stroke)

                // Правая затененная грань
                val rightFace = Path().apply {
                    moveTo(apexX, apexY)
                    lineTo(bottomMidX, bottomMidY)
                    lineTo(x + width, y + height - 16f)
                    close()
                }
                drawScope.drawPath(path = rightFace, color = rightColor, style = Fill)
                drawScope.drawPath(path = rightFace, color = strokeColor, style = stroke)
            }

            ShapeType.SPHERE_3D -> {
                val sizeVal = kotlin.math.min(width, height)
                val left = x + (width - sizeVal) / 2f
                val top = y + (height - sizeVal) / 2f
                val baseColor = fillColor ?: Color(0x28007AFF)

                val brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.85f),
                        baseColor,
                        Color(
                            red = (baseColor.red * 0.6f).coerceIn(0f, 1f),
                            green = (baseColor.green * 0.6f).coerceIn(0f, 1f),
                            blue = (baseColor.blue * 0.6f).coerceIn(0f, 1f),
                            alpha = baseColor.alpha.coerceAtLeast(0.6f)
                        )
                    ),
                    center = Offset(left + sizeVal * 0.35f, top + sizeVal * 0.35f),
                    radius = sizeVal * 0.65f
                )
                drawScope.drawOval(brush = brush, topLeft = Offset(left, top), size = Size(sizeVal, sizeVal))
                drawScope.drawOval(color = strokeColor, topLeft = Offset(left, top), size = Size(sizeVal, sizeVal), style = stroke)
            }

            ShapeType.FRAME -> {
                drawScope.drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(x, y),
                    size = Size(width, height),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawScope.drawRoundRect(
                    color = strokeColor.copy(alpha = 0.5f),
                    topLeft = Offset(x, y),
                    size = Size(width, height),
                    cornerRadius = CornerRadius(6f, 6f),
                    style = stroke
                )
            }
        }
    }
}
