package com.lkboard.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.skia.Image
import java.io.File

/**
 * Сервис управления логотипом приложения Lkboard:
 * - Загружает оригинальный Logo.png из ресурсов или локального каталога.
 * - Предоставляет Painter для окна приложения (Window icon) и Compose UI компонентов.
 * - Имеет безопасный векторный fallback в цветах бренда (Black & Liquid Orange).
 */
object AppLogo {

    private var cachedBitmap: ImageBitmap? = null

    fun getLogoBitmap(): ImageBitmap? {
        if (cachedBitmap != null) return cachedBitmap
        return try {
            val bytes = Thread.currentThread().contextClassLoader.getResourceAsStream("logo.png")?.readBytes()
                ?: File("Logo.png").takeIf { it.exists() }?.readBytes()
                ?: File("src/jvmMain/resources/logo.png").takeIf { it.exists() }?.readBytes()
            if (bytes != null) {
                val bitmap = Image.makeFromEncoded(bytes).toComposeImageBitmap()
                cachedBitmap = bitmap
                bitmap
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @Composable
    fun rememberLogoPainter(): Painter? {
        val bitmap = remember { getLogoBitmap() }
        return remember(bitmap) {
            bitmap?.let { BitmapPainter(it) }
        }
    }

    @Composable
    fun LogoIcon(
        size: Dp = 32.dp,
        modifier: Modifier = Modifier
    ) {
        val painter = rememberLogoPainter()
        if (painter != null) {
            Image(
                painter = painter,
                contentDescription = "LKBoard Logo",
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(8.dp))
            )
        } else {
            // Элегантный fallback бейдж в цветах бренда
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF1E1E1E), Color(0xFFFF7A00))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "LK",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = (size.value * 0.45f).sp
                )
            }
        }
    }
}
