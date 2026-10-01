package com.noisefile.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONArray

class BaselineStoreTest {
    @Test
    fun aBaselineRoundTripsWithItsCodeMinutes() {
        val b = Baseline("Bedroom", 28.0, 300L, 1_700_000L, codeMinutes = 10, cityName = "San Francisco")
        val back = parseBaselines(JSONArray().put(b.toJson()).toString()).single()
        assertEquals(b, back)
        val none = Baseline("Kitchen", 40.0, 120L, 5L, codeMinutes = null, cityName = "Hayward")
        assertEquals(none, parseBaselines(JSONArray().put(none.toJson()).toString()).single())
    }

    @Test
    fun damagedStorageReadsAsNoBaselines() {
        assertTrue(parseBaselines("not json").isEmpty())
    }

    @Test
    fun theRoomListIsShortAndEndsWithOther() {
        assertEquals(6, Rooms.common.size)
        assertEquals("Other", Rooms.OTHER)
        assertTrue(Rooms.common.all { it.length <= 12 })
    }
}
