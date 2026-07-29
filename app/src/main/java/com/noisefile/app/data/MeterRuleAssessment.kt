package com.noisefile.app.data

import com.noisefile.app.model.MeterReading
import com.noisefile.app.model.RuleWorkflow
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

enum class MeterAssessmentStatus {
    LISTENING,
    REACHES_LISTED_CONDITION,
    DOES_NOT_REACH_LISTED_CONDITION,
    NEEDS_INFORMATION,
}

enum class RuleConditionOutcome {
    REACHED,
    NOT_REACHED,
    NEEDS_INFORMATION,
}

data class RuleConditionResult(
    val outcome: RuleConditionOutcome,
    val text: String,
)

data class MeterRuleAssessment(
    val status: MeterAssessmentStatus,
    val headline: String,
    val detail: String,
    val conditions: List<RuleConditionResult>,
)

fun assessMeterReading(
    rule: RuleWorkflow,
    reading: MeterReading,
    incidentCount: Int = 0,
    localDateTime: LocalDateTime = LocalDateTime.now(),
): MeterRuleAssessment {
    if (reading.sampleWindows == 0) {
        return MeterRuleAssessment(
            status = MeterAssessmentStatus.LISTENING,
            headline = "Listening for a stable phone estimate…",
            detail = "NoiseFile will check every condition it can read from this incident.",
            conditions = listOf(
                RuleConditionResult(
                    outcome = RuleConditionOutcome.NEEDS_INFORMATION,
                    text = "City requirement: ${rule.summary}",
                ),
            ),
        )
    }

    val conditions = buildList {
        rule.meterLimit?.let { meterLimit ->
            add(meterCondition(meterLimit, reading, localDateTime.toLocalTime()))
        }

        rule.requiredIncidentCount?.let { requiredCount ->
            val countIncludingCurrent = incidentCount + 1
            add(
                RuleConditionResult(
                    outcome = if (countIncludingCurrent >= requiredCount) {
                        RuleConditionOutcome.REACHED
                    } else {
                        RuleConditionOutcome.NOT_REACHED
                    },
                    text = if (countIncludingCurrent >= requiredCount) {
                        "Incident log: this recording reaches $countIncludingCurrent of $requiredCount required incidents."
                    } else {
                        val remaining = requiredCount - countIncludingCurrent
                        "Incident log: $countIncludingCurrent of $requiredCount including this recording; $remaining more needed."
                    },
                ),
            )
        }

        automaticProfiles[rule.id]?.let { profile ->
            profile.minimumDurationSeconds?.let { requiredSeconds ->
                val elapsedSeconds = reading.elapsedMillis / 1_000L
                add(
                    RuleConditionResult(
                        outcome = if (elapsedSeconds >= requiredSeconds) {
                            RuleConditionOutcome.REACHED
                        } else {
                            RuleConditionOutcome.NOT_REACHED
                        },
                        text = if (elapsedSeconds >= requiredSeconds) {
                            "Duration: ${formatDuration(elapsedSeconds)} recorded; the ${formatDuration(requiredSeconds)} listed duration is reached."
                        } else {
                            "Duration: ${formatDuration(elapsedSeconds)} of ${formatDuration(requiredSeconds)}; ${formatDuration(requiredSeconds - elapsedSeconds)} more needed."
                        },
                    ),
                )
            }

            profile.restrictedHours?.let { restrictedHours ->
                val currentTime = localDateTime.toLocalTime()
                val isRestricted = restrictedHours.contains(currentTime)
                add(
                    RuleConditionResult(
                        outcome = if (isRestricted) {
                            RuleConditionOutcome.REACHED
                        } else {
                            RuleConditionOutcome.NOT_REACHED
                        },
                        text = if (isRestricted) {
                            "Time: ${formatClock(currentTime)} is within the listed restricted hours, ${restrictedHours.label()}."
                        } else {
                            "Time: ${formatClock(currentTime)} is outside the listed restricted hours, ${restrictedHours.label()}."
                        },
                    ),
                )
            }

            profile.allowedSchedule?.let { schedule ->
                val currentTime = localDateTime.toLocalTime()
                val ranges = schedule.rangesFor(localDateTime.dayOfWeek)
                val isAllowed = ranges.any { it.contains(currentTime) }
                add(
                    RuleConditionResult(
                        outcome = if (isAllowed) {
                            RuleConditionOutcome.NOT_REACHED
                        } else {
                            RuleConditionOutcome.REACHED
                        },
                        text = if (isAllowed) {
                            "Time: ${formatDayAndClock(localDateTime)} is within regular allowed hours (${ranges.joinToString { it.label() }})."
                        } else if (ranges.isEmpty()) {
                            "Time: ${formatDayAndClock(localDateTime)} is outside regular allowed hours; none are listed for ${localDateTime.dayOfWeek.displayName()}."
                        } else {
                            "Time: ${formatDayAndClock(localDateTime)} is outside regular allowed hours (${ranges.joinToString { it.label() }})."
                        },
                    ),
                )
            }
        }

        add(
            RuleConditionResult(
                outcome = RuleConditionOutcome.NEEDS_INFORMATION,
                text = "Ordinance test: ${rule.summary}",
            ),
        )
        add(
            RuleConditionResult(
                outcome = RuleConditionOutcome.NEEDS_INFORMATION,
                text = "Still document: ${rule.captureInstruction}",
            ),
        )
    }

    val evaluatedConditions = conditions.filter {
        it.outcome != RuleConditionOutcome.NEEDS_INFORMATION
    }
    val hasReachedCondition = evaluatedConditions.any {
        it.outcome == RuleConditionOutcome.REACHED
    }

    return when {
        hasReachedCondition -> MeterRuleAssessment(
            status = MeterAssessmentStatus.REACHES_LISTED_CONDITION,
            headline = "This incident reaches at least one listed condition",
            detail = "The checks below show what matched and what still needs evidence.",
            conditions = conditions,
        )

        evaluatedConditions.isNotEmpty() -> MeterRuleAssessment(
            status = MeterAssessmentStatus.DOES_NOT_REACH_LISTED_CONDITION,
            headline = "This incident does not yet reach the checked condition",
            detail = "The checks below show exactly what is short. Another listed route may still apply.",
            conditions = conditions,
        )

        else -> MeterRuleAssessment(
            status = MeterAssessmentStatus.NEEDS_INFORMATION,
            headline = "This ordinance depends on evidence beyond the meter",
            detail = "The city requirement below explains what must be observed or documented.",
            conditions = conditions,
        )
    }
}

