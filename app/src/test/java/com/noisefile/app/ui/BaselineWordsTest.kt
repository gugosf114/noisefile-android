package com.noisefile.app.ui

import com.noisefile.app.data.RuleCatalog
import com.noisefile.app.model.NoiseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BaselineWordsTest {
    private val catalog = RuleCatalog.fromJson(
        listOf(File("app/src/main/assets/rules/catalog-v1.json"), File("../app/src/main/assets/rules/catalog-v1.json"))
            .first { it.isFile }.readText(),
    )

    @Test
    fun citiesWithARecipeSayTheirMinutesAndTheOthersSayItIsOurDefault() {
        val sf = catalog.retrieve("san-francisco", NoiseType.PARTY_MUSIC)!!
        assertEquals(
            "San Francisco's code measures the quiet for 10 minutes and judges noise by the jump above it.",
            baselineRuleLine(sf, 5),
        )
        assertEquals("San Francisco's code", baselineMinutesSource(sf))
        val hayward = catalog.retrieve("hayward", NoiseType.PARTY_MUSIC)!!
        val line = baselineRuleLine(hayward, 5)
        assertTrue(line, line.startsWith("Hayward's code sets "))
        assertTrue(line, line.contains("does not ask for a baseline"))
        assertTrue(line, line.endsWith("Our default is 5 minutes."))
        assertEquals("not in Hayward's code; our default", baselineMinutesSource(hayward))
    }

    @Test
    fun everyRuleGetsOneHonestLine() {
        catalog.rules.forEach { rule ->
            val line = baselineRuleLine(rule, 5)
            assertTrue(rule.id, line.contains("'s code"))
            assertTrue(rule.id, (rule.ambientRecipe != null) == line.contains("judges noise by the jump"))
        }
    }
}
