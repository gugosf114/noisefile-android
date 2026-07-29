package com.noisefile.app.data

import android.content.Context
import com.noisefile.app.model.Jurisdiction
import com.noisefile.app.model.NoiseType
import com.noisefile.app.model.RuleWorkflow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class RuleCatalog private constructor(
    val schemaVersion: Int,
    val catalogVersion: String,
    val jurisdictions: List<Jurisdiction>,
    val rules: List<RuleWorkflow>,
) {
    init {
        require(schemaVersion == SUPPORTED_SCHEMA_VERSION) {
            "Unsupported ordinance catalog schema: $schemaVersion"
        }
        require(catalogVersion.isNotBlank()) { "Catalog version is required." }
        require(jurisdictions.isNotEmpty()) { "At least one jurisdiction is required." }
        require(rules.isNotEmpty()) { "At least one verified rule is required." }
        require(jurisdictions.map { it.id }.distinct().size == jurisdictions.size) {
            "Jurisdiction IDs must be unique."
        }
        require(rules.map { it.id }.distinct().size == rules.size) {
            "Rule IDs must be unique."
        }
        require(
            rules
                .map { it.jurisdictionId to it.noiseType }
                .distinct()
                .size == rules.size,
        ) {
            "Each jurisdiction and noise category may have only one active rule."
        }

        val jurisdictionIds = jurisdictions.map { it.id }.toSet()
        require(rules.all { it.jurisdictionId in jurisdictionIds }) {
            "Every rule must reference a known jurisdiction."
        }
        require(
            jurisdictions
                .filter { it.isAvailable }
                .all { jurisdiction -> rules.any { it.jurisdictionId == jurisdiction.id } },
        ) {
            "Every available jurisdiction must contain at least one verified rule."
        }
        require(jurisdictions.any { it.id == SAN_JOSE_ID && it.isAvailable }) {
            "The default jurisdiction must exist and be available."
        }
        require(
            rules.any {
                it.id == DEFAULT_RULE_ID &&
                    it.jurisdictionId == SAN_JOSE_ID
            },
        ) {
            "The default rule must exist in the default jurisdiction."
        }
        require(
            jurisdictions.all {
                it.id.isNotBlank() &&
                    it.displayName.isNotBlank() &&
                    it.region.isNotBlank()
            },
        ) {
            "Jurisdiction fields may not be blank."
        }
        require(
            rules.all {
                it.id.isNotBlank() &&
                    it.jurisdictionId.isNotBlank() &&
                    it.jurisdiction.isNotBlank() &&
                    it.title.isNotBlank() &&
                    it.summary.isNotBlank() &&
                    it.captureInstruction.isNotBlank() &&
                    it.nextAction.isNotBlank() &&
                    it.actionLabel.isNotBlank() &&
                    it.officialSourceLabel.isNotBlank()
            },
        ) {
            "Required rule fields may not be blank."
        }
        require(rules.all { it.officialSourceUrl.startsWith("https://") }) {
            "Official sources must use HTTPS."
        }
        require(
            rules.all {
                it.actionUri.startsWith("https://") || it.actionUri.startsWith("tel:")
            },
        ) {
            "Rule actions must use HTTPS or a telephone URI."
        }
        require(rules.all { it.requiredIncidentCount == null || it.requiredIncidentCount > 0 }) {
            "Required incident counts must be positive."
        }
        require(
            rules.all { rule ->
                runCatching { LocalDate.parse(rule.verifiedDate) }.isSuccess
            },
        ) {
            "Verification dates must use ISO YYYY-MM-DD format."
        }
    }

    fun forJurisdiction(jurisdictionId: String): List<RuleWorkflow> =
        rules.filter { it.jurisdictionId == jurisdictionId }

    fun retrieve(jurisdictionId: String, noiseType: NoiseType): RuleWorkflow? =
        rules.firstOrNull {
            it.jurisdictionId == jurisdictionId && it.noiseType == noiseType
        }

    fun jurisdictionById(id: String): Jurisdiction? =
        jurisdictions.firstOrNull { it.id == id }

    fun byId(id: String): RuleWorkflow? =
        rules.firstOrNull { it.id == id }

    companion object {
        const val SAN_JOSE_ID = "san-jose"
        const val DEFAULT_RULE_ID = "san-jose-barking-dog"
        const val ASSET_PATH = "rules/catalog-v1.json"
        private const val SUPPORTED_SCHEMA_VERSION = 1

        fun fromAssets(context: Context): RuleCatalog {
            val json = context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
            return fromJson(json)
        }

        fun fromJson(json: String): RuleCatalog {
            val root = JSONObject(json)
            return RuleCatalog(
                schemaVersion = root.getInt("schemaVersion"),
                catalogVersion = root.getString("catalogVersion"),
                jurisdictions = root.getJSONArray("jurisdictions").mapObjects { item ->
                    Jurisdiction(
                        id = item.getString("id"),
                        displayName = item.getString("displayName"),
                        region = item.getString("region"),
                        isAvailable = item.getBoolean("available"),
                    )
                },
                rules = root.getJSONArray("rules").mapObjects { item ->
                    RuleWorkflow(
                        id = item.getString("id"),
                        jurisdictionId = item.getString("jurisdictionId"),
                        jurisdiction = item.getString("jurisdiction"),
                        noiseType = NoiseType.valueOf(item.getString("noiseType")),
                        title = item.getString("title"),
                        summary = item.getString("summary"),
                        captureInstruction = item.getString("captureInstruction"),
                        requiredIncidentCount = if (item.isNull("requiredIncidentCount")) {
                            null
                        } else {
                            item.getInt("requiredIncidentCount")
                        },
                        nextAction = item.getString("nextAction"),
                        actionLabel = item.getString("actionLabel"),
                        actionUri = item.getString("actionUri"),
                        secondaryActionLabel = if (item.isNull("secondaryActionLabel")) null else item.getString("secondaryActionLabel"),
                        secondaryActionUri = if (item.isNull("secondaryActionUri")) null else item.getString("secondaryActionUri"),
                        officialSourceLabel = item.getString("officialSourceLabel"),
                        officialSourceUrl = item.getString("officialSourceUrl"),
                        verifiedDate = item.getString("verifiedDate"),
                    )
                },
            )
        }

        private inline fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
            buildList {
                for (index in 0 until length()) {
                    add(transform(getJSONObject(index)))
                }
            }
    }
}
