package com.lkboard.app.clipboard

import com.lkboard.app.data.BoardSerializer
import com.lkboard.app.model.BoardElement
import kotlinx.serialization.builtins.ListSerializer
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.Base64
import javax.imageio.ImageIO
import kotlin.math.min

/**
 * Типы данных, извлеченные из системного буфера обмена.
 */
sealed interface ClipboardContent {
    data class Image(val base64: String, val width: Float, val height: Float) : ClipboardContent
    data class Text(val text: String) : ClipboardContent
    data class Elements(val elements: List<BoardElement>) : ClipboardContent
}

/**
 * Сервис интеграции с системным буфером обмена (AWT Clipboard).
 * Поддерживает вставку скриншотов (Win+Shift+S), текста и объектов доски.
 */
object ClipboardManager {

    private var internalCopiedElements: List<BoardElement> = emptyList()

    /**
     * Копирует элементы в локальную память и системный буфер обмена.
     */
    fun copyElements(elements: List<BoardElement>) {
        if (elements.isEmpty()) return
        internalCopiedElements = elements

        try {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            val json = BoardSerializer.jsonFormat.encodeToString(
                ListSerializer(BoardElement.serializer()),
                elements
            )
            clipboard.setContents(StringSelection(json), null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Копирует растровое изображение напрямую в системный буфер обмена для быстрой вставки в мессенджеры.
     */
    fun copyImageToClipboard(image: BufferedImage) {
        try {
            val transferable = object : java.awt.datatransfer.Transferable {
                override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(DataFlavor.imageFlavor)
                override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = flavor == DataFlavor.imageFlavor
                override fun getTransferData(flavor: DataFlavor): Any {
                    if (flavor == DataFlavor.imageFlavor) return image
                    throw java.awt.datatransfer.UnsupportedFlavorException(flavor)
                }
            }
            Toolkit.getDefaultToolkit().systemClipboard.setContents(transferable, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Читает системный буфер обмена с приоритетом: Image -> Скопированные элементы -> Обычный текст.
     */
    fun readFromClipboard(): ClipboardContent? {
        try {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard

            // 1. Проверяем наличие растрового изображения (скриншот, веб-картинка)
            if (clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor)) {
                val image = clipboard.getData(DataFlavor.imageFlavor) as? java.awt.Image
                if (image != null) {
                    val bufferedImage = toBufferedImage(image)
                    val baos = ByteArrayOutputStream()
                    ImageIO.write(bufferedImage, "png", baos)
                    val bytes = baos.toByteArray()
                    val base64 = Base64.getEncoder().encodeToString(bytes)

                    // Разумное авто-масштабирование для больших экранов
                    val origW = bufferedImage.width.toFloat().coerceAtLeast(30f)
                    val origH = bufferedImage.height.toFloat().coerceAtLeast(30f)
                    val maxDimension = 720f
                    val scaleFactor = if (origW > maxDimension || origH > maxDimension) {
                        min(maxDimension / origW, maxDimension / origH)
                    } else 1.0f

                    return ClipboardContent.Image(
                        base64 = base64,
                        width = origW * scaleFactor,
                        height = origH * scaleFactor
                    )
                }
            }

            // 2. Если в текущей сессии скопированы элементы доски
            if (internalCopiedElements.isNotEmpty()) {
                return ClipboardContent.Elements(internalCopiedElements)
            }

            // 3. Текст
            if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                val text = clipboard.getData(DataFlavor.stringFlavor) as? String
                if (!text.isNullOrBlank()) {
                    // Проверяем, не является ли текст сериализованным списком элементов
                    try {
                        val parsed = BoardSerializer.jsonFormat.decodeFromString(
                            ListSerializer(BoardElement.serializer()),
                            text
                        )
                        if (parsed.isNotEmpty()) {
                            return ClipboardContent.Elements(parsed)
                        }
                    } catch (_: Exception) {
                        // Обычный пользовательский текст
                    }
                    return ClipboardContent.Text(text)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun toBufferedImage(img: java.awt.Image): BufferedImage {
        if (img is BufferedImage) return img
        val width = img.getWidth(null).coerceAtLeast(1)
        val height = img.getHeight(null).coerceAtLeast(1)
        val bi = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g2 = bi.createGraphics()
        g2.drawImage(img, 0, 0, null)
        g2.dispose()
        return bi
    }
}
