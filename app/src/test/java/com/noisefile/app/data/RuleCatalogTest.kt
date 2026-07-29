package com.noisefile.app.data

import com.noisefile.app.model.NoiseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONObject
import java.io.File

class RuleCatalogTest {
    private fun catalogJson(): String {
        val candidates = listOf(
            File("app/src/main/assets/${RuleCatalog.ASSET_PATH}"),
            File("src/main/assets/${RuleCatalog.ASSET_PATH}"),
        )
        val catalogFile = candidates.firstOrNull { it.isFile }
            ?: error("Could not find ${RuleCatalog.ASSET_PATH}")
        return catalogFile.readText()
    }

    private fun catalog(): RuleCatalog = RuleCatalog.fromJson(catalogJson())

    @Test
    fun sanJoseBarkingWorkflowRequiresFiveIncidents() {
        val rule = catalog().retrieve(RuleCatalog.SAN_JOSE_ID, NoiseType.BARKING_DOG)
            ?: error("Missing San Jose barking-dog rule")

        assertEquals(5, rule.requiredIncidentCount)
        assertTrue(rule.officialSourceUrl.startsWith("https://www.sanjoseca.gov/"))
    }

    @Test
    fun everyRuleHasAnOfficialSourceAndVerificationDate() {
        catalog().rules.forEach { rule ->
            assertTrue(rule.officialSourceUrl.startsWith("https://"))
            assertFalse(rule.officialSourceUrl.contains("local-government-website"))
            assertFalse(rule.officialSourceLabel.endsWith("/ Guidelines"))
            assertTrue(rule.verifiedDate.isNotBlank())
        }
    }

    @Test
    fun everyRuleHasSpecificResidentGuidance() {
        catalog().rules.forEach { rule ->
            assertFalse(rule.title == "Follow city procedures")
            assertFalse(rule.summary == "Follow the local municipal code guidelines.")
        }
    }

    @Test
    fun sanMateoRulesContainTheActualLocalRequirements() {
        val catalog = catalog()
        val barking = catalog.retrieve("san-mateo", NoiseType.BARKING_DOG)
            ?: error("Missing San Mateo barking rule")
        val music = catalog.retrieve("san-mateo", NoiseType.PARTY_MUSIC)
            ?: error("Missing San Mateo music rule")
        val construction = catalog.retrieve("san-mateo", NoiseType.CONSTRUCTION)
            ?: error("Missing San Mateo construction rule")

        assertTrue(barking.summary.contains("11:00 p.m."))
        assertTrue(music.summary.contains("7:00 a.m."))
        assertTrue(construction.summary.contains("Sundays and holidays from noon to 4:00 p.m."))
        assertTrue(construction.summary.contains("90 dB"))
    }

    @Test
    fun dalyCityBarkingRoutesToPoliceInsteadOfAnimalControl() {
        val rule = catalog().retrieve("daly-city", NoiseType.BARKING_DOG)
            ?: error("Missing Daly City barking rule")

        assertTrue(rule.actionUri.startsWith("tel:"))
        assertTrue(rule.nextAction.contains("Daly City Police"))
        assertFalse(rule.nextAction.contains("Peninsula Humane Society & SPCA to report"))
        assertEquals("https://phs-spca.org/laws/", rule.officialSourceUrl)
    }

    @Test
    fun unconfirmedRichmondBarkingAuthorityDoesNotShip() {
        assertNull(catalog().retrieve("richmond", NoiseType.BARKING_DOG))
    }

    @Test
    fun sanJoseIncludesConstructionWorkflow() {
        val construction = catalog().retrieve(RuleCatalog.SAN_JOSE_ID, NoiseType.CONSTRUCTION)
            ?: error("Missing San Jose construction rule")

        assertTrue(construction.summary.contains("7:00 a.m."))
        assertTrue(construction.summary.contains("prohibits it on weekends"))
        assertTrue(construction.officialSourceLabel.contains("20.100.450"))
    }

    @Test
    fun unavailableCityNeverFallsBackToAnotherCityRule() {
        val catalog = catalog()

        assertTrue(catalog.forJurisdiction("not-a-city").isEmpty())
        assertNull(catalog.retrieve("not-a-city", NoiseType.CONSTRUCTION))
        assertNull(catalog.jurisdictionById("not-a-city"))
        assertNull(catalog.byId("not-a-rule"))
    }

    @Test
    fun catalogIsVersionedAndUsesSupportedSchema() {
        val catalog = catalog()

        assertEquals(1, catalog.schemaVersion)
        assertTrue(catalog.catalogVersion.isNotBlank())
    }

    @Test
    fun duplicateCityAndCategoryIsRejected() {
        val root = JSONObject(catalogJson())
        val rules = root.getJSONArray("rules")
        val duplicate = JSONObject(rules.getJSONObject(0).toString())
            .put("id", "duplicate-rule-id")
        rules.put(duplicate)

        assertThrows(IllegalArgumentException::class.java) {
            RuleCatalog.fromJson(root.toString())
        }
    }

    @Test
    fun insecureOfficialSourceIsRejected() {
        val root = JSONObject(catalogJson())
        root.getJSONArray("rules")
            .getJSONObject(0)
            .put("officialSourceUrl", "http://example.com/not-official")

        assertThrows(IllegalArgumentException::class.java) {
            RuleCatalog.fromJson(root.toString())
        }
    }

    @Test
    fun placeholderOfficialSourceIsRejected() {
        val root = JSONObject(catalogJson())
        root.getJSONArray("rules")
            .getJSONObject(0)
            .put("officialSourceUrl", "https://local-government-website.gov")

        assertThrows(IllegalArgumentException::class.java) {
            RuleCatalog.fromJson(root.toString())
        }
    }
}
