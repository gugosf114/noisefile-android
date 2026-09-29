package com.noisefile.app.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PeakClipKeeperTest {
    private fun window(value: Short, n: Int) = ShortArray(n) { value }

    @Test
    fun nothingHeardGivesNoClip() {
        assertNull(PeakClipKeeper(sampleRate = 100, clipSeconds = 10, beforeSeconds = 5).snapshot())
    }

    @Test
    fun theClipHoldsFiveSecondsBeforeThePeakAndFiveAfter() {
        val k = PeakClipKeeper(sampleRate = 100, clipSeconds = 10, beforeSeconds = 5)
        // 8 quiet seconds (value 1), then one loud second (value 9), then 7 more quiet seconds (value 2)
        repeat(8) { k.add(window(1, 100), 100, 30.0) }
        k.add(window(9, 100), 100, 80.0)
        repeat(7) { k.add(window(2, 100), 100, 30.0) }
        val clip = k.snapshot()!!
        assertEquals(1_000, clip.size)
        assertEquals(1.toShort(), clip[0])       // 5 s before the peak
        assertEquals(9.toShort(), clip[500])     // the peak second
        assertEquals(2.toShort(), clip[999])     // after it
        assertEquals(300L, k.clipStartSample)    // peak at second 8 → clip starts at second 3
    }

    @Test
    fun aLouderMomentLaterRestartsTheClipAroundIt() {
        val k = PeakClipKeeper(sampleRate = 100, clipSeconds = 10, beforeSeconds = 5)
        k.add(window(5, 100), 100, 60.0)
        repeat(20) { k.add(window(1, 100), 100, 30.0) }
        k.add(window(9, 100), 100, 90.0)
        repeat(3) { k.add(window(1, 100), 100, 30.0) }
        val clip = k.snapshot()!!
        assertEquals(1_600L, k.clipStartSample) // five seconds before the new peak at sample 2,100
        assertEquals(9.toShort(), clip[500])
        assertTrue(clip.size < 1_000) // the take ended 3 s after the peak: a short clip, not padded
        assertEquals(900, clip.size)
    }

    @Test
    fun aShortTakeStillGivesWhatItHeard() {
        val k = PeakClipKeeper(sampleRate = 100, clipSeconds = 10, beforeSeconds = 5)
        k.add(window(3, 100), 100, 50.0)
        k.add(window(4, 100), 100, 55.0)
        assertNotNull(k.snapshot())
        assertEquals(200, k.snapshot()!!.size)
    }

    @Test
    fun wavHeaderIsRightForMono16Bit() {
        val bytes = WavWriter.bytes(ShortArray(48_000) { 0 }, 48_000)
        assertEquals(44 + 96_000, bytes.size)
        assertEquals("RIFF", String(bytes, 0, 4))
        assertEquals("WAVE", String(bytes, 8, 4))
        assertEquals("data", String(bytes, 36, 4))
        val rate = (bytes[24].toInt() and 0xff) or (bytes[25].toInt() and 0xff shl 8) or (bytes[26].toInt() and 0xff shl 16)
        assertEquals(48_000, rate)
        assertEquals(1, bytes[22].toInt())
        assertEquals(16, bytes[34].toInt())
    }
}
