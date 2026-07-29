package com.noisefile.app.data

import com.noisefile.app.model.MeterReading
import com.noisefile.app.model.RuleWorkflow
import java.time.LocalTime
import kotlin.math.roundToInt

enum class MeterAssessmentStatus {
    LISTENING,
    BELOW_LISTED_LIMIT,
    AT_OR_ABOVE_LISTED_LIMIT,
    CONDITIONS_REQUIRED,
}

data class MeterRuleAssessment(
    val status: MeterAssessmentStatus,
    val headline: String,
    val detail: String,
)

fun assessMeterReading(
    rule: RuleWorkflow,
    reading: MeterReading,
    localTime: LocalTime = LocalTime.now(),
): MeterRuleAssessment {
    if (reading.sampleWindows == 0) {
        return MeterRuleAssessment(
            status = MeterAssessmentStatus.LISTENING,
            headline = "Listening for a stable phone estimate…",
            detail = "NoiseFile will compare the highest estimate with a listed dB limit when this rule has one.",
        )
    }

    val meterLimit = rule.meterLimit
        ?: return MeterRuleAssessment(
            status = MeterAssessmentStatus.CONDITIONS_REQUIRED,
            headline = "One phone dB number cannot decide this rule",
            detail = "Use the city-specific requirement below. NoiseFile will not invent a cutoff.",
        )

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
    val observedRounded = observedDb.roundToInt()
    val limitRounded = limitDb.roundToInt()
    val isAtOrAbove = observedDb >= limitDb

    return MeterRuleAssessment(
        status = if (isAtOrAbove) {
            MeterAssessmentStatus.AT_OR_ABOVE_LISTED_LIMIT
        } else {
            MeterAssessmentStatus.BELOW_LISTED_LIMIT
        },
        headline = if (isAtOrAbove) {
            "$observedRounded dB estimate — at or above the listed$periodLabel $limitRounded dB limit"
        } else {
            "$observedRounded dB estimate — below the listed$periodLabel $limitRounded dB limit"
        },
        detail = meterLimit.comparisonContext,
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
