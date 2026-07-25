package com.noisefile.app.data

import com.noisefile.app.model.NoiseType
import com.noisefile.app.model.RuleWorkflow

object RuleCatalog {
    val sanJose: List<RuleWorkflow> = listOf(
        RuleWorkflow(
            id = "san-jose-barking-dog",
            jurisdiction = "San José, California",
            noiseType = NoiseType.BARKING_DOG,
            title = "Build a five-incident log",
            summary = "San José Animal Care requires a case number and a signed nuisance log with at least five documented incidents.",
            captureInstruction = "Record the start time, end time, duration, and how the barking interfered with the reasonable use of your home.",
            requiredIncidentCount = 5,
            nextAction = "Contact Animal Care first for a case number. After five incidents, submit the signed petition and original log.",
            actionLabel = "Call Animal Care",
            actionUri = "tel:4087947297",
            officialSourceLabel = "San José Animal Nuisance Petition",
            officialSourceUrl = "https://www.sanjoseca.gov/home/showpublisheddocument/10831/636664003494630000",
            verifiedDate = "2026-07-25",
        ),
        RuleWorkflow(
            id = "san-jose-party-music",
            jurisdiction = "San José, California",
            noiseType = NoiseType.PARTY_MUSIC,
            title = "Report it while it is happening",
            summary = "San José directs active noise disturbances to the Police Department's non-emergency line.",
            captureInstruction = "Measure from the place where the disturbance affects you. Keep a separate record of each date, time, and duration.",
            requiredIncidentCount = null,
            nextAction = "For an active ordinary noise disturbance, call the police non-emergency line. Use emergency services only for an immediate threat.",
            actionLabel = "Call non-emergency",
            actionUri = "tel:4082778900",
            officialSourceLabel = "City of San José contact directory",
            officialSourceUrl = "https://www.sanjoseca.gov/your-government/departments-offices/information-technology/customer-service",
            verifiedDate = "2026-07-25",
        ),
    )

    fun byId(id: String): RuleWorkflow =
        sanJose.firstOrNull { it.id == id } ?: sanJose.first()
}
