package com.rideautoacceptor.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.rideautoacceptor.data.local.AppDatabase
import com.rideautoacceptor.data.local.PreferencesManager
import com.rideautoacceptor.data.model.FilterPrefs
import com.rideautoacceptor.data.model.RideEvent
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for all app data.
 * Wraps Room DAO and DataStore PreferencesManager.
 */
class RideRepository(
    private val db: AppDatabase,
    val prefsManager: PreferencesManager
) {
    private val dao = db.rideEventDao()

    // ── Filter Preferences ────────────────────────────────────────────────────

    val filterPrefsFlow: Flow<FilterPrefs> = prefsManager.filterPrefsFlow

    suspend fun setMasterEnabled(enabled: Boolean) =
        prefsManager.setMasterEnabled(enabled)

    suspend fun setMaxPickup(enabled: Boolean, km: Float) =
        prefsManager.setMaxPickup(enabled, km)

    suspend fun setMinDrop(enabled: Boolean, km: Float) =
        prefsManager.setMinDrop(enabled, km)

    suspend fun setMinFare(enabled: Boolean, amount: Float) =
        prefsManager.setMinFare(enabled, amount)

    suspend fun setAppEnabled(key: String, enabled: Boolean) =
        prefsManager.setAppEnabled(key, enabled)

    // ── Ride Events ───────────────────────────────────────────────────────────

    suspend fun insertEvent(event: RideEvent): Long = dao.insert(event)

    fun getAllEventsPaged(filter: String = "ALL"): Flow<PagingData<RideEvent>> {
        val pager = when (filter) {
            "ACCEPTED" -> Pager(PagingConfig(pageSize = 30)) { dao.getAcceptedPaged() }
            "SKIPPED"  -> Pager(PagingConfig(pageSize = 30)) { dao.getSkippedPaged() }
            else       -> Pager(PagingConfig(pageSize = 30)) { dao.getAllPaged() }
        }
        return pager.flow
    }

    fun getTodayAcceptedCount(): Flow<Int> = dao.getTodayAcceptedCount()
    fun getTodaySkippedCount():  Flow<Int> = dao.getTodaySkippedCount()
    fun getLatestEvent():        Flow<RideEvent?> = dao.getLatestEvent()

    suspend fun deleteEvent(event: RideEvent) = dao.delete(event)
    suspend fun deleteAll()                    = dao.deleteAll()

    /** Auto-prune events older than 30 days */
    suspend fun pruneOldEvents() {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        dao.deleteOlderThan(thirtyDaysAgo)
    }

    // ── Singleton ─────────────────────────────────────────────────────────────
    companion object {
        @Volatile private var INSTANCE: RideRepository? = null

        fun getInstance(db: AppDatabase, prefsManager: PreferencesManager): RideRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: RideRepository(db, prefsManager).also { INSTANCE = it }
            }
    }
}
