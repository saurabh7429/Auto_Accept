package com.rideautoacceptor.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import com.rideautoacceptor.MainActivity
import com.rideautoacceptor.R
import com.rideautoacceptor.data.local.AppDatabase
import com.rideautoacceptor.data.local.PreferencesManager
import com.rideautoacceptor.data.model.FilterPrefs
import com.rideautoacceptor.data.model.RideEvent
import com.rideautoacceptor.data.repository.RideRepository
import com.rideautoacceptor.util.Constants
import com.rideautoacceptor.util.Constants.ACCEPT_TEXTS
import com.rideautoacceptor.util.Constants.DECLINE_TEXTS
import com.rideautoacceptor.util.RideParser
import com.rideautoacceptor.util.collectText
import com.rideautoacceptor.util.findClickableNode
import com.rideautoacceptor.util.toRupeeString
import com.rideautoacceptor.util.toDistanceString
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║          AutomationAccessibilityService — Core Engine           ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  Listens to window events from supported ride-hailing apps.     ║
 * ║  Parses fare, pickup distance, drop distance from screen text.  ║
 * ║  Evaluates user-defined filter criteria.                        ║
 * ║  Clicks Accept if criteria pass — NEVER clicks Decline/Reject.  ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
class AutomationAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AutomationService"

        /** Exposed so other components can check live status */
        @Volatile var isRunning: Boolean = false
            private set
    }

    // ── Coroutine scope tied to service lifecycle ─────────────────────────────
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── Repository (lazily initialized after service is created) ─────────────
    private lateinit var repository: RideRepository

    // ── Debounce / cooldown tracking ──────────────────────────────────────────
    private var lastProcessedPackage: String = ""
    private var lastProcessedTime:    Long   = 0L
    private var lastClickTime:        Long   = 0L

    // ── Cached preferences (updated live) ────────────────────────────────────
    @Volatile private var currentPrefs: FilterPrefs = FilterPrefs()

    // ═════════════════════════════════════════════════════════════════════════
    //  Lifecycle
    // ═════════════════════════════════════════════════════════════════════════

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "✅ Service connected")

        isRunning = true

        // Init repository
        val db       = AppDatabase.getInstance(applicationContext)
        val prefs    = PreferencesManager(applicationContext)
        repository   = RideRepository.getInstance(db, prefs)

        // Subscribe to preference changes so the service always has fresh prefs
        serviceScope.launch {
            repository.filterPrefsFlow.collect { prefs ->
                currentPrefs = prefs
                Log.d(TAG, "Prefs updated: enabled=${prefs.isEnabled}")
            }
        }

        // Start foreground so we stay alive
        startForegroundService()

        // Prune old events on start
        serviceScope.launch { repository.pruneOldEvents() }

        // Broadcast status
        broadcastStatus(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceScope.cancel()
        broadcastStatus(false)
        Log.i(TAG, "Service destroyed")
    }

    override fun onInterrupt() {
        Log.w(TAG, "Service interrupted")
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Core Event Handling
    // ═════════════════════════════════════════════════════════════════════════

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Only react to window state/content changes
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            return
        }

        val packageName = event.packageName?.toString() ?: return

        // ── 1. Check master toggle ────────────────────────────────────────────
        if (!currentPrefs.isEnabled) return

        // ── 2. Check supported package ────────────────────────────────────────
        if (!Constants.SUPPORTED_PACKAGES.contains(packageName)) return

        // ── 3. Check per-app toggle ───────────────────────────────────────────
        if (!currentPrefs.isAppEnabled(packageName)) return

        // ── 4. Debounce: ignore rapid repeated events from same app ───────────
        val now = SystemClock.elapsedRealtime()
        if (packageName == lastProcessedPackage &&
            now - lastProcessedTime < Constants.DEBOUNCE_MS) {
            return
        }

        // ── 5. Click cooldown: don't click again too soon ─────────────────────
        if (now - lastClickTime < Constants.CLICK_COOLDOWN_MS) return

        lastProcessedPackage = packageName
        lastProcessedTime    = now

        // ── 6. Process in background to avoid blocking the UI thread ──────────
        serviceScope.launch(Dispatchers.Main) {
            processWindow(packageName)
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Window Processing
    // ═════════════════════════════════════════════════════════════════════════

    private suspend fun processWindow(packageName: String) {
        // Grab the accessibility tree
        val rootNode = rootInActiveWindow ?: return

        try {
            // ── Collect all text from the window ──────────────────────────────
            val textBuilder = StringBuilder()
            rootNode.collectText(textBuilder)
            val allText = textBuilder.toString()

            if (allText.isBlank()) return

            // ── Quick heuristic check before parsing ──────────────────────────
            if (!RideParser.looksLikeRideRequest(allText)) return

            // ── Parse ride data ───────────────────────────────────────────────
            val parsed = RideParser.parse(allText)

            // ── Evaluate criteria ─────────────────────────────────────────────
            val (shouldAccept, skipReason) = evaluateCriteria(parsed)

            val appName = Constants.APP_DISPLAY_NAMES[packageName] ?: packageName

            if (shouldAccept) {
                // ── Find and click Accept ─────────────────────────────────────
                val acceptNode = findAcceptNode(rootNode)
                if (acceptNode != null) {
                    Log.i(TAG, "🟢 Clicking ACCEPT for $appName — fare=${parsed.fareAmount} pickup=${parsed.pickupDistanceKm}")
                    val clicked = acceptNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    if (clicked) {
                        lastClickTime = SystemClock.elapsedRealtime()
                        logEvent(packageName, appName, parsed, RideEvent.ACTION_ACCEPTED, null)
                    } else {
                        Log.w(TAG, "Click action returned false — node may no longer be on screen")
                    }
                } else {
                    Log.d(TAG, "Accept node not found in window — will retry on next event")
                }
            } else {
                // ── Zero Decline Rule: just log and do nothing ────────────────
                Log.i(TAG, "🟡 SKIPPING $appName — $skipReason")
                logEvent(packageName, appName, parsed, RideEvent.ACTION_SKIPPED, skipReason)
            }

        } finally {
            rootNode.recycle()
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Criteria Evaluation
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Check each enabled filter criterion against the parsed values.
     * ALL enabled criteria must pass. If no criteria are set, auto-accept.
     *
     * Returns Pair<shouldAccept: Boolean, skipReason: String?>
     */
    private fun evaluateCriteria(parsed: com.rideautoacceptor.util.ParsedRide): Pair<Boolean, String?> {
        val prefs = currentPrefs

        // ── Min Fare ──────────────────────────────────────────────────────────
        prefs.minFareAmount?.let { minFare ->
            val fare = parsed.fareAmount
            if (fare == null) {
                return false to "Fare not detected (threshold ${minFare.toRupeeString()})"
            }
            if (fare < minFare) {
                return false to "Fare ${fare.toRupeeString()} below min ${minFare.toRupeeString()}"
            }
        }

        // ── Max Pickup Distance ───────────────────────────────────────────────
        prefs.maxPickupDistanceKm?.let { maxPickup ->
            val pickup = parsed.pickupDistanceKm
            if (pickup == null) {
                return false to "Pickup distance not detected (max ${maxPickup.toDistanceString()})"
            }
            if (pickup > maxPickup) {
                return false to "Pickup ${pickup.toDistanceString()} exceeds max ${maxPickup.toDistanceString()}"
            }
        }

        // ── Min Drop Distance ─────────────────────────────────────────────────
        prefs.minDropDistanceKm?.let { minDrop ->
            val drop = parsed.dropDistanceKm
            if (drop == null) {
                return false to "Drop distance not detected (min ${minDrop.toDistanceString()})"
            }
            if (drop < minDrop) {
                return false to "Drop ${drop.toDistanceString()} below min ${minDrop.toDistanceString()}"
            }
        }

        // All checks passed (or no criteria set)
        return true to null
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Node Finding — Safe Accept Click
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Find the Accept button node.
     * Strategy:
     *  1. Look for clickable nodes with text in ACCEPT_TEXTS
     *  2. NEVER return a node whose text is in DECLINE_TEXTS
     *  3. Try exact text match first, then contains-match as fallback
     */
    private fun findAcceptNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // First pass: exact text match
        for (acceptText in ACCEPT_TEXTS) {
            val nodes = root.findAccessibilityNodeInfosByText(acceptText)
            for (node in nodes) {
                val text = node.text?.toString()?.trim() ?: ""
                // Safety double-check: never select a decline node
                if (text in DECLINE_TEXTS) {
                    node.recycle()
                    continue
                }
                if (text in ACCEPT_TEXTS) {
                    // Walk up to find a clickable ancestor if this node isn't clickable
                    val clickable = findClickableAncestor(node) ?: node
                    if (clickable.isClickable) return clickable
                }
                node.recycle()
            }
        }

        // Second pass: recursive tree walk (for nodes without text set via getText())
        return root.findClickableNode(ACCEPT_TEXTS, DECLINE_TEXTS)
    }

    /**
     * Walk up the node tree to find the nearest clickable ancestor.
     * Some apps wrap the label in a non-clickable TextView inside a clickable Button.
     */
    private fun findClickableAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        var depth = 0
        while (current != null && depth < 5) {
            if (current.isClickable) return current
            current = current.parent
            depth++
        }
        return null
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Logging
    // ═════════════════════════════════════════════════════════════════════════

    private fun logEvent(
        packageName: String,
        appName: String,
        parsed: com.rideautoacceptor.util.ParsedRide,
        action: String,
        skipReason: String?
    ) {
        serviceScope.launch(Dispatchers.IO) {
            val event = RideEvent(
                packageName      = packageName,
                appName          = appName,
                fareAmount       = parsed.fareAmount,
                pickupDistanceKm = parsed.pickupDistanceKm,
                dropDistanceKm   = parsed.dropDistanceKm,
                action           = action,
                skipReason       = skipReason
            )
            val id = repository.insertEvent(event)

            // Notify UI via broadcast
            val intent = Intent(Constants.ACTION_RIDE_EVENT).apply {
                putExtra(Constants.EXTRA_RIDE_EVENT_ID, id)
                setPackage(packageName)
            }
            // Use LocalBroadcastManager-equivalent: direct intent to our package
            val uiIntent = Intent(Constants.ACTION_RIDE_EVENT).apply {
                putExtra(Constants.EXTRA_RIDE_EVENT_ID, id)
            }
            sendBroadcast(uiIntent)
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Foreground Service Notification
    // ═════════════════════════════════════════════════════════════════════════

    private fun startForegroundService() {
        createNotificationChannel()

        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Ride Auto-Acceptor")
            .setContentText("Listening for ride requests…")
            .setSmallIcon(R.drawable.ic_notification_small)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(Constants.NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_ID,
            Constants.NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows when ride automation is running"
            setShowBadge(false)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun broadcastStatus(active: Boolean) {
        val intent = Intent(Constants.ACTION_SERVICE_STATUS).apply {
            putExtra(Constants.EXTRA_SERVICE_ACTIVE, active)
        }
        sendBroadcast(intent)
    }
}
