package com.noisefile.app.data

import com.noisefile.app.model.FormAnswer
import com.noisefile.app.model.FormGuide
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
    /** A city mailbox: the complaint goes in the email body, filled in. */
    val isEmail: Boolean = false,
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
        ?: actions.firstOrNull { it.isEmail }
        ?: actions.first()
    return ComplaintDestination(
        uri = selected.uri,
        label = selected.label,
        isOnlineForm = selected.isOnlineForm,
        isDocumentPacket = selected.isDocumentPacket,
        isEmail = selected.isEmail,
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

    val isEmail: Boolean = uri.startsWith("mailto:")
}

/**
 * A mailto link with the subject and the short complaint already in the body,
 * ending with lines for the user's own name, phone and address.
 */
fun buildComplaintEmailUri(
    incident: Incident,
    rule: RuleWorkflow,
    mailto: String,
    zoneId: ZoneId = ZoneId.systemDefault(),
): String {
    val address = mailto.removePrefix("mailto:").substringBefore('?')
    val subject = buildComplaintTitle(incident, rule, zoneId)
    val body = buildComplaintDraft(incident, rule, zoneId, maxChars = 2_000) +
        "\n\nMy name:\nMy phone:\nMy address:\n"
    fun enc(text: String) = java.net.URLEncoder.encode(text, "UTF-8").replace("+", "%20")
    return "mailto:$address?subject=${enc(subject)}&body=${enc(body)}"
}

/**
 * The complaint as the city form wants it: short, the facts, sized to fit a box.
 * The rule text, the code quote and the sources stay in the PDF report.
 */
fun buildComplaintDraft(
    incident: Incident,
    rule: RuleWorkflow,
    zoneId: ZoneId = ZoneId.systemDefault(),
    maxChars: Int = 1_000,
): String {
    val start = Instant.ofEpochMilli(incident.startedAtEpochMillis).atZone(zoneId)
    val end = start.plusSeconds(incident.durationSeconds)
    val date = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.US).format(start)
    val clock = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    val minutes = (incident.durationSeconds / 60).coerceAtLeast(if (incident.durationSeconds > 0) 1 else 0)
    val kind = when (rule.noiseType) {
        com.noisefile.app.model.NoiseType.BARKING_DOG -> "Animal noise"
        com.noisefile.app.model.NoiseType.PARTY_MUSIC -> "Loud noise"
        com.noisefile.app.model.NoiseType.CONSTRUCTION -> "Construction or mechanical noise"
    }
    val baseline = incident.ambientDb?.let { quiet ->
        " Quiet at the same spot with the source silent: ${quiet.toInt()} dB, so the noise peaked " +
            "${(incident.maximumDb - quiet).toInt()} dB above quiet."
    } ?: ""
    val attachments = buildList {
        if (incident.photoNames.isNotEmpty()) add("${incident.photoNames.size} photo${if (incident.photoNames.size > 1) "s" else ""}")
        if (incident.clipName != null) add("a ${incident.clipSeconds}-second audio clip")
    }
    val proof = "I have a sealed PDF report with a second-by-second level trace" +
        (if (attachments.isEmpty()) "" else ", " + attachments.joinToString(" and ")) +
        ", available on request."
    val notes = incident.notes.trim()

    val levelNote = incident.levelNote?.trim()?.trimEnd('.')

    fun compose(withNotes: Boolean, withLevelNote: Boolean, withProof: Boolean) = buildString {
        append("$kind from ${incident.location.trim()}. ")
        append("$date, ${clock.format(start)} to ${clock.format(end)} ($minutes min). ")
        append("Phone estimate at my spot: highest ${incident.maximumDb.toInt()} dB, average ${incident.averageDb.toInt()} dB.")
        append(baseline)
        if (withLevelNote && !levelNote.isNullOrEmpty()) append(" $levelNote.")
        append(" Impact: ${incident.impact.trim().trimEnd('.')}.")
        if (withNotes && notes.isNotEmpty()) {
            append(" $notes")
            if (!notes.endsWith(".")) append(".")
        }
        if (withProof) append(" $proof")
    }
    // Drop the notes first, then the microphone note, then the proof line, then cut.
    return listOf(
        compose(withNotes = true, withLevelNote = true, withProof = true),
        compose(withNotes = false, withLevelNote = true, withProof = true),
        compose(withNotes = false, withLevelNote = false, withProof = true),
        compose(withNotes = false, withLevelNote = false, withProof = false),
    ).firstOrNull { it.length <= maxChars }
        ?: (compose(withNotes = false, withLevelNote = false, withProof = false).take(maxChars - 1).trimEnd() + "\u2026")
}

