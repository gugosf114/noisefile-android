package com.noisefile.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelTraceTest {
    @Test
    fun keepsTheHighestEstimateOfEachSecond() {
        val r = LevelTraceRecorder()
        r.add(0, 40.0); r.add(300, 55.4); r.add(900, 48.0)
        r.add(1_000, 61.0); r.add(1_500, 70.2)
        assertEquals(listOf(55, 70), r.snapshot())
        assertEquals(1, r.secondsPerSample)
    }

    @Test
    fun silentSecondsInTheMiddleAreZeroNotMissing() {
        val r = LevelTraceRecorder()
        r.add(0, 50.0); r.add(3_000, 52.0)
        assertEquals(listOf(50, 0, 0, 52), r.snapshot())
    }

    @Test
    fun aLongTakeFoldsInsteadOfGrowingForever() {
        val r = LevelTraceRecorder(maxPoints = 8)
        for (s in 0 until 20) r.add(s * 1_000L, 40.0 + s)
        assertTrue(r.snapshot().size <= 8)
        assertEquals(4, r.secondsPerSample)
        assertEquals(59, r.snapshot().max())
    }

    @Test
    fun resetStartsANewTake() {
        val r = LevelTraceRecorder()
        r.add(0, 50.0); r.reset(); r.add(0, 30.0)
        assertEquals(listOf(30), r.snapshot())
    }
}
