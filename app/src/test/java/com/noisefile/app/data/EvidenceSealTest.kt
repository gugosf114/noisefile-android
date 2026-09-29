package com.noisefile.app.data

import com.noisefile.app.model.Incident
import com.noisefile.app.model.NoiseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EvidenceSealTest {
    private fun take(id: Long, maxDb: Double = 67.9, notes: String = "Bass.") = Incident(
        id = id, ruleId = "san-jose-party_music", noiseType = NoiseType.PARTY_MUSIC,
        startedAtEpochMillis = id * 1_000L, durationSeconds = 45L,
        minimumDb = 41.2, averageDb = 58.7, maximumDb = maxDb,
        location = "440 Price Avenue", impact = "Interrupted rest or quiet use", notes = notes,
        levelTrace = listOf(40, 55, 68, 60),
    )

    private fun chain(vararg takes: Incident): List<Incident> {
        val out = ArrayList<Incident>()
        takes.forEach { out.add(EvidenceSeal.seal(it, out.lastOrNull())) }
        return out
    }

    @Test
    fun sameFactsGiveTheSameSeal() {
        assertEquals(EvidenceSeal.hash(take(1), EvidenceSeal.GENESIS), EvidenceSeal.hash(take(1), EvidenceSeal.GENESIS))
        assertEquals(64, EvidenceSeal.hash(take(1), EvidenceSeal.GENESIS).length)
    }

    @Test
    fun anIntactChainVerifies() {
        val report = EvidenceSeal.verify(chain(take(1), take(2), take(3)).shuffled())
        assertTrue(report.intact)
        assertEquals(3, report.sealedCount)
        assertNull(report.brokenAtId)
    }

    @Test
    fun editingAMeasuredNumberBreaksTheSeal() {
        val takes = chain(take(1), take(2), take(3))
        val edited = takes.map { if (it.id == 2L) it.copy(maximumDb = 90.0) else it }
        val report = EvidenceSeal.verify(edited)
        assertFalse(report.intact)
        assertEquals(2L, report.brokenAtId)
    }

    @Test
    fun removingATakeBreaksTheLinkOfTheNextOne() {
        val takes = chain(take(1), take(2), take(3))
        val report = EvidenceSeal.verify(takes.filter { it.id != 2L })
        assertFalse(report.intact)
        assertEquals(3L, report.brokenAtId)
    }

    @Test
    fun theUsersOwnWordsStayOutsideTheSeal() {
        val takes = chain(take(1), take(2))
        val noted = takes.map { if (it.id == 2L) it.copy(notes = "Added later.", location = "12 Westlake Ave") else it }
        assertTrue(EvidenceSeal.verify(noted).intact)
    }

    @Test
    fun takesSavedBeforeSealingAreCountedNotJudged() {
        val old = take(1)
        val sealed = EvidenceSeal.seal(take(2), null)
        val report = EvidenceSeal.verify(listOf(old, sealed))
        assertTrue(report.intact)
        assertEquals(1, report.unsealedCount)
        assertEquals(1, report.sealedCount)
    }

    @Test
    fun aChangedClipOrPhotoChangesTheSeal() {
        val base = take(1)
        val h0 = EvidenceSeal.hash(base, EvidenceSeal.GENESIS)
        assertNotEquals(h0, EvidenceSeal.hash(base.copy(clipHash = "abc", clipSeconds = 10), EvidenceSeal.GENESIS))
        assertNotEquals(h0, EvidenceSeal.hash(base.copy(photoHashes = listOf("p1")), EvidenceSeal.GENESIS))
    }

    @Test
    fun differentTracesGiveDifferentSeals() {
        assertNotEquals(
            EvidenceSeal.hash(take(1), EvidenceSeal.GENESIS),
            EvidenceSeal.hash(take(1).copy(levelTrace = listOf(40, 55, 68, 61)), EvidenceSeal.GENESIS),
        )
    }
}
