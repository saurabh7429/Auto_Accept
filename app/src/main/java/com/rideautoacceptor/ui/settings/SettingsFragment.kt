package com.rideautoacceptor.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.rideautoacceptor.R
import com.rideautoacceptor.RideAutoAcceptorApp
import com.rideautoacceptor.databinding.FragmentSettingsBinding
import com.rideautoacceptor.util.Constants
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels {
        SettingsViewModel.Factory(RideAutoAcceptorApp.get(requireContext()).repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAppToggles()
        setupFilterToggles()
        observePreferences()
    }

    // ── App Toggles ───────────────────────────────────────────────────────────

    private fun setupAppToggles() {
        // Wire each app switch to DataStore; observe live pref state
        val switches = mapOf(
            binding.switchOla     to Constants.KEY_APP_OLA,
            binding.switchRapido  to Constants.KEY_APP_RAPIDO,
            binding.switchUber    to Constants.KEY_APP_UBER,
            binding.switchPorter  to Constants.KEY_APP_PORTER
        )

        switches.forEach { (switch, key) ->
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.getAppEnabledFlow(key, true).collect { enabled ->
                    switch.setOnCheckedChangeListener(null)
                    switch.isChecked = enabled
                    switch.setOnCheckedChangeListener { _, isChecked ->
                        save { viewModel.setAppEnabled(key, isChecked) }
                    }
                }
            }
        }
        // InDrive disabled (Phase 2)
        binding.switchIndrive.isEnabled = false
    }

    // ── Filter Criteria Toggles ───────────────────────────────────────────────

    private fun setupFilterToggles() {
        // Max pickup
        binding.switchMaxPickup.setOnCheckedChangeListener { _, isChecked ->
            binding.containerMaxPickup.isVisible = isChecked
            savePickup()
        }
        binding.etMaxPickupKm.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { savePickup(); true } else false
        }

        // Min drop
        binding.switchMinDrop.setOnCheckedChangeListener { _, isChecked ->
            binding.containerMinDrop.isVisible = isChecked
            saveDrop()
        }
        binding.etMinDropKm.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { saveDrop(); true } else false
        }

        // Min fare
        binding.switchMinFare.setOnCheckedChangeListener { _, isChecked ->
            binding.containerMinFare.isVisible = isChecked
            saveFare()
        }
        binding.etMinFare.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { saveFare(); true } else false
        }
    }

    private fun savePickup() {
        val enabled = binding.switchMaxPickup.isChecked
        val km = binding.etMaxPickupKm.text?.toString()?.toFloatOrNull() ?: 5f
        save { viewModel.setMaxPickup(enabled, km) }
    }

    private fun saveDrop() {
        val enabled = binding.switchMinDrop.isChecked
        val km = binding.etMinDropKm.text?.toString()?.toFloatOrNull() ?: 3f
        save { viewModel.setMinDrop(enabled, km) }
    }

    private fun saveFare() {
        val enabled = binding.switchMinFare.isChecked
        val amount = binding.etMinFare.text?.toString()?.toFloatOrNull() ?: 50f
        save { viewModel.setMinFare(enabled, amount) }
    }

    private fun save(block: suspend () -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            block()
            Snackbar.make(binding.root, R.string.save_settings, Snackbar.LENGTH_SHORT).show()
        }
    }

    // ── Observe prefs to pre-fill fields when fragment is opened ─────────────

    private fun observePreferences() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.maxPickupEnabled.collect { enabled ->
                binding.switchMaxPickup.setOnCheckedChangeListener(null)
                binding.switchMaxPickup.isChecked = enabled
                binding.containerMaxPickup.isVisible = enabled
                setupFilterToggles()
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.maxPickupKm.collect { km ->
                if (binding.etMaxPickupKm.text.isNullOrEmpty())
                    binding.etMaxPickupKm.setText(if (km > 0) km.toString() else "")
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.minDropEnabled.collect { enabled ->
                binding.switchMinDrop.setOnCheckedChangeListener(null)
                binding.switchMinDrop.isChecked = enabled
                binding.containerMinDrop.isVisible = enabled
                setupFilterToggles()
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.minDropKm.collect { km ->
                if (binding.etMinDropKm.text.isNullOrEmpty())
                    binding.etMinDropKm.setText(if (km > 0) km.toString() else "")
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.minFareEnabled.collect { enabled ->
                binding.switchMinFare.setOnCheckedChangeListener(null)
                binding.switchMinFare.isChecked = enabled
                binding.containerMinFare.isVisible = enabled
                setupFilterToggles()
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.minFareAmount.collect { amount ->
                if (binding.etMinFare.text.isNullOrEmpty())
                    binding.etMinFare.setText(if (amount > 0) amount.toInt().toString() else "")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
