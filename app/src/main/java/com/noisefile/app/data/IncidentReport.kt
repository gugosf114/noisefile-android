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
        appendLine("Duration: ${incident.durationSeconds} sec")
        appendLine(
            "Levels: ${incident.averageDb.toInt()} dB avg / " +
                "${incident.maximumDb.toInt()} dB max",
        )
        appendLine("Impact: ${incident.impact}")
        appendLine("Notes: ${incident.notes.ifBlank { "None added" }}")
        appendLine()
    }
}
