package com.lkboard.app.export

import com.lkboard.app.model.BoardState
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import java.io.File

/**
 * Сервис экспорта доски Lkboard в документ PDF с помощью Apache PDFBox.
 */
object PdfExporter {

    fun exportToPdf(targetFile: File, state: BoardState): Boolean {
        val bufferedImage = PngExporter.renderBoardToBufferedImage(state) ?: return false
        val width = bufferedImage.width.toFloat()
        val height = bufferedImage.height.toFloat()

        val document = PDDocument()
        return try {
            val page = PDPage(PDRectangle(width, height))
            document.addPage(page)

            val pdImage = LosslessFactory.createFromImage(document, bufferedImage)
            PDPageContentStream(document, page).use { cs ->
                cs.drawImage(pdImage, 0f, 0f, width, height)
            }

            document.save(targetFile)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            document.close()
        }
    }
}
