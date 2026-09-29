package com.noisefile.app.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign
import kotlin.random.Random

class SelfTestMathTest {
    private val rate = 48_000

    /**
     * Play the real test sound through a pretend speaker, room and microphone and cut it into windows.
     * [shape] bends each sample the way a squeezing speaker or microphone would.
     */
    private fun hear(
        gainDb: Double = 0.0,
        noise: Double = 0.0,
        delaySamples: Int = 3_000,
        burstAtWindow: Int? = null,
        shape: (Double) -> Double = { it },
    ): List<ToneWindow> {
        val played = SelfTestMath.toneSequence(rate)
        val r = Random(3)
        val gain = 32_767.0 * 10.0.pow(gainDb / 20.0)
        val n = SelfTestMath.WINDOW_SAMPLES
        val heard = ShortArray(played.size + delaySamples + n * 8)
        for (i in heard.indices) {
            val source = if (i - delaySamples in played.indices) played[i - delaySamples] * gain else 0.0
            var v = shape(source) + noise * (r.nextDouble() * 2 - 1)
            if (burstAtWindow != null && i / n == burstAtWindow) v += 9_000.0 * (r.nextDouble() * 2 - 1)
            heard[i] = v.toInt().coerceIn(-32_768, 32_767).toShort()
        }
        return (0 until heard.size / n).map { w ->
            val window = heard.copyOfRange(w * n, (w + 1) * n)
            ToneWindow(
                share = ToneProbe.share(window, n, SelfTestMath.TONE_HZ, rate),
                levelDb = ToneProbe.levelDbfs(window, n, SelfTestMath.TONE_HZ, rate),
                standoutDb = ToneProbe.standoutDb(window, n, SelfTestMath.TONE_HZ, rate),
            )
        }
    }

    /** A limiter: sound above the knee only grows a quarter as fast. */
    private fun limiterAbove(kneeDbfs: Double): (Double) -> Double {
        val knee = 32_767.0 * 10.0.pow(kneeDbfs / 20.0)
        return { v -> if (abs(v) > knee) sign(v) * (knee + (abs(v) - knee) * 0.25) else v }
    }

    @Test
    fun aStraightMicrophoneHearsTenDbStepsAllTheWayDown() {
        val result = SelfTestMath.evaluate(hear())
        assertEquals(SelfTestOutcome.READS_STRAIGHT, result.outcome)
        assertEquals(6, result.stepsHeard)
        assertEquals(50, result.rangeDb)
        assertEquals(0, result.squeezedLoudSteps)
        result.measuredDropsDb.forEach { assertEquals(10.0, it, 0.4) }
    }

    @Test
    fun theSpeakersOwnLoudnessDoesNotMatterOnlyTheSteps() {
        listOf(0.0, -10.0, -20.0).forEach { gain ->
            val result = SelfTestMath.evaluate(hear(gainDb = gain))
            assertEquals("gain $gain", SelfTestOutcome.READS_STRAIGHT, result.outcome)
            assertTrue("gain $gain range ${result.rangeDb}", result.rangeDb >= 40)
        }
    }

    @Test
    fun aSpeakerThatSqueezesItsLoudTonesStillPassesOnTheQuietOnes() {
        // The Fold on 9/29: loud steps read 6 dB, quieter ones came closer to 10.
        val result = SelfTestMath.evaluate(hear(shape = limiterAbove(-35.0)))
        assertEquals(SelfTestOutcome.READS_STRAIGHT, result.outcome)
        assertTrue("squeezed ${result.squeezedLoudSteps}", result.squeezedLoudSteps >= 1)
        assertTrue("range ${result.rangeDb}", result.rangeDb in 20..40)
        assertTrue(result.measuredDropsDb.first() < 7.5)
    }

    @Test
    fun aChainThatSqueezesEverythingIsNotConfirmed() {
        // every 10 dB step comes out as 5
        val squeezeAll: (Double) -> Double = { v -> sign(v) * 32_767.0 * (abs(v) / 32_767.0).pow(0.5) }
        val result = SelfTestMath.evaluate(hear(shape = squeezeAll))
        assertEquals(SelfTestOutcome.NOT_CONFIRMED, result.outcome)
        assertTrue(result.worstErrorDb > SelfTestMath.TOLERANCE_DB)
    }

    @Test
    fun aQuietToneInANoisyRoomIsStillFoundAmongItsNeighbours() {
        val result = SelfTestMath.evaluate(hear(noise = 400.0))
        assertEquals(SelfTestOutcome.READS_STRAIGHT, result.outcome)
        assertTrue("heard ${result.stepsHeard}", result.stepsHeard >= 4)
    }

    @Test
    fun aVeryLoudRoomLimitsTheRangeToWhatWasHeard() {
        val result = SelfTestMath.evaluate(hear(noise = 8_000.0))
        assertTrue("heard ${result.stepsHeard}", result.stepsHeard in 1..4)
        assertTrue(result.rangeDb <= 30)
    }

    @Test
    fun aBumpOrAVoiceInTheMiddleOfOneToneDoesNotSplitItInTwo() {
        // window 20 sits inside the second tone; on 9/29 a split tone was counted twice
        val result = SelfTestMath.evaluate(hear(burstAtWindow = 20))
        assertEquals(SelfTestOutcome.READS_STRAIGHT, result.outcome)
        assertEquals(6, result.stepsHeard)
        result.measuredDropsDb.forEach { assertEquals(10.0, it, 1.0) }
    }

    @Test
    fun silenceMeansTheToneWasNotHeard() {
        val silent = List(100) { ToneWindow(share = 0.0, levelDb = -120.0) }
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
    fun theProgressLineCountsOneToSix() {
        assertEquals(1, SelfTestMath.stepAt(0))
        assertEquals(1, SelfTestMath.stepAt(12))
        assertEquals(2, SelfTestMath.stepAt(20))
        assertEquals(6, SelfTestMath.stepAt(75))
        assertEquals(6, SelfTestMath.stepAt(95))
    }
}
