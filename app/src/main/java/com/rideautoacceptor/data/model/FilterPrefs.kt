package com.rideautoacceptor.data.model

/**
 * In-memory representation of all user filter preferences.
 * Sourced from DataStore via [PreferencesManager].
 * Every criterion is optional (null/false = disabled = skip that check).
 */
data class FilterPrefs(
    /** Master ON/OFF switch */
    val isEnabled: Boolean = false,

    // ── Per-App Toggles ───────────────────────────────────────────────────────
    val olaEnabled:    Boolean = true,
    val rapidoEnabled: Boolean = true,
    val uberEnabled:   Boolean = true,
    val porterEnabled: Boolean = true,
    val indriveEnabled: Boolean = false,   // Phase 2 — off by default

    // ── Filter Criteria (null = disabled) ────────────────────────────────────
    /** Accept only if pickup distance ≤ this value (km) */
    val maxPickupDistanceKm: Float? = null,

    /** Accept only if drop distance ≥ this value (km) */
    val minDropDistanceKm: Float? = null,

    /** Accept only if fare ≥ this value (₹) */
    val minFareAmount: Float? = null
) {
    /**
     * Returns true if the given [packageName] has automation enabled
     * according to user's per-app toggles.
     */
    fun isAppEnabled(packageName: String): Boolean = when {
        packageName.contains("olacabs")                 -> olaEnabled
        packageName.contains("rapido")                  -> rapidoEnabled
        packageName.contains("roppenlabs")              -> rapidoEnabled
        packageName.contains("ubercab")                 -> uberEnabled
        packageName.contains("porter")                  -> porterEnabled
        packageName.contains("theporter")               -> porterEnabled
        packageName.contains("indrive")                 -> indriveEnabled
        else                                            -> false
    }
}
