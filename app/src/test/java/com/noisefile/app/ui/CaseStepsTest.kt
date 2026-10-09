package com.noisefile.app.ui

import com.noisefile.app.model.Incident
import com.noisefile.app.model.NoiseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class CaseStepsTest {
    private fun take(id: Long, filedAt: Long? = null) = Incident(
        id = id, ruleId = "x-party_music", noiseType = NoiseType.PARTY_MUSIC, startedAtEpochMillis = id * 1000,
        durationSeconds = 10, minimumDb = 30.0, averageDb = 50.0, maximumDb = 60.0, location = "a", impact = "b", notes = "",
        filedAtEpochMillis = filedAt,
    )

    @Test
    fun aFreshPhoneStartsAtBaseline() {
        val s = CaseSteps.of(baselineDone = false, incidents = emptyList())
        assertEquals(0, s.nextIndex)
        assertEquals("Next: measure the quiet in your room, on a quiet night.", s.nextLine(ZoneOffset.UTC))
    }

    @Test
    fun aBaselineMovesTheNextStepToRecord() {
        val s = CaseSteps.of(true, emptyList())
        assertTrue(s.baselineDone); assertFalse(s.recordDone); assertEquals(1, s.nextIndex)
        assertEquals("Next: press Record when the noise starts.", s.nextLine(ZoneOffset.UTC))
    }

    @Test
    fun openIncidentsMeanRecordIsDoneAndFileIsNext() {
        val s = CaseSteps.of(true, listOf(take(1), take(2), take(3)))
        assertTrue(s.recordDone); assertFalse(s.fileDone); assertEquals(2, s.nextIndex); assertEquals(3, s.openCount)
        assertEquals("Next: file your 3 incidents, then mark them filed.", s.nextLine(ZoneOffset.UTC))
    }

    @Test
    fun markingFiledResetsTheCaseAndSaysSo() {
        val s = CaseSteps.of(true, listOf(take(1, filedAt = 86_400_000L)))
        assertTrue(s.recordDone); assertTrue(s.fileDone); assertEquals(1, s.nextIndex)
        assertEquals("Filed Jan 2. Ready for the next one.", s.nextLine(ZoneOffset.UTC))
    }

    @Test
    fun aNewIncidentAfterAFiledOneOpensANewCase() {
        val s = CaseSteps.of(true, listOf(take(1, filedAt = 5L), take(2)))
        assertTrue(s.recordDone); assertFalse(s.fileDone); assertEquals(1, s.openCount)
    }
}
