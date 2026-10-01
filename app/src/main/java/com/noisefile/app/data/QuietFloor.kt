package com.noisefile.app.data

/**
 * The quiet baseline as the codes mean it: the level the room sits at, not the
 * loud blips. Redwood City 24.2 says "the lowest sound level repeating itself";
 * engineers call it L90, the level met or exceeded 90% of the time. An energy
 * average would let one sneeze at 70 dB lift a 35 dB room to 45; this does not.
 */
object QuietFloor {
    /** The 10th percentile of the per-second levels, rounded. Null for an empty run. */
    fun of(levelsDb: List<Int>): Double? {
        if (levelsDb.isEmpty()) return null
        val sorted = levelsDb.sorted()
        // Index of the 10th percentile; with few points this leans low, which is the safe side for a floor.
        val index = ((sorted.size - 1) * 0.10).toInt()
        return sorted[index].toDouble()
    }
}
