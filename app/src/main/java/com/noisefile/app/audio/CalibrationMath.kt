package com.noisefile.app.audio

/**
 * Per-microphone calibration against a reference meter: the user holds a real
 * meter next to the phone in a steady sound, the app averages, the user types
 * what the meter said. The difference becomes this microphone's offset. Pure
 * math, so it is testable without a phone.
 */
object CalibrationMath {
    const val MIN_REFERENCE_DB = 30.0
    const val MAX_REFERENCE_DB = 120.0

    /** The new offset that makes the phone's average read what the meter read. */
    fun newUserOffset(existingUserOffsetDb: Double, phoneAverageDb: Double, referenceDb: Double): Double =
        existingUserOffsetDb + (referenceDb - phoneAverageDb)

    /** UL 217 / NFPA 72: a residential smoke alarm sounds at 85 dBA or more, measured 10 feet away. */
    const val SMOKE_ALARM_DBA_AT_10_FT = 85.0

    fun isPlausibleReference(referenceDb: Double): Boolean =
        referenceDb in MIN_REFERENCE_DB..MAX_REFERENCE_DB

    fun signed(offsetDb: Double): String {
        val rounded = kotlin.math.round(offsetDb).toInt()
        return if (rounded >= 0) "+$rounded dB" else "$rounded dB"
    }
}
