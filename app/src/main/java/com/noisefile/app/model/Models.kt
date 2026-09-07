package com.noisefile.app.model

enum class NoiseType(val displayName: String) {
    BARKING_DOG("Animal noise (e.g. barking, pets)"),
    PARTY_MUSIC("General noise (e.g. voices, music)"),
    CONSTRUCTION("Mechanical or Construction"),
}

data class Jurisdiction(
    val id: String,
    val displayName: String,
    val region: String,
    val isAvailable: Boolean,
)

data class RuleWorkflow(
    val id: String,
    val jurisdictionId: String,
    val jurisdiction: String,
    val noiseType: NoiseType,
    val title: String,
    val summary: String,
    val captureInstruction: String,
    val requiredIncidentCount: Int?,
    val nextAction: String,
    val actionLabel: String,
    val actionUri: String,
    val secondaryActionLabel: String? = null,
    val secondaryActionUri: String? = null,
    val officialSourceLabel: String,
    val officialSourceUrl: String,
    val verifiedDate: String,
    val meterLimit: MeterLimit? = null,
    val hoursRule: HoursRule? = null,
)

data class MeterLimit(
    val fixedMaximumDb: Double? = null,
    val daytimeMaximumDb: Double? = null,
    val nighttimeMaximumDb: Double? = null,
    val daytimeStartsHour: Int? = null,
    val nighttimeStartsHour: Int? = null,
    val comparisonContext: String,
) {
    init {
        val hasFixedLimit = fixedMaximumDb != null
        val hasScheduledLimits = daytimeMaximumDb != null || nighttimeMaximumDb != null

        require(hasFixedLimit.xor(hasScheduledLimits)) {
            "A meter limit must define either one fixed maximum or day/night maximums."
        }
        require(fixedMaximumDb == null || fixedMaximumDb > 0.0)
        if (hasScheduledLimits) {
            require(daytimeMaximumDb != null && daytimeMaximumDb > 0.0)
            require(nighttimeMaximumDb != null && nighttimeMaximumDb > 0.0)
            require(daytimeStartsHour != null && daytimeStartsHour in 0..23)
            require(nighttimeStartsHour != null && nighttimeStartsHour in 0..23)
            require(daytimeStartsHour != nighttimeStartsHour)
        }
        require(comparisonContext.isNotBlank())
    }
}

data class MeterReading(
    val currentDb: Double = 0.0,
    val minimumDb: Double = 0.0,
    val averageDb: Double = 0.0,
    val maximumDb: Double = 0.0,
    val elapsedMillis: Long = 0L,
    val sampleWindows: Int = 0,
)

data class Incident(
    val id: Long,
    val ruleId: String,
    val noiseType: NoiseType,
    val startedAtEpochMillis: Long,
    val durationSeconds: Long,
    val minimumDb: Double,
    val averageDb: Double,
    val maximumDb: Double,
    val location: String,
    val impact: String,
    val notes: String,
)

/** How a published schedule reads: the windows are when noise is allowed, or when it is restricted. */
enum class HoursKind {
    /** Noise is allowed only inside the listed windows, e.g. construction hours. */
    ALLOWED,

    /** The listed windows are the restricted period, e.g. quiet hours. */
    QUIET,
}

enum class DayGroup {
    WEEKDAY,
    SATURDAY,
    SUNDAY,
    ALL,
}

data class HoursWindow(
    val days: DayGroup,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
) {
    init {
        require(startMinuteOfDay in 0..1439) { "A window must start at a minute of the day." }
        require(endMinuteOfDay in 0..1439) { "A window must end at a minute of the day." }
        require(startMinuteOfDay != endMinuteOfDay) { "A window must span some time." }
    }

    /** A window whose end comes before its start crosses midnight, like 10:00 PM to 6:00 AM. */
    fun contains(minuteOfDay: Int): Boolean =
        if (startMinuteOfDay < endMinuteOfDay) {
            minuteOfDay >= startMinuteOfDay && minuteOfDay < endMinuteOfDay
        } else {
            minuteOfDay >= startMinuteOfDay || minuteOfDay < endMinuteOfDay
        }
}

/**
 * A schedule the ordinance publishes. The phone's clock is exact, so the app can say
 * whether the incident falls inside or outside it. The clock alone is never the
 * violation: permits, conditions of approval and the ordinance's own wording decide,
 * which is why the context sentence travels with every schedule.
 */
data class HoursRule(
    val kind: HoursKind,
    val windows: List<HoursWindow>,
    val context: String,
) {
    init {
        require(windows.isNotEmpty()) { "An hours rule needs at least one window." }
        require(context.isNotBlank()) { "An hours rule needs its context sentence." }
    }
}
