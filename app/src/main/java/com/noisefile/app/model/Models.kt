package com.noisefile.app.model

enum class NoiseType(val displayName: String) {
    BARKING_DOG("Barking dog"),
    PARTY_MUSIC("Party or amplified music"),
    CONSTRUCTION("Construction"),
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
    val officialSourceLabel: String,
    val officialSourceUrl: String,
    val verifiedDate: String,
)

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
    val impact: String,
    val notes: String,
)
