package com.rideautoacceptor.ui.onboarding

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import android.util.Log
import com.rideautoacceptor.R
import com.rideautoacceptor.databinding.FragmentPermissionsBinding
import com.rideautoacceptor.service.AutomationAccessibilityService

class PermissionsFragment : Fragment() {

    private var _binding: FragmentPermissionsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PermissionAdapter

    // Permission IDs (used as keys)
    companion object {
        const val ID_ACCESSIBILITY  = "accessibility"
        const val ID_NOTIFICATION   = "notification"
        const val ID_OVERLAY        = "overlay"
        const val ID_BATTERY        = "battery"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPermissionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val items = buildPermissionItems()
        adapter = PermissionAdapter(items) { item -> handlePermissionAction(item) }

        binding.recyclerPermissions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@PermissionsFragment.adapter
            isNestedScrollingEnabled = false
        }

        binding.btnProceed.setOnClickListener {
            findNavController().navigate(R.id.dashboardFragment)
        }

        updateAllStatuses()
    }

    override fun onResume() {
        super.onResume()
        // Re-check permissions whenever user returns from system settings
        updateAllStatuses()
    }

    private fun buildPermissionItems(): List<PermissionItem> = listOf(
        PermissionItem(
            id          = ID_ACCESSIBILITY,
            icon        = "♿",
            title       = "Accessibility Service",
            subtitle    = "स्क्रीन से किराया और दूरी पढ़ने के लिए",
            whyDescription = "Ride Auto-Acceptor needs Accessibility permission to read the fare " +
                    "amount (e.g., ₹89) and distances (e.g., 1.6 km) shown on ride request " +
                    "popups — and to tap the Accept button on your behalf.\n\n" +
                    "आपकी screen पर दिखाई देने वाले ride request popup से fare और distance " +
                    "पढ़ने के लिए यह permission जरूरी है।",
            actionLabel = "Enable Accessibility Service"
        ),
        PermissionItem(
            id          = ID_NOTIFICATION,
            icon        = "🔔",
            title       = "Notification Access",
            subtitle    = "Ride requests को notification से detect करने के लिए",
            whyDescription = "Notification Access allows the app to detect ride requests the " +
                    "moment they arrive — even if you're on a different screen.\n\n" +
                    "जब भी कोई ride request आए, तुरंत detect हो सके — इसके लिए " +
                    "notification access जरूरी है।",
            actionLabel = "Enable Notification Access"
        ),
        PermissionItem(
            id          = ID_OVERLAY,
            icon        = "🖥️",
            title       = "Display Over Other Apps",
            subtitle    = "दूसरे apps के ऊपर status दिखाने के लिए",
            whyDescription = "This permission lets Ride Auto-Acceptor show a small status " +
                    "indicator while you're using other apps (like Maps or music).\n\n" +
                    "जब आप दूसरे apps use कर रहे हों, तब भी automation status दिखाने " +
                    "के लिए यह permission चाहिए।",
            actionLabel = "Enable Display Over Apps"
        ),
        PermissionItem(
            id          = ID_BATTERY,
            icon        = "🔋",
            title       = "Ignore Battery Optimization",
            subtitle    = "Background में बिना रुके चलते रहने के लिए",
            whyDescription = "Without this, Android may pause the app in the background, " +
                    "causing you to miss ride requests.\n\n" +
                    "Battery optimization से app background में रुक सकती है और ride " +
                    "requests miss हो सकती हैं। इसे disable करना जरूरी है।",
            actionLabel = "Disable Battery Optimization"
        )
    )

    private fun handlePermissionAction(item: PermissionItem) {
        val ctx = context ?: return
        when (item.id) {
            ID_ACCESSIBILITY -> {
                try {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                } catch (e: Exception) {
                    Log.e("Permissions", "Failed to open accessibility settings", e)
                }
            }
            ID_NOTIFICATION -> {
                try {
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                } catch (e: Exception) {
                    Log.e("Permissions", "Failed to open notification settings", e)
                }
            }
            ID_OVERLAY -> {
                try {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${ctx.packageName}")
                    )
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.w("Permissions", "Could not request overlay with package Uri, trying general settings", e)
                    try {
                        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                    } catch (ex: Exception) {
                        Log.e("Permissions", "Failed to open overlay settings", ex)
                    }
                }
            }
            ID_BATTERY -> {
                try {
                    val intent = Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${ctx.packageName}")
                    )
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.w("Permissions", "ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS failed, trying fallback settings", e)
                    try {
                        startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    } catch (ex: Exception) {
                        Log.e("Permissions", "Failed to open battery optimization settings", ex)
                    }
                }
            }
        }
    }

    private fun updateAllStatuses() {
        val ctx = context ?: return
        val accessibilityGranted = try { isAccessibilityEnabled(ctx) } catch (e: Exception) { false }
        val notificationGranted  = try { isNotificationListenerEnabled(ctx) } catch (e: Exception) { false }
        val overlayGranted       = try { Settings.canDrawOverlays(ctx) } catch (e: Exception) { false }
        val batteryGranted       = try { isBatteryOptimizationIgnored(ctx) } catch (e: Exception) { false }

        adapter.updateGrantStatus(ID_ACCESSIBILITY, accessibilityGranted)
        adapter.updateGrantStatus(ID_NOTIFICATION,  notificationGranted)
        adapter.updateGrantStatus(ID_OVERLAY,       overlayGranted)
        adapter.updateGrantStatus(ID_BATTERY,       batteryGranted)

        val allGranted = accessibilityGranted && notificationGranted &&
                         overlayGranted && batteryGranted

        if (allGranted) {
            binding.tvOverallStatusIcon.text  = "✅"
            binding.tvOverallStatus.text      = getString(R.string.all_permissions_granted)
            binding.cardOverallStatus.setCardBackgroundColor(
                ctx.getColor(R.color.color_primary_container))
        } else {
            binding.tvOverallStatusIcon.text  = "⚠️"
            binding.tvOverallStatus.text      = getString(R.string.permissions_incomplete)
            binding.cardOverallStatus.setCardBackgroundColor(
                ctx.getColor(R.color.color_surface_variant))
        }
        binding.btnProceed.isVisible = true
    }

    // ── Permission Checks ─────────────────────────────────────────────────────

    private fun isAccessibilityEnabled(ctx: Context): Boolean {
        val am = ctx.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        val target = ComponentName(ctx, AutomationAccessibilityService::class.java)
        return enabledServices.any { info ->
            info.resolveInfo.serviceInfo.let { si ->
                si.packageName == target.packageName && si.name == target.className
            }
        }
    }

    private fun isNotificationListenerEnabled(ctx: Context): Boolean {
        val flat = Settings.Secure.getString(
            ctx.contentResolver, "enabled_notification_listeners"
        ) ?: return false
        return flat.contains(ctx.packageName)
    }

    private fun isBatteryOptimizationIgnored(ctx: Context): Boolean {
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(ctx.packageName)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
