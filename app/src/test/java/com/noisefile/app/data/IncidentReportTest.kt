package com.noisefile.app.data

import com.noisefile.app.model.Incident
import com.noisefile.app.model.NoiseType
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class IncidentReportTest {
    @Test
    fun reportIncludesIncidentNotes() {
        val report = buildIncidentHistoryReport(
            incidents = listOf(incident(notes = "Bass was vibrating the bedroom window.")),
            generatedAtLabel = "test time",
            zoneId = ZoneOffset.UTC,
        )

        assertTrue(report.contains("Notes: Bass was vibrating the bedroom window."))
        assertTrue(report.contains("Location: 440 Price Avenue"))
    }

    @Test
    fun reportMakesMissingNotesExplicit() {
        val report = buildIncidentHistoryReport(
            incidents = listOf(incident(notes = "")),
            generatedAtLabel = "test time",
            zoneId = ZoneOffset.UTC,
        )

        assertTrue(report.contains("Notes: None added"))
    }

    private fun incident(notes: String) = Incident(
        id = 1L,
        ruleId = "san-jose-party_music",
        noiseType = NoiseType.PARTY_MUSIC,
        startedAtEpochMillis = 0L,
        durationSeconds = 45L,
        minimumDb = 41.2,
        averageDb = 58.7,
        maximumDb = 67.9,
        location = "440 Price Avenue",
        impact = "Interrupted rest or quiet use",
        notes = notes,
    )
}
