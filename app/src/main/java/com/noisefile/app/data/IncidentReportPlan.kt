package com.noisefile.app.data

import com.noisefile.app.model.AmbientReading
import com.noisefile.app.model.Incident
import com.noisefile.app.model.MeterReading
import com.noisefile.app.model.RuleWorkflow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Everything the PDF says, worked out with no drawing code, so it can be tested.
 * Exhibits run oldest to newest, numbered from 1, the way a log reads.
 */
data class ReportExhibit(
    val number: Int,
    val incident: Incident,
    val rule: RuleWorkflow?,
    val assessment: MeterRuleAssessment?,
    val whenLabel: String,
    /** The dB limit that applied at that time, for the chart line. */
    val limitDbAtThatTime: Double?,
)

data class ReportSource(val city: String, val label: String, val url: String, val verifiedDate: String)

data class ReportPlan(
    val title: String,
    val preparedOn: String,
    val cities: List<String>,
    val dateRange: String,
    val exhibits: List<ReportExhibit>,
    val patternSentence: String?,
    val patternGrid: Array<IntArray>,
    val seal: EvidenceSeal.Report,
    val sources: List<ReportSource>,
) {
    val statusCounts: Map<MeterAssessmentStatus, Int>
        get() = exhibits.mapNotNull { it.assessment?.status }.groupingBy { it }.eachCount()
}

private val WHEN = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy · h:mm a", Locale.US)
private val DAY = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)

fun buildReportPlan(
    incidents: List<Incident>,
    ruleFor: (String) -> RuleWorkflow?,
    zone: ZoneId = ZoneId.systemDefault(),
    nowEpochMillis: Long = System.currentTimeMillis(),
): ReportPlan {
    val ordered = incidents.sortedBy { it.startedAtEpochMillis }
    val seenPerRule = HashMap<String, Int>()
    val exhibits = ordered.mapIndexed { index, incident ->
        val rule = ruleFor(incident.ruleId)
        val at = Instant.ofEpochMilli(incident.startedAtEpochMillis).atZone(zone)
        val earlier = seenPerRule[incident.ruleId] ?: 0
        seenPerRule[incident.ruleId] = earlier + 1
        val assessment = rule?.let {
            assessMeterReading(
                rule = it,
                reading = MeterReading(
                    currentDb = incident.maximumDb,
                    minimumDb = incident.minimumDb,
                    averageDb = incident.averageDb,
                    maximumDb = incident.maximumDb,
                    elapsedMillis = incident.durationSeconds * 1_000L,
                    sampleWindows = 1_000,
                ),
                incidentCount = earlier,
                localDateTime = at.toLocalDateTime(),
                ambient = if (incident.ambientDb != null && incident.ambientSeconds != null) {
                    AmbientReading(db = incident.ambientDb, seconds = incident.ambientSeconds, sampleWindows = 1_000)
                } else null,
            )
        }
        val limit = rule?.meterLimit?.let { limit ->
            limit.fixedMaximumDb ?: run {
                val hour = at.hour
                val dayStart = limit.daytimeStartsHour ?: 7
                val nightStart = limit.nighttimeStartsHour ?: 22
                val isDay = if (dayStart < nightStart) hour in dayStart until nightStart else hour >= dayStart || hour < nightStart
                if (isDay) limit.daytimeMaximumDb else limit.nighttimeMaximumDb
            }
        }
        ReportExhibit(
            number = index + 1,
            incident = incident,
            rule = rule,
            assessment = assessment,
            whenLabel = WHEN.format(at),
            limitDbAtThatTime = limit,
        )
    }
    val cities = exhibits.mapNotNull { it.rule?.jurisdiction?.substringBefore(",") }.distinct()
    val range = if (ordered.isEmpty()) "" else {
        val first = DAY.format(Instant.ofEpochMilli(ordered.first().startedAtEpochMillis).atZone(zone))
        val last = DAY.format(Instant.ofEpochMilli(ordered.last().startedAtEpochMillis).atZone(zone))
        if (first == last) first else "$first – $last"
    }
    val sources = exhibits.mapNotNull { it.rule }.distinctBy { it.id }.map {
        ReportSource(it.jurisdiction.substringBefore(","), it.officialSourceLabel, it.officialSourceUrl, it.verifiedDate)
    }
    return ReportPlan(
        title = "NoiseFile Incident Report",
        preparedOn = WHEN.format(Instant.ofEpochMilli(nowEpochMillis).atZone(zone)),
        cities = cities,
        dateRange = range,
        exhibits = exhibits,
        patternSentence = IncidentPatterns.sentence(incidents, zone),
        patternGrid = IncidentPatterns.grid(incidents, zone),
        seal = EvidenceSeal.verify(incidents),
        sources = sources,
    )
}

/** One line for the summary table. */
fun ReportExhibit.statusWord(): String = when (assessment?.status) {
    MeterAssessmentStatus.REACHES_LISTED_CONDITION -> "Listed condition reached"
    MeterAssessmentStatus.DOES_NOT_REACH_LISTED_CONDITION -> "Condition not yet reached"
    MeterAssessmentStatus.NEEDS_INFORMATION -> "Needs the city's own test"
    MeterAssessmentStatus.LISTENING, null -> "No city rule on file"
}
