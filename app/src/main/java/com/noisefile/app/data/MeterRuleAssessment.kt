package com.noisefile.app.data

import com.noisefile.app.model.AmbientReading
import com.noisefile.app.model.DayGroup
import com.noisefile.app.model.HoursKind
import com.noisefile.app.model.HoursRule
import com.noisefile.app.model.MeterReading
import com.noisefile.app.model.RuleWorkflow
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
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
    ambient: AmbientReading? = null,
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

        rule.hoursRule?.let { hours ->
            add(hoursCondition(hours, rule, localDateTime))
        }

        ambient?.let { baseline ->
            add(ambientCondition(baseline, rule, reading))
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
            headline = "This ${reading.maximumDb.roundToInt()} dB reading cannot pass or fail the ${rule.jurisdiction.substringBefore(",")} rule by itself",
            detail = "The city uses the specific requirement below. The phone reading is supporting evidence.",
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

private fun isWithinDaytime(
    localTime: LocalTime,
    daytimeStartsHour: Int,
    nighttimeStartsHour: Int,
): Boolean {
    val daytimeStart = LocalTime.of(daytimeStartsHour, 0)
    val nighttimeStart = LocalTime.of(nighttimeStartsHour, 0)
    return !localTime.isBefore(daytimeStart) && localTime.isBefore(nighttimeStart)
}

/**
 * The published schedule against the phone's clock. The clock is exact, so the line
 * says plainly whether the incident falls inside or outside the schedule. It is
 * reported as information, never as a verdict: permits and conditions of approval
 * can move construction hours, and quiet-hour rules still require the noise to
 * disturb someone. So this line never flips the headline on its own.
 */
private fun hoursCondition(
    hours: HoursRule,
    rule: RuleWorkflow,
    localDateTime: LocalDateTime,
): RuleConditionResult {
    val minuteOfDay = localDateTime.hour * 60 + localDateTime.minute
    val group = when (localDateTime.dayOfWeek) {
        DayOfWeek.SATURDAY -> DayGroup.SATURDAY
        DayOfWeek.SUNDAY -> DayGroup.SUNDAY
        else -> DayGroup.WEEKDAY
    }
    val inside = hours.windows
        .filter { it.days == group || it.days == DayGroup.ALL }
        .any { it.contains(minuteOfDay) }
    val city = rule.jurisdiction.substringBefore(",")
    val stamp = localDateTime.format(DateTimeFormatter.ofPattern("h:mm a EEEE", Locale.US))
    val schedule = describeSchedule(hours)
    val position = if (inside) "inside" else "outside"
    val name = when (hours.kind) {
        HoursKind.ALLOWED -> "published construction hours"
        HoursKind.QUIET -> "published quiet hours"
    }
    return RuleConditionResult(
        outcome = RuleConditionOutcome.NEEDS_INFORMATION,
        text = "Time: $stamp is $position $city's $name ($schedule). ${hours.context}",
    )
}

private fun describeSchedule(hours: HoursRule): String {
    val labels = mapOf(
        DayGroup.WEEKDAY to "weekdays",
        DayGroup.SATURDAY to "Saturdays",
        DayGroup.SUNDAY to "Sundays",
        DayGroup.ALL to "every day",
    )
    val parts = mutableListOf<String>()
    for (group in listOf(DayGroup.WEEKDAY, DayGroup.SATURDAY, DayGroup.SUNDAY, DayGroup.ALL)) {
        val windows = hours.windows.filter { it.days == group }
        if (windows.isEmpty()) continue
        val spans = windows.joinToString(", ") { window ->
            "${clockLabel(window.startMinuteOfDay)}-${clockLabel(window.endMinuteOfDay)}"
        }
        parts.add("${labels.getValue(group)} $spans")
    }
    if (hours.kind == HoursKind.ALLOWED && hours.windows.none { it.days == DayGroup.ALL }) {
        if (hours.windows.none { it.days == DayGroup.SATURDAY }) parts.add("no Saturdays")
        if (hours.windows.none { it.days == DayGroup.SUNDAY }) parts.add("no Sundays")
    }
    return parts.joinToString("; ")
}

private fun clockLabel(minuteOfDay: Int): String =
    LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
        .format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))

/**
 * The jump: the noise against the quiet baseline the same phone took in the same
 * spot. The phone's unknown offset is the same on both, so it cancels out of the
 * difference. Reported as information, never as a verdict: the city measures its
 * ambient with its own meter and its own recipe, quoted from the rule.
 */
private fun ambientCondition(
    baseline: AmbientReading,
    rule: RuleWorkflow,
    reading: MeterReading,
): RuleConditionResult {
    val city = rule.jurisdiction.substringBefore(",")
    val peakDifference = (reading.maximumDb - baseline.db).roundToInt()
    val averageDifference = (reading.averageDb - baseline.db).roundToInt()
    val recipe = rule.ambientRecipe?.note
        ?: "$city's code sets no ambient recipe, so this difference is context, not a listed condition."
    return RuleConditionResult(
        outcome = RuleConditionOutcome.NEEDS_INFORMATION,
        text = "Sound: ${reading.maximumDb.roundToInt()} dB highest estimate is " +
            "${describeDifference(peakDifference)} your ${baselineLabel(baseline.seconds)} quiet baseline of " +
            "${baseline.db.roundToInt()} dB; the ${reading.averageDb.roundToInt()} dB average is " +
            "${describeDifference(averageDifference)} it. Both numbers come from this phone in this spot, " +
            "so the phone's own offset cancels out of the difference. $recipe",
    )
}

private fun describeDifference(differenceDb: Int): String = when {
    differenceDb > 0 -> "$differenceDb dB above"
    differenceDb < 0 -> "${abs(differenceDb)} dB below"
    else -> "level with"
}

/** "6-minute" when the capture ran whole minutes, otherwise "4:30". */
internal fun baselineLabel(seconds: Long): String =
    if (seconds >= 60 && seconds % 60 == 0L) {
        "${seconds / 60}-minute"
    } else {
        "%d:%02d".format(Locale.US, seconds / 60, seconds % 60)
    }
