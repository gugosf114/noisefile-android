package com.noisefile.app.data

import com.noisefile.app.model.Incident
import com.noisefile.app.model.quietDb
import com.noisefile.app.model.quietSeconds
import com.noisefile.app.model.quietWasLater
import java.time.Instant
import kotlin.math.roundToInt
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
        incident.soundKind?.let { appendLine("Sound: $it") }
        appendLine("Location: ${incident.location.ifBlank { "None added" }}")
        incident.room?.let { appendLine("Where the person stood: $it") }
        appendLine("Duration: ${incident.durationSeconds} sec")
        appendLine(
            "Levels: ${incident.averageDb.roundToInt()} dB avg / " +
                "${incident.maximumDb.roundToInt()} dB max",
        )
        val baselineDb = incident.quietDb
        val baselineSeconds = incident.quietSeconds
        if (baselineDb != null && baselineSeconds != null) {
            appendLine(
                "Quiet baseline (ambient)${if (incident.quietWasLater) ", measured later, same spot, source silent" else ""}: " +
                    "${baselineDb.roundToInt()} dB over $baselineSeconds sec at the same spot; " +
                    "disturbance ${(incident.averageDb - baselineDb).roundToInt()} dB above it on average, " +
                    "${(incident.maximumDb - baselineDb).roundToInt()} dB above at peak",
            )
        }
        appendLine("Impact: ${incident.impact}")
        incident.levelNote?.let { appendLine("Levels source: $it") }
        appendLine("Notes: ${incident.notes.ifBlank { "None added" }}")
        appendLine()
    }
}
