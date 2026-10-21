package com.lkboard.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lkboard.app.model.GridType
import com.lkboard.app.model.PenSettings

/**
 * Обратная совместимость для быстрого меню стилуса:
 * Делегирует в StylusPropertiesPopup в эталонном стиле iOS Liquid Glass.
 */
@Composable
fun StylusQuickMenu(
    settings: PenSettings,
    onSettingsChange: (PenSettings) -> Unit,
    gridType: GridType = GridType.DOTS,
    onGridTypeChange: ((GridType) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    StylusPropertiesPopup(
        settings = settings,
        onSettingsChange = onSettingsChange,
        gridType = gridType,
        onGridTypeChange = onGridTypeChange,
        modifier = modifier
    )
}
