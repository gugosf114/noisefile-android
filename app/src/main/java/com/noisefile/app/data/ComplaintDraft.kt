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
)

fun complaintDestination(rule: RuleWorkflow): ComplaintDestination {
    val actions = buildList {
        add(rule.actionUri to rule.actionLabel)
        if (rule.secondaryActionUri != null && rule.secondaryActionLabel != null) {
            add(rule.secondaryActionUri to rule.secondaryActionLabel)
        }
    }
    val selected = actions.firstOrNull { (uri, _) -> uri.startsWith("https://") }
        ?: actions.first()
    return ComplaintDestination(
        uri = selected.first,
        label = selected.second,
        isOnlineForm = selected.first.startsWith("https://"),
    )
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

    return """
        Subject: Noise complaint — ${rule.noiseType.displayName} — ${rule.jurisdiction}

        I am reporting a ${rule.noiseType.displayName.lowercase(Locale.US)} disturbance.

        Location of disturbance: ${incident.location}
        Date and start time: $date
        Duration documented: ${incident.durationSeconds} seconds
        Impact: ${incident.impact}
        Description: $description
        Phone-estimated sound levels: ${incident.minimumDb.toInt()} dB minimum, ${incident.averageDb.toInt()} dB average, ${incident.maximumDb.toInt()} dB maximum

        The sound levels above are estimates from my phone and are included as incident context.

        City guidance: ${rule.title}
        ${rule.summary}

        Official source: ${rule.officialSourceLabel}
        ${rule.officialSourceUrl}
    """.trimIndent()
}
