package com.rideautoacceptor.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single ride-request event processed by the automation service.
 * Stored in Room for the Activity Log screen.
 */
@Entity(tableName = "ride_events")
data class RideEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** System time when this event was processed (epoch millis) */
    val timestamp: Long = System.currentTimeMillis(),

    /** Package name of the source app (e.g. "com.olacabs.oladriver") */
    val packageName: String,

    /** Human-readable app name (e.g. "Ola Partner") */
    val appName: String,

    /** Fare extracted from the popup (₹) — null if not found */
    val fareAmount: Float?,

    /** Pickup distance in km — null if not found */
    val pickupDistanceKm: Float?,

    /** Drop/ride distance in km — null if not found */
    val dropDistanceKm: Float?,

    /** "ACCEPTED" or "SKIPPED" */
    val action: String,

    /**
     * Human-readable reason for skipping, null when accepted.
     * E.g. "Fare ₹31 below min ₹50"
     *      "Pickup 3.2 km exceeds max 2.0 km"
     */
    val skipReason: String? = null
) {
    companion object {
        const val ACTION_ACCEPTED = "ACCEPTED"
        const val ACTION_SKIPPED  = "SKIPPED"
    }
}
