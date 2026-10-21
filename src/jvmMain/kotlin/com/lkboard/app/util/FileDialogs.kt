package com.lkboard.app.util

import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/**
 * Утилита вызова нативных диалоговых окон выбора файлов Windows (AWT FileDialog).
 */
object FileDialogs {

    fun openBoardFileDialog(parent: Frame? = null): File? {
        val dialog = FileDialog(parent, "Открыть доску Lkboard", FileDialog.LOAD).apply {
            setFilenameFilter { _, name -> name.endsWith(".lkboard", ignoreCase = true) }
            isVisible = true
        }
        val file = dialog.file ?: return null
        val dir = dialog.directory ?: return null
        return File(dir, file)
    }

    fun saveBoardFileDialog(parent: Frame? = null, defaultName: String = "доска.lkboard"): File? {
        val dialog = FileDialog(parent, "Сохранить доску Lkboard", FileDialog.SAVE).apply {
            this.file = if (defaultName.endsWith(".lkboard")) defaultName else "$defaultName.lkboard"
            isVisible = true
        }
        val file = dialog.file ?: return null
        val dir = dialog.directory ?: return null
        val finalName = if (file.endsWith(".lkboard", ignoreCase = true)) file else "$file.lkboard"
        return File(dir, finalName)
    }

    fun exportPngFileDialog(parent: Frame? = null, defaultName: String = "lkboard-export.png"): File? {
        val dialog = FileDialog(parent, "Экспорт доски в изображение PNG", FileDialog.SAVE).apply {
            this.file = if (defaultName.endsWith(".png")) defaultName else "$defaultName.png"
            isVisible = true
        }
        val file = dialog.file ?: return null
        val dir = dialog.directory ?: return null
        val finalName = if (file.endsWith(".png", ignoreCase = true)) file else "$file.png"
        return File(dir, finalName)
    }

    fun exportSvgFileDialog(parent: Frame? = null, defaultName: String = "lkboard-export.svg"): File? {
        val dialog = FileDialog(parent, "Экспорт доски в векторный SVG", FileDialog.SAVE).apply {
            this.file = if (defaultName.endsWith(".svg")) defaultName else "$defaultName.svg"
            isVisible = true
        }
        val file = dialog.file ?: return null
        val dir = dialog.directory ?: return null
        val finalName = if (file.endsWith(".svg", ignoreCase = true)) file else "$file.svg"
        return File(dir, finalName)
    }

    fun exportPdfFileDialog(parent: Frame? = null, defaultName: String = "lkboard-export.pdf"): File? {
        val dialog = FileDialog(parent, "Экспорт доски в PDF документ", FileDialog.SAVE).apply {
            this.file = if (defaultName.endsWith(".pdf")) defaultName else "$defaultName.pdf"
            isVisible = true
        }
        val file = dialog.file ?: return null
        val dir = dialog.directory ?: return null
        val finalName = if (file.endsWith(".pdf", ignoreCase = true)) file else "$file.pdf"
        return File(dir, finalName)
    }

    fun openDocumentFileDialog(parent: Frame? = null): File? {
        val dialog = FileDialog(parent, "Загрузить презентацию (PPTX) или PDF", FileDialog.LOAD).apply {
            setFilenameFilter { _, name ->
                val lower = name.lowercase()
                lower.endsWith(".pdf") || lower.endsWith(".pptx") ||
                        lower.endsWith(".png") || lower.endsWith(".jpg") ||
                        lower.endsWith(".jpeg") || lower.endsWith(".webp") || lower.endsWith(".bmp")
            }
            isVisible = true
        }
        val file = dialog.file ?: return null
        val dir = dialog.directory ?: return null
        return File(dir, file)
    }
}