private fun meterCondition(
    meterLimit: com.noisefile.app.model.MeterLimit,
    reading: MeterReading,
    localTime: LocalTime,
): RuleConditionResult {
    val (limitDb, periodLabel) = if (meterLimit.fixedMaximumDb != null) {
        meterLimit.fixedMaximumDb to ""
    } else {
        val isDaytime = isWithinDaytime(
            localTime = localTime,
            daytimeStartsHour = checkNotNull(meterLimit.daytimeStartsHour),
            nighttimeStartsHour = checkNotNull(meterLimit.nighttimeStartsHour),
        )
        if (isDaytime) {
            checkNotNull(meterLimit.daytimeMaximumDb) to " daytime"
        } else {
            checkNotNull(meterLimit.nighttimeMaximumDb) to " nighttime"
        }
    }

    val observedDb = reading.maximumDb
    val differenceDb = abs(limitDb - observedDb).roundToInt()
    val isAtOrAbove = observedDb >= limitDb

    return RuleConditionResult(
        outcome = if (isAtOrAbove) {
            RuleConditionOutcome.REACHED
        } else {
            RuleConditionOutcome.NOT_REACHED
        },
        text = if (isAtOrAbove) {
            "Sound: ${observedDb.roundToInt()} dB highest estimate is $differenceDb dB at or above the listed$periodLabel ${limitDb.roundToInt()} dB limit. ${meterLimit.comparisonContext}"
        } else {
            "Sound: ${observedDb.roundToInt()} dB highest estimate is $differenceDb dB below the listed$periodLabel ${limitDb.roundToInt()} dB limit. ${meterLimit.comparisonContext}"
        },
    )
}

private data class AutomaticProfile(
    val minimumDurationSeconds: Long? = null,
    val restrictedHours: ClockRange? = null,
    val allowedSchedule: WeeklySchedule? = null,
)

private data class ClockRange(
    val start: LocalTime,
    val end: LocalTime,
) {
    fun contains(time: LocalTime): Boolean =
        if (start < end) {
            !time.isBefore(start) && time.isBefore(end)
        } else {
            !time.isBefore(start) || time.isBefore(end)
        }

    fun label(): String = "${formatClock(start)}–${formatClock(end)}"
}

private data class WeeklySchedule(
    val weekday: List<ClockRange>,
    val saturday: List<ClockRange>,
    val sunday: List<ClockRange>,
) {
    fun rangesFor(dayOfWeek: DayOfWeek): List<ClockRange> = when (dayOfWeek) {
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY -> weekday

        DayOfWeek.SATURDAY -> saturday
        DayOfWeek.SUNDAY -> sunday
    }
}

