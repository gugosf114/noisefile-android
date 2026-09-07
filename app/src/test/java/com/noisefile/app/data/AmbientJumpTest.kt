package com.noisefile.app.data

import com.noisefile.app.model.AmbientReading
import com.noisefile.app.model.Incident
import com.noisefile.app.model.MeterReading
import com.noisefile.app.model.NoiseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * The jump: the noise against the quiet baseline the same phone took in the
 * same spot. The phone's offset cancels out of the difference. The line is
 * information; the 2026-07-29 boundary (no phone-reading verdict) holds.
 */
class AmbientJumpTest {
    private val catalog: RuleCatalog by lazy {
        val candidates = listOf(
            File("app/src/main/assets/${RuleCatalog.ASSET_PATH}"),
            File("src/main/assets/${RuleCatalog.ASSET_PATH}"),
        )
        val catalogFile = candidates.firstOrNull { it.isFile }
            ?: error("Could not find ${RuleCatalog.ASSET_PATH}")
        RuleCatalog.fromJson(catalogFile.readText())
    }

    private fun reading(maximumDb: Double, averageDb: Double) = MeterReading(
        currentDb = maximumDb,
        minimumDb = averageDb - 5.0,
        averageDb = averageDb,
        maximumDb = maximumDb,
        elapsedMillis = 45_000L,
        sampleWindows = 5,
    )

    @Test
    fun sanMateoNoiseIsReportedAsTheDifferenceAboveItsSixMinuteBaseline() {
        val rule = catalog.retrieve("san-mateo", NoiseType.PARTY_MUSIC) ?: error("Missing rule")
        val assessment = assessMeterReading(
            rule = rule,
            reading = reading(maximumDb = 61.0, averageDb = 55.0),
            localDateTime = LocalDateTime.of(2026, 9, 9, 15, 0),
            ambient = AmbientReading(db = 47.0, seconds = 360L, sampleWindows = 400),
        )
        val line = assessment.conditions.map { it.text }.first { it.startsWith("Sound:") }

        assertTrue(line, line.contains("61 dB highest estimate is 14 dB above your 6-minute quiet baseline of 47 dB"))
        assertTrue(line, line.contains("the 55 dB average is 8 dB above it"))
        assertTrue(line, line.contains("cancels out of the difference"))
        assertTrue(line, line.contains("six minutes"))
        assertEquals(6, rule.ambientRecipe?.minutes)
        // information, not a verdict
        assertEquals(MeterAssessmentStatus.NEEDS_INFORMATION, assessment.status)
        assertEquals(
            RuleConditionOutcome.NEEDS_INFORMATION,
            assessment.conditions.first { it.text.startsWith("Sound:") }.outcome,
        )
    }

    @Test
    fun aCityWithoutARecipeStillGetsTheDifferenceAndSaysTheRecipeIsMissing() {
        val rule = catalog.retrieve("hayward", NoiseType.PARTY_MUSIC) ?: error("Missing rule")
        val assessment = assessMeterReading(
            rule = rule,
            reading = reading(maximumDb = 58.0, averageDb = 50.0),
            localDateTime = LocalDateTime.of(2026, 9, 9, 15, 0),
            ambient = AmbientReading(db = 52.0, seconds = 270L, sampleWindows = 300),
        )
        val line = assessment.conditions.map { it.text }.first { it.startsWith("Sound:") }

        assertTrue(line, line.contains("58 dB highest estimate is 6 dB above your 4:30 quiet baseline of 52 dB"))
        assertTrue(line, line.contains("the 50 dB average is 2 dB below it"))
        assertTrue(line, line.contains("Hayward's code sets no ambient recipe"))
        assertNull(rule.ambientRecipe)
    }

