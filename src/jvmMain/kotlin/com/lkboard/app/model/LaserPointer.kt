package com.lkboard.app.model

import androidx.compose.ui.geometry.Offset

/**
 * Точка следа лазерной указки с временной меткой для плавного затухания.
 */
data class LaserPoint(
    val pos: Offset,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Менеджер следа лазерной указки (Laser Pointer).
 */
object LaserPointerManager {
    const val TRAIL_DURATION_MS = 1500L

    fun pruneTrail(trail: List<LaserPoint>, now: Long = System.currentTimeMillis()): List<LaserPoint> {
        return trail.filter { now - it.timestamp <= TRAIL_DURATION_MS }
    }
}
