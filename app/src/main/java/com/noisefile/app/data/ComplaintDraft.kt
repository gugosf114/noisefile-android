package com.noisefile.app.data

import com.noisefile.app.model.Incident
import com.noisefile.app.model.RuleWorkflow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ComplaintDestination(
    val uri: String,
    val label: String,
    val isOnlineForm: Boolean,
    val isDocumentPacket: Boolean,
)

fun complaintDestination(rule: RuleWorkflow): ComplaintDestination {
    val actions = buildList {
        add(ComplaintAction(rule.actionUri, rule.actionLabel))
        if (rule.secondaryActionUri != null && rule.secondaryActionLabel != null) {
            add(ComplaintAction(rule.secondaryActionUri, rule.secondaryActionLabel))
        }
    }
    val selected = actions.firstOrNull { it.isOnlineForm }
        ?: actions.firstOrNull { it.isDocumentPacket }
        ?: actions.first()
    return ComplaintDestination(
        uri = selected.uri,
        label = selected.label,
        isOnlineForm = selected.isOnlineForm,
        isDocumentPacket = selected.isDocumentPacket,
    )
}

private data class ComplaintAction(
    val uri: String,
    val label: String,
) {
    val isDocumentPacket: Boolean =
        uri.startsWith("https://") &&
            (label.contains("packet", ignoreCase = true) ||
                label.contains("petition", ignoreCase = true))

    val isOnlineForm: Boolean =
        uri.startsWith("https://") &&
            !isDocumentPacket &&
            !label.contains("procedure", ignoreCase = true)
}

fun buildComplaintDraft(
    incident: Incident,
    rule: RuleWorkflow,
    zoneId: ZoneId = ZoneId.systemDefault(),
): String {
    val date = DateTimeFormatter
        .ofPattern("EEEE, MMMM d, yyyy 'at' h:mm a", Locale.US)
        .format(Instant.ofEpochMilli(incident.startedAtEpochMillis).atZone(zoneId))
    val description = incident.notes.ifBlank { "No additional description entered." }
    val levelSentence = incident.levelNote
        ?: "The sound levels above are estimates from my phone and are included as incident context."
    val baselineDb = incident.ambientDb
    val baselineSeconds = incident.ambientSeconds
    val baseline = if (baselineDb != null && baselineSeconds != null) {
        "\n        Quiet baseline at the same spot with the source silent: ${baselineDb.toInt()} dB over " +
            "${baselineSeconds / 60}:${"%02d".format(Locale.US, baselineSeconds % 60)}. " +
            "The disturbance averaged ${(incident.averageDb - baselineDb).toInt()} dB above that baseline " +
            "and peaked ${(incident.maximumDb - baselineDb).toInt()} dB above it. " +
            "Both readings are from the same phone, so its offset cancels out of the difference."
    } else {
        ""
    }

    return """
        Subject: Noise complaint — ${rule.noiseType.displayName} — ${rule.jurisdiction}

        I am reporting a ${rule.noiseType.displayName.lowercase(Locale.US)} disturbance.

        Location of disturbance: ${incident.location}
        Date and start time: $date
        Duration documented: ${incident.durationSeconds} seconds
        Impact: ${incident.impact}
        Description: $description
        Phone-estimated sound levels: ${incident.minimumDb.toInt()} dB minimum, ${incident.averageDb.toInt()} dB average, ${incident.maximumDb.toInt()} dB maximum

        $levelSentence$baseline

        City guidance: ${rule.title}
        ${rule.summary}

        Official source: ${rule.officialSourceLabel}
        ${rule.officialSourceUrl}
    """.trimIndent()
}
