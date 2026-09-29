package com.noisefile.app.audio

import android.content.Context
import org.json.JSONObject

/** One microphone's user calibration: offset, what it was checked against, and when. */
/** The last self-test of one microphone. */
data class SelfTestRecord(
    val outcome: SelfTestOutcome,
    val rangeDb: Int,
    val worstErrorDb: Double,
    val atEpochMillis: Long,
) {
    val passed: Boolean get() = outcome == SelfTestOutcome.READS_STRAIGHT
}

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

    /** The phone's own declared mic sensitivity (dBFS at 94 dB SPL), remembered once read; null = never declared. */
    fun declaredSensitivity(micKey: String): Double? =
        if (preferences.contains("declared:$micKey")) preferences.getFloat("declared:$micKey", 0f).toDouble() else null

    fun rememberDeclaredSensitivity(micKey: String, dbfsAt94: Double) {
        preferences.edit().putFloat("declared:$micKey", dbfsAt94.toFloat()).apply()
    }

    fun selfTest(micKey: String): SelfTestRecord? {
        val raw = preferences.getString("selftest:$micKey", null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            SelfTestRecord(
                outcome = SelfTestOutcome.valueOf(json.getString("outcome")),
                rangeDb = json.getInt("rangeDb"),
                worstErrorDb = json.getDouble("worstErrorDb"),
                atEpochMillis = json.getLong("at"),
            )
        }.getOrNull()
    }

    fun saveSelfTest(micKey: String, record: SelfTestRecord) {
        val json = JSONObject()
            .put("outcome", record.outcome.name)
            .put("rangeDb", record.rangeDb)
            .put("worstErrorDb", record.worstErrorDb)
            .put("at", record.atEpochMillis)
        preferences.edit().putString("selftest:$micKey", json.toString()).apply()
    }

    /** The once-only "make your numbers count" card: shown after the first incident until Skip or a calibration. */
    var calibrationPromptDismissed: Boolean
        get() = preferences.getBoolean("prompt_dismissed", false)
        set(value) { preferences.edit().putBoolean("prompt_dismissed", value).apply() }

    private companion object {
        const val PREFERENCES_NAME = "noisefile_mic_profiles"
    }
}
