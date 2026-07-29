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
    fun readingIsClampedToDisplayRange() {
        val fullScale = DoubleArray(128) { Short.MAX_VALUE.toDouble() }
        val reading = NoiseMath.rmsToEstimatedDbA(fullScale)

        assertTrue(reading in 0.0..100.0)
    }
}
