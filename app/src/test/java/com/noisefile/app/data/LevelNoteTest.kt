package com.noisefile.app.data

import com.noisefile.app.model.Incident
import com.noisefile.app.model.NoiseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.ZoneId

/** The complaint says where the dB numbers came from: which mic, calibrated how. */
class LevelNoteTest {
    private val catalog: RuleCatalog by lazy {
        val candidates = listOf(
            File("app/src/main/assets/${RuleCatalog.ASSET_PATH}"),
            File("src/main/assets/${RuleCatalog.ASSET_PATH}"),
        )
        val catalogFile = candidates.firstOrNull { it.isFile }
            ?: error("Could not find ${RuleCatalog.ASSET_PATH}")
        RuleCatalog.fromJson(catalogFile.readText())
    }

    private val note = "The sound levels above come from my phone's built-in microphone, calibrated against a reference meter on Sep 7, 2026 (+4 dB), and are included as incident context."

    private fun incident(levelNote: String?) = Incident(
        id = 1L,
        ruleId = "hayward-party_music",
        noiseType = NoiseType.PARTY_MUSIC,
        startedAtEpochMillis = 0L,
        durationSeconds = 45L,
        minimumDb = 41.2,
        averageDb = 58.7,
        maximumDb = 67.9,
        location = "440 Price Avenue",
        impact = "Interrupted rest or quiet use",
        notes = "",
        levelNote = levelNote,
    )

    @Test
    fun theComplaintCarriesTheCalibrationNoteInsteadOfTheEstimateSentence() {
        val rule = catalog.retrieve("hayward", NoiseType.PARTY_MUSIC) ?: error("Missing rule")
        val draft = buildComplaintDraft(incident(note), rule, ZoneId.of("America/Los_Angeles"))
        assertTrue(draft, draft.contains("calibrated against a reference meter on Sep 7, 2026 (+4 dB)"))
        assertFalse(draft, draft.contains("estimates from my phone"))

        val plain = buildComplaintDraft(incident(null), rule, ZoneId.of("America/Los_Angeles"))
        assertTrue(plain, plain.contains("estimates from my phone"))
    }

    @Test
    fun theHistoryExportAndTheStoreKeepTheNote() {
        val report = buildIncidentHistoryReport(listOf(incident(note)), generatedAtLabel = "test", zoneId = ZoneId.of("America/Los_Angeles"))
        assertTrue(report, report.contains("Levels source: The sound levels above come from my phone's built-in microphone"))
        assertFalse(buildIncidentHistoryReport(listOf(incident(null)), generatedAtLabel = "test").contains("Levels source"))

        val stored = """[{"id":1,"ruleId":"hayward-party_music","noiseType":"PARTY_MUSIC",
            "startedAtEpochMillis":0,"durationSeconds":45,"minimumDb":41.2,"averageDb":58.7,"maximumDb":67.9,
            "location":"440 Price Avenue","impact":"Interrupted rest or quiet use","notes":"",
            "levelNote":"calibrated note"},
            {"id":2,"ruleId":"hayward-party_music","noiseType":"PARTY_MUSIC",
            "startedAtEpochMillis":1,"durationSeconds":45,"minimumDb":41.2,"averageDb":58.7,"maximumDb":67.9,
            "location":"440 Price Avenue","impact":"Interrupted rest or quiet use","notes":""}]"""
        val incidents = parseIncidents(stored)
        assertEquals("calibrated note", incidents.first { it.id == 1L }.levelNote)
        assertNull(incidents.first { it.id == 2L }.levelNote)
    }
}
