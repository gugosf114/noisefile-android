package com.noisefile.app.audio

import kotlin.math.log10
import kotlin.math.sqrt

object NoiseMath {
    private const val MAX_PCM_AMPLITUDE = 32768.0
    private const val DISPLAY_OFFSET_DB = 90.0

    fun rmsToEstimatedDb(samples: ShortArray, count: Int = samples.size): Double {
        if (count <= 0) return 0.0

        var squaredTotal = 0.0
        for (index in 0 until count.coerceAtMost(samples.size)) {
            val sample = samples[index].toDouble()
            squaredTotal += sample * sample
        }

        val rms = sqrt(squaredTotal / count.coerceAtMost(samples.size))
        if (rms <= 0.0) return 0.0

        val dbfs = 20.0 * log10(rms / MAX_PCM_AMPLITUDE)
        return (dbfs + DISPLAY_OFFSET_DB).coerceIn(0.0, 100.0)
    }
}
