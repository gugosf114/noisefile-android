package com.noisefile.app.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin

/** One 0.1-second window heard during the self-test. */
data class ToneWindow(
    val share: Double,
    val levelDb: Double,
    /** How far the test pitch stands above its neighbours, in dB. */
    val standoutDb: Double = 0.0,
)

enum class SelfTestOutcome {
    /** The microphone heard the steps the speaker played, within tolerance, over at least 20 dB. */
    READS_STRAIGHT,
    /** The steps were heard but did not match, or too few were heard. Nothing is claimed. */
    NOT_CONFIRMED,
    /** The microphone never heard the tone. */
    TONE_NOT_HEARD,
}

data class SelfTestResult(
    val outcome: SelfTestOutcome,
    /** How many of the tones were heard. */
    val stepsHeard: Int,
    /** The range over which steps read correctly, in dB. */
    val rangeDb: Int,
    val worstErrorDb: Double,
    /** The step between each pair of neighbouring tones that were both heard, loudest first. */
    val measuredDropsDb: List<Double>,
    /** Steps at the loud end that read short and were left out of the range. */
    val squeezedLoudSteps: Int = 0,
)

/**
 * The microphone checks itself. The phone's speaker plays one pitch at six
 * levels, each exactly 10 dB quieter than the last (the steps are made in the
 * numbers, so they are exact). The tones follow a fixed timetable, so each one
 * is measured where it is known to be, not hunted for. A microphone that reads
 * straight hears steps of 10 dB. Phone speakers squeeze loud sound, so the
 * test looks for the longest stretch of tones whose steps read true.
 */
object SelfTestMath {
    const val TONE_HZ = 1_000.0
    const val WINDOW_SAMPLES = 4_800
    const val TONES = 6
    const val LOUDEST_DBFS = -20.0
    const val STEP_DB = 10.0
    const val LEAD_SECONDS = 0.5
    const val TONE_SECONDS = 1.0
    const val GAP_SECONDS = 0.3
    /** The test sets the alarm channel to this share of its full volume, then puts it back. */
    const val TEST_VOLUME_SHARE = 0.8
    const val TOLERANCE_DB = 2.5
    const val RUN_TOLERANCE_DB = 4.0
    const val MIN_SHARE = 0.5
    /** A window counts as "tone heard" when the pitch stands this far above the sound next to it. */
    const val MIN_STANDOUT_DB = 12.0
    /** Steps that must read true, one after another, for a pass: 2 steps = 20 dB. */
    const val MIN_STRAIGHT_STEPS = 2

    private const val WINDOWS_PER_TONE = 10
    private const val WINDOWS_PER_STEP = 13
    private const val SLOT_FROM = 3
    private const val SLOT_TO = 8

    /** The whole test sound: lead-in, six tones with gaps, lead-out. Samples are -1..1. */
    fun toneSequence(sampleRate: Int): FloatArray {
        val lead = (LEAD_SECONDS * sampleRate).toInt()
        val tone = (TONE_SECONDS * sampleRate).toInt()
        val gap = (GAP_SECONDS * sampleRate).toInt()
        val ramp = sampleRate / 100
        val out = FloatArray(lead + TONES * tone + (TONES - 1) * gap + lead)
        var at = lead
        for (index in 0 until TONES) {
            val amplitude = 10.0.pow((LOUDEST_DBFS - STEP_DB * index) / 20.0)
            for (i in 0 until tone) {
                val fade = when {
                    i < ramp -> i.toDouble() / ramp
                    i > tone - ramp -> (tone - i).toDouble() / ramp
                    else -> 1.0
                }
                out[at + i] = (amplitude * fade * sin(2.0 * PI * TONE_HZ * i / sampleRate)).toFloat()
            }
            at += tone + if (index < TONES - 1) gap else 0
        }
        return out
    }

    fun totalWindows(sampleRate: Int): Int = toneSequence(sampleRate).size / WINDOW_SAMPLES + 8

    /** Which tone (1..6) is playing at a given window, for the progress line. */
    fun stepAt(windowIndex: Int, sampleRate: Int = 48_000): Int {
        val seconds = windowIndex.toDouble() * WINDOW_SAMPLES / sampleRate
        val perStep = TONE_SECONDS + GAP_SECONDS
        return (((seconds - LEAD_SECONDS) / perStep).toInt() + 1).coerceIn(1, TONES)
    }

    /** Where the first tone starts: the first window of five in a row that hear the pitch. */
    fun onset(windows: List<ToneWindow>): Int? =
        (0..windows.size - 5).firstOrNull { i -> (i until i + 5).all { windows[it].standoutDb >= MIN_STANDOUT_DB } }

    /** The level of each tone at its place in the timetable; null where the tone was not heard. */
    fun toneLevels(windows: List<ToneWindow>): List<Double?> {
        val start = onset(windows) ?: return List(TONES) { null }
        return List(TONES) { k ->
            val from = start + k * WINDOWS_PER_STEP + SLOT_FROM
            val to = start + k * WINDOWS_PER_STEP + SLOT_TO
            if (to > windows.size) return@List null
            val heard = windows.subList(from, to).filter { it.standoutDb >= MIN_STANDOUT_DB }.map { it.levelDb }.sorted()
            if (heard.size >= 3) heard[heard.size / 2] else null
        }
    }

    fun evaluate(windows: List<ToneWindow>): SelfTestResult {
        val levels = toneLevels(windows)
        val heard = levels.count { it != null }
        if (heard == 0) return SelfTestResult(SelfTestOutcome.TONE_NOT_HEARD, 0, 0, 0.0, emptyList())
        val steps: List<Double?> = (0 until TONES - 1).map { k ->
            val a = levels[k]; val b = levels[k + 1]
            if (a != null && b != null) a - b else null
        }
        // the longest stretch of neighbouring steps that all read true
        var bestFrom = 0; var bestLength = 0; var bestWorst = 0.0
        for (from in steps.indices) {
            var sum = 0.0; var worst = 0.0; var length = 0
            for (k in from until steps.size) {
                val step = steps[k] ?: break
                if (abs(step - STEP_DB) > TOLERANCE_DB) break
                sum += step
                if (abs(sum - STEP_DB * (k - from + 1)) > RUN_TOLERANCE_DB) break
                worst = maxOf(worst, abs(step - STEP_DB))
                length = k - from + 1
                if (length > bestLength) { bestLength = length; bestFrom = from; bestWorst = worst }
            }
        }
        val measured = steps.filterNotNull()
        if (bestLength < MIN_STRAIGHT_STEPS) {
            val worst = measured.maxOfOrNull { abs(it - STEP_DB) } ?: 0.0
            return SelfTestResult(SelfTestOutcome.NOT_CONFIRMED, heard, (bestLength * STEP_DB).toInt(), worst, measured)
        }
        val before = steps.subList(0, bestFrom)
        val squeezed = if (before.all { it != null && it < STEP_DB - TOLERANCE_DB }) before.size else 0
        return SelfTestResult(
            outcome = SelfTestOutcome.READS_STRAIGHT,
            stepsHeard = heard,
            rangeDb = (bestLength * STEP_DB).toInt(),
            worstErrorDb = bestWorst,
            measuredDropsDb = measured,
            squeezedLoudSteps = squeezed,
        )
    }
}
