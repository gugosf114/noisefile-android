package com.noisefile.app.data

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

        assertTrue(draft.contains("Location of disturbance: 440 Price Avenue"))
        assertTrue(draft.contains("Bass was vibrating the bedroom window."))
        assertTrue(draft.contains("Interrupted rest or quiet use"))
        assertTrue(draft.contains("58 dB average"))
        assertTrue(draft.contains("Official source: City noise code"))
        assertTrue(draft.contains("https://city.example.gov/noise-code"))
        assertTrue(draft.contains("estimates from my phone"))
        assertFalse(draft.contains("violation occurred", ignoreCase = true))
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
        secondaryActionLabel = secondaryActionUri?.let { "Open written complaint" },
        secondaryActionUri = secondaryActionUri,
        officialSourceLabel = "City noise code",
        officialSourceUrl = "https://city.example.gov/noise-code",
        verifiedDate = "2026-07-29",
    )
}
