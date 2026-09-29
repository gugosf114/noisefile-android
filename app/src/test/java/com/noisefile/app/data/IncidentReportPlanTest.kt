package com.noisefile.app.data

import com.noisefile.app.model.Incident
import com.noisefile.app.model.NoiseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneOffset

class IncidentReportPlanTest {
    private val catalog: RuleCatalog by lazy {
        val file = listOf(File("app/src/main/assets/${RuleCatalog.ASSET_PATH}"), File("src/main/assets/${RuleCatalog.ASSET_PATH}"))
            .firstOrNull { it.isFile } ?: error("Could not find ${RuleCatalog.ASSET_PATH}")
        RuleCatalog.fromJson(file.readText())
    }

    private fun take(id: Long, ruleId: String, type: NoiseType, at: LocalDateTime, maxDb: Double) = Incident(
        id = id, ruleId = ruleId, noiseType = type,
        startedAtEpochMillis = at.toInstant(ZoneOffset.UTC).toEpochMilli(), durationSeconds = 40L,
        minimumDb = 35.0, averageDb = maxDb - 8, maximumDb = maxDb,
        location = "12 Westlake Ave", impact = "Woke me or someone in my home", notes = "",
        levelTrace = listOf(40, 50, maxDb.toInt(), 45),
    )

    @Test
    fun exhibitsRunOldestFirstAndCarryTheCityCheck() {
        val takes = listOf(
            take(2, "san-mateo-party_music", NoiseType.PARTY_MUSIC, LocalDateTime.of(2026, 9, 26, 23, 0), 72.0),
            take(1, "san-mateo-party_music", NoiseType.PARTY_MUSIC, LocalDateTime.of(2026, 9, 25, 15, 0), 48.0),
        )
        val plan = buildReportPlan(takes, catalog::byId, ZoneOffset.UTC, nowEpochMillis = 0L)
        assertEquals(listOf(1L, 2L), plan.exhibits.map { it.incident.id })
        assertEquals(listOf(1, 2), plan.exhibits.map { it.number })
        assertEquals(listOf("San Mateo"), plan.cities)
        assertEquals("Sep 25, 2026 – Sep 26, 2026", plan.dateRange)
        // 48 dB at 3 pm is below the 60 dB day base; 72 dB at 11 pm is above the 50 dB night base
        assertEquals("Condition not yet reached", plan.exhibits[0].statusWord())
        assertEquals(60.0, plan.exhibits[0].limitDbAtThatTime)
        assertEquals("Listed condition reached", plan.exhibits[1].statusWord())
        assertEquals(50.0, plan.exhibits[1].limitDbAtThatTime)
        assertTrue(plan.exhibits[1].assessment!!.conditions.any { it.text.startsWith("Limit:") && it.text.contains("at or above") })
        assertEquals(1, plan.sources.size)
        assertTrue(plan.sources[0].url.startsWith("http"))
    }

    @Test
    fun theIncidentCountSeenByEachExhibitIsTheCountBeforeIt() {
        val takes = (1..5).map { take(it.toLong(), "san-jose-barking-dog", NoiseType.BARKING_DOG, LocalDateTime.of(2026, 9, 20 + it, 8, 0), 60.0) }
        val plan = buildReportPlan(takes, catalog::byId, ZoneOffset.UTC, nowEpochMillis = 0L)
        assertTrue(plan.exhibits[0].assessment!!.conditions.any { it.text.contains("1 of 5") })
        assertTrue(plan.exhibits[4].assessment!!.conditions.any { it.text.contains("5 of 5") })
        assertEquals("Listed condition reached", plan.exhibits[4].statusWord())
        assertNotNull(plan.patternSentence)
    }

    @Test
    fun aTakeWithNoRuleOnFileStillGetsAPage() {
        val plan = buildReportPlan(listOf(take(1, "gone-city-rule", NoiseType.PARTY_MUSIC, LocalDateTime.of(2026, 9, 25, 15, 0), 50.0)), catalog::byId, ZoneOffset.UTC, 0L)
        assertEquals("No city rule on file", plan.exhibits[0].statusWord())
        assertEquals(emptyList<String>(), plan.cities)
    }
}
