package com.lkboard.app.data

import com.lkboard.app.model.BoardElement
import com.lkboard.app.model.CanvasTransform
import com.lkboard.app.model.GridType
import com.lkboard.app.model.PenSettings
import com.lkboard.app.model.ShapeSettings
import kotlinx.serialization.Serializable

/**
 * Формат файла проекта Lkboard (.lkboard).
 */
@Serializable
data class BoardDocument(
    val version: Int = CURRENT_VERSION,
    val title: String = "Без названия",
    val elements: List<BoardElement> = emptyList(),
    val transform: CanvasTransform = CanvasTransform(),
    val gridType: GridType = GridType.DOTS,
    val backgroundColorHex: Long = 0xFFFFFFFF,
    val penSettings: PenSettings? = null,
    val shapeSettings: ShapeSettings? = null
) {
    companion object {
        const val CURRENT_VERSION = 1
        const val FILE_EXTENSION = "lkboard"
        const val FILE_DESCRIPTION = "Lkboard Project (*.lkboard)"
    }
}
