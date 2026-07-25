package com.noisefile.app.data

import com.noisefile.app.model.NoiseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleCatalogTest {
    @Test
    fun sanJoseBarkingWorkflowRequiresFiveIncidents() {
        val rule = RuleCatalog.sanJose.first {
            it.noiseType == NoiseType.BARKING_DOG
        }

        assertEquals(5, rule.requiredIncidentCount)
        assertTrue(rule.officialSourceUrl.startsWith("https://www.sanjoseca.gov/"))
    }

    @Test
    fun everyRuleHasAnOfficialSourceAndVerificationDate() {
        RuleCatalog.sanJose.forEach { rule ->
            assertTrue(rule.officialSourceUrl.startsWith("https://"))
            assertTrue(rule.verifiedDate.isNotBlank())
        }
    }
}
