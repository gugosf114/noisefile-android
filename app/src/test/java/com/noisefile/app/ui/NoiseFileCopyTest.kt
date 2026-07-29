package com.noisefile.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NoiseFileCopyTest {
    @Test
    fun interfaceDoesNotDeclareViolationsFromAGenericDecibelCutoff() {
        val source = sourceFile().readText()

        assertFalse(source.contains("typical residential ordinance thresholds"))
        assertFalse(source.contains("Clear violation in most zones"))
        assertFalse(source.contains("avg >= 55"))
        assertFalse(source.contains("legally admissible", ignoreCase = true))
        assertFalse(source.contains("MORE EVIDENCE NEEDED"))
        assertTrue(source.contains("METER CANNOT DECIDE"))
        assertTrue(source.contains("City enforcement uses the required equipment"))
    }

    @Test
    fun reviewAndIncidentCardsLeadWithMaximumInsteadOfAverage() {
        val source = sourceFile().readText()

        assertTrue(source.contains("maximum estimated dB"))
        assertTrue(source.contains("dB max"))
        assertFalse(source.contains("estimated average dB"))
        assertFalse(source.contains("dB avg"))
    }

    @Test
    fun missingRequiredConditionUsesWarningColorInsteadOfSuccessColor() {
        val source = sourceFile().readText()

        assertTrue(
            source.contains(
                "MeterAssessmentStatus.DOES_NOT_REACH_LISTED_CONDITION -> Signal",
            ),
        )
        assertTrue(source.contains("RuleConditionOutcome.NOT_REACHED -> Signal"))
        assertFalse(
            source.contains(
                "MeterAssessmentStatus.DOES_NOT_REACH_LISTED_CONDITION -> Success",
            ),
        )
        assertFalse(source.contains("RuleConditionOutcome.NOT_REACHED -> Success"))
    }

    private fun sourceFile(): File {
        val relativePath = "app/src/main/java/com/noisefile/app/ui/NoiseFileApp.kt"
        return listOf(
            File(relativePath),
            File("../$relativePath"),
        ).firstOrNull { it.isFile }
            ?: error("Could not find $relativePath")
    }
}
