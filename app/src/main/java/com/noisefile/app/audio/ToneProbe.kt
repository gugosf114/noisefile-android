package com.noisefile.app.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sqrt

/** Measures one pitch inside a window of sound: how strong it is, and how much of the window it holds. */
object ToneProbe {
    /** Goertzel power of one frequency over the first [n] samples. A sine of amplitude A gives (A n / 2)^2. */
    fun power(samples: ShortArray, n: Int, frequency: Double, sampleRate: Int): Double {
        val coefficient = 2.0 * cos(2.0 * PI * frequency / sampleRate)
        var s1 = 0.0; var s2 = 0.0
        for (i in 0 until n) {
            val s0 = samples[i] + coefficient * s1 - s2
            s2 = s1; s1 = s0
        }
        return s1 * s1 + s2 * s2 - coefficient * s1 * s2
    }

    /** Share (0..1) of the window's energy held by [frequency]. */
    fun share(samples: ShortArray, count: Int, frequency: Double, sampleRate: Int): Double {
        val n = count.coerceAtMost(samples.size)
        if (n < 256) return 0.0
        var total = 0.0
        for (i in 0 until n) { val s = samples[i].toDouble(); total += s * s }
        if (total <= 0.0) return 0.0
        return (2.0 * power(samples, n, frequency, sampleRate) / (n * total)).coerceIn(0.0, 1.0)
    }

    /**
     * How far [frequency] stands above the sound right next to it, in dB. A tone in a noisy room
     * is lost in the room's total sound long before it is lost among its own neighbours, so this
     * finds a quiet tone that [share] would miss. Neighbours sit whole bins away (no leakage).
     */
    fun standoutDb(samples: ShortArray, count: Int, frequency: Double, sampleRate: Int): Double {
        val n = count.coerceAtMost(samples.size)
        if (n < 256) return 0.0
        val tone = power(samples, n, frequency, sampleRate)
        if (tone <= 0.0) return 0.0
        val bin = sampleRate.toDouble() / n
        val neighbours = listOf(-10, -5, 5, 10).map { power(samples, n, frequency + it * bin, sampleRate) }
        val floor = neighbours.average()
        if (floor <= 1e-9) return 60.0
        return (10.0 * log10(tone / floor)).coerceIn(0.0, 60.0)
    }

    /** Level of [frequency] in dB relative to a full-scale sine (which reads 0). */
    fun levelDbfs(samples: ShortArray, count: Int, frequency: Double, sampleRate: Int): Double {
        val n = count.coerceAtMost(samples.size)
        if (n < 256) return -120.0
        val amplitude = 2.0 * sqrt(power(samples, n, frequency, sampleRate).coerceAtLeast(0.0)) / n
        return if (amplitude <= 0.0) -120.0 else 20.0 * log10(amplitude / 32_768.0)
    }
}
