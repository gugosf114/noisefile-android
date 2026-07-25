package com.noisefile.app.data

import com.noisefile.app.model.Jurisdiction
import com.noisefile.app.model.NoiseType
import com.noisefile.app.model.RuleWorkflow

object RuleCatalog {
    const val SAN_JOSE_ID = "san-jose"

    val jurisdictions: List<Jurisdiction> = listOf(
        Jurisdiction(
            id = SAN_JOSE_ID,
            displayName = "San José",
            region = "Santa Clara County",
            isAvailable = true,
        ),
        Jurisdiction(
            id = "san-francisco",
            displayName = "San Francisco",
            region = "Rule packet next",
            isAvailable = false,
        ),
        Jurisdiction(
            id = "oakland",
            displayName = "Oakland",
            region = "Rule packet next",
            isAvailable = false,
        ),
        Jurisdiction(
            id = "daly-city",
            displayName = "Daly City",
            region = "Rule packet next",
            isAvailable = false,
        ),
    )

    val sanJose: List<RuleWorkflow> = listOf(
        RuleWorkflow(
            id = "san-jose-barking-dog",
            jurisdictionId = SAN_JOSE_ID,
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
            jurisdictionId = SAN_JOSE_ID,
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
        RuleWorkflow(
            id = "san-jose-construction",
            jurisdictionId = SAN_JOSE_ID,
            jurisdiction = "San José, California",
            noiseType = NoiseType.CONSTRUCTION,
            title = "Check the permit and the clock",
            summary = "For construction covered by a development permit or planning approval within 500 feet of a residence, San José generally allows work only from 7:00 a.m. to 7:00 p.m., Monday through Friday. An approved permit may allow different hours.",
            captureInstruction = "Document the work address, activity, start and end times, approximate distance from the nearest home, and any posted permit or after-hours notice.",
            requiredIncidentCount = null,
            nextAction = "Check for a posted permit or approved exception. If the work appears outside its allowed hours, submit a Code Enforcement service request with the address and your incident history.",
            actionLabel = "Open service request",
            actionUri = "https://www.sanjoseca.gov/your-government/departments-offices/planning-building-code-enforcement/code-enforcement/request-service-check-status/code-service-request-form",
            officialSourceLabel = "San José Municipal Code §20.100.450",
            officialSourceUrl = "https://library.municode.com/ca/san_jose/codes/code_of_ordinances?nodeId=TIT20ZO_CH20.100ADPE_PT10ADPE_20.100.1240DIPE",
            verifiedDate = "2026-07-25",
        ),
    )

    fun forJurisdiction(jurisdictionId: String): List<RuleWorkflow> =
        sanJose.filter { it.jurisdictionId == jurisdictionId }

    fun jurisdictionById(id: String): Jurisdiction =
        jurisdictions.firstOrNull { it.id == id } ?: jurisdictions.first()

    fun byId(id: String): RuleWorkflow =
        sanJose.firstOrNull { it.id == id } ?: sanJose.first()
}