/** One line for a "title" box. */
fun buildComplaintTitle(incident: Incident, rule: RuleWorkflow, zoneId: ZoneId = ZoneId.systemDefault()): String {
    val start = Instant.ofEpochMilli(incident.startedAtEpochMillis).atZone(zoneId)
    val kind = when (rule.noiseType) {
        com.noisefile.app.model.NoiseType.BARKING_DOG -> "Barking / animal noise"
        com.noisefile.app.model.NoiseType.PARTY_MUSIC -> "Loud noise"
        com.noisefile.app.model.NoiseType.CONSTRUCTION -> "Construction noise"
    }
    return "$kind at ${incident.location.trim()}, " +
        DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.US).format(start)
}

/** A ready answer for one box on the city form. [text] is null when only the user knows it. */
data class FormAnswerRow(
    val label: String,
    val required: Boolean,
    val maxLength: Int?,
    val text: String?,
    val hint: String?,
    /** True when the answer is a choice or a checkbox on the form, not words to paste. */
    val isChoice: Boolean,
)

fun formAnswers(
    incident: Incident,
    rule: RuleWorkflow,
    guide: FormGuide,
    zoneId: ZoneId = ZoneId.systemDefault(),
): List<FormAnswerRow> {
    val start = Instant.ofEpochMilli(incident.startedAtEpochMillis).atZone(zoneId)
    val end = start.plusSeconds(incident.durationSeconds)
    val clock = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    val minutes = (incident.durationSeconds / 60).coerceAtLeast(if (incident.durationSeconds > 0) 1 else 0)
    return guide.fields.map { field ->
        val text: String? = when (field.answer) {
            FormAnswer.TITLE -> buildComplaintTitle(incident, rule, zoneId)
            FormAnswer.DESCRIPTION -> buildComplaintDraft(incident, rule, zoneId, field.maxLength ?: 1_000)
            FormAnswer.ADDRESS -> incident.location.trim()
            FormAnswer.DATE -> DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.US).format(start)
            FormAnswer.START_TIME -> clock.format(start)
            FormAnswer.END_TIME -> clock.format(end)
            FormAnswer.TIME_RANGE -> "${clock.format(start)} to ${clock.format(end)}"
            FormAnswer.DAY -> DateTimeFormatter.ofPattern("EEEE", Locale.US).format(start)
            FormAnswer.DURATION -> "$minutes min"
            FormAnswer.IMPACT -> incident.impact.trim()
            FormAnswer.PHOTOS -> when (incident.photoNames.size) {
                0 -> null
                1 -> "1 photo is on this incident and in the PDF. The original is still in your gallery."
                else -> "${incident.photoNames.size} photos are on this incident and in the PDF. The originals are still in your gallery."
            }
            FormAnswer.FIXED -> field.fixedText
            FormAnswer.YOURS -> null
        }
        val cut = if (text != null && field.maxLength != null && text.length > field.maxLength) {
            text.take(field.maxLength - 1).trimEnd() + "\u2026"
        } else {
            text
        }
        FormAnswerRow(
            label = field.label,
            required = field.required,
            maxLength = field.maxLength,
            text = cut,
            hint = field.hint ?: when (field.answer) {
                FormAnswer.YOURS -> "Only you know this. Type it in."
                FormAnswer.PHOTOS -> if (incident.photoNames.isEmpty()) "No photos on this incident." else null
                else -> null
            },
            isChoice = field.answer == FormAnswer.FIXED,
        )
    }
}
