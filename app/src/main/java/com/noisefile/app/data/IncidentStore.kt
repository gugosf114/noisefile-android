package com.noisefile.app.data

import android.content.Context
import com.noisefile.app.model.Incident
import com.noisefile.app.model.NoiseType
import org.json.JSONArray
import org.json.JSONObject

class IncidentStore(context: Context) {
    private val preferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun load(): List<Incident> {
        val raw = preferences.getString(KEY_INCIDENTS, null) ?: return emptyList()
        return parseIncidents(raw)
    }

    @Synchronized
    fun add(incident: Incident): List<Incident> {
        val incidents = (listOf(incident) + load())
            .distinctBy { it.id }
            .take(MAX_INCIDENTS)
        persist(incidents)
        return incidents
    }

    @Synchronized
    fun updateDetails(
        incidentId: Long,
        location: String,
        notes: String,
    ): List<Incident> {
        val incidents = load().map { incident ->
            if (incident.id == incidentId) {
                incident.copy(
                    location = location.trim(),
                    notes = notes.trim(),
                )
            } else {
                incident
            }
        }
        persist(incidents)
        return incidents
    }

    private fun persist(incidents: List<Incident>) {
        val array = JSONArray()
        incidents.forEach { array.put(it.toJson()) }
        preferences.edit().putString(KEY_INCIDENTS, array.toString()).apply()
    }

    private fun Incident.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("ruleId", ruleId)
        .put("noiseType", noiseType.name)
        .put("startedAtEpochMillis", startedAtEpochMillis)
        .put("durationSeconds", durationSeconds)
        .put("minimumDb", minimumDb)
        .put("averageDb", averageDb)
        .put("maximumDb", maximumDb)
        .put("location", location)
        .put("impact", impact)
        .put("notes", notes)
        .put("ambientDb", ambientDb ?: JSONObject.NULL)
        .put("ambientSeconds", ambientSeconds ?: JSONObject.NULL)
        .put("levelNote", levelNote ?: JSONObject.NULL)

    private companion object {
        const val PREFERENCES_NAME = "noisefile_incidents"
        const val KEY_INCIDENTS = "incidents"
        const val MAX_INCIDENTS = 500
    }
}

internal fun parseIncidents(raw: String): List<Incident> {
    val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
    return buildList {
        for (index in 0 until array.length()) {
            runCatching {
                array.getJSONObject(index).toIncident()
            }.getOrNull()?.let(::add)
        }
    }.sortedByDescending { it.startedAtEpochMillis }
}

private fun JSONObject.toIncident(): Incident = Incident(
    id = getLong("id"),
    ruleId = getString("ruleId"),
    noiseType = NoiseType.valueOf(getString("noiseType")),
    startedAtEpochMillis = getLong("startedAtEpochMillis"),
    durationSeconds = getLong("durationSeconds"),
    minimumDb = getDouble("minimumDb"),
    averageDb = getDouble("averageDb"),
    maximumDb = getDouble("maximumDb"),
    location = optString("location"),
    impact = getString("impact"),
    notes = optString("notes"),
    ambientDb = if (has("ambientDb") && !isNull("ambientDb")) getDouble("ambientDb") else null,
    ambientSeconds = if (has("ambientSeconds") && !isNull("ambientSeconds")) getLong("ambientSeconds") else null,
    levelNote = if (has("levelNote") && !isNull("levelNote")) getString("levelNote") else null,
)
