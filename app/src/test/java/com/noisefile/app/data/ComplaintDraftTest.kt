package com.noisefile.app.data

import com.noisefile.app.model.FormAnswer
import com.noisefile.app.model.FormField
import com.noisefile.app.model.FormGuide
import com.noisefile.app.model.Incident
import com.noisefile.app.model.NoiseType
import com.noisefile.app.model.RuleWorkflow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class ComplaintDraftTest {
    @Test
    fun draftUsesSavedIncidentAndVerifiedRuleFacts() {
        val draft = buildComplaintDraft(
            incident = incident(),
            rule = rule(),
            zoneId = ZoneOffset.UTC,
        )

        assertTrue(draft, draft.startsWith("Loud noise from 440 Price Avenue."))
        assertTrue(draft, draft.contains("Thursday, January 1, 1970, 12:00 AM to 12:00 AM (1 min)."))
        assertTrue(draft, draft.contains("highest 67 dB, average 58 dB"))
        assertTrue(draft, draft.contains("Impact: Interrupted rest or quiet use."))
        assertTrue(draft, draft.contains("Bass was vibrating the bedroom window."))
        assertTrue(draft, draft.contains("Phone estimate at my spot"))
        assertTrue(draft, draft.endsWith("available on request."))
        // The rule text and the sources belong to the PDF, not the form box.
        assertFalse(draft.contains("Official source"))
        assertFalse(draft.contains("https://"))
        assertFalse(draft.contains("violation occurred", ignoreCase = true))
        assertTrue(draft, draft.length < 500)
    }

    @Test
    fun draftShrinksToFitTheFormBox() {
        val longNotes = incident().copy(notes = "x".repeat(300))
        val full = buildComplaintDraft(longNotes, rule(), ZoneOffset.UTC)
        assertTrue(full.contains("xxxx"))

        val fitted = buildComplaintDraft(longNotes, rule(), ZoneOffset.UTC, maxChars = 200)
        assertTrue(fitted, fitted.length <= 200)
        assertFalse(fitted.contains("xxxx"))
        assertTrue(fitted, fitted.contains("Impact:"))

        val tiny = buildComplaintDraft(longNotes, rule(), ZoneOffset.UTC, maxChars = 60)
        assertTrue(tiny, tiny.length <= 60)
        assertTrue(tiny, tiny.endsWith("\u2026"))
    }

    @Test
    fun formAnswersFollowTheGuide() {
        val guide = FormGuide(
            portal = "Example 311",
            access = "No sign-in needed.",
            steps = listOf("Details", "Contact"),
            fields = listOf(
                FormField(label = "Issue Title", required = true, answer = FormAnswer.TITLE),
                FormField(label = "Description", maxLength = 120, answer = FormAnswer.DESCRIPTION),
                FormField(label = "Address", required = true, answer = FormAnswer.ADDRESS),
                FormField(label = "Date", answer = FormAnswer.DATE),
                FormField(label = "Times", answer = FormAnswer.TIME_RANGE),
                FormField(label = "Category", answer = FormAnswer.FIXED, fixedText = "Code Enforcement"),
                FormField(label = "Your name", required = true, answer = FormAnswer.YOURS),
                FormField(label = "Photo", answer = FormAnswer.PHOTOS),
            ),
        )
        val rows = formAnswers(incident(), rule(), guide, ZoneOffset.UTC)

        assertEquals("Loud noise at 440 Price Avenue, Jan 1, 12:00 AM", rows[0].text)
        assertTrue(rows[1].text!!, rows[1].text!!.length <= 120)
        assertEquals("440 Price Avenue", rows[2].text)
        assertEquals("01/01/1970", rows[3].text)
        assertEquals("12:00 AM to 12:00 AM", rows[4].text)
        assertEquals("Code Enforcement", rows[5].text)
        assertTrue(rows[5].isChoice)
        assertEquals(null, rows[6].text)
        assertEquals("Only you know this. Type it in.", rows[6].hint)
        assertTrue(rows[6].required)
        assertEquals(null, rows[7].text)
        assertEquals("No photos on this incident.", rows[7].hint)
    }

    @Test
    fun onlineFormIsPreferredOverPrimaryPhoneRoute() {
        val destination = complaintDestination(
            rule(
                actionUri = "tel:311",
                secondaryActionUri = "https://city.example.gov/noise-complaint",
            ),
        )

        assertTrue(destination.isOnlineForm)
        assertFalse(destination.isDocumentPacket)
        assertEquals("https://city.example.gov/noise-complaint", destination.uri)
    }

    @Test
    fun phoneRouteIsUsedWhenNoOnlineFormExists() {
        val destination = complaintDestination(
            rule(
                actionUri = "tel:311",
                secondaryActionUri = null,
            ),
        )

        assertFalse(destination.isOnlineForm)
        assertFalse(destination.isDocumentPacket)
        assertEquals("tel:311", destination.uri)
    }

    @Test
    fun requiredDocumentPacketIsNotCalledAnOnlineForm() {
        val destination = complaintDestination(
            rule(
                actionUri = "tel:311",
                secondaryActionUri = "https://city.example.gov/animal-petition",
                secondaryActionLabel = "Open Five-Incident Petition",
            ),
        )

        assertFalse(destination.isOnlineForm)
        assertTrue(destination.isDocumentPacket)
        assertEquals("https://city.example.gov/animal-petition", destination.uri)
    }

    @Test
    fun informationPageDoesNotReplacePrimaryComplaintContact() {
        val destination = complaintDestination(
            rule(
                actionUri = "tel:311",
                secondaryActionUri = "https://city.example.gov/barking-procedure",
                secondaryActionLabel = "Open Official Barking Procedure",
            ),
        )

        assertFalse(destination.isOnlineForm)
        assertFalse(destination.isDocumentPacket)
        assertEquals("tel:311", destination.uri)
    }

    private fun incident() = Incident(
        id = 1L,
        ruleId = "example-party",
        noiseType = NoiseType.PARTY_MUSIC,
        startedAtEpochMillis = 0L,
        durationSeconds = 45L,
        minimumDb = 41.2,
        averageDb = 58.7,
        maximumDb = 67.9,
        location = "440 Price Avenue",
        impact = "Interrupted rest or quiet use",
        notes = "Bass was vibrating the bedroom window.",
    )

    private fun rule(
        actionUri: String = "https://city.example.gov/noise-complaint",
        secondaryActionUri: String? = null,
        secondaryActionLabel: String? = secondaryActionUri?.let { "Open written complaint" },
    ) = RuleWorkflow(
        id = "example-party",
        jurisdictionId = "example",
        jurisdiction = "Example, California",
        noiseType = NoiseType.PARTY_MUSIC,
        title = "Report ongoing amplified noise",
        summary = "The city accepts complaints through its official form.",
        captureInstruction = "Document the time and duration.",
        requiredIncidentCount = null,
        nextAction = "Submit the city complaint form.",
        actionLabel = "Open complaint form",
        actionUri = actionUri,
        secondaryActionLabel = secondaryActionLabel,
        secondaryActionUri = secondaryActionUri,
        officialSourceLabel = "City noise code",
        officialSourceUrl = "https://city.example.gov/noise-code",
        verifiedDate = "2026-07-29",
    )
}
