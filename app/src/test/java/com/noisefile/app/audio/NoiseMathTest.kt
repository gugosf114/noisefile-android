package com.noisefile.app.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoiseMathTest {
    @Test
    fun silenceReturnsZero() {
        assertEquals(0.0, NoiseMath.rmsToEstimatedDb(ShortArray(128)), 0.001)
    }

    @Test
    fun louderSignalProducesHigherReading() {
        val quiet = ShortArray(128) { 100 }
        val loud = ShortArray(128) { 5_000 }

        assertTrue(
            NoiseMath.rmsToEstimatedDb(loud) >
                NoiseMath.rmsToEstimatedDb(quiet),
        )
    }

    @Test
    fun readingIsClampedToDisplayRange() {
        val fullScale = ShortArray(128) { Short.MAX_VALUE }
        val reading = NoiseMath.rmsToEstimatedDb(fullScale)

        assertTrue(reading in 0.0..100.0)
    }
}
