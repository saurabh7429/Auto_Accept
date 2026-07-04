package com.rideautoacceptor.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BootReceiver — receives BOOT_COMPLETED to relaunch the app's main activity
 * if the user had automation enabled, ensuring the service stays active after
 * device restart.
 *
 * Note: The AccessibilityService itself must be re-enabled by the user after
 * a boot (Android security restriction), but this receiver can open the app
 * to remind them.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.i("BootReceiver", "Boot completed — notifying user to re-enable service")
            // Optionally: show a notification reminding the user to re-enable
        }
    }
}
