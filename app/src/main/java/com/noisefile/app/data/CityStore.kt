package com.noisefile.app.data

import android.content.Context

/** The city and rule the person last picked, kept so the next open lands on the same city. */
data class SavedCity(val jurisdictionId: String, val ruleId: String)

/**
 * Pure: turn what was saved into a city and rule that exist in this catalog.
 * A missing or stale save falls back to the catalog default (San Jose). A city that
 * still exists keeps its own first rule when the saved rule is gone.
 */
fun resolveSavedCity(saved: SavedCity?, catalog: RuleCatalog): SavedCity {
    val fallback = SavedCity(RuleCatalog.SAN_JOSE_ID, RuleCatalog.DEFAULT_RULE_ID)
    if (saved == null) return fallback
    val city = catalog.jurisdictionById(saved.jurisdictionId)
    if (city == null || !city.isAvailable) return fallback
    val rule = catalog.byId(saved.ruleId)?.takeIf { it.jurisdictionId == city.id }
        ?: catalog.forJurisdiction(city.id).firstOrNull()
        ?: return fallback
    return SavedCity(city.id, rule.id)
}

class CityStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("noisefile_city", Context.MODE_PRIVATE)

    fun load(): SavedCity? {
        val city = prefs.getString(KEY_CITY, null) ?: return null
        val rule = prefs.getString(KEY_RULE, null) ?: return null
        return SavedCity(city, rule)
    }

    fun save(jurisdictionId: String, ruleId: String) {
        prefs.edit().putString(KEY_CITY, jurisdictionId).putString(KEY_RULE, ruleId).apply()
    }

    private companion object {
        const val KEY_CITY = "jurisdiction"
        const val KEY_RULE = "rule"
    }
}
