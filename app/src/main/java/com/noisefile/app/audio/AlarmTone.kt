package com.noisefile.app.audio


/**
 * Hears a smoke alarm by its tone, not by loudness. Residential alarms sound a
 * steady pitch near 3.1 kHz (piezo horn) or 520 Hz (low-frequency sounders).
 * A window counts as "alarm" only when one pitch in those bands holds at least
 * half of the window's sound energy. Talking, music, a door slam, a TV: none do.
 */
object AlarmTone {
    const val MIN_TONE_SHARE = 0.5
    /** Windows of alarm tone needed before a calibration is accepted (about half a second). */
    const val MIN_TONE_WINDOWS = 6

    private val BANDS = listOf(2_800.0 to 3_600.0, 480.0 to 560.0)

    /** Share (0..1) of the window's energy held by the strongest single pitch inside the alarm bands. */
    fun toneShare(samples: ShortArray, count: Int, sampleRate: Int): Double {
        val n = count.coerceAtMost(samples.size)
        if (n < 256) return 0.0
        var total = 0.0
        for (i in 0 until n) { val s = samples[i].toDouble(); total += s * s }
        if (total <= 0.0) return 0.0
        // Half a frequency bin per step, so a pitch between two steps still lands near one.
        val step = sampleRate.toDouble() / n / 2.0
        var best = 0.0
        for ((low, high) in BANDS) {
            var f = low
            while (f <= high) {
                best = maxOf(best, ToneProbe.power(samples, n, f, sampleRate))
                f += step
            }
        }
        // A pure sine of amplitude A over n samples: power = (A n / 2)^2, total = n A^2 / 2 -> share 1.
        return (2.0 * best / (n * total)).coerceIn(0.0, 1.0)
    }

    /** A saved offset further than this from the phone's own estimate is refused as implausible. */
    const val MAX_PLAUSIBLE_OFFSET_DB = 30.0
}
