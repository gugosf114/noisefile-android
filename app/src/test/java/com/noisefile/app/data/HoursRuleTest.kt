package com.noisefile.app.data

import com.noisefile.app.model.DayGroup
import com.noisefile.app.model.HoursKind
import com.noisefile.app.model.HoursWindow
import com.noisefile.app.model.MeterReading
import com.noisefile.app.model.NoiseType
import com.noisefile.app.model.RuleWorkflow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDateTime

/**
 * The published schedules against the phone's clock. George's 2026-07-25 scenarios:
 * 2 AM construction in Oakland, a dog barking 15 minutes in Concord, music at
 * 10:30 PM in Daly City. The clock line informs; it never becomes a verdict, which
 * keeps the 2026-07-29 boundary that a clock-only or phone-reading-only check must
 * not decide a violation.
 */
class HoursRuleTest {
    private val catalog: RuleCatalog by lazy {
        val candidates = listOf(
            File("app/src/main/assets/${RuleCatalog.ASSET_PATH}"),
            File("src/main/assets/${RuleCatalog.ASSET_PATH}"),
        )
        val catalogFile = candidates.firstOrNull { it.isFile }
            ?: error("Could not find ${RuleCatalog.ASSET_PATH}")
        RuleCatalog.fromJson(catalogFile.readText())
    }

    private fun rule(city: String, noiseType: NoiseType): RuleWorkflow =
        catalog.retrieve(city, noiseType) ?: error("Missing $city $noiseType rule")

    private fun reading(maximumDb: Double = 70.0, elapsedSeconds: Long = 60L) = MeterReading(
        currentDb = maximumDb,
        minimumDb = maximumDb - 10.0,
        averageDb = maximumDb - 5.0,
        maximumDb = maximumDb,
        elapsedMillis = elapsedSeconds * 1_000L,
        sampleWindows = 5,
    )

    private fun timeLine(rule: RuleWorkflow, at: LocalDateTime): String? =
        assessMeterReading(rule = rule, reading = reading(), localDateTime = at)
            .conditions
            .map { it.text }
            .firstOrNull { it.startsWith("Time:") }

    // 2026-09-08 is a Tuesday, 2026-09-09 a Wednesday, 2026-09-12 a Saturday, 2026-09-13 a Sunday.

    @Test
    fun twoAmConstructionInOaklandIsNamedAsOutsideThePublishedHours() {
        val rule = rule("oakland", NoiseType.CONSTRUCTION)
        val at = LocalDateTime.of(2026, 9, 8, 2, 0)
        val line = timeLine(rule, at)

        assertNotNull(line)
        assertTrue(line!!, line.contains("2:00 AM Tuesday is outside Oakland's published construction hours"))
        assertTrue(line, line.contains("weekdays 7:00 AM-7:00 PM"))
        assertTrue(line, line.contains("Saturdays 9:00 AM-8:00 PM"))
        assertTrue(line, line.contains("permit"))

        val assessment = assessMeterReading(rule = rule, reading = reading(), localDateTime = at)
        assertEquals(MeterAssessmentStatus.NEEDS_INFORMATION, assessment.status)
        assertEquals(
            RuleConditionOutcome.NEEDS_INFORMATION,
            assessment.conditions.first { it.text.startsWith("Time:") }.outcome,
        )
    }

    @Test
    fun weekdayAfternoonConstructionInOaklandIsInsideThePublishedHours() {
        val line = timeLine(rule("oakland", NoiseType.CONSTRUCTION), LocalDateTime.of(2026, 9, 8, 15, 0))
        assertNotNull(line)
        assertTrue(line!!, line.contains("3:00 PM Tuesday is inside Oakland's published construction hours"))
    }

    @Test
    fun sundayMorningUsesTheWeekendWindowWhereTheCityHasOne() {
        val sunday = LocalDateTime.of(2026, 9, 13, 10, 0)

        val oakland = timeLine(rule("oakland", NoiseType.CONSTRUCTION), sunday)
        assertTrue(oakland!!, oakland.contains("10:00 AM Sunday is inside Oakland's published construction hours"))

        val sanJose = timeLine(rule("san-jose", NoiseType.CONSTRUCTION), sunday)
        assertTrue(sanJose!!, sanJose.contains("10:00 AM Sunday is outside San Jose's published construction hours"))
        assertTrue(sanJose, sanJose.contains("no Saturdays; no Sundays"))

        val santaClara = timeLine(rule("santa-clara", NoiseType.CONSTRUCTION), sunday)
        assertTrue(santaClara!!, santaClara.contains("outside Santa Clara's published construction hours"))
        assertTrue(santaClara, santaClara.contains("Saturdays 9:00 AM-6:00 PM; no Sundays"))
    }

