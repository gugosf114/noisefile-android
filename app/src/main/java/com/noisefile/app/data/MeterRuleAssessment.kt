package com.noisefile.app.data

import com.noisefile.app.model.MeterReading
import com.noisefile.app.model.RuleWorkflow
import java.time.LocalDateTime
import java.time.LocalTime
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
