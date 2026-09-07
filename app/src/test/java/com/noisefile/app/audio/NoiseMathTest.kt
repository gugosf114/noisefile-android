package com.noisefile.app.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoiseMathTest {
    @Test
    fun silenceReturnsZero() {
        assertEquals(0.0, NoiseMath.rmsToEstimatedDbA(DoubleArray(128)), 0.001)
    }

    @Test
    fun louderSignalProducesHigherReading() {
        val quiet = DoubleArray(128) { 100.0 }
        val loud = DoubleArray(128) { 5_000.0 }

        assertTrue(
            NoiseMath.rmsToEstimatedDbA(loud) >
                NoiseMath.rmsToEstimatedDbA(quiet),
        )
    }

    @Test
    fun unprocessedPathMapsTheCddReferenceToneToNinetyFourDb() {
        // CDD 5.11 [C-1-5]: 94 dB SPL at 1 kHz -> RMS 520 on 16-bit samples (-36 dBFS).
        val referenceTone = DoubleArray(4096) { index ->
            520.0 * Math.sqrt(2.0) * Math.sin(2.0 * Math.PI * index / 48.0)
        }
        val reading = NoiseMath.rmsToEstimatedDbA(
            referenceTone,
            offsetDb = NoiseMath.CDD_UNPROCESSED_OFFSET_DBA,
        )
        assertEquals(94.0, reading, 0.2)
    }

    @Test
    fun estimatePathStillUsesTheOldDefaultOffset() {
        val referenceTone = DoubleArray(4096) { index ->
            520.0 * Math.sqrt(2.0) * Math.sin(2.0 * Math.PI * index / 48.0)
        }
        val reading = NoiseMath.rmsToEstimatedDbA(referenceTone)
        assertEquals(94.0 - 40.0, reading, 0.2)
    }

    @Test
    fun sensitivityFromMicrophoneInfoBecomesAnOffset() {
        assertEquals(130.0, NoiseMath.offsetFromSensitivity(-36.0), 0.001)
        assertEquals(120.0, NoiseMath.offsetFromSensitivity(-26.0), 0.001)
    }

    @Test
    fun readingIsClampedToDisplayRange() {
        val fullScale = DoubleArray(128) { Short.MAX_VALUE.toDouble() }
        val reading = NoiseMath.rmsToEstimatedDbA(fullScale)

        assertTrue(reading in 0.0..NoiseMath.MAX_DISPLAY_DB)
    }
}
