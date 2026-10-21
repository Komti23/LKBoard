package com.lkboard.app.data

import com.lkboard.app.model.GridType
import com.lkboard.app.model.PenSettings
import com.lkboard.app.model.ShapeSettings

/**
 * Хранилище настроек приложения (обратная совместимость).
 * Делегирует вызовы в синглтон AppPreferences.
 */
object PreferencesManager {
    fun loadPenSettings(): PenSettings = AppPreferences.loadPenSettings()

    fun savePenSettings(settings: PenSettings) = AppPreferences.savePenSettings(settings)

    fun loadGridType(): GridType = AppPreferences.loadGridType()

    fun saveGridType(gridType: GridType) = AppPreferences.saveGridType(gridType)

    fun loadBackgroundColor(): Long = AppPreferences.loadBackgroundColor()

    fun saveBackgroundColor(colorHex: Long) = AppPreferences.saveBackgroundColor(colorHex)

    fun loadShapeSettings(): ShapeSettings = AppPreferences.loadShapeSettings()

    fun saveShapeSettings(settings: ShapeSettings) = AppPreferences.saveShapeSettings(settings)
}
