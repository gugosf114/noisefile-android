package com.noisefile.app.data

import com.noisefile.app.model.MeterReading
import com.noisefile.app.model.NoiseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDateTime

class MeterRuleAssessmentTest {
    private fun catalog(): RuleCatalog {
        val candidates = listOf(
            File("app/src/main/assets/${RuleCatalog.ASSET_PATH}"),
            File("src/main/assets/${RuleCatalog.ASSET_PATH}"),
        )
        val catalogFile = candidates.firstOrNull { it.isFile }
            ?: error("Could not find ${RuleCatalog.ASSET_PATH}")
        return RuleCatalog.fromJson(catalogFile.readText())
    }

    @Test
    fun sanMateoConstructionExplainsBelowAndAtOrAboveTheListedLimit() {
        val rule = catalog().retrieve("san-mateo", NoiseType.CONSTRUCTION)
            ?: error("Missing San Mateo construction rule")

        val below = assessMeterReading(
            rule = rule,
            reading = reading(maximumDb = 72.0),
            localDateTime = LocalDateTime.of(2026, 7, 29, 12, 0),
        )
        val atOrAbove = assessMeterReading(
            rule = rule,
            reading = reading(maximumDb = 91.0),
            localDateTime = LocalDateTime.of(2026, 7, 29, 12, 0),
        )

        assertEquals(MeterAssessmentStatus.DOES_NOT_REACH_LISTED_CONDITION, below.status)
        assertTrue(below.conditionText().contains("18 dB below the listed 90 dB limit"))
        assertEquals(MeterAssessmentStatus.REACHES_LISTED_CONDITION, atOrAbove.status)
        assertTrue(atOrAbove.conditionText().contains("1 dB at or above the listed 90 dB limit"))
    }

    @Test
    fun scheduledDecibelAndRestrictedHourChecksUseCurrentTime() {
        val rule = catalog().retrieve("sunnyvale", NoiseType.PARTY_MUSIC)
            ?: error("Missing Sunnyvale general-noise rule")

        val daytime = assessMeterReading(
            rule = rule,
            reading = reading(maximumDb = 55.0),
            localDateTime = LocalDateTime.of(2026, 7, 29, 12, 0),
        )
        val nighttime = assessMeterReading(
            rule = rule,
            reading = reading(maximumDb = 55.0),
            localDateTime = LocalDateTime.of(2026, 7, 29, 23, 0),
        )

        assertEquals(MeterAssessmentStatus.DOES_NOT_REACH_LISTED_CONDITION, daytime.status)
        assertTrue(daytime.conditionText().contains("5 dB below the listed daytime 60 dB limit"))
        assertTrue(daytime.conditionText().contains("outside the listed restricted hours"))
        assertEquals(MeterAssessmentStatus.REACHES_LISTED_CONDITION, nighttime.status)
        assertTrue(nighttime.conditionText().contains("5 dB at or above the listed nighttime 50 dB limit"))
        assertTrue(nighttime.conditionText().contains("within the listed restricted hours"))
    }

    @Test
    fun constructionScheduleExplainsWhetherCurrentTimeIsAllowed() {
        val rule = catalog().retrieve("san-jose", NoiseType.CONSTRUCTION)
            ?: error("Missing San Jose construction rule")

        val weekday = assessMeterReading(
            rule = rule,
            reading = reading(),
            localDateTime = LocalDateTime.of(2026, 7, 29, 12, 0),
        )
        val saturday = assessMeterReading(
            rule = rule,
            reading = reading(),
            localDateTime = LocalDateTime.of(2026, 8, 1, 12, 0),
        )

        assertEquals(MeterAssessmentStatus.DOES_NOT_REACH_LISTED_CONDITION, weekday.status)
        assertTrue(weekday.conditionText().contains("within regular allowed hours"))
        assertEquals(MeterAssessmentStatus.REACHES_LISTED_CONDITION, saturday.status)
        assertTrue(saturday.conditionText().contains("none are listed for Saturday"))
    }

    @Test
    fun durationAndIncidentCountShowExactProgress() {
        val berkeleyBarking = catalog().retrieve("berkeley", NoiseType.BARKING_DOG)
            ?: error("Missing Berkeley barking rule")
        val sanJoseBarking = catalog().retrieve("san-jose", NoiseType.BARKING_DOG)
            ?: error("Missing San Jose barking rule")

        val shortDuration = assessMeterReading(
            rule = berkeleyBarking,
            reading = reading(elapsedSeconds = 7 * 60L),
        )
        val completedLog = assessMeterReading(
            rule = sanJoseBarking,
            reading = reading(),
            incidentCount = 4,
        )

        assertEquals(MeterAssessmentStatus.DOES_NOT_REACH_LISTED_CONDITION, shortDuration.status)
        assertTrue(shortDuration.conditionText().contains("7:00 of 10:00"))
        assertEquals(MeterAssessmentStatus.REACHES_LISTED_CONDITION, completedLog.status)
        assertTrue(completedLog.conditionText().contains("reaches 5 of 5 required incidents"))
    }

    @Test
    fun everyRuleProducesAnExplicitResultAndReason() {
        catalog().rules.forEach { rule ->
            val assessment = assessMeterReading(
                rule = rule,
                reading = reading(),
                localDateTime = LocalDateTime.of(2026, 7, 29, 12, 0),
            )

            assertTrue("${rule.id} headline is blank", assessment.headline.isNotBlank())
            assertTrue("${rule.id} detail is blank", assessment.detail.isNotBlank())
            assertTrue("${rule.id} has no reasons", assessment.conditions.isNotEmpty())
            assertTrue(
                "${rule.id} omitted its ordinance test",
                assessment.conditionText().contains(rule.summary),
            )
            assertTrue(
                "${rule.id} omitted its capture requirement",
                assessment.conditionText().contains(rule.captureInstruction),
            )
        }
    }

    private fun MeterRuleAssessment.conditionText(): String =
        conditions.joinToString("\n") { it.text }

    private fun reading(
        maximumDb: Double = 65.0,
        elapsedSeconds: Long = 60L,
    ) = MeterReading(
        currentDb = maximumDb,
        minimumDb = maximumDb,
        averageDb = maximumDb,
        maximumDb = maximumDb,
        elapsedMillis = elapsedSeconds * 1_000L,
        sampleWindows = 5,
    )
}
