package com.noisefile.app.audio

import kotlin.math.min

/**
 * Keeps the loudest [clipSeconds] of a measurement: the [beforeSeconds] before
 * the highest window and the rest after it. Raw 16-bit mono samples, no
 * processing. If a louder moment comes later, the clip restarts around it.
 */
class PeakClipKeeper(
    val sampleRate: Int,
    clipSeconds: Int = 10,
    private val beforeSeconds: Int = 5,
) {
    private val clipLength = sampleRate * clipSeconds
    private val ringLength = sampleRate * (beforeSeconds + 1)
    private val ring = ShortArray(ringLength)
    private var ringWrite = 0
    private var ringFilled = 0

    private val clip = ShortArray(clipLength)
    private var clipFilled = 0
    private var armed = false
    private var peakDb = -1.0
    /** Sample index (from the start of the take) where the clip begins; -1 until a peak exists. */
    var clipStartSample: Long = -1
        private set
    private var totalSamples = 0L

    fun add(samples: ShortArray, count: Int, windowDb: Double) {
        if (windowDb > peakDb) {
            peakDb = windowDb
            // restart the clip: the last beforeSeconds from the ring, then this window, then keep filling
            clipFilled = 0
            val back = min(ringFilled, sampleRate * beforeSeconds)
            var idx = (ringWrite - back + ringLength) % ringLength
            for (i in 0 until back) { clip[clipFilled++] = ring[idx]; idx = (idx + 1) % ringLength }
            clipStartSample = totalSamples - back
            armed = true
        }
        if (armed && clipFilled < clipLength) {
            val take = min(count, clipLength - clipFilled)
            System.arraycopy(samples, 0, clip, clipFilled, take)
            clipFilled += take
        }
        for (i in 0 until count) { ring[ringWrite] = samples[i]; ringWrite = (ringWrite + 1) % ringLength }
        ringFilled = min(ringLength, ringFilled + count)
        totalSamples += count
    }

    /** The clip so far (may be shorter than the full length on a short take), or null if nothing was heard. */
    fun snapshot(): ShortArray? = if (!armed || clipFilled == 0) null else clip.copyOf(clipFilled)

    val peakEstimateDb: Double get() = peakDb
}

/** A 16-bit mono PCM WAV file: 44-byte header + samples. */
object WavWriter {
    fun bytes(samples: ShortArray, sampleRate: Int): ByteArray {
        val dataLength = samples.size * 2
        val out = ByteArray(44 + dataLength)
        fun putInt(at: Int, v: Int) { out[at] = (v and 0xff).toByte(); out[at + 1] = (v shr 8 and 0xff).toByte(); out[at + 2] = (v shr 16 and 0xff).toByte(); out[at + 3] = (v shr 24 and 0xff).toByte() }
        fun putShort(at: Int, v: Int) { out[at] = (v and 0xff).toByte(); out[at + 1] = (v shr 8 and 0xff).toByte() }
        fun putAscii(at: Int, s: String) { s.forEachIndexed { i, c -> out[at + i] = c.code.toByte() } }
        putAscii(0, "RIFF"); putInt(4, 36 + dataLength); putAscii(8, "WAVE")
        putAscii(12, "fmt "); putInt(16, 16); putShort(20, 1); putShort(22, 1)
        putInt(24, sampleRate); putInt(28, sampleRate * 2); putShort(32, 2); putShort(34, 16)
        putAscii(36, "data"); putInt(40, dataLength)
        var at = 44
        samples.forEach { s -> out[at] = (s.toInt() and 0xff).toByte(); out[at + 1] = (s.toInt() shr 8 and 0xff).toByte(); at += 2 }
        return out
    }
}