    @Test
    fun withoutABaselineThereIsNoSoundLine() {
        val rule = catalog.retrieve("san-francisco", NoiseType.PARTY_MUSIC) ?: error("Missing rule")
        val assessment = assessMeterReading(
            rule = rule,
            reading = reading(maximumDb = 70.0, averageDb = 60.0),
            localDateTime = LocalDateTime.of(2026, 9, 9, 15, 0),
        )
        assertFalse(assessment.conditions.any { it.text.startsWith("Sound:") })
        assertEquals(10, rule.ambientRecipe?.minutes)
    }

    @Test
    fun threeRulesCarryTheCitysOwnAmbientRecipe() {
        val withRecipe = catalog.rules.filter { it.ambientRecipe != null }.map { it.id }.sorted()
        assertEquals(
            listOf("san-francisco-construction", "san-francisco-party_music", "san-mateo-party_music"),
            withRecipe,
        )
    }

    @Test
    fun baselineLabelsReadLikeAPerson() {
        assertEquals("6-minute", baselineLabel(360L))
        assertEquals("10-minute", baselineLabel(600L))
        assertEquals("4:30", baselineLabel(270L))
        assertEquals("0:45", baselineLabel(45L))
    }

    @Test
    fun storedIncidentsKeepTheBaselineAndOldOnesLoadWithoutIt() {
        val withBaseline = """[{"id":1,"ruleId":"san-mateo-party-music","noiseType":"PARTY_MUSIC",
            "startedAtEpochMillis":0,"durationSeconds":45,"minimumDb":41.2,"averageDb":58.7,"maximumDb":67.9,
            "location":"440 Price Avenue","impact":"Interrupted rest or quiet use","notes":"",
            "ambientDb":47.0,"ambientSeconds":360},
            {"id":2,"ruleId":"san-mateo-party-music","noiseType":"PARTY_MUSIC",
            "startedAtEpochMillis":1,"durationSeconds":45,"minimumDb":41.2,"averageDb":58.7,"maximumDb":67.9,
            "location":"440 Price Avenue","impact":"Interrupted rest or quiet use","notes":""}]"""
        val incidents = parseIncidents(withBaseline)
        assertEquals(2, incidents.size)
        val newer = incidents.first { it.id == 2L }
        val older = incidents.first { it.id == 1L }
        assertNull(newer.ambientDb)
        assertEquals(47.0, older.ambientDb!!, 0.001)
        assertEquals(360L, older.ambientSeconds)
    }

    @Test
    fun theComplaintAndTheHistoryCarryTheBaseline() {
        val rule = catalog.retrieve("san-mateo", NoiseType.PARTY_MUSIC) ?: error("Missing rule")
        val incident = Incident(
            id = 1L,
            ruleId = rule.id,
            noiseType = rule.noiseType,
            startedAtEpochMillis = 0L,
            durationSeconds = 45L,
            minimumDb = 41.2,
            averageDb = 55.4,
            maximumDb = 61.0,
            location = "440 Price Avenue",
            impact = "Interrupted rest or quiet use",
            notes = "",
            ambientDb = 47.0,
            ambientSeconds = 360L,
        )
        val draft = buildComplaintDraft(incident, rule, ZoneId.of("America/Los_Angeles"))
        assertTrue(draft, draft.contains("Quiet baseline at the same spot with the source silent: 47 dB over 6:00"))
        assertTrue(draft, draft.contains("averaged 8 dB above that baseline and peaked 14 dB above it"))

        val report = buildIncidentHistoryReport(listOf(incident), generatedAtLabel = "test", zoneId = ZoneId.of("America/Los_Angeles"))
        assertTrue(report, report.contains("Quiet baseline: 47 dB over 360 sec"))
        assertTrue(report, report.contains("8 dB above it on average, 14 dB above at peak"))

        val plain = incident.copy(ambientDb = null, ambientSeconds = null)
        assertFalse(buildComplaintDraft(plain, rule, ZoneId.of("America/Los_Angeles")).contains("Quiet baseline"))
        assertFalse(buildIncidentHistoryReport(listOf(plain), generatedAtLabel = "test").contains("Quiet baseline"))
    }
}
