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
    fun sanMateoPhoneReadingDoesNotPretendToBeTheRequiredPrecisionMeasurement() {
        val rules = listOf(
            catalog().retrieve("san-mateo", NoiseType.BARKING_DOG)
                ?: error("Missing San Mateo barking rule"),
            catalog().retrieve("san-mateo", NoiseType.PARTY_MUSIC)
                ?: error("Missing San Mateo general-noise rule"),
            catalog().retrieve("san-mateo", NoiseType.CONSTRUCTION)
                ?: error("Missing San Mateo construction rule"),
        )

        rules.forEach { rule ->
            val assessment = assessMeterReading(
                rule = rule,
                reading = reading(maximumDb = 95.0),
                localDateTime = LocalDateTime.of(2026, 7, 29, 23, 0),
            )

            assertEquals(rule.id, MeterAssessmentStatus.NEEDS_INFORMATION, assessment.status)
            assertTrue(rule.id, assessment.headline.contains("cannot pass or fail the San Mateo rule"))
        }

        rules.drop(1).forEach { rule ->
            assertTrue(rule.id, rule.captureInstruction.contains("Type 1 precision meter"))
            assertTrue(rule.id, rule.captureInstruction.contains("phone estimate is supporting evidence"))
        }

        assertTrue(rules[1].summary.contains("six-minute ambient"))
        assertTrue(rules[2].summary.contains("requires at least one of two 90 dB conditions"))
    }

    @Test
    fun sanMateoBarkingKeepsTheCurrentLawSeparateFromTheOlderHostedPacket() {
        val rule = catalog().retrieve("san-mateo", NoiseType.BARKING_DOG)
            ?: error("Missing San Mateo barking rule")
        val assessment = assessMeterReading(
            rule = rule,
            reading = reading(maximumDb = 95.0),
            localDateTime = LocalDateTime.of(2026, 7, 29, 12, 0),
        )

        assertEquals(MeterAssessmentStatus.NEEDS_INFORMATION, assessment.status)
        assertTrue(rule.summary.contains("at any hour"))
        assertTrue(rule.summary.contains("currently hosted 2020 prosecution packet"))
        assertTrue(rule.captureInstruction.contains("eight or more barking days"))
        assertTrue(rule.nextAction.contains("predates the 2024 animal-code amendment"))
    }

    @Test
    fun sanJoseConstructionNeedsTheActualPermitInsteadOfAClockOnlyVerdict() {
        val rule = catalog().retrieve("san-jose", NoiseType.CONSTRUCTION)
            ?: error("Missing San Jose construction rule")

        val assessment = assessMeterReading(
            rule = rule,
            reading = reading(),
            localDateTime = LocalDateTime.of(2026, 8, 1, 12, 0),
        )

        assertEquals(MeterAssessmentStatus.NEEDS_INFORMATION, assessment.status)
        assertTrue(assessment.headline.contains("cannot pass or fail the San Jose rule"))
        assertTrue(rule.summary.contains("permit or planning approval may expressly allow different hours"))
        assertTrue(rule.captureInstruction.contains("requiring no development permit"))
    }

    @Test
    fun incidentCountShowsExactProgressWithoutGuessingBerkeleyBarking() {
        val berkeleyBarking = catalog().retrieve("berkeley", NoiseType.BARKING_DOG)
            ?: error("Missing Berkeley barking rule")
        val sanJoseBarking = catalog().retrieve("san-jose", NoiseType.BARKING_DOG)
            ?: error("Missing San Jose barking rule")

        val berkeleyAssessment = assessMeterReading(
            rule = berkeleyBarking,
            reading = reading(elapsedSeconds = 7 * 60L),
        )
        val completedLog = assessMeterReading(
            rule = sanJoseBarking,
            reading = reading(),
            incidentCount = 4,
        )

        assertEquals(MeterAssessmentStatus.NEEDS_INFORMATION, berkeleyAssessment.status)
        assertTrue(berkeleyAssessment.headline.contains("cannot pass or fail the Berkeley rule"))
        assertTrue(berkeleyBarking.summary.contains("intermittently for 30 minutes"))
        assertTrue(berkeleyBarking.summary.contains("warning"))
        assertEquals(MeterAssessmentStatus.REACHES_LISTED_CONDITION, completedLog.status)
        assertTrue(completedLog.conditionText().contains("reaches 5 of 5 required incidents"))
    }

    @Test
    fun everyRuleProducesAnExplicitResultAndReason() {
        val catalog = catalog()
        assertEquals(15, catalog.jurisdictions.size)
        assertEquals(45, catalog.rules.size)

        val assessments = catalog.rules.map { rule ->
            val assessment = assessMeterReading(
                rule = rule,
                reading = reading(maximumDb = 120.0, elapsedSeconds = 24 * 60 * 60L),
                localDateTime = LocalDateTime.of(2026, 8, 2, 3, 0),
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
            if (rule.id == "san-jose-barking-dog") {
                assertEquals(
                    rule.id,
                    MeterAssessmentStatus.DOES_NOT_REACH_LISTED_CONDITION,
                    assessment.status,
                )
                assertTrue(
                    rule.id,
                    assessment.conditionText().contains("1 of 5 including this recording; 4 more needed"),
                )
            } else {
                assertEquals(rule.id, MeterAssessmentStatus.NEEDS_INFORMATION, assessment.status)
                assertTrue(
                    "${rule.id} does not explain the phone reading",
                    assessment.headline.contains("120 dB reading cannot pass or fail"),
                )
                assertTrue(
                    "${rule.id} does not name its city",
                    assessment.headline.contains(rule.jurisdiction.substringBefore(",")),
                )
            }
            assessment
        }

        assertEquals(
            44,
            assessments.count { it.status == MeterAssessmentStatus.NEEDS_INFORMATION },
        )
        assertEquals(
            1,
            assessments.count {
                it.status == MeterAssessmentStatus.DOES_NOT_REACH_LISTED_CONDITION
            },
        )
    }

    @Test
    fun fremontMeterDoesNotTurnPartialTimeChecksIntoLegalVerdicts() {
        val generalNoise = catalog().retrieve("fremont", NoiseType.PARTY_MUSIC)
            ?: error("Missing Fremont general-noise rule")
        val construction = catalog().retrieve("fremont", NoiseType.CONSTRUCTION)
            ?: error("Missing Fremont construction rule")

        listOf(
            assessMeterReading(
                rule = generalNoise,
                reading = reading(maximumDb = 95.0),
                localDateTime = LocalDateTime.of(2026, 7, 29, 8, 40),
            ),
            assessMeterReading(
                rule = generalNoise,
                reading = reading(maximumDb = 95.0),
                localDateTime = LocalDateTime.of(2026, 7, 29, 23, 0),
            ),
            assessMeterReading(
                rule = construction,
                reading = reading(maximumDb = 95.0),
                localDateTime = LocalDateTime.of(2026, 8, 2, 12, 0),
            ),
        ).forEach { assessment ->
            assertEquals(MeterAssessmentStatus.NEEDS_INFORMATION, assessment.status)
            assertTrue(assessment.headline.contains("cannot pass or fail the Fremont rule"))
        }

        assertTrue(generalNoise.summary.contains("no fixed citywide dB cutoff"))
        assertTrue(generalNoise.summary.contains("50-foot test is distance, not 50 dB"))
        assertTrue(construction.summary.contains("farther than 500 feet"))
        assertTrue(construction.summary.contains("city-approved project may have modified hours"))
    }

    @Test
    fun propertyAndSourceDependentRulesDoNotUseOneUniversalDbOrScheduleVerdict() {
        val rules = listOf(
            catalog().retrieve("sunnyvale", NoiseType.PARTY_MUSIC)
                ?: error("Missing Sunnyvale general-noise rule"),
            catalog().retrieve("sunnyvale", NoiseType.CONSTRUCTION)
                ?: error("Missing Sunnyvale construction rule"),
            catalog().retrieve("santa-clara", NoiseType.PARTY_MUSIC)
                ?: error("Missing Santa Clara general-noise rule"),
            catalog().retrieve("santa-clara", NoiseType.CONSTRUCTION)
                ?: error("Missing Santa Clara construction rule"),
        )

        rules.forEach { rule ->
            val assessment = assessMeterReading(
                rule = rule,
                reading = reading(maximumDb = 95.0),
                localDateTime = LocalDateTime.of(2026, 8, 2, 12, 0),
            )

            assertEquals(rule.id, MeterAssessmentStatus.NEEDS_INFORMATION, assessment.status)
            assertTrue(
                rule.id,
                assessment.headline.contains("cannot pass or fail the ${rule.jurisdiction.substringBefore(",")} rule"),
            )
        }

        assertTrue(rules[0].summary.contains("Multifamily primary usable open space: 65/55"))
        assertTrue(rules[1].summary.contains("two unpaid helpers"))
        assertTrue(rules[2].summary.contains("Music or informational speech reduces"))
        assertTrue(rules[3].summary.contains("work an owner performs"))
    }

    @Test
    fun antiochBerkeleyAndConcordNeedFactsThePhoneCannotSupply() {
        val catalog = catalog()
        val rules = listOf("antioch", "berkeley", "concord").flatMap { jurisdictionId ->
            NoiseType.entries.map { noiseType ->
                catalog.retrieve(jurisdictionId, noiseType)
                    ?: error("Missing $jurisdictionId $noiseType rule")
            }
        }

        rules.forEach { rule ->
            val assessment = assessMeterReading(
                rule = rule,
                reading = reading(maximumDb = 95.0, elapsedSeconds = 60 * 60L),
                localDateTime = LocalDateTime.of(2026, 8, 2, 3, 0),
            )

            assertEquals(rule.id, MeterAssessmentStatus.NEEDS_INFORMATION, assessment.status)
            assertTrue(
                rule.id,
                assessment.headline.contains(
                    "cannot pass or fail the ${rule.jurisdiction.substringBefore(",")} rule",
                ),
            )
        }

        assertTrue(rules.first { it.id == "antioch-barking_dog" }.summary.contains("15-day follow-up"))
        assertTrue(rules.first { it.id == "antioch-construction" }.summary.contains("within 300 feet"))
        assertTrue(rules.first { it.id == "berkeley-party_music" }.summary.contains("A higher measured ambient"))
        assertTrue(rules.first { it.id == "berkeley-construction" }.summary.contains("hours alone are not the complete legal test"))
        assertTrue(rules.first { it.id == "concord-barking-dog" }.summary.contains("notarized sworn affidavit"))
        assertTrue(rules.first { it.id == "concord-construction" }.summary.contains("does not give every project a general Sunday window"))
    }

    @Test
    fun oaklandRulesNeedFactsAndOfficialActionThePhoneCannotSupply() {
        val catalog = catalog()
        val rules = NoiseType.entries.map { noiseType ->
            catalog.retrieve("oakland", noiseType)
                ?: error("Missing Oakland $noiseType rule")
        }

        rules.forEach { rule ->
            val assessment = assessMeterReading(
                rule = rule,
                reading = reading(maximumDb = 95.0, elapsedSeconds = 60 * 60L),
                localDateTime = LocalDateTime.of(2026, 8, 2, 3, 0),
            )

            assertEquals(rule.id, MeterAssessmentStatus.NEEDS_INFORMATION, assessment.status)
            assertTrue(
                rule.id,
                assessment.headline.contains("cannot pass or fail the Oakland rule"),
            )
        }

        assertTrue(rules.first { it.id == "oakland-barking_dog" }.summary.contains("no fixed minute or dB threshold"))
        assertTrue(rules.first { it.id == "oakland-party_music" }.summary.contains("official warning is required"))
        assertTrue(rules.first { it.id == "oakland-construction" }.summary.contains("no blanket hours ban"))
        assertTrue(rules.first { it.id == "oakland-construction" }.summary.contains("less than 10 days"))
    }

    @Test
    fun haywardRulesNeedTheLegalMeasurementAndEnforcementFacts() {
        val catalog = catalog()
        val rules = NoiseType.entries.map { noiseType ->
            catalog.retrieve("hayward", noiseType)
                ?: error("Missing Hayward $noiseType rule")
        }

        rules.forEach { rule ->
            val assessment = assessMeterReading(
                rule = rule,
                reading = reading(maximumDb = 95.0, elapsedSeconds = 60 * 60L),
                localDateTime = LocalDateTime.of(2026, 8, 2, 3, 0),
            )

            assertEquals(rule.id, MeterAssessmentStatus.NEEDS_INFORMATION, assessment.status)
            assertTrue(
                rule.id,
                assessment.headline.contains("cannot pass or fail the Hayward rule"),
            )
        }

        assertTrue(rules.first { it.id == "hayward-barking_dog" }.summary.contains("no fixed minute or dB threshold"))
        assertTrue(rules.first { it.id == "hayward-party_music" }.summary.contains("second complaint arrives within 72 hours"))
        assertTrue(rules.first { it.id == "hayward-construction" }.summary.contains("not an automatic violation"))
    }

    @Test
    fun vallejoRulesNeedTheLegalRouteZoneAndMeasurementFacts() {
        val catalog = catalog()
        val rules = NoiseType.entries.map { noiseType ->
            catalog.retrieve("vallejo", noiseType)
                ?: error("Missing Vallejo $noiseType rule")
        }

        rules.forEach { rule ->
            val assessment = assessMeterReading(
                rule = rule,
                reading = reading(maximumDb = 95.0, elapsedSeconds = 60 * 60L),
                localDateTime = LocalDateTime.of(2026, 8, 2, 3, 0),
            )

            assertEquals(rule.id, MeterAssessmentStatus.NEEDS_INFORMATION, assessment.status)
            assertTrue(
                rule.id,
                assessment.headline.contains("cannot pass or fail the Vallejo rule"),
            )
        }

        assertTrue(rules.first { it.id == "vallejo-barking_dog" }.summary.contains("two-week barking journal"))
        assertTrue(rules.first { it.id == "vallejo-party_music" }.summary.contains("more restrictive applicable rule controls"))
        assertTrue(rules.first { it.id == "vallejo-construction" }.summary.contains("one-quarter mile"))
    }

    @Test
    fun richmondRulesNeedTheSeparateLegalTestsAndOfficialAction() {
        val catalog = catalog()
        val rules = NoiseType.entries.map { noiseType ->
            catalog.retrieve("richmond", noiseType)
                ?: error("Missing Richmond $noiseType rule")
        }

        rules.forEach { rule ->
            val assessment = assessMeterReading(
                rule = rule,
                reading = reading(maximumDb = 95.0, elapsedSeconds = 60 * 60L),
                localDateTime = LocalDateTime.of(2026, 8, 2, 3, 0),
            )

            assertEquals(rule.id, MeterAssessmentStatus.NEEDS_INFORMATION, assessment.status)
            assertTrue(
                rule.id,
                assessment.headline.contains("cannot pass or fail the Richmond rule"),
            )
        }

        val animal = rules.first { it.id == "richmond-barking_dog" }
        val noise = rules.first { it.id == "richmond-party_music" }
        val construction = rules.first { it.id == "richmond-construction" }
        assertTrue(animal.summary.contains("no fixed minute or dB threshold"))
        assertTrue(animal.summary.contains("30 continuous minutes or 60 intermittent minutes"))
        assertTrue(noise.summary.contains("sole test for that incident"))
        assertTrue(construction.summary.contains("one to five dwelling units"))
    }

    @Test
    fun sanFranciscoRulesNeedContinuousConductOrOfficialMeasurementFacts() {
        val catalog = catalog()
        val rules = NoiseType.entries.map { noiseType ->
            catalog.retrieve("san-francisco", noiseType)
                ?: error("Missing San Francisco $noiseType rule")
        }

        rules.forEach { rule ->
            val assessment = assessMeterReading(
                rule = rule,
                reading = reading(maximumDb = 95.0, elapsedSeconds = 60 * 60L),
                localDateTime = LocalDateTime.of(2026, 8, 2, 3, 0),
            )

            assertEquals(rule.id, MeterAssessmentStatus.NEEDS_INFORMATION, assessment.status)
            assertTrue(
                rule.id,
                assessment.headline.contains("cannot pass or fail the San Francisco rule"),
            )
        }

        val animal = rules.first { it.id == "san-francisco-barking_dog" }
        val noise = rules.first { it.id == "san-francisco-party_music" }
        val construction = rules.first { it.id == "san-francisco-construction" }
        assertTrue(animal.summary.contains("continuously and incessantly for 10 minutes"))
        assertTrue(animal.summary.contains("warning letters for the first two complaints"))
        assertTrue(noise.summary.contains("Type 1 precision meter"))
        assertTrue(construction.summary.contains("impact tools"))
    }

    @Test
    fun dalyCityRulesNeedNuisanceScopeWitnessOrPermitFacts() {
        val catalog = catalog()
        val rules = NoiseType.entries.map { noiseType ->
            catalog.retrieve("daly-city", noiseType)
                ?: error("Missing Daly City $noiseType rule")
        }

        rules.forEach { rule ->
            val assessment = assessMeterReading(
                rule = rule,
                reading = reading(maximumDb = 95.0, elapsedSeconds = 60 * 60L),
                localDateTime = LocalDateTime.of(2026, 8, 2, 3, 0),
            )

            assertEquals(rule.id, MeterAssessmentStatus.NEEDS_INFORMATION, assessment.status)
            assertTrue(
                rule.id,
                assessment.headline.contains("cannot pass or fail the Daly City rule"),
            )
        }

        val animal = rules.first { it.id == "daly-city-barking-dog" }
        val noise = rules.first { it.id == "daly-city-party-music" }
        val construction = rules.first { it.id == "daly-city-construction" }
        assertTrue(animal.summary.contains("five-person requirement"))
        assertTrue(noise.summary.contains("No minute or dB threshold"))
        assertTrue(construction.summary.contains("new Chapter 15.09"))
        assertTrue(construction.summary.contains("daylight-saving time"))
    }

    @Test
    fun santaRosaRulesNeedReasonablePersonZoneMeasurementAndProjectFacts() {
        val catalog = catalog()
        val rules = NoiseType.entries.map { noiseType ->
            catalog.retrieve("santa-rosa", noiseType)
                ?: error("Missing Santa Rosa $noiseType rule")
        }

        rules.forEach { rule ->
            val assessment = assessMeterReading(
                rule = rule,
                reading = reading(maximumDb = 95.0, elapsedSeconds = 60 * 60L),
                localDateTime = LocalDateTime.of(2026, 8, 2, 3, 0),
            )

            assertEquals(rule.id, MeterAssessmentStatus.NEEDS_INFORMATION, assessment.status)
            assertTrue(
                rule.id,
                assessment.headline.contains("cannot pass or fail the Santa Rosa rule"),
            )
        }

        val animal = rules.first { it.id == "santa-rosa-barking_dog" }
        val noise = rules.first { it.id == "santa-rosa-party_music" }
        val construction = rules.first { it.id == "santa-rosa-construction" }
        assertTrue(animal.summary.contains("no fixed minute or dB threshold"))
        assertTrue(noise.summary.contains("does not prove compliance"))
        assertTrue(construction.summary.contains("does not establish one universal construction-hours schedule"))
    }

    @Test
    fun sanJoseGeneralNoiseSaysExactlyWhatTheMeterCanAndCannotDecide() {
        val rule = catalog().retrieve("san-jose", NoiseType.PARTY_MUSIC)
            ?: error("Missing San Jose general-noise rule")

        val assessment = assessMeterReading(
            rule = rule,
            reading = reading(maximumDb = 60.0, elapsedSeconds = 3),
            localDateTime = LocalDateTime.of(2026, 7, 29, 7, 16),
        )

        assertEquals(MeterAssessmentStatus.NEEDS_INFORMATION, assessment.status)
        assertEquals(
            "This 60 dB reading cannot pass or fail the San Jose rule by itself",
            assessment.headline,
        )
        assertTrue(rule.nextAction.contains("408-277-8900"))
        assertTrue(rule.nextAction.contains("ordinarily will not respond"))
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
