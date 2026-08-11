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
