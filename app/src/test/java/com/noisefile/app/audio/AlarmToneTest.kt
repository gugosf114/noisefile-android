package com.noisefile.app.audio

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class AlarmToneTest {
    private val rate = 48_000
    private val n = 3_840

    private fun tone(hz: Double, amplitude: Double = 8_000.0, noise: Double = 0.0, seed: Int = 1): ShortArray {
        val r = Random(seed)
        return ShortArray(n) { i -> (amplitude * sin(2 * PI * hz * i / rate) + noise * (r.nextDouble() * 2 - 1)).toInt().toShort() }
    }

    @Test
    fun aPiezoAlarmPitchIsHeard() {
        listOf(2_900.0, 3_100.0, 3_137.0, 3_400.0).forEach { hz ->
            val share = AlarmTone.toneShare(tone(hz), n, rate)
            assertTrue("$hz Hz share $share", share >= 0.8)
        }
    }

    @Test
    fun aLowFrequencySounderIsHeard() {
        assertTrue(AlarmTone.toneShare(tone(520.0), n, rate) >= 0.8)
    }

    @Test
    fun anAlarmInARoomWithSomeNoiseIsStillHeard() {
        assertTrue(AlarmTone.toneShare(tone(3_100.0, amplitude = 8_000.0, noise = 3_000.0), n, rate) >= AlarmTone.MIN_TONE_SHARE)
    }

    @Test
    fun loudRoomNoiseIsNotAnAlarm() {
        val r = Random(7)
        val noise = ShortArray(n) { (20_000 * (r.nextDouble() * 2 - 1)).toInt().toShort() }
        assertTrue(AlarmTone.toneShare(noise, n, rate) < 0.1)
    }

    @Test
    fun aVoiceLikePitchOutsideTheBandsIsNotAnAlarm() {
        listOf(200.0, 1_000.0, 2_000.0, 4_500.0).forEach { hz ->
            val share = AlarmTone.toneShare(tone(hz), n, rate)
            assertTrue("$hz Hz share $share", share < AlarmTone.MIN_TONE_SHARE)
        }
    }

    @Test
    fun silenceIsNotAnAlarm() {
        assertTrue(AlarmTone.toneShare(ShortArray(n), n, rate) == 0.0)
    }
}
