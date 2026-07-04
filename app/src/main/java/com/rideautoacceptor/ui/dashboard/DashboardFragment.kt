package com.rideautoacceptor.ui.dashboard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.rideautoacceptor.R
import com.rideautoacceptor.RideAutoAcceptorApp
import com.rideautoacceptor.databinding.FragmentDashboardBinding
import com.rideautoacceptor.service.AutomationAccessibilityService
import com.rideautoacceptor.util.Constants
import com.rideautoacceptor.util.toRupeeString
import com.rideautoacceptor.util.toTimeString
import com.rideautoacceptor.util.toDistanceString
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels {
        DashboardViewModel.Factory(RideAutoAcceptorApp.get(requireContext()).repository)
    }

    // Receive events when a ride is processed
    private val rideEventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Refresh stats when a new ride event is logged
            viewModel.refresh()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToggle()
        observeViewModel()

        // Fix accessibility quick action
        binding.btnFixAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        requireContext().registerReceiver(
            rideEventReceiver,
            IntentFilter(Constants.ACTION_RIDE_EVENT),
            Context.RECEIVER_NOT_EXPORTED
        )
        updateAccessibilityWarning()
        viewModel.refresh()
    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(rideEventReceiver)
    }

    private fun setupToggle() {
        // Observe master enabled state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.filterPrefs.collect { prefs ->
                // Update without triggering listener
                binding.switchMaster.setOnCheckedChangeListener(null)
                binding.switchMaster.isChecked = prefs.isEnabled
                updateToggleUI(prefs.isEnabled)
                setupToggleListener()
            }
        }
    }

    private fun setupToggleListener() {
        binding.switchMaster.setOnCheckedChangeListener { _, isChecked ->
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.setMasterEnabled(isChecked)
            }
            updateToggleUI(isChecked)
        }
    }

    private fun updateToggleUI(isEnabled: Boolean) {
        if (isEnabled) {
            binding.tvAutomationState.text = getString(R.string.automation_enabled)
            binding.tvToggleHint.text = getString(R.string.toggle_hint_disable)
            binding.tvServiceStatus.text = if (AutomationAccessibilityService.isRunning)
                getString(R.string.service_status_listening)
            else
                getString(R.string.service_status_no_accessibility)
        } else {
            binding.tvAutomationState.text = getString(R.string.automation_disabled)
            binding.tvToggleHint.text = getString(R.string.toggle_hint_enable)
            binding.tvServiceStatus.text = getString(R.string.service_status_inactive)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.todayAccepted.collect { count ->
                binding.tvAcceptedCount.text = count.toString()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.todaySkipped.collect { count ->
                binding.tvSkippedCount.text = count.toString()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.latestEvent.collect { event ->
                if (event != null) {
                    val fare = event.fareAmount?.toRupeeString() ?: "—"
                    val time = event.timestamp.toTimeString()
                    val action = if (event.action == "ACCEPTED") "✅ Accepted" else "⏭ Skipped"
                    binding.tvLastEvent.text = "$time  $action  $fare  (${event.appName})"
                } else {
                    binding.tvLastEvent.text = getString(R.string.no_events_yet)
                }
            }
        }
    }

    private fun updateAccessibilityWarning() {
        val isRunning = AutomationAccessibilityService.isRunning
        binding.cardAccessibilityWarning.visibility =
            if (!isRunning) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
