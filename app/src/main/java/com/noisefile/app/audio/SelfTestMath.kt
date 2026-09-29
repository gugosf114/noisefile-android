package com.noisefile.app.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

/** One 0.1-second window heard during the self-test. */
data class ToneWindow(val share: Double, val levelDb: Double)

enum class SelfTestOutcome {
    /** The microphone heard the steps the speaker played, within tolerance. */
    READS_STRAIGHT,
    /** The steps were heard but did not match, or only one was heard. Nothing is claimed. */
    NOT_CONFIRMED,
    /** The microphone never heard the tone. */
    TONE_NOT_HEARD,
}

data class SelfTestResult(
    val outcome: SelfTestOutcome,
    val stepsHeard: Int,
    /** The range the check covers: 10 when two steps were heard, 20 when all three were. */
    val rangeDb: Int,
    val worstErrorDb: Double,
    /** How far below the first tone the microphone heard each later tone. */
    val measuredDropsDb: List<Double>,
)

/**
 * The microphone checks itself. The phone's speaker plays one pitch at three
 * levels, each exactly 10 dB quieter than the last (the steps are made in the
 * numbers, so they are exact). A microphone that reads straight hears drops of
 * 10 and 20 dB. One that squeezes or stretches sound hears something else.
 */
object SelfTestMath {
    const val TONE_HZ = 1_000.0
    const val WINDOW_SAMPLES = 4_800
    const val LEAD_SECONDS = 0.5
    const val TONE_SECONDS = 2.0
    const val GAP_SECONDS = 0.6
    /** Loudest tone, below full scale, so the speaker's own limiter stays out of the way. */
    const val BASE_DBFS = -15.0
    /** The test sets the alarm channel to this share of its full volume, then puts it back. */
    const val TEST_VOLUME_SHARE = 0.8
    val STEP_DROPS_DB = listOf(0.0, 10.0, 20.0)
    const val TOLERANCE_DB = 2.5
    const val MIN_SHARE = 0.5
    private const val MIN_RUN_WINDOWS = 6
    private const val TRIM_WINDOWS = 2

    /** The whole test sound: lead-in, three tones with gaps, lead-out. */
    fun toneSequence(sampleRate: Int): ShortArray {
        val lead = (LEAD_SECONDS * sampleRate).toInt()
        val tone = (TONE_SECONDS * sampleRate).toInt()
        val gap = (GAP_SECONDS * sampleRate).toInt()
        val ramp = sampleRate / 100
        val out = ShortArray(lead + STEP_DROPS_DB.size * tone + (STEP_DROPS_DB.size - 1) * gap + lead)
        var at = lead
        STEP_DROPS_DB.forEachIndexed { index, drop ->
            val amplitude = 32_767.0 * 10.0.pow((BASE_DBFS - drop) / 20.0)
            for (i in 0 until tone) {
                val fade = when {
                    i < ramp -> i.toDouble() / ramp
                    i > tone - ramp -> (tone - i).toDouble() / ramp
                    else -> 1.0
                }
                out[at + i] = (amplitude * fade * sin(2.0 * PI * TONE_HZ * i / sampleRate)).toInt().toShort()
            }
            at += tone + if (index < STEP_DROPS_DB.size - 1) gap else 0
        }
        return out
    }

    fun totalWindows(sampleRate: Int): Int = toneSequence(sampleRate).size / WINDOW_SAMPLES + 8

    /** Which tone (1..3) is playing at a given window, for the progress line. */
    fun stepAt(windowIndex: Int, sampleRate: Int = 48_000): Int {
        val seconds = windowIndex.toDouble() * WINDOW_SAMPLES / sampleRate
        val perStep = TONE_SECONDS + GAP_SECONDS
        return (((seconds - LEAD_SECONDS) / perStep).toInt() + 1).coerceIn(1, STEP_DROPS_DB.size)
    }

    /** The level of each stretch where the tone clearly held the window, in the order heard. */
    fun plateaus(windows: List<ToneWindow>): List<Double> {
        val out = ArrayList<Double>()
        var run = ArrayList<Double>()
        fun close() {
            if (run.size >= MIN_RUN_WINDOWS) {
                val core = run.subList(TRIM_WINDOWS, run.size - TRIM_WINDOWS).sorted()
                out.add(core[core.size / 2])
            }
            run = ArrayList()
        }
        windows.forEach { w -> if (w.share >= MIN_SHARE) run.add(w.levelDb) else close() }
        close()
        return out
    }

    fun evaluate(windows: List<ToneWindow>): SelfTestResult {
        val levels = plateaus(windows).take(STEP_DROPS_DB.size)
        if (levels.isEmpty()) return SelfTestResult(SelfTestOutcome.TONE_NOT_HEARD, 0, 0, 0.0, emptyList())
        val drops = levels.drop(1).map { levels[0] - it }
        if (drops.isEmpty()) return SelfTestResult(SelfTestOutcome.NOT_CONFIRMED, 1, 0, 0.0, emptyList())
        var worst = 0.0
        drops.forEachIndexed { i, drop -> worst = max(worst, abs(drop - STEP_DROPS_DB[i + 1])) }
        return SelfTestResult(
            outcome = if (worst <= TOLERANCE_DB) SelfTestOutcome.READS_STRAIGHT else SelfTestOutcome.NOT_CONFIRMED,
            stepsHeard = levels.size,
            rangeDb = STEP_DROPS_DB[levels.size - 1].toInt(),
            worstErrorDb = worst,
            measuredDropsDb = drops,
        )
    }
}
