package com.rideautoacceptor.util

import android.util.Log

/**
 * RideParser — the brain of text extraction.
 *
 * All regex patterns are derived from real Ola / Rapido popup screenshots:
 *
 *  Ola overlay (home screen):
 *    ₹108
 *    • 1.6 km  → Udhana Bypass …
 *    ▼ 7.7 km  → Avadh Textile Market …
 *    [–]  [Accept]
 *
 *  Ola in-app card:
 *    OLA Auto • Cash
 *    ₹31 | ₹19/km
 *    0.6 km · 2 mins   ← pickup
 *    • 1, Dumbhal Rd …
 *    1.6 km · 6 mins   ← drop
 *    • Anjada Bridge …
 *    [X]  [ACCEPT]
 *
 *  Parsing strategy:
 *    - Fare  : first ₹N or ₹N.N found (stops at | separator)
 *    - Pickup: first km value in the window text
 *    - Drop  : second km value in the window text
 */
object RideParser {

    private const val TAG = "RideParser"

    // ── Regex Patterns ────────────────────────────────────────────────────────

    /**
     * Matches: ₹108  ₹ 31  ₹89.50  ₹1,234
     * Captures the numeric part only.
     * The fare is almost always the first ₹-value before the | separator.
     */
    private val FARE_REGEX = Regex("""₹\s*(\d[\d,]*)(?:\.\d+)?""")

    /**
     * Matches: 1.6 km   7.7 km   0.6 km   10 km
     * Captures the numeric part (allows decimal).
     */
    private val DISTANCE_KM_REGEX = Regex("""(\d+(?:\.\d+)?)\s*km\b""", RegexOption.IGNORE_CASE)

    /**
     * Matches: 500 m   800 m  (metres, NOT inside "km")
     * Captures the numeric part. Will be converted to km.
     */
    private val DISTANCE_M_REGEX = Regex("""(\d+(?:\.\d+)?)\s*\bm\b(?!\s*in)""")

    /**
     * Matches: 2 mins   15 mins   7 min  (used as secondary pickup indicator)
     */
    private val TIME_MINS_REGEX = Regex("""(\d+)\s*min""", RegexOption.IGNORE_CASE)

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Parse a flattened string of all visible text nodes from the ride popup.
     * Returns a [ParsedRide] with nullable fields — null means not found.
     */
    fun parse(allText: String): ParsedRide {
        Log.d(TAG, "Parsing text: ${allText.take(300)}")

        val fare       = extractFare(allText)
        val distances  = extractDistancesKm(allText)
        val pickupKm   = distances.getOrNull(0)
        val dropKm     = distances.getOrNull(1)

        val result = ParsedRide(
            fareAmount        = fare,
            pickupDistanceKm  = pickupKm,
            dropDistanceKm    = dropKm,
            rawText           = allText
        )
        Log.d(TAG, "Parsed: fare=₹$fare  pickup=${pickupKm}km  drop=${dropKm}km")
        return result
    }

    /**
     * Quick check: does this window text look like a ride request?
     * Avoids processing every random window event.
     */
    fun looksLikeRideRequest(text: String): Boolean {
        val hasRupee  = text.contains('₹')
        val hasKm     = text.contains("km", ignoreCase = true)
        val hasAccept = Constants.ACCEPT_TEXTS.any { text.contains(it) }
        return hasRupee && (hasKm || hasAccept)
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private fun extractFare(text: String): Float? {
        // Take only the text before the first | to avoid picking up per-km rate
        val segment = text.substringBefore("|").substringBefore("per km")
        val raw = FARE_REGEX.find(segment)?.groupValues?.get(1)
            ?: FARE_REGEX.find(text)?.groupValues?.get(1) // fallback: full text
        return raw?.replace(",", "")?.toFloatOrNull()
    }

    /**
     * Extract all km distances in document order.
     * Also converts any metre values and inserts them in order.
     * Result is ordered: index 0 = pickup, index 1 = drop (typical layout).
     */
    private fun extractDistancesKm(text: String): List<Float> {
        // Build a list of (index, valueKm) pairs then sort by index
        val results = mutableListOf<Pair<Int, Float>>()

        DISTANCE_KM_REGEX.findAll(text).forEach { match ->
            val value = match.groupValues[1].toFloatOrNull() ?: return@forEach
            results.add(match.range.first to value)
        }

        DISTANCE_M_REGEX.findAll(text).forEach { match ->
            val valueM = match.groupValues[1].toFloatOrNull() ?: return@forEach
            val valueKm = valueM / 1000f
            results.add(match.range.first to valueKm)
        }

        return results
            .sortedBy { it.first }
            .map { it.second }
            .distinct()
    }
}

/**
 * Data class holding all values extracted from a single ride-request popup.
 * Null fields mean the value was not found in the text.
 */
data class ParsedRide(
    val fareAmount:       Float?,
    val pickupDistanceKm: Float?,
    val dropDistanceKm:   Float?,
    val rawText:          String = ""
)
