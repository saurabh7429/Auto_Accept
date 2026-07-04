package com.rideautoacceptor.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.rideautoacceptor.data.model.FilterPrefs
import com.rideautoacceptor.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// Extension property — single DataStore per app
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = Constants.PREFS_NAME
)

/**
 * DataStore-backed preferences manager.
 * All reads return [Flow] so the UI auto-updates when settings change.
 * All writes are suspend functions (coroutine-safe).
 */
class PreferencesManager(context: Context) {

    private val dataStore = context.dataStore

    // ── Preference Keys ───────────────────────────────────────────────────────

    private val KEY_MASTER_ENABLED     = booleanPreferencesKey(Constants.KEY_MASTER_ENABLED)
    private val KEY_MAX_PICKUP_ENABLED = booleanPreferencesKey(Constants.KEY_MAX_PICKUP_ENABLED)
    private val KEY_MAX_PICKUP_KM      = floatPreferencesKey(Constants.KEY_MAX_PICKUP_KM)
    private val KEY_MIN_DROP_ENABLED   = booleanPreferencesKey(Constants.KEY_MIN_DROP_ENABLED)
    private val KEY_MIN_DROP_KM        = floatPreferencesKey(Constants.KEY_MIN_DROP_KM)
    private val KEY_MIN_FARE_ENABLED   = booleanPreferencesKey(Constants.KEY_MIN_FARE_ENABLED)
    private val KEY_MIN_FARE_AMOUNT    = floatPreferencesKey(Constants.KEY_MIN_FARE_AMOUNT)
    private val KEY_APP_OLA            = booleanPreferencesKey(Constants.KEY_APP_OLA)
    private val KEY_APP_RAPIDO         = booleanPreferencesKey(Constants.KEY_APP_RAPIDO)
    private val KEY_APP_UBER           = booleanPreferencesKey(Constants.KEY_APP_UBER)
    private val KEY_APP_PORTER         = booleanPreferencesKey(Constants.KEY_APP_PORTER)
    private val KEY_APP_INDRIVE        = booleanPreferencesKey(Constants.KEY_APP_INDRIVE)

    // ── Read ──────────────────────────────────────────────────────────────────

    val filterPrefsFlow: Flow<FilterPrefs> = dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences())
            else throw e
        }
        .map { prefs ->
            FilterPrefs(
                isEnabled         = prefs[KEY_MASTER_ENABLED]     ?: false,
                olaEnabled        = prefs[KEY_APP_OLA]            ?: true,
                rapidoEnabled     = prefs[KEY_APP_RAPIDO]         ?: true,
                uberEnabled       = prefs[KEY_APP_UBER]           ?: true,
                porterEnabled     = prefs[KEY_APP_PORTER]         ?: true,
                indriveEnabled    = prefs[KEY_APP_INDRIVE]        ?: false,
                maxPickupDistanceKm = if (prefs[KEY_MAX_PICKUP_ENABLED] == true)
                                        prefs[KEY_MAX_PICKUP_KM] else null,
                minDropDistanceKm = if (prefs[KEY_MIN_DROP_ENABLED] == true)
                                        prefs[KEY_MIN_DROP_KM] else null,
                minFareAmount     = if (prefs[KEY_MIN_FARE_ENABLED] == true)
                                        prefs[KEY_MIN_FARE_AMOUNT] else null
            )
        }

    // ── Write ─────────────────────────────────────────────────────────────────

    suspend fun setMasterEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_MASTER_ENABLED] = enabled }
    }

    suspend fun setMaxPickup(enabled: Boolean, km: Float) {
        dataStore.edit {
            it[KEY_MAX_PICKUP_ENABLED] = enabled
            it[KEY_MAX_PICKUP_KM]      = km
        }
    }

    suspend fun setMinDrop(enabled: Boolean, km: Float) {
        dataStore.edit {
            it[KEY_MIN_DROP_ENABLED] = enabled
            it[KEY_MIN_DROP_KM]      = km
        }
    }

    suspend fun setMinFare(enabled: Boolean, amount: Float) {
        dataStore.edit {
            it[KEY_MIN_FARE_ENABLED] = enabled
            it[KEY_MIN_FARE_AMOUNT]  = amount
        }
    }

    suspend fun setAppEnabled(key: String, enabled: Boolean) {
        val prefKey = booleanPreferencesKey(key)
        dataStore.edit { it[prefKey] = enabled }
    }

    // Raw boolean reads for the settings screen toggle states
    val maxPickupEnabledFlow: Flow<Boolean> = dataStore.data.map { it[KEY_MAX_PICKUP_ENABLED] ?: false }
    val maxPickupKmFlow:      Flow<Float>   = dataStore.data.map { it[KEY_MAX_PICKUP_KM]      ?: 5f   }
    val minDropEnabledFlow:   Flow<Boolean> = dataStore.data.map { it[KEY_MIN_DROP_ENABLED]   ?: false }
    val minDropKmFlow:        Flow<Float>   = dataStore.data.map { it[KEY_MIN_DROP_KM]        ?: 3f   }
    val minFareEnabledFlow:   Flow<Boolean> = dataStore.data.map { it[KEY_MIN_FARE_ENABLED]   ?: false }
    val minFareAmountFlow:    Flow<Float>   = dataStore.data.map { it[KEY_MIN_FARE_AMOUNT]    ?: 50f  }
}
