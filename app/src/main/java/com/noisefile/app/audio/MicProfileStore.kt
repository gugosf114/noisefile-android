package com.noisefile.app.audio

import android.content.Context
import org.json.JSONObject

/** One microphone's user calibration: offset, what it was checked against, and when. */
data class MicProfile(
    val micKey: String,
    val offsetDb: Double,
    val referenceLabel: String,
    val calibratedAtEpochMillis: Long,
)

/** Stored on the phone only, keyed by microphone (built-in model or USB product name). */
class MicProfileStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun load(micKey: String): MicProfile? {
        val raw = preferences.getString(micKey, null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            MicProfile(
                micKey = micKey,
                offsetDb = json.getDouble("offsetDb"),
                referenceLabel = json.optString("referenceLabel", "reference meter"),
                calibratedAtEpochMillis = json.getLong("calibratedAtEpochMillis"),
            )
        }.getOrNull()
    }

    @Synchronized
    fun save(profile: MicProfile) {
        val json = JSONObject()
            .put("offsetDb", profile.offsetDb)
            .put("referenceLabel", profile.referenceLabel)
            .put("calibratedAtEpochMillis", profile.calibratedAtEpochMillis)
        preferences.edit().putString(profile.micKey, json.toString()).apply()
    }

    @Synchronized
    fun clear(micKey: String) {
        preferences.edit().remove(micKey).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "noisefile_mic_profiles"
    }
}
