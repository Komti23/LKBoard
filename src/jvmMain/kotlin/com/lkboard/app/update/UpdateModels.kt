package com.lkboard.app.update

import kotlinx.serialization.Serializable

/**
 * Описание манифеста обновления, возвращаемого сервером.
 */
@Serializable
data class UpdateInfo(
    val version: String,
    val versionCode: Int,
    val releaseDate: String = "",
    val title: String = "",
    val changelog: List<String> = emptyList(),
    val downloadUrl: String = "",
    val mandatory: Boolean = false
)

/**
 * Текущее состояние проверки обновлений в приложении.
 */
data class UpdateCheckState(
    val isChecking: Boolean = false,
    val updateAvailable: Boolean = false,
    val updateInfo: UpdateInfo? = null,
    val isManualCheck: Boolean = false,
    val isUpToDateMessageShown: Boolean = false,
    val errorMessage: String? = null
)
