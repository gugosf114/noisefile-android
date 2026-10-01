package com.noisefile.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuietFloorTest {
    @Test
    fun threeSneezesDoNotLiftAQuietRoom() {
        val room = MutableList(300) { 35 }
        room[40] = 70; room[120] = 68; room[200] = 72
        assertEquals(35.0, QuietFloor.of(room))
    }

    @Test
    fun aFridgeThatRunsHalfTheTimeStillReadsAsTheLowSide() {
        val room = List(300) { if (it % 2 == 0) 34 else 42 }
        assertEquals(34.0, QuietFloor.of(room))
    }

    @Test
    fun aSteadyLoudRoomReadsLoud() {
        assertEquals(58.0, QuietFloor.of(List(60) { 58 }))
    }

    @Test
    fun shortRunsStillGiveANumberAndEmptyGivesNone() {
        assertEquals(29.0, QuietFloor.of(listOf(30, 31, 29, 45, 30)) ?: -1.0)
        assertNull(QuietFloor.of(emptyList()))
    }
}
