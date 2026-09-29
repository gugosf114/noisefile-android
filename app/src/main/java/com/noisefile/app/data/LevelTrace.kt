package com.noisefile.app.data

import kotlin.math.max

/**
 * The take, second by second: the highest estimate seen in each second.
 * Numbers only, never audio. Kept to at most [maxPoints] points by merging
 * neighbouring seconds, so an hour-long take still fits in one small array.
 */
class LevelTraceRecorder(private val maxPoints: Int = 1_800) {
    private val seconds = ArrayList<Int>()
    private var secondsPerPoint = 1

    fun add(elapsedMillis: Long, db: Double) {
        val second = (elapsedMillis / 1_000L).toInt()
        val index = second / secondsPerPoint
        val value = max(0, db.toInt())
        while (seconds.size <= index) seconds.add(0)
        seconds[index] = max(seconds[index], value)
        if (seconds.size > maxPoints) fold()
    }

    /** Merge every two points into one so the array halves and each point spans twice as long. */
    private fun fold() {
        val merged = ArrayList<Int>((seconds.size + 1) / 2)
        var i = 0
        while (i < seconds.size) {
            val a = seconds[i]
            val b = if (i + 1 < seconds.size) seconds[i + 1] else a
            merged.add(max(a, b))
            i += 2
        }
        seconds.clear(); seconds.addAll(merged)
        secondsPerPoint *= 2
    }

    /** Seconds each point covers (1 for a short take, 2, 4, … after folding). */
    val secondsPerSample: Int get() = secondsPerPoint

    fun snapshot(): List<Int> = seconds.toList()

    fun reset() { seconds.clear(); secondsPerPoint = 1 }
}
