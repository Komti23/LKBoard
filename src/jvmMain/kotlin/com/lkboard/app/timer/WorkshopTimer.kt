package com.lkboard.app.timer

import java.awt.Toolkit
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Состояние плавающего таймера для воркшопов, уроков и спринтов.
 */
data class WorkshopTimerState(
    val isVisible: Boolean = false,
    val isRunning: Boolean = false,
    val totalSeconds: Int = 15 * 60,
    val remainingSeconds: Int = 15 * 60,
    val isStopwatch: Boolean = false,
    val isFinished: Boolean = false,
    val isMinimized: Boolean = false
) {
    val formattedTime: String
        get() {
            val sec = remainingSeconds.coerceAtLeast(0)
            val m = sec / 60
            val s = sec % 60
            return String.format("%02d:%02d", m, s)
        }

    val progress: Float
        get() = if (totalSeconds <= 0) 0f else (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
}

/**
 * Синтезатор гармоничного гонга / колокольчика окончания времени (C5 + E5 + G5 chord).
 * Работает автономно через стандартный Java Sound API без внешних аудиофайлов.
 */
object AudioChimePlayer {
    fun playChime() {
        Thread {
            try {
                val sampleRate = 44100f
                val durationSec = 1.6f
                val numSamples = (durationSec * sampleRate).toInt()
                val buffer = ByteArray(numSamples * 2)

                // Трезвучие C-major (C5 523.25 Гц, E5 659.25 Гц, G5 783.99 Гц) с мягким затуханием
                for (i in 0 until numSamples) {
                    val time = i / sampleRate
                    val decay = exp(-3.2 * time)
                    val s1 = sin(2.0 * PI * 523.25 * time)
                    val s2 = sin(2.0 * PI * 659.25 * time) * 0.75
                    val s3 = sin(2.0 * PI * 783.99 * time) * 0.55
                    val sample = (s1 + s2 + s3) * decay * 0.35
                    val clamped = (sample * 32767).toInt().coerceIn(-32768, 32767).toShort()
                    buffer[2 * i] = (clamped.toInt() and 0xFF).toByte()
                    buffer[2 * i + 1] = ((clamped.toInt() shr 8) and 0xFF).toByte()
                }

                val format = AudioFormat(sampleRate, 16, 1, true, false)
                val line = AudioSystem.getSourceDataLine(format)
                line.open(format)
                line.start()
                line.write(buffer, 0, buffer.size)
                line.drain()
                line.close()
            } catch (_: Exception) {
                try {
                    Toolkit.getDefaultToolkit().beep()
                } catch (_: Exception) {}
            }
        }.apply {
            isDaemon = true
            name = "WorkshopTimerAudioThread"
            start()
        }
    }
}
