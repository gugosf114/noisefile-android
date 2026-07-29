package com.noisefile.app.audio

import kotlin.math.log10
import kotlin.math.sqrt

object NoiseMath {
    private const val MAX_PCM_AMPLITUDE = 32768.0
    
    // We maintain a 90.0 offset as an uncalibrated default, but note that 
    // real calibration requires comparing this app against a Type 1 or Type 2 SPL meter.
    private const val DISPLAY_OFFSET_DBA = 90.0

    fun rmsToEstimatedDbA(filteredSamples: DoubleArray, count: Int = filteredSamples.size): Double {
        if (count <= 0) return 0.0

        var squaredTotal = 0.0
        for (index in 0 until count.coerceAtMost(filteredSamples.size)) {
            val sample = filteredSamples[index]
            squaredTotal += sample * sample
        }

        val rms = sqrt(squaredTotal / count.coerceAtMost(filteredSamples.size))
        if (rms <= 0.0) return 0.0

        val dbfs = 20.0 * log10(rms / MAX_PCM_AMPLITUDE)
        return (dbfs + DISPLAY_OFFSET_DBA).coerceIn(0.0, 100.0)
    }
}
