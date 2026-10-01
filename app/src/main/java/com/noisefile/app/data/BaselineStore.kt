package com.noisefile.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** One room's quiet, measured once and kept until remeasured. Never an incident; never in the seal chain. */
data class Baseline(
    val room: String,
    val db: Double,
    val seconds: Long,
    val measuredAtEpochMillis: Long,
    /** The minutes the city's code asked for when this was taken, or null when the code sets none. */
    val codeMinutes: Int?,
    val cityName: String,
)

/** The rooms a person can stand in. "Other" takes a typed name. */
object Rooms {
    val common = listOf("Bedroom", "Living room", "Kitchen", "Back yard", "Street side", "Balcony")
    const val OTHER = "Other"
}

class BaselineStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("noisefile_baselines", Context.MODE_PRIVATE)

    fun load(): List<Baseline> = parseBaselines(prefs.getString(KEY, "[]") ?: "[]")

    /** Newest wins: a room holds one baseline. */
    fun save(baseline: Baseline): List<Baseline> {
        val kept = load().filter { !it.room.equals(baseline.room, ignoreCase = true) } + baseline
        val array = JSONArray()
        kept.sortedBy { it.room.lowercase() }.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY, array.toString()).apply()
        return kept.sortedBy { it.room.lowercase() }
    }

    fun forRoom(room: String?): Baseline? = room?.let { r -> load().firstOrNull { it.room.equals(r, ignoreCase = true) } }

    val lastRoom: String? get() = prefs.getString(KEY_LAST_ROOM, null)
    fun rememberRoom(room: String) = prefs.edit().putString(KEY_LAST_ROOM, room).apply()

    private companion object {
        const val KEY = "baselines"
        const val KEY_LAST_ROOM = "lastRoom"
    }
}

internal fun Baseline.toJson(): JSONObject = JSONObject()
    .put("room", room)
    .put("db", db)
    .put("seconds", seconds)
    .put("measuredAtEpochMillis", measuredAtEpochMillis)
    .put("codeMinutes", codeMinutes ?: JSONObject.NULL)
    .put("cityName", cityName)

internal fun parseBaselines(raw: String): List<Baseline> = runCatching {
    val array = JSONArray(raw)
    List(array.length()) { i ->
        val o = array.getJSONObject(i)
        Baseline(
            room = o.getString("room"),
            db = o.getDouble("db"),
            seconds = o.getLong("seconds"),
            measuredAtEpochMillis = o.getLong("measuredAtEpochMillis"),
            codeMinutes = if (o.has("codeMinutes") && !o.isNull("codeMinutes")) o.getInt("codeMinutes") else null,
            cityName = o.optString("cityName"),
        )
    }
}.getOrDefault(emptyList())
