package com.noisefile.app.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalibrationMathTest {
    @Test
    fun theOffsetMakesThePhoneReadWhatTheMeterRead() {
        // Phone averaged 58 with no offset; the meter said 62 -> +4.
        assertEquals(4.0, CalibrationMath.newUserOffset(0.0, 58.0, 62.0), 0.001)
        // A second calibration builds on the first: reading already includes +4, meter now says 61.
        assertEquals(3.0, CalibrationMath.newUserOffset(4.0, 62.0, 61.0), 0.001)
        // Phone reads high: negative offset.
        assertEquals(-6.0, CalibrationMath.newUserOffset(0.0, 70.0, 64.0), 0.001)
    }

    @Test
    fun onlyPlausibleMeterReadingsAreAccepted() {
        assertTrue(CalibrationMath.isPlausibleReference(45.0))
        assertTrue(CalibrationMath.isPlausibleReference(30.0))
        assertTrue(CalibrationMath.isPlausibleReference(120.0))
        assertFalse(CalibrationMath.isPlausibleReference(12.0))
        assertFalse(CalibrationMath.isPlausibleReference(140.0))
    }

    @Test
    fun offsetsReadWithTheirSign() {
        assertEquals("+4 dB", CalibrationMath.signed(3.6))
        assertEquals("-6 dB", CalibrationMath.signed(-5.5))
        assertEquals("+0 dB", CalibrationMath.signed(0.2))
    }
}
