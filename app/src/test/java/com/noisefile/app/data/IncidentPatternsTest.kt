package com.noisefile.app.data

import com.noisefile.app.model.Incident
import com.noisefile.app.model.NoiseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class IncidentPatternsTest {
    private fun take(id: Long, at: LocalDateTime) = Incident(
        id = id, ruleId = "daly-city-party-music", noiseType = NoiseType.PARTY_MUSIC,
        startedAtEpochMillis = at.toInstant(ZoneOffset.UTC).toEpochMilli(), durationSeconds = 30L,
        minimumDb = 40.0, averageDb = 50.0, maximumDb = 60.0,
        location = "x", impact = "Woke me or someone in my home", notes = "",
    )

    @Test
    fun twoTakesGiveNoSentenceYet() {
        assertNull(IncidentPatterns.sentence(listOf(take(1, LocalDateTime.of(2026, 9, 25, 23, 0)), take(2, LocalDateTime.of(2026, 9, 26, 23, 30))), ZoneOffset.UTC))
    }

    @Test
    fun theGridCountsByWeekdayAndHour() {
        // 2026-09-25 is a Friday
        val grid = IncidentPatterns.grid(listOf(take(1, LocalDateTime.of(2026, 9, 25, 23, 10)), take(2, LocalDateTime.of(2026, 9, 25, 23, 50))), ZoneOffset.UTC)
        assertEquals(2, grid[4][23])
        assertEquals(0, grid[0][23])
    }

    @Test
    fun theSentenceNamesTheBusiestDaysAndTheBusiestThreeHours() {
        val takes = listOf(
            take(1, LocalDateTime.of(2026, 9, 25, 23, 10)), // Fri 11 PM
            take(2, LocalDateTime.of(2026, 9, 26, 0, 20)),  // Sat 12 AM
            take(3, LocalDateTime.of(2026, 9, 26, 23, 40)), // Sat 11 PM
            take(4, LocalDateTime.of(2026, 9, 25, 22, 0)),  // Fri 10 PM
        )
        // Friday and Saturday tie at two; the busiest three hours start at 10 PM (10 PM, 11 PM, 12 AM hold 1 + 2 + 1).
        assertEquals("Most incidents: Friday and Saturday, 10 PM–1 AM.", IncidentPatterns.sentence(takes, ZoneOffset.UTC))
    }
}
