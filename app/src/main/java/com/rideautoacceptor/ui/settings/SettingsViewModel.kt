package com.rideautoacceptor.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rideautoacceptor.data.repository.RideRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: RideRepository) : ViewModel() {

    // ── Filter criteria state flows ───────────────────────────────────────────
    val maxPickupEnabled: Flow<Boolean> = repository.prefsManager.maxPickupEnabledFlow
    val maxPickupKm:      Flow<Float>   = repository.prefsManager.maxPickupKmFlow
    val minDropEnabled:   Flow<Boolean> = repository.prefsManager.minDropEnabledFlow
    val minDropKm:        Flow<Float>   = repository.prefsManager.minDropKmFlow
    val minFareEnabled:   Flow<Boolean> = repository.prefsManager.minFareEnabledFlow
    val minFareAmount:    Flow<Float>   = repository.prefsManager.minFareAmountFlow

    // ── App toggle state ──────────────────────────────────────────────────────
    fun getAppEnabledFlow(key: String, default: Boolean): Flow<Boolean> =
        repository.filterPrefsFlow.map { prefs ->
            when (key) {
                "app_ola_enabled"     -> prefs.olaEnabled
                "app_rapido_enabled"  -> prefs.rapidoEnabled
                "app_uber_enabled"    -> prefs.uberEnabled
                "app_porter_enabled"  -> prefs.porterEnabled
                "app_indrive_enabled" -> prefs.indriveEnabled
                else                  -> default
            }
        }

    // ── Writes ────────────────────────────────────────────────────────────────
    suspend fun setMaxPickup(enabled: Boolean, km: Float) =
        repository.setMaxPickup(enabled, km)

    suspend fun setMinDrop(enabled: Boolean, km: Float) =
        repository.setMinDrop(enabled, km)

    suspend fun setMinFare(enabled: Boolean, amount: Float) =
        repository.setMinFare(enabled, amount)

    suspend fun setAppEnabled(key: String, enabled: Boolean) =
        repository.setAppEnabled(key, enabled)

    class Factory(private val repository: RideRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(repository) as T
    }
}
