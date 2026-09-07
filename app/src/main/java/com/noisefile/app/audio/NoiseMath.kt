package com.noisefile.app.audio

import kotlin.math.log10
import kotlin.math.sqrt

object NoiseMath {
    private const val MAX_PCM_AMPLITUDE = 32768.0

    /** The reference tone every sensitivity figure is stated against: 1 kHz at 94 dB SPL. */
    const val REFERENCE_SPL_DB = 94.0

    /**
     * Uncalibrated default for the processed capture paths (VOICE_RECOGNITION). The
     * phone applies its own level, so this is an estimate until the phone is
     * compared against a Type 1 or Type 2 meter.
     */
    const val ESTIMATE_OFFSET_DBA = 90.0

    /**
     * Android CDD 5.11 [C-1-5]: a device that declares the UNPROCESSED source MUST
     * deliver a 1 kHz tone at 94 dB SPL as -36 dBFS (RMS 520 for 16-bit samples),
     * with no AGC, noise reduction or filtering in the path. So on that path
     * dB SPL = dBFS + 130. This is the platform's own calibration, not ours.
     */
    const val CDD_UNPROCESSED_OFFSET_DBA = REFERENCE_SPL_DB + 36.0

    /** Display ceiling. A spec-calibrated path can legitimately read above 100 dB. */
    const val MAX_DISPLAY_DB = 120.0

    /** MicrophoneInfo.getSensitivity() is the dBFS produced by 94 dB SPL at 1 kHz. */
    fun offsetFromSensitivity(sensitivityDbfsAt94DbSpl: Double): Double =
        REFERENCE_SPL_DB - sensitivityDbfsAt94DbSpl

    fun rmsToEstimatedDbA(
        filteredSamples: DoubleArray,
        count: Int = filteredSamples.size,
        offsetDb: Double = ESTIMATE_OFFSET_DBA,
    ): Double {
        if (count <= 0) return 0.0

        var squaredTotal = 0.0
        val used = count.coerceAtMost(filteredSamples.size)
        for (index in 0 until used) {
            val sample = filteredSamples[index]
            squaredTotal += sample * sample
        }

        val rms = sqrt(squaredTotal / used)
        if (rms <= 0.0) return 0.0

        val dbfs = 20.0 * log10(rms / MAX_PCM_AMPLITUDE)
        return (dbfs + offsetDb).coerceIn(0.0, MAX_DISPLAY_DB)
    }
}
