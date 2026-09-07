package com.noisefile.app.data

import com.noisefile.app.model.Incident
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

fun buildIncidentHistoryReport(
    incidents: List<Incident>,
    generatedAtLabel: String = Date().toString(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = buildString {
    appendLine("NOISEFILE INCIDENT LOG")
    appendLine("Generated on: $generatedAtLabel")
    appendLine("Total incidents: ${incidents.size}")
    appendLine()
    incidents.forEachIndexed { index, incident ->
        val date = DateTimeFormatter
            .ofPattern("EEE, MMM d, yyyy · h:mm a", Locale.US)
            .format(Instant.ofEpochMilli(incident.startedAtEpochMillis).atZone(zoneId))
        appendLine("Incident ${index + 1}")
        appendLine("Date: $date")
        appendLine("Type: ${incident.noiseType.displayName}")
        appendLine("Location: ${incident.location.ifBlank { "None added" }}")
        appendLine("Duration: ${incident.durationSeconds} sec")
        appendLine(
            "Levels: ${incident.averageDb.toInt()} dB avg / " +
                "${incident.maximumDb.toInt()} dB max",
        )
        val baselineDb = incident.ambientDb
        val baselineSeconds = incident.ambientSeconds
        if (baselineDb != null && baselineSeconds != null) {
            appendLine(
                "Quiet baseline: ${baselineDb.toInt()} dB over $baselineSeconds sec at the same spot; " +
                    "disturbance ${(incident.averageDb - baselineDb).toInt()} dB above it on average, " +
                    "${(incident.maximumDb - baselineDb).toInt()} dB above at peak",
            )
        }
        appendLine("Impact: ${incident.impact}")
        incident.levelNote?.let { appendLine("Levels source: $it") }
        appendLine("Notes: ${incident.notes.ifBlank { "None added" }}")
        appendLine()
    }
}
