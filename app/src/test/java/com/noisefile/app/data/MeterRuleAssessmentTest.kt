package com.noisefile.app.data

import com.noisefile.app.model.MeterReading
import com.noisefile.app.model.NoiseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalTime

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
    fun sanMateoConstructionShowsBelowAndAtOrAboveTheListedLimit() {
        val rule = catalog().retrieve("san-mateo", NoiseType.CONSTRUCTION)
            ?: error("Missing San Mateo construction rule")

        val below = assessMeterReading(
            rule = rule,
            reading = readingWithMaximum(72.0),
            localTime = LocalTime.NOON,
        )
        val atOrAbove = assessMeterReading(
            rule = rule,
            reading = readingWithMaximum(91.0),
            localTime = LocalTime.NOON,
        )

        assertEquals(MeterAssessmentStatus.BELOW_LISTED_LIMIT, below.status)
        assertTrue(below.headline.contains("below the listed 90 dB limit"))
        assertEquals(MeterAssessmentStatus.AT_OR_ABOVE_LISTED_LIMIT, atOrAbove.status)
        assertTrue(atOrAbove.headline.contains("at or above the listed 90 dB limit"))
    }

    @Test
    fun scheduledLimitUsesTheCurrentLocalTime() {
        val rule = catalog().retrieve("sunnyvale", NoiseType.PARTY_MUSIC)
            ?: error("Missing Sunnyvale general-noise rule")

        val daytime = assessMeterReading(
            rule = rule,
            reading = readingWithMaximum(55.0),
            localTime = LocalTime.of(12, 0),
        )
        val nighttime = assessMeterReading(
            rule = rule,
            reading = readingWithMaximum(55.0),
            localTime = LocalTime.of(23, 0),
        )

        assertEquals(MeterAssessmentStatus.BELOW_LISTED_LIMIT, daytime.status)
        assertTrue(daytime.headline.contains("daytime 60 dB limit"))
        assertEquals(MeterAssessmentStatus.AT_OR_ABOVE_LISTED_LIMIT, nighttime.status)
        assertTrue(nighttime.headline.contains("nighttime 50 dB limit"))
    }

    @Test
    fun everyRuleProducesCitySpecificFeedback() {
        catalog().rules.forEach { rule ->
            val assessment = assessMeterReading(
                rule = rule,
                reading = readingWithMaximum(65.0),
                localTime = LocalTime.NOON,
            )

            assertTrue("${rule.id} headline is blank", assessment.headline.isNotBlank())
            assertTrue("${rule.id} detail is blank", assessment.detail.isNotBlank())
            if (rule.meterLimit == null) {
                assertEquals(
                    "${rule.id} must avoid an invented cutoff",
                    MeterAssessmentStatus.CONDITIONS_REQUIRED,
                    assessment.status,
                )
            }
        }
    }

    private fun readingWithMaximum(maximumDb: Double) = MeterReading(
        currentDb = maximumDb,
        minimumDb = maximumDb,
        averageDb = maximumDb,
        maximumDb = maximumDb,
        elapsedMillis = 5_000,
        sampleWindows = 5,
    )
}
