package com.noisefile.app.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioReadResultTest {
    @Test
    fun positiveSampleCountIsProcessed() {
        assertEquals(AudioReadAction.PROCESS, classifyAudioReadResult(256))
    }

    @Test
    fun emptyReadCanBeRetried() {
        assertEquals(AudioReadAction.RETRY, classifyAudioReadResult(0))
    }

    @Test
    fun microphoneReadErrorStopsMeasurement() {
        assertEquals(AudioReadAction.FAIL, classifyAudioReadResult(-1))
        assertEquals(AudioReadAction.FAIL, classifyAudioReadResult(-6))
    }

    @Test
    fun rejectedUsbPreferenceStopsMeasurement() {
        assertEquals(false, usbPreferenceAccepted(hasUsbInput = true, preferenceAccepted = false))
        assertEquals(true, usbPreferenceAccepted(hasUsbInput = true, preferenceAccepted = true))
        assertEquals(true, usbPreferenceAccepted(hasUsbInput = false, preferenceAccepted = false))
    }

    @Test
    fun usbReadingRequiresTheRequestedMicrophoneToBeTheRealInput() {
        assertEquals(true, isExpectedUsbRoute(expectedUsbDeviceId = 17, routedDeviceId = 17))
        assertEquals(false, isExpectedUsbRoute(expectedUsbDeviceId = 17, routedDeviceId = 23))
        assertEquals(false, isExpectedUsbRoute(expectedUsbDeviceId = 17, routedDeviceId = null))
        assertEquals(true, isExpectedUsbRoute(expectedUsbDeviceId = null, routedDeviceId = null))
    }
}
