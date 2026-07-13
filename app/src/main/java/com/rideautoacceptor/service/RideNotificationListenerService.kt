package com.rideautoacceptor.service

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.rideautoacceptor.util.Constants
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

/**
 * RideNotificationListenerService
 *
 * Monitors incoming notifications from supported ride-hailing apps.
 * Primary purpose in Phase 1:
 *  - Acts as a "wake-up" trigger — when a notification arrives from a supported
 *    app, it broadcasts an intent that can help the UI show an alert.
 *  - The actual click automation is handled by [AutomationAccessibilityService].
 *
 * Phase 2 potential:
 *  - Parse notification extras for fare/distance data (some apps embed this).
 *  - Provide a faster, notification-based trigger path alongside the
 *    window-scanning path.
 */
class RideNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "NotifListenerService"
    }

    private val serviceScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO
    )
    private lateinit var prefsManager: com.rideautoacceptor.data.local.PreferencesManager
    @Volatile private var isEnabled = false

    override fun onCreate() {
        super.onCreate()
        prefsManager = com.rideautoacceptor.data.local.PreferencesManager(applicationContext)
        serviceScope.launch {
            prefsManager.filterPrefsFlow.collect { prefs ->
                isEnabled = prefs.isEnabled
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!isEnabled) return
        val pkg = sbn.packageName ?: return
        if (!Constants.SUPPORTED_PACKAGES.contains(pkg)) return

        Log.d(TAG, "📬 Notification from $pkg: ${sbn.notification.extras?.getString("android.title")}")

        // Broadcast to UI so dashboard can show "notification detected" status
        val intent = Intent(Constants.ACTION_SERVICE_STATUS).apply {
            putExtra(Constants.EXTRA_SERVICE_ACTIVE, true)
            putExtra("notification_from", pkg)
        }
        sendBroadcast(intent)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // No action needed — ride was either accepted or timed out
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "✅ Notification listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "Notification listener disconnected")
    }
}

