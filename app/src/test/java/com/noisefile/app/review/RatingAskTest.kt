package com.noisefile.app.review

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RatingAskTest {
    private val day = 24L * 60 * 60 * 1000
    private val now = 1_800_000_000_000L

    @Test fun firstFilingAsks() = assertTrue(RatingAsk.shouldAsk(lastAskedMs = 0L, nowMs = now))

    @Test fun aSecondFilingTheSameMonthDoesNotAsk() =
        assertFalse(RatingAsk.shouldAsk(lastAskedMs = now - 10 * day, nowMs = now))

    @Test fun oneDayShortOfTheGapDoesNotAsk() =
        assertFalse(RatingAsk.shouldAsk(lastAskedMs = now - 119 * day, nowMs = now))

    @Test fun afterTheGapItAsksAgain() =
        assertTrue(RatingAsk.shouldAsk(lastAskedMs = now - 120 * day, nowMs = now))
}