    @Test
    fun dalyCityQuietHoursCrossMidnight() {
        val rule = rule("daly-city", NoiseType.PARTY_MUSIC)

        val lateEvening = timeLine(rule, LocalDateTime.of(2026, 9, 9, 22, 30))
        assertTrue(lateEvening!!, lateEvening.contains("10:30 PM Wednesday is inside Daly City's published quiet hours"))
        assertTrue(lateEvening, lateEvening.contains("every day 10:00 PM-6:00 AM"))

        val earlyEvening = timeLine(rule, LocalDateTime.of(2026, 9, 9, 21, 0))
        assertTrue(earlyEvening!!, earlyEvening.contains("9:00 PM Wednesday is outside Daly City's published quiet hours"))

        val beforeDawn = timeLine(rule, LocalDateTime.of(2026, 9, 9, 5, 30))
        assertTrue(beforeDawn!!, beforeDawn.contains("5:30 AM Wednesday is inside Daly City's published quiet hours"))

        val assessment = assessMeterReading(rule = rule, reading = reading(), localDateTime = LocalDateTime.of(2026, 9, 9, 22, 30))
        assertEquals(MeterAssessmentStatus.NEEDS_INFORMATION, assessment.status)
    }

    @Test
    fun concordBarkingHasNoClockAndNoMeterCondition() {
        val rule = rule("concord", NoiseType.BARKING_DOG)
        val assessment = assessMeterReading(
            rule = rule,
            reading = reading(maximumDb = 72.0, elapsedSeconds = 15 * 60L),
            localDateTime = LocalDateTime.of(2026, 9, 9, 14, 0),
        )

        assertNull(rule.hoursRule)
        assertNull(rule.meterLimit)
        assertEquals(MeterAssessmentStatus.NEEDS_INFORMATION, assessment.status)
        assertFalse(assessment.conditions.any { it.text.startsWith("Time:") })
        assertFalse(assessment.conditions.any { it.text.startsWith("Sound:") })
    }

    @Test
    fun fourteenConstructionSchedulesAndFourQuietHourRulesShipWithReceipts() {
        val scheduled = catalog.rules.filter { it.hoursRule != null }
        assertEquals(14, scheduled.count { it.hoursRule!!.kind == HoursKind.ALLOWED })
        assertEquals(4, scheduled.count { it.hoursRule!!.kind == HoursKind.QUIET })
        assertTrue(scheduled.filter { it.hoursRule!!.kind == HoursKind.ALLOWED }.all { it.noiseType == NoiseType.CONSTRUCTION })
        assertTrue(scheduled.filter { it.hoursRule!!.kind == HoursKind.QUIET }.all { it.noiseType == NoiseType.PARTY_MUSIC })
        scheduled.forEach { rule ->
            assertTrue("${rule.id} context names its code section", rule.hoursRule!!.context.contains("Code"))
        }
        // Santa Rosa construction has no schedule: the corpus holds no section for it.
        // Vallejo gained one on 2026-09-06 once Ch. 16.502 was captured.
        assertNull(rule("santa-rosa", NoiseType.CONSTRUCTION).hoursRule)
        assertEquals(HoursKind.ALLOWED, rule("vallejo", NoiseType.CONSTRUCTION).hoursRule?.kind)
    }

    @Test
    fun theClockLineNeverChangesAStatusOnItsOwn() {
        val sundayThreeAm = LocalDateTime.of(2026, 9, 13, 3, 0)
        catalog.rules.filter { it.hoursRule != null }.forEach { rule ->
            val assessment = assessMeterReading(rule = rule, reading = reading(), localDateTime = sundayThreeAm)
            assertEquals(rule.id, MeterAssessmentStatus.NEEDS_INFORMATION, assessment.status)
            assertTrue(rule.id, assessment.conditions.any { it.text.startsWith("Time:") })
            assertTrue(rule.id, assessment.conditions.any { it.text.startsWith("Ordinance test:") })
        }
    }

    @Test
    fun windowsHandleMidnightAndExclusiveEnds() {
        val quiet = HoursWindow(DayGroup.ALL, 22 * 60, 6 * 60)
        assertTrue(quiet.contains(23 * 60))
        assertTrue(quiet.contains(0))
        assertTrue(quiet.contains(5 * 60 + 59))
        assertFalse(quiet.contains(6 * 60))
        assertFalse(quiet.contains(12 * 60))

        val workday = HoursWindow(DayGroup.WEEKDAY, 7 * 60, 19 * 60)
        assertTrue(workday.contains(7 * 60))
        assertTrue(workday.contains(18 * 60 + 59))
        assertFalse(workday.contains(19 * 60))
        assertFalse(workday.contains(6 * 60 + 59))
    }
}