private val automaticProfiles = mapOf(
    "san-jose-construction" to AutomaticProfile(
        allowedSchedule = schedule(weekday = hours(7, 19)),
    ),
    "antioch-construction" to AutomaticProfile(
        allowedSchedule = schedule(
            weekday = hours(7, 18),
            saturday = hours(9, 17),
            sunday = hours(9, 17),
        ),
    ),
    "oakland-construction" to AutomaticProfile(
        allowedSchedule = schedule(
            weekday = hours(7, 19),
            saturday = hours(9, 20),
            sunday = hours(9, 20),
        ),
    ),
    "san-mateo-barking_dog" to AutomaticProfile(
        restrictedHours = hours(23, 7),
    ),
    "san-mateo-party_music" to AutomaticProfile(
        restrictedHours = hours(23, 7),
    ),
    "san-mateo-construction" to AutomaticProfile(
        allowedSchedule = schedule(
            weekday = hours(7, 19),
            saturday = hours(9, 17),
            sunday = hours(12, 16),
        ),
    ),
    "hayward-construction" to AutomaticProfile(
        allowedSchedule = schedule(
            weekday = hours(7, 19),
            saturday = hours(7, 19),
            sunday = hours(10, 18),
        ),
    ),
    "concord-construction" to AutomaticProfile(
        allowedSchedule = schedule(
            weekday = hours(7, 30, 18, 0),
            saturday = hours(8, 17),
            sunday = hours(8, 17),
        ),
    ),
    "berkeley-barking_dog" to AutomaticProfile(
        minimumDurationSeconds = 10 * 60,
    ),
    "berkeley-construction" to AutomaticProfile(
        allowedSchedule = schedule(
            weekday = hours(7, 19),
            saturday = hours(9, 20),
            sunday = hours(9, 20),
        ),
    ),
    "vallejo-construction" to AutomaticProfile(
        allowedSchedule = schedule(
            weekday = hours(7, 21),
            saturday = hours(7, 21),
            sunday = hours(7, 21),
        ),
    ),
    "richmond-party_music" to AutomaticProfile(
        minimumDurationSeconds = 5 * 60,
    ),
    "richmond-construction" to AutomaticProfile(
        allowedSchedule = schedule(
            weekday = hours(7, 19),
            saturday = hours(9, 20),
            sunday = hours(9, 20),
        ),
    ),
    "sunnyvale-party_music" to AutomaticProfile(
        restrictedHours = hours(22, 7),
    ),
    "sunnyvale-construction" to AutomaticProfile(
        allowedSchedule = schedule(
            weekday = hours(7, 18),
            saturday = hours(8, 17),
        ),
    ),
    "fremont-party-music" to AutomaticProfile(
        restrictedHours = hours(22, 7),
    ),
    "fremont-construction" to AutomaticProfile(
        allowedSchedule = schedule(
            weekday = hours(7, 19),
            saturday = hours(9, 18),
        ),
    ),
    "san-francisco-barking_dog" to AutomaticProfile(
        minimumDurationSeconds = 10 * 60,
    ),
    "san-francisco-construction" to AutomaticProfile(
        allowedSchedule = schedule(
            weekday = hours(7, 20),
            saturday = hours(7, 20),
            sunday = hours(7, 20),
        ),
    ),
    "daly-city-party-music" to AutomaticProfile(
        restrictedHours = hours(22, 6),
    ),
    "daly-city-construction" to AutomaticProfile(
        restrictedHours = hours(22, 6),
    ),
    "santa-clara-construction" to AutomaticProfile(
        allowedSchedule = schedule(
            weekday = hours(7, 18),
            saturday = hours(9, 18),
        ),
    ),
)

private fun schedule(
    weekday: ClockRange? = null,
    saturday: ClockRange? = null,
    sunday: ClockRange? = null,
) = WeeklySchedule(
    weekday = listOfNotNull(weekday),
    saturday = listOfNotNull(saturday),
    sunday = listOfNotNull(sunday),
)

private fun hours(startHour: Int, endHour: Int) =
    ClockRange(LocalTime.of(startHour, 0), LocalTime.of(endHour, 0))

private fun hours(
    startHour: Int,
    startMinute: Int,
    endHour: Int,
    endMinute: Int,
) = ClockRange(
    LocalTime.of(startHour, startMinute),
    LocalTime.of(endHour, endMinute),
)

private fun isWithinDaytime(
    localTime: LocalTime,
    daytimeStartsHour: Int,
    nighttimeStartsHour: Int,
): Boolean {
    val daytimeStart = LocalTime.of(daytimeStartsHour, 0)
    val nighttimeStart = LocalTime.of(nighttimeStartsHour, 0)
    return !localTime.isBefore(daytimeStart) && localTime.isBefore(nighttimeStart)
}

private val clockFormatter = DateTimeFormatter.ofPattern("h:mm a")

private fun formatClock(time: LocalTime): String =
    time.format(clockFormatter).lowercase()

private fun formatDayAndClock(dateTime: LocalDateTime): String =
    "${dateTime.dayOfWeek.displayName()} ${formatClock(dateTime.toLocalTime())}"

private fun DayOfWeek.displayName(): String =
    name.lowercase().replaceFirstChar { it.titlecase() }

private fun formatDuration(seconds: Long): String =
    "%d:%02d".format(seconds / 60, seconds % 60)
