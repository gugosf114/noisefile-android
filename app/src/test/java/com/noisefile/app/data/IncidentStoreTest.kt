package com.noisefile.app.data

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class IncidentStoreTest {
    @Test
    fun corruptEntryDoesNotHideValidHistory() {
        val raw = JSONArray()
            .put(incidentJson(id = 1L, startedAt = 100L))
            .put(JSONObject().put("id", "damaged"))
            .put(incidentJson(id = 2L, startedAt = 200L))
            .toString()

        val incidents = parseIncidents(raw)

        assertEquals(listOf(2L, 1L), incidents.map { it.id })
    }

    @Test
    fun damagedRootReturnsEmptyHistory() {
        assertEquals(emptyList<com.noisefile.app.model.Incident>(), parseIncidents("not json"))
    }

    @Test
    fun traceAndSealFieldsRoundTripAndOldEntriesDefault() {
        val sealed = incidentJson(id = 3L, startedAt = 300L)
            .put("levelTrace", JSONArray().put(40).put(55).put(68))
            .put("traceSecondsPerSample", 2)
            .put("evidenceHash", "abc")
            .put("previousHash", "GENESIS")
        val incidents = parseIncidents(JSONArray().put(sealed).put(incidentJson(id = 1L, startedAt = 100L)).toString())
        assertEquals(listOf(40, 55, 68), incidents[0].levelTrace)
        assertEquals(2, incidents[0].traceSecondsPerSample)
        assertEquals("abc", incidents[0].evidenceHash)
        assertEquals(emptyList<Int>(), incidents[1].levelTrace)
        assertEquals(1, incidents[1].traceSecondsPerSample)
        assertEquals(null, incidents[1].evidenceHash)
    }

    @Test
    fun soundKindRoundTripsAndOldEntriesHaveNone() {
        val withKind = incidentJson(id = 5L, startedAt = 500L).put("soundKind", "Hammering or banging")
        val incidents = parseIncidents(JSONArray().put(withKind).put(incidentJson(id = 1L, startedAt = 100L)).toString())
        assertEquals("Hammering or banging", incidents[0].soundKind)
        assertEquals(null, incidents[1].soundKind)
        val back = parseIncidents(JSONArray().put(incidents[0].toJson()).toString())
        assertEquals("Hammering or banging", back[0].soundKind)
    }

    private fun incidentJson(id: Long, startedAt: Long): JSONObject = JSONObject()
        .put("id", id)
        .put("ruleId", "san-jose-party_music")
        .put("noiseType", "PARTY_MUSIC")
        .put("startedAtEpochMillis", startedAt)
        .put("durationSeconds", 45L)
        .put("minimumDb", 41.2)
        .put("averageDb", 58.7)
        .put("maximumDb", 67.9)
        .put("location", "440 Price Avenue")
        .put("impact", "Interrupted rest or quiet use")
        .put("notes", "Bass was shaking the window.")
}
