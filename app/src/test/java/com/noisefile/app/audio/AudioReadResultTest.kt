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
}
