package com.noisefile.app.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class CityStoreTest {
    private fun catalog(): RuleCatalog {
        val file = listOf(
            File("app/src/main/assets/${RuleCatalog.ASSET_PATH}"),
            File("src/main/assets/${RuleCatalog.ASSET_PATH}"),
        ).firstOrNull { it.isFile } ?: error("Could not find ${RuleCatalog.ASSET_PATH}")
        return RuleCatalog.fromJson(file.readText())
    }

    @Test
    fun nothingSavedMeansSanJose() {
        assertEquals(SavedCity(RuleCatalog.SAN_JOSE_ID, RuleCatalog.DEFAULT_RULE_ID), resolveSavedCity(null, catalog()))
    }

    @Test
    fun aSavedCityAndRuleComeBackAsIs() {
        val c = catalog()
        val rule = c.forJurisdiction("daly-city").first()
        assertEquals(SavedCity("daly-city", rule.id), resolveSavedCity(SavedCity("daly-city", rule.id), c))
    }

    @Test
    fun aStaleRuleKeepsTheCityAndTakesItsFirstRule() {
        val c = catalog()
        val first = c.forJurisdiction("oakland").first()
        assertEquals(SavedCity("oakland", first.id), resolveSavedCity(SavedCity("oakland", "gone-rule"), c))
    }

    @Test
    fun aRuleFromAnotherCityIsNotTrusted() {
        val c = catalog()
        val sfRule = c.forJurisdiction("san-francisco").first()
        val back = resolveSavedCity(SavedCity("oakland", sfRule.id), c)
        assertEquals("oakland", back.jurisdictionId)
        assertEquals(c.forJurisdiction("oakland").first().id, back.ruleId)
    }

    @Test
    fun anUnknownCityFallsBackToSanJose() {
        assertEquals(SavedCity(RuleCatalog.SAN_JOSE_ID, RuleCatalog.DEFAULT_RULE_ID), resolveSavedCity(SavedCity("atlantis", "x"), catalog()))
    }
}
