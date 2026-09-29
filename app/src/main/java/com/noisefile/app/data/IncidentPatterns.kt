package com.noisefile.app.data

import com.noisefile.app.model.Incident
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/** When it happens: a 7-day by 24-hour count of saved takes, plus one plain sentence. */
object IncidentPatterns {
    /** grid[dayIndex][hour], dayIndex 0 = Monday. */
    fun grid(incidents: List<Incident>, zone: ZoneId): Array<IntArray> {
        val grid = Array(7) { IntArray(24) }
        incidents.forEach { incident ->
            val at = Instant.ofEpochMilli(incident.startedAtEpochMillis).atZone(zone)
            grid[at.dayOfWeek.value - 1][at.hour] += 1
        }
        return grid
    }

    /** "Most incidents: Friday and Saturday, 10 PM–1 AM." Null until there are three takes. */
    fun sentence(incidents: List<Incident>, zone: ZoneId): String? {
        if (incidents.size < 3) return null
        val grid = grid(incidents, zone)
        val byDay = (0 until 7).map { d -> d to grid[d].sum() }
        val byHour = (0 until 24).map { h -> h to (0 until 7).sumOf { d -> grid[d][h] } }
        val topDays = byDay.filter { it.second > 0 }.sortedByDescending { it.second }.take(2)
            .map { DayOfWeek.of(it.first + 1).getDisplayName(TextStyle.FULL, Locale.US) }
        // best 3-hour band, wrapping past midnight
        var bestStart = 0; var bestSum = -1
        for (start in 0 until 24) {
            val sum = (0 until 3).sumOf { byHour[(start + it) % 24].second }
            if (sum > bestSum) { bestSum = sum; bestStart = start }
        }
        val days = when (topDays.size) { 0 -> ""; 1 -> topDays[0]; else -> "${topDays[0]} and ${topDays[1]}" }
        return "Most incidents: $days, ${hourLabel(bestStart)}–${hourLabel((bestStart + 3) % 24)}."
    }

    fun hourLabel(hour: Int): String = when {
        hour == 0 -> "12 AM"
        hour < 12 -> "$hour AM"
        hour == 12 -> "12 PM"
        else -> "${hour - 12} PM"
    }
}
