package com.rideautoacceptor.util

object Constants {

    // ── Supported App Package Names ──────────────────────────────────────────
    const val PKG_OLA            = "com.olacabs.oladriver"
    const val PKG_RAPIDO_CAPTAIN = "com.rapido.captain"
    const val PKG_RAPIDO_ALT     = "com.roppenlabs.rapido.captain"
    const val PKG_UBER_DRIVER    = "com.ubercab.driver"
    const val PKG_PORTER         = "com.porter.partner"
    const val PKG_PORTER_ALT     = "com.theporter.partnerapp"
    const val PKG_INDRIVE        = "com.sap.indrive.driver"       // Phase 2 placeholder

    /** All packages the AccessibilityService should react to */
    val SUPPORTED_PACKAGES = setOf(
        PKG_OLA,
        PKG_RAPIDO_CAPTAIN,
        PKG_RAPIDO_ALT,
        PKG_UBER_DRIVER,
        PKG_PORTER,
        PKG_PORTER_ALT,
        PKG_INDRIVE
    )

    /** Human-readable display names keyed by package */
    val APP_DISPLAY_NAMES = mapOf(
        PKG_OLA            to "Ola Partner",
        PKG_RAPIDO_CAPTAIN to "Rapido Captain",
        PKG_RAPIDO_ALT     to "Rapido Captain",
        PKG_UBER_DRIVER    to "Uber Driver",
        PKG_PORTER         to "Porter Partner",
        PKG_PORTER_ALT     to "Porter Partner",
        PKG_INDRIVE        to "InDrive"
    )

    // ── Accept Button Texts (case-sensitive variants found in the wild) ──────
    val ACCEPT_TEXTS = setOf(
        "ACCEPT", "Accept", "accept",
        "ACCEPT RIDE", "Accept Ride",
        "ACCEPT REQUEST", "Accept Request",
        "CONFIRM", "Confirm",
        "START TRIP", "Start Trip"
    )

    // ── Decline / Reject texts — NEVER click these under any circumstance ────
    val DECLINE_TEXTS = setOf(
        "X", "x", "✕", "×", "✗",
        "DECLINE", "Decline", "decline",
        "REJECT", "Reject", "reject",
        "CANCEL", "Cancel", "cancel",
        "-", "–", "—"
    )

    // ── DataStore Preference Keys ─────────────────────────────────────────────
    const val PREFS_NAME             = "ride_auto_acceptor_prefs"
    const val KEY_MASTER_ENABLED     = "master_enabled"
    const val KEY_MAX_PICKUP_ENABLED = "max_pickup_enabled"
    const val KEY_MAX_PICKUP_KM      = "max_pickup_km"
    const val KEY_MIN_DROP_ENABLED   = "min_drop_enabled"
    const val KEY_MIN_DROP_KM        = "min_drop_km"
    const val KEY_MIN_FARE_ENABLED   = "min_fare_enabled"
    const val KEY_MIN_FARE_AMOUNT    = "min_fare_amount"
    const val KEY_APP_OLA            = "app_ola_enabled"
    const val KEY_APP_RAPIDO         = "app_rapido_enabled"
    const val KEY_APP_UBER           = "app_uber_enabled"
    const val KEY_APP_PORTER         = "app_porter_enabled"
    const val KEY_APP_INDRIVE        = "app_indrive_enabled"

    // ── Broadcast Actions ─────────────────────────────────────────────────────
    const val ACTION_RIDE_EVENT      = "com.rideautoacceptor.RIDE_EVENT"
    const val ACTION_SERVICE_STATUS  = "com.rideautoacceptor.SERVICE_STATUS"
    const val EXTRA_RIDE_EVENT_ID    = "ride_event_id"
    const val EXTRA_SERVICE_ACTIVE   = "service_active"

    // ── Foreground Service Notification ──────────────────────────────────────
    const val NOTIFICATION_CHANNEL_ID   = "ride_auto_acceptor_service"
    const val NOTIFICATION_CHANNEL_NAME = "Automation Service"
    const val NOTIFICATION_ID           = 1001

    // ── Timing ────────────────────────────────────────────────────────────────
    /** Debounce interval in ms — ignore repeated events from the same window */
    const val DEBOUNCE_MS = 800L

    /** How long (ms) to wait after a click before allowing another click attempt */
    const val CLICK_COOLDOWN_MS = 5_000L

    // ── Keyword hints that suggest a ride-request popup is active ────────────
    /** If any of these appear alongside ₹, we're likely looking at a ride card */
    val RIDE_INDICATOR_KEYWORDS = listOf("km", "mins", "Accept", "ACCEPT", "₹")
}
