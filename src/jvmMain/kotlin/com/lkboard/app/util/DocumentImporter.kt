package com.lkboard.app.util

import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.poi.xslf.usermodel.XMLSlideShow
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.util.Base64
import javax.imageio.ImageIO
import kotlin.math.roundToInt

data class DocumentImportResult(
    val title: String,
    val pages: List<String>,
    val preferredWidth: Float,
    val preferredHeight: Float
)

/**
 * Сервис импорта презентаций (PowerPoint .pptx), документов PDF (.pdf) и изображений
 * во фрейм холста Lkboard с высокой четкостью рендеринга.
 */
object DocumentImporter {

    fun importFile(file: File): DocumentImportResult {
        val extension = file.extension.lowercase()
        return when (extension) {
            "pdf" -> importPdf(file)
            "pptx" -> importPptx(file)
            "png", "jpg", "jpeg", "bmp", "webp" -> importImage(file)
            else -> throw IllegalArgumentException("Неподдерживаемый формат файла: $extension. Поддерживаются PDF, PPTX и изображения.")
        }
    }

    private fun importPdf(file: File): DocumentImportResult {
        val document = Loader.loadPDF(file)
        val renderer = PDFRenderer(document)
        val pages = mutableListOf<String>()

        var firstPageW = 800f
        var firstPageH = 500f

        try {
            val pageCount = document.numberOfPages
            for (i in 0 until pageCount) {
                // Рендерим с плотностью 144 DPI для четкости текста на масштабируемом холсте
                val bim = renderer.renderImageWithDPI(i, 144f)
                if (i == 0) {
                    val ratio = bim.width.toFloat() / bim.height.toFloat()
                    firstPageW = 800f
                    firstPageH = 800f / ratio
                }
                pages.add(imageToBase64Png(bim))
            }
        } finally {
            document.close()
        }

        return DocumentImportResult(
            title = file.nameWithoutExtension,
            pages = pages,
            preferredWidth = firstPageW,
            preferredHeight = firstPageH
        )
    }

    private fun importPptx(file: File): DocumentImportResult {
        val fis = FileInputStream(file)
        val ppt = XMLSlideShow(fis)
        val pages = mutableListOf<String>()

        val slideSize = ppt.pageSize
        val scale = 1.5f
        val w = (slideSize.width * scale).roundToInt().coerceAtLeast(400)
        val h = (slideSize.height * scale).roundToInt().coerceAtLeast(300)

        try {
            for (slide in ppt.slides) {
                val img = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
                val g2d = img.createGraphics()
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
                g2d.scale(scale.toDouble(), scale.toDouble())
                g2d.paint = java.awt.Color.WHITE
                g2d.fill(java.awt.Rectangle(0, 0, slideSize.width, slideSize.height))
                slide.draw(g2d)
                g2d.dispose()
                pages.add(imageToBase64Png(img))
            }
        } finally {
            ppt.close()
            fis.close()
        }

        val ratio = w.toFloat() / h.toFloat()
        val preferredW = 860f
        val preferredH = preferredW / ratio

        return DocumentImportResult(
            title = file.nameWithoutExtension,
            pages = pages,
            preferredWidth = preferredW,
            preferredHeight = preferredH
        )
    }

    private fun importImage(file: File): DocumentImportResult {
        val img = ImageIO.read(file) ?: throw IllegalArgumentException("Не удалось прочитать изображение")
        val base64 = imageToBase64Png(img)
        val ratio = img.width.toFloat() / img.height.toFloat()
        val preferredW = 750f
        val preferredH = preferredW / ratio

        return DocumentImportResult(
            title = file.nameWithoutExtension,
            pages = listOf(base64),
            preferredWidth = preferredW,
            preferredHeight = preferredH
        )
    }

    private fun imageToBase64Png(image: BufferedImage): String {
        val baos = ByteArrayOutputStream()
        ImageIO.write(image, "PNG", baos)
        return Base64.getEncoder().encodeToString(baos.toByteArray())
    }
}
