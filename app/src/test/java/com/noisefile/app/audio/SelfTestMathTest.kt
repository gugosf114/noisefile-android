package com.noisefile.app.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow
import kotlin.random.Random

class SelfTestMathTest {
    private val rate = 48_000

    /** Play the real test sound through a pretend speaker + room + microphone and cut it into windows. */
    private fun hear(gainDb: Double = -20.0, noise: Double = 0.0, squeezeAboveDbfs: Double? = null, delaySamples: Int = 3_000): List<ToneWindow> {
        val played = SelfTestMath.toneSequence(rate)
        val r = Random(3)
        val gain = 10.0.pow(gainDb / 20.0)
        val heard = ShortArray(played.size + delaySamples + SelfTestMath.WINDOW_SAMPLES * 8)
        for (i in heard.indices) {
            val source = if (i - delaySamples in played.indices) played[i - delaySamples] * gain else 0.0
            var v = source
            squeezeAboveDbfs?.let { knee ->
                val limit = 32_767.0 * 10.0.pow(knee / 20.0)
                if (kotlin.math.abs(v) > limit) v = kotlin.math.sign(v) * (limit + (kotlin.math.abs(v) - limit) * 0.25)
            }
            heard[i] = (v + noise * (r.nextDouble() * 2 - 1)).toInt().coerceIn(-32_768, 32_767).toShort()
        }
        val n = SelfTestMath.WINDOW_SAMPLES
        return (0 until heard.size / n).map { w ->
            val window = heard.copyOfRange(w * n, (w + 1) * n)
            ToneWindow(ToneProbe.share(window, n, SelfTestMath.TONE_HZ, rate), ToneProbe.levelDbfs(window, n, SelfTestMath.TONE_HZ, rate))
        }
    }

    @Test
    fun aStraightMicrophoneHearsTenAndTwenty() {
        val result = SelfTestMath.evaluate(hear())
        assertEquals(SelfTestOutcome.READS_STRAIGHT, result.outcome)
        assertEquals(3, result.stepsHeard)
        assertEquals(20, result.rangeDb)
        assertEquals(10.0, result.measuredDropsDb[0], 0.3)
        assertEquals(20.0, result.measuredDropsDb[1], 0.3)
    }

    @Test
    fun theSpeakersOwnLoudnessDoesNotMatterOnlyTheSteps() {
        listOf(-5.0, -20.0, -35.0).forEach { gain ->
            assertEquals("gain $gain", SelfTestOutcome.READS_STRAIGHT, SelfTestMath.evaluate(hear(gainDb = gain)).outcome)
        }
    }

    @Test
    fun aLittleRoomNoiseStillPasses() {
        assertEquals(SelfTestOutcome.READS_STRAIGHT, SelfTestMath.evaluate(hear(gainDb = -10.0, noise = 60.0)).outcome)
    }

    @Test
    fun aMicrophoneThatSqueezesLoudSoundIsNotConfirmed() {
        val result = SelfTestMath.evaluate(hear(gainDb = 0.0, squeezeAboveDbfs = -30.0))
        assertEquals(SelfTestOutcome.NOT_CONFIRMED, result.outcome)
        assertTrue(result.worstErrorDb > SelfTestMath.TOLERANCE_DB)
    }

    @Test
    fun aLoudRoomThatBuriesTheQuietToneCoversOnlyTheRangeItHeard() {
        // noise strong enough to bury the -20 dB tone but not the first two
        val result = SelfTestMath.evaluate(hear(gainDb = -20.0, noise = 400.0))
        assertTrue(result.stepsHeard in 1..2)
        if (result.stepsHeard == 2) {
            assertEquals(10, result.rangeDb)
            assertEquals(SelfTestOutcome.READS_STRAIGHT, result.outcome)
        } else {
            assertEquals(SelfTestOutcome.NOT_CONFIRMED, result.outcome)
        }
    }

    @Test
    fun silenceMeansTheToneWasNotHeard() {
        val silent = List(90) { ToneWindow(share = 0.0, levelDb = -120.0) }
        assertEquals(SelfTestOutcome.TONE_NOT_HEARD, SelfTestMath.evaluate(silent).outcome)
    }

    @Test
    fun aFullScaleSineReadsZeroAndTenDownReadsMinusTen() {
        val n = SelfTestMath.WINDOW_SAMPLES
        fun sine(amplitude: Double) = ShortArray(n) { i -> (amplitude * kotlin.math.sin(2 * Math.PI * 1_000.0 * i / rate)).toInt().toShort() }
        assertEquals(0.0, ToneProbe.levelDbfs(sine(32_767.0), n, 1_000.0, rate), 0.05)
        assertEquals(-10.0, ToneProbe.levelDbfs(sine(32_767.0 * 10.0.pow(-0.5)), n, 1_000.0, rate), 0.05)
    }

    @Test
    fun theProgressLineCountsOneTwoThree() {
        assertEquals(1, SelfTestMath.stepAt(0))
        assertEquals(1, SelfTestMath.stepAt(20))
        assertEquals(2, SelfTestMath.stepAt(35))
        assertEquals(3, SelfTestMath.stepAt(60))
        assertEquals(3, SelfTestMath.stepAt(90))
    }
}
