package com.noisefile.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TourTest {
    @Test
    fun fiveStopsInTheOrderAPersonUsesTheApp() {
        assertEquals(
            listOf(TourStep.CITY, TourStep.TYPES, TourStep.QUIET, TourStep.RECORD, TourStep.INCIDENTS),
            TourStep.entries,
        )
        assertEquals(TourStep.TYPES, TourStep.CITY.next)
        assertNull(TourStep.INCIDENTS.next)
    }

    @Test
    fun everyStopIsShortAndPlain() {
        TourStep.entries.forEach { step ->
            assertTrue(step.name, step.title.length <= 40)
            assertTrue(step.name, step.body("\$7.99").length <= 230)
            assertTrue(step.name, step.body(null).isNotBlank())
        }
    }

    @Test
    fun theLastStopNamesThePaidPartWithPlaysOwnPrice() {
        val withPrice = TourStep.INCIDENTS.body("\$7.99")
        assertTrue(withPrice, withPrice.contains("stay free"))
        assertTrue(withPrice, withPrice.endsWith("\$7.99, once."))
        assertTrue(TourStep.INCIDENTS.body(null).endsWith("paid once."))
    }

    @Test
    fun theQuietStopTellsThemTheyMaySkipIt() {
        assertTrue(TourStep.QUIET.body(null).contains("Skip it if the noise is already going"))
    }
}
